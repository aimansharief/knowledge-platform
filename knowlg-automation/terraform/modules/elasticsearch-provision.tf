locals {
  es_dependency = var.environment == "azure" ? [helm_release.cassandra] : []
}

resource "helm_release" "elasticsearch" {
  name             = "elasticsearch"
  chart            = var.ELASTICSEARCH_CHART
  namespace        = var.ELASTICSEARCH_NAMESPACE
  create_namespace = true
  dependency_update = true
  depends_on       = [local.es_dependency]
  wait_for_jobs    = true

}
