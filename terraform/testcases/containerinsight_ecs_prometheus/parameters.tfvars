# this file is defined in validator/src/main/resources/validations
validation_config =   "ecs-container-insight.yml"
# no need for any lb
sample_app_callable = false
# sample apps that emit ecs metrics
ecs_extra_apps =      {
  jmx = {
    definition =   "jmx.tpl"
    service_name = "jmx"
    service_type = "replica"
    replicas =     1
    network_mode = "host"
  }

  # TODO: need both host and aws vpc
  nginx = {
    definition =   "nginx.tpl"
    service_name = "nginx"
    service_type = "replica"
    replicas =     1
    network_mode = "host"
  }
}