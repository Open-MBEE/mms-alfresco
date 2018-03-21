package gov.nasa.jpl.view_repo.db;

import java.util.Map;
import com.google.gson.JsonObject;

import gov.nasa.jpl.view_repo.util.JsonUtil;

public class Edge {

    private int parent;
    private int child;
    private int edgeType;
    private Map<String, String> properties;
    
    public Edge(int parent, int child, int edgeType) {
        super();
        this.parent = parent;
        this.child = child;
        this.edgeType = edgeType;
    }
    public int getParent() {
        return parent;
    }
    public void setParent(int parent) {
        this.parent = parent;
    }
    public int getChild() {
        return child;
    }
    public void setChild(int child) {
        this.child = child;
    }
    public int getEdgeType() {
        return edgeType;
    }
    public void setEdgeType(int edgeType) {
        this.edgeType = edgeType;
    }
    
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("parent", this.parent);
        json.addProperty("child", this.child);
        json.addProperty("edgeType", this.edgeType);
        json.add("properties", JsonUtil.fromMap(this.properties));

        return json;
    }
    
}
