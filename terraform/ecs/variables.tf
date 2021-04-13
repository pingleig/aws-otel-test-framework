# ------------------------------------------------------------------------
# Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.
# -------------------------------------------------------------------------

variable "ecs_launch_type" {
  default = "EC2"
}

variable "sample_app_callable" {
  default = true
}

# we use efs for copy cert, which is not required in cases like prometheus
variable "disable_efs" {
  default = false
}

# prometheus does not need mocked server
variable "disable_mocked_server" {
  default = false
}

variable "mock_endpoint" {
  default = "localhost/put-data"
}

variable "ecs_taskdef_directory" {
  default = "defaults"
}

variable "ecs_extra_apps_image_repo" {
  # TODO: change the image repo to some actual repo
  default = "public.ecr.aws/aws-observability/blabla"
}

#variable "cloudwatch_context" {
#  default = {}
#}

variable "ecs_extra_apps" {
  type = map(object({
    definition   = string
    service_name = string
    service_type = string
    replicas     = number
    network_mode = string
    launch_type  = string
    cpu          = number
    memory       = number
  }))
  default = {}
}

