package org.sunbird.graph

import org.sunbird.common.Platform
import org.sunbird.common.dto.{Property, Request, Response, ResponseHandler}
import org.sunbird.common.exception.ResponseCode
import org.sunbird.graph.dac.model.{Node, SearchCriteria, SubGraph, Vertex}
import org.sunbird.graph.external.ExternalPropsManager
import org.sunbird.graph.service.operation.{GraphAsyncOperations, Neo4JBoltSearchOperations, NodeAsyncOperations, SearchAsyncOperations}
import org.sunbird.graph.util.CSPMetaUtil
import org.sunbird.janus.service.operation.{EdgeOperations, SearchOperations, VertexOperations}

import java.lang
import scala.concurrent.{ExecutionContext, Future}
class JanusGraphService {

  private val VertexOperations = new VertexOperations()
  private val EdgeOperations = new EdgeOperations()
  private val SearchOperations = new SearchOperations()

  implicit val ec: ExecutionContext = ExecutionContext.global
  val isrRelativePathEnabled: lang.Boolean = Platform.getBoolean("cloudstorage.metadata.replace_absolute_path", false)


  def addVertex(graphId: String, vertex: Vertex): Future[Vertex] = {
    if (isrRelativePathEnabled) {
      val metadata = CSPMetaUtil.updateRelativePath(vertex.getMetadata)
      vertex.setMetadata(metadata)
    }
    VertexOperations.addVertex(graphId, vertex).map(resVertex => if (isrRelativePathEnabled) CSPMetaUtil.updateAbsolutePath(resVertex) else resVertex)

  }

  def createEdges(graphId: String, edgeMap: java.util.List[java.util.Map[String, AnyRef]]) = {
    EdgeOperations.createEdges(graphId, edgeMap)
  }

  def getNodeByUniqueId(graphId: String, vertexId: String, getTags: Boolean, request: Request): Future[Vertex] = {
    SearchOperations.getNodeByUniqueId(graphId, vertexId, getTags, request).map(vertex => if (isrRelativePathEnabled) CSPMetaUtil.updateAbsolutePath(vertex) else vertex)
  }

}
