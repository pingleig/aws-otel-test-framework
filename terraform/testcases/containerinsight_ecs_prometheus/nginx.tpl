[
    {
      "name": "nginx",
      "image": "616237574086.dkr.ecr.us-west-2.amazonaws.com/nginx-cwagent:latest",
      "essential": true,
      "portMappings": [
        {
          "containerPort": 80,
          "protocol": "tcp"
        }
      ],
      "links": [
        "app"
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-create-group": "True",
          "awslogs-group": "/pingleig/aoc/ecs-sd",
          "awslogs-region": "${region}",
          "awslogs-stream-prefix": "aco-ecs-sd-nginx"
        }
      }
    },
    {
      "name": "app",
      "image": "616237574086.dkr.ecr.us-west-2.amazonaws.com/nginx-app:latest",
      "essential": true,
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-create-group": "True",
          "awslogs-group": "/pingleig/aoc/ecs-sd",
          "awslogs-region": "${region}",
          "awslogs-stream-prefix": "aco-ecs-sd-nginx"
        }
      }
    },
    {
      "name": "nginx-prometheus-exporter",
      "image": "616237574086.dkr.ecr.us-west-2.amazonaws.com/nginx-prometheus-exporter:0.8.0",
      "essential": true,
      "command": [
        "-nginx.scrape-uri",
        "http://nginx:8080/stub_status"
      ],
      "links": [
        "nginx"
      ],
      "portMappings": [
        {
          "containerPort": 9113,
          "protocol": "tcp"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-create-group": "True",
          "awslogs-group": "/pingleig/aoc/ecs-sd",
          "awslogs-region": "${region}",
          "awslogs-stream-prefix": "aco-ecs-sd-nginx"
        }
      }
    }
]