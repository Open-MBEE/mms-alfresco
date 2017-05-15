package gov.nasa.jpl.view_repo.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.ElasticResult;
import gov.nasa.jpl.view_repo.db.Node;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.db.PostgresHelper.DbCommitTypes;
import gov.nasa.jpl.view_repo.db.PostgresHelper.DbEdgeTypes;
import gov.nasa.jpl.view_repo.db.PostgresHelper.DbNodeTypes;

public class EmsNodeUtil {

    private ElasticHelper eh = null;
    private PostgresHelper pgh = null;
    private String projectId = null;
    private String workspaceName = "master";
    private static Logger logger = Logger.getLogger(EmsNodeUtil.class);

    public EmsNodeUtil() {
        try {
            eh = new ElasticHelper();
            pgh = new PostgresHelper();
            switchWorkspace("master");
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public EmsNodeUtil(String projectId, WorkspaceNode workspace) {
        try {
            eh = new ElasticHelper();
            pgh = new PostgresHelper();
            switchProject(projectId);
            switchWorkspace(workspace);
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public EmsNodeUtil(String projectId, String workspaceName) {
        try {
            eh = new ElasticHelper();
            pgh = new PostgresHelper();
            switchProject(projectId);
            switchWorkspace(workspaceName);
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public void switchWorkspace(WorkspaceNode workspace) {
        String workspaceName = workspace == null ? "" : workspace.getId();
        switchWorkspace(workspaceName);
    }

    private void switchWorkspace(String workspaceName) {
        if (!this.workspaceName.equals(workspaceName) && pgh != null) {
            if (workspaceName.equals("null")) {
                workspaceName = "";
            }
            this.workspaceName = workspaceName;
            pgh.setWorkspace(workspaceName);
        }
    }

    public void switchProject(String projectId) {
        if (projectId != null && (this.projectId == null || !this.projectId.equals(projectId)) && pgh != null) {
            this.projectId = projectId;
            pgh.setProject(projectId);
        }
    }

    public JSONArray getOrganization(String orgId) {
        JSONArray orgs = new JSONArray();
        List<Map<String, String>> organizations = pgh.getOrganizations(orgId);
        organizations.forEach((n) -> {
            JSONObject org = new JSONObject();
            org.put(Sjm.SYSMLID, n.get("orgId"));
            org.put(Sjm.NAME, n.get("orgName"));
            orgs.put(org);
        });

        return orgs;
    }

    public String getOrganizationFromProject(String projectId) {
        return pgh.getOrganizationFromProject(projectId);
    }

    public JSONArray getProjects(String orgId) {
        JSONArray projects = new JSONArray();
        List<Map<String, Object>> orgProjects = pgh.getProjects(orgId);
        orgProjects.forEach((n) -> {
            switchProject(n.get(Sjm.SYSMLID).toString());
            JSONObject project = getNodeBySysmlid(n.get(Sjm.SYSMLID).toString());
            project.put("orgId", orgId);
            projects.put(project);
        });

        return projects;
    }

    public JSONArray getProjects() {
        JSONArray projects = new JSONArray();
        pgh.getProjects().forEach((project) -> {
            switchProject(project.get(Sjm.SYSMLID).toString());
            JSONObject proj = getNodeBySysmlid(project.get(Sjm.SYSMLID).toString());
            proj.put("orgId", project.get("orgId").toString());
            projects.put(proj);
        });

        return projects;
    }

    public JSONObject getProject(String projectId) {
        Map<String, Object> project = pgh.getProject(projectId);
        if (!project.isEmpty() && !project.get(Sjm.SYSMLID).toString().contains("no_project")) {
            switchProject(projectId);
            JSONObject proj = getNodeBySysmlid(projectId);
            proj.put("orgId", project.get("orgId").toString());
            return proj;
        }
        return null;
    }

    public JSONObject getProjectWithFullMounts(String projectId, String refId, List<String> found) {
        List<String> realFound = found;
        if (realFound == null) {
            realFound = new ArrayList<>();
        }
        Map<String, Object> project = pgh.getProject(projectId);

        if (!project.isEmpty() && !project.get(Sjm.SYSMLID).toString().contains("no_project")) {
            switchProject(projectId);
            switchWorkspace(refId);
            JSONObject projectJson = getNodeBySysmlid(projectId);
            projectJson.put("orgId", project.get("orgId").toString());
            realFound.add(projectId);
            JSONArray mountObject = getFullMounts(projectJson.get(Sjm.SYSMLID).toString(), realFound);
            projectJson.put(Sjm.MOUNTS, mountObject);
            return projectJson;
        }
        return null;
    }

    public JSONArray getFullMounts(String projectId, List<String> found) {
        JSONArray mounts = new JSONArray();
        String curProjectId = this.projectId;
        String curRefId = this.workspaceName;
        List<Node> nodes = pgh.getNodesByType(DbNodeTypes.MOUNT);
        if (nodes.isEmpty()) {
            return mounts;
        }
        Set<String> mountIds = new HashSet<String>();
        for (int i = 0; i < nodes.size(); i++) {
            mountIds.add(nodes.get(i).getSysmlId());
        }
        JSONArray nodeList = getNodesBySysmlids(mountIds);
        for (int i = 0; i < nodeList.length(); i++) {
            JSONObject mountJson = nodeList.getJSONObject(i);
            if (mountJson.has(Sjm.MOUNTEDELEMENTPROJECTID) && mountJson.has("refId")) {
                if (found.contains(mountJson.getString(Sjm.MOUNTEDELEMENTPROJECTID))) {
                    continue;
                }
                JSONObject childProject = getProjectWithFullMounts(mountJson.getString(Sjm.MOUNTEDELEMENTPROJECTID),
                    mountJson.getString("refId"), found);
                if (childProject != null) {
                    mounts.put(childProject);
                }
            }
        }
        switchProject(curProjectId);
        switchWorkspace(curRefId);
        return mounts;
    }

    public JSONObject getElementByElementAndCommitId(String commitId, String sysmlid) {
        try {
            return eh.getElementByCommitId(commitId, sysmlid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public Node getById(String sysmlId) {
        return pgh.getNodeFromSysmlId(sysmlId);
    }

    public Boolean commitContainsElement(String elementId, String commitId) {
        try {
            return eh.checkForElasticIdInCommit(elementId, commitId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public JSONObject getNodeBySysmlid(String sysmlid) {
        return getNodeBySysmlid(sysmlid, this.workspaceName);
    }

    private JSONObject getNodeBySysmlid(String sysmlid, String workspaceName) {
        return getNodeBySysmlid(sysmlid, workspaceName, null, true);
    }

    private JSONObject getNodeBySysmlid(String sysmlid, boolean withChildViews) {
        return getNodeBySysmlid(sysmlid, workspaceName, null, withChildViews);
    }

    /**
     * Retrieves node by sysmlid adding childViews as necessary
     *
     * @param sysmlid       String of sysmlid to look up
     * @param workspaceName Workspace to retrieve id against
     * @param visited       Map of visited sysmlids when traversing to unravel childViews
     * @return
     */

    private JSONObject getNodeBySysmlid(String sysmlid, String workspaceName, Map<String, JSONObject> visited,
        boolean withChildViews) {
        if (!this.workspaceName.equals(workspaceName)) {
            switchWorkspace(workspaceName);
        }

        if (visited == null) {
            visited = new HashMap<>();
        }
        if (visited.containsKey(sysmlid)) {
            return addChildViews(visited.get(sysmlid), visited);
        }

        String elasticId = pgh.getElasticIdFromSysmlId(sysmlid);
        if (elasticId != null) {
            try {
                JSONObject result = eh.getElementByElasticId(elasticId);
                if (result != null) {
                    result.put(Sjm.PROJECTID, this.projectId);
                    result.put(Sjm.REFID, this.workspaceName);
                    visited.put(sysmlid, result);
                    return withChildViews ? addChildViews(result, visited) : result;
                }
            } catch (Exception e) {
                logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }
        return new JSONObject();
    }


    public JSONArray getNodesBySysmlids(Set<String> sysmlids) {
        List<String> elasticids = pgh.getElasticIdsFromSysmlIds(new ArrayList<>(sysmlids));
        JSONArray elementsFromElastic = new JSONArray();
        try {
            elementsFromElastic = eh.getElementsFromElasticIds(elasticids);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < elementsFromElastic.length(); i++) {
            JSONObject formatted = elementsFromElastic.getJSONObject(i);
            formatted.put(Sjm.PROJECTID, this.projectId);
            formatted.put(Sjm.REFID, this.workspaceName);
            elementsFromElastic.put(i, addChildViews(formatted));
        }

        return elementsFromElastic;
    }

    public JSONArray getNodeHistory(String sysmlId) {
        JSONArray nodeHistory = new JSONArray();
        try {
            nodeHistory = filterCommitsByRefs(eh.getCommitHistory(sysmlId));
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return nodeHistory;
    }

    public JSONArray getRefHistory(String refId) {
        JSONArray result = new JSONArray();

        pgh.getRefsCommits(refId).forEach((refCommit) -> {
            JSONObject commit = new JSONObject();
            commit.put(Sjm.SYSMLID, refCommit.get(Sjm.SYSMLID));
            commit.put(Sjm.CREATOR, refCommit.get(Sjm.CREATOR));
            commit.put(Sjm.TIMESTAMP, refCommit.get(Sjm.TIMESTAMP));
            result.put(commit);
        });

        return result;
    }

    private JSONArray filterCommitsByRefs(JSONArray commits) {
        JSONArray filtered = new JSONArray();
        JSONArray refHistory = getRefHistory(this.workspaceName);
        List<String> commitList = new ArrayList<>();
        for (int i = 0; i < refHistory.length(); i++) {
            commitList.add(refHistory.getJSONObject(i).getString(Sjm.SYSMLID));
        }
        for (int i = 0; i < commits.length(); i++) {
            if (commitList.contains(commits.getJSONObject(i).getString(Sjm.SYSMLID))) {
                filtered.put(commits.getJSONObject(i));
            }
        }

        return filtered;
    }

    public JSONObject getElasticElement(String elasticId) {
        JSONObject jObj = null;

        try {
            jObj = eh.getElementByElasticId(elasticId);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jObj;
    }

    public void insertRef(String refId, String refName, String elasticId, boolean isTag) {
        pgh.insertRef(refId, refName, 0, elasticId, isTag);
    }

    public void insertRef(String refId, String refName, int commitId, String elasticId, boolean isTag) {
        pgh.insertRef(refId, refName, commitId, elasticId, isTag);
    }

    public void updateRef(String refId, String refName, String elasticId, boolean isTag) {
        pgh.updateRef(refId, refName, elasticId, isTag);
    }

    public JSONObject getRefJson(String refId) {
        JSONObject jObj = null;
        Pair<String, String> refInfo = pgh.getRefElastic(refId);
        if (refInfo != null) {
            try {
                jObj = eh.getElementByElasticId(refInfo.second);
            } catch (IOException e) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }
        return jObj;
    }

    public JSONArray getRefsJson() {
        JSONArray result = null;
        List<Pair<String, String>> refs = pgh.getRefsElastic();
        List<String> elasticIds = new ArrayList<>();
        refs.forEach((ref) -> {
            elasticIds.add(ref.second);
        });
        try {
            result = eh.getElementsFromElasticIds(elasticIds);
        } catch (IOException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return result;
    }

    public String getHeadCommit() {
        return pgh.getHeadCommitString();
    }

    public JSONArray getChildren(String sysmlid) {
        return getChildren(sysmlid, DbEdgeTypes.CONTAINMENT, null);
    }

    public JSONArray getChildren(String sysmlid, final Long maxDepth) {
        return getChildren(sysmlid, DbEdgeTypes.CONTAINMENT, maxDepth);
    }

    public JSONArray getChildren(String sysmlid, DbEdgeTypes dbEdge, final Long maxDepth) {
        Set<String> children = new HashSet<>();

        int depth = maxDepth == null ? 100000 : maxDepth.intValue();

        pgh.getChildren(sysmlid, dbEdge, depth).forEach((childId) -> {
            children.add(childId.second);
        });

        try {
            List<String> childrenList = new ArrayList<>(children);
            JSONArray childs = eh.getElementsFromElasticIds(childrenList);
            JSONArray result = new JSONArray();
            for (int i = 0; i < childs.length(); i++) {
                JSONObject current = childs.getJSONObject(i);
                current.put(Sjm.PROJECTID, this.projectId);
                current.put(Sjm.REFID, this.workspaceName);
                JSONObject withChildViews = addChildViews(current);
                result.put(withChildViews);
            }
            return result;
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return new JSONArray();
    }

    public JSONObject deleteNode(String sysmlid) {
        JSONObject original = getNodeBySysmlid(sysmlid);
        pgh.deleteEdgesForNode(sysmlid);
        pgh.deleteNode(sysmlid);
        try {
            return original;
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return null;
    }

    public Map<String, String> search(JSONObject query) {
        try {
            return eh.search(query);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    public JSONArray filterResultsByWorkspace(JSONArray elements) {
        JSONArray filtered = new JSONArray();
        if (elements.length() <= 0) {
            return filtered;
        }

        List<String> elasticIds = new ArrayList<>();
        Map<String, JSONObject> elasticMap = new HashMap<>();

        for (int i = 0; i < elements.length(); i++) {
            elasticIds.add(elements.getJSONObject(i).getString(Sjm.ELASTICID));
            elasticMap.put(elements.getJSONObject(i).getString(Sjm.ELASTICID), elements.getJSONObject(i));
        }
        for (String elastic : pgh.filterNodesWithElastic(elasticIds)) {
            filtered.put(elasticMap.get(elastic));
        }
        Map<String, JSONObject> resultMap = getSysmlMap(filtered);

        return addExtraDocs(filtered, resultMap);
    }

    public JSONArray addExtraDocs(JSONArray elements, Map<String, JSONObject> existing) {
        JSONArray results = new JSONArray();
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            String elementSysmlId = element.getString(Sjm.SYSMLID);
            JSONArray properties = new JSONArray();
            JSONArray slots = new JSONArray();
            JSONArray relatedDocuments = new JSONArray();
            Map<String, List<String>> relatedDocumentsMap = new HashMap<>();

            switch (element.getString(Sjm.TYPE)) {
                case "Property":
                    if (existing.containsKey(element.getString(Sjm.OWNERID))) {
                        element = existing.get(element.getString(Sjm.OWNERID));
                    } else {
                        element = getNodeBySysmlid(element.getString(Sjm.OWNERID));
                        existing.put(element.getString(Sjm.SYSMLID), element);
                    }
                case "Slot":
                    JSONObject appliedStereotypeInstance;
                    if (existing.containsKey(element.getString(Sjm.OWNERID))) {
                        appliedStereotypeInstance = existing.get(element.getString(Sjm.OWNERID));
                    } else {
                        appliedStereotypeInstance = getNodeBySysmlid(element.getString(Sjm.OWNERID));
                        existing.put(appliedStereotypeInstance.getString(Sjm.SYSMLID), appliedStereotypeInstance);
                    }
                    if (existing.containsKey(appliedStereotypeInstance.getString(Sjm.OWNERID))) {
                        element = existing.get(appliedStereotypeInstance.getString(Sjm.OWNERID));
                    } else {
                        element = getNodeBySysmlid(appliedStereotypeInstance.getString(Sjm.OWNERID));
                        existing.put(element.getString(Sjm.SYSMLID), element);
                    }
            }

            JSONArray children = getChildren(elementSysmlId, 2L);
            for (int ii = 0; ii < children.length(); ii++) {
                JSONObject child = children.getJSONObject(ii);
                switch (child.getString(Sjm.TYPE)) {
                    case "Property":
                        properties.put(child);
                        if (!element.has(Sjm.PROPERTIES)) {
                            element.put(Sjm.PROPERTIES, properties);
                        }
                    case "Slot":
                        slots.put(child);
                        if (!element.has(Sjm.SLOTS)) {
                            element.put(Sjm.SLOTS, slots);
                        }
                }
            }

            Set<Pair<String, String>> immediateParents =
                pgh.getImmediateParents(elementSysmlId, PostgresHelper.DbEdgeTypes.VIEW);

            for (Pair<String, String> viewParent : immediateParents) {
                for (String rootSysmlId : pgh.getRootParents(viewParent.first, PostgresHelper.DbEdgeTypes.VIEW)) {
                    if (relatedDocumentsMap.containsKey(rootSysmlId)) {
                        relatedDocumentsMap.get(rootSysmlId).add(viewParent.first);
                    } else {
                        List<String> viewParents = new ArrayList<>();
                        viewParents.add(viewParent.first);
                        relatedDocumentsMap.put(rootSysmlId, viewParents);
                    }
                }
            }
            Iterator<Map.Entry<String, List<String>>> it = relatedDocumentsMap.entrySet().iterator();
            it.forEachRemaining((pair) -> {
                JSONArray viewIds = new JSONArray();
                pair.getValue().forEach(viewIds::put);
                JSONObject relatedDocObject = new JSONObject();
                relatedDocObject.put(Sjm.SYSMLID, pair.getKey());
                relatedDocObject.put(Sjm.PARENTVIEWS, viewIds);
                relatedDocObject.put(Sjm.PROJECTID, this.projectId);
                relatedDocObject.put(Sjm.REFID, this.workspaceName);
                relatedDocuments.put(relatedDocObject);
            });
            element.put(Sjm.RELATEDDOCUMENTS, relatedDocuments);

            results.put(addExtendedInformationForElement(element));
        }

        return results;
    }

    /**
     * Get the documents that exist in a site at a specified time
     *
     * @param sysmlId  Site to filter documents against
     * @param commitId Commit ID to look up documents at
     * @return JSONArray of the documents in the site
     */
    public JSONArray getDocJson(String sysmlId, String commitId, boolean extended) {

        JSONArray result = new JSONArray();
        List<Node> docNodes = pgh.getNodesByType(DbNodeTypes.DOCUMENT);
        List<String> docElasticIds = new ArrayList<>();
        Map<String, String> docSysml2Elastic = new HashMap<>();
        docNodes.forEach((node) -> {
            docSysml2Elastic.put(node.getSysmlId(), node.getElasticId());
        });

        List<Pair<String, String>> siteChildren = pgh.getChildren(sysmlId, DbEdgeTypes.CONTAINMENT, 10000);
        Set<String> siteChildrenIds = new HashSet<>();
        siteChildren.forEach((child) -> {
            siteChildrenIds.add(child.first);
        });
        docSysml2Elastic.keySet().forEach((docSysmlId) -> {
            if (siteChildrenIds.contains(docSysmlId)) {
                docElasticIds.add(docSysml2Elastic.get(docSysmlId));
            }
        });

        JSONArray docJson = new JSONArray();
        try {
            docJson = eh.getElementsFromElasticIds(docElasticIds);
        } catch (IOException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }

        if (extended) {
            docJson = addExtendedInformation(docJson);
        }

        for (int i = 0; i < docJson.length(); i++) {
            docJson.getJSONObject(i).put(Sjm.PROJECTID, this.projectId);
            docJson.getJSONObject(i).put(Sjm.REFID, this.workspaceName);
            if (!docJson.getJSONObject(i).has(Sjm.SITECHARACTERIZATIONID)) {
                docJson.getJSONObject(i)
                    .put(Sjm.SITECHARACTERIZATIONID, pgh.getGroup(docJson.getJSONObject(i).getString(Sjm.SYSMLID)));
            }
            result.put(addChildViews(docJson.getJSONObject(i)));
        }

        return result;
    }

    public JSONArray addRelatedDocs(JSONArray elements) {
        if (elements.length() > 0) {
            for (int i = 0; i < elements.length(); i++) {
                Set<Pair<String, String>> views =
                    pgh.getImmediateParents(elements.getJSONObject(i).getString(Sjm.SYSMLID), DbEdgeTypes.VIEW);
                views.forEach((pair) -> {

                });
            }
        }

        return elements;
    }

    private JSONArray createAddedOrDeleted(JSONArray elements) {
        JSONArray commitElements = new JSONArray();
        for (int i = 0; i < elements.length(); i++) {
            JSONObject entry = elements.getJSONObject(i);
            JSONObject newObj = new JSONObject();
            newObj.put(Sjm.SYSMLID, entry.optString(Sjm.SYSMLID));
            newObj.put(Sjm.ELASTICID, entry.optString(Sjm.ELASTICID));
            commitElements.put(newObj);
        }
        return commitElements;
    }

    public JSONObject processCommit(JSONObject elements, String user, Map<String, JSONObject> foundElements,
        Map<String, String> foundParentElements) {

        JSONObject o = new JSONObject();
        JSONObject results = new JSONObject();
        String date = TimeUtils.toTimestamp(new Date().getTime());

        if (isInitialCommit()) {
            o.put(Sjm.ELASTICID, UUID.randomUUID().toString());
            o.put("added", new JSONObject());
            o.put("_creator", user);
            o.put("_created", date);
            results.put("commit", o);
            return results;
        }

        // Arrays of Elements to Process
        JSONArray addedElements = elements.optJSONArray("addedElements");
        JSONArray updatedElements = elements.optJSONArray("updatedElements");
        JSONArray deletedElements = elements.optJSONArray("deletedElements");
        // Keys for result
        JSONArray added = createAddedOrDeleted(addedElements);
        JSONArray updated = new JSONArray();
        JSONArray deleted = createAddedOrDeleted(deletedElements);

        for (int i = 0; i < updatedElements.length(); i++) {
            JSONObject entry = updatedElements.getJSONObject(i);
            String sysmlid = entry.getString(Sjm.SYSMLID);
            JSONObject parent = new JSONObject();
            if (foundElements.containsKey(sysmlid))
                parent.put("previousElasticId", foundElements.get(sysmlid).getString(Sjm.ELASTICID));
            parent.put(Sjm.SYSMLID, sysmlid);
            parent.put(Sjm.ELASTICID, entry.getString(Sjm.ELASTICID));
            updated.put(parent);
        }

        o.put("added", added);
        o.put("updated", updated);
        o.put("deleted", deleted);
        o.put("_creator", user);
        o.put("_created", date);
        // pregenerate the elasticId
        o.put(Sjm.ELASTICID, UUID.randomUUID().toString());
        results.put("commit", o);

        return results;
    }

    public JSONArray processElements(JSONArray elements, String user, Map<String, JSONObject> foundElements,
        JSONArray updatedElements, JSONArray deletedElements) {

        String date = TimeUtils.toTimestamp(new Date().getTime());

        String holdingBinSysmlid = "holding_bin";
        if (projectId != null) {
            holdingBinSysmlid = "holding_bin_" + projectId;
        }

        for (int i = 0; i < elements.length(); i++) {
            JSONArray newElements = new JSONArray();
            JSONObject o = elements.getJSONObject(i);
            String sysmlid = o.optString(Sjm.SYSMLID);
            if (sysmlid == null || sysmlid.equals("")) {
                sysmlid = createId();
                o.put(Sjm.SYSMLID, sysmlid);
            }
            if (!o.has(Sjm.OWNERID) || o.getString(Sjm.OWNERID) == null || o.getString(Sjm.OWNERID)
                .equalsIgnoreCase("null")) {
                o.put(Sjm.OWNERID, holdingBinSysmlid);
            }
            // pregenerate the elasticId
            o.put(Sjm.ELASTICID, UUID.randomUUID().toString());
            logger.debug(sysmlid);

            boolean added = !pgh.sysmlIdExists(sysmlid);
            boolean updated = false;

            if (!added) {
                updated = isUpdated(sysmlid);
            }

            if (added) {
                logger.debug("ELEMENT ADDED!");
                o.put(Sjm.CREATOR, user);
                o.put(Sjm.CREATED, date);
                o.put(Sjm.MODIFIER, user);
                o.put(Sjm.MODIFIED, date);
            }

            if (updated) {
                logger.debug("ELEMENT UPDATED!");
                // Get originalNode if updated
                o.put(Sjm.MODIFIER, user);
                o.put(Sjm.MODIFIED, date);
                JSONObject updating = getNodeBySysmlid(sysmlid);
                if (!updating.has(Sjm.SYSMLID)) {
                    pgh.resurrectNode(sysmlid);
                    updating = getNodeBySysmlid(sysmlid);
                }
                foundElements.put(sysmlid, updating);
            }

            if (added || updated) {
                reorderChildViews(o, newElements, deletedElements);
                elements.put(i, o);
            }

            for (int ii = 0; ii < newElements.length(); ii++) {
                elements.put(newElements.getJSONObject(ii));
            }
        }

        return elements;
    }

    public JSONObject processConfiguration(JSONObject postJson, String user, String date) {

        String oldId = null;
        JSONObject element = new JSONObject();

        List<Pair<String, String>> configs = pgh.getTags();

        for (Pair<String, String> config : configs) {
            logger.debug(config.second + " " + postJson.getString(Sjm.NAME));
            if (config.second.equalsIgnoreCase(postJson.getString(Sjm.NAME))) {
                oldId = config.first;
                break;
            }
        }

        if (oldId != null) {
            logger.debug("exists...");
            element.put(Sjm.NAME, postJson.getString(Sjm.NAME));
            element.put(Sjm.DESCRIPTION, postJson.getString(Sjm.DESCRIPTION));
            element.put(Sjm.MODIFIER, user);
            element.put(Sjm.MODIFIED, date);

            try {
                ElasticResult r = eh.indexElement(element);
                pgh.updateTag(postJson.getString(Sjm.NAME), r.elasticId, oldId);
            } catch (IOException e) {
                logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
            }

        } else {
            List<Map<String, String>> commits = pgh.getAllCommits();

            element.put(Sjm.NAME, postJson.getString(Sjm.NAME));
            element.put(Sjm.DESCRIPTION, postJson.getString(Sjm.DESCRIPTION));
            element.put(Sjm.CREATOR, user);
            element.put(Sjm.CREATED, date);
            element.put(Sjm.MODIFIER, user);
            element.put(Sjm.MODIFIED, date);
            element.put("commitId", commits.get(0).get("commitId"));
            element.put("_timestamp", commits.get(0).get("timestamp"));

            try {
                ElasticResult r = eh.indexElement(element);
                pgh.createBranchFromWorkspace(postJson.getString(Sjm.NAME), postJson.getString(Sjm.NAME), r.elasticId,
                    true);
            } catch (IOException e) {
                logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }

        return element;
    }

    public void deleteRef(String refId) {
        pgh.deleteRef(refId);
    }

    public boolean isTag() {
        return pgh.isTag(this.workspaceName);
    }

    public JSONObject addChildViews(JSONObject o) {
        return addChildViews(o, null);
    }

    private JSONObject addChildViews(JSONObject o, Map<String, JSONObject> visited) {
        if (visited == null) {
            visited = new HashMap<>();
        }
        if (o.has(Sjm.SYSMLID) && !visited.containsKey(o.getString(Sjm.SYSMLID))) {
            String sysmlid = o.getString(Sjm.SYSMLID);

            JSONArray typeArray = o.optJSONArray(Sjm.APPLIEDSTEREOTYPEIDS);
            if (typeArray != null) {
                for (int i = 0; i < typeArray.length(); i++) {
                    String typeJson = typeArray.optString(i);
                    if (Sjm.STEREOTYPEIDS.containsKey(typeJson) && (Sjm.STEREOTYPEIDS.get(typeJson).matches("view|document"))) {
                        JSONArray childViews = new JSONArray();
                        List<Map<String, String>> gotChildViews = pgh.getChildViews(sysmlid);
                        if (gotChildViews.size() > 0) {
                            for (Map<String, String> foundChildView : gotChildViews) {
                                Iterator<Map.Entry<String, String>> it = foundChildView.entrySet().iterator();
                                it.forEachRemaining((pair) -> {
                                    JSONObject childView = new JSONObject();
                                    childView.put("id", pair.getKey());
                                    childView.put("aggregation", pair.getValue());
                                    childViews.put(childView);
                                });
                            }
                            o.put(Sjm.CHILDVIEWS, childViews);
                            visited.put(o.getString(Sjm.SYSMLID), o);
                        }
                    }
                }
            }
        }
        return o;
    }

    private JSONObject reorderChildViews2(JSONObject element, JSONArray newElements, JSONArray updatedElements, JSONArray deletedElements) {

        if (!element.has(Sjm.CHILDVIEWS)) {
            return element;
        } else {
            if (!element.has(Sjm.OWNEDATTRIBUTEIDS)) {
                return element;
            }
        }

        String sysmlId = element.optString(Sjm.SYSMLID);
        Set<DbNodeTypes> dbnt = new HashSet<>();
        dbnt.add(DbNodeTypes.PACKAGE);
        String ownerParentPackage = pgh.getImmediateParentOfType(sysmlId, DbEdgeTypes.CONTAINMENT, dbnt);

        JSONObject oldElement = addChildViews(getNodeBySysmlid(element.optString(Sjm.SYSMLID)));

        JSONArray oldChildViews = oldElement.optJSONArray(Sjm.CHILDVIEWS);
        JSONArray oldOwnedAttributes = oldElement.optJSONArray(Sjm.OWNEDATTRIBUTEIDS);
        JSONArray newChildViews = element.optJSONArray(Sjm.CHILDVIEWS);
        JSONArray newOwnedAttributes = element.optJSONArray(Sjm.OWNEDATTRIBUTEIDS);

        JSONArray ownedAttributes = new JSONArray();

        if (oldChildViews != null && oldChildViews.length() > 0) {
            for (int i = 0; i < oldChildViews.length(); i++) {
                JSONObject child = oldChildViews.optJSONObject(i);
                if (child.optString(Sjm.SYSMLID) != null && newChildViews != null && !newChildViews.toString().contains(child.optString(Sjm.SYSMLID))) {
                    JSONObject childNode = getNodeBySysmlid(child.optString(Sjm.SYSMLID));
                    if (childNode.optString(Sjm.SYSMLID) != null) {
                        deletedElements.put(childNode);
                    }
                }
            }
        }

        if (oldOwnedAttributes != null && oldOwnedAttributes.length() > 0) {
            for (int i = 0; i < oldOwnedAttributes.length(); i++) {
                String childOwnedAttribute = oldOwnedAttributes.optString(i);
                if (childOwnedAttribute != null && newOwnedAttributes != null && !newOwnedAttributes.toString().contains(childOwnedAttribute)) {
                    JSONObject childNode = getNodeBySysmlid(childOwnedAttribute);
                    if (childNode.optString(Sjm.SYSMLID) != null) {
                        updatedElements.put(childNode);
                    }
                }
            }
        }

        if (newChildViews != null && newChildViews.length() > 0) {
            for (int i = 0; i < newChildViews.length(); i++) {
                JSONObject child = newChildViews.getJSONObject(i);
                if (child.has(Sjm.SYSMLID) && child.getString(Sjm.SYSMLID) != null) {
                    JSONObject childNode = getNodeBySysmlid(child.getString(Sjm.SYSMLID));
                    if (childNode.isNull(Sjm.SYSMLID)) {
                        String cvSysmlId = child.getString("id");
                        String aggregation = child.getString("aggregation");

                        String propertySysmlId = createId();
                        String associationSysmlId = createId();
                        String assocPropSysmlId = createId();

                        // Create Property
                        JSONObject property = new JSONObject();
                        property.put(Sjm.SYSMLID, propertySysmlId);
                        property.put(Sjm.NAME, "childView" + (i + 1));
                        property.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
                        property.put(Sjm.TYPE, "Property");
                        property.put(Sjm.OWNERID, sysmlId);
                        property.put(Sjm.TYPEID, cvSysmlId);
                        property.put(Sjm.AGGREGATION, aggregation);
                        property.put(Sjm.ELASTICID, UUID.randomUUID().toString());
                        // Default Fields
                        property.put(Sjm.ASSOCIATIONID, associationSysmlId);
                        JSONArray asid = new JSONArray();
                        asid.put("_15_0_be00301_1199377756297_348405_2678");
                        property.put(Sjm.APPLIEDSTEREOTYPEIDS, asid);
                        property.put(Sjm.DOCUMENTATION, "");
                        property.put(Sjm.MDEXTENSIONSIDS, new JSONArray());
                        property.put(Sjm.SYNCELEMENTID, JSONObject.NULL);
                        property.put(Sjm.APPLIEDSTEREOTYPEINSTANCEID, propertySysmlId + "_asi");
                        property.put(Sjm.CLIENTDEPENDENCYIDS, new JSONArray());
                        property.put(Sjm.SUPPLIERDEPENDENCYIDS, new JSONArray());
                        property.put(Sjm.VISIBILITY, "private");
                        property.put(Sjm.ISLEAF, false);
                        property.put(Sjm.ISSTATIC, false);
                        property.put(Sjm.ISORDERED, false);
                        property.put(Sjm.ISUNIQUE, true);
                        property.put(Sjm.LOWERVALUE, JSONObject.NULL);
                        property.put(Sjm.UPPERVALUE, JSONObject.NULL);
                        property.put(Sjm.ISREADONLY, false);
                        property.put(Sjm.TEMPLATEPARAMETERID, JSONObject.NULL);
                        property.put(Sjm.ENDIDS, new JSONArray());
                        property.put(Sjm.DEPLOYMENTIDS, new JSONArray());
                        property.put(Sjm.ASSOCIATIONENDID, JSONObject.NULL);
                        property.put(Sjm.QUALIFIERIDS, new JSONArray());
                        property.put(Sjm.DATATYPEID, JSONObject.NULL);
                        property.put(Sjm.DEFAULTVALUE, JSONObject.NULL);
                        property.put(Sjm.INTERFACEID, JSONObject.NULL);
                        property.put(Sjm.ISDERIVED, false);
                        property.put(Sjm.ISDERIVEDUNION, false);
                        property.put(Sjm.ISID, false);
                        property.put(Sjm.REDEFINEDPROPERTYIDS, new JSONArray());
                        property.put(Sjm.SUBSETTEDPROPERTYIDS, new JSONArray());

                        newElements.put(property);

                        // Create AppliedStereotypeInstance
                        JSONObject propertyASI = new JSONObject();
                        propertyASI.put(Sjm.SYSMLID, propertySysmlId + "_asi");
                        propertyASI.put(Sjm.NAME, "");
                        propertyASI.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
                        propertyASI.put(Sjm.TYPE, "InstanceSpecification");
                        propertyASI.put(Sjm.APPLIEDSTEREOTYPEIDS, new JSONArray());
                        propertyASI.put(Sjm.DOCUMENTATION, "");
                        propertyASI.put(Sjm.MDEXTENSIONSIDS, new JSONArray());
                        propertyASI.put(Sjm.OWNERID, propertySysmlId);
                        propertyASI.put(Sjm.SYNCELEMENTID, JSONObject.NULL);
                        propertyASI.put(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JSONObject.NULL);
                        propertyASI.put(Sjm.CLIENTDEPENDENCYIDS, new JSONArray());
                        propertyASI.put(Sjm.SUPPLIERDEPENDENCYIDS, new JSONArray());
                        propertyASI.put(Sjm.VISIBILITY, JSONObject.NULL);
                        propertyASI.put(Sjm.TEMPLATEPARAMETERID, JSONObject.NULL);
                        propertyASI.put(Sjm.DEPLOYMENTIDS, new JSONArray());
                        propertyASI.put(Sjm.SLOTIDS, new JSONArray());
                        propertyASI.put(Sjm.SPECIFICATION, JSONObject.NULL);
                        JSONArray classifierids = new JSONArray();
                        classifierids.put("_15_0_be00301_1199377756297_348405_2678");
                        propertyASI.put(Sjm.CLASSIFIERIDS, classifierids);
                        propertyASI.put(Sjm.STEREOTYPEDELEMENTID, propertySysmlId);

                        newElements.put(propertyASI);

                        // Create Associations
                        JSONObject association = new JSONObject();
                        JSONArray memberEndIds = new JSONArray();
                        memberEndIds.put(0, propertySysmlId);
                        memberEndIds.put(1, assocPropSysmlId);
                        JSONArray ownedEndIds = new JSONArray();
                        ownedEndIds.put(assocPropSysmlId);

                        association.put(Sjm.SYSMLID, associationSysmlId);
                        association.put(Sjm.NAME, "");
                        association.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
                        association.put(Sjm.TYPE, "Association");
                        association.put(Sjm.OWNERID, ownerParentPackage);
                        association.put(Sjm.MEMBERENDIDS, memberEndIds);
                        association.put(Sjm.OWNEDENDIDS, ownedEndIds);
                        association.put(Sjm.ELASTICID, UUID.randomUUID().toString());
                        // Default Fields
                        association.put(Sjm.DOCUMENTATION, "");
                        association.put(Sjm.MDEXTENSIONSIDS, new JSONArray());
                        association.put(Sjm.SYNCELEMENTID, JSONObject.NULL);
                        association.put(Sjm.APPLIEDSTEREOTYPEIDS, new JSONArray());
                        association.put(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JSONObject.NULL);
                        association.put(Sjm.CLIENTDEPENDENCYIDS, new JSONArray());
                        association.put(Sjm.SUPPLIERDEPENDENCYIDS, new JSONArray());
                        association.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
                        association.put(Sjm.VISIBILITY, "public");
                        association.put(Sjm.TEMPLATEPARAMETERID, JSONObject.NULL);
                        association.put(Sjm.ELEMENTIMPORTIDS, new JSONArray());
                        association.put(Sjm.PACKAGEIMPORTIDS, new JSONArray());
                        association.put(Sjm.ISLEAF, false);
                        association.put(Sjm.TEMPLATEBINDINGIDS, new JSONArray());
                        association.put(Sjm.USECASEIDS, new JSONArray());
                        association.put(Sjm.REPRESENTATIONID, JSONObject.NULL);
                        association.put(Sjm.COLLABORATIONUSEIDS, new JSONArray());
                        association.put(Sjm.GENERALIZATIONIDS, new JSONArray());
                        association.put(Sjm.POWERTYPEEXTENTIDS, new JSONArray());
                        association.put(Sjm.ISABSTRACT, false);
                        association.put(Sjm.ISFINALSPECIALIZATION, false);
                        association.put(Sjm.REDEFINEDCLASSIFIERIDS, new JSONArray());
                        association.put(Sjm.SUBSTITUTIONIDS, new JSONArray());
                        association.put(Sjm.ISDERIVED, false);
                        association.put(Sjm.NAVIGABLEOWNEDENDIDS, new JSONArray());

                        newElements.put(association);

                        // Create Association Property
                        JSONObject assocProperty = new JSONObject();
                        assocProperty.put(Sjm.SYSMLID, assocPropSysmlId);
                        assocProperty.put(Sjm.NAME, "");
                        assocProperty.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
                        assocProperty.put(Sjm.TYPE, "Property");
                        assocProperty.put(Sjm.TYPEID, sysmlId);
                        assocProperty.put(Sjm.OWNERID, associationSysmlId);
                        assocProperty.put(Sjm.AGGREGATION, "none");
                        assocProperty.put(Sjm.ELASTICID, UUID.randomUUID().toString());
                        // Default Fields
                        assocProperty.put(Sjm.ASSOCIATIONID, associationSysmlId);
                        assocProperty.put(Sjm.APPLIEDSTEREOTYPEIDS, new JSONArray());
                        assocProperty.put(Sjm.DOCUMENTATION, "");
                        assocProperty.put(Sjm.MDEXTENSIONSIDS, new JSONArray());
                        assocProperty.put(Sjm.SYNCELEMENTID, JSONObject.NULL);
                        assocProperty.put(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JSONObject.NULL);
                        assocProperty.put(Sjm.CLIENTDEPENDENCYIDS, new JSONArray());
                        assocProperty.put(Sjm.SUPPLIERDEPENDENCYIDS, new JSONArray());
                        assocProperty.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
                        assocProperty.put(Sjm.VISIBILITY, "private");
                        assocProperty.put(Sjm.ISLEAF, false);
                        assocProperty.put(Sjm.ISSTATIC, false);
                        assocProperty.put(Sjm.ISORDERED, false);
                        assocProperty.put(Sjm.ISUNIQUE, true);
                        assocProperty.put(Sjm.LOWERVALUE, JSONObject.NULL);
                        assocProperty.put(Sjm.UPPERVALUE, JSONObject.NULL);
                        assocProperty.put(Sjm.ISREADONLY, false);
                        assocProperty.put(Sjm.TEMPLATEPARAMETERID, JSONObject.NULL);
                        assocProperty.put(Sjm.ENDIDS, new JSONArray());
                        assocProperty.put(Sjm.DEPLOYMENTIDS, new JSONArray());
                        assocProperty.put(Sjm.ASSOCIATIONENDID, JSONObject.NULL);
                        assocProperty.put(Sjm.QUALIFIERIDS, new JSONArray());
                        assocProperty.put(Sjm.DATATYPEID, JSONObject.NULL);
                        assocProperty.put(Sjm.DEFAULTVALUE, JSONObject.NULL);
                        assocProperty.put(Sjm.INTERFACEID, JSONObject.NULL);
                        assocProperty.put(Sjm.ISDERIVED, false);
                        assocProperty.put(Sjm.ISDERIVEDUNION, false);
                        assocProperty.put(Sjm.ISID, false);
                        assocProperty.put(Sjm.REDEFINEDPROPERTYIDS, new JSONArray());
                        assocProperty.put(Sjm.SUBSETTEDPROPERTYIDS, new JSONArray());

                        newElements.put(assocProperty);
                        ownedAttributes.put(propertySysmlId);
                    } else {
                        String sysmlid = childNode.getString(Sjm.SYSMLID);
                        List<Map<String, String>> gotChildViews = pgh.getChildViews(sysmlid);
                        if (gotChildViews.size() > 0) {
                            for (Map<String, String> foundChildView : gotChildViews) {
                                Iterator<Map.Entry<String, String>> it = foundChildView.entrySet().iterator();
                                it.forEachRemaining((pair) -> {
                                    if (!ownedAttributes.toString().contains(pair.getKey())) {
                                        ownedAttributes.put(pair.getKey());
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }

        element.remove(Sjm.CHILDVIEWS);

        return element;
    }

    private JSONObject reorderChildViews(JSONObject element, JSONArray newElements, JSONArray deletedElements) {

        if (!element.has(Sjm.CHILDVIEWS)) {
            return element;
        } else {
            if (!element.has(Sjm.OWNEDATTRIBUTEIDS)) {
                return element;
            }
        }

        String sysmlId = element.optString(Sjm.SYSMLID);
        Set<DbNodeTypes> dbnt = new HashSet<>();
        dbnt.add(DbNodeTypes.PACKAGE);
        String ownerParentPackage = pgh.getImmediateParentOfType(sysmlId, DbEdgeTypes.CONTAINMENT, dbnt);
        JSONArray newChildViews = element.optJSONArray(Sjm.CHILDVIEWS);

        if (newChildViews != null && newChildViews.length() > 0) {
            logger.error(newChildViews.toString(4));
            pgh.deleteChildViews(sysmlId).forEach((childId) -> {
                JSONObject child = getElasticElement(childId);
                if (child.has(Sjm.SYSMLID)) {
                    deletedElements.put(child);

                    JSONObject association = getNodeBySysmlid(child.optString(Sjm.ASSOCIATIONID));
                    if (association.has(Sjm.SYSMLID)) {
                        pgh.deleteNode(association.getString(Sjm.SYSMLID));
                        deletedElements.put(association);
                    }

                    JSONObject asi = getNodeBySysmlid(child.optString(Sjm.APPLIEDSTEREOTYPEINSTANCEID));
                    if (asi.has(Sjm.SYSMLID)) {
                        pgh.deleteNode(asi.getString(Sjm.SYSMLID));
                        deletedElements.put(asi);
                    }
                    JSONArray assocProps = association.optJSONArray(Sjm.OWNEDENDIDS);
                    for (int i = 0; i < assocProps.length(); i++) {
                        JSONObject assocProp = getNodeBySysmlid(assocProps.getString(i));
                        if (assocProp.has(Sjm.SYSMLID)) {
                            pgh.deleteNode(assocProp.getString(Sjm.SYSMLID));
                            deletedElements.put(assocProp);
                        }
                    }
                }
            });

            JSONArray ownedAttributes = new JSONArray();

            for (int i = 0; i < newChildViews.length(); i++) {
                String cvSysmlId = newChildViews.getJSONObject(i).getString("id");
                String aggregation = newChildViews.getJSONObject(i).getString("aggregation");

                String propertySysmlId = createId();
                String associationSysmlId = createId();
                String assocPropSysmlId = createId();

                // Create Property
                JSONObject property = new JSONObject();
                property.put(Sjm.SYSMLID, propertySysmlId);
                property.put(Sjm.NAME, "childView" + (i + 1));
                property.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
                property.put(Sjm.TYPE, "Property");
                property.put(Sjm.OWNERID, sysmlId);
                property.put(Sjm.TYPEID, cvSysmlId);
                property.put(Sjm.AGGREGATION, aggregation);
                property.put(Sjm.ELASTICID, UUID.randomUUID().toString());
                // Default Fields
                property.put(Sjm.ASSOCIATIONID, associationSysmlId);
                JSONArray asid = new JSONArray();
                asid.put("_15_0_be00301_1199377756297_348405_2678");
                property.put(Sjm.APPLIEDSTEREOTYPEIDS, asid);
                property.put(Sjm.DOCUMENTATION, "");
                property.put(Sjm.MDEXTENSIONSIDS, new JSONArray());
                property.put(Sjm.SYNCELEMENTID, JSONObject.NULL);
                property.put(Sjm.APPLIEDSTEREOTYPEINSTANCEID, propertySysmlId + "_asi");
                property.put(Sjm.CLIENTDEPENDENCYIDS, new JSONArray());
                property.put(Sjm.SUPPLIERDEPENDENCYIDS, new JSONArray());
                property.put(Sjm.VISIBILITY, "private");
                property.put(Sjm.ISLEAF, false);
                property.put(Sjm.ISSTATIC, false);
                property.put(Sjm.ISORDERED, false);
                property.put(Sjm.ISUNIQUE, true);
                property.put(Sjm.LOWERVALUE, JSONObject.NULL);
                property.put(Sjm.UPPERVALUE, JSONObject.NULL);
                property.put(Sjm.ISREADONLY, false);
                property.put(Sjm.TEMPLATEPARAMETERID, JSONObject.NULL);
                property.put(Sjm.ENDIDS, new JSONArray());
                property.put(Sjm.DEPLOYMENTIDS, new JSONArray());
                property.put(Sjm.ASSOCIATIONENDID, JSONObject.NULL);
                property.put(Sjm.QUALIFIERIDS, new JSONArray());
                property.put(Sjm.DATATYPEID, JSONObject.NULL);
                property.put(Sjm.DEFAULTVALUE, JSONObject.NULL);
                property.put(Sjm.INTERFACEID, JSONObject.NULL);
                property.put(Sjm.ISDERIVED, false);
                property.put(Sjm.ISDERIVEDUNION, false);
                property.put(Sjm.ISID, false);
                property.put(Sjm.REDEFINEDPROPERTYIDS, new JSONArray());
                property.put(Sjm.SUBSETTEDPROPERTYIDS, new JSONArray());

                newElements.put(property);

                // Create AppliedStereotypeInstance
                JSONObject propertyASI = new JSONObject();
                propertyASI.put(Sjm.SYSMLID, propertySysmlId + "_asi");
                propertyASI.put(Sjm.NAME, "");
                propertyASI.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
                propertyASI.put(Sjm.TYPE, "InstanceSpecification");
                propertyASI.put(Sjm.APPLIEDSTEREOTYPEIDS, new JSONArray());
                propertyASI.put(Sjm.DOCUMENTATION, "");
                propertyASI.put(Sjm.MDEXTENSIONSIDS, new JSONArray());
                propertyASI.put(Sjm.OWNERID, propertySysmlId);
                propertyASI.put(Sjm.SYNCELEMENTID, JSONObject.NULL);
                propertyASI.put(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JSONObject.NULL);
                propertyASI.put(Sjm.CLIENTDEPENDENCYIDS, new JSONArray());
                propertyASI.put(Sjm.SUPPLIERDEPENDENCYIDS, new JSONArray());
                propertyASI.put(Sjm.VISIBILITY, JSONObject.NULL);
                propertyASI.put(Sjm.TEMPLATEPARAMETERID, JSONObject.NULL);
                propertyASI.put(Sjm.DEPLOYMENTIDS, new JSONArray());
                propertyASI.put(Sjm.SLOTIDS, new JSONArray());
                propertyASI.put(Sjm.SPECIFICATION, JSONObject.NULL);
                JSONArray classifierids = new JSONArray();
                classifierids.put("_15_0_be00301_1199377756297_348405_2678");
                propertyASI.put(Sjm.CLASSIFIERIDS, classifierids);
                propertyASI.put(Sjm.STEREOTYPEDELEMENTID, propertySysmlId);

                newElements.put(propertyASI);

                // Create Associations
                JSONObject association = new JSONObject();
                JSONArray memberEndIds = new JSONArray();
                memberEndIds.put(0, propertySysmlId);
                memberEndIds.put(1, assocPropSysmlId);
                JSONArray ownedEndIds = new JSONArray();
                ownedEndIds.put(assocPropSysmlId);

                association.put(Sjm.SYSMLID, associationSysmlId);
                association.put(Sjm.NAME, "");
                association.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
                association.put(Sjm.TYPE, "Association");
                association.put(Sjm.OWNERID, ownerParentPackage);
                association.put(Sjm.MEMBERENDIDS, memberEndIds);
                association.put(Sjm.OWNEDENDIDS, ownedEndIds);
                association.put(Sjm.ELASTICID, UUID.randomUUID().toString());
                // Default Fields
                association.put(Sjm.DOCUMENTATION, "");
                association.put(Sjm.MDEXTENSIONSIDS, new JSONArray());
                association.put(Sjm.SYNCELEMENTID, JSONObject.NULL);
                association.put(Sjm.APPLIEDSTEREOTYPEIDS, new JSONArray());
                association.put(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JSONObject.NULL);
                association.put(Sjm.CLIENTDEPENDENCYIDS, new JSONArray());
                association.put(Sjm.SUPPLIERDEPENDENCYIDS, new JSONArray());
                association.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
                association.put(Sjm.VISIBILITY, "public");
                association.put(Sjm.TEMPLATEPARAMETERID, JSONObject.NULL);
                association.put(Sjm.ELEMENTIMPORTIDS, new JSONArray());
                association.put(Sjm.PACKAGEIMPORTIDS, new JSONArray());
                association.put(Sjm.ISLEAF, false);
                association.put(Sjm.TEMPLATEBINDINGIDS, new JSONArray());
                association.put(Sjm.USECASEIDS, new JSONArray());
                association.put(Sjm.REPRESENTATIONID, JSONObject.NULL);
                association.put(Sjm.COLLABORATIONUSEIDS, new JSONArray());
                association.put(Sjm.GENERALIZATIONIDS, new JSONArray());
                association.put(Sjm.POWERTYPEEXTENTIDS, new JSONArray());
                association.put(Sjm.ISABSTRACT, false);
                association.put(Sjm.ISFINALSPECIALIZATION, false);
                association.put(Sjm.REDEFINEDCLASSIFIERIDS, new JSONArray());
                association.put(Sjm.SUBSTITUTIONIDS, new JSONArray());
                association.put(Sjm.ISDERIVED, false);
                association.put(Sjm.NAVIGABLEOWNEDENDIDS, new JSONArray());

                newElements.put(association);

                // Create Association Property
                JSONObject assocProperty = new JSONObject();
                assocProperty.put(Sjm.SYSMLID, assocPropSysmlId);
                assocProperty.put(Sjm.NAME, "");
                assocProperty.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
                assocProperty.put(Sjm.TYPE, "Property");
                assocProperty.put(Sjm.TYPEID, sysmlId);
                assocProperty.put(Sjm.OWNERID, associationSysmlId);
                assocProperty.put(Sjm.AGGREGATION, "none");
                assocProperty.put(Sjm.ELASTICID, UUID.randomUUID().toString());
                // Default Fields
                assocProperty.put(Sjm.ASSOCIATIONID, associationSysmlId);
                assocProperty.put(Sjm.APPLIEDSTEREOTYPEIDS, new JSONArray());
                assocProperty.put(Sjm.DOCUMENTATION, "");
                assocProperty.put(Sjm.MDEXTENSIONSIDS, new JSONArray());
                assocProperty.put(Sjm.SYNCELEMENTID, JSONObject.NULL);
                assocProperty.put(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JSONObject.NULL);
                assocProperty.put(Sjm.CLIENTDEPENDENCYIDS, new JSONArray());
                assocProperty.put(Sjm.SUPPLIERDEPENDENCYIDS, new JSONArray());
                assocProperty.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
                assocProperty.put(Sjm.VISIBILITY, "private");
                assocProperty.put(Sjm.ISLEAF, false);
                assocProperty.put(Sjm.ISSTATIC, false);
                assocProperty.put(Sjm.ISORDERED, false);
                assocProperty.put(Sjm.ISUNIQUE, true);
                assocProperty.put(Sjm.LOWERVALUE, JSONObject.NULL);
                assocProperty.put(Sjm.UPPERVALUE, JSONObject.NULL);
                assocProperty.put(Sjm.ISREADONLY, false);
                assocProperty.put(Sjm.TEMPLATEPARAMETERID, JSONObject.NULL);
                assocProperty.put(Sjm.ENDIDS, new JSONArray());
                assocProperty.put(Sjm.DEPLOYMENTIDS, new JSONArray());
                assocProperty.put(Sjm.ASSOCIATIONENDID, JSONObject.NULL);
                assocProperty.put(Sjm.QUALIFIERIDS, new JSONArray());
                assocProperty.put(Sjm.DATATYPEID, JSONObject.NULL);
                assocProperty.put(Sjm.DEFAULTVALUE, JSONObject.NULL);
                assocProperty.put(Sjm.INTERFACEID, JSONObject.NULL);
                assocProperty.put(Sjm.ISDERIVED, false);
                assocProperty.put(Sjm.ISDERIVEDUNION, false);
                assocProperty.put(Sjm.ISID, false);
                assocProperty.put(Sjm.REDEFINEDPROPERTYIDS, new JSONArray());
                assocProperty.put(Sjm.SUBSETTEDPROPERTYIDS, new JSONArray());

                newElements.put(assocProperty);

                // Add OwnedAttributeIds
                ownedAttributes.put(i, propertySysmlId);
            }
            if (ownedAttributes.length() > 0) {
                element.put(Sjm.OWNEDATTRIBUTEIDS, ownedAttributes);
            }
        }

        element.remove(Sjm.CHILDVIEWS);

        return element;
    }

    public String insertCommitIntoElastic(JSONObject elasticElements) {
        // JSON Object or Array passes the result with Eids from element insert
        String commitElasticId = null;
        try {
            commitElasticId = eh.indexElement(elasticElements).elasticId;
        } catch (IOException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return commitElasticId;

    }

    public void insertCommitIntoPostgres(String commitElasticId) {
        pgh.insertCommit(commitElasticId, DbCommitTypes.COMMIT, null);
    }

    public String insertCommitIntoPostgres(String commitElasticId, String creator) {
        return pgh.insertCommit(commitElasticId, DbCommitTypes.COMMIT, creator);
    }

    public Map<String, String> getGuidAndTimestampFromElasticId(String elasticid) {
        return pgh.getCommitAndTimestamp("elasticId", elasticid);
    }

    public String getGuidFromElasticId(String elasticid) {
        return pgh.getCommit("id", "elasticId", elasticid);
    }

    public String getTimestampfromElasticId(String elasticid) {
        return pgh.getCommit("timestamp", "elasticId", elasticid);
    }

    public JSONObject getElementByElasticID(String elasticId) {
        try {
            return eh.getElementByElasticId(elasticId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Map<String, JSONObject> convertToMap(JSONArray elements) {
        Map<String, JSONObject> result = new HashMap<>();
        for (int i = 0; i < elements.length(); i++) {
            result.put(elements.getJSONObject(i).getString(Sjm.SYSMLID), elements.getJSONObject(i));
        }

        return result;
    }

    public JSONObject insertIntoElastic(JSONArray elasticElements, Map<String, String> foundParentElements,
        JSONArray updatedElements, JSONArray deletedElements) {

        JSONArray addedElements = new JSONArray();
        JSONArray newElements = new JSONArray();

        JSONObject results = new JSONObject();

        Map<String, JSONObject> elementMap = convertToMap(elasticElements);

        for (Entry<String, JSONObject> entry : elementMap.entrySet()) {
            // saves you a lookup provides access to key and value
            String sysmlid = entry.getKey();
            if (logger.isInfoEnabled()) {
                logger.info("insertIntoElastic: " + sysmlid);
            }

            JSONObject e = entry.getValue();

            boolean added;
            boolean updated = false;

            added = !pgh.sysmlIdExists(sysmlid);
            if (!added) {
                updated = isUpdated(sysmlid);
            }

            if (added) {
                if (logger.isInfoEnabled()) {
                    logger.info("Added: " + sysmlid);
                }
                addedElements.put(e);
            } else if (updated) {
                if (logger.isInfoEnabled()) {
                    logger.info("Updated: " + sysmlid);
                }
                foundParentElements.put(sysmlid, e.getString(Sjm.ELASTICID));
                updatedElements.put(e);
            } else {
                logger.error("Element is not added or updated: " + sysmlid);
            }

            if (added || updated) {
                if (logger.isInfoEnabled()) {
                    logger.info("Added or updated:" + sysmlid);
                }
                e.put(Sjm.PROJECTID, this.projectId);
                e.put(Sjm.REFID, this.workspaceName);
                newElements.put(e);
            }
        }

        results.put("addedElements", addedElements);
        results.put("updatedElements", updatedElements);
        results.put("newElements", newElements);
        results.put("deletedElements", deletedElements);

        return results;
    }

    public JSONArray getOwnersNotFound(JSONArray elements, String projectId) {
        JSONArray ownersNotFound = new JSONArray();
        List<String> ownersToCheck = new ArrayList<>();
        String holdingBinSysmlid = "holding_bin";

        Map<String, JSONObject> sysmlid2elements = getSysmlMap(elements);

        if (projectId == null) {
            projectId = this.projectId;
        }

        if (projectId != null) {
            holdingBinSysmlid = "holding_bin_" + projectId;
        }

        for (int i = 0; i < elements.length(); i++) {
            JSONObject o = elements.getJSONObject(i);
            if (!o.has(Sjm.OWNERID) || o.getString(Sjm.OWNERID) == null || o.getString(Sjm.OWNERID)
                .equalsIgnoreCase("null")) {
                o.put(Sjm.OWNERID, holdingBinSysmlid);
            } else if (!sysmlid2elements.containsKey(o.getString(Sjm.OWNERID))) {
                String ownerId = o.getString(Sjm.OWNERID);
                ownersToCheck.add(ownerId);
            }
        }

        List<String> foundOwners = pgh.filterNodesWithSysmlid(ownersToCheck);
        ownersToCheck.forEach((ownerId) -> {
            if (!foundOwners.contains(ownerId)) {
                JSONObject ob = new JSONObject();
                ob.put(Sjm.SYSMLID, ownerId);
                ownersNotFound.put(ob);
            }
        });

        return ownersNotFound;
    }

    private Map<String, Map<String, String>> calculateQualifiedInformation(JSONArray elements)
        throws JSONException, IOException {
        Map<String, Map<String, String>> result = new HashMap<>();
        Map<String, JSONObject> sysmlid2elements = getSysmlMap(elements);

        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            String sysmlid = element.getString(Sjm.SYSMLID);
            Map<String, String> extendedInfo = getQualifiedInformationForElement(element, sysmlid2elements);

            Map<String, String> attrs = new HashMap<>();
            attrs.put(Sjm.QUALIFIEDNAME, extendedInfo.get(Sjm.QUALIFIEDNAME));
            attrs.put(Sjm.QUALIFIEDID, extendedInfo.get(Sjm.QUALIFIEDID));
            attrs.put(Sjm.SITECHARACTERIZATIONID, extendedInfo.get(Sjm.SITECHARACTERIZATIONID));

            result.put(sysmlid, attrs);
        }

        return result;
    }

    private Map<String, String> getQualifiedInformationForElement(JSONObject element,
        Map<String, JSONObject> elementMap) {
        Map<String, JSONObject> cache = new HashMap<>();
        return getQualifiedInformationForElement(element, elementMap, cache);
    }

    private Map<String, String> getQualifiedInformationForElement(JSONObject element,
        Map<String, JSONObject> elementMap, Map<String, JSONObject> cache) {

        Map<String, String> result = new HashMap<>();

        JSONObject o = element;
        List<Map<String, String>> organizations = pgh.getOrganizations(null);
        List<String> orgList = new ArrayList<>();
        organizations.forEach((organization) -> {
            orgList.add(organization.get("orgId"));
        });
        ArrayList<String> qn = new ArrayList<>();
        ArrayList<String> qid = new ArrayList<>();
        String sqn;
        String sqid;
        String siteCharacterizationId = null;
        qn.add(o.optString("name"));
        qid.add(o.optString(Sjm.SYSMLID));

        while (o.has(Sjm.OWNERID) && o.optString(Sjm.OWNERID) != null && !o.optString(Sjm.OWNERID).equals("null")) {
            String sysmlid = o.optString(Sjm.OWNERID);
            JSONObject owner = elementMap.get(sysmlid);
            if (owner == null) {
                if (cache.containsKey(sysmlid)) {
                    owner = cache.get(sysmlid);
                } else {
                    owner = getNodeBySysmlid(sysmlid);
                    cache.put(sysmlid, owner);
                }
            }

            String ownerId = owner.optString(Sjm.SYSMLID);
            if (ownerId == null || ownerId.equals("")) {
                ownerId = "null";
            }
            qid.add(ownerId);

            String ownerName = owner.optString(Sjm.NAME);
            if (ownerName == null || ownerName.equals("")) {
                ownerName = "null";
            }
            qn.add(ownerName);

            if (siteCharacterizationId == null && CommitUtil.isSite(owner)) {
                siteCharacterizationId = owner.optString(Sjm.SYSMLID);
            }
            o = owner;
        }

        List<Pair<String, String>> containmentParents = pgh.getContainmentParents(o.optString(Sjm.SYSMLID), 1000);
        for (Pair<String, String> parent : containmentParents) {
            try {
                JSONObject containmentNode = eh.getElementByElasticId(parent.second);
                if (containmentNode != null && !containmentNode.optString(Sjm.SYSMLID)
                    .equals(o.optString(Sjm.SYSMLID))) {
                    qn.add(containmentNode.optString(Sjm.NAME));
                    qid.add(containmentNode.optString(Sjm.SYSMLID));
                }
            } catch (Exception e) {
                logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }

        Collections.reverse(qn);
        Collections.reverse(qid);

        sqn = "/" + String.join("/", qn);
        sqid = "/" + String.join("/", qid);

        if (siteCharacterizationId == null) {
            Matcher matcher = Pattern.compile("/?(.*?)/.*").matcher(sqid);
            if (matcher.matches()) {
                siteCharacterizationId = matcher.group(1);
            } else {
                siteCharacterizationId = sqid;
            }
        }

        if (orgList.contains(siteCharacterizationId)) {
            siteCharacterizationId = null;
        }

        result.put(Sjm.QUALIFIEDNAME, sqn);
        result.put(Sjm.QUALIFIEDID, sqid);
        result.put(Sjm.SITECHARACTERIZATIONID, siteCharacterizationId);

        return result;
    }

    public boolean isInitialCommit() {
        return pgh.isInitialCommit();
    }

    private boolean isMoved(String sysmlid, String owner) {
        return pgh.isMoved(sysmlid, owner);
    }

    private boolean isUpdated(String sysmlid) {
        return pgh.sysmlIdExists(sysmlid);
    }

    public boolean isDeleted(String sysmlid) {
        return pgh.isDeleted(sysmlid);
    }

    public boolean siteExists(String siteName) {
        return siteName.equals("swsdp") || pgh.siteExists(siteName);
    }

    public boolean orgExists(String orgName) {
        return orgName.equals("swsdp") || pgh.orgExists(orgName);
    }

    public List<String> filterSearchResults(List<String> sysmlIds) {
        return pgh.getElasticIdsFromSysmlIds(sysmlIds);
    }

    public String getSite(String sysmlid) {
        String result = null;
        for (String parent : pgh.getRootParents(sysmlid, DbEdgeTypes.CONTAINMENT)) {
            result = parent;
        }
        return result;
    }

    public String getSiteFromElements(JSONArray elements) {
        for (int i = 0; i < elements.length(); i++) {
            String siteId = getSite(elements.getJSONObject(i).optString(Sjm.OWNERID));
            if (siteId != null) {
                return siteId;
            }
        }
        return null;
    }

    public String getProjectSysmlId(String siteSysmlId) {
        for (Map<String, Object> project : pgh.getProjects(siteSysmlId)) {
            if (!project.get(Sjm.SYSMLID).toString().contains("no_project")) {
                return project.get("sysmlId").toString();
            }
        }
        return null;
    }

    public JSONArray populateElements(JSONArray elements) {
        for (int i = 0; i < elements.length(); i++) {
            diffUpdateJson(elements.getJSONObject(i));
        }

        return elements;
    }

    private void diffUpdateJson(JSONObject json) {
        JSONObject existing;

        if (json.has(Sjm.SYSMLID)) {
            existing = getNodeBySysmlid(json.getString(Sjm.SYSMLID), false);
            if (existing.has(Sjm.SYSMLID)) {
                mergeJson(json, existing);
            }
        }
    }

    private void mergeJson(JSONObject partial, JSONObject original) {
        if (original == null) {
            return;
        }

        for (String attr : JSONObject.getNames(original)) {
            if (!partial.has(attr)) {
                partial.put(attr, original.get(attr));
            }
        }
    }

    public JSONObject addCommitId(JSONObject elements, String commitId) {
        JSONObject result = new JSONObject();

        List<String> attributes =
            Stream.of("addedElements", "updatedElements", "movedElements", "newElements", "deletedElements")
                .collect(Collectors.toList());

        attributes.forEach((e) -> {
            if (elements.has(e)) {
                JSONArray newResult = new JSONArray();
                for (int i = 0; i < elements.getJSONArray(e).length(); i++) {
                    JSONObject o = elements.getJSONArray(e).getJSONObject(i);
                    o.put(Sjm.COMMITID, commitId);
                    newResult.put(o);
                }
                result.put(e, newResult);
            }
        });

        return result;
    }

    public JSONArray addExtendedInformation(JSONArray elements) {
        JSONArray newElements = new JSONArray();

        Map<String, Map<String, String>> sysmlid2qualified = new HashMap<>();

        try {
            sysmlid2qualified = calculateQualifiedInformation(elements);
        } catch (IOException e) {
            logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
        }

        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);

            JSONObject newElement = addExtendedInformationForElement(element, sysmlid2qualified);
            newElements.put(newElement);
        }

        return newElements.length() >= elements.length() ? newElements : elements;
    }

    JSONObject addExtendedInformationForElement(JSONObject element) {
        JSONArray tmpArray = new JSONArray();
        tmpArray.put(element);
        Map<String, Map<String, String>> qualifiedInformation = new HashMap<>();
        try {
            qualifiedInformation = calculateQualifiedInformation(tmpArray);
        } catch (IOException e) {
            logger.debug(e.getMessage());
        }
        return addExtendedInformationForElement(element, qualifiedInformation);
    }

    private JSONObject addExtendedInformationForElement(JSONObject element,
        Map<String, Map<String, String>> qualifiedInformation) {

        String sysmlid = element.getString(Sjm.SYSMLID);

        if (qualifiedInformation.containsKey(sysmlid)) {
            Map<String, String> current = qualifiedInformation.get(sysmlid);
            if (current.containsKey(Sjm.QUALIFIEDNAME)) {
                element.put(Sjm.QUALIFIEDNAME, current.get(Sjm.QUALIFIEDNAME));
            }
            if (current.containsKey(Sjm.QUALIFIEDID)) {
                element.put(Sjm.QUALIFIEDID, current.get(Sjm.QUALIFIEDID));
            }
            if (current.containsKey(Sjm.SITECHARACTERIZATIONID)) {
                element.put(Sjm.SITECHARACTERIZATIONID, current.get(Sjm.SITECHARACTERIZATIONID));
            }
        }

        return element;
    }

    public String insertSingleElastic(JSONObject o) {
        try {
            ElasticResult r = eh.indexElement(o);
            return r.elasticId;
        } catch (IOException e) {
            logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return null;
    }

    private static Map<String, JSONObject> getSysmlMap(JSONArray elements) {
        Map<String, JSONObject> sysmlid2elements = new HashMap<>();
        for (int i = 0; i < elements.length(); i++) {
            JSONObject newJson = elements.getJSONObject(i);
            String sysmlid = newJson.optString(Sjm.SYSMLID);
            if (!sysmlid.isEmpty()) {
                sysmlid2elements.put(sysmlid, newJson);
            }
        }
        return sysmlid2elements;
    }

    public static JSONArray wrapResponse(JSONArray elements) {
        JSONArray results = new JSONArray();
        for (int i = 0; i < elements.length(); i++) {
            JSONObject result = new JSONObject();
            result.put("element", elements.get(i));
            results.put(result);
        }

        return results;
    }

    static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
        Map<String, Object> result = new HashMap<>();

        if (json != JSONObject.NULL) {
            result = toMap(json);
        }

        return result;
    }

    private static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<>();

        Iterator<?> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = (String) keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }

        return map;
    }

    private static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }

        return list;
    }

    private String createId() {
        for (int i = 0; i < 10; ++i) {
            String id = "MMS_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString();
            if (!pgh.sysmlIdExists(id)) {
                return id;
            }
        }
        return null;
    }

    public String getImmediateParentOfType(String sysmlId, DbEdgeTypes edgeType, DbNodeTypes nodeType) {
        Set<DbNodeTypes> nodeTypes = new HashSet<>();
        nodeTypes.add(nodeType);
        return getImmediateParentOfTypes(sysmlId, edgeType, nodeTypes);
    }

    public String getImmediateParentOfTypes(String sysmlId, DbEdgeTypes edgeType, Set<DbNodeTypes> nodeTypes) {
        return pgh.getImmediateParentOfType(sysmlId, edgeType, nodeTypes);
    }

    public JSONArray processImageData(JSONArray elements, WorkspaceNode workspace) {
        for (int i = 0; i < elements.length(); i++) {
            String content = elements.getJSONObject(i).toString();
            if (isImageData(content)) {
                content = extractAndReplaceImageData(content, workspace,
                    getSite(elements.getJSONObject(i).getString(Sjm.SYSMLID)));
                elements.put(i, new JSONObject(content));
            }
        }

        return elements;
    }

    private boolean isImageData(String value) {
        if (value == null) {
            return false;
        }

        Pattern p = Pattern.compile(
            "(.*)<img[^>]*\\ssrc\\s*=\\\\\\s*[\\\"']data:image/([^;]*);\\s*base64\\s*,([^\\\"']*)[\\\"'][^>]*>(.*)",
            Pattern.DOTALL);
        Matcher m = p.matcher(value);

        return m.matches();
    }

    private String extractAndReplaceImageData(String value, WorkspaceNode ws, String siteName) {
        if (value == null) {
            return null;
        }

        Pattern p = Pattern.compile(
            "(.*)<img[^>]*\\ssrc\\s*=\\\\\\s*[\\\"']data:image/([^;]*);\\s*base64\\s*,([^\\\"']*)[\\\"'][^>]*>(.*)",
            Pattern.DOTALL);
        while (true) {
            Matcher m = p.matcher(value);
            if (!m.matches()) {
                logger.debug(String.format("no match found for value=%s",
                    value.substring(0, Math.min(value.length(), 100)) + (value.length() > 100 ? " . . ." : "")));
                break;
            } else {
                logger.debug(String.format("match found for value=%s",
                    value.substring(0, Math.min(value.length(), 100)) + (value.length() > 100 ? " . . ." : "")));
                if (m.groupCount() != 4) {
                    logger.debug(String.format("Expected 4 match groups, got %s! %s", m.groupCount(), m));
                    break;
                }
                String extension = m.group(2);
                String content = m.group(3);
                String name = "img_" + System.currentTimeMillis();

                // No need to pass a date since this is called in the context of
                // updating a node, so the time is the current time (which is
                // null).
                String subfolder = this.projectId + "/refs/" + this.workspaceName;
                EmsScriptNode artNode = NodeUtil
                    .updateOrCreateArtifact(name, extension, content, null, siteName, subfolder, ws, null, null, null,
                        false);
                if (artNode == null || !artNode.exists()) {
                    logger.debug("Failed to pull out image data for value! " + value);
                    break;
                }

                String url = artNode.getUrl();
                String link = "<img src=\\\"" + url + "\\\"/>";
                link = link.replace("/d/d/", "/alfresco/service/api/node/content/");
                value = m.group(1) + link + m.group(4);
            }
        }

        return value;
    }

    private static String camelize(String str) {
        Pattern p = Pattern.compile("_|\\s+(.)");
        Matcher m = p.matcher(str.replaceAll("[^A-Za-z0-9]", ""));
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toUpperCase());
        }
        m.appendTail(sb);
        System.out.println(sb.toString());
        return sb.toString();
    }
}
