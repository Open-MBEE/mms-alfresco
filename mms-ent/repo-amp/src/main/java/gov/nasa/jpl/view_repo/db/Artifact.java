package gov.nasa.jpl.view_repo.db;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;


public class Artifact {

    private int id;
    private String elasticId;
    private String contentType;
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

    public Artifact(int id, String elasticId, String sysmlId, String lastCommit, String initialCommit,
        boolean isDeleted) {
        super();
        this.id = id;
        this.elasticId = elasticId;
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

    public String getNodeType() {
        return contentType;
    }

    public void setNodeType(int nodeType) {
        this.contentType = contentType;
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

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("sysmlId", this.sysmlId);
        json.put("elasticId", this.elasticId);
        json.put("nodeType", this.contentType);

        return json;
    }
}
