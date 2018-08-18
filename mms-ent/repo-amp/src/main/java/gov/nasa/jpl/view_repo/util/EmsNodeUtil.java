package gov.nasa.jpl.view_repo.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Calendar;
import java.util.TimeZone;

import gov.nasa.jpl.view_repo.db.*;
import org.alfresco.repo.service.ServiceDescriptorRegistry;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDependency;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.db.GraphInterface.DbEdgeTypes;
import gov.nasa.jpl.view_repo.db.GraphInterface.DbNodeTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
            try {
                JsonObject current = eh.getElementByElasticId(n.get(ORG_ID), EmsConfig.get("elastic.index.element"));
                if (current != null) {
                    orgs.add(current);
                } else {
                    JsonObject org = new JsonObject();
                    org.addProperty(Sjm.SYSMLID, n.get(ORG_ID));
                    org.addProperty(Sjm.NAME, n.get(ORG_NAME));
                    orgs.add(org);
                }
            } catch (IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Elasticsearch Error: ", e);
                }
            }
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
                JsonObject childProject =
                    getProjectWithFullMounts(mountJson.get(Sjm.MOUNTEDELEMENTPROJECTID).getAsString(),
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

    public JsonObject getArtifactByArtifactAndCommitId(String commitId, String sysmlid) {
        try {
            return eh.getArtifactByCommitId(commitId, sysmlid, projectId);
        } catch (IOException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return new JsonObject();
    }

    public Node getById(String sysmlId) {
        return pgh.getNodeFromSysmlId(sysmlId, true);
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
        List<String> elasticids = pgh.getElasticIdsFromSysmlIdsNodes(new ArrayList<>(sysmlids), withDeleted);
        return getJsonByElasticIds(elasticids, withChildViews);
    }

    public JsonArray getArtifactsBySysmlids(Set<String> sysmlids, boolean withChildViews, boolean withDeleted) {
        List<String> elasticIds = pgh.getElasticIdsFromSysmlIdsArtifacts(new ArrayList<>(sysmlids), withDeleted);
        return getJsonByElasticIds(elasticIds, withChildViews);
    }

    public JsonArray getJsonByElasticIds(List<String> elasticIds, boolean withChildViews) {
        JsonArray elementsFromElastic = new JsonArray();
        try {
            elementsFromElastic = eh.getElementsFromElasticIds(elasticIds, projectId);
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        for (int i = 0; i < elementsFromElastic.size(); i++) {
            JsonObject formatted = elementsFromElastic.get(i).getAsJsonObject();
            formatted.addProperty(Sjm.PROJECTID, this.projectId);
            formatted.addProperty(Sjm.REFID, this.workspaceName);
            elementsFromElastic.set(i, withChildViews ? addChildViews(formatted) : formatted);
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
        return getRefHistory(refId, null, 0);
    }

    public JsonArray getRefHistory(String refId, String commitId, int limit) {
        JsonArray result = new JsonArray();
        int cId = pgh.getCommitId(commitId);
        List<Map<String, Object>> refCommits = pgh.getRefsCommits(refId, cId, limit);
        if (refCommits.size() > 0) {
            result = processCommits(refCommits);
        }

        return result;
    }

    private JsonArray filterCommitsByRefs(JsonArray commits) {
        JsonArray filtered = new JsonArray();
        List<Map<String, Object>> refCommits = pgh.getRefsCommits(this.workspaceName, 0, 0);
        Set<String> commitSet = new HashSet<>();
        for (Map<String, Object> commit: refCommits) {
            commitSet.add((String)commit.get(Sjm.SYSMLID));
        }
        for (int i = 0; i < commits.size(); i++) {
            if (commitSet.contains(commits.get(i).getAsJsonObject().get(Sjm.SYSMLID).getAsString())) {
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
        Map<String, String> refInfo = pgh.getRefElastic(refId);
        if (refInfo != null) {
            try {
                jObj = eh.getElementByElasticId(refInfo.get("elasticId"), projectId);
                jObj.addProperty("parentRefId",
                    (refInfo.get("parent").equals("")) ? "noParent" : refInfo.get("parent"));
                jObj.addProperty("type", (refInfo.get("isTag").equals("true")) ? "tag" : "branch");
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

    public JsonObject getArtifactById(String sysmlid, boolean withDeleted) {
        String artifact = pgh.getElasticIdFromSysmlIdArtifact(sysmlid, withDeleted);
        if (artifact != null) {
            try {
                return eh.getElementByElasticIdArtifact(artifact, projectId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new JsonObject();
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

    public JsonObject search(JsonObject query) {
        try {
            return eh.search(query);
        } catch (IOException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return new JsonObject();
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
     * @return JsonArray of the documents in the site
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
                    if (groupId != null) {
                        doc.addProperty(Sjm.SITECHARACTERIZATIONID, groupId);
                    }
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
        String src, String comment, String type) {

        JsonObject result = new JsonObject();

        String date = TimeUtils.toTimestamp(new Date().getTime());
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

        Map<String, JsonObject> existingMap = (!type.equals("Artifact")) ?
            convertToMap(getNodesBySysmlids(sysmlids, false, true)) :
            convertToMap(getArtifactsBySysmlids(sysmlids, false, true));

        for (int i = 0; i < elements.size(); i++) {
            JsonObject o = elements.get(i).getAsJsonObject();
            String sysmlid = JsonUtil.getOptString(o, Sjm.SYSMLID);
            if (sysmlid.equals("")) {
                sysmlid = createId();
                o.addProperty(Sjm.SYSMLID, sysmlid);
            }

            boolean added = !existingMap.containsKey(sysmlid);
            boolean updated = false;
            Map<Integer, String> rejected = new HashMap<>();
            if (!added) {
                if (!overwriteJson) {
                    if (isUpdated(o, existingMap.get(sysmlid), rejected)) {
                        updated = diffUpdateJson(o, existingMap.get(sysmlid), rejected);
                    }
                } else {
                    updated = true;
                }
            }

            o.addProperty(Sjm.PROJECTID, this.projectId);
            o.addProperty(Sjm.REFID, this.workspaceName);
            JsonArray arry = new JsonArray();
            arry.add(this.workspaceName);
            o.add(Sjm.INREFIDS, arry);

            if (o.has(Sjm.QUALIFIEDID)) {
                o.remove(Sjm.QUALIFIEDID);
            }
            if (o.has(Sjm.QUALIFIEDNAME)) {
                o.remove(Sjm.QUALIFIEDNAME);
            }

            if ((!o.has(Sjm.OWNERID) || o.get(Sjm.OWNERID).isJsonNull() || o.get(Sjm.OWNERID).getAsString()
                .equalsIgnoreCase("null")) && !type.equals("Artifact")) {
                o.addProperty(Sjm.OWNERID, holdingBinSysmlid);
            }
            if (o.has(Sjm.CHILDVIEWS)) {
                reorderChildViews(o, newElements, addedElements, updatedElements, deletedElements, commitAdded,
                    commitUpdated, commitDeleted, commitId, user, date, oldElasticIds);
            }
            if (added) {
                logger.debug("ELEMENT ADDED!");

                o.addProperty(Sjm.CREATOR, user);
                o.addProperty(Sjm.CREATED, date);

                o.addProperty(Sjm.ELASTICID, UUID.randomUUID().toString());
                o.addProperty(Sjm.COMMITID, commitId);
                o.addProperty(Sjm.MODIFIER, user);
                o.addProperty(Sjm.MODIFIED, date);

                addedElements.add(o);

                JsonObject newObj = new JsonObject();
                newObj.add(Sjm.SYSMLID, o.get(Sjm.SYSMLID));
                newObj.add(Sjm.ELASTICID, o.get(Sjm.ELASTICID));
                newObj.addProperty(Sjm.TYPE, type);
                // this for the artifact object, has extra key...
                if (type.equals("Artifact")) {
                    newObj.add(Sjm.CONTENTTYPE, o.get(Sjm.CONTENTTYPE));
                }
                commitAdded.add(newObj);
                newElements.add(o);
            } else if (updated) {
                logger.debug("ELEMENT UPDATED!");

                o.addProperty(Sjm.ELASTICID, UUID.randomUUID().toString());
                o.addProperty(Sjm.COMMITID, commitId);
                o.addProperty(Sjm.MODIFIER, user);
                o.addProperty(Sjm.MODIFIED, date);

                updatedElements.add(o);

                JsonObject parent = new JsonObject();
                parent.add("previousElasticId", existingMap.get(sysmlid).get(Sjm.ELASTICID));
                oldElasticIds.add(existingMap.get(sysmlid).getAsJsonObject().get(Sjm.ELASTICID).getAsString());
                parent.addProperty(Sjm.SYSMLID, sysmlid);
                parent.add(Sjm.ELASTICID, o.get(Sjm.ELASTICID));
                parent.addProperty(Sjm.TYPE, type);
                if (type.equals("Artifact")) {
                    parent.add(Sjm.CONTENTTYPE, o.get(Sjm.CONTENTTYPE));
                }
                commitUpdated.add(parent);
                newElements.add(o);
            } else {
                for (Map.Entry<Integer, String> message : rejected.entrySet()) {
                    JsonObject errorPayload = new JsonObject();
                    errorPayload.addProperty("code", message.getKey());
                    errorPayload.add("element", o);
                    errorPayload.addProperty("message", message.getValue());
                    errorPayload.addProperty("severity", Sjm.WARN);
                    rejectedElements.add(errorPayload);
                }
                logger.debug("ELEMENT REJECTED!");
            }
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
        if (!comment.isEmpty()) {
            commit.addProperty(Sjm.COMMENT, comment);
        }

        result.add("commit", commit);

        return result;
    }

    private static final String updateScript =
        "{\"script\": {\"inline\":\"if(ctx._source.containsKey(\\\"%1$s\\\")){ctx._source.%1$s.removeAll([params.refId])}\", \"params\":{\"refId\":\"%2$s\"}}}";

    public void updateElasticRemoveRefs(Set<String> elasticIds, String type) {
        try {
            String scriptToRun = String.format(updateScript, Sjm.INREFIDS, this.workspaceName);
            logger.debug(String.format("elastic script: %s", scriptToRun));
            eh.bulkUpdateElements(elasticIds, scriptToRun, projectId, type);
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
            if (ownedAttributes.size() > 0) {
                for (int j = 0; j < ownedAttributes.size(); j++) {
                    ownedAttributeSet.add(ownedAttributes.get(j).getAsString());
                }
            }

            JsonArray ownedAttributesJSON = getNodesBySysmlids(ownedAttributeSet, false, false);
            Map<String, JsonObject> ownedAttributesMap = new HashMap<>();
            for (int i = 0; i < ownedAttributesJSON.size(); i++) {
                //originally had optJsonObject, but then it wouldn't have the SYSMLID key in the next line
                // if the optional empty object is returned
                JsonObject ownedAttribute = ownedAttributesJSON.get(i).getAsJsonObject();
                ownedAttributesMap.put(ownedAttribute.get(Sjm.SYSMLID).getAsString(), ownedAttribute);
            }
            if (ownedAttributes.size() > 0) {
                for (int j = 0; j < ownedAttributes.size(); j++) {
                    if (ownedAttributesMap.containsKey(ownedAttributes.get(j).getAsString())) {
                        JsonObject ownedAttribute = ownedAttributesMap.get(ownedAttributes.get(j).getAsString());
                        if (ownedAttribute != null && ownedAttribute.get(Sjm.TYPE).getAsString().equals("Property")) {
                            if (!JsonUtil.getOptString(ownedAttribute, Sjm.TYPEID).equals("")) {
                                JsonObject childView = new JsonObject();
                                childView.add(Sjm.SYSMLID, ownedAttribute.get(Sjm.TYPEID));
                                childView.add(Sjm.AGGREGATION, ownedAttribute.get(Sjm.AGGREGATION));
                                childView.add(Sjm.PROPERTYID, ownedAttribute.get(Sjm.SYSMLID));
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

        JsonArray gotOwnedAttributes = getNodesBySysmlids(oldOwnedAttributeSet, false, false);

        Map<String, String> createProps = new HashMap<>();
        List<String> notAViewList = new ArrayList<>();
        JsonObject mountJson = null;
        for (int i = 0; i < gotOwnedAttributes.size(); i++) {
            JsonObject ownedAttribute = JsonUtil.getOptObject(gotOwnedAttributes, i);
            if (ownedAttribute.get(Sjm.TYPE).getAsString().equals("Property")) {
                if (!JsonUtil.getOptString(ownedAttribute, Sjm.TYPEID).equals("")) {
                    if (!newChildViewsSet.contains(ownedAttribute.get(Sjm.TYPEID).getAsString())) {
                        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(JsonUtil.getOptString(ownedAttribute, Sjm.PROJECTID),
                            JsonUtil.getOptString(ownedAttribute, Sjm.REFID));
                        JsonArray childViews = new JsonArray();
                        Set<String> childViewsSet = new HashSet<>();
                        childViewsSet.add(ownedAttribute.get(Sjm.TYPEID).getAsString());
                        try {
                            if (mountJson == null) {
                                mountJson =
                                    getProjectWithFullMounts(JsonUtil.getOptString(ownedAttribute, Sjm.PROJECTID),
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
                                    processChildViewDeleteCommit(ownedAttribute, deletedElements, oldElasticIds,
                                        commitDeleted);

                                    JsonObject asi = emsNodeUtil.getNodeBySysmlid(
                                        JsonUtil.getOptString(ownedAttribute, Sjm.APPLIEDSTEREOTYPEINSTANCEID));
                                    processChildViewDeleteCommit(asi, deletedElements, oldElasticIds, commitDeleted);

                                    JsonObject association = emsNodeUtil
                                        .getNodeBySysmlid(JsonUtil.getOptString(ownedAttribute, Sjm.ASSOCIATIONID));
                                    processChildViewDeleteCommit(association, deletedElements, oldElasticIds,
                                        commitDeleted);

                                    JsonArray associationProps = JsonUtil.getOptArray(association, Sjm.OWNEDENDIDS);
                                    for (int k = 0; k < associationProps.size(); k++) {
                                        if (!JsonUtil.getOptString(associationProps, k).equals("")) {
                                            JsonObject assocProp = emsNodeUtil
                                                .getNodeBySysmlid(JsonUtil.getOptString(associationProps, k));
                                            processChildViewDeleteCommit(assocProp, deletedElements, oldElasticIds,
                                                commitDeleted);
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
            } else {
                notAViewList.add(ownedAttribute.get(Sjm.SYSMLID).getAsString());
            }
        }

        for (int i = 0; i < newChildViews.size(); i++) {
            JsonObject child = newChildViews.get(i).getAsJsonObject();
            if (child.has(Sjm.SYSMLID)) {
                if (createProps.containsKey(child.get(Sjm.SYSMLID).getAsString())) {
                    if (!ownedAttributesIds.toString()
                        .contains(createProps.get(child.get(Sjm.SYSMLID).getAsString()))) {
                        ownedAttributesIds.add(createProps.get(child.get(Sjm.SYSMLID).getAsString()));
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
                    newASI.add(Sjm.SYSMLID, propertyASI.get(Sjm.SYSMLID));
                    newASI.add(Sjm.ELASTICID, propertyASI.get(Sjm.ELASTICID));
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
                    newAssociation.add(Sjm.SYSMLID, association.get(Sjm.SYSMLID));
                    newAssociation.add(Sjm.ELASTICID, association.get(Sjm.ELASTICID));
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
                    newAssociationProperty.add(Sjm.SYSMLID, assocProperty.get(Sjm.SYSMLID));
                    newAssociationProperty.add(Sjm.ELASTICID, assocProperty.get(Sjm.ELASTICID));
                    commitAdded.add(newAssociationProperty);

                    ownedAttributesIds.add(propertySysmlId);
                }
            }
        }

        for (String id : notAViewList) {
            ownedAttributesIds.add(id);
        }

        element.add(Sjm.OWNEDATTRIBUTEIDS, ownedAttributesIds);
        element.remove(Sjm.CHILDVIEWS);
    }

    private void processChildViewDeleteCommit(JsonObject element, JsonArray deletedElements, Set<String> oldElasticIds,
        JsonArray commitDeleted) {
        if (!JsonUtil.getOptString(element, Sjm.SYSMLID).equals("")) {
            deletedElements.add(element);
            oldElasticIds.add(element.get(Sjm.ELASTICID).getAsString());
            JsonObject newObj = new JsonObject();
            newObj.add(Sjm.SYSMLID, element.get(Sjm.SYSMLID));
            newObj.add(Sjm.ELASTICID, element.get(Sjm.ELASTICID));
            commitDeleted.add(newObj);
        }
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

        List<String> seen = new ArrayList<>();

        while (o.has(Sjm.OWNERID) && !JsonUtil.getOptString(o, Sjm.OWNERID).isEmpty() && !o.get(Sjm.OWNERID)
            .getAsString().equals("null") && !seen.contains(o.get(Sjm.OWNERID).getAsString())) {
            String sysmlid = JsonUtil.getOptString(o, Sjm.OWNERID);
            seen.add(sysmlid);
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

            if (siteCharacterizationId == null && CommitUtil.isGroup(owner)) {
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

    public boolean isDeleted(String sysmlid) {
        return pgh.isDeleted(sysmlid);
    }

    public boolean orgExists(String orgName) {
        return orgName.equals("swsdp") || pgh.orgExists(orgName);
    }

    public boolean refExists(String refId) {
        return pgh.refExists(refId);
    }

    private boolean diffUpdateJson(JsonObject json, JsonObject existing, Map<Integer, String> rejection) {
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
                        rejection.put(HttpServletResponse.SC_CONFLICT, "Conflict Detected");
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

        for (Map.Entry<String, JsonElement> entry : original.entrySet()) {
            String attr = entry.getKey();
            if (!partial.has(attr)) {
                partial.add(attr, original.get(attr));
            }
        }
        return true;
    }

    private boolean isUpdated(JsonObject json, JsonObject existing, Map<Integer, String> rejection) {
        if (existing == null) {
            return false;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("New Element: " + json);
            logger.debug("Old Element: " + existing);
        }

        Map<String, Object> newElement = toMap(json);
        Map<String, Object> oldElement = toMap(existing);

        boolean equiv = isEquivalent(newElement, oldElement);

        if (equiv) {
            rejection.put(HttpServletResponse.SC_NOT_MODIFIED, "Is Equivalent");
        }

        return !equiv;
    }

    public JsonArray addExtendedInformation(JsonArray elements) {
        Map<String, Map<String, String>> sysmlid2qualified = calculateQualifiedInformation(elements);

        for (int i = 0; i < elements.size(); i++) {
            JsonObject element = elements.get(i).getAsJsonObject();
            JsonObject newElement = addExtendedInformationForElement(element, sysmlid2qualified);
            elements.set(i, newElement);
        }

        return elements;
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
                String siteId = current.get(Sjm.SITECHARACTERIZATIONID);
                if (siteId != null) {
                    element.addProperty(Sjm.SITECHARACTERIZATIONID, siteId);
                }
            }
        }

        return element;
    }

    public static void handleMountSearch(JsonObject mountsJson, boolean extended, boolean extraDocs,
        final Long maxDepth, Set<String> elementsToFind, JsonArray result) throws IOException {

        handleMountSearch(mountsJson, extended, extraDocs, maxDepth, elementsToFind, result, null, null);
    }

    public static void handleMountSearch(JsonObject mountsJson, boolean extended, boolean extraDocs,
        final Long maxDepth, Set<String> elementsToFind, JsonArray result, String timestamp, String type)
        throws IOException {
        if (elementsToFind.isEmpty() || mountsJson == null) {
            return;
        }

        EmsNodeUtil emsNodeUtil =
            new EmsNodeUtil(mountsJson.get(Sjm.SYSMLID).getAsString(), mountsJson.get(Sjm.REFID).getAsString());
        JsonArray nodeList;
        Set<String> foundElements = new HashSet<>();
        JsonArray curFound = new JsonArray();
        if (timestamp != null) {
            extended = false;
            extraDocs = false;

            if (type != null && type.contains("artifacts")) {
                nodeList = emsNodeUtil.getArtifactsBySysmlids(elementsToFind, false, true);
            } else {
                nodeList = emsNodeUtil.getNodesBySysmlids(elementsToFind, false, true);
            }

            JsonArray nearestCommitId =
                emsNodeUtil.getNearestCommitFromTimestamp(mountsJson.get(Sjm.REFID).getAsString(), timestamp, 1);

            if (nearestCommitId.size() > 0 && nearestCommitId.get(0).getAsJsonObject().has(Sjm.SYSMLID)) {
                for (int i = 0; i < nodeList.size(); i++) {
                    String id = nodeList.get(i).getAsJsonObject().get(Sjm.SYSMLID).getAsString();
                    JsonObject obj = emsNodeUtil.getElementAtCommit(id,
                        nearestCommitId.get(0).getAsJsonObject().get(Sjm.SYSMLID).getAsString());
                    if (obj != null) {
                        curFound.add(obj);
                        foundElements.add(id);
                    }
                }
            }
        } else {
            if (type != null && type.contains("artifacts")) {
                nodeList = emsNodeUtil.getArtifactsBySysmlids(elementsToFind, false, false);
            } else {
                nodeList = emsNodeUtil.getNodesBySysmlids(elementsToFind, true, false);
            }
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
            mountsJson = emsNodeUtil.getProjectWithFullMounts(mountsJson.get(Sjm.SYSMLID).getAsString(),
                mountsJson.get(Sjm.REFID).getAsString(), null);
        }
        JsonArray mountsArray = mountsJson.get(Sjm.MOUNTS).getAsJsonArray();

        for (int i = 0; i < mountsArray.size(); i++) {
            handleMountSearch(mountsArray.get(i).getAsJsonObject(), extended, extraDocs, maxDepth, elementsToFind,
                result, timestamp, type);
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

    public JsonArray getCommitObjects(List<String> commitIds) {
        try {
            return eh.getElementsFromElasticIds(commitIds, projectId);
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
            ElasticResult r = eh.indexElement(o, projectId, ElasticHelper.ELEMENT);
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
            Object val = getValue(value);
            map.put(key, val);
        }

        return map;
    }

    public static List<Object> toList(JsonArray array) {
        List<Object> list = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            JsonElement value = array.get(i);
            Object val = getValue(value);
            list.add(val);
        }

        return list;
    }

    public static Object getValue(JsonElement value) {
        Object val;

        if (value.isJsonArray()) {
            val = toList(value.getAsJsonArray());
        } else if (value.isJsonObject()) {
            val = toMap(value.getAsJsonObject());
        } else if (value.isJsonNull()) {
            val = null;
        } else if (value.isJsonPrimitive()) {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                val = primitive.getAsBoolean();
            } else if (primitive.isString()) {
                val = primitive.getAsString();
            } else if (primitive.isNumber()) {
                val = primitive.getAsNumber();
            } else {
                val = primitive;
            }
        } else {
            val = value;
        }

        return val;
    }

    @SuppressWarnings("unchecked")
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
                    if (!isEquivalent((Map<String, Object>) value1, (Map<String, Object>) value2)) {
                        return false;
                    }
                }
            } else if (value1 instanceof List) {
                if (!(value2 instanceof List)) {
                    return false;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Is Equivalent: " + isEquivalent((List<Object>) value1, (List<Object>) value2));
                    }
                    if (!isEquivalent((List<Object>) value1, (List<Object>) value2)) {
                        return false;
                    }
                }
            } else if (value1 instanceof String) {
                if (!(value2 instanceof String)) {
                    return false;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Is Equivalent: " + value1.equals(value2));
                    }
                    if (!value1.equals(value2)) {
                        return false;
                    }
                }
            } else if (value1 instanceof Number) {
                if (!(value2 instanceof Number)) {
                    return false;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Is Equivalent: " + value1.equals(value2));
                    }
                    if (!value1.equals(value2)) {
                        return false;
                    }
                }
            } else if (value1 instanceof Boolean) {
                if (!(value2 instanceof Boolean)) {
                    return false;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Is Equivalent: " + value1 != value2);
                    }
                    if (value1 != value2) {
                        return false;
                    }
                }
            } else {
                if (logger.isDebugEnabled()) {
                    if (value1 != null) {
                        logger.debug("Value 1 Type: " + value1.getClass());
                    }
                    if (value2 != null) {
                        logger.debug("Value 2 Type: " + value2.getClass());
                    }
                }
                if (value1 != value2) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isEquivalent(List<Object> list1, List<Object> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }

        for (int i = 0; i < list1.size(); i++) {
            Map<String, Object> toTestMap = new HashMap<>();
            Map<String, Object> testAgainstMap = new HashMap<>();

            toTestMap.put("fromList", list1.get(i));
            testAgainstMap.put("fromList", list2.get(i));

            if (!isEquivalent(toTestMap, testAgainstMap)) {
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

    public JsonObject getModelAtCommit(String commitId) {
        JsonObject result = new JsonObject();
        JsonArray elements = new JsonArray();
        JsonArray artifacts = new JsonArray();
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
            List<String> artifactElasticIds = new ArrayList<>();
            for (Map<String, Object> n : pgh.getAllNodesWithLastCommitTimestamp()) {
                processElementForModelAtCommit(n, deletedElementIds, commit, commitId, refsCommitsIds, elements,
                    elasticIds);
            }

            for (Map<String, Object> a : pgh.getAllArtifactsWithLastCommitTimestamp()) {
                processElementForModelAtCommit(a, deletedElementIds, commit, commitId, refsCommitsIds, artifacts,
                    artifactElasticIds);
            }

            try {
                JsonArray elems = eh.getElementsFromElasticIds(elasticIds, projectId);
                for (int i = 0; i < elems.size(); i++) {
                    elements.add(elems.get(i));
                }
                JsonArray artifactElastic = eh.getElementsFromElasticIds(artifactElasticIds, projectId);
                for (int i = 0; i < artifactElastic.size(); i++) {
                    artifacts.add(artifactElastic.get(i));
                }
            } catch (IOException e) {
                logger.error("Error getting model: ", e);
            }
            result.add(Sjm.ELEMENTS, elements);
            result.add(Sjm.ARTIFACTS, artifacts);
        }
        return result;
    }

    public void processElementForModelAtCommit(Map<String, Object> element, Map<String, String> deletedElementIds,
        Map<String, Object> commit, String commitId, List<String> refsCommitsIds, JsonArray elements,
        List<String> elasticIds) {
        JsonObject pastElement = null;
        if (((Date) element.get(Sjm.TIMESTAMP)).getTime() <= ((Date) commit.get(Sjm.TIMESTAMP)).getTime()) {
            if (!deletedElementIds.containsKey((String) element.get(Sjm.ELASTICID))) {
                elasticIds.add((String) element.get(Sjm.ELASTICID));
            }
        } else {
            pastElement = getElementAtCommit((String) element.get(Sjm.SYSMLID), commitId, refsCommitsIds);
        }

        if (pastElement != null && pastElement.has(Sjm.SYSMLID) && !deletedElementIds
            .containsKey(pastElement.get(Sjm.ELASTICID).getAsString())) {
            elements.add(pastElement);
        }
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
                .containsKey(pastElement.get(Sjm.ELASTICID).getAsString())) {
                pastElement = new JsonObject();
            }
        }
        return pastElement == null ? new JsonObject() : pastElement;
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

    public JsonArray getNearestCommitFromTimestamp(String refId, String timestamp, int limit) {
        Date requestedTime;
        List<Map<String, Object>> commits;
        JsonArray response = new JsonArray();
        try {
            requestedTime = df.parse(timestamp);
            Timestamp time = new Timestamp(requestedTime.getTime());
            commits = pgh.getRefsCommits(refId, time, limit);
            if (!commits.isEmpty()) {
                response = processCommits(commits);
            }
        } catch (ParseException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }

        return response;
    }

    public JsonArray processCommits(List<Map<String, Object>> commits) {
        JsonArray result = new JsonArray();
        Map<String, JsonObject> commitObjectMap = new HashMap<>();
        List<String> commitIds = new ArrayList<>();
        for (int i = 0; i < commits.size(); i++) {
            commitIds.add(commits.get(i).get(Sjm.SYSMLID).toString());
        }

        JsonArray commitObjects = getCommitObjects(commitIds);
        for (int i = 0; i < commitObjects.size(); i++) {
            if (commitObjects.get(i).isJsonObject() && commitObjects.get(i).getAsJsonObject().has(Sjm.ELASTICID)) {
                commitObjectMap.put(commitObjects.get(i).getAsJsonObject().get(Sjm.ELASTICID).toString(),
                    commitObjects.get(i).getAsJsonObject());
            }
        }

        for (int i = 0; i < commits.size(); i++) {
            Map<String, Object> refCommit = commits.get(i);
            JsonObject commitJson = commitObjectMap.getOrDefault(refCommit.get(Sjm.SYSMLID).toString(), null);
            JsonObject commit = new JsonObject();
            commit.addProperty(Sjm.SYSMLID, refCommit.get(Sjm.SYSMLID).toString());
            commit.addProperty(Sjm.CREATOR, refCommit.get(Sjm.CREATOR).toString());
            commit.addProperty(Sjm.CREATED, df.format(refCommit.get(Sjm.CREATED)));
            if (commitJson != null && commitJson.has(Sjm.COMMENT)) {
                commit.addProperty(Sjm.COMMENT, commitJson.get(Sjm.COMMENT).toString());
            }
            result.add(commit);
        }

        return result;
    }

    public List<String> getModel() {
        List<String> model = new ArrayList();

        try {
            model = pgh.getAllNodes();

        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return model;
    }

    public static String md5Hash(String str) {
        return DigestUtils.md5Hex(str);
    }

    public JsonObject getProfile(String id) throws IOException {
        return eh.getProfileByElasticId(id, "mms");
    }

    public static String md5Hash(File file) {
        String digest = null;
        try (FileInputStream fin = new FileInputStream(file)) {
            digest = DigestUtils.md5Hex(fin); //used to get MD5
        } catch (Exception e) {
            logger.error(e);
        }
        return digest;

    }

    public Artifact getArtifact(String sysmlid, boolean withDeleted) {
        return pgh.getArtifactFromSysmlId(sysmlid, withDeleted);
    }

    public static Path saveToFilesystem(String filename, InputStream content) throws Throwable {
        File tempDir = TempFileProvider.getTempDir();
        Path filePath = Paths.get(tempDir.getAbsolutePath(), filename);
        File file = new File(filePath.toString());

        try (FileOutputStream out = new FileOutputStream(file)) {
            file.mkdirs();
            int i = 0;
            byte[] b = new byte[1024];
            while ((i = content.read(b)) != -1) {
                out.write(b, 0, i);
            }
            out.flush();
            out.close();
            return filePath;
        } catch (Throwable ex) {
            throw new Throwable("Failed to save file to filesystem. " + ex.getMessage());
        }
    }

    public static String getHostname() {
        ServiceDescriptorRegistry sdr = new ServiceDescriptorRegistry();
        return sdr.getSysAdminParams().getAlfrescoHost();
    }

    /**
     * getModuleService Retrieves the ModuleService of the ServiceRegistry passed in
     *
     * @param services ServiceRegistry object that contains the desired ModuleService
     * @return ModuleService
     */
    public static ModuleService getModuleService(ServiceRegistry services) {
        if (services == null) {
            return null;
        }
        // Takes the ServiceRegistry and calls the ModuleService super method
        // getService(Creates an Alfresco QName using the namespace
        // service and passes in the default URI
        return (ModuleService) services.getService(QName.createQName(NamespaceService.ALFRESCO_URI, "ModuleService"));
    }

    /**
     * getServiceModules
     *
     * Returns a JSONArray of Module Details from the Service Modules
     *
     * @param service the service containing modules to be returned
     * @return JSONArray of ModuleDetails within the ModuleService object
     */
    public static JsonArray getServiceModulesJson(ModuleService service) {

        JsonArray jsonArray = new JsonArray();
        List<ModuleDetails> modules = service.getAllModules();
        for (ModuleDetails detail : modules) {
            JsonObject jsonModule = moduleDetailsToJson(detail);
            jsonArray.add(jsonModule);
        }
        return jsonArray;
    }

    /**
     * moduleDetailsToJson
     *
     * Takes a module of type ModuleDetails and retrieves all off the module's members and puts them
     * into a newly instantiated JSONObject.
     *
     * JSONObject will have the details : title, version, aliases, class, dependencies, editions id
     * and properties
     *
     * @param module A single module of type ModuleDetails
     * @return JSONObject which contains all the details of that module
     */
    public static JsonObject moduleDetailsToJson(ModuleDetails module) {
        JsonObject jsonModule = new JsonObject();
        jsonModule.addProperty("mmsTitle", module.getTitle());
        jsonModule.addProperty("mmsVersion", module.getModuleVersionNumber().toString());
        JsonUtil.addStringList(jsonModule, "mmsAliases", module.getAliases());
        jsonModule.addProperty("mmsClass", module.getClass().toString());
        JsonArray depArray = new JsonArray();
        for (ModuleDependency depend: module.getDependencies())
            depArray.add(depend.toString());
        jsonModule.add("mmsDependencies", depArray);
        JsonUtil.addStringList(jsonModule, "mmsEditions", module.getEditions());
        jsonModule.addProperty("mmsId", module.getId());
        JsonObject propObj = new JsonObject();
        Enumeration<?> enumerator = module.getProperties().propertyNames();
        while (enumerator.hasMoreElements()) {
            String key = (String)enumerator.nextElement();
            propObj.addProperty(key, module.getProperties().getProperty(key));
        }
        jsonModule.add("mmsProperties", propObj);
        return jsonModule;
    }

    /**
     * getMMSversion
     *
     * Gets the version number of a module, returns a JSONObject which calls on getString with
     * 'version' as an argument. This will return a String representing the version of the
     * mms.
     *
     * @return Version number of the MMS as type String
     */
    public static String getMMSversion(ServiceRegistry services) {
        ModuleService service = getModuleService(services);
        JsonArray moduleDetails = getServiceModulesJson(service);
        String mmsVersion = "NA";
        for (int i = 0; i < moduleDetails.size(); i++) {
            JsonObject o = moduleDetails.get(i).getAsJsonObject();
            if (o.get("mmsId").getAsString().equalsIgnoreCase("mms-amp")) {
                mmsVersion = o.get("mmsVersion").getAsString();
            }
        }

        // Remove appended tags from version
        int endIndex = mmsVersion.indexOf('-');
        return endIndex > -1 ? mmsVersion.substring(0, endIndex) : mmsVersion;
    }
}
