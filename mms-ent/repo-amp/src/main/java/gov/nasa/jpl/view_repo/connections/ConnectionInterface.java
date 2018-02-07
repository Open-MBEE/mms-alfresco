package gov.nasa.jpl.view_repo.connections;

import com.google.gson.JsonObject;

public interface ConnectionInterface {
    void setRefId(String refId);

    void setProjectId(String projectId);

    boolean publish(JsonObject jsonObject, String eventType, String workspaceId, String projectId);

    JsonObject toJson();

    void ingestJson(JsonObject json);
}
