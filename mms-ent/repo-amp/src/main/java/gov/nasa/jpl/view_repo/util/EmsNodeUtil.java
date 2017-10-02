package gov.nasa.jpl.view_repo.util;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import org.alfresco.service.cmr.version.Version;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.ElasticResult;
import gov.nasa.jpl.view_repo.db.Node;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.db.GraphInterface.DbEdgeTypes;
import gov.nasa.jpl.view_repo.db.GraphInterface.DbNodeTypes;

public class EmsNodeUtil {

    private ElasticHelper eh = null;
    private PostgresHelper pgh = null;
    private String projectId = null;
    private String workspaceName = "master";
    private static Logger logger = Logger.getLogger(EmsNodeUtil.class);

    private static final String ORG_ID = "orgId";
    private static final String ORG_NAME = "orgName";

    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public EmsNodeUtil() {
        try {
            eh = new ElasticHelper();
            pgh = new PostgresHelper();
            switchWorkspace("master");
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

    public void switchWorkspace(String workspaceName) {
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
        for (Map<String, String> n : organizations) {
            JSONObject org = new JSONObject();
            org.put(Sjm.SYSMLID, n.get(ORG_ID));
            org.put(Sjm.NAME, n.get(ORG_NAME));
            orgs.put(org);
        }
        return orgs;
    }

    public String getOrganizationFromProject(String projectId) {
        return pgh.getOrganizationFromProject(projectId);
    }

    public JSONArray getProjects(String orgId) {
        JSONArray projects = new JSONArray();
        List<Map<String, Object>> orgProjects = pgh.getProjects(orgId);
        for (Map<String, Object> n : orgProjects) {
            switchProject(n.get(Sjm.SYSMLID).toString());
            JSONObject project = getNodeBySysmlid(n.get(Sjm.SYSMLID).toString());
            project.put(ORG_ID, orgId);
            projects.put(project);
        }
        return projects;
    }

    public JSONArray getProjects() {
        JSONArray projects = new JSONArray();
        for (Map<String, Object> project : pgh.getProjects()) {
            switchProject(project.get(Sjm.SYSMLID).toString());
            JSONObject proj = getNodeBySysmlid(project.get(Sjm.SYSMLID).toString());
            proj.put(ORG_ID, project.get(ORG_ID).toString());
            projects.put(proj);
        }
        return projects;
    }

    public JSONObject getProject(String projectId) {
        Map<String, Object> project = pgh.getProject(projectId);
        if (!project.isEmpty() && !project.get(Sjm.SYSMLID).toString().contains("no_project")) {
            switchProject(projectId);
            JSONObject proj = getNodeBySysmlid(projectId);
            proj.put(ORG_ID, project.get(ORG_ID).toString());
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
            projectJson.put(ORG_ID, project.get(ORG_ID).toString());
            realFound.add(projectId);
            JSONArray mountObject = getFullMounts(realFound);
            projectJson.put(Sjm.MOUNTS, mountObject);
            return projectJson;
        }
        return null;
    }

    public JSONArray getFullMounts(List<String> found) {
        JSONArray mounts = new JSONArray();
        String curProjectId = this.projectId;
        String curRefId = this.workspaceName;
        List<Node> nodes = pgh.getNodesByType(DbNodeTypes.MOUNT);
        if (nodes.isEmpty()) {
            return mounts;
        }
        Set<String> mountIds = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            mountIds.add(nodes.get(i).getSysmlId());
        }
        JSONArray nodeList = getNodesBySysmlids(mountIds);
        for (int i = 0; i < nodeList.length(); i++) {
            JSONObject mountJson = nodeList.getJSONObject(i);
            if (mountJson.has(Sjm.MOUNTEDELEMENTPROJECTID) && mountJson.has(Sjm.MOUNTEDREFID)) {
                if (found.contains(mountJson.getString(Sjm.MOUNTEDELEMENTPROJECTID))) {
                    continue;
                }
                JSONObject childProject = getProjectWithFullMounts(mountJson.getString(Sjm.MOUNTEDELEMENTPROJECTID),
                    mountJson.getString(Sjm.MOUNTEDREFID), found);
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
            return eh.getElementByCommitId(commitId, sysmlid, projectId);
        } catch (IOException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return new JSONObject();
    }

    public Node getById(String sysmlId) {
        return pgh.getNodeFromSysmlId(sysmlId, true);
    }

    public Boolean commitContainsElement(String elementId, String commitId) {
        try {
            return eh.checkForElasticIdInCommit(elementId, commitId, projectId);
        } catch (IOException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return false;
    }

    public JSONObject getNodeBySysmlid(String sysmlid) {
        return getNodeBySysmlid(sysmlid, this.workspaceName, true);
    }

    /**
     * Retrieves node by sysmlid adding childViews as necessary
     *
     * @param sysmlid       String of sysmlid to look up
     * @param workspaceName Workspace to retrieve id against
     * @return
     */

    private JSONObject getNodeBySysmlid(String sysmlid, String workspaceName,
        boolean withChildViews) {
        if (!this.workspaceName.equals(workspaceName)) {
            switchWorkspace(workspaceName);
        }

        String elasticId = pgh.getElasticIdFromSysmlId(sysmlid);
        if (elasticId != null) {
            try {
                JSONObject result = eh.getElementByElasticId(elasticId, projectId);
                if (result != null) {
                    result.put(Sjm.PROJECTID, this.projectId);
                    result.put(Sjm.REFID, this.workspaceName);
                    return withChildViews ? addChildViews(result) : result;
                }
            } catch (Exception e) {
                logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }
        return new JSONObject();
    }

    public JSONArray getNodesBySysmlids(Set<String> sysmlids) {
        return getNodesBySysmlids(sysmlids, true, false);
    }

    public JSONArray getNodesBySysmlids(Set<String> sysmlids, boolean withChildViews, boolean withDeleted) {
        List<String> elasticids = pgh.getElasticIdsFromSysmlIds(new ArrayList<>(sysmlids), withDeleted);
        JSONArray elementsFromElastic = new JSONArray();
        try {
            elementsFromElastic = eh.getElementsFromElasticIds(elasticids, projectId);
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        for (int i = 0; i < elementsFromElastic.length(); i++) {
            JSONObject formatted = elementsFromElastic.getJSONObject(i);
            formatted.put(Sjm.PROJECTID, this.projectId);
            formatted.put(Sjm.REFID, this.workspaceName);
            elementsFromElastic.put(i, withChildViews ? addChildViews(formatted) : formatted);
        }

        return elementsFromElastic;
    }

    public JSONArray getNodeHistory(String sysmlId) {
        JSONArray nodeHistory = new JSONArray();
        try {
            nodeHistory = filterCommitsByRefs(eh.getCommitHistory(sysmlId, projectId));
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return nodeHistory;
    }

    public JSONArray getRefHistory(String refId) {
        JSONArray result = new JSONArray();
        List<Map<String, Object>> refCommits = pgh.getRefsCommits(refId);
        for (int i = 0; i < refCommits.size(); i++) {
            Map<String, Object> refCommit = refCommits.get(i);
            JSONObject commit = new JSONObject();
            commit.put(Sjm.SYSMLID, refCommit.get(Sjm.SYSMLID));
            commit.put(Sjm.CREATOR, refCommit.get(Sjm.CREATOR));
            commit.put(Sjm.CREATED, df.format(refCommit.get(Sjm.CREATED)));
            result.put(commit);
        }

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

    public void insertRef(String refId, String refName, String elasticId, boolean isTag) {
        pgh.insertRef(refId, refName, 0, elasticId, isTag);
    }

    public void updateRef(String refId, String refName, String elasticId, boolean isTag) {
        pgh.updateRef(refId, refName, elasticId, isTag);
    }

    public JSONObject getRefJson(String refId) {
        JSONObject jObj = null;
        Pair<String, String> refInfo = pgh.getRefElastic(refId);
        if (refInfo != null) {
            try {
                jObj = eh.getElementByElasticId(refInfo.second, projectId);
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
        for (Pair<String, String> ref : refs) {
            elasticIds.add(ref.second);
        }
        try {
            result = eh.getElementsFromElasticIds(elasticIds, projectId);
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

        for (Pair<String, String> childId : pgh.getChildren(sysmlid, dbEdge, depth)) {
            children.add(childId.second);
        }

        try {
            List<String> childrenList = new ArrayList<>(children);
            JSONArray childs = eh.getElementsFromElasticIds(childrenList, projectId);
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

    public JSONArray search(JSONObject query) {
        try {
            return eh.search(query);
        } catch (IOException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return new JSONArray();
    }

    public JSONArray addExtraDocs(JSONArray elements) {
        JSONArray results = new JSONArray();
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            String elementSysmlId = element.getString(Sjm.SYSMLID);
            JSONArray relatedDocuments = new JSONArray();
            Map<String, List<JSONObject>> relatedDocumentsMap = new HashMap<>();

            Map<String, Set<String>> docView = new HashMap<>();
            Set<Pair<String, Integer>> parentViews =
                pgh.getParentsOfType(elementSysmlId, PostgresHelper.DbEdgeTypes.VIEW);

            for (Pair<String, Integer> parentView : parentViews) {

                if (parentView.second != DbNodeTypes.VIEW.getValue() && parentView.second != DbNodeTypes.DOCUMENT
                    .getValue()) {
                    continue;
                }
                for (Pair<String, Integer> doc : pgh
                    .getParentsOfType(parentView.first, PostgresHelper.DbEdgeTypes.CHILDVIEW)) {
                    if (doc.second != DbNodeTypes.DOCUMENT.getValue()) {
                        continue;
                    }
                    if (relatedDocumentsMap.containsKey(doc.first) && !docView.get(doc.first)
                        .contains(parentView.first)) {
                        relatedDocumentsMap.get(doc.first).add(new JSONObject().put(Sjm.SYSMLID, parentView.first));
                        docView.get(doc.first).add(parentView.first);
                    } else {
                        docView.put(doc.first, new HashSet<String>());
                        List<JSONObject> viewParents = new ArrayList<>();
                        viewParents.add(new JSONObject().put(Sjm.SYSMLID, parentView.first));
                        docView.get(doc.first).add(parentView.first);
                        relatedDocumentsMap.put(doc.first, viewParents);
                    }
                }
            }
            Iterator<Map.Entry<String, List<JSONObject>>> it = relatedDocumentsMap.entrySet().iterator();
            it.forEachRemaining(pair -> {
                JSONArray viewIds = new JSONArray();
                for (JSONObject value : pair.getValue()) {
                    viewIds.put(value);
                }
                JSONObject relatedDocObject = new JSONObject();
                relatedDocObject.put(Sjm.SYSMLID, pair.getKey());
                relatedDocObject.put(Sjm.PARENTVIEWS, viewIds);
                relatedDocObject.put(Sjm.PROJECTID, this.projectId);
                relatedDocObject.put(Sjm.REFID, this.workspaceName);
                relatedDocuments.put(relatedDocObject);
            });
            element.put(Sjm.RELATEDDOCUMENTS, relatedDocuments);

            results.put(element);
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
    public JSONArray getDocJson(String sysmlId, String commitId, boolean extended, int depth) {

        JSONArray result = new JSONArray();
        List<Node> docNodes = pgh.getNodesByType(DbNodeTypes.DOCUMENT);
        List<String> docElasticIds = new ArrayList<>();
        Map<String, String> docSysml2Elastic = new HashMap<>();
        for (Node node : docNodes) {
            docSysml2Elastic.put(node.getSysmlId(), node.getElasticId());
        }

        if (sysmlId != null) {
            List<Pair<String, String>> siteChildren = pgh.getChildren(sysmlId, DbEdgeTypes.CONTAINMENT, depth);
            Set<String> siteChildrenIds = new HashSet<>();
            for (Pair<String, String> child : siteChildren) {
                siteChildrenIds.add(child.first);
            }
            for (String docSysmlId : docSysml2Elastic.keySet()) {
                if (siteChildrenIds.contains(docSysmlId)) {
                    docElasticIds.add(docSysml2Elastic.get(docSysmlId));
                }
            }
        } else {
            docElasticIds.addAll(docSysml2Elastic.values());
        }

        JSONArray docJson = new JSONArray();
        try {
            docJson = eh.getElementsFromElasticIds(docElasticIds, projectId);
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

    public JSONObject processPostJson(JSONArray elements, String user, Set<String> oldElasticIds, boolean overwriteJson,
        String src) {

        JSONObject result = new JSONObject();

        String date = TimeUtils.toTimestamp(new Date().getTime());
        String organization = getOrganizationFromProject(this.projectId);
        final String holdingBinSysmlid = (this.projectId != null) ? ("holding_bin_" + this.projectId) : "holding_bin";

        String commitId = UUID.randomUUID().toString();
        JSONObject commit = new JSONObject();
        commit.put(Sjm.ELASTICID, commitId);
        JSONArray commitAdded = new JSONArray();
        JSONArray commitUpdated = new JSONArray();
        JSONArray commitDeleted = new JSONArray();

        JSONArray addedElements = new JSONArray();
        JSONArray updatedElements = new JSONArray();
        JSONArray deletedElements = new JSONArray();
        JSONArray rejectedElements = new JSONArray();
        JSONArray newElements = new JSONArray();

        Map<String, JSONObject> elementMap = convertToMap(elements);
        Set<String> sysmlids = new HashSet<>();
        sysmlids.addAll(elementMap.keySet());

        Map<String, JSONObject> existingMap = convertToMap(getNodesBySysmlids(sysmlids, false, true));

        for (int i = 0; i < elements.length(); i++) {
            JSONObject o = elements.getJSONObject(i);
            String sysmlid = o.optString(Sjm.SYSMLID, null);
            if (sysmlid == null || sysmlid.equals("")) {
                sysmlid = createId();
                o.put(Sjm.SYSMLID, sysmlid);
            }

            String content = o.toString();
            if (isImageData(content)) {
                content = extractAndReplaceImageData(content, organization);
                o = new JSONObject(content);
            }

            boolean added = !existingMap.containsKey(sysmlid);
            boolean updated = false;
            if (!added) {
                if (!overwriteJson) {
                    diffUpdateJson(o, existingMap.get(sysmlid));
                    updated = isUpdated(o, existingMap.get(sysmlid));
                } else {
                    updated = true;
                }
            }

            if (!added && !updated) {
                rejectedElements.put(o);
            }

            // pregenerate the elasticId
            o.put(Sjm.ELASTICID, UUID.randomUUID().toString());
            o.put(Sjm.COMMITID, commitId);
            o.put(Sjm.PROJECTID, this.projectId);
            o.put(Sjm.REFID, this.workspaceName);
            o.put(Sjm.INREFIDS, new JSONArray().put(this.workspaceName));
            o.put(Sjm.MODIFIER, user);
            o.put(Sjm.MODIFIED, date);

            if (o.has(Sjm.QUALIFIEDID)) {
                o.remove(Sjm.QUALIFIEDID);
            }
            if (o.has(Sjm.QUALIFIEDNAME)) {
                o.remove(Sjm.QUALIFIEDNAME);
            }

            if (!o.has(Sjm.OWNERID) || o.getString(Sjm.OWNERID) == null || o.getString(Sjm.OWNERID)
                .equalsIgnoreCase("null")) {
                o.put(Sjm.OWNERID, holdingBinSysmlid);
            }
            reorderChildViews(o, newElements, addedElements, updatedElements, deletedElements, commitAdded,
                commitUpdated, commitDeleted, commitId, user, date, oldElasticIds);

            if (added) {
                logger.debug("ELEMENT ADDED!");
                o.put(Sjm.CREATOR, user);
                o.put(Sjm.CREATED, date);
                addedElements.put(o);

                JSONObject newObj = new JSONObject();
                newObj.put(Sjm.SYSMLID, o.getString(Sjm.SYSMLID));
                newObj.put(Sjm.ELASTICID, o.getString(Sjm.ELASTICID));
                commitAdded.put(newObj);
            } else if (updated) {
                logger.debug("ELEMENT UPDATED!");
                updatedElements.put(o);

                JSONObject parent = new JSONObject();
                parent.put("previousElasticId", existingMap.get(sysmlid).getString(Sjm.ELASTICID));
                oldElasticIds.add(existingMap.get(sysmlid).getString(Sjm.ELASTICID));
                parent.put(Sjm.SYSMLID, sysmlid);
                parent.put(Sjm.ELASTICID, o.getString(Sjm.ELASTICID));
                commitUpdated.put(parent);
            } else {
                logger.debug("ELEMENT CONFLICT!");
            }

            newElements.put(o);
        }

        result.put("addedElements", addedElements);
        result.put("updatedElements", updatedElements);
        result.put("newElements", newElements);
        result.put("deletedElements", deletedElements);
        result.put("rejectedElements", rejectedElements);

        commit.put("added", commitAdded);
        commit.put("updated", commitUpdated);
        commit.put("deleted", commitDeleted);
        commit.put(Sjm.CREATOR, user);
        commit.put(Sjm.CREATED, date);
        commit.put(Sjm.PROJECTID, projectId);
        commit.put(Sjm.SOURCE, src);


        result.put("commit", commit);

        return result;
    }

    public void updateElasticRemoveRefs(Set<String> elasticIds) {
        try {
            String payload = new JSONObject().put("script", new JSONObject().put("inline",
                "if(ctx._source.containsKey(\"" + Sjm.INREFIDS + "\")){ctx._source." + Sjm.INREFIDS
                    + ".removeAll([params.refId])}").put("params", new JSONObject().put("refId", this.workspaceName)))
                .toString();
            eh.bulkUpdateElements(elasticIds, payload, projectId);
        } catch (IOException ex) {
            // This catch left intentionally blank
        }
    }

    public void deleteRef(String refId) {
        pgh.deleteRef(refId);
    }

    public boolean isTag() {
        return pgh.isTag(this.workspaceName);
    }

    public JSONObject addChildViews(JSONObject o) {
        boolean isView = false;
        if (o.has(Sjm.SYSMLID)) {
            JSONArray typeArray = o.optJSONArray(Sjm.APPLIEDSTEREOTYPEIDS);
            if (typeArray != null) {
                for (int i = 0; i < typeArray.length(); i++) {
                    String typeJson = typeArray.optString(i);
                    if (Sjm.STEREOTYPEIDS.containsKey(typeJson) && (Sjm.STEREOTYPEIDS.get(typeJson)
                        .matches("view|document"))) {
                        isView = true;
                    }
                }
            }
        }
        if (isView) {
            JSONArray childViews = new JSONArray();
            JSONArray ownedAttributes = o.optJSONArray(Sjm.OWNEDATTRIBUTEIDS);
            Set<String> ownedAttributeSet = new HashSet<>();
            if (ownedAttributes != null && ownedAttributes.length() > 0) {
                for (int j = 0; j < ownedAttributes.length(); j++) {
                    ownedAttributeSet.add(ownedAttributes.getString(j));
                }
            }

            JSONArray ownedAttributesJSON = getNodesBySysmlids(ownedAttributeSet);
            Map<String, JSONObject> ownedAttributesMap = new HashMap<>();
            for (int i = 0; i < ownedAttributesJSON.length(); i++) {
                JSONObject ownedAttribute = ownedAttributesJSON.optJSONObject(i);
                ownedAttributesMap.put(ownedAttribute.getString(Sjm.SYSMLID), ownedAttribute);
            }
            if (ownedAttributes != null && ownedAttributes.length() > 0) {
                for (int j = 0; j < ownedAttributes.length(); j++) {
                    if (ownedAttributesMap.containsKey(ownedAttributes.getString(j))) {
                        JSONObject ownedAttribute = ownedAttributesMap.get(ownedAttributes.getString(j));
                        if (ownedAttribute != null && ownedAttribute.getString(Sjm.TYPE).equals("Property")) {
                            if (ownedAttribute.optString(Sjm.TYPEID, null) != null) {
                                JSONObject childView = new JSONObject();
                                childView.put(Sjm.SYSMLID, ownedAttribute.getString(Sjm.TYPEID));
                                childView.put(Sjm.AGGREGATION, ownedAttribute.getString(Sjm.AGGREGATION));
                                childViews.put(childView);
                            }
                        }
                    }
                }
            }
            o.put(Sjm.CHILDVIEWS, childViews);
        }
        return o;
    }

    private void reorderChildViews(JSONObject element, JSONArray newElements, JSONArray addedElements,
        JSONArray updatedElements, JSONArray deletedElements, JSONArray commitAdded, JSONArray commitUpdated,
        JSONArray commitDeleted, String commitId, String creator, String now, Set<String> oldElasticIds) {

        if (!element.has(Sjm.CHILDVIEWS)) {
            return;
        }

        String sysmlId = element.optString(Sjm.SYSMLID);
        Set<DbNodeTypes> dbnt = new HashSet<>();
        dbnt.add(DbNodeTypes.PACKAGE);
        String ownerParentPackage = pgh.getImmediateParentOfType(sysmlId, DbEdgeTypes.CONTAINMENT, dbnt);

        JSONObject oldElement = getNodeBySysmlid(sysmlId);

        JSONArray oldOwnedAttributes = oldElement.optJSONArray(Sjm.OWNEDATTRIBUTEIDS);
        JSONArray newChildViews = element.optJSONArray(Sjm.CHILDVIEWS);

        JSONArray ownedAttributes;
        JSONArray ownedAttributesIds = new JSONArray();

        Set<String> oldOwnedAttributeSet = new HashSet<>();
        if (oldOwnedAttributes != null && oldOwnedAttributes.length() > 0) {
            for (int i = 0; i < oldOwnedAttributes.length(); i++) {
                oldOwnedAttributeSet.add(oldOwnedAttributes.getString(i));
            }
        }

        Set<String> newChildViewsSet = new HashSet<>();
        if (newChildViews != null && newChildViews.length() > 0) {
            for (int i = 0; i < newChildViews.length(); i++) {
                if (newChildViews.optJSONObject(i) != null
                    && newChildViews.optJSONObject(i).optString(Sjm.SYSMLID, null) != null) {
                    newChildViewsSet.add(newChildViews.optJSONObject(i).optString(Sjm.SYSMLID));
                }
            }
        }

        ownedAttributes = getNodesBySysmlids(oldOwnedAttributeSet);

        Map<String, String> createProps = new HashMap<>();
        List<String> notAViewList = new ArrayList<>();
        JSONObject mountJson = null;
        for (int i = 0; i < ownedAttributes.length(); i++) {
            JSONObject ownedAttribute = ownedAttributes.optJSONObject(i);
            if (ownedAttribute != null && ownedAttribute.getString(Sjm.TYPE).equals("Property")) {
                if (ownedAttribute.optString(Sjm.TYPEID, null) != null) {
                    if (!newChildViewsSet.contains(ownedAttribute.getString(Sjm.TYPEID))) {
                        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(ownedAttribute.optString(Sjm.PROJECTID),
                            ownedAttribute.optString(Sjm.REFID));
                        JSONArray childViews = new JSONArray();
                        Set<String> childViewsSet = new HashSet<>();
                        childViewsSet.add(ownedAttribute.getString(Sjm.TYPEID));
                        try {
                            if (mountJson == null) {
                                mountJson = getProjectWithFullMounts(ownedAttribute.optString(Sjm.PROJECTID),
                                    ownedAttribute.optString(Sjm.REFID), null);
                            }
                            handleMountSearch(mountJson, false, false, 0L, childViewsSet, childViews);
                        } catch (Exception e) {
                            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
                        }

                        if (childViews.length() > 0) {
                            for (int j = 0; j < childViews.length(); j++) {
                                JSONObject childView = childViews.optJSONObject(j);

                                JSONArray appliedStereotypeIds = childView.optJSONArray(Sjm.APPLIEDSTEREOTYPEIDS);
                                String asids = (appliedStereotypeIds == null) ? "" : appliedStereotypeIds.toString();
                                if (asids.contains("_17_0_1_232f03dc_1325612611695_581988_21583") || asids
                                    .contains("_17_0_2_3_87b0275_1371477871400_792964_43374") || asids
                                    .contains("_17_0_1_407019f_1332453225141_893756_11936") || asids
                                    .contains("_11_5EAPbeta_be00301_1147420760998_43940_227") || asids
                                    .contains("_18_0beta_9150291_1392290067481_33752_4359")) {
                                    if (childView.optString(Sjm.SYSMLID, null) != null) {
                                        deletedElements.put(ownedAttribute);
                                        oldElasticIds.add(ownedAttribute.getString(Sjm.ELASTICID));
                                        JSONObject newObj = new JSONObject();
                                        newObj.put(Sjm.SYSMLID, ownedAttribute.getString(Sjm.SYSMLID));
                                        newObj.put(Sjm.ELASTICID, ownedAttribute.getString(Sjm.ELASTICID));
                                        commitDeleted.put(newObj);
                                    }
                                    JSONObject asi = emsNodeUtil
                                        .getNodeBySysmlid(ownedAttribute.optString(Sjm.APPLIEDSTEREOTYPEINSTANCEID));
                                    if (asi.optString(Sjm.SYSMLID, null) != null) {
                                        deletedElements.put(asi);
                                        oldElasticIds.add(asi.getString(Sjm.ELASTICID));
                                        JSONObject newObj = new JSONObject();
                                        newObj.put(Sjm.SYSMLID, asi.getString(Sjm.SYSMLID));
                                        newObj.put(Sjm.ELASTICID, asi.getString(Sjm.ELASTICID));
                                        commitDeleted.put(newObj);
                                    }
                                    JSONObject association =
                                        emsNodeUtil.getNodeBySysmlid(ownedAttribute.optString(Sjm.ASSOCIATIONID));
                                    if (association.optString(Sjm.SYSMLID, null) != null) {
                                        deletedElements.put(association);
                                        oldElasticIds.add(association.getString(Sjm.ELASTICID));
                                        JSONObject newObj = new JSONObject();
                                        newObj.put(Sjm.SYSMLID, association.getString(Sjm.SYSMLID));
                                        newObj.put(Sjm.ELASTICID, association.getString(Sjm.ELASTICID));
                                        commitDeleted.put(newObj);
                                    }
                                    JSONArray associationProps = association.optJSONArray(Sjm.OWNEDENDIDS);
                                    for (int k = 0; k < associationProps.length(); k++) {
                                        if (associationProps.optString(k, null) != null) {
                                            JSONObject assocProp =
                                                emsNodeUtil.getNodeBySysmlid(associationProps.optString(k));
                                            if (assocProp.optString(Sjm.SYSMLID, null) != null) {
                                                deletedElements.put(assocProp);
                                                oldElasticIds.add(assocProp.getString(Sjm.ELASTICID));
                                                JSONObject newObj = new JSONObject();
                                                newObj.put(Sjm.SYSMLID, assocProp.getString(Sjm.SYSMLID));
                                                newObj.put(Sjm.ELASTICID, assocProp.getString(Sjm.ELASTICID));
                                                commitDeleted.put(newObj);
                                            }
                                        }
                                    }
                                } else {
                                    notAViewList.add(ownedAttribute.getString(Sjm.SYSMLID));
                                }
                            }
                        }
                    } else {
                        createProps.put(ownedAttribute.getString(Sjm.TYPEID), ownedAttribute.getString(Sjm.SYSMLID));
                    }
                } else {
                    notAViewList.add(ownedAttribute.getString(Sjm.SYSMLID));
                }
            }
        }

        if (newChildViews != null && newChildViews.length() > 0) {
            for (int i = 0; i < newChildViews.length(); i++) {
                JSONObject child = newChildViews.getJSONObject(i);
                if (child.has(Sjm.SYSMLID)) {
                    if (createProps.containsKey(child.getString(Sjm.SYSMLID))) {
                        if (!ownedAttributesIds.toString().contains(createProps.get(child.getString(Sjm.SYSMLID)))) {
                            ownedAttributesIds.put(createProps.get(child.getString(Sjm.SYSMLID)));
                        }
                    } else {
                        String cvSysmlId = child.getString(Sjm.SYSMLID);
                        String aggregation = child.getString(Sjm.AGGREGATION);

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
                        asid.put(alterIdAggregationType(aggregation));
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
                        property.put(Sjm.INREFIDS, new JSONArray().put(this.workspaceName));
                        property.put(Sjm.PROJECTID, this.projectId);
                        property.put(Sjm.REFID, this.workspaceName);
                        property.put(Sjm.COMMITID, commitId);
                        property.put(Sjm.CREATOR, creator);
                        property.put(Sjm.CREATED, now);
                        property.put(Sjm.MODIFIER, creator);
                        property.put(Sjm.MODIFIED, now);

                        newElements.put(property);
                        addedElements.put(property);
                        JSONObject newProperty = new JSONObject();
                        newProperty.put(Sjm.SYSMLID, property.getString(Sjm.SYSMLID));
                        newProperty.put(Sjm.ELASTICID, property.getString(Sjm.ELASTICID));
                        commitAdded.put(newProperty);

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
                        propertyASI.put(Sjm.ELASTICID, UUID.randomUUID().toString());
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
                        classifierids.put(alterIdAggregationType(aggregation));
                        propertyASI.put(Sjm.CLASSIFIERIDS, classifierids);
                        propertyASI.put(Sjm.STEREOTYPEDELEMENTID, propertySysmlId);
                        propertyASI.put(Sjm.INREFIDS, new JSONArray().put(this.workspaceName));
                        propertyASI.put(Sjm.PROJECTID, this.projectId);
                        propertyASI.put(Sjm.REFID, this.workspaceName);
                        propertyASI.put(Sjm.COMMITID, commitId);
                        propertyASI.put(Sjm.CREATOR, creator);
                        propertyASI.put(Sjm.CREATED, now);
                        propertyASI.put(Sjm.MODIFIER, creator);
                        propertyASI.put(Sjm.MODIFIED, now);

                        newElements.put(propertyASI);
                        addedElements.put(propertyASI);
                        JSONObject newASI = new JSONObject();
                        newASI.put(Sjm.SYSMLID, property.getString(Sjm.SYSMLID));
                        newASI.put(Sjm.ELASTICID, property.getString(Sjm.ELASTICID));
                        commitAdded.put(newASI);

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
                        association.put(Sjm.INREFIDS, new JSONArray().put(this.workspaceName));
                        association.put(Sjm.PROJECTID, this.projectId);
                        association.put(Sjm.REFID, this.workspaceName);
                        association.put(Sjm.COMMITID, commitId);
                        association.put(Sjm.CREATOR, creator);
                        association.put(Sjm.CREATED, now);
                        association.put(Sjm.MODIFIER, creator);
                        association.put(Sjm.MODIFIED, now);

                        newElements.put(association);
                        addedElements.put(association);
                        JSONObject newAssociation = new JSONObject();
                        newAssociation.put(Sjm.SYSMLID, property.getString(Sjm.SYSMLID));
                        newAssociation.put(Sjm.ELASTICID, property.getString(Sjm.ELASTICID));
                        commitAdded.put(newAssociation);

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
                        assocProperty.put(Sjm.INREFIDS, new JSONArray().put(this.workspaceName));
                        assocProperty.put(Sjm.PROJECTID, this.projectId);
                        assocProperty.put(Sjm.REFID, this.workspaceName);
                        assocProperty.put(Sjm.COMMITID, commitId);
                        assocProperty.put(Sjm.CREATOR, creator);
                        assocProperty.put(Sjm.CREATED, now);
                        assocProperty.put(Sjm.MODIFIER, creator);
                        assocProperty.put(Sjm.MODIFIED, now);

                        newElements.put(assocProperty);
                        addedElements.put(assocProperty);
                        JSONObject newAssociationProperty = new JSONObject();
                        newAssociationProperty.put(Sjm.SYSMLID, property.getString(Sjm.SYSMLID));
                        newAssociationProperty.put(Sjm.ELASTICID, property.getString(Sjm.ELASTICID));
                        commitAdded.put(newAssociationProperty);

                        ownedAttributesIds.put(propertySysmlId);
                    }
                }
            }
        }

        for (String id : notAViewList) {
            ownedAttributesIds.put(id);
        }

        element.put(Sjm.OWNEDATTRIBUTEIDS, ownedAttributesIds);
        element.remove(Sjm.CHILDVIEWS);
    }

    public String alterIdAggregationType(String aggregationType) {
        if (aggregationType.equals("none")) {
            return "_15_0_be00301_1199378032543_992832_3096";
        } else if (aggregationType.equals("shared")) {
            return "_15_0_be00301_1199378020836_340320_3071";
        } else {
            return "_15_0_be00301_1199377756297_348405_2678";
        }
    }

    public Map<String, String> getGuidAndTimestampFromElasticId(String elasticid) {
        return pgh.getCommitAndTimestamp("elasticId", elasticid);
    }

    public Long getTimestampFromElasticId(String elasticid) {
        return pgh.getTimestamp("elasticId", elasticid);
    }

    public JSONObject getElementByElasticID(String elasticId) {
        try {
            return eh.getElementByElasticId(elasticId, projectId);
        } catch (IOException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return null;
    }

    private Map<String, JSONObject> convertToMap(JSONArray elements) {
        Map<String, JSONObject> result = new HashMap<>();
        for (int i = 0; i < elements.length(); i++) {
            if (elements.getJSONObject(i).optString(Sjm.SYSMLID, null) != null) {
                result.put(elements.getJSONObject(i).getString(Sjm.SYSMLID), elements.getJSONObject(i));
            }
        }

        return result;
    }

    private Map<String, Map<String, String>> calculateQualifiedInformation(JSONArray elements) {
        Map<String, Map<String, String>> result = new HashMap<>();
        Map<String, JSONObject> sysmlid2elements = getSysmlMap(elements);
        Map<String, JSONObject> cache = new HashMap<>();
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            String sysmlid = element.getString(Sjm.SYSMLID);
            Map<String, String> extendedInfo = getQualifiedInformationForElement(element, sysmlid2elements, cache);

            Map<String, String> attrs = new HashMap<>();
            attrs.put(Sjm.QUALIFIEDNAME, extendedInfo.get(Sjm.QUALIFIEDNAME));
            attrs.put(Sjm.QUALIFIEDID, extendedInfo.get(Sjm.QUALIFIEDID));
            attrs.put(Sjm.SITECHARACTERIZATIONID, extendedInfo.get(Sjm.SITECHARACTERIZATIONID));

            result.put(sysmlid, attrs);
        }

        return result;
    }

    private Map<String, String> getQualifiedInformationForElement(JSONObject element,
        Map<String, JSONObject> elementMap, Map<String, JSONObject> cache) {

        Map<String, String> result = new HashMap<>();

        JSONObject o = element;
        ArrayList<String> qn = new ArrayList<>();
        ArrayList<String> qid = new ArrayList<>();
        String sqn;
        String sqid;
        String siteCharacterizationId = null;
        qn.add(o.optString("name"));
        qid.add(o.optString(Sjm.SYSMLID));

        while (o.has(Sjm.OWNERID) && o.optString(Sjm.OWNERID, null) != null && !o.getString(Sjm.OWNERID)
            .equals("null")) {
            String sysmlid = o.optString(Sjm.OWNERID);
            JSONObject owner = elementMap.get(sysmlid);
            if (owner == null) {
                if (cache.containsKey(sysmlid)) {
                    owner = cache.get(sysmlid);
                } else {
                    owner = getNodeBySysmlid(sysmlid, this.workspaceName, false);
                    cache.put(sysmlid, owner);
                }
            }

            String ownerId = owner.optString(Sjm.SYSMLID);
            qid.add(ownerId);

            String ownerName = owner.optString(Sjm.NAME);
            qn.add(ownerName);

            if (siteCharacterizationId == null && CommitUtil.isSite(owner)) {
                siteCharacterizationId = owner.optString(Sjm.SYSMLID);
            }
            o = owner;
        }

        Collections.reverse(qn);
        Collections.reverse(qid);

        sqn = "/" + String.join("/", qn);
        sqid = "/" + String.join("/", qid);


        result.put(Sjm.QUALIFIEDNAME, sqn);
        result.put(Sjm.QUALIFIEDID, sqid);
        result.put(Sjm.SITECHARACTERIZATIONID, siteCharacterizationId);

        return result;
    }

    public Version imageVersionBeforeTimestamp(NavigableMap<Long, Version> versions, Long timestamp) {
        // finds entry with the greatest key less than or equal to key, or null if it does not exist
        Map.Entry<Long, Version> nearestDate = versions.floorEntry(timestamp);
        if (nearestDate != null) {
            return nearestDate.getValue();
        }
        // ClassCastException - if the specified key cannot be compared with the keys currently in the map
        // NullPointerException - if the specified key is null and this map does not permit null keys
        return null;
    }

    public Pair<String, Long> getDirectParentRef(String refId) {
        return pgh.getParentRef(refId);
    }

    public boolean isDeleted(String sysmlid) {
        return pgh.isDeleted(sysmlid);
    }

    public boolean orgExists(String orgName) {
        return orgName.equals("swsdp") || pgh.orgExists(orgName);
    }

    public boolean refExists(String refId) {
        return pgh.refExists(refId);
    }

    private boolean diffUpdateJson(JSONObject json, JSONObject existing) {
        if (json.has(Sjm.SYSMLID) && existing.has(Sjm.SYSMLID)) {
            String jsonModified = json.optString(Sjm.MODIFIED);
            String existingModified = existing.optString(Sjm.MODIFIED);
            if (!jsonModified.isEmpty()) {
                try {
                    Date jsonModDate = df.parse(jsonModified);
                    Date existingModDate = df.parse(existingModified);
                    if (jsonModDate.before(existingModDate)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Conflict Detected");
                        }
                        return false;
                    }
                } catch (ParseException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
                    }
                }
            }
            return mergeJson(json, existing);
        }
        return false;
    }

    private boolean mergeJson(JSONObject partial, JSONObject original) {
        if (original == null) {
            return false;
        }

        for (String attr : JSONObject.getNames(original)) {
            if (!partial.has(attr)) {
                partial.put(attr, original.get(attr));
            }
        }
        return true;
    }

    private boolean isUpdated(JSONObject json, JSONObject existing) {
        if (existing == null) {
            return false;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("New Element: " + json);
            logger.debug("Old Element: " + existing);
        }

        Map<String, Object> newElement = toMap(json);
        Map<String, Object> oldElement = toMap(existing);

        return !isEquivalent(newElement, oldElement);
    }

    public JSONArray addExtendedInformation(JSONArray elements) {
        JSONArray newElements = new JSONArray();

        Map<String, Map<String, String>> sysmlid2qualified = calculateQualifiedInformation(elements);

        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);

            JSONObject newElement = addExtendedInformationForElement(element, sysmlid2qualified);
            newElements.put(newElement);
        }

        return newElements.length() >= elements.length() ? newElements : elements;
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

    public static void handleMountSearch(JSONObject mountsJson, boolean extended, boolean extraDocs,
        final Long maxDepth, Set<String> elementsToFind, JSONArray result) throws IOException {

        if (elementsToFind.isEmpty() || mountsJson == null) {
            return;
        }
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(mountsJson.getString(Sjm.SYSMLID), mountsJson.getString(Sjm.REFID));
        JSONArray nodeList = emsNodeUtil.getNodesBySysmlids(elementsToFind);
        Set<String> foundElements = new HashSet<>();
        JSONArray curFound = new JSONArray();
        for (int index = 0; index < nodeList.length(); index++) {
            String id = nodeList.getJSONObject(index).getString(Sjm.SYSMLID);
            if (maxDepth != 0) {
                JSONArray children = emsNodeUtil.getChildren(id, maxDepth);
                for (int i = 0; i < children.length(); i++) {
                    String cid = children.getJSONObject(i).getString(Sjm.SYSMLID);
                    if (foundElements.contains(cid)) {
                        continue;
                    }
                    curFound.put(children.getJSONObject(i));
                    foundElements.add(cid);
                }
            } else {
                curFound.put(nodeList.getJSONObject(index));
                foundElements.add(id);
            }
        }
        curFound = extended ?
            emsNodeUtil
                .addExtendedInformation(extraDocs ? emsNodeUtil.addExtraDocs(curFound) : curFound) :
            (extraDocs ? emsNodeUtil.addExtraDocs(curFound) : curFound);
        for (int i = 0; i < curFound.length(); i++) {
            result.put(curFound.get(i));
        }
        elementsToFind.removeAll(foundElements);
        if (elementsToFind.isEmpty()) {
            return;
        }
        if (!mountsJson.has(Sjm.MOUNTS)) {
            mountsJson = emsNodeUtil
                .getProjectWithFullMounts(mountsJson.getString(Sjm.SYSMLID), mountsJson.getString(Sjm.REFID), null);
        }
        JSONArray mountsArray = mountsJson.getJSONArray(Sjm.MOUNTS);

        for (int i = 0; i < mountsArray.length(); i++) {
            handleMountSearch(mountsArray.getJSONObject(i), extended, extraDocs, maxDepth, elementsToFind, result);
        }
    }

    public List<Node> getSites(boolean sites, boolean sitepackages) {
        return pgh.getSites(sites, sitepackages);
    }

    public JSONObject getCommitObject(String commitId) {
        try {
            return eh.getCommitByElasticId(commitId, projectId);
        } catch (IOException e) {
            logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return null;
    }

    public void insertProjectIndex(String projectId) {
        try {
            eh.createIndex(projectId);
        } catch (IOException e) {
            logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public String insertSingleElastic(JSONObject o) {
        try {
            ElasticResult r = eh.indexElement(o, projectId);
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

    private static Map<String, Object> toMap(JSONObject object) {
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

    private static List<Object> toList(JSONArray array) {
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

    private static boolean isEquivalent(Map<String, Object> map1, Map<String, Object> map2) {
        for (Map.Entry<String, Object> entry : map1.entrySet()) {
            Object value1 = entry.getValue();
            Object value2 = map2.get(entry.getKey());
            if (logger.isDebugEnabled()) {
                logger.debug("Value 1: " + value1);
                logger.debug("Value 2: " + value2);
            }
            if (value1 instanceof Map) {
                if (!(value2 instanceof Map)) {
                    return false;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Is Equivalent: " + isEquivalent((Map<String, Object>) value1,
                            (Map<String, Object>) value2));
                    }
                    if (!isEquivalent((Map<String, Object>) value1, (Map<String, Object>) value2))
                        return false;
                }
            } else if (value1 instanceof List) {
                if (!(value2 instanceof List)) {
                    return false;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Is Equivalent: " + isEquivalent((List<Object>) value1, (List<Object>) value2));
                    }
                    if (!isEquivalent((List<Object>) value1, (List<Object>) value2))
                        return false;
                }
            } else if (value1 instanceof String) {
                if (!(value2 instanceof String)) {
                    return false;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Is Equivalent: " + value1.equals(value2));
                    }
                    if (!value1.equals(value2))
                        return false;
                }
            } else if (value1 instanceof Boolean) {
                if (!(value2 instanceof Boolean)) {
                    return false;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Is Equivalent: " + value1 != value2);
                    }
                    if (value1 != value2)
                        return false;
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Value 1 Type: " + value1.getClass());
                    logger.debug("Value 2 Type: " + value2.getClass());
                }
                return false;
            }
        }

        return true;
    }

    private static boolean isEquivalent(List<Object> list1, List<Object> list2) {
        if (list1.size() != list2.size()) {
            List<Object> tester;
            List<Object> testing;
            if (list1.size() > list2.size()) {
                testing = list1;
                tester = list2;
            } else {
                testing = list2;
                tester = list1;
            }

            for (Object element : testing) {
                if (!tester.contains(element)) {
                    return false;
                }
            }
            return true;
        }

        for (Object element : list1) {
            if (!list2.contains(element)) {
                return false;
            }
        }
        return true;
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

    public String getImmediateParentOfTypes(String sysmlId, DbEdgeTypes edgeType, Set<DbNodeTypes> nodeTypes) {
        return pgh.getImmediateParentOfType(sysmlId, edgeType, nodeTypes);
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

    private String extractAndReplaceImageData(String value, String siteName) {
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
                EmsScriptNode artNode = NodeUtil
                    .updateOrCreateArtifact(name, extension, content, null, siteName, projectId, this.workspaceName,
                        null, null, null, false);
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

    public JSONObject getModelAtCommit(String commitId) {
        JSONObject result = new JSONObject();
        JSONObject pastElement = null;
        JSONArray elements = new JSONArray();
        ArrayList<String> refsCommitsIds = new ArrayList<>();

        Map<String, Object> commit = pgh.getCommit(commitId);
        if (commit != null) {
            String refId = commit.get(Sjm.REFID).toString();

            List<Map<String, Object>> refsCommits = pgh.getRefsCommits(refId, (int) commit.get(Sjm.SYSMLID));
            for (Map<String, Object> ref : refsCommits) {
                refsCommitsIds.add((String) ref.get(Sjm.SYSMLID));
            }

            Map<String, String> deletedElementIds = eh.getDeletedElementsFromCommits(refsCommitsIds, projectId);

            for (Map<String, Object> n : pgh.getAllNodesWithLastCommitTimestamp()) {
                if (((Date) n.get(Sjm.TIMESTAMP)).getTime() <= ((Date) commit.get(Sjm.TIMESTAMP)).getTime()) {
                    try {
                        if (!deletedElementIds.containsKey((String) n.get(Sjm.ELASTICID))) {
                            pastElement = eh.getElementByCommitId((String) n.get(PostgresHelper.LASTCOMMIT),
                                (String) n.get(Sjm.SYSMLID), projectId);
                        }
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                } else {
                    pastElement = getElementAtCommit((String) n.get(Sjm.SYSMLID), commitId, refsCommitsIds);
                }

                if (pastElement != null && pastElement.has(Sjm.SYSMLID) && !deletedElementIds
                    .containsKey(pastElement.getString(Sjm.ELASTICID))) {
                    elements.put(pastElement);
                }

                // Reset to null so if there is an exception it doesn't add a duplicate
                pastElement = null;
            }
            result.put(Sjm.ELEMENTS, elements);
        }
        return result;
    }

    /**
     * Method will take a sysmlId and search for it at a specific commit. If the element is not found at the current
     * commit then it will find the previous commit (in the same branch or it's parent) and search for the element at
     * that point. Repeats the process it finds the element.
     *
     * @param sysmlId
     * @param commitId
     * @return Element JSON
     */
    public JSONObject getElementAtCommit(String sysmlId, String commitId) {
        JSONObject pastElement = null;
        Map<String, Object> commit = pgh.getCommit(commitId);
        ArrayList<String> refsCommitsIds = new ArrayList<>();
        if (commit != null) {
            String refId = commit.get(Sjm.REFID).toString();
            List<Map<String, Object>> refsCommits =
                pgh.getRefsCommits(refId, Integer.parseInt(commit.get(Sjm.SYSMLID).toString()));

            for (Map<String, Object> ref : refsCommits) {
                refsCommitsIds.add((String) ref.get(Sjm.SYSMLID));
            }

            Map<String, String> deletedElementIds = eh.getDeletedElementsFromCommits(refsCommitsIds, projectId);

            pastElement = getElementAtCommit(sysmlId, commitId, refsCommitsIds);

            if (pastElement != null && pastElement.has(Sjm.SYSMLID) && deletedElementIds
                .containsKey(pastElement.getString(Sjm.ELASTICID))) {
                pastElement = new JSONObject();
            }
        }
        return pastElement == null ? new JSONObject() : pastElement;
    }

    public JSONArray getNearestCommitFromTimestamp(String timestamp, JSONArray commits) {
        Date requestedTime = null;
        try {
            requestedTime = requestedTime = df.parse(timestamp);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < commits.length(); i++) {
            JSONObject current = commits.getJSONObject(i);
            Date currentTime;
            try {
                currentTime = df.parse(current.getString(Sjm.CREATED));
                if (requestedTime.getTime() >= currentTime.getTime()) {
                    return new JSONArray().put(current);
                }
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
                }
            }
        }
        return new JSONArray();
    }

    public JSONObject getElementAtCommit(String sysmlId, String commitId, List<String> refIds) {
        JSONObject result = new JSONObject();

        try {
            // Get commit object and retrieve the refs commits
            Map<String, Object> commit = pgh.getCommit(commitId);

            Date date = (Date) commit.get(Sjm.TIMESTAMP);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(date.getTime());
            cal.setTimeZone(TimeZone.getTimeZone("GMT"));
            String timestamp = df.format(cal.getTime());

            // Search for element at commit
            result = eh.getElementsLessThanOrEqualTimestamp(sysmlId, timestamp, refIds, projectId);

        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return result;
    }
}
