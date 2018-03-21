package gov.nasa.jpl.view_repo.db;

import com.google.gson.JsonObject;

public class Node {

    private int id;
    private String elasticId;
    private int nodeType;
    private String sysmlId;
    private String lastCommit;
    private String initialCommit;
    private boolean isDeleted;

    public String getSysmlId() {
        return sysmlId;
    }

    public void setSysmlId(String sysmlId) {
        this.sysmlId = sysmlId;
    }

    public Node(int id, String elasticId, int nodeType, String sysmlId, String lastCommit, String initialCommit,
        boolean isDeleted) {
        super();
        this.id = id;
        this.elasticId = elasticId;
        this.nodeType = nodeType;
        this.sysmlId = sysmlId;
        this.lastCommit = lastCommit;
        this.initialCommit = initialCommit;
        this.isDeleted = isDeleted;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getElasticId() {
        return elasticId;
    }

    public void setNodeRefId(String elasticId) {
        this.elasticId = elasticId;
    }

    public int getNodeType() {
        return nodeType;
    }

    public void setNodeType(int nodeType) {
        this.nodeType = nodeType;
    }

    public String getInitialCommit() {
        return initialCommit;
    }

    public String getLastCommit() {
        return lastCommit;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", this.id);
        json.addProperty("sysmlId", this.sysmlId);
        json.addProperty("elasticId", this.elasticId);
        json.addProperty("nodeType", this.nodeType);

        return json;
    }
}
