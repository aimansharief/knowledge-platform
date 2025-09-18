package org.sunbird.content.util

import org.apache.commons.lang3.StringUtils
import org.sunbird.common.JsonUtils
import org.sunbird.common.dto.Request
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.graph.nodes.DataNode
import org.sunbird.managers.HierarchyManager

import java.util
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object CompetencyFrameworkValidator {

  def validateCompetencyFramework(request: Request, node: Node)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Unit] = {
    val primaryCategory = node.getMetadata.getOrDefault("primaryCategory", "").asInstanceOf[String]
    
    if (StringUtils.equalsIgnoreCase("Competency Framework", primaryCategory)) {
      // Get hierarchy for validation
      val hierarchyRequest = new Request(request)
      hierarchyRequest.put("rootId", node.getIdentifier)
      hierarchyRequest.put("mode", "edit")
      
      HierarchyManager.getHierarchy(hierarchyRequest).flatMap { response =>
        if (response.getResponseCode.code() != 200) {
          throw new ClientException("ERR_HIERARCHY_NOT_FOUND", "Unable to fetch hierarchy for validation")
        }
        
        val hierarchyData = response.getResult.get("content").asInstanceOf[util.Map[String, AnyRef]]
        val errors = ListBuffer[String]()
        validateHierarchyRecursive(hierarchyData, errors, node)
      }
    } else {
      Future.successful(())
    }
  }

  private def validateHierarchyRecursive(nodeData: util.Map[String, AnyRef], errors: ListBuffer[String], parentNode: Node)
                                (implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Unit] = {
    val primaryCategory = nodeData.getOrDefault("primaryCategory", "").asInstanceOf[String]
    
    val currentValidations = if (StringUtils.equalsIgnoreCase("Competency Framework", primaryCategory)) {
      validateCompetencyFrameworkNode(nodeData, errors, parentNode)
    } else if (StringUtils.equalsIgnoreCase("Competency Level", primaryCategory)) {
      validateCompetencyLevelNode(nodeData, errors, parentNode)
    } else {
      Future.successful(())
    }
    
    // Recursively validate children
    val children = nodeData.getOrDefault("children", new util.ArrayList[util.Map[String, AnyRef]]()).asInstanceOf[util.List[util.Map[String, AnyRef]]]
    val childValidations = children.asScala.map(child => validateHierarchyRecursive(child, errors, parentNode))
    
    // Wait for all validations to complete
    Future.sequence(currentValidations :: childValidations.toList).map(_ => ())
  }

  private def validateCompetencyFrameworkNode(nodeData: util.Map[String, AnyRef], errors: ListBuffer[String], parentNode: Node)
                                         (implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Unit] = {
    // Validate visibility
    val visibility = nodeData.getOrDefault("visibility", "").asInstanceOf[String]
    if (!StringUtils.equalsIgnoreCase("Private", visibility)) {
      throw new ClientException("ERR_COMPETENCY_FRAMEWORK_VALIDATION", "Competency Framework visibility must be 'Private'")
    }

    // Validate sector
    val sector = nodeData.get("sector") match {
      case map: util.Map[String, AnyRef] => map
      case null => null
      case _ => null // If sector exists but is not a Map, treat as null
    }
    if (sector != null) {
      val sectorName = sector.getOrDefault("name", "").asInstanceOf[String]
      val sectorDomain = sector.getOrDefault("domain", "").asInstanceOf[String]
      
      if (!StringUtils.equalsIgnoreCase("Education", sectorName)) {
        throw new ClientException("ERR_COMPETENCY_FRAMEWORK_VALIDATION", "Competency Framework sector name must be 'Education'")
      }
      
      if (!StringUtils.equalsIgnoreCase("Preschool", sectorDomain)) {
        throw new ClientException("ERR_COMPETENCY_FRAMEWORK_VALIDATION", "Competency Framework sector domain must be 'Preschool'")
      }
    }

    // Validate signupBy
    val signupBy = nodeData.getOrDefault("signupBy", "").asInstanceOf[String]
    if (StringUtils.isNotEmpty(signupBy) && !StringUtils.equalsAnyIgnoreCase(signupBy, "Admin", "User")) {
      throw new ClientException("ERR_COMPETENCY_FRAMEWORK_VALIDATION", "Competency Framework signupBy must be 'Admin' or 'User'")
    }

    // Validate enrollmentType
    val enrollmentType = nodeData.getOrDefault("enrollmentType", "").asInstanceOf[String]
    if (StringUtils.isNotEmpty(enrollmentType) && 
        !StringUtils.equalsAnyIgnoreCase(enrollmentType, "Full Enrollment", "Entrance Exam Based", "Progress Based")) {
      throw new ClientException("ERR_COMPETENCY_FRAMEWORK_VALIDATION", "Competency Framework enrollmentType must be 'Full Enrollment', 'Entrance Exam Based', or 'Progress Based'")
    }

    // Validate entranceExam requirement for "Entrance Exam Based" enrollmentType
    // TODO: Make this async with collectionId validation
    if (StringUtils.equalsIgnoreCase("Entrance Exam Based", enrollmentType)) {
      validateEntranceExamRequiredForFramework(nodeData)
    }

    // Validate certificate
    val certificate = nodeData.get("certificate") match {
      case map: util.Map[String, AnyRef] => map
      case null => null
      case _ => null // If certificate exists but is not a Map, treat as null
    }
    if (certificate != null) {
      val certificateEnabled = certificate.getOrDefault("enabled", "").asInstanceOf[String]
      if (StringUtils.isNotEmpty(certificateEnabled) && !StringUtils.equalsAnyIgnoreCase(certificateEnabled, "Yes", "No")) {
        throw new ClientException("ERR_COMPETENCY_FRAMEWORK_VALIDATION", "Competency Framework certificate enabled must be 'Yes' or 'No'")
      }
    }
    
    Future.successful(())
  }

  private def validateCompetencyLevelNode(nodeData: util.Map[String, AnyRef], errors: ListBuffer[String], parentNode: Node)
                                 (implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Unit] = {
    val validationFutures = ListBuffer[Future[Unit]]()
    
    // Validate name is required
    val name = nodeData.getOrDefault("name", "").asInstanceOf[String]
    if (StringUtils.isEmpty(name)) {
      throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level name is required")
    }

    // Validate visibility
    val visibility = nodeData.getOrDefault("visibility", "").asInstanceOf[String]
    if (!StringUtils.equalsIgnoreCase("Parent", visibility)) {
      throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level visibility must be 'Parent'")
    }

    // Validate timeLimit
    val timeLimit = nodeData.get("timeLimit") match {
      case map: util.Map[String, AnyRef] => map
      case null => null
      case _ => null // If timeLimit exists but is not a Map, treat as null
    }
    if (timeLimit != null) {
      val timeLimitEnabled = timeLimit.getOrDefault("enabled", "").asInstanceOf[String]
      if (StringUtils.isNotEmpty(timeLimitEnabled) && !StringUtils.equalsAnyIgnoreCase(timeLimitEnabled, "Yes", "No")) {
        throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level timeLimit enabled must be 'Yes' or 'No'")
      }
      
      if (StringUtils.equalsIgnoreCase("Yes", timeLimitEnabled)) {
        val duration = timeLimit.get("duration") match {
          case map: util.Map[String, AnyRef] => map
          case null => null
          case _ => null // If duration exists but is not a Map, treat as null
        }
        if (duration == null) {
          throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level timeLimit duration is required when enabled")
        } else {
          val durationValue = duration.get("value")
          val durationUnit = duration.getOrDefault("unit", "").asInstanceOf[String]
          
          if (durationValue == null) {
            throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level timeLimit duration value is required when enabled")
          }
          
          val value = durationValue match {
            case i: Integer => i.intValue()
            case d: java.lang.Double => d.intValue()
            case s: String => try { s.toInt } catch { case _: NumberFormatException => 0 }
            case _ => 0
          }
          
          if (value < 1) {
            throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level timeLimit duration value must be at least 1")
          }
          
          if (StringUtils.isNotEmpty(durationUnit) && !StringUtils.equalsAnyIgnoreCase(durationUnit, "Days", "Months", "Years")) {
            throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level timeLimit duration unit must be 'Days', 'Months', or 'Years'")
          }
        }
      }
    }

    // Validate entranceExam
    val entranceExam = nodeData.get("entranceExam") match {
      case map: util.Map[String, AnyRef] => map
      case null => null
      case _ => null // If entranceExam exists but is not a Map, treat as null
    }
    if (entranceExam != null) {
      val entranceExamEnabled = entranceExam.getOrDefault("enabled", "").asInstanceOf[String]
      if (StringUtils.isNotEmpty(entranceExamEnabled) && !StringUtils.equalsAnyIgnoreCase(entranceExamEnabled, "Yes", "No")) {
        throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level entranceExam enabled must be 'Yes' or 'No'")
      }
      
      if (StringUtils.equalsIgnoreCase("Yes", entranceExamEnabled)) {
        val collectionId = entranceExam.getOrDefault("collectionId", "").asInstanceOf[String]
        if (StringUtils.isEmpty(collectionId)) {
          throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level entranceExam collectionId is required when enabled")
        }
        // Add async validation for entranceExam collectionId
        val entranceExamValidation = validateCourseExists(collectionId, "entranceExam", errors)(oec, ec, parentNode)
        validationFutures += entranceExamValidation
      }
    }

    // Validate levelExam
    val levelExam = nodeData.get("levelExam") match {
      case map: util.Map[String, AnyRef] => map
      case null => null
      case _ => null // If levelExam exists but is not a Map, treat as null
    }
    if (levelExam != null) {
      val levelExamCollectionId = levelExam.getOrDefault("collectionId", "").asInstanceOf[String]
      val passingCriteria = levelExam.get("passingCriteria") match {
        case map: util.Map[String, AnyRef] => map
        case null => null
        case _ => null // If passingCriteria exists but is not a Map, treat as null
      }
      
      // If collectionId is provided, passingCriteria must be provided
      if (StringUtils.isNotEmpty(levelExamCollectionId)) {
        if (passingCriteria == null) {
          throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level levelExam passingCriteria is required when collectionId is provided")
        } else {
          val mustPass = passingCriteria.getOrDefault("mustPass", "").asInstanceOf[String]
          if (StringUtils.isNotEmpty(mustPass) && !StringUtils.equalsAnyIgnoreCase(mustPass, "Yes", "No")) {
            throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level levelExam passingCriteria mustPass must be 'Yes' or 'No'")
          }
        }
        // Add async validation for levelExam collectionId
        val levelExamValidation = validateCourseExists(levelExamCollectionId, "levelExam", errors)(oec, ec, parentNode)
        validationFutures += levelExamValidation
      }
      
      // If passingCriteria is provided, collectionId must be provided
      if (passingCriteria != null && StringUtils.isEmpty(levelExamCollectionId)) {
        throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level levelExam collectionId is required when passingCriteria is provided")
      }
    }

    // Validate certificate
    val certificate = nodeData.get("certificate") match {
      case map: util.Map[String, AnyRef] => map
      case null => null
      case _ => null // If certificate exists but is not a Map, treat as null
    }
    if (certificate != null) {
      val certificateEnabled = certificate.getOrDefault("enabled", "").asInstanceOf[String]
      if (StringUtils.isNotEmpty(certificateEnabled) && !StringUtils.equalsAnyIgnoreCase(certificateEnabled, "Yes", "No")) {
        throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level certificate enabled must be 'Yes' or 'No'")
      }
    }
    
    // Wait for all async validations to complete
    Future.sequence(validationFutures.toList).map(_ => ())
  }

  private def validateEntranceExamRequiredForFramework(nodeData: util.Map[String, AnyRef]): Unit = {
    // For enrollmentType "Entrance Exam Based", check that all Competency Level children have entranceExam enabled with collectionId
    val children = nodeData.getOrDefault("children", new util.ArrayList[util.Map[String, AnyRef]]()).asInstanceOf[util.List[util.Map[String, AnyRef]]]
    val competencyLevels = children.asScala.filter(child => {
      val childPrimaryCategory = child.getOrDefault("primaryCategory", "").asInstanceOf[String]
      StringUtils.equalsIgnoreCase("Competency Level", childPrimaryCategory)
    })
    
    if (competencyLevels.isEmpty) {
      throw new ClientException("ERR_COMPETENCY_FRAMEWORK_VALIDATION", "Competency Framework with enrollmentType 'Entrance Exam Based' must have at least one Competency Level")
    }
    
    competencyLevels.foreach(child => {
      val entranceExam = child.get("entranceExam") match {
        case map: util.Map[String, AnyRef] => map
        case null => null
        case _ => null // If entranceExam exists but is not a Map, treat as null
      }
      if (entranceExam == null) {
        throw new ClientException("ERR_COMPETENCY_FRAMEWORK_VALIDATION", "Competency Framework with enrollmentType 'Entrance Exam Based' requires all Competency Levels to have entranceExam enabled with collectionId provided")
      } else {
        val entranceExamEnabled = entranceExam.getOrDefault("enabled", "").asInstanceOf[String]
        val collectionId = entranceExam.getOrDefault("collectionId", "").asInstanceOf[String]
        if (!StringUtils.equalsIgnoreCase("Yes", entranceExamEnabled) || StringUtils.isEmpty(collectionId)) {
          throw new ClientException("ERR_COMPETENCY_FRAMEWORK_VALIDATION", "Competency Framework with enrollmentType 'Entrance Exam Based' requires all Competency Levels to have entranceExam enabled with collectionId provided")
        }
      }
    })
  }

  // Validate that the given collectionId exists and is of contentType=Course and status=Live.
  def validateCourseExists(collectionId: String, fieldName: String, errors: ListBuffer[String])
                          (implicit oec: OntologyEngineContext, ec: ExecutionContext, parentNode: Node): Future[Unit] = {
    if (StringUtils.isBlank(collectionId)) {
      val msg = s"$fieldName collectionId is missing"
      errors += msg
      Future.failed(new ClientException("ERR_BLANK_COLLECTION_ID", msg))
    } else {
      val request = new Request()
      Option(request.getContext).getOrElse {
        val context = new java.util.HashMap[String, AnyRef]()
        request.setContext(context)
        context
      }

      val parentMetadata = parentNode.getMetadata.asScala
      val graphId = parentMetadata.getOrElse("graphId", parentNode.getGraphId).toString.toLowerCase
      val channel = parentMetadata.getOrElse("channel", "").toString.toLowerCase
      val objectType = parentMetadata.getOrElse("objectType", "").toString.toLowerCase

      val contextMap = Map[String, AnyRef](
        "identifier" -> collectionId,
        "graph_id" -> graphId,
        "channel" -> channel,
        "schemaName" -> objectType,
        "version" -> "1.0",
        "objectType" -> objectType
      )
      contextMap.foreach { case (k, v) => request.getContext.put(k, v) }
      request.put("identifier", collectionId)
      request.put("objectType", objectType)

      DataNode.read(request).flatMap { node =>
        if (node == null) {
          val msg = s"$fieldName collectionId $collectionId not found"
          errors += msg
          Future.failed(new ClientException("ERR_COLLECTION_NOT_FOUND", msg))
        } else {
          val metadata = node.getMetadata.asScala
          val status = metadata.getOrElse("status", "").toString
          val contentType = metadata.getOrElse("contentType", "").toString

          if (!"Course".equalsIgnoreCase(contentType)) {
            val msg = s"$fieldName collectionId $collectionId has invalid contentType: $contentType (expected Course)"
            errors += msg
            Future.failed(new ClientException("ERR_INVALID_CONTENT_TYPE", msg))
          } else if (!"Live".equalsIgnoreCase(status)) {
            val msg = s"$fieldName collectionId $collectionId has invalid status: $status (expected Live)"
            errors += msg
            Future.failed(new ClientException("ERR_INVALID_STATUS", msg))
          } else {
            Future.unit
          }
        }
      }
    }
  }
}