package gov.nasa.jpl.view_repo.db;

import org.json.JSONObject;

public class Node {

    private int id;
    private String elasticId;
    private int nodeType;
    private String sysmlId;

    public String getSysmlId() {
        return sysmlId;
    }
    public void setSysmlId(String sysmlId) {
        this.sysmlId = sysmlId;
    }

    public Node(int id, String elasticId, int nodeType, String sysmlId) {
        super();
        this.id = id;
        this.elasticId = elasticId;
        this.nodeType = nodeType;
        this.sysmlId = sysmlId;
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

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("sysmlId", this.sysmlId);
        json.put("elasticId", this.elasticId);
        json.put("nodeType", this.nodeType);

        return json;
    }
}
