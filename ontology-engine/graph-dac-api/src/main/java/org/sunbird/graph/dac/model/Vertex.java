package org.sunbird.graph.dac.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.graph.common.enums.SystemProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Vertex implements Serializable {

    private static final long serialVersionUID = 252337826576516976L;

    private long id;
    private String graphId;
    private String identifier;
    private String nodeType;
    private String objectType;
    private Map<String, Object> metadata;
    private List<Edges> outRelations;
    private List<Edges> inRelations;
    private List<Edges> addedRelations;
    private List<Edges> deletedRelations;
    private Map<String, Vertex> relationNodes;
    private Map<String, Object> externalData;

    public Vertex() {
        addedRelations = new ArrayList<>();
        deletedRelations = new ArrayList<>();
    }

    public Vertex(String identifier, String nodeType, String objectType) {
        this.identifier = identifier;
        this.nodeType = nodeType;
        this.objectType = objectType;
        addedRelations = new ArrayList<>();
        deletedRelations = new ArrayList<>();
    }

    public Vertex(String graphId, Map<String, Object> metadata) {
        this.graphId = graphId;
        this.metadata = metadata;
        if (null != metadata && !metadata.isEmpty()) {
            if (null != metadata.get(SystemProperties.IL_UNIQUE_ID.name()))
                this.identifier = metadata.get(SystemProperties.IL_UNIQUE_ID.name()).toString();
            if (null != metadata.get(SystemProperties.IL_SYS_NODE_TYPE.name()))
                this.nodeType = metadata.get(SystemProperties.IL_SYS_NODE_TYPE.name()).toString();
            if (null != metadata.get(SystemProperties.IL_FUNC_OBJECT_TYPE.name()))
                this.objectType = metadata.get(SystemProperties.IL_FUNC_OBJECT_TYPE.name()).toString();
        }
        addedRelations = new ArrayList<>();
        deletedRelations = new ArrayList<>();
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @JsonIgnore
    public String getGraphId() {
        return graphId;
    }

    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }

    public String getIdentifier() {
        if (StringUtils.isBlank(identifier) && null != metadata)
            this.identifier = (String) metadata.get(SystemProperties.IL_UNIQUE_ID.name());
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getNodeType() {
        if (StringUtils.isBlank(nodeType) && null != metadata)
            this.nodeType = (String) metadata.get(SystemProperties.IL_SYS_NODE_TYPE.name());
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getObjectType() {
        if (StringUtils.isBlank(objectType) && null != metadata)
            this.objectType = (String) metadata.get(SystemProperties.IL_FUNC_OBJECT_TYPE.name());
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<Edges> getOutRelations() {
        if (!CollectionUtils.isEmpty(outRelations))
            return outRelations;
        else return new ArrayList<>();
    }

    public void setOutRelations(List<Edges> outRelations) {
        this.outRelations = outRelations;
    }

    public List<Edges> getInRelations() {
        if (!CollectionUtils.isEmpty(inRelations))
            return inRelations;
        else return new ArrayList<>();
    }

    public void setInRelations(List<Edges> inRelations) {
        this.inRelations = inRelations;
    }

    public List<Edges> getAddedRelations() {
        return addedRelations;
    }

    public void setAddedRelations(List<Edges> addedRelations) {
        if(CollectionUtils.isEmpty(this.addedRelations))
            this.addedRelations = new ArrayList<>();
        this.addedRelations.addAll(addedRelations);
    }

    public List<Edges> getDeletedRelations() {
        return deletedRelations;
    }

    public void setDeletedRelations(List<Edges> deletedRelations) {
        this.deletedRelations = deletedRelations;
    }

    public Map<String, Object> getExternalData() {
        return externalData;
    }

    public Map<String, Vertex> getRelationNodes() {
        return relationNodes;
    }

    public void setRelationNodes(Map<String, Vertex> relationNodes) {
        this.relationNodes = relationNodes;
    }

    public void setExternalData(Map<String, Object> externalData) {
        this.externalData = externalData;
    }

    public Vertex getNode() {
        return (Vertex) this;
    }

    public Vertex getRelationNode(String identifier) {
        return relationNodes.get(identifier);
    }

    public String getArtifactUrl() {
        return (String) this.metadata.getOrDefault("artifactUrl", "");
    }

}
