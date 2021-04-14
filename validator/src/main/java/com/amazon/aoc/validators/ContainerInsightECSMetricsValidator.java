package com.amazon.aoc.validators;

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import com.amazon.aoc.services.CloudWatchService;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.MetricDataResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
// TODO(pingleig): merge with other cw validators
public class ContainerInsightECSMetricsValidator implements IValidator {
  private CloudWatchService cloudWatchService;
  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
  private List<Metric> expectedMetrics;

  private static final int MAX_RETRY_COUNT = 6;
  private static final int CHECK_INTERVAL_IN_MILLI = 30 * 1000;
  private static final int CHECK_DURATION_IN_SECONDS = 2 * 60;

  @Override
  public void init(Context context, ValidationConfig validationConfig, ICaller caller,
                   FileConfig expectedDataTemplate) throws Exception {
    log.info("expected template is {}", expectedDataTemplate.getPath());
    cloudWatchService = new CloudWatchService(context.getRegion());

    MustacheHelper mustacheHelper = new MustacheHelper();
    String templateInput = mustacheHelper.render(expectedDataTemplate, context);
    expectedMetrics = mapper.readValue(templateInput.getBytes(StandardCharsets.UTF_8),
        new TypeReference<List<Metric>>() {
        });
  }

  @Override
  public void validate() throws Exception {
    log.info("[ContainerInsight] start validating metrics, pause 60s for metric collection");
    TimeUnit.SECONDS.sleep(60);
    log.info("[ContainerInsight] resume validation");

    RetryHelper.retry(MAX_RETRY_COUNT, CHECK_INTERVAL_IN_MILLI, true, () -> {
      Instant startTime = Instant.now().minusSeconds(CHECK_DURATION_IN_SECONDS)
          .truncatedTo(ChronoUnit.MINUTES);
      Instant endTime = startTime.plusSeconds(CHECK_DURATION_IN_SECONDS);

      boolean hasNotFound = false;
      for (Metric expectedMetric : expectedMetrics) {
        log.info("get metric name {} nameesapce {} cluster {} TaskDefinitionFamily {}",
            expectedMetric.getMetricName(),
            expectedMetric.getNamespace(),
            expectedMetric.getDimensions().get(0),
            expectedMetric.getDimensions().get(1)
        );
        List<MetricDataResult> batchResult = cloudWatchService.getMetricData(expectedMetric,
            Date.from(startTime), Date.from(endTime));
        boolean found = false;
        for (MetricDataResult result : batchResult) {
          if (result.getValues().size() > 0) {
            found = true;
          }
        }
        if (!found) {
          log.warn("metric not found {}", expectedMetric.getMetricName());
          hasNotFound = true;
          //  throw new BaseException(ExceptionCode.EXPECTED_METRIC_NOT_FOUND,
          //  String.format("[ContainerInsight] metric %s not found under namespace %s",
          //  expectedMetric.getMetricName(), expectedMetric.getNamespace()));
        }
      }
      if (hasNotFound) {
        throw new BaseException(ExceptionCode.EXPECTED_METRIC_NOT_FOUND);
      }
    });
    log.info("[ContainerInsight] finish validation successfully");
  }
}
