package org.sunbird.content.review.mgr

import org.apache.commons.lang3.StringUtils
import org.sunbird.common.Platform
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.content.publish.mgr.PublishManager
import org.sunbird.content.util.{CompetencyFrameworkValidator, ContentConstants}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.graph.nodes.DataNode
import org.sunbird.mimetype.factory.MimeTypeManagerFactory

import java.util
import scala.collection.Map
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object ReviewManager {

	private val AUTO_PUBLISH_ENABLED: Boolean = Platform.getBoolean("content.auto_publish.enabled", false)
	private val AUTO_PUBLISH_PRIMARY_CATEGORIES: util.List[String] = Platform.getStringList("content.auto_publish_categories", new util.ArrayList[String]())

	def review(request: Request, node: Node)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Response] = {
		val identifier: String = node.getIdentifier
		val mimeType = node.getMetadata().getOrDefault("mimeType", "").asInstanceOf[String]
		
		// Validate Competency Framework if applicable
		CompetencyFrameworkValidator.validateCompetencyFramework(request, node).flatMap { _ =>
			val mgr = MimeTypeManagerFactory.getManager(node.getObjectType, mimeType)
			val reviewFuture: Future[Map[String, AnyRef]] = mgr.review(identifier, node)
			reviewFuture.map(result => {
				val updateReq = new Request()
				updateReq.setContext(request.getContext)
				updateReq.putAll(result.asJava)
				DataNode.update(updateReq).map(updatedNode => {
					val primaryCategory = updatedNode.getMetadata.getOrDefault("primaryCategory", "").asInstanceOf[String]
					// Check if auto-publish is enabled and primaryCategory matches the configured list
					if (AUTO_PUBLISH_ENABLED && StringUtils.isNotBlank(primaryCategory) && AUTO_PUBLISH_PRIMARY_CATEGORIES.contains(primaryCategory)) {
						// Trigger publish with System as publisher and return publish response
						triggerPublish(request, updatedNode)
					} else {
						Future(ResponseHandler.OK.putAll(Map("identifier" -> updatedNode.getIdentifier.replace(".img", ""), "versionKey" -> updatedNode.getMetadata.get("versionKey")).asJava))
					}
				}).flatMap(f => f)
			}).flatMap(f => f)
		}
	}

	private def triggerPublish(request: Request, node: Node)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Response] = {
		// Create a new request for publish operation
		val publishReq = new Request(request)
		publishReq.getRequest.put(ContentConstants.LAST_PUBLISHED_BY, ContentConstants.SYSTEM)
		node.getMetadata.put(ContentConstants.LAST_PUBLISHED_BY, ContentConstants.SYSTEM)
		PublishManager.publish(publishReq, node)
	}
}


