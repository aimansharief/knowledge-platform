locals {
  search_dependency = var.environment == "azure" ? [helm_release.content] : []
}


resource "helm_release" "search" {
  name             = "search"
  chart            = var.SEARCH_CHART
  namespace        = var.SEARCH_NAMESPACE
  create_namespace = true
  dependency_update = true
  depends_on       = [local.search_dependency]
  wait_for_jobs    = true

}