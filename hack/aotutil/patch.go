package main

import (
	"context"
	"fmt"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/ssm"
	"github.com/aws/aws-sdk-go-v2/service/ssm/types"
	"go.uber.org/zap"
)

const (
	SSMPatchDocument  = "AWS-RunPatchBaseline"
	SSMReportDocument = "AWS-GatherSoftwareInventory"
)

type EC2PatchStatus struct {
	PatchTime  time.Time
	ReportTime time.Time
}

type SSMWrapper struct {
	logger *zap.Logger
	client *ssm.Client
}

const (
	waitInterval           = time.Minute
	waitPatchReportTimeout = 30 * time.Minute // this is the minimal ssm association interval
)

func NewSSM(cfg aws.Config, logger *zap.Logger) *SSMWrapper {
	client := ssm.NewFromConfig(cfg)
	logger = logger.With(zap.String("Component", "ssm"))
	return &SSMWrapper{logger: logger, client: client}
}

// NOTE: there is no builtin waiter implementation for checking association status.
func (s *SSMWrapper) WaitPatch(ctx context.Context, instanceId string, timeout time.Duration) error {
	logger := s.logger.With(zap.String("InstanceId", instanceId), zap.String("Action", "WaitPatch"))
	start := time.Now()
	timer := time.After(timeout)
	ticker := time.NewTicker(waitInterval)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			infos, err := describeInstanceAssocStatus(ctx, s.client, instanceId)
			if err != nil {
				return err
			}
			for _, assoc := range infos {
				if aws.ToString(assoc.Name) != SSMPatchDocument {
					continue
				}
				status := aws.ToString(assoc.Status)
				switch status {
				case "Success":
					logger.Info("patch on instance succeeded", zap.Duration("Waited", time.Now().Sub(start)))
					return nil
				case "Failed":
					return fmt.Errorf("patch on instance failed instanceId %s waited %s", instanceId, time.Now().Sub(start))
				default:
					logger.Info("waiting patching", zap.String("Status", status))
				}
			}
		case <-timer:
			return fmt.Errorf("wait patch timeout on instace %s after %s", instanceId, time.Now().Sub(start))
		}
	}
}

func (s *SSMWrapper) WaitPatchReported(ctx context.Context, instanceId string) (bool, error) {
	logger := s.logger.With(zap.String("InstanceId", instanceId), zap.String("Action", "WaitPatchReported"))
	start := time.Now()
	timer := time.After(waitPatchReportTimeout)
	ticker := time.NewTicker(waitInterval)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			infos, err := describeInstanceAssocStatus(ctx, s.client, instanceId)
			if err != nil {
				return false, err
			}
			var patchTime, reportTime time.Time
			for _, assoc := range infos {
				switch aws.ToString(assoc.Name) {
				case SSMPatchDocument:
					patchTime = aws.ToTime(assoc.ExecutionDate)
				case SSMReportDocument:
					reportTime = aws.ToTime(assoc.ExecutionDate)
				}
			}
			logger.Info("waiting patch report", zap.Time("PatchTime", patchTime), zap.Time("ReportTime", reportTime))
			if patchTime.IsZero() || reportTime.IsZero() || reportTime.Before(patchTime) {
				continue
			}
			logger.Info("patch reported")
			return true, nil
		case <-timer:
			return false, fmt.Errorf("wait patch report timeout after %s", time.Now().Sub(start))
		}
	}
}

func describeInstanceAssocStatus(ctx context.Context, client *ssm.Client, instanceId string) ([]types.InstanceAssociationStatusInfo, error) {
	res, err := client.DescribeInstanceAssociationsStatus(ctx, &ssm.DescribeInstanceAssociationsStatusInput{
		InstanceId: aws.String(instanceId),
	})
	if err != nil {
		return nil, fmt.Errorf("describe instance association status failed: %w", err)
	}
	return res.InstanceAssociationStatusInfos, nil
}
