locals {
  taxonomy_dependency = var.environment == "azure" ? [helm_release.search] : []
}

resource "helm_release" "taxonomy" {
  name             = "taxonomy"
  chart            = var.TAXONOMY_CHART
  namespace        = var.TAXONOMY_NAMESPACE
  create_namespace = true
  dependency_update = true
  depends_on       = [local.taxonomy_dependency]
  wait_for_jobs    = true

}
