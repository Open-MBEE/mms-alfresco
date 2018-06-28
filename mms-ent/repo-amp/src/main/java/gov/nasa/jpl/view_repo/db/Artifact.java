package gov.nasa.jpl.view_repo.db;

import com.google.gson.JsonObject;

public class Artifact {

    private int id;
    private String elasticId;
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

        return json;
    }
}
