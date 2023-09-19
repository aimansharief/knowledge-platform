variable "kind_cluster_name" {
  type        = string
  description = "The name of the cluster."
  default     = "one-click"
}

variable "kind_cluster_config_path" {
  type        = string
  description = "The location where this cluster's kubeconfig will be saved to."
  default = "~/.kube/config"
}

variable "kube_config_context" {
  type        = string
  description = "The config context in kubeconfig"
  default = "kind-one-click"
 }

variable "ingress_nginx_helm_version" {
  type        = string
  description = "The Helm version for the nginx ingress controller."
  default     = "4.0.6"
}

variable "ingress_nginx_namespace" {
  type        = string
  description = "The nginx ingress namespace (it will be created if needed)."
  default     = "ingress-nginx"
}


variable "STAGE" {
  description = "Deployment Stage"
  default     = "dev"
}

#KAFKA
variable "KAFKA_CHART" {
  description = "Kafka Instance Running Namespace"
  default     = "../../helm_charts/kafka"
}

variable "KAFKA_NAMESPACE" {
  description = "Kafka Instance Running Namespace"
  default     = "knowlg-db"
}


#FLINK
variable "flink_release_name" {
    type        = string
    description = "Flink helm release name."
    default     = "qrcode-image-generator"
}

variable "flink_namespace" {
    type        = string
    description = "Flink namespace."
    default     = "knowlg-job"
}

variable "flink_chart_path" {
    type        = string
    description = "Flink chart path."
    default     = "../../helm_charts/flink"
}

variable "flink_chart_install_timeout" {
    type        = number
    description = "Flink chart install timeout."
    default     = 900
}

variable "flink_create_namespace" {
    type        = bool
    description = "Create flink namespace."
    default     = true
}

variable "flink_wait_for_jobs" {
    type        = bool
    description = "Flink wait for jobs paramater."
    default     = false
}

variable "flink_chart_template" {
    type        = string
    description = "Flink chart values.yaml path."
    default     = "../terraform_helm_templates/flink.yaml.tfpl"
}

variable "flink_kubernetes_service_name" {
    type        = string
    description = "Flink kubernetes service name."
    default     = "qrcode-image-generator-jobmanager"
}

variable "environment" {
  type        = string
  description = "Environment value"
  default     = "local"
}