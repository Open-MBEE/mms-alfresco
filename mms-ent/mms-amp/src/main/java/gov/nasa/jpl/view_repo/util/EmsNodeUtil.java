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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

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

    public JsonArray getOrganization(String orgId) {
        JsonArray orgs = new JsonArray();
        List<Map<String, String>> organizations = pgh.getOrganizations(orgId);
        for (Map<String, String> n : organizations) {
            JsonObject org = new JsonObject();
            org.addProperty(Sjm.SYSMLID, n.get(ORG_ID));
            org.addProperty(Sjm.NAME, n.get(ORG_NAME));
            orgs.add(org);
        }
        return orgs;
    }

    public String getOrganizationFromProject(String projectId) {
        return pgh.getOrganizationFromProject(projectId);
    }

    public JsonArray getProjects(String orgId) {
        JsonArray projects = new JsonArray();
        List<Map<String, Object>> orgProjects = pgh.getProjects(orgId);
        for (Map<String, Object> n : orgProjects) {
            switchProject(n.get(Sjm.SYSMLID).toString());
            JsonObject project = getNodeBySysmlid(n.get(Sjm.SYSMLID).toString());
            project.addProperty(ORG_ID, orgId);
            projects.add(project);
        }
        return projects;
    }

    public JsonArray getProjects() {
        JsonArray projects = new JsonArray();
        for (Map<String, Object> project : pgh.getProjects()) {
            switchProject(project.get(Sjm.SYSMLID).toString());
            JsonObject proj = getNodeBySysmlid(project.get(Sjm.SYSMLID).toString());
            proj.addProperty(ORG_ID, project.get(ORG_ID).toString());
            projects.add(proj);
        }
        return projects;
    }

    public JsonObject getProject(String projectId) {
        Map<String, Object> project = pgh.getProject(projectId);
        if (!project.isEmpty() && !project.get(Sjm.SYSMLID).toString().contains("no_project")) {
            switchProject(projectId);
            JsonObject proj = getNodeBySysmlid(projectId);
            proj.addProperty(ORG_ID, project.get(ORG_ID).toString());
            return proj;
        }
        return null;
    }

    public JsonObject getProjectWithFullMounts(String projectId, String refId, List<String> found) {
        List<String> realFound = found;
        if (realFound == null) {
            realFound = new ArrayList<>();
        }
        Map<String, Object> project = pgh.getProject(projectId);

        if (!project.isEmpty() && !project.get(Sjm.SYSMLID).toString().contains("no_project")) {
            switchProject(projectId);
            switchWorkspace(refId);
            JsonObject projectJson = getNodeBySysmlid(projectId);
            projectJson.addProperty(ORG_ID, project.get(ORG_ID).toString());
            realFound.add(projectId);
            JsonArray mountObject = getFullMounts(realFound);
            projectJson.add(Sjm.MOUNTS, mountObject);
            return projectJson;
        }
        return null;
    }

    public JsonArray getFullMounts(List<String> found) {
        JsonArray mounts = new JsonArray();
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
        JsonArray nodeList = getNodesBySysmlids(mountIds);
        for (int i = 0; i < nodeList.size(); i++) {
            JsonObject mountJson = nodeList.get(i).getAsJsonObject();
            if (mountJson.has(Sjm.MOUNTEDELEMENTPROJECTID) && mountJson.has(Sjm.MOUNTEDREFID)) {
                if (found.contains(mountJson.get(Sjm.MOUNTEDELEMENTPROJECTID).getAsString())) {
                    continue;
                }
                JsonObject childProject = getProjectWithFullMounts(
                                mountJson.get(Sjm.MOUNTEDELEMENTPROJECTID).getAsString(),
                                mountJson.get(Sjm.MOUNTEDREFID).getAsString(), found);
                if (childProject != null) {
                    mounts.add(childProject);
                }
            }
        }
        switchProject(curProjectId);
        switchWorkspace(curRefId);
        return mounts;
    }

    public JsonObject getElementByElementAndCommitId(String commitId, String sysmlid) {
        try {
            return eh.getElementByCommitId(commitId, sysmlid, projectId);
        } catch (IOException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return new JsonObject();
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

    public JsonObject getNodeBySysmlid(String sysmlid) {
        return getNodeBySysmlid(sysmlid, this.workspaceName, true);
    }

    /**
     * Retrieves node by sysmlid adding childViews as necessary
     *
     * @param sysmlid       String of sysmlid to look up
     * @param workspaceName Workspace to retrieve id against
     * @return
     */

    private JsonObject getNodeBySysmlid(String sysmlid, String workspaceName, boolean withChildViews) {
        if (!this.workspaceName.equals(workspaceName)) {
            switchWorkspace(workspaceName);
        }

        String elasticId = pgh.getElasticIdFromSysmlId(sysmlid);
        if (elasticId != null) {
            try {
                JsonObject result = eh.getElementByElasticId(elasticId, projectId);
                if (result != null) {
                    result.addProperty(Sjm.PROJECTID, this.projectId);
                    result.addProperty(Sjm.REFID, this.workspaceName);
                    return withChildViews ? addChildViews(result) : result;
                }
            } catch (Exception e) {
                logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }
        return new JsonObject();
    }

    public JsonArray getNodesBySysmlids(Set<String> sysmlids) {
        return getNodesBySysmlids(sysmlids, true, false);
    }

    public JsonArray getNodesBySysmlids(Set<String> sysmlids, boolean withChildViews, boolean withDeleted) {
        List<String> elasticids = pgh.getElasticIdsFromSysmlIds(new ArrayList<>(sysmlids), withDeleted);
        JsonArray elementsFromElastic = new JsonArray();
        try {
            elementsFromElastic = eh.getElementsFromElasticIds(elasticids, projectId);
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        for (int i = 0; i < elementsFromElastic.size(); i++) {
            JsonObject formatted = elementsFromElastic.get(i).getAsJsonObject();
            formatted.addProperty(Sjm.PROJECTID, this.projectId);
            formatted.addProperty(Sjm.REFID, this.workspaceName);
            elementsFromElastic.add(withChildViews ? addChildViews(formatted) : formatted);
        }

        return elementsFromElastic;
    }

    public JsonArray getNodeHistory(String sysmlId) {
        JsonArray nodeHistory = new JsonArray();
        try {
            nodeHistory = filterCommitsByRefs(eh.getCommitHistory(sysmlId, projectId));
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return nodeHistory;
    }

    public JsonArray getRefHistory(String refId) {
        JsonArray result = new JsonArray();
        List<Map<String, Object>> refCommits = pgh.getRefsCommits(refId);
        for (int i = 0; i < refCommits.size(); i++) {
            Map<String, Object> refCommit = refCommits.get(i);
            JsonObject commit = new JsonObject();
            commit.addProperty(Sjm.SYSMLID, refCommit.get(Sjm.SYSMLID).toString());
            commit.addProperty(Sjm.CREATOR, refCommit.get(Sjm.CREATOR).toString());
            commit.addProperty(Sjm.CREATED, df.format(refCommit.get(Sjm.CREATED)));
            result.add(commit);
        }

        return result;
    }

    private JsonArray filterCommitsByRefs(JsonArray commits) {
        JsonArray filtered = new JsonArray();
        JsonArray refHistory = getRefHistory(this.workspaceName);
        List<String> commitList = new ArrayList<>();
        for (int i = 0; i < refHistory.size(); i++) {
            commitList.add(refHistory.get(i).getAsJsonObject().get(Sjm.SYSMLID).getAsString());
        }
        for (int i = 0; i < commits.size(); i++) {
            if (commitList.contains(commits.get(i).getAsJsonObject().get(Sjm.SYSMLID).getAsString())) {
                filtered.add(commits.get(i).getAsJsonObject());
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

    public JsonObject getRefJson(String refId) {
        JsonObject jObj = null;
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

    public JsonArray getRefsJson() {
        JsonArray result = null;
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

    public JsonArray getChildren(String sysmlid) {
        return getChildren(sysmlid, DbEdgeTypes.CONTAINMENT, null);
    }

    public JsonArray getChildren(String sysmlid, final Long maxDepth) {
        return getChildren(sysmlid, DbEdgeTypes.CONTAINMENT, maxDepth);
    }

    public JsonArray getChildrenIds(String sysmlid, DbEdgeTypes dbEdge, final Long maxDepth) {
        JsonArray children = new JsonArray();
        int depth = maxDepth == null ? 100000 : maxDepth.intValue();

        for (Pair<String, String> childId : pgh.getChildren(sysmlid, dbEdge, depth)) {
            children.add(childId.first);
        }
        return children;
    }

    public JsonArray getChildren(String sysmlid, DbEdgeTypes dbEdge, final Long maxDepth) {
        Set<String> children = new HashSet<>();

        int depth = maxDepth == null ? 100000 : maxDepth.intValue();

        for (Pair<String, String> childId : pgh.getChildren(sysmlid, dbEdge, depth)) {
            children.add(childId.second);
        }

        try {
            List<String> childrenList = new ArrayList<>(children);
            JsonArray childs = eh.getElementsFromElasticIds(childrenList, projectId);
            JsonArray result = new JsonArray();
            for (int i = 0; i < childs.size(); i++) {
                JsonObject current = childs.get(i).getAsJsonObject();
                current.addProperty(Sjm.PROJECTID, this.projectId);
                current.addProperty(Sjm.REFID, this.workspaceName);
                JsonObject withChildViews = addChildViews(current);
                result.add(withChildViews);
            }
            return result;
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return new JsonArray();
    }

    public JsonArray search(JsonObject query) {
        try {
            return eh.search(query);
        } catch (IOException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return new JsonArray();
    }

    public JsonArray addExtraDocs(JsonArray elements) {
        JsonArray results = new JsonArray();
        for (int i = 0; i < elements.size(); i++) {
            JsonObject element = elements.get(i).getAsJsonObject();
            String elementSysmlId = element.get(Sjm.SYSMLID).getAsString();
            JsonArray relatedDocuments = new JsonArray();
            Map<String, List<JsonObject>> relatedDocumentsMap = new HashMap<>();

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
                        JsonObject o = new JsonObject();
                        o.addProperty(Sjm.SYSMLID, parentView.first);
                        relatedDocumentsMap.get(doc.first).add(o);
                        docView.get(doc.first).add(parentView.first);
                    } else {
                        docView.put(doc.first, new HashSet<String>());
                        List<JsonObject> viewParents = new ArrayList<>();
                        JsonObject o = new JsonObject();
                        o.addProperty(Sjm.SYSMLID, parentView.first);
                        viewParents.add(o);
                        docView.get(doc.first).add(parentView.first);
                        relatedDocumentsMap.put(doc.first, viewParents);
                    }
                }
            }
            Iterator<Map.Entry<String, List<JsonObject>>> it = relatedDocumentsMap.entrySet().iterator();
            it.forEachRemaining(pair -> {
                JsonArray viewIds = new JsonArray();
                for (JsonObject value : pair.getValue()) {
                    viewIds.add(value);
                }
                JsonObject relatedDocObject = new JsonObject();
                relatedDocObject.addProperty(Sjm.SYSMLID, pair.getKey());
                relatedDocObject.add(Sjm.PARENTVIEWS, viewIds);
                relatedDocObject.addProperty(Sjm.PROJECTID, this.projectId);
                relatedDocObject.addProperty(Sjm.REFID, this.workspaceName);
                relatedDocuments.add(relatedDocObject);
            });
            element.add(Sjm.RELATEDDOCUMENTS, relatedDocuments);

            results.add(element);
        }

        return results;
    }

    /**
     * Get the documents that exist in a site at a specified time or get the docs by groupId
     *
     * @param sysmlId Site to filter documents against
     * @return JSONArray of the documents in the site
     */
    public JsonArray getDocJson(String sysmlId, int depth, boolean extended) {

        JsonArray result = new JsonArray();
        List<String> docElasticIds = new ArrayList<>();

        if (sysmlId != null) {
            docElasticIds = pgh.getGroupDocuments(sysmlId, DbEdgeTypes.CONTAINMENT, depth, DbNodeTypes.SITEANDPACKAGE);
        } else {
            List<Node> docNodes = pgh.getNodesByType(DbNodeTypes.DOCUMENT);
            for (Node node : docNodes) {
                if (!node.isDeleted()) {
                    docElasticIds.add(node.getElasticId());
                }
            }
        }

        JsonArray docJson = new JsonArray();
        try {
            docJson = eh.getElementsFromElasticIds(docElasticIds, projectId);
        } catch (IOException e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
        if (extended) {
            docJson = addExtendedInformation(docJson);
        }

        for (int i = 0; i < docJson.size(); i++) {
            JsonObject doc = docJson.get(i).getAsJsonObject();
            doc.addProperty(Sjm.PROJECTID, this.projectId);
            doc.addProperty(Sjm.REFID, this.workspaceName);
            if (!extended) {
                if (sysmlId == null) {
                    String groupId = pgh.getGroup(doc.get(Sjm.SYSMLID).getAsString());
                    doc.addProperty(Sjm.SITECHARACTERIZATIONID, groupId);
                } else {
                    if (!sysmlId.equals(projectId)) {
                        doc.addProperty(Sjm.SITECHARACTERIZATIONID, sysmlId);
                    }
                }
            }
            result.add(addChildViews(doc));
        }

        return result;
    }

    public JsonObject processPostJson(JsonArray elements, String user, Set<String> oldElasticIds, boolean overwriteJson,
        String src) {

        JsonObject result = new JsonObject();

        String date = TimeUtils.toTimestamp(new Date().getTime());
        String organization = getOrganizationFromProject(this.projectId);
        final String holdingBinSysmlid = (this.projectId != null) ? ("holding_bin_" + this.projectId) : "holding_bin";

        String commitId = UUID.randomUUID().toString();
        JsonObject commit = new JsonObject();
        commit.addProperty(Sjm.ELASTICID, commitId);
        JsonArray commitAdded = new JsonArray();
        JsonArray commitUpdated = new JsonArray();
        JsonArray commitDeleted = new JsonArray();

        JsonArray addedElements = new JsonArray();
        JsonArray updatedElements = new JsonArray();
        JsonArray deletedElements = new JsonArray();
        JsonArray rejectedElements = new JsonArray();
        JsonArray newElements = new JsonArray();

        Map<String, JsonObject> elementMap = convertToMap(elements);
        Set<String> sysmlids = new HashSet<>();
        sysmlids.addAll(elementMap.keySet());

        Map<String, JsonObject> existingMap = convertToMap(getNodesBySysmlids(sysmlids, false, true));

        for (int i = 0; i < elements.size(); i++) {
            JsonObject o = elements.get(i).getAsJsonObject();
            String sysmlid = JsonUtil.getOptString(o, Sjm.SYSMLID);
            if (sysmlid.equals("")) {
                sysmlid = createId();
                o.addProperty(Sjm.SYSMLID, sysmlid);
            }

            String content = o.toString();
            if (isImageData(content)) {
                content = extractAndReplaceImageData(content, organization);
                JsonParser parser = new JsonParser();
                try {
                   o = parser.parse(content).getAsJsonObject();
                } catch (JsonSyntaxException e) {
                    // pass
                }
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
                rejectedElements.add(o);
            }

            // pregenerate the elasticId
            o.addProperty(Sjm.ELASTICID, UUID.randomUUID().toString());
            o.addProperty(Sjm.COMMITID, commitId);
            o.addProperty(Sjm.PROJECTID, this.projectId);
            o.addProperty(Sjm.REFID, this.workspaceName);
            JsonArray arry = new JsonArray();
            arry.add(this.workspaceName);
            o.add(Sjm.INREFIDS, arry);
            o.addProperty(Sjm.MODIFIER, user);
            o.addProperty(Sjm.MODIFIED, date);

            if (o.has(Sjm.QUALIFIEDID)) {
                o.remove(Sjm.QUALIFIEDID);
            }
            if (o.has(Sjm.QUALIFIEDNAME)) {
                o.remove(Sjm.QUALIFIEDNAME);
            }

            if (!o.has(Sjm.OWNERID) || o.get(Sjm.OWNERID).isJsonNull() || o.get(Sjm.OWNERID).getAsString()
                .equalsIgnoreCase("null")) {
                o.addProperty(Sjm.OWNERID, holdingBinSysmlid);
            }
            reorderChildViews(o, newElements, addedElements, updatedElements, deletedElements, commitAdded,
                commitUpdated, commitDeleted, commitId, user, date, oldElasticIds);

            if (added) {
                logger.debug("ELEMENT ADDED!");
                o.addProperty(Sjm.CREATOR, user);
                o.addProperty(Sjm.CREATED, date);
                addedElements.add(o);

                JsonObject newObj = new JsonObject();
                newObj.add(Sjm.SYSMLID, o.get(Sjm.SYSMLID));
                newObj.add(Sjm.ELASTICID, o.get(Sjm.ELASTICID));
                commitAdded.add(newObj);
            } else if (updated) {
                logger.debug("ELEMENT UPDATED!");
                updatedElements.add(o);

                JsonObject parent = new JsonObject();
                parent.add("previousElasticId", existingMap.get(sysmlid).get(Sjm.ELASTICID));
                oldElasticIds.add(existingMap.get(sysmlid).get(Sjm.ELASTICID).getAsString());
                parent.addProperty(Sjm.SYSMLID, sysmlid);
                parent.add(Sjm.ELASTICID, o.get(Sjm.ELASTICID));
                commitUpdated.add(parent);
            } else {
                logger.debug("ELEMENT CONFLICT!");
            }

            newElements.add(o);
        }

        result.add("addedElements", addedElements);
        result.add("updatedElements", updatedElements);
        result.add("newElements", newElements);
        result.add("deletedElements", deletedElements);
        result.add("rejectedElements", rejectedElements);

        commit.add("added", commitAdded);
        commit.add("updated", commitUpdated);
        commit.add("deleted", commitDeleted);
        commit.addProperty(Sjm.CREATOR, user);
        commit.addProperty(Sjm.CREATED, date);
        commit.addProperty(Sjm.PROJECTID, projectId);
        commit.addProperty(Sjm.SOURCE, src);


        result.add("commit", commit);

        return result;
    }

    public void updateElasticRemoveRefs(Set<String> elasticIds) {
        try {
            JsonObject script = new JsonObject();
            JsonObject inline = new JsonObject();
            JsonObject params = new JsonObject();
            script.add("script", inline);
            script.add("params", params);
            inline.addProperty("inline", "if(ctx._source.containsKey(\""
                               + Sjm.INREFIDS + "\")){ctx._source." 
                               + Sjm.INREFIDS
                               + ".add(params.refId)} else {ctx._source."
                               + Sjm.INREFIDS + " = [params.refId]}");
            params.addProperty("refId", this.workspaceName);
            eh.bulkUpdateElements(elasticIds, script.toString(), projectId);
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

    public JsonObject addChildViews(JsonObject o) {
        boolean isView = false;
        if (o.has(Sjm.SYSMLID)) {
            JsonArray typeArray = JsonUtil.getOptArray(o, Sjm.APPLIEDSTEREOTYPEIDS);
            for (int i = 0; i < typeArray.size(); i++) {
                String typeJson = JsonUtil.getOptString(typeArray, i);
                if (Sjm.STEREOTYPEIDS.containsKey(typeJson) && (Sjm.STEREOTYPEIDS.get(typeJson)
                                .matches("view|document"))) {
                            isView = true;
                }
            }
        }
        if (isView) {
            JsonArray childViews = new JsonArray();
            JsonArray ownedAttributes = JsonUtil.getOptArray(o, Sjm.OWNEDATTRIBUTEIDS);
            Set<String> ownedAttributeSet = new HashSet<>();
            if (ownedAttributes != null && ownedAttributes.size() > 0) {
                for (int j = 0; j < ownedAttributes.size(); j++) {
                    ownedAttributeSet.add(ownedAttributes.get(j).getAsString());
                }
            }

            JsonArray ownedAttributesJSON = getNodesBySysmlids(ownedAttributeSet);
            Map<String, JsonObject> ownedAttributesMap = new HashMap<>();
            for (int i = 0; i < ownedAttributesJSON.size(); i++) {
                JsonObject ownedAttribute = JsonUtil.getOptObject(ownedAttributesJSON, i);
                ownedAttributesMap.put(ownedAttribute.get(Sjm.SYSMLID).getAsString(), ownedAttribute);
            }
            if (ownedAttributes != null && ownedAttributes.size() > 0) {
                for (int j = 0; j < ownedAttributes.size(); j++) {
                    if (ownedAttributesMap.containsKey(ownedAttributes.get(j))) {
                        JsonObject ownedAttribute = ownedAttributesMap.get(ownedAttributes.get(j));
                        if (ownedAttribute != null && ownedAttribute.get(Sjm.TYPE).getAsString().equals("Property")) {
                            if (!JsonUtil.getOptString(ownedAttribute, Sjm.TYPEID).equals("")) {
                                JsonObject childView = new JsonObject();
                                childView.add(Sjm.SYSMLID, ownedAttribute.get(Sjm.TYPEID));
                                childView.add(Sjm.AGGREGATION, ownedAttribute.get(Sjm.AGGREGATION));
                                childViews.add(childView);
                            }
                        }
                    }
                }
            }
            o.add(Sjm.CHILDVIEWS, childViews);
        }
        return o;
    }

    private void reorderChildViews(JsonObject element, JsonArray newElements, JsonArray addedElements,
        JsonArray updatedElements, JsonArray deletedElements, JsonArray commitAdded, JsonArray commitUpdated,
        JsonArray commitDeleted, String commitId, String creator, String now, Set<String> oldElasticIds) {

        if (!element.has(Sjm.CHILDVIEWS)) {
            return;
        }

        String sysmlId = JsonUtil.getOptString(element, Sjm.SYSMLID);
        Set<DbNodeTypes> dbnt = new HashSet<>();
        dbnt.add(DbNodeTypes.PACKAGE);
        String ownerParentPackage = pgh.getImmediateParentOfType(sysmlId, DbEdgeTypes.CONTAINMENT, dbnt);

        JsonObject oldElement = getNodeBySysmlid(sysmlId);

        JsonArray oldOwnedAttributes = JsonUtil.getOptArray(oldElement, Sjm.OWNEDATTRIBUTEIDS);
        JsonArray newChildViews = JsonUtil.getOptArray(element, Sjm.CHILDVIEWS);

        JsonArray ownedAttributes;
        JsonArray ownedAttributesIds = new JsonArray();

        Set<String> oldOwnedAttributeSet = new HashSet<>();
        for (int i = 0; i < oldOwnedAttributes.size(); i++) {
            oldOwnedAttributeSet.add(oldOwnedAttributes.get(i).getAsString());
        }

        Set<String> newChildViewsSet = new HashSet<>();
        for (int i = 0; i < newChildViews.size(); i++) {
            JsonObject obji = JsonUtil.getOptObject(newChildViews, i);
            if (!JsonUtil.getOptString(obji, Sjm.SYSMLID).equals("")) {
                newChildViewsSet.add(JsonUtil.getOptString(obji, Sjm.SYSMLID));
            }
        }

        ownedAttributes = getNodesBySysmlids(oldOwnedAttributeSet);

        Map<String, String> createProps = new HashMap<>();
        List<String> notAViewList = new ArrayList<>();
        JsonObject mountJson = null;
        for (int i = 0; i < ownedAttributes.size(); i++) {
            JsonObject ownedAttribute = JsonUtil.getOptObject(ownedAttributes, i);
            if (ownedAttribute.get(Sjm.TYPE).getAsString().equals("Property")) {
                if (!JsonUtil.getOptString(ownedAttribute, Sjm.TYPEID).equals("")) {
                    if (!newChildViewsSet.contains(ownedAttribute.get(Sjm.TYPEID).getAsString())) {
                        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(
                                        JsonUtil.getOptString(ownedAttribute, Sjm.PROJECTID),
                                        JsonUtil.getOptString(ownedAttribute, Sjm.REFID));
                        JsonArray childViews = new JsonArray();
                        Set<String> childViewsSet = new HashSet<>();
                        childViewsSet.add(ownedAttribute.get(Sjm.TYPEID).getAsString());
                        try {
                            if (mountJson == null) {
                                mountJson = getProjectWithFullMounts(
                                                JsonUtil.getOptString(ownedAttribute, Sjm.PROJECTID),
                                                JsonUtil.getOptString(ownedAttribute, Sjm.REFID), null);
                            }
                            handleMountSearch(mountJson, false, false, 0L, childViewsSet, childViews);
                        } catch (Exception e) {
                            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
                        }

                        if (childViews.size() > 0) {
                            for (int j = 0; j < childViews.size(); j++) {
                                JsonObject childView = JsonUtil.getOptObject(childViews, j);

                                JsonArray appliedStereotypeIds = 
                                                JsonUtil.getOptArray(childView, Sjm.APPLIEDSTEREOTYPEIDS);
                                String asids = (appliedStereotypeIds == null) ? "" : appliedStereotypeIds.toString();
                                if (asids.contains("_17_0_1_232f03dc_1325612611695_581988_21583") || asids
                                    .contains("_17_0_2_3_87b0275_1371477871400_792964_43374") || asids
                                    .contains("_17_0_1_407019f_1332453225141_893756_11936") || asids
                                    .contains("_11_5EAPbeta_be00301_1147420760998_43940_227") || asids
                                    .contains("_18_0beta_9150291_1392290067481_33752_4359")) {
                                    if (!JsonUtil.getOptString(childView, Sjm.SYSMLID).equals("")) {
                                        deletedElements.add(ownedAttribute);
                                        oldElasticIds.add(ownedAttribute.get(Sjm.ELASTICID).getAsString());
                                        JsonObject newObj = new JsonObject();
                                        newObj.add(Sjm.SYSMLID, ownedAttribute.get(Sjm.SYSMLID));
                                        newObj.add(Sjm.ELASTICID, ownedAttribute.get(Sjm.ELASTICID));
                                        commitDeleted.add(newObj);
                                    }
                                    JsonObject asi = emsNodeUtil
                                        .getNodeBySysmlid(JsonUtil.getOptString(ownedAttribute, 
                                                        Sjm.APPLIEDSTEREOTYPEINSTANCEID));
                                    if (!JsonUtil.getOptString(asi, Sjm.SYSMLID).equals("")) {
                                        deletedElements.add(asi);
                                        oldElasticIds.add(asi.get(Sjm.ELASTICID).getAsString());
                                        JsonObject newObj = new JsonObject();
                                        newObj.add(Sjm.SYSMLID, asi.get(Sjm.SYSMLID));
                                        newObj.add(Sjm.ELASTICID, asi.get(Sjm.ELASTICID));
                                        commitDeleted.add(newObj);
                                    }
                                    JsonObject association =
                                        emsNodeUtil.getNodeBySysmlid(JsonUtil.getOptString(ownedAttribute, 
                                                        Sjm.ASSOCIATIONID));
                                    if (!JsonUtil.getOptString(association, Sjm.SYSMLID).equals("")) {
                                        deletedElements.add(association);
                                        oldElasticIds.add(association.get(Sjm.ELASTICID).getAsString());
                                        JsonObject newObj = new JsonObject();
                                        newObj.add(Sjm.SYSMLID, association.get(Sjm.SYSMLID));
                                        newObj.add(Sjm.ELASTICID, association.get(Sjm.ELASTICID));
                                        commitDeleted.add(newObj);
                                    }
                                    JsonArray associationProps = JsonUtil.getOptArray(association, Sjm.OWNEDENDIDS);
                                    for (int k = 0; k < associationProps.size(); k++) {
                                        if (!JsonUtil.getOptString(associationProps, k).equals("")) {
                                            JsonObject assocProp =
                                                emsNodeUtil.getNodeBySysmlid(JsonUtil.getOptString(associationProps, k));
                                            if (!JsonUtil.getOptString(assocProp, Sjm.SYSMLID).equals("")) {
                                                deletedElements.add(assocProp);
                                                oldElasticIds.add(assocProp.get(Sjm.ELASTICID).getAsString());
                                                JsonObject newObj = new JsonObject();
                                                newObj.add(Sjm.SYSMLID, assocProp.get(Sjm.SYSMLID));
                                                newObj.add(Sjm.ELASTICID, assocProp.get(Sjm.ELASTICID));
                                                commitDeleted.add(newObj);
                                            }
                                        }
                                    }
                                } else {
                                    notAViewList.add(ownedAttribute.get(Sjm.SYSMLID).getAsString());
                                }
                            }
                        }
                    } else {
                        createProps.put(ownedAttribute.get(Sjm.TYPEID).getAsString(), 
                                        ownedAttribute.get(Sjm.SYSMLID).getAsString());
                    }
                } else {
                    notAViewList.add(ownedAttribute.get(Sjm.SYSMLID).getAsString());
                }
            }
        }

        if (newChildViews != null && newChildViews.size() > 0) {
            for (int i = 0; i < newChildViews.size(); i++) {
                JsonObject child = newChildViews.get(i).getAsJsonObject();
                if (child.has(Sjm.SYSMLID)) {
                    if (createProps.containsKey(child.get(Sjm.SYSMLID))) {
                        if (!ownedAttributesIds.toString().contains(createProps.get(child.get(Sjm.SYSMLID)))) {
                            ownedAttributesIds.add(createProps.get(child.get(Sjm.SYSMLID)));
                        }
                    } else {
                        String cvSysmlId = child.get(Sjm.SYSMLID).getAsString();
                        String aggregation = child.get(Sjm.AGGREGATION).getAsString();

                        String propertySysmlId = createId();
                        String associationSysmlId = createId();
                        String assocPropSysmlId = createId();

                        // Create Property
                        JsonObject property = new JsonObject();
                        property.addProperty(Sjm.SYSMLID, propertySysmlId);
                        property.addProperty(Sjm.NAME, "childView" + (i + 1));
                        property.add(Sjm.NAMEEXPRESSION, JsonNull.INSTANCE);
                        property.addProperty(Sjm.TYPE, "Property");
                        property.addProperty(Sjm.OWNERID, sysmlId);
                        property.addProperty(Sjm.TYPEID, cvSysmlId);
                        property.addProperty(Sjm.AGGREGATION, aggregation);
                        property.addProperty(Sjm.ELASTICID, UUID.randomUUID().toString());
                        // Default Fields
                        property.addProperty(Sjm.ASSOCIATIONID, associationSysmlId);
                        JsonArray asid = new JsonArray();
                        asid.add(alterIdAggregationType(aggregation));
                        property.add(Sjm.APPLIEDSTEREOTYPEIDS, asid);
                        property.addProperty(Sjm.DOCUMENTATION, "");
                        property.add(Sjm.MDEXTENSIONSIDS, new JsonArray());
                        property.add(Sjm.SYNCELEMENTID, JsonNull.INSTANCE);
                        property.addProperty(Sjm.APPLIEDSTEREOTYPEINSTANCEID, propertySysmlId + "_asi");
                        property.add(Sjm.CLIENTDEPENDENCYIDS, new JsonArray());
                        property.add(Sjm.SUPPLIERDEPENDENCYIDS, new JsonArray());
                        property.addProperty(Sjm.VISIBILITY, "private");
                        property.addProperty(Sjm.ISLEAF, false);
                        property.addProperty(Sjm.ISSTATIC, false);
                        property.addProperty(Sjm.ISORDERED, false);
                        property.addProperty(Sjm.ISUNIQUE, true);
                        property.add(Sjm.LOWERVALUE, JsonNull.INSTANCE);
                        property.add(Sjm.UPPERVALUE, JsonNull.INSTANCE);
                        property.addProperty(Sjm.ISREADONLY, false);
                        property.add(Sjm.TEMPLATEPARAMETERID, JsonNull.INSTANCE);
                        property.add(Sjm.ENDIDS, new JsonArray());
                        property.add(Sjm.DEPLOYMENTIDS, new JsonArray());
                        property.add(Sjm.ASSOCIATIONENDID, JsonNull.INSTANCE);
                        property.add(Sjm.QUALIFIERIDS, new JsonArray());
                        property.add(Sjm.DATATYPEID, JsonNull.INSTANCE);
                        property.add(Sjm.DEFAULTVALUE, JsonNull.INSTANCE);
                        property.add(Sjm.INTERFACEID, JsonNull.INSTANCE);
                        property.addProperty(Sjm.ISDERIVED, false);
                        property.addProperty(Sjm.ISDERIVEDUNION, false);
                        property.addProperty(Sjm.ISID, false);
                        property.add(Sjm.REDEFINEDPROPERTYIDS, new JsonArray());
                        property.add(Sjm.SUBSETTEDPROPERTYIDS, new JsonArray());
                        JsonArray worklist = new JsonArray();
                        worklist.add(this.workspaceName);
                        property.add(Sjm.INREFIDS, worklist);
                        property.addProperty(Sjm.PROJECTID, this.projectId);
                        property.addProperty(Sjm.REFID, this.workspaceName);
                        property.addProperty(Sjm.COMMITID, commitId);
                        property.addProperty(Sjm.CREATOR, creator);
                        property.addProperty(Sjm.CREATED, now);
                        property.addProperty(Sjm.MODIFIER, creator);
                        property.addProperty(Sjm.MODIFIED, now);

                        newElements.add(property);
                        addedElements.add(property);
                        JsonObject newProperty = new JsonObject();
                        newProperty.add(Sjm.SYSMLID, property.get(Sjm.SYSMLID));
                        newProperty.add(Sjm.ELASTICID, property.get(Sjm.ELASTICID));
                        commitAdded.add(newProperty);

                        // Create AppliedStereotypeInstance
                        JsonObject propertyASI = new JsonObject();
                        propertyASI.addProperty(Sjm.SYSMLID, propertySysmlId + "_asi");
                        propertyASI.addProperty(Sjm.NAME, "");
                        propertyASI.add(Sjm.NAMEEXPRESSION, JsonNull.INSTANCE);
                        propertyASI.addProperty(Sjm.TYPE, "InstanceSpecification");
                        propertyASI.add(Sjm.APPLIEDSTEREOTYPEIDS, new JsonArray());
                        propertyASI.addProperty(Sjm.DOCUMENTATION, "");
                        propertyASI.add(Sjm.MDEXTENSIONSIDS, new JsonArray());
                        propertyASI.addProperty(Sjm.OWNERID, propertySysmlId);
                        propertyASI.addProperty(Sjm.ELASTICID, UUID.randomUUID().toString());
                        propertyASI.add(Sjm.SYNCELEMENTID, JsonNull.INSTANCE);
                        propertyASI.add(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JsonNull.INSTANCE);
                        propertyASI.add(Sjm.CLIENTDEPENDENCYIDS, new JsonArray());
                        propertyASI.add(Sjm.SUPPLIERDEPENDENCYIDS, new JsonArray());
                        propertyASI.add(Sjm.VISIBILITY, JsonNull.INSTANCE);
                        propertyASI.add(Sjm.TEMPLATEPARAMETERID, JsonNull.INSTANCE);
                        propertyASI.add(Sjm.DEPLOYMENTIDS, new JsonArray());
                        propertyASI.add(Sjm.SLOTIDS, new JsonArray());
                        propertyASI.add(Sjm.SPECIFICATION, JsonNull.INSTANCE);
                        JsonArray classifierids = new JsonArray();
                        classifierids.add(alterIdAggregationType(aggregation));
                        propertyASI.add(Sjm.CLASSIFIERIDS, classifierids);
                        propertyASI.addProperty(Sjm.STEREOTYPEDELEMENTID, propertySysmlId);
                        JsonArray worklist2 = new JsonArray();
                        worklist2.add(this.workspaceName);
                        propertyASI.add(Sjm.INREFIDS, worklist2);
                        propertyASI.addProperty(Sjm.PROJECTID, this.projectId);
                        propertyASI.addProperty(Sjm.REFID, this.workspaceName);
                        propertyASI.addProperty(Sjm.COMMITID, commitId);
                        propertyASI.addProperty(Sjm.CREATOR, creator);
                        propertyASI.addProperty(Sjm.CREATED, now);
                        propertyASI.addProperty(Sjm.MODIFIER, creator);
                        propertyASI.addProperty(Sjm.MODIFIED, now);

                        newElements.add(propertyASI);
                        addedElements.add(propertyASI);
                        JsonObject newASI = new JsonObject();
                        newASI.add(Sjm.SYSMLID, property.get(Sjm.SYSMLID));
                        newASI.add(Sjm.ELASTICID, property.get(Sjm.ELASTICID));
                        commitAdded.add(newASI);

                        // Create Associations
                        JsonObject association = new JsonObject();
                        JsonArray memberEndIds = new JsonArray();
                        memberEndIds.add(propertySysmlId);
                        memberEndIds.add(assocPropSysmlId);
                        JsonArray ownedEndIds = new JsonArray();
                        ownedEndIds.add(assocPropSysmlId);

                        association.addProperty(Sjm.SYSMLID, associationSysmlId);
                        association.addProperty(Sjm.NAME, "");
                        association.add(Sjm.NAMEEXPRESSION, JsonNull.INSTANCE);
                        association.addProperty(Sjm.TYPE, "Association");
                        association.addProperty(Sjm.OWNERID, ownerParentPackage);
                        association.add(Sjm.MEMBERENDIDS, memberEndIds);
                        association.add(Sjm.OWNEDENDIDS, ownedEndIds);
                        association.addProperty(Sjm.ELASTICID, UUID.randomUUID().toString());
                        // Default Fields
                        association.addProperty(Sjm.DOCUMENTATION, "");
                        association.add(Sjm.MDEXTENSIONSIDS, new JsonArray());
                        association.add(Sjm.SYNCELEMENTID, JsonNull.INSTANCE);
                        association.add(Sjm.APPLIEDSTEREOTYPEIDS, new JsonArray());
                        association.add(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JsonNull.INSTANCE);
                        association.add(Sjm.CLIENTDEPENDENCYIDS, new JsonArray());
                        association.add(Sjm.SUPPLIERDEPENDENCYIDS, new JsonArray());
                        association.add(Sjm.NAMEEXPRESSION, JsonNull.INSTANCE);
                        association.addProperty(Sjm.VISIBILITY, "public");
                        association.add(Sjm.TEMPLATEPARAMETERID, JsonNull.INSTANCE);
                        association.add(Sjm.ELEMENTIMPORTIDS, new JsonArray());
                        association.add(Sjm.PACKAGEIMPORTIDS, new JsonArray());
                        association.addProperty(Sjm.ISLEAF, false);
                        association.add(Sjm.TEMPLATEBINDINGIDS, new JsonArray());
                        association.add(Sjm.USECASEIDS, new JsonArray());
                        association.add(Sjm.REPRESENTATIONID, JsonNull.INSTANCE);
                        association.add(Sjm.COLLABORATIONUSEIDS, new JsonArray());
                        association.add(Sjm.GENERALIZATIONIDS, new JsonArray());
                        association.add(Sjm.POWERTYPEEXTENTIDS, new JsonArray());
                        association.addProperty(Sjm.ISABSTRACT, false);
                        association.addProperty(Sjm.ISFINALSPECIALIZATION, false);
                        association.add(Sjm.REDEFINEDCLASSIFIERIDS, new JsonArray());
                        association.add(Sjm.SUBSTITUTIONIDS, new JsonArray());
                        association.addProperty(Sjm.ISDERIVED, false);
                        association.add(Sjm.NAVIGABLEOWNEDENDIDS, new JsonArray());
                        JsonArray worklist3 = new JsonArray();
                        worklist3.add(this.workspaceName);
                        association.add(Sjm.INREFIDS, worklist3);
                        association.addProperty(Sjm.PROJECTID, this.projectId);
                        association.addProperty(Sjm.REFID, this.workspaceName);
                        association.addProperty(Sjm.COMMITID, commitId);
                        association.addProperty(Sjm.CREATOR, creator);
                        association.addProperty(Sjm.CREATED, now);
                        association.addProperty(Sjm.MODIFIER, creator);
                        association.addProperty(Sjm.MODIFIED, now);

                        newElements.add(association);
                        addedElements.add(association);
                        JsonObject newAssociation = new JsonObject();
                        newAssociation.add(Sjm.SYSMLID, property.get(Sjm.SYSMLID));
                        newAssociation.add(Sjm.ELASTICID, property.get(Sjm.ELASTICID));
                        commitAdded.add(newAssociation);

                        // Create Association Property
                        JsonObject assocProperty = new JsonObject();
                        assocProperty.addProperty(Sjm.SYSMLID, assocPropSysmlId);
                        assocProperty.addProperty(Sjm.NAME, "");
                        assocProperty.add(Sjm.NAMEEXPRESSION, JsonNull.INSTANCE);
                        assocProperty.addProperty(Sjm.TYPE, "Property");
                        assocProperty.addProperty(Sjm.TYPEID, sysmlId);
                        assocProperty.addProperty(Sjm.OWNERID, associationSysmlId);
                        assocProperty.addProperty(Sjm.AGGREGATION, "none");
                        assocProperty.addProperty(Sjm.ELASTICID, UUID.randomUUID().toString());
                        // Default Fields
                        assocProperty.addProperty(Sjm.ASSOCIATIONID, associationSysmlId);
                        assocProperty.add(Sjm.APPLIEDSTEREOTYPEIDS, new JsonArray());
                        assocProperty.addProperty(Sjm.DOCUMENTATION, "");
                        assocProperty.add(Sjm.MDEXTENSIONSIDS, new JsonArray());
                        assocProperty.add(Sjm.SYNCELEMENTID, JsonNull.INSTANCE);
                        assocProperty.add(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JsonNull.INSTANCE);
                        assocProperty.add(Sjm.CLIENTDEPENDENCYIDS, new JsonArray());
                        assocProperty.add(Sjm.SUPPLIERDEPENDENCYIDS, new JsonArray());
                        assocProperty.add(Sjm.NAMEEXPRESSION, JsonNull.INSTANCE);
                        assocProperty.addProperty(Sjm.VISIBILITY, "private");
                        assocProperty.addProperty(Sjm.ISLEAF, false);
                        assocProperty.addProperty(Sjm.ISSTATIC, false);
                        assocProperty.addProperty(Sjm.ISORDERED, false);
                        assocProperty.addProperty(Sjm.ISUNIQUE, true);
                        assocProperty.add(Sjm.LOWERVALUE, JsonNull.INSTANCE);
                        assocProperty.add(Sjm.UPPERVALUE, JsonNull.INSTANCE);
                        assocProperty.addProperty(Sjm.ISREADONLY, false);
                        assocProperty.add(Sjm.TEMPLATEPARAMETERID, JsonNull.INSTANCE);
                        assocProperty.add(Sjm.ENDIDS, new JsonArray());
                        assocProperty.add(Sjm.DEPLOYMENTIDS, new JsonArray());
                        assocProperty.add(Sjm.ASSOCIATIONENDID, JsonNull.INSTANCE);
                        assocProperty.add(Sjm.QUALIFIERIDS, new JsonArray());
                        assocProperty.add(Sjm.DATATYPEID, JsonNull.INSTANCE);
                        assocProperty.add(Sjm.DEFAULTVALUE, JsonNull.INSTANCE);
                        assocProperty.add(Sjm.INTERFACEID, JsonNull.INSTANCE);
                        assocProperty.addProperty(Sjm.ISDERIVED, false);
                        assocProperty.addProperty(Sjm.ISDERIVEDUNION, false);
                        assocProperty.addProperty(Sjm.ISID, false);
                        assocProperty.add(Sjm.REDEFINEDPROPERTYIDS, new JsonArray());
                        assocProperty.add(Sjm.SUBSETTEDPROPERTYIDS, new JsonArray());
                        JsonArray worklist4 = new JsonArray();
                        worklist4.add(this.workspaceName);
                        assocProperty.add(Sjm.INREFIDS, worklist4);
                        assocProperty.addProperty(Sjm.PROJECTID, this.projectId);
                        assocProperty.addProperty(Sjm.REFID, this.workspaceName);
                        assocProperty.addProperty(Sjm.COMMITID, commitId);
                        assocProperty.addProperty(Sjm.CREATOR, creator);
                        assocProperty.addProperty(Sjm.CREATED, now);
                        assocProperty.addProperty(Sjm.MODIFIER, creator);
                        assocProperty.addProperty(Sjm.MODIFIED, now);

                        newElements.add(assocProperty);
                        addedElements.add(assocProperty);
                        JsonObject newAssociationProperty = new JsonObject();
                        newAssociationProperty.add(Sjm.SYSMLID, property.get(Sjm.SYSMLID));
                        newAssociationProperty.add(Sjm.ELASTICID, property.get(Sjm.ELASTICID));
                        commitAdded.add(newAssociationProperty);

                        ownedAttributesIds.add(propertySysmlId);
                    }
                }
            }
        }

        for (String id : notAViewList) {
            ownedAttributesIds.add(id);
        }

        element.add(Sjm.OWNEDATTRIBUTEIDS, ownedAttributesIds);
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

    public JsonObject getElementByElasticID(String elasticId) {
        try {
            return eh.getElementByElasticId(elasticId, projectId);
        } catch (IOException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return null;
    }

    private Map<String, JsonObject> convertToMap(JsonArray elements) {
        Map<String, JsonObject> result = new HashMap<>();
        for (int i = 0; i < elements.size(); i++) {
            JsonObject elem = elements.get(i).getAsJsonObject();
            if (!JsonUtil.getOptString(elem, Sjm.SYSMLID).equals("")) {
                result.put(elem.get(Sjm.SYSMLID).getAsString(), elem);
            }
        }

        return result;
    }

    private Map<String, Map<String, String>> calculateQualifiedInformation(JsonArray elements) {
        Map<String, Map<String, String>> result = new HashMap<>();
        Map<String, JsonObject> sysmlid2elements = getSysmlMap(elements);
        Map<String, JsonObject> cache = new HashMap<>();
        for (int i = 0; i < elements.size(); i++) {
            JsonObject element = elements.get(i).getAsJsonObject();
            String sysmlid = element.get(Sjm.SYSMLID).getAsString();
            Map<String, String> extendedInfo = getQualifiedInformationForElement(element, sysmlid2elements, cache);

            Map<String, String> attrs = new HashMap<>();
            attrs.put(Sjm.QUALIFIEDNAME, extendedInfo.get(Sjm.QUALIFIEDNAME));
            attrs.put(Sjm.QUALIFIEDID, extendedInfo.get(Sjm.QUALIFIEDID));
            attrs.put(Sjm.SITECHARACTERIZATIONID, extendedInfo.get(Sjm.SITECHARACTERIZATIONID));

            result.put(sysmlid, attrs);
        }

        return result;
    }

    private Map<String, String> getQualifiedInformationForElement(JsonObject element,
        Map<String, JsonObject> elementMap, Map<String, JsonObject> cache) {

        Map<String, String> result = new HashMap<>();

        JsonObject o = element;
        ArrayList<String> qn = new ArrayList<>();
        ArrayList<String> qid = new ArrayList<>();
        String sqn;
        String sqid;
        String siteCharacterizationId = null;
        qn.add(JsonUtil.getOptString(o, "name"));
        qid.add(JsonUtil.getOptString(o, Sjm.SYSMLID));

        while (o.has(Sjm.OWNERID) && !JsonUtil.getOptString(o, Sjm.OWNERID).equals("") 
                        && !o.get(Sjm.OWNERID).getAsString().equals("null")) {
            String sysmlid = JsonUtil.getOptString(o, Sjm.OWNERID);
            JsonObject owner = elementMap.get(sysmlid);
            if (owner == null) {
                if (cache.containsKey(sysmlid)) {
                    owner = cache.get(sysmlid);
                } else {
                    owner = getNodeBySysmlid(sysmlid, this.workspaceName, false);
                    cache.put(sysmlid, owner);
                }
            }

            String ownerId = JsonUtil.getOptString(owner, Sjm.SYSMLID);
            qid.add(ownerId);

            String ownerName = JsonUtil.getOptString(owner, Sjm.NAME);
            qn.add(ownerName);

            if (siteCharacterizationId == null && CommitUtil.isSite(owner)) {
                siteCharacterizationId = JsonUtil.getOptString(owner, Sjm.SYSMLID);
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

    private boolean diffUpdateJson(JsonObject json, JsonObject existing) {
        if (json.has(Sjm.SYSMLID) && existing.has(Sjm.SYSMLID)) {
            String jsonModified = JsonUtil.getOptString(json, Sjm.MODIFIED);
            String existingModified = JsonUtil.getOptString(existing, Sjm.MODIFIED);
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

    private boolean mergeJson(JsonObject partial, JsonObject original) {
        if (original == null) {
            return false;
        }

        for (Map.Entry<String, JsonElement> entry: original.entrySet()) {
            String attr = entry.getKey();
            if (!partial.has(attr)) {
                partial.add(attr, original.get(attr));
            }
        }
        return true;
    }

    private boolean isUpdated(JsonObject json, JsonObject existing) {
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

    public JsonArray addExtendedInformation(JsonArray elements) {
        JsonArray newElements = new JsonArray();

        Map<String, Map<String, String>> sysmlid2qualified = calculateQualifiedInformation(elements);

        for (int i = 0; i < elements.size(); i++) {
            JsonObject element = elements.get(i).getAsJsonObject();

            JsonObject newElement = addExtendedInformationForElement(element, sysmlid2qualified);
            newElements.add(newElement);
        }

        return newElements.size() >= elements.size() ? newElements : elements;
    }

    private JsonObject addExtendedInformationForElement(JsonObject element,
        Map<String, Map<String, String>> qualifiedInformation) {

        String sysmlid = element.get(Sjm.SYSMLID).getAsString();

        if (qualifiedInformation.containsKey(sysmlid)) {
            Map<String, String> current = qualifiedInformation.get(sysmlid);
            if (current.containsKey(Sjm.QUALIFIEDNAME)) {
                element.addProperty(Sjm.QUALIFIEDNAME, current.get(Sjm.QUALIFIEDNAME));
            }
            if (current.containsKey(Sjm.QUALIFIEDID)) {
                element.addProperty(Sjm.QUALIFIEDID, current.get(Sjm.QUALIFIEDID));
            }
            if (current.containsKey(Sjm.SITECHARACTERIZATIONID)) {
                element.addProperty(Sjm.SITECHARACTERIZATIONID, current.get(Sjm.SITECHARACTERIZATIONID));
            }
        }

        return element;
    }

    public static void handleMountSearch(JsonObject mountsJson, boolean extended, boolean extraDocs,
        final Long maxDepth, Set<String> elementsToFind, JsonArray result) throws IOException {

        if (elementsToFind.isEmpty() || mountsJson == null) {
            return;
        }
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(mountsJson.get(Sjm.SYSMLID).getAsString(), 
                        mountsJson.get(Sjm.REFID).getAsString());
        JsonArray nodeList = emsNodeUtil.getNodesBySysmlids(elementsToFind);
        Set<String> foundElements = new HashSet<>();
        JsonArray curFound = new JsonArray();
        for (int index = 0; index < nodeList.size(); index++) {
            String id = nodeList.get(index).getAsJsonObject().get(Sjm.SYSMLID).getAsString();
            if (maxDepth != 0) {
                JsonArray children = emsNodeUtil.getChildren(id, maxDepth);
                for (int i = 0; i < children.size(); i++) {
                    String cid = children.get(i).getAsJsonObject().get(Sjm.SYSMLID).getAsString();
                    if (foundElements.contains(cid)) {
                        continue;
                    }
                    curFound.add(children.get(i));
                    foundElements.add(cid);
                }
            } else {
                curFound.add(nodeList.get(index));
                foundElements.add(id);
            }
        }
        curFound = extended ?
            emsNodeUtil.addExtendedInformation(extraDocs ? emsNodeUtil.addExtraDocs(curFound) : curFound) :
            (extraDocs ? emsNodeUtil.addExtraDocs(curFound) : curFound);
        for (int i = 0; i < curFound.size(); i++) {
            result.add(curFound.get(i));
        }
        elementsToFind.removeAll(foundElements);
        if (elementsToFind.isEmpty()) {
            return;
        }
        if (!mountsJson.has(Sjm.MOUNTS)) {
            mountsJson = emsNodeUtil
                .getProjectWithFullMounts(mountsJson.get(Sjm.SYSMLID).getAsString(), 
                                mountsJson.get(Sjm.REFID).getAsString(), null);
        }
        JsonArray mountsArray = mountsJson.get(Sjm.MOUNTS).getAsJsonArray();

        for (int i = 0; i < mountsArray.size(); i++) {
            handleMountSearch(mountsArray.get(i).getAsJsonObject(), extended, extraDocs, 
                            maxDepth, elementsToFind, result);
        }
    }

    public List<Node> getSites(boolean sites, boolean sitepackages) {
        return pgh.getSites(sites, sitepackages);
    }

    public JsonObject getCommitObject(String commitId) {
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

    public String insertSingleElastic(JsonObject o) {
        try {
            ElasticResult r = eh.indexElement(o, projectId);
            return r.elasticId;
        } catch (IOException e) {
            logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return null;
    }

    private static Map<String, JsonObject> getSysmlMap(JsonArray elements) {
        Map<String, JsonObject> sysmlid2elements = new HashMap<>();
        for (int i = 0; i < elements.size(); i++) {
            JsonObject newJson = elements.get(i).getAsJsonObject();
            String sysmlid = JsonUtil.getOptString(newJson, Sjm.SYSMLID);
            if (!sysmlid.isEmpty()) {
                sysmlid2elements.put(sysmlid, newJson);
            }
        }
        return sysmlid2elements;
    }

    public static Map<String, Object> toMap(JsonObject object) {
        Map<String, Object> map = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = object.get(key);
            Object val;
            if (value.isJsonArray()) {
                val = toList(value.getAsJsonArray());
            } else if (value.isJsonObject()) {
                val = toMap(value.getAsJsonObject());
            } else if (value.isJsonNull()) {
                val = null;
            } else
            	val = (Object) value;
            map.put(key, val);
        }

        return map;
    }

    private static List<Object> toList(JsonArray array) {
        List<Object> list = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            JsonElement value = array.get(i);
            Object val;
            if (value.isJsonArray()) {
                val = toList(value.getAsJsonArray());
            } else if (value.isJsonObject()) {
                val = toMap(value.getAsJsonObject());
            } else if (value.isJsonNull())
            	val = null;
            else
            	val = (Object) value;
            list.add(val);
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

    public JsonObject getModelAtCommit(String commitId) {
        JsonObject result = new JsonObject();
        JsonObject pastElement = null;
        JsonArray elements = new JsonArray();
        ArrayList<String> refsCommitsIds = new ArrayList<>();

        Map<String, Object> commit = pgh.getCommit(commitId);
        if (commit != null) {
            String refId = commit.get(Sjm.REFID).toString();

            List<Map<String, Object>> refsCommits = pgh.getRefsCommits(refId, (int) commit.get(Sjm.SYSMLID));
            for (Map<String, Object> ref : refsCommits) {
                refsCommitsIds.add((String) ref.get(Sjm.SYSMLID));
            }

            Map<String, String> deletedElementIds = eh.getDeletedElementsFromCommits(refsCommitsIds, projectId);
            List<String> elasticIds = new ArrayList<>();
            for (Map<String, Object> n : pgh.getAllNodesWithLastCommitTimestamp()) {
                if (((Date) n.get(Sjm.TIMESTAMP)).getTime() <= ((Date) commit.get(Sjm.TIMESTAMP)).getTime()) {
                    if (!deletedElementIds.containsKey((String) n.get(Sjm.ELASTICID))) {
                        elasticIds.add((String) n.get(Sjm.ELASTICID));
                    }
                } else {
                    pastElement = getElementAtCommit((String) n.get(Sjm.SYSMLID), commitId, refsCommitsIds);
                }

                if (pastElement != null && pastElement.has(Sjm.SYSMLID) && !deletedElementIds
                    .containsKey(pastElement.get(Sjm.ELASTICID).getAsString())) {
                    elements.add(pastElement);
                }

                // Reset to null so if there is an exception it doesn't add a duplicate
                pastElement = null;
            }

            try {
                JsonArray elems = eh.getElementsFromElasticIds(elasticIds, projectId);
                for (int i = 0; i < elems.size(); i++) {
                    elements.add(elems.get(i));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            result.add(Sjm.ELEMENTS, elements);
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
    public JsonObject getElementAtCommit(String sysmlId, String commitId) {
        JsonObject pastElement = null;
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
                .containsKey(pastElement.get(Sjm.ELASTICID))) {
                pastElement = new JsonObject();
            }
        }
        return pastElement == null ? new JsonObject() : pastElement;
    }

    public JsonArray getNearestCommitFromTimestamp(String timestamp, JsonArray commits) {
        Date requestedTime = null;
        try {
            requestedTime = requestedTime = df.parse(timestamp);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < commits.size(); i++) {
            JsonObject current = commits.get(i).getAsJsonObject();
            Date currentTime;
            try {
                currentTime = df.parse(current.get(Sjm.CREATED).getAsString());
                if (requestedTime.getTime() >= currentTime.getTime()) {
                    JsonArray ja = new JsonArray();
                    ja.add(current);
                    return ja;
                }
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
                }
            }
        }
        return new JsonArray();
    }

    public JsonObject getElementAtCommit(String sysmlId, String commitId, List<String> refIds) {
        JsonObject result = new JsonObject();

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
