package org.sunbird.content.util

import org.apache.commons.lang3.StringUtils
import org.sunbird.common.JsonUtils
import org.sunbird.common.dto.Request
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.managers.HierarchyManager

import java.util
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

object CompetencyFrameworkValidator {

  def validateCompetencyFramework(request: Request, node: Node)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Unit] = {
    val primaryCategory = node.getMetadata.getOrDefault("primaryCategory", "").asInstanceOf[String]
    
    if (StringUtils.equalsIgnoreCase("Competency Framework", primaryCategory)) {
      // Get hierarchy for validation
      val hierarchyRequest = new Request(request)
      hierarchyRequest.put("rootId", node.getIdentifier)
      hierarchyRequest.put("mode", "edit")
      
      HierarchyManager.getHierarchy(hierarchyRequest).map { response =>
        if (response.getResponseCode.code() != 200) {
          throw new ClientException("ERR_HIERARCHY_NOT_FOUND", "Unable to fetch hierarchy for validation")
        }
        
        val hierarchyData = response.getResult.get("content").asInstanceOf[util.Map[String, AnyRef]]
        validateHierarchyRecursive(hierarchyData)
      }
    } else {
      Future.successful(())
    }
  }

  private def validateHierarchyRecursive(nodeData: util.Map[String, AnyRef]): Unit = {
    val primaryCategory = nodeData.getOrDefault("primaryCategory", "").asInstanceOf[String]
    
    if (StringUtils.equalsIgnoreCase("Competency Framework", primaryCategory)) {
      validateCompetencyFrameworkNode(nodeData)
    } else if (StringUtils.equalsIgnoreCase("Competency Level", primaryCategory)) {
      validateCompetencyLevelNode(nodeData)
    }
    
    // Recursively validate children
    val children = nodeData.getOrDefault("children", new util.ArrayList[util.Map[String, AnyRef]]()).asInstanceOf[util.List[util.Map[String, AnyRef]]]
    children.asScala.foreach(child => validateHierarchyRecursive(child))
  }

  private def validateCompetencyFrameworkNode(nodeData: util.Map[String, AnyRef]): Unit = {
    // Validate visibility
    val visibility = nodeData.getOrDefault("visibility", "").asInstanceOf[String]
    if (!StringUtils.equalsIgnoreCase("Private", visibility)) {
      throw new ClientException("ERR_COMPETENCY_FRAMEWORK_VALIDATION", "Competency Framework visibility must be 'Private'")
    }

    // Validate sector
    val sector = nodeData.get("sector").asInstanceOf[util.Map[String, AnyRef]]
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
    if (StringUtils.equalsIgnoreCase("Entrance Exam Based", enrollmentType)) {
      validateEntranceExamRequiredForFramework(nodeData)
    }

    // Validate certificate
    val certificate = nodeData.get("certificate").asInstanceOf[util.Map[String, AnyRef]]
    if (certificate != null) {
      val certificateEnabled = certificate.getOrDefault("enabled", "").asInstanceOf[String]
      if (StringUtils.isNotEmpty(certificateEnabled) && !StringUtils.equalsAnyIgnoreCase(certificateEnabled, "Yes", "No")) {
        throw new ClientException("ERR_COMPETENCY_FRAMEWORK_VALIDATION", "Competency Framework certificate enabled must be 'Yes' or 'No'")
      }
    }
  }

  private def validateCompetencyLevelNode(nodeData: util.Map[String, AnyRef]): Unit = {
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
    val timeLimit = nodeData.get("timeLimit").asInstanceOf[util.Map[String, AnyRef]]
    if (timeLimit != null) {
      val timeLimitEnabled = timeLimit.getOrDefault("enabled", "").asInstanceOf[String]
      if (StringUtils.isNotEmpty(timeLimitEnabled) && !StringUtils.equalsAnyIgnoreCase(timeLimitEnabled, "Yes", "No")) {
        throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level timeLimit enabled must be 'Yes' or 'No'")
      }
      
      if (StringUtils.equalsIgnoreCase("Yes", timeLimitEnabled)) {
        val duration = timeLimit.get("duration").asInstanceOf[util.Map[String, AnyRef]]
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
    val entranceExam = nodeData.get("entranceExam").asInstanceOf[util.Map[String, AnyRef]]
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
      }
    }

    // Validate levelExam
    val levelExam = nodeData.get("levelExam").asInstanceOf[util.Map[String, AnyRef]]
    if (levelExam != null) {
      val levelExamCollectionId = levelExam.getOrDefault("collectionId", "").asInstanceOf[String]
      val passingCriteria = levelExam.get("passingCriteria").asInstanceOf[util.Map[String, AnyRef]]
      
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
      }
      
      // If passingCriteria is provided, collectionId must be provided
      if (passingCriteria != null && StringUtils.isEmpty(levelExamCollectionId)) {
        throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level levelExam collectionId is required when passingCriteria is provided")
      }
    }

    // Validate certificate
    val certificate = nodeData.get("certificate").asInstanceOf[util.Map[String, AnyRef]]
    if (certificate != null) {
      val certificateEnabled = certificate.getOrDefault("enabled", "").asInstanceOf[String]
      if (StringUtils.isNotEmpty(certificateEnabled) && !StringUtils.equalsAnyIgnoreCase(certificateEnabled, "Yes", "No")) {
        throw new ClientException("ERR_COMPETENCY_LEVEL_VALIDATION", "Competency Level certificate enabled must be 'Yes' or 'No'")
      }
    }
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
      val entranceExam = child.get("entranceExam").asInstanceOf[util.Map[String, AnyRef]]
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
}