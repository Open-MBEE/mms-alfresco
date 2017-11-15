package gov.nasa.jpl.view_repo.connections;

import gov.nasa.jpl.view_repo.util.SerialJSONObject;

public interface ConnectionInterface {
    void setRefId(String refId);

    void setProjectId(String projectId);

    boolean publish(SerialJSONObject jsonObject, String eventType, String workspaceId, String projectId);

    SerialJSONObject toJson();

    void ingestJson(SerialJSONObject json);
}
