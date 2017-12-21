package gov.nasa.jpl.view_repo.connections;

//import org.json.JSONObject;
import gov.nasa.jpl.view_repo.util.JSONObject;
//import gov.nasa.jpl.view_repo.util.JSONArray;

public interface ConnectionInterface {
    void setRefId(String refId);

    void setProjectId(String projectId);

    boolean publish(JSONObject jsonObject, String eventType, String workspaceId, String projectId);

    JSONObject toJson();

    void ingestJson(JSONObject json);
}
