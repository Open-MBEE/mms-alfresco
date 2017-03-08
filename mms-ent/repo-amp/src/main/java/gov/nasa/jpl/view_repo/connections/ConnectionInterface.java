package gov.nasa.jpl.view_repo.connections;

import org.json.JSONObject;

public interface ConnectionInterface {
    public void setWorkspace(String workspace);
    
    public void setProjectId(String projectId);
    
    public boolean publish(JSONObject jsonObject, String eventType, String workspaceId, String projectId);
    
    public JSONObject toJson();
    
    public void ingestJson(JSONObject json);
}
