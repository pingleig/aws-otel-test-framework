[
    {
      "name": "tomcat-prometheus-workload-java-ec2-bridge-dynamic-port",
      "image": "616237574086.dkr.ecr.us-west-2.amazonaws.com/prometheus-sample-tomcat-jmx:0.1",
      "portMappings": [
        {
          "protocol": "tcp",
          "containerPort": 9404
        }
      ],
      "dockerLabels": {
        "ECS_PROMETHEUS_EXPORTER_PORT": "9404",
        "Java_EMF_Metrics": "true"
      },
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-create-group": "True",
          "awslogs-group": "/pingleig/aoc/ecs-sd",
          "awslogs-region": "{{awslogs-region}}",
          "awslogs-stream-prefix": "aoc-ecs-sd-tomcat-jmx"
        }
      }
    }
]