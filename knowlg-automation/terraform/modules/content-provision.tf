
locals {
  content_dependency = var.environment == "azure" ? [helm_release.redis] : []
}

resource "helm_release" "content" {
  name             = "content"
  chart            = var.CONTENT_CHART
  namespace        = var.CONTENT_NAMESPACE
  create_namespace = true
  dependency_update = true
  depends_on       = [local.content_dependency]
  wait_for_jobs    = true
}
 