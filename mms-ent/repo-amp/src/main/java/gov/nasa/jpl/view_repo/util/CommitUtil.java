package gov.nasa.jpl.view_repo.util;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hazelcast.config.Config;
import com.hazelcast.config.ItemListenerConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import gov.nasa.jpl.view_repo.util.tasks.BranchTask;
import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.connections.JmsConnection;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.ElasticResult;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.db.GraphInterface.DbCommitTypes;
import gov.nasa.jpl.view_repo.db.GraphInterface.DbEdgeTypes;
import gov.nasa.jpl.view_repo.db.GraphInterface.DbNodeTypes;

/**
 * Utilities for saving commits and sending out deltas based on commits
 *
 * @author cinyoung
 */
public class CommitUtil {
    static Logger logger = Logger.getLogger(CommitUtil.class);

    public static final String TYPE_BRANCH = "BRANCH";
    public static final String TYPE_COMMIT = "COMMIT";
    public static final String TYPE_DELTA = "DELTA";
    public static final String TYPE_MERGE = "MERGE";

    private static final String NODES = "nodes";
    private static final String EDGES = "edges";
    private static final String PARENT = "parent";
    private static final String CHILD = "child";
    private static final String NODETYPE = "nodetype";
    private static final String EDGETYPE = "edgetype";
    private static final String DELETED = "deleted";
    private static final String INITIALCOMMIT = "initialcommit";
    private static final String LASTCOMMIT = "lastcommit";
    private static final String ARTIFACTS = "artifacts";
    private static final String CONTENTTYPE = "contentType";

    private static final String HOLDING_BIN_PREFIX = "holding_bin_";

    private static ElasticHelper eh = null;
    private static JmsConnection jmsConnection = null;

    private static String user = null;

    private static HazelcastInstance hzInstance = null;

    public static void setJmsConnection(JmsConnection jmsConnection) {
        if (logger.isInfoEnabled()) {
            logger.info("Setting jms");
        }
        CommitUtil.jmsConnection = jmsConnection;
    }

    public static void initHazelcastClient() {
        if (hzInstance == null) {
            Config config = new Config();
            hzInstance = Hazelcast.newHazelcastInstance(config);
        }
    }

    private static void initHazelcastQueue(String name) {
        Config config = hzInstance.getConfig();
        QueueConfig queueConfig = config.getQueueConfig(name);
        if (queueConfig.getItemListenerConfigs().isEmpty()) {
            logger.info("Creating new queue named: " + name);
            queueConfig.addItemListenerConfig(new ItemListenerConfig("gov.nasa.jpl.view_repo.util.QueueConsumer", true));
            config.addQueueConfig(queueConfig);
        }
    }

    public static DbNodeTypes getNodeType(JSONObject e) {

        String type = e.optString(Sjm.TYPE).toLowerCase();

        if ((type.contains("class") || type.contains("generalization") || type.contains("dependency")) && e
            .has(Sjm.APPLIEDSTEREOTYPEIDS)) {
            JSONArray typeArray = e.optJSONArray(Sjm.APPLIEDSTEREOTYPEIDS);

            for (int i = 0; i < typeArray.length(); i++) {
                String typeString = typeArray.getString(i);
                if (Sjm.STEREOTYPEIDS.containsKey(typeString)) {
                    if (!type.equals("document")) {
                        type = Sjm.STEREOTYPEIDS.get(typeString);
                    }
                }
            }
        }

        switch (type) {
            case "site":
                return DbNodeTypes.SITE;
            case "project":
                return DbNodeTypes.PROJECT;
            case "model":
            case "package":
                if (isSite(e)) {
                    return DbNodeTypes.SITEANDPACKAGE;
                } else {
                    return DbNodeTypes.PACKAGE;
                }
            case "document":
                return DbNodeTypes.DOCUMENT;
            case "comment":
                return DbNodeTypes.COMMENT;
            case "constraint":
                return DbNodeTypes.CONSTRAINT;
            case "instancespecification":
                return DbNodeTypes.INSTANCESPECIFICATION;
            case "operation":
                return DbNodeTypes.OPERATION;
            case "property":
                return DbNodeTypes.PROPERTY;
            case "parameter":
                return DbNodeTypes.PARAMETER;
            case "view":
                return DbNodeTypes.VIEW;
            case "viewpoint":
                return DbNodeTypes.VIEWPOINT;
            case "mount":
                return DbNodeTypes.MOUNT;
            default:
                return DbNodeTypes.ELEMENT;
        }
    }

    public static boolean isSite(JSONObject element) {
        return element.has(Sjm.ISSITE) && element.getBoolean(Sjm.ISSITE);
    }

    private static boolean bulkElasticEntry(JSONArray elements, String operation, boolean refresh, String index, String type) {
        if (elements.length() > 0) {
            try {
                boolean bulkEntry = eh.bulkIndexElements(elements, operation, refresh, index, type);
                if (!bulkEntry) {
                    return false;
                }
            } catch (IOException e) {
                logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
                return false;
            }
        }

        return true;
    }

    public static boolean isPartProperty(JSONObject e) {
        if (!e.has(Sjm.TYPE) || !e.getString(Sjm.TYPE).equals("Property")) {
            return false;
        }
        JSONArray appliedS = e.optJSONArray(Sjm.APPLIEDSTEREOTYPEIDS);
        if (appliedS == null || appliedS.length() == 0) {
            return false;
        }
        for (int i = 0; i < appliedS.length(); i++) {
            String s = appliedS.getString(i);
            if (Sjm.PROPERTYSIDS.containsValue(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean processArtifactDeltasForDb(JSONObject delta, String projectId, String refId,
        JSONObject jmsPayload) {
        PostgresHelper pgh = new PostgresHelper();
        pgh.setProject(projectId);
        pgh.setWorkspace(refId);

        JSONArray added = delta.optJSONArray("addedElements");
        JSONArray updated = delta.optJSONArray("updatedElements");
        JSONArray deleted = delta.optJSONArray("deletedElements");

        String creator = delta.getJSONObject("commit").getString(Sjm.CREATOR);
        String commitElasticId = delta.getJSONObject("commit").getString(Sjm.ELASTICID);

        JSONObject jmsWorkspace = new JSONObject();
        JSONArray jmsAdded = new JSONArray();
        JSONArray jmsUpdated = new JSONArray();
        JSONArray jmsDeleted = new JSONArray();

        List<String> deletedSysmlIds = new ArrayList<>();
        if (bulkElasticEntry(added, "added", false, projectId, "artifact") && bulkElasticEntry(updated, "updated", false,
            projectId, "artifact")) {
            try {
                List<Map<String, String>> artifactInserts = new ArrayList<>();
                List<Map<String, String>> artifactUpdates = new ArrayList<>();

                for (int i = 0; i < added.length(); i++) {
                    JSONObject e = added.getJSONObject(i);
                    Map<String, String> artifact = new HashMap<>();
                    jmsAdded.put(e.getString(Sjm.SYSMLID));

                    if (e.has(Sjm.ELASTICID)) {
                        artifact.put(Sjm.ELASTICID, e.getString(Sjm.ELASTICID));
                        artifact.put(Sjm.SYSMLID, e.getString(Sjm.SYSMLID));
                        artifact.put(INITIALCOMMIT, e.getString(Sjm.ELASTICID));
                        artifact.put(LASTCOMMIT, commitElasticId);
                        artifact.put(CONTENTTYPE, e.getString(Sjm.CONTENTTYPE));
                        artifactInserts.add(artifact);
                    }
                }

                for (int i = 0; i < deleted.length(); i++) {
                    JSONObject e = deleted.getJSONObject(i);
                    jmsDeleted.put(e.getString(Sjm.SYSMLID));
                    pgh.deleteArtifact(e.getString(Sjm.SYSMLID));
                    deletedSysmlIds.add(e.getString(Sjm.SYSMLID));
                }

                for (int i = 0; i < updated.length(); i++) {
                    JSONObject e = updated.getJSONObject(i);
                    jmsUpdated.put(e.getString(Sjm.SYSMLID));

                    if (e.has(Sjm.ELASTICID)) {
                        Map<String, String> updatedArtifact = new HashMap<>();
                        updatedArtifact.put(Sjm.ELASTICID, e.getString(Sjm.ELASTICID));
                        updatedArtifact.put(Sjm.SYSMLID, e.getString(Sjm.SYSMLID));
                        updatedArtifact.put(DELETED, "false");
                        updatedArtifact.put(LASTCOMMIT, commitElasticId);
                        artifactUpdates.add(updatedArtifact);
                    }
                }

                List<String> nullParents;
                Savepoint sp = null;
                try {//do node insert, updates, and containment edge updates
                    //do bulk delete edges for affected sysmlids here - delete containment, view and childview
                    sp = pgh.startTransaction();
                    pgh.runBulkQueries(artifactInserts, ARTIFACTS);
                    pgh.runBulkQueries(artifactUpdates, "artifactUpdates");
                    pgh.updateBySysmlIds(ARTIFACTS, LASTCOMMIT, commitElasticId, deletedSysmlIds);
                    pgh.commitTransaction();
                    pgh.insertCommit(commitElasticId, DbCommitTypes.COMMIT, creator);
                    nullParents = pgh.findNullParents();
                    if (nullParents != null) {
                        updateNullEdges(nullParents, projectId);
                    }
                } catch (Exception e) {
                    try {
                        pgh.rollBackToSavepoint(sp);
                    } catch (SQLException se) {
                        logger.error(String.format("%s", LogUtil.getStackTrace(se)));
                    }
                    logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                    return false;
                } finally {
                    pgh.close();
                }
                try {
                    eh.indexElement(delta, projectId); //initial commit may fail to read back but does get indexed
                } catch (Exception e) {
                    logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                }

            } catch (Exception e1) {
                logger.warn("Could not complete graph storage");
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("%s", LogUtil.getStackTrace(e1)));
                }
                return false;
            }

        } else {
            logger.error("Elasticsearch insert error occurred");
            return false;
        }

        jmsWorkspace.put("addedElements", jmsAdded);
        jmsWorkspace.put("updatedElements", jmsUpdated);
        jmsWorkspace.put("deletedElements", jmsDeleted);

        jmsPayload.put("refs", jmsWorkspace);

        return true;

    }

    private static boolean processDeltasForDb(SerialJSONObject delta, String projectId, String refId, JSONObject jmsPayload,
        boolean withChildViews, ServiceRegistry services) {
        // :TODO write to elastic for elements, write to postgres, write to elastic for commits
        // :TODO should return a 500 here to stop writes if one insert fails
        PostgresHelper pgh = new PostgresHelper();
        pgh.setProject(projectId);
        pgh.setWorkspace(refId);

        SerialJSONArray added = delta.optJSONArray("addedElements");
        SerialJSONArray updated = delta.optJSONArray("updatedElements");
        SerialJSONArray deleted = delta.optJSONArray("deletedElements");

        String creator = delta.getJSONObject("commit").getString(Sjm.CREATOR);
        String commitElasticId = delta.getJSONObject("commit").getString(Sjm.ELASTICID);

        SerialJSONObject jmsWorkspace = new SerialJSONObject();
        SerialJSONArray jmsAdded = new SerialJSONArray();
        SerialJSONArray jmsUpdated = new SerialJSONArray();
        SerialJSONArray jmsDeleted = new SerialJSONArray();

        List<String> deletedSysmlIds = new ArrayList<>();
        List<Pair<String, String>> addEdges = new ArrayList<>();
        List<Pair<String, String>> viewEdges = new ArrayList<>();
        List<Pair<String, String>> childViewEdges = new ArrayList<>();

        if (bulkElasticEntry(added, "added", withChildViews, projectId, "element") && bulkElasticEntry(updated, "updated",
            withChildViews, projectId, "element")) {

            try {
                List<Map<String, String>> nodeInserts = new ArrayList<>();
                List<Map<String, String>> edgeInserts = new ArrayList<>();
                List<Map<String, String>> childEdgeInserts = new ArrayList<>();
                List<Map<String, String>> nodeUpdates = new ArrayList<>();
                Set<String> uniqueEdge = new HashSet<>();

                for (int i = 0; i < added.length(); i++) {
                    SerialJSONObject e = added.getJSONObject(i);
                    Map<String, String> node = new HashMap<>();
                    jmsAdded.put(e.getString(Sjm.SYSMLID));
                    int nodeType = getNodeType(e).getValue();

                    if (e.has(Sjm.ELASTICID)) {
                        node.put(Sjm.ELASTICID, e.getString(Sjm.ELASTICID));
                        node.put(Sjm.SYSMLID, e.getString(Sjm.SYSMLID));
                        node.put(NODETYPE, Integer.toString(nodeType));
                        node.put(INITIALCOMMIT, e.getString(Sjm.ELASTICID));
                        node.put(LASTCOMMIT, commitElasticId);
                        nodeInserts.add(node);
                    }

                    if (e.has(Sjm.OWNERID) && e.getString(Sjm.OWNERID) != null && e.getString(Sjm.SYSMLID) != null) {
                        Pair<String, String> p = new Pair<>(e.getString(Sjm.OWNERID), e.getString(Sjm.SYSMLID));
                        addEdges.add(p);
                    }

                    String doc = e.optString(Sjm.DOCUMENTATION);
                    if (doc != null && !doc.equals("")) {
                        processDocumentEdges(e.getString(Sjm.SYSMLID), doc, viewEdges);
                    }

                    if (nodeType == DbNodeTypes.SITEANDPACKAGE.getValue()) {
                        createOrUpdateSiteChar(e, projectId, refId, services);
                    }
                    String type = e.optString(Sjm.TYPE);
                    if (type.equals("Slot") || type.equals("Property") || type.equals("Port")) {
                        processValueEdges(e, viewEdges);
                    }
                    if (e.has(Sjm.CONTENTS)) {
                        SerialJSONObject contents = e.optJSONObject(Sjm.CONTENTS);
                        if (contents != null) {
                            processContentsJson(e.getString(Sjm.SYSMLID), contents, viewEdges);
                        }
                    } else if (e.has(Sjm.SPECIFICATION) && nodeType == DbNodeTypes.INSTANCESPECIFICATION.getValue()) {
                        SerialJSONObject iss = e.optJSONObject(Sjm.SPECIFICATION);
                        if (iss != null) {
                            processInstanceSpecificationSpecificationJson(e.getString(Sjm.SYSMLID), iss, viewEdges);
                            processContentsJson(e.getString(Sjm.SYSMLID), iss, viewEdges);
                        }
                    }
                    if (nodeType == DbNodeTypes.VIEW.getValue() || nodeType == DbNodeTypes.DOCUMENT.getValue()) {
                        SerialJSONArray owned = e.optJSONArray(Sjm.OWNEDATTRIBUTEIDS);
                        if (owned != null) {
                            for (int j = 0; j < owned.length(); j++) {
                                Pair<String, String> p = new Pair<>(e.getString(Sjm.SYSMLID), owned.getString(j));
                                childViewEdges.add(p);
                            }
                        }
                    }
                    if (isPartProperty(e)) {
                        String typeid = e.optString(Sjm.TYPEID);
                        if (typeid != null) {
                            Pair<String, String> p = new Pair<>(e.getString(Sjm.SYSMLID), typeid);
                            childViewEdges.add(p);
                        }
                    }
                }

                for (int i = 0; i < deleted.length(); i++) {
                    SerialJSONObject e = deleted.getJSONObject(i);
                    jmsDeleted.put(e.getString(Sjm.SYSMLID));
                    pgh.deleteEdgesForNode(e.getString(Sjm.SYSMLID));
                    pgh.deleteNode(e.getString(Sjm.SYSMLID));
                    deletedSysmlIds.add(e.getString(Sjm.SYSMLID));
                }

                for (int i = 0; i < updated.length(); i++) {
                    SerialJSONObject e = updated.getJSONObject(i);
                    jmsUpdated.put(e.getString(Sjm.SYSMLID));
                    int nodeType = getNodeType(e).getValue();
                    pgh.deleteEdgesForNode(e.getString(Sjm.SYSMLID), true, DbEdgeTypes.CONTAINMENT);
                    pgh.deleteEdgesForNode(e.getString(Sjm.SYSMLID), false, DbEdgeTypes.VIEW);
                    pgh.deleteEdgesForNode(e.getString(Sjm.SYSMLID), false, DbEdgeTypes.CHILDVIEW);

                    if (e.has(Sjm.OWNERID) && e.getString(Sjm.OWNERID) != null && e.getString(Sjm.SYSMLID) != null) {
                        Pair<String, String> p = new Pair<>(e.getString(Sjm.OWNERID), e.getString(Sjm.SYSMLID));
                        addEdges.add(p);
                    }
                    String doc = e.optString(Sjm.DOCUMENTATION);
                    if (doc != null && !doc.equals("")) {
                        processDocumentEdges(e.getString(Sjm.SYSMLID), doc, viewEdges);
                    }

                    if (nodeType == DbNodeTypes.SITEANDPACKAGE.getValue()) {
                        createOrUpdateSiteChar(e, projectId, refId, services);
                    }
                    String type = e.optString(Sjm.TYPE);
                    if (type.equals("Slot") || type.equals("Property") || type.equals("Port")) {
                        processValueEdges(e, viewEdges);
                    }
                    if (e.has(Sjm.CONTENTS)) {
                        SerialJSONObject contents = e.optJSONObject(Sjm.CONTENTS);
                        if (contents != null) {
                            processContentsJson(e.getString(Sjm.SYSMLID), contents, viewEdges);
                        }
                    } else if (e.has(Sjm.SPECIFICATION) && nodeType == DbNodeTypes.INSTANCESPECIFICATION.getValue()) {
                        SerialJSONObject iss = e.optJSONObject(Sjm.SPECIFICATION);
                        if (iss != null) {
                            processInstanceSpecificationSpecificationJson(e.getString(Sjm.SYSMLID), iss, viewEdges);
                            processContentsJson(e.getString(Sjm.SYSMLID), iss, viewEdges);
                        }
                    }
                    if (nodeType == DbNodeTypes.VIEW.getValue() || nodeType == DbNodeTypes.DOCUMENT.getValue()) {
                        SerialJSONArray owned = e.optJSONArray(Sjm.OWNEDATTRIBUTEIDS);
                        if (owned != null) {
                            for (int j = 0; j < owned.length(); j++) {
                                Pair<String, String> p = new Pair<>(e.getString(Sjm.SYSMLID), owned.getString(j));
                                childViewEdges.add(p);
                            }
                        }
                    }
                    if (isPartProperty(e)) {
                        String typeid = e.optString(Sjm.TYPEID);
                        if (typeid != null) {
                            Pair<String, String> p = new Pair<>(e.getString(Sjm.SYSMLID), typeid);
                            childViewEdges.add(p);
                        }
                    }
                    if (e.has(Sjm.ELASTICID)) {
                        Map<String, String> updatedNode = new HashMap<>();
                        updatedNode.put(Sjm.ELASTICID, e.getString(Sjm.ELASTICID));
                        updatedNode.put(Sjm.SYSMLID, e.getString(Sjm.SYSMLID));
                        updatedNode.put(NODETYPE, Integer.toString(getNodeType(e).getValue()));
                        updatedNode.put(DELETED, "false");
                        updatedNode.put(LASTCOMMIT, commitElasticId);
                        nodeUpdates.add(updatedNode);
                    }
                }

                for (Pair<String, String> e : addEdges) {
                    if (!pgh.edgeExists(e.first, e.second, DbEdgeTypes.CONTAINMENT)) {
                        String edgeTest = e.first + e.second + DbEdgeTypes.CONTAINMENT.getValue();
                        if (!uniqueEdge.contains(edgeTest)) {
                            Map<String, String> edge = new HashMap<>();
                            edge.put(PARENT, e.first);
                            edge.put(CHILD, e.second);
                            edge.put(EDGETYPE, Integer.toString(DbEdgeTypes.CONTAINMENT.getValue()));
                            edgeInserts.add(edge);
                            uniqueEdge.add(edgeTest);
                        }
                    }
                }

                for (Pair<String, String> e : viewEdges) {
                    if (!pgh.edgeExists(e.first, e.second, DbEdgeTypes.VIEW)) {
                        String edgeTest = e.first + e.second + DbEdgeTypes.VIEW.getValue();
                        if (!uniqueEdge.contains(edgeTest)) {
                            Map<String, String> edge = new HashMap<>();
                            edge.put(PARENT, e.first);
                            edge.put(CHILD, e.second);
                            edge.put(EDGETYPE, Integer.toString(DbEdgeTypes.VIEW.getValue()));
                            childEdgeInserts.add(edge);
                            uniqueEdge.add(edgeTest);
                        }
                    }
                }
                for (Pair<String, String> e : childViewEdges) {
                    if (!pgh.edgeExists(e.first, e.second, DbEdgeTypes.CHILDVIEW)) {
                        String edgeTest = e.first + e.second + DbEdgeTypes.CHILDVIEW.getValue();
                        if (!uniqueEdge.contains(edgeTest)) {
                            Map<String, String> edge = new HashMap<>();
                            edge.put(PARENT, e.first);
                            edge.put(CHILD, e.second);
                            edge.put(EDGETYPE, Integer.toString(DbEdgeTypes.CHILDVIEW.getValue()));
                            childEdgeInserts.add(edge);
                            uniqueEdge.add(edgeTest);
                        }
                    }
                }
                List<String> nullParents;
                Savepoint sp = null;
                try {//do node insert, updates, and containment edge updates
                    //do bulk delete edges for affected sysmlids here - delete containment, view and childview
                    sp = pgh.startTransaction();
                    pgh.runBulkQueries(nodeInserts, NODES);
                    pgh.runBulkQueries(nodeUpdates, "updates");
                    pgh.updateBySysmlIds(NODES, LASTCOMMIT, commitElasticId, deletedSysmlIds);
                    pgh.commitTransaction();
                    pgh.insertCommit(commitElasticId, DbCommitTypes.COMMIT, creator);
                    sp = pgh.startTransaction();
                    pgh.runBulkQueries(edgeInserts, EDGES);
                    pgh.commitTransaction();
                    nullParents = pgh.findNullParents();
                    if (nullParents != null) {
                        updateNullEdges(nullParents, projectId);
                    }
                    pgh.cleanEdges();
                } catch (Exception e) {
                    try {
                        pgh.rollBackToSavepoint(sp);
                    } catch (SQLException se) {
                        logger.error(String.format("%s", LogUtil.getStackTrace(se)));
                    }
                    logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                    return false;
                } finally {
                    pgh.close();
                }
                try {//view and childview edge updates
                    sp = pgh.startTransaction();
                    pgh.runBulkQueries(childEdgeInserts, EDGES);
                    pgh.commitTransaction();
                    pgh.cleanEdges();
                } catch (Exception e) {
                    try {
                        pgh.rollBackToSavepoint(sp);
                    } catch (SQLException se) {
                        logger.error(String.format("%s", LogUtil.getStackTrace(se)));
                    }
                    logger.error(String.format("%s", LogUtil.getStackTrace(e))); //childedges are not critical
                } finally {
                    pgh.close();
                }
                try {
                    eh.indexElement(delta, projectId); //initial commit may fail to read back but does get indexed
                } catch (Exception e) {
                    logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                }

            } catch (Exception e1) {
                logger.warn("Could not complete graph storage");
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("%s", LogUtil.getStackTrace(e1)));
                }
                return false;
            }
        } else {
            logger.error("Elasticsearch insert error occurred");
            return false;
        }

        jmsWorkspace.put("addedElements", jmsAdded);
        jmsWorkspace.put("updatedElements", jmsUpdated);
        jmsWorkspace.put("deletedElements", jmsDeleted);

        jmsPayload.put("refs", jmsWorkspace);

        return true;
    }

    /**
     * Update edges where the parent is null to the holding bin + _projectId
     *
     * @param updateParents Parent edges that have null values
     * @return true if updated
     */
    public static boolean updateNullEdges(List<String> updateParents, String projectId) {
        try {
            eh = new ElasticHelper();
            Set<String> updateSet = new HashSet<>(updateParents);
            String owner = HOLDING_BIN_PREFIX + projectId;
            JSONObject query = new JSONObject();
            query.put("doc", new JSONObject().put("ownerId", owner));
            eh.bulkUpdateElements(updateSet, query.toString(), projectId, "element");
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            return false;
        }
        return true;
    }

    /**
     * Send off the deltas to various endpoints
     *
     * @param deltaJson JSONObject of the deltas to be published
     * @param projectId String of the project Id to post to
     * @param source    Source of the delta (e.g., MD, EVM, whatever, only necessary for MD so it can
     *                  ignore)
     * @return true if publish completed
     * @throws JSONException
     */
    public static boolean sendDeltas(SerialJSONObject deltaJson, String projectId, String workspaceId, String source,
        ServiceRegistry services, boolean withChildViews, boolean isArtifact) {

        JSONObject jmsPayload = new JSONObject();
        try {
            eh = new ElasticHelper();
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        if (isArtifact) {
            if (!processArtifactDeltasForDb(deltaJson, projectId, workspaceId, jmsPayload)) {
                return false;
            }
        } else {
            if (!processDeltasForDb(deltaJson, projectId, workspaceId, jmsPayload, withChildViews, services)) {
                return false;
            }
        }

        if (source != null) {
            jmsPayload.put("source", source);
        }

        sendJmsMsg(jmsPayload, TYPE_DELTA, workspaceId, projectId);

        return true;
    }

    public static void sendOrganizationDelta(String orgId, String orgName, String user) throws PSQLException {
        PostgresHelper pgh = new PostgresHelper();
        pgh.createOrganization(orgId, orgName);
    }

    public static void sendProjectDelta(JSONObject o, String orgId, String user) {

        PostgresHelper pgh = new PostgresHelper();
        String date = TimeUtils.toTimestamp(new Date().getTime());
        JSONObject jmsMsg = new JSONObject();
        JSONObject siteElement = new JSONObject();

        JSONObject projectHoldingBin;
        JSONObject viewInstanceBin;
        JSONObject project;
        ElasticResult eProject;
        ElasticResult eProjectHoldingBin;
        ElasticResult eViewInstanceBin;

        String projectSysmlid;
        String projectName;

        String projectLocation = o.optString("location");

        projectName = o.getString("name");
        projectSysmlid = o.getString(Sjm.SYSMLID);


        pgh.createProjectDatabase(projectSysmlid, orgId, projectName, projectLocation);

        siteElement.put(Sjm.SYSMLID, orgId);

        project = createNode(projectSysmlid, user, date, o);

        projectHoldingBin = createNode(HOLDING_BIN_PREFIX + projectSysmlid, user, date, null);
        projectHoldingBin.put(Sjm.NAME, "Holding Bin");
        projectHoldingBin.put(Sjm.OWNERID, projectSysmlid);
        projectHoldingBin.put(Sjm.TYPE, "Package");
        projectHoldingBin.put(Sjm.URI, JSONObject.NULL);
        projectHoldingBin.put(Sjm.APPLIEDSTEREOTYPEIDS, new JSONArray());
        projectHoldingBin.put(Sjm.ISSITE, false);
        projectHoldingBin.put(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JSONObject.NULL);
        projectHoldingBin.put(Sjm.CLIENTDEPENDENCYIDS, new JSONArray());
        projectHoldingBin.put(Sjm.DOCUMENTATION, "");
        projectHoldingBin.put(Sjm.ELEMENTIMPORTIDS, new JSONArray());
        projectHoldingBin.put(Sjm.MDEXTENSIONSIDS, new JSONArray());
        projectHoldingBin.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
        projectHoldingBin.put(Sjm.PACKAGEIMPORTIDS, new JSONArray());
        projectHoldingBin.put(Sjm.PACKAGEMERGEIDS, new JSONArray());
        projectHoldingBin.put(Sjm.PROFILEAPPLICATIONIDS, new JSONArray());
        projectHoldingBin.put(Sjm.SUPPLIERDEPENDENCYIDS, new JSONArray());
        projectHoldingBin.put(Sjm.SYNCELEMENTID, JSONObject.NULL);
        projectHoldingBin.put(Sjm.TEMPLATEBINDINGIDS, new JSONArray());
        projectHoldingBin.put(Sjm.TEMPLATEPARAMETERID, JSONObject.NULL);
        projectHoldingBin.put(Sjm.VISIBILITY, "public");

        viewInstanceBin = createNode("view_instances_bin_" + projectSysmlid, user, date, null);
        viewInstanceBin.put(Sjm.NAME, "View Instances Bin");
        viewInstanceBin.put(Sjm.OWNERID, projectSysmlid);
        viewInstanceBin.put(Sjm.TYPE, "Package");
        viewInstanceBin.put(Sjm.URI, JSONObject.NULL);
        viewInstanceBin.put(Sjm.APPLIEDSTEREOTYPEIDS, new JSONArray());
        viewInstanceBin.put(Sjm.ISSITE, false);
        viewInstanceBin.put(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JSONObject.NULL);
        viewInstanceBin.put(Sjm.CLIENTDEPENDENCYIDS, new JSONArray());
        viewInstanceBin.put(Sjm.DOCUMENTATION, "");
        viewInstanceBin.put(Sjm.ELEMENTIMPORTIDS, new JSONArray());
        viewInstanceBin.put(Sjm.MDEXTENSIONSIDS, new JSONArray());
        viewInstanceBin.put(Sjm.NAMEEXPRESSION, JSONObject.NULL);
        viewInstanceBin.put(Sjm.PACKAGEIMPORTIDS, new JSONArray());
        viewInstanceBin.put(Sjm.PACKAGEMERGEIDS, new JSONArray());
        viewInstanceBin.put(Sjm.PROFILEAPPLICATIONIDS, new JSONArray());
        viewInstanceBin.put(Sjm.SUPPLIERDEPENDENCYIDS, new JSONArray());
        viewInstanceBin.put(Sjm.SYNCELEMENTID, JSONObject.NULL);
        viewInstanceBin.put(Sjm.TEMPLATEBINDINGIDS, new JSONArray());
        viewInstanceBin.put(Sjm.TEMPLATEPARAMETERID, JSONObject.NULL);
        viewInstanceBin.put(Sjm.VISIBILITY, "public");

        try {
            ElasticHelper eh = new ElasticHelper();
            eh.createIndex(projectSysmlid);
            eProject = eh.indexElement(project, projectSysmlid);
            eh.refreshIndex();

            // only insert if the project does not exist already
            if (pgh.getNodeFromSysmlId(projectSysmlid) == null) {
                eProjectHoldingBin = eh.indexElement(projectHoldingBin, projectSysmlid);
                eViewInstanceBin = eh.indexElement(viewInstanceBin, projectSysmlid);
                eh.refreshIndex();

                pgh.insertNode(eProject.elasticId, eProject.sysmlid, DbNodeTypes.PROJECT);
                pgh.insertNode(eProjectHoldingBin.elasticId, HOLDING_BIN_PREFIX + projectSysmlid,
                    DbNodeTypes.HOLDINGBIN);
                pgh.insertNode(eViewInstanceBin.elasticId, "view_instances_bin_" + projectSysmlid,
                    DbNodeTypes.HOLDINGBIN);

                pgh.insertEdge(projectSysmlid, eProjectHoldingBin.sysmlid, DbEdgeTypes.CONTAINMENT);
                pgh.insertEdge(projectSysmlid, eViewInstanceBin.sysmlid, DbEdgeTypes.CONTAINMENT);

                JSONObject addedElements = new JSONObject();
                JSONArray elementsArray = new JSONArray();
                elementsArray.put(projectHoldingBin);
                elementsArray.put(viewInstanceBin);
                addedElements.put("addedElements", elementsArray);
                jmsMsg.put("refs", addedElements);
                jmsMsg.put("source", "mms");
                sendJmsMsg(jmsMsg, TYPE_DELTA, null, projectSysmlid);
            } else {
                Map<String, String> projectElastic = new HashMap<>();
                projectElastic.put("elasticid", eProject.elasticId);
                pgh.updateNode(projectSysmlid, projectElastic);
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }
    }

    // make sure only one branch is made at a time
    public static synchronized JSONObject sendBranch(String projectId, SerialJSONObject src, SerialJSONObject created,
        String elasticId, Boolean isTag, String source, ServiceRegistry services) {
        return sendBranch(projectId, src, created, elasticId, isTag, source, null, services);
    }

    // make sure only one branch is made at a time
    public static synchronized JSONObject sendBranch(String projectId, SerialJSONObject src, SerialJSONObject created,
        String elasticId, Boolean isTag, String source, String commitId, ServiceRegistry services) {
        // FIXME: need to include branch in commit history
        SerialJSONObject branchJson = new SerialJSONObject();

        branchJson.put("source", source);
        logger.info("SrcJSON in sendBranch: " + src.toString());
        String srcId = src.getString(Sjm.SYSMLID);
        String createdId = created.getString(Sjm.SYSMLID);

        NodeRef person = services.getPersonService().getPersonOrNull(created.optString(Sjm.CREATOR));
        if (person != null) {
            user = services.getNodeService().getProperty(person, ContentModel.PROP_EMAIL).toString();
        }

        initHazelcastQueue(String.format("%s-%s", projectId, createdId));
        BlockingQueue<BranchTask> queue = hzInstance.getQueue(String.format("%s-%s", projectId, createdId));
        BranchTask task = new BranchTask(
            projectId, srcId, created.toString(), elasticId, isTag, source, commitId, user
        );

        try {
            queue.put(task);
        } catch (InterruptedException ie) {
            logger.debug(String.format("Interrupted: %s", LogUtil.getStackTrace(ie)));
            Thread.currentThread().interrupt();
        }

        return branchJson;
    }

    public static boolean sendJmsMsg(JSONObject json, String eventType, String refId, String projectId) {
        boolean status = false;
        if (jmsConnection != null) {
            status = jmsConnection.publish(json, eventType, refId, projectId);
            if (logger.isDebugEnabled()) {
                String msg = "Event: " + eventType + ", RefId: " + refId + ", ProjectId: " + projectId + "\n";
                msg += "JSONObject: " + json;
                logger.debug(msg);
            }
        } else {
            if (logger.isInfoEnabled())
                logger.info("JMS Connection not available");
        }

        return status;
    }

    private static JSONObject createNode(String sysmlid, String user, String date, JSONObject e) {

        if (e == null) {
            e = new JSONObject();
        }

        e.put(Sjm.SYSMLID, sysmlid);
        e.put(Sjm.CREATOR, user);
        e.put(Sjm.CREATED, date);
        e.put(Sjm.MODIFIER, user);
        e.put(Sjm.MODIFIED, date);

        return e;
    }

    public static boolean createOrUpdateSiteChar(JSONObject siteChar, String projectId, String refId,
        ServiceRegistry services) {

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        String folderName = siteChar.optString(Sjm.NAME);
        String folderId = siteChar.optString(Sjm.SYSMLID);

        if (services != null && folderName != null && folderId != null) {
            String orgId = emsNodeUtil.getOrganizationFromProject(projectId);
            SiteInfo orgInfo = services.getSiteService().getSite(orgId);
            if (orgInfo != null) {

                EmsScriptNode site = new EmsScriptNode(orgInfo.getNodeRef(), services);

                if (!site.checkPermissions(PermissionService.WRITE)) {
                    return false;
                }

                EmsScriptNode documentLibrary = site.childByNamePath("documentLibrary");
                if (documentLibrary == null) {
                    documentLibrary = site.createFolder("documentLibrary", null, null);
                    documentLibrary.createOrUpdateProperty(Acm.CM_TITLE, "Document Library");
                }
                EmsScriptNode projectDocumentLibrary = documentLibrary.childByNamePath(projectId);
                if (projectDocumentLibrary == null) {
                    projectDocumentLibrary = documentLibrary.createFolder(projectId, null, null);
                }
                EmsScriptNode siteCharFolder = projectDocumentLibrary.childByNamePath(folderId);
                if (siteCharFolder == null) {
                    siteCharFolder = projectDocumentLibrary.createFolder(folderId, null, null);
                    siteCharFolder.createOrUpdateProperty(Acm.CM_TITLE, folderName);
                    return true;
                } else {
                    siteCharFolder.createOrUpdateProperty(Acm.CM_TITLE, folderName);
                    return true;
                }
            }
        }

        return false;
    }

    private static Pattern pattern = Pattern.compile("<mms-cf.*?mms-element-id=\"([a-zA-Z0-9_\\-]+)\"");

    public static void processValueEdges(SerialJSONObject element, List<Pair<String, String>> edges) {
        SerialJSONObject defaultValue = element.optJSONObject(Sjm.DEFAULTVALUE);
        SerialJSONArray slotValues = element.optJSONArray("value");
        if (defaultValue != null && defaultValue.optString(Sjm.TYPE).equals("LiteralString")) {
            processDocumentEdges(element.getString(Sjm.SYSMLID), defaultValue.optString("value"), edges);
        }
        if (slotValues != null) {
            for (int i = 0; i < slotValues.length(); i++) {
                SerialJSONObject val = slotValues.optJSONObject(i);
                if (val != null && val.optString(Sjm.TYPE).equals("LiteralString")) {
                    processDocumentEdges(element.getString(Sjm.SYSMLID), val.optString("value"), edges);
                }
            }
        }
    }

    public static void processDocumentEdges(String sysmlid, String doc, List<Pair<String, String>> documentEdges) {
        if (doc != null) {
            Matcher matcher = pattern.matcher(doc);

            while (matcher.find()) {
                String mmseid = matcher.group(1);
                if (mmseid != null) {
                    documentEdges.add(new Pair<>(sysmlid, mmseid));
                }
            }
        }
    }

    public static void processContentsJson(String sysmlId, SerialJSONObject contents,
        List<Pair<String, String>> documentEdges) {
        if (contents != null) {
            if (contents.has("operand")) {
                SerialJSONArray operand = contents.getJSONArray("operand");
                for (int ii = 0; ii < operand.length(); ii++) {
                    SerialJSONObject value = operand.optJSONObject(ii);
                    if (value != null && value.has("instanceId")) {
                        documentEdges.add(new Pair<>(sysmlId, value.getString("instanceId")));
                    }
                }
            }
        }
    }

    public static void processInstanceSpecificationSpecificationJson(String sysmlId, SerialJSONObject iss,
        List<Pair<String, String>> documentEdges) {
        if (iss != null) {
            if (iss.has("value") && iss.has("type") && iss.getString("type").equals("LiteralString")) {
                String string = iss.getString("value");
                try {
                    SerialJSONObject json = new SerialJSONObject(string);
                    StringBuilder text = new StringBuilder();
                    Set<Object> sources = findKeyValueInJsonObject(json, "source", text);
                    for (Object source : sources) {
                        if (source instanceof String) {
                            documentEdges.add(new Pair<>(sysmlId, (String) source));
                        }
                    }
                    Matcher matcher = pattern.matcher(text);
                    while (matcher.find()) {
                        String mmseid = matcher.group(1);
                        if (mmseid != null) {
                            documentEdges.add(new Pair<>(sysmlId, mmseid));
                        }
                    }
                } catch (JSONException ex) {
                    //case if value string isn't actually a serialized jsonobject
                }
            }
        }
    }

    public static Set<Object> findKeyValueInJsonObject(SerialJSONObject json, String keyMatch, StringBuilder text) {
        Set<Object> result = new HashSet<>();
        Iterator<?> keys = json.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value = json.get(key);
            if (key.equals("text")) {
                text.append(value);
            }
            if (key.equals(keyMatch)) {
                result.add(value);
            } else if (value instanceof JSONObject) {
                result.addAll(findKeyValueInJsonObject((SerialJSONObject) value, keyMatch, text));
            } else if (value instanceof JSONArray) {
                result.addAll(findKeyValueInJsonArray((SerialJSONArray) value, keyMatch, text));
            }
        }
        return result;
    }

    public static Set<Object> findKeyValueInJsonArray(SerialJSONArray jsonArray, String keyMatch, StringBuilder text) {
        Set<Object> result = new HashSet<>();

        for (int ii = 0; ii < jsonArray.length(); ii++) {
            if (jsonArray.get(ii) instanceof JSONObject) {
                result.addAll(findKeyValueInJsonObject((SerialJSONObject) jsonArray.get(ii), keyMatch, text));
            } else if (jsonArray.get(ii) instanceof JSONArray) {
                result.addAll(findKeyValueInJsonArray((SerialJSONArray) jsonArray.get(ii), keyMatch, text));
            }
        }

        return result;
    }
}
