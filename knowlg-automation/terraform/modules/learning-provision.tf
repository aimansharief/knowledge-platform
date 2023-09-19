locals {
  learning_dependency = var.environment == "azure" ? [helm_release.taxonomy] : []
}

resource "helm_release" "learning" {
  name             = "learning"
  chart            = var.LEARNING_CHART
  namespace        = var.LEARNING_NAMESPACE
  create_namespace = true
  dependency_update = true
  depends_on       = [local.learning_dependency]
  wait_for_jobs    = true

}