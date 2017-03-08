package gov.nasa.jpl.view_repo.util;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.json.JSONObject;
import org.json.JSONArray;

import gov.nasa.jpl.view_repo.util.EmsNodeUtil;

public class EmsNode {

    private String sysmlid;
    private String projectId;
    private String workspaceId;
    private JSONObject json;
    private EmsNodeUtil emsNodeUtil;
    private Map<String, Object> properties = new HashMap<>();

    public EmsNode(String sysmlId, String projectId) {
        new EmsNode(sysmlId, projectId, "master");
    }

    public EmsNode(String sysmlid, String projectId, String workspaceId) {
        this.sysmlid = sysmlid;
        this.projectId = projectId;
        this.workspaceId = workspaceId;
        emsNodeUtil = new EmsNodeUtil(projectId, workspaceId);
        loadEmsNode(sysmlid);
    }

    public String getSysmlId() {
        return this.sysmlid;
    }

    public Object getProperty(String key) {
        try {
            Class<?> classType = Class.forName(properties.get(key).getClass().getName());
            return classType.cast(properties.get(key));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public EmsNode getParentNode() {
        if (this.properties.get(Sjm.OWNERID) != null) {
            return new EmsNode((String) this.properties.get(Sjm.OWNERID), this.projectId, this.workspaceId);
        }
        return null;
    }

    public List<EmsNode> getChildNodes() {
        return getChildNodes(100000L);
    }

    public List<EmsNode> getChildNodes(Long depth) {
        List<EmsNode> children = new ArrayList<>();
        JSONArray childrenJson = emsNodeUtil.getChildren(this.sysmlid, depth);
        for (int i = 0; i < childrenJson.length(); i++) {
            String childId = childrenJson.getString(i);
            children.add(new EmsNode(childId, this.workspaceId));
        }
        return children;
    }

    public JSONArray getChildren() {
        return getChildren(this.sysmlid, 100000L);
    }

    public JSONArray getChildren(String sysmlid, Long depth) {
        return emsNodeUtil.getChildren(sysmlid, depth);
    }

    public Boolean exists() {
        return this.properties != null && !this.properties.isEmpty();
    }

    public Boolean isOwnedValueSpec() {
        return isOwnedValueSpec(this.sysmlid);
    }

    // TODO: finish this
    public Boolean isOwnedValueSpec(String sysmlid) {
        return true;
    }

    private void loadEmsNode(String sysmlid) {
        this.json = emsNodeUtil.getNodeBySysmlid(sysmlid);
        this.properties = EmsNodeUtil.jsonToMap(this.json);
    }

}
