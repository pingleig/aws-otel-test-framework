# this file is defined in validator/src/main/resources/validations
validation_config = "ecs-container-insight.yml"
# no need for any lb
sample_app_callable = false
# sample apps that emit ecs metrics
ecs_extra_apps = {
  # TODO: need both host network and aws vpc
  jmx = {
    definition   = "jmx.tpl"
    service_name = "jmx"
    service_type = "replica"
    replicas     = 1
    network_mode = "bridge"
    launch_type  = "EC2"
    cpu          = 256
    memory       = 256
  }

  nginx = {
    definition   = "nginx.tpl"
    service_name = "nginx-service"
    service_type = "replica"
    replicas     = 1
    network_mode = "bridge"
    launch_type  = "EC2"
    cpu          = 384
    memory       = 384
  }
}