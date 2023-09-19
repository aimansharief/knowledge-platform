locals {
  redis_dependency = var.environment == "azure" ? [helm_release.elasticsearch] : []
}

resource "helm_release" "redis" {
  name             = "redis"
  chart            = var.REDIS_CHART
  namespace        = var.REDIS_NAMESPACE
  create_namespace = true
  dependency_update = true
  depends_on       = [local.redis_dependency]
  wait_for_jobs    = true

}