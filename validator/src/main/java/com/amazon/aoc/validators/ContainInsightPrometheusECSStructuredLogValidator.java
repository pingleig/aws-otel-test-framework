package com.amazon.aoc.validators;

import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.helpers.MustacheHelper;
import com.amazon.aoc.models.CloudWatchContext;
import com.amazon.aoc.models.Context;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.report.ProcessingReport;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j2
public class ContainInsightPrometheusECSStructuredLogValidator
    extends AbstractStructuredLogValidator {
  public ContainInsightPrometheusECSStructuredLogValidator() {
    // TODO: this is not very useful as we are resetting the log group in init
    super("prometheus");
  }

  // Key is app name
  private Map<String, JsonSchema> validateJsonSchema = new HashMap<>();
  private List<CloudWatchContext.App> validateApps;

  @Override
  void init(Context context, String templatePath) throws Exception {
    validateApps = getAppsToValidate(context.getCloudWatchContext());
    MustacheHelper mustacheHelper = new MustacheHelper();

    for (CloudWatchContext.App app : validateApps) {
      String templateInput = mustacheHelper.render(new JsonSchemaFileConfig(
          FilenameUtils.concat(templatePath, app.getName() + ".json")), context);
      validateJsonSchema.put(app.getName(), parseJsonSchema(templateInput));
    }

    // /aws/ecs/containerinsights/aoc-prometheus-dashboard-1/prometheus
    logGroupName = String.format("/aws/ecs/containerinsights/%s/%s",
        context.getCloudWatchContext().getClusterName(), "prometheus");
    log.info("log group name is {}", logGroupName);
    log.info("size of validate schema is {}", validateJsonSchema.size());
  }

  @Override
  Set<String> getValidatingLogStreamNames() {
    Set<String> logStreamNames = new HashSet<>();
    for (CloudWatchContext.App validateApp : validateApps) {
      logStreamNames.add(validateApp.getJob());
    }
    return logStreamNames;
  }

  @Override
  JsonSchema findJsonSchemaForValidation(JsonNode logEventNode) {
    // TODO: we will use need detect app based on log event
    // We can use TaskDefinitionFamily to check
    // ServiceName is kind of optional, it's only there when we fetch service ... (
    // might want to change that later)
    String taskFamily = logEventNode.get("TaskDefinitionFamily").asText();
    if (taskFamily.contains("jmx")) {
      // log.info("jmx task family {}", taskFamily);
      return validateJsonSchema.get("jmx");
    }
    if (taskFamily.contains("nginx")) {
      return validateJsonSchema.get("nginx");
    }
    // TODO: what to when we can't find the valid validator, seems just return null
    return null;
  }

  @Override
  void printJsonSchemaValidationResult(JsonNode logEventNode, ProcessingReport report) {
    if (!report.isSuccess()) {
      log.warn("validation failed for {}", logEventNode);
      log.error(report);
    } else {
      log.info("validation passed for {}", logEventNode);
    }
  }

  // TODO(mengyi): the update and check result
  @Override
  void updateJsonSchemaValidationResult(JsonNode logEventNode, boolean success) {
    if (success) {
      String taskFamily = logEventNode.get("TaskDefinitionFamily").asText();
      if (taskFamily.contains("jmx")) {
        validateJsonSchema.remove("jmx");
      }
      if (taskFamily.contains("nginx")) {
        validateJsonSchema.remove("nginx");
      }
    }
  }

  // TODO(pingleig): same as eks
  @Override
  void checkResult() throws Exception {
    if (validateJsonSchema.size() == 0) {
      return;
    }
    String[] failedTargets = new String[validateJsonSchema.size()];
    int i = 0;
    for (String appNamespace : validateJsonSchema.keySet()) {
      failedTargets[i] = appNamespace;
      i++;
    }
    throw new BaseException(
        ExceptionCode.LOG_FORMAT_NOT_MATCHED,
        String.format("[ContainerInsight] log structure validation failed in namespace %s",
            StringUtils.join(",", failedTargets)));
  }

  private static List<CloudWatchContext.App> getAppsToValidate(CloudWatchContext cwContext) {
    List<CloudWatchContext.App> apps = new ArrayList<>();
    if (cwContext.getNginx() != null) {
      apps.add(cwContext.getNginx());
    }
    if (cwContext.getJmx() != null) {
      apps.add(cwContext.getJmx());
    }
    return apps;
  }
}
