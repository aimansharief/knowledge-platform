locals {
  cassandra_dependency = var.environment == "azure" ? [helm_release.neo4j] : []
}

resource "helm_release" "cassandra" {
  name             = "cassandra"
  chart            = var.CASSANDRA_CHART
  namespace        = var.CASSANDRA_NAMESPACE
  create_namespace = true
  dependency_update = true
  depends_on       = [local.cassandra_dependency]
  wait_for_jobs    = true

}
