locals {
  dial_dependency = var.environment == "azure" ? [helm_release.learning] : []
}

resource "helm_release" "dial" {
  name             = "dial"
  chart            = var.DIAL_CHART
  namespace        = var.DIAL_NAMESPACE
  create_namespace = true
  dependency_update = true
  depends_on       = [local.dial_dependency]
  wait_for_jobs    = true

}