package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.view_repo.db.*;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import org.postgresql.util.PSQLException;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.connections.JmsConnection;
import gov.nasa.jpl.view_repo.db.DocStoreInterface;
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

    private static final String HOLDING_BIN_PREFIX = "holding_bin_";

    private static DocStoreInterface docStoreHelper = null;
    private static JmsConnection jmsConnection = null;

    private static String user = null;

    private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

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
            config.getNetworkConfig().setPort(5901).setPortAutoIncrement(true);
            hzInstance = Hazelcast.newHazelcastInstance(config);
        }
    }

    private static void initHazelcastQueue(String name) {
        Config config = hzInstance.getConfig();
        QueueConfig queueConfig = config.getQueueConfig(name);
        if (queueConfig.getItemListenerConfigs().isEmpty()) {
            logger.info("Creating new queue named: " + name);
            queueConfig
                .addItemListenerConfig(new ItemListenerConfig("gov.nasa.jpl.view_repo.util.QueueConsumer", true));
            config.addQueueConfig(queueConfig);
        }
    }

    public static DbNodeTypes getNodeType(JsonObject e) {

        String type = e.has(Sjm.TYPE) ? e.get(Sjm.TYPE).getAsString().toLowerCase() : "";

        if ((type.contains("class") || type.contains("generalization") || type.contains("dependency")) && e
            .has(Sjm.APPLIEDSTEREOTYPEIDS)) {
            JsonArray typeArray = JsonUtil.getOptArray(e, Sjm.APPLIEDSTEREOTYPEIDS);

            for (int i = 0; i < typeArray.size(); i++) {
                String typeString = typeArray.get(i).getAsString();
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
                if (isGroup(e)) {
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

    public static boolean isGroup(JsonObject element) {
        return element.has(Sjm.ISGROUP) && element.get(Sjm.ISGROUP).getAsBoolean();
    }

    private static boolean bulkElasticEntry(JsonArray elements, String operation, boolean refresh, String index,
        String type) {
        if (elements.size() > 0) {
            try {
                boolean bulkEntry = docStoreHelper.bulkIndexElements(elements, operation, refresh, index, type);
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

    public static boolean isPartProperty(JsonObject e) {
        if (!e.has(Sjm.TYPE) || !e.get(Sjm.TYPE).getAsString().equals("Property")) {
            return false;
        }
        if (!e.has(Sjm.AGGREGATION) || e.get(Sjm.AGGREGATION).getAsString().equals("none")) {
            return false;
        }
        if (e.has(Sjm.DEFAULTVALUE) && !e.get(Sjm.DEFAULTVALUE).isJsonNull()) {
            return false;
        }
        JsonArray appliedS = JsonUtil.getOptArray(e, Sjm.APPLIEDSTEREOTYPEIDS);
        for (int i = 0; i < appliedS.size(); i++) {
            String s = appliedS.get(i).getAsString();
            if (s.equals(Sjm.VALUEPROPERTY) || s.equals(Sjm.CONSTRAINTPROPERTY)) {
                return false;
            }
        }
        return true;
    }

    private static boolean processArtifactDeltasForDb(JsonObject delta, String projectId, String refId,
        JsonObject jmsPayload) {
        PostgresHelper pgh = new PostgresHelper();
        pgh.setProject(projectId);
        pgh.setWorkspace(refId);

        JsonArray added = JsonUtil.getOptArray(delta, "addedElements");
        JsonArray updated = JsonUtil.getOptArray(delta, "updatedElements");
        JsonArray deleted = JsonUtil.getOptArray(delta, "deletedElements");

        String creator = delta.get("commit").getAsJsonObject().get(Sjm.CREATOR).getAsString();
        String created = delta.get("commit").getAsJsonObject().get(Sjm.CREATED).getAsString();
        String commitElasticId = delta.get("commit").getAsJsonObject().get(Sjm.ELASTICID).getAsString();

        JsonObject jmsWorkspace = new JsonObject();
        JsonArray jmsAdded = new JsonArray();
        JsonArray jmsUpdated = new JsonArray();
        JsonArray jmsDeleted = new JsonArray();

        List<String> deletedSysmlIds = new ArrayList<>();
        if (bulkElasticEntry(added, "added", true, projectId, "artifact") && bulkElasticEntry(updated, "updated", true,
            projectId, "artifact")) {
            try {
                List<Map<String, Object>> artifactInserts = new ArrayList<>();
                List<Map<String, Object>> artifactUpdates = new ArrayList<>();

                for (int i = 0; i < added.size(); i++) {
                    JsonObject e = added.get(i).getAsJsonObject();
                    Map<String, Object> artifact = new HashMap<>();
                    jmsAdded.add(e.get(Sjm.SYSMLID).getAsString());

                    if (e.has(Sjm.ELASTICID)) {
                        artifact.put(Sjm.ELASTICID, e.get(Sjm.ELASTICID).getAsString());
                        artifact.put(Sjm.SYSMLID, e.get(Sjm.SYSMLID).getAsString());
                        artifact.put(INITIALCOMMIT, e.get(Sjm.ELASTICID).getAsString());
                        artifact.put(LASTCOMMIT, commitElasticId);
                        artifactInserts.add(artifact);
                    }
                }

                for (int i = 0; i < deleted.size(); i++) {
                    JsonObject e = deleted.get(i).getAsJsonObject();
                    jmsDeleted.add(e.get(Sjm.SYSMLID).getAsString());
                    pgh.deleteArtifact(e.get(Sjm.SYSMLID).getAsString());
                    deletedSysmlIds.add(e.get(Sjm.SYSMLID).getAsString());
                }

                for (int i = 0; i < updated.size(); i++) {
                    JsonObject e = updated.get(i).getAsJsonObject();
                    jmsUpdated.add(e.get(Sjm.SYSMLID).getAsString());

                    if (e.has(Sjm.ELASTICID)) {
                        Map<String, Object> updatedArtifact = new HashMap<>();
                        updatedArtifact.put(Sjm.ELASTICID, e.get(Sjm.ELASTICID).getAsString());
                        updatedArtifact.put(Sjm.SYSMLID, e.get(Sjm.SYSMLID).getAsString());
                        updatedArtifact.put(DELETED, false);
                        updatedArtifact.put(LASTCOMMIT, commitElasticId);
                        artifactUpdates.add(updatedArtifact);
                    }
                }

                Savepoint sp = null;
                try {//do node insert, updates, and containment edge updates
                    //do bulk delete edges for affected sysmlids here - delete containment, view and childview
                    sp = pgh.startTransaction();
                    pgh.runBatchQueries(artifactInserts, ARTIFACTS);
                    pgh.runBatchQueries(artifactUpdates, "artifactUpdates");
                    pgh.updateLastCommitsArtifacts(commitElasticId, deletedSysmlIds);
                    pgh.commitTransaction();
                    pgh.insertCommit(commitElasticId, DbCommitTypes.COMMIT, creator, new Timestamp(df.parse(created).getTime()));
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
                	docStoreHelper.indexElement(delta.get("commit").getAsJsonObject(), projectId,
                			DocStoreInterface.COMMIT); //initial commit may fail to read back but does get indexed
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

        jmsWorkspace.add("addedElements", jmsAdded);
        jmsWorkspace.add("updatedElements", jmsUpdated);
        jmsWorkspace.add("deletedElements", jmsDeleted);

        jmsPayload.add("refs", jmsWorkspace);

        return true;

    }

    private static boolean processDeltasForDb(JsonObject delta, String projectId, String refId, JsonObject jmsPayload,
        ServiceRegistry services) {
        // :TODO write to elastic for elements, write to postgres, write to elastic for commits
        // :TODO should return a 500 here to stop writes if one insert fails
        PostgresHelper pgh = new PostgresHelper();
        pgh.setProject(projectId);
        pgh.setWorkspace(refId);

        JsonArray added = JsonUtil.getOptArray(delta, "addedElements");
        JsonArray updated = JsonUtil.getOptArray(delta, "updatedElements");
        JsonArray deleted = JsonUtil.getOptArray(delta, "deletedElements");

        String creator = delta.get("commit").getAsJsonObject().get(Sjm.CREATOR).getAsString();
        String created = delta.get("commit").getAsJsonObject().get(Sjm.CREATED).getAsString();
        String commitElasticId = delta.get("commit").getAsJsonObject().get(Sjm.ELASTICID).getAsString();

        JsonObject jmsWorkspace = new JsonObject();
        JsonArray jmsAdded = new JsonArray();
        JsonArray jmsUpdated = new JsonArray();
        JsonArray jmsDeleted = new JsonArray();

        List<String> deletedSysmlIds = new ArrayList<>();
        List<Pair<String, String>> addEdges = new ArrayList<>();
        List<Pair<String, String>> viewEdges = new ArrayList<>();
        List<Pair<String, String>> childViewEdges = new ArrayList<>();

        if (bulkElasticEntry(added, "added", true, projectId, "element") && bulkElasticEntry(updated, "updated", true,
            projectId, "element")) {

            try {
                List<Map<String, Object>> nodeInserts = new ArrayList<>();
                List<Map<String, Object>> edgeInserts = new ArrayList<>();
                List<Map<String, Object>> childEdgeInserts = new ArrayList<>();
                List<Map<String, Object>> nodeUpdates = new ArrayList<>();
                Set<String> uniqueEdge = new HashSet<>();

                for (int i = 0; i < added.size(); i++) {
                    JsonObject e = added.get(i).getAsJsonObject();
                    Map<String, Object> node = new HashMap<>();
                    jmsAdded.add(e.get(Sjm.SYSMLID).getAsString());
                    int nodeType = getNodeType(e).getValue();

                    if (e.has(Sjm.ELASTICID)) {
                        node.put(Sjm.ELASTICID, e.get(Sjm.ELASTICID).getAsString());
                        node.put(Sjm.SYSMLID, e.get(Sjm.SYSMLID).getAsString());
                        node.put(NODETYPE, nodeType);
                        node.put(INITIALCOMMIT, e.get(Sjm.ELASTICID).getAsString());
                        node.put(LASTCOMMIT, commitElasticId);
                        nodeInserts.add(node);
                    }

                    if (e.has(Sjm.OWNERID) && !e.get(Sjm.OWNERID).isJsonNull() && e.has(Sjm.SYSMLID) && !e
                        .get(Sjm.SYSMLID).isJsonNull()) {
                        Pair<String, String> p =
                            new Pair<>(e.get(Sjm.OWNERID).getAsString(), e.get(Sjm.SYSMLID).getAsString());
                        addEdges.add(p);
                    }

                    String doc = JsonUtil.getOptString(e, Sjm.DOCUMENTATION);
                    processDocumentEdges(e.get(Sjm.SYSMLID).getAsString(), doc, viewEdges);

                    if (nodeType == DbNodeTypes.SITEANDPACKAGE.getValue()) {
                        createOrUpdateSiteChar(e, projectId, refId, services);
                    }
                    String type = JsonUtil.getOptString(e, Sjm.TYPE);
                    if (type.equals("Slot") || type.equals("Property") || type.equals("Port")) {
                        processValueEdges(e, viewEdges);
                    }
                    if (e.has(Sjm.CONTENTS)) {
                        JsonObject contents = JsonUtil.getOptObject(e, Sjm.CONTENTS);
                        processContentsJson(e.get(Sjm.SYSMLID).getAsString(), contents, viewEdges);
                    } else if (e.has(Sjm.SPECIFICATION) && nodeType == DbNodeTypes.INSTANCESPECIFICATION.getValue()) {
                        JsonObject iss = JsonUtil.getOptObject(e, Sjm.SPECIFICATION);
                        processInstanceSpecificationSpecificationJson(e.get(Sjm.SYSMLID).getAsString(), iss, viewEdges);
                        processContentsJson(e.get(Sjm.SYSMLID).getAsString(), iss, viewEdges);
                    }
                    if (nodeType == DbNodeTypes.VIEW.getValue() || nodeType == DbNodeTypes.DOCUMENT.getValue()) {
                        JsonArray owned = JsonUtil.getOptArray(e, Sjm.OWNEDATTRIBUTEIDS);
                        for (int j = 0; j < owned.size(); j++) {
                            Pair<String, String> p =
                                new Pair<>(e.get(Sjm.SYSMLID).getAsString(), owned.get(j).getAsString());
                            childViewEdges.add(p);
                        }
                    }
                    if (isPartProperty(e)) {
                        String typeId = JsonUtil.getOptString(e, Sjm.TYPEID);
                        if (!typeId.isEmpty()) {
                            Pair<String, String> p = new Pair<>(e.get(Sjm.SYSMLID).getAsString(), typeId);
                            childViewEdges.add(p);
                        }
                    }
                }

                for (int i = 0; i < deleted.size(); i++) {
                    JsonObject e = deleted.get(i).getAsJsonObject();
                    jmsDeleted.add(e.get(Sjm.SYSMLID).getAsString());
                    pgh.deleteEdgesForNode(e.get(Sjm.SYSMLID).getAsString());
                    pgh.deleteNode(e.get(Sjm.SYSMLID).getAsString());
                    deletedSysmlIds.add(e.get(Sjm.SYSMLID).getAsString());
                }

                for (int i = 0; i < updated.size(); i++) {
                    JsonObject e = updated.get(i).getAsJsonObject();
                    jmsUpdated.add(e.get(Sjm.SYSMLID).getAsString());
                    int nodeType = getNodeType(e).getValue();
                    pgh.deleteEdgesForNode(e.get(Sjm.SYSMLID).getAsString(), true, DbEdgeTypes.CONTAINMENT);
                    pgh.deleteEdgesForNode(e.get(Sjm.SYSMLID).getAsString(), false, DbEdgeTypes.VIEW);
                    pgh.deleteEdgesForNode(e.get(Sjm.SYSMLID).getAsString(), false, DbEdgeTypes.CHILDVIEW);

                    if (e.get(Sjm.SYSMLID).getAsString().equalsIgnoreCase(projectId)) {
                        // Remove owner from project element
                        e.remove(Sjm.OWNERID);
                    }

                    if (e.has(Sjm.OWNERID) && !e.get(Sjm.OWNERID).isJsonNull() && !e.get(Sjm.SYSMLID).isJsonNull()) {
                        Pair<String, String> p =
                            new Pair<>(e.get(Sjm.OWNERID).getAsString(), e.get(Sjm.SYSMLID).getAsString());
                        addEdges.add(p);
                    }
                    String doc = JsonUtil.getOptString(e, Sjm.DOCUMENTATION);
                    processDocumentEdges(e.get(Sjm.SYSMLID).getAsString(), doc, viewEdges);

                    if (nodeType == DbNodeTypes.SITEANDPACKAGE.getValue()) {
                        createOrUpdateSiteChar(e, projectId, refId, services);
                    }
                    String type = JsonUtil.getOptString(e, Sjm.TYPE);
                    if (type.equals("Slot") || type.equals("Property") || type.equals("Port")) {
                        processValueEdges(e, viewEdges);
                    }
                    if (e.has(Sjm.CONTENTS)) {
                        JsonObject contents = JsonUtil.getOptObject(e, Sjm.CONTENTS);
                        processContentsJson(e.get(Sjm.SYSMLID).getAsString(), contents, viewEdges);
                    } else if (e.has(Sjm.SPECIFICATION) && nodeType == DbNodeTypes.INSTANCESPECIFICATION.getValue()) {
                        JsonObject iss = JsonUtil.getOptObject(e, Sjm.SPECIFICATION);
                        processInstanceSpecificationSpecificationJson(e.get(Sjm.SYSMLID).getAsString(), iss, viewEdges);
                        processContentsJson(e.get(Sjm.SYSMLID).getAsString(), iss, viewEdges);
                    }
                    if (nodeType == DbNodeTypes.VIEW.getValue() || nodeType == DbNodeTypes.DOCUMENT.getValue()) {
                        JsonArray owned = JsonUtil.getOptArray(e, Sjm.OWNEDATTRIBUTEIDS);
                        for (int j = 0; j < owned.size(); j++) {
                            Pair<String, String> p =
                                new Pair<>(e.get(Sjm.SYSMLID).getAsString(), owned.get(j).getAsString());
                            childViewEdges.add(p);
                        }
                    }
                    if (isPartProperty(e)) {
                        String typeId = JsonUtil.getOptString(e, Sjm.TYPEID);
                        if (!typeId.isEmpty()) {
                            Pair<String, String> p = new Pair<>(e.get(Sjm.SYSMLID).getAsString(), typeId);
                            childViewEdges.add(p);
                        }
                    }
                    if (e.has(Sjm.ELASTICID)) {
                        Map<String, Object> updatedNode = new HashMap<>();
                        updatedNode.put(Sjm.ELASTICID, e.get(Sjm.ELASTICID).getAsString());
                        updatedNode.put(Sjm.SYSMLID, e.get(Sjm.SYSMLID).getAsString());
                        updatedNode.put(NODETYPE, getNodeType(e).getValue());
                        updatedNode.put(DELETED, false);
                        updatedNode.put(LASTCOMMIT, commitElasticId);
                        nodeUpdates.add(updatedNode);
                    }
                }

                for (Pair<String, String> e : addEdges) {
                    if (!pgh.edgeExists(e.first, e.second, DbEdgeTypes.CONTAINMENT)) {
                        String edgeTest = e.first + e.second + DbEdgeTypes.CONTAINMENT.getValue();
                        if (!uniqueEdge.contains(edgeTest)) {
                            Map<String, Object> edge = new HashMap<>();
                            edge.put(PARENT, e.first);
                            edge.put(CHILD, e.second);
                            edge.put(EDGETYPE, DbEdgeTypes.CONTAINMENT.getValue());
                            edgeInserts.add(edge);
                            uniqueEdge.add(edgeTest);
                        }
                    }
                }

                for (Pair<String, String> e : viewEdges) {
                    if (!pgh.edgeExists(e.first, e.second, DbEdgeTypes.VIEW)) {
                        String edgeTest = e.first + e.second + DbEdgeTypes.VIEW.getValue();
                        if (!uniqueEdge.contains(edgeTest)) {
                            Map<String, Object> edge = new HashMap<>();
                            edge.put(PARENT, e.first);
                            edge.put(CHILD, e.second);
                            edge.put(EDGETYPE, DbEdgeTypes.VIEW.getValue());
                            childEdgeInserts.add(edge);
                            uniqueEdge.add(edgeTest);
                        }
                    }
                }
                for (Pair<String, String> e : childViewEdges) {
                    if (!pgh.edgeExists(e.first, e.second, DbEdgeTypes.CHILDVIEW)) {
                        String edgeTest = e.first + e.second + DbEdgeTypes.CHILDVIEW.getValue();
                        if (!uniqueEdge.contains(edgeTest)) {
                            Map<String, Object> edge = new HashMap<>();
                            edge.put(PARENT, e.first);
                            edge.put(CHILD, e.second);
                            edge.put(EDGETYPE, DbEdgeTypes.CHILDVIEW.getValue());
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
                    pgh.runBatchQueries(nodeInserts, NODES);
                    pgh.runBatchQueries(nodeUpdates, "updates");
                    //pgh.updateBySysmlIds(NODES, LASTCOMMIT, commitElasticId, deletedSysmlIds);
                    pgh.updateLastCommitsNodes(commitElasticId, deletedSysmlIds);
                    pgh.commitTransaction();
                    pgh.insertCommit(commitElasticId, DbCommitTypes.COMMIT, creator, new Timestamp(df.parse(created).getTime()));
                    sp = pgh.startTransaction();
                    pgh.runBatchQueries(edgeInserts, EDGES);
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
                    pgh.runBatchQueries(childEdgeInserts, EDGES);
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
                	docStoreHelper.indexElement(delta.get("commit").getAsJsonObject(), projectId,
                			DocStoreInterface.COMMIT); //initial commit may fail to read back but does get indexed
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

        jmsWorkspace.add("addedElements", jmsAdded);
        jmsWorkspace.add("updatedElements", jmsUpdated);
        jmsWorkspace.add("deletedElements", jmsDeleted);

        jmsPayload.add("refs", jmsWorkspace);

        if (!commitElasticId.isEmpty()) {
            jmsPayload.addProperty(Sjm.COMMITID, commitElasticId);
        }

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
        	DocStoreInterface docStoreHelper = DocStoreFactory.getDocStore();

        	Set<String> updateSet = new HashSet<>(updateParents);
            String owner = HOLDING_BIN_PREFIX + projectId;
            JsonObject query = new JsonObject();
            JsonObject doc = new JsonObject();
            doc.addProperty(Sjm.OWNERID, owner);
            query.add("doc", doc);
            docStoreHelper.bulkUpdateElements(updateSet, query.toString(), projectId, "element");
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            return false;
        }
        return true;
    }

    /**
     * Send off the deltas to various endpoints
     *
     * @param deltaJson JsonObject of the deltas to be published
     * @param projectId String of the project Id to post to
     * @param source    Source of the delta (e.g., MD, EVM, whatever, only necessary for MD so it can
     *                  ignore)
     * @return true if publish completed
     */
    public static boolean sendDeltas(JsonObject deltaJson, String projectId, String workspaceId, String source,
        ServiceRegistry services, boolean withChildViews, boolean isArtifact) {

        JsonObject jmsPayload = new JsonObject();
        try {
        	docStoreHelper = DocStoreFactory.getDocStore();
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        if (isArtifact) {
            if (!processArtifactDeltasForDb(deltaJson, projectId, workspaceId, jmsPayload)) {
                return false;
            }
        } else {
            if (!processDeltasForDb(deltaJson, projectId, workspaceId, jmsPayload, services)) {
                return false;
            }
        }

        if (source != null) {
            jmsPayload.addProperty("source", source);
        }

        sendJmsMsg(jmsPayload, TYPE_DELTA, workspaceId, projectId);

        return true;
    }

    public static JsonObject sendOrganizationDelta(String orgId, String orgName, JsonObject orgJson)
        throws PSQLException {
        PostgresHelper pgh = new PostgresHelper();

        String defaultIndex = EmsConfig.get("elastic.index.element");

        try {
        	DocStoreInterface docStoreHelper = DocStoreFactory.getDocStore();

            if (!pgh.orgExists(orgId)) {
                pgh.createOrganization(orgId, orgName);
                docStoreHelper.createIndex(defaultIndex);
                orgJson.addProperty(Sjm.ELASTICID, orgId);
                DocumentResult result = docStoreHelper.indexElement(orgJson, defaultIndex, DocStoreInterface.ELEMENT);
                return result.current;
            } else {
                pgh.updateOrganization(orgId, orgName);
                orgJson.addProperty(Sjm.ELASTICID, orgId);
                if (docStoreHelper.updateById(orgId, orgJson, defaultIndex, DocStoreInterface.ELEMENT).size() > 0 && docStoreHelper.refreshIndex()) {
                    return docStoreHelper.getByInternalId(orgId, defaultIndex, DocStoreInterface.ELEMENT);
                }
            }

        } catch (Exception e) {
            logger.error(e);
        }

        return null;
    }

    public static void sendProjectDelta(JsonObject o, String orgId, String user) {

        PostgresHelper pgh = new PostgresHelper();
        String date = TimeUtils.toTimestamp(new Date().getTime());
        JsonObject jmsMsg = new JsonObject();
        JsonObject siteElement = new JsonObject();

        JsonObject projectHoldingBin;
        JsonObject viewInstanceBin;
        JsonObject project;
        DocumentResult eProject;
        DocumentResult eProjectHoldingBin;
        DocumentResult eViewInstanceBin;

        String projectSysmlid;
        String projectName;

        String projectLocation = JsonUtil.getOptString(o, "location");

        projectName = o.get("name").getAsString();
        projectSysmlid = o.get(Sjm.SYSMLID).getAsString();


        pgh.createProjectDatabase(projectSysmlid, orgId, projectName, projectLocation);

        siteElement.addProperty(Sjm.SYSMLID, orgId);

        project = createNode(projectSysmlid, user, date, o);

        projectHoldingBin = createNode(HOLDING_BIN_PREFIX + projectSysmlid, user, date, null);
        projectHoldingBin.addProperty(Sjm.NAME, "Holding Bin");
        projectHoldingBin.addProperty(Sjm.OWNERID, projectSysmlid);
        projectHoldingBin.addProperty(Sjm.TYPE, "Package");
        projectHoldingBin.add(Sjm.URI, JsonNull.INSTANCE);
        projectHoldingBin.add(Sjm.APPLIEDSTEREOTYPEIDS, new JsonArray());
        projectHoldingBin.addProperty(Sjm.ISGROUP, false);
        projectHoldingBin.add(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JsonNull.INSTANCE);
        projectHoldingBin.add(Sjm.CLIENTDEPENDENCYIDS, new JsonArray());
        projectHoldingBin.addProperty(Sjm.DOCUMENTATION, "");
        projectHoldingBin.add(Sjm.ELEMENTIMPORTIDS, new JsonArray());
        projectHoldingBin.add(Sjm.MDEXTENSIONSIDS, new JsonArray());
        projectHoldingBin.add(Sjm.NAMEEXPRESSION, JsonNull.INSTANCE);
        projectHoldingBin.add(Sjm.PACKAGEIMPORTIDS, new JsonArray());
        projectHoldingBin.add(Sjm.PACKAGEMERGEIDS, new JsonArray());
        projectHoldingBin.add(Sjm.PROFILEAPPLICATIONIDS, new JsonArray());
        projectHoldingBin.add(Sjm.SUPPLIERDEPENDENCYIDS, new JsonArray());
        projectHoldingBin.add(Sjm.SYNCELEMENTID, JsonNull.INSTANCE);
        projectHoldingBin.add(Sjm.TEMPLATEBINDINGIDS, new JsonArray());
        projectHoldingBin.add(Sjm.TEMPLATEPARAMETERID, JsonNull.INSTANCE);
        projectHoldingBin.addProperty(Sjm.VISIBILITY, "public");

        viewInstanceBin = createNode("view_instances_bin_" + projectSysmlid, user, date, null);
        viewInstanceBin.addProperty(Sjm.NAME, "View Instances Bin");
        viewInstanceBin.addProperty(Sjm.OWNERID, projectSysmlid);
        viewInstanceBin.addProperty(Sjm.TYPE, "Package");
        viewInstanceBin.add(Sjm.URI, JsonNull.INSTANCE);
        viewInstanceBin.add(Sjm.APPLIEDSTEREOTYPEIDS, new JsonArray());
        viewInstanceBin.addProperty(Sjm.ISGROUP, false);
        viewInstanceBin.add(Sjm.APPLIEDSTEREOTYPEINSTANCEID, JsonNull.INSTANCE);
        viewInstanceBin.add(Sjm.CLIENTDEPENDENCYIDS, new JsonArray());
        viewInstanceBin.addProperty(Sjm.DOCUMENTATION, "");
        viewInstanceBin.add(Sjm.ELEMENTIMPORTIDS, new JsonArray());
        viewInstanceBin.add(Sjm.MDEXTENSIONSIDS, new JsonArray());
        viewInstanceBin.add(Sjm.NAMEEXPRESSION, JsonNull.INSTANCE);
        viewInstanceBin.add(Sjm.PACKAGEIMPORTIDS, new JsonArray());
        viewInstanceBin.add(Sjm.PACKAGEMERGEIDS, new JsonArray());
        viewInstanceBin.add(Sjm.PROFILEAPPLICATIONIDS, new JsonArray());
        viewInstanceBin.add(Sjm.SUPPLIERDEPENDENCYIDS, new JsonArray());
        viewInstanceBin.add(Sjm.SYNCELEMENTID, JsonNull.INSTANCE);
        viewInstanceBin.add(Sjm.TEMPLATEBINDINGIDS, new JsonArray());
        viewInstanceBin.add(Sjm.TEMPLATEPARAMETERID, JsonNull.INSTANCE);
        viewInstanceBin.addProperty(Sjm.VISIBILITY, "public");

        try {
        	DocStoreInterface docStoreHelper = DocStoreFactory.getDocStore();

        	Node projectNode = pgh.getNodeFromSysmlId(projectSysmlid);

            // only insert if the project does not exist already
            if (projectNode == null) {
            	docStoreHelper.createIndex(projectSysmlid);
                eProject = docStoreHelper.indexElement(project, projectSysmlid, DocStoreInterface.ELEMENT);
                docStoreHelper.refreshIndex();

                eProjectHoldingBin = docStoreHelper.indexElement(projectHoldingBin, projectSysmlid, DocStoreInterface.ELEMENT);
                eViewInstanceBin = docStoreHelper.indexElement(viewInstanceBin, projectSysmlid, DocStoreInterface.ELEMENT);
                docStoreHelper.refreshIndex();

                pgh.insertNode(eProject.internalId, eProject.sysmlid, DbNodeTypes.PROJECT);
                pgh.insertNode(eProjectHoldingBin.internalId, HOLDING_BIN_PREFIX + projectSysmlid,
                    DbNodeTypes.HOLDINGBIN);
                pgh.insertNode(eViewInstanceBin.internalId, "view_instances_bin_" + projectSysmlid,
                    DbNodeTypes.HOLDINGBIN);

                pgh.insertEdge(projectSysmlid, eProjectHoldingBin.sysmlid, DbEdgeTypes.CONTAINMENT);
                pgh.insertEdge(projectSysmlid, eViewInstanceBin.sysmlid, DbEdgeTypes.CONTAINMENT);

                JsonObject addedElements = new JsonObject();
                JsonArray elementsArray = new JsonArray();
                elementsArray.add(projectHoldingBin);
                elementsArray.add(viewInstanceBin);
                addedElements.add("addedElements", elementsArray);
                jmsMsg.add("refs", addedElements);
                jmsMsg.addProperty("source", "mms");
                sendJmsMsg(jmsMsg, TYPE_DELTA, null, projectSysmlid);
            } else {
                project.remove(Sjm.CREATED);
                project.remove(Sjm.CREATOR);
                docStoreHelper.updateById(projectNode.getElasticId(), project, projectSysmlid, DocStoreInterface.ELEMENT);
                docStoreHelper.refreshIndex();
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }
    }

    // make sure only one branch is made at a time
    public static synchronized JsonObject sendBranch(String projectId, JsonObject src, JsonObject created,
        String internalId, Boolean isTag, String source, ServiceRegistry services) {
        return sendBranch(projectId, src, created, internalId, isTag, source, null, services);
    }

    // make sure only one branch is made at a time
    public static synchronized JsonObject sendBranch(String projectId, JsonObject src, JsonObject created,
        String internalId, Boolean isTag, String source, String commitId, ServiceRegistry services) {
        // FIXME: need to include branch in commit history
        JsonObject branchJson = new JsonObject();

        branchJson.addProperty("source", source);
        logger.info("SrcJSON in sendBranch: " + src.toString());
        String srcId = src.get(Sjm.SYSMLID).getAsString();
        String createdId = created.get(Sjm.SYSMLID).getAsString();
        boolean hasCommit = (commitId != null && !commitId.isEmpty());

        NodeRef person = services.getPersonService().getPersonOrNull(JsonUtil.getOptString(created, Sjm.CREATOR));
        if (person != null) {
            user = services.getNodeService().getProperty(person, ContentModel.PROP_EMAIL).toString();
        }

        BranchTask task =
            new BranchTask(projectId, srcId, created.toString(), internalId, isTag, source, commitId, user);

        created.addProperty("status", "creating");

        String queueId = hasCommit ? "branch_from_the_past" : String.format("%s-%s", projectId, createdId);

        initHazelcastQueue(queueId);
        BlockingQueue<BranchTask> queue = hzInstance.getQueue(queueId);

        try {
            queue.put(task);
        } catch (InterruptedException ie) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Interrupted: %s", LogUtil.getStackTrace(ie)));
            }
            created.addProperty("status", "rejected");
            Thread.currentThread().interrupt();
        }

        return branchJson;
    }

    public static boolean sendJmsMsg(JsonObject json, String eventType, String refId, String projectId) {
        boolean status = false;
        if (jmsConnection != null) {
            status = jmsConnection.publish(json, eventType, refId, projectId);
            if (logger.isDebugEnabled()) {
                String msg =
                    "Event: " + eventType + ", RefId: " + refId + ", ProjectId: " + projectId + System.lineSeparator();
                msg += "JSONObject: " + json;
                logger.debug(msg);
            }
        } else {
            if (logger.isInfoEnabled())
                logger.info("JMS Connection not available");
        }

        return status;
    }

    private static JsonObject createNode(String sysmlid, String user, String date, JsonObject e) {

        if (e == null) {
            e = new JsonObject();
        }

        e.addProperty(Sjm.SYSMLID, sysmlid);
        e.addProperty(Sjm.CREATOR, user);
        e.addProperty(Sjm.CREATED, date);
        e.addProperty(Sjm.MODIFIER, user);
        e.addProperty(Sjm.MODIFIED, date);

        return e;
    }

    public static boolean createOrUpdateSiteChar(JsonObject siteChar, String projectId, String refId,
        ServiceRegistry services) {

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        String folderName = JsonUtil.getOptString(siteChar, Sjm.NAME);
        String folderId = JsonUtil.getOptString(siteChar, Sjm.SYSMLID);

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
                    documentLibrary = site.createFolder("documentLibrary", null);
                    documentLibrary.setProperty(Acm.CM_TITLE, "Document Library");
                }
                EmsScriptNode projectDocumentLibrary = documentLibrary.childByNamePath(projectId);
                if (projectDocumentLibrary == null) {
                    projectDocumentLibrary = documentLibrary.createFolder(projectId, null);
                }
                EmsScriptNode siteCharFolder = projectDocumentLibrary.childByNamePath(folderId);
                if (siteCharFolder == null) {
                    siteCharFolder = projectDocumentLibrary.createFolder(folderId, null);
                    siteCharFolder.setProperty(Acm.CM_TITLE, folderName);
                    return true;
                } else {
                    siteCharFolder.setProperty(Acm.CM_TITLE, folderName);
                    return true;
                }
            }
        }

        return false;
    }

    private static Pattern pattern = Pattern.compile("<mms-cf.*?mms-element-id=\"([a-zA-Z0-9_\\-]+)\"");

    public static void processValueEdges(JsonObject element, List<Pair<String, String>> edges) {
        JsonObject defaultValue = JsonUtil.getOptObject(element, Sjm.DEFAULTVALUE);
        JsonArray slotValues = JsonUtil.getOptArray(element, "value");
        if (JsonUtil.getOptString(defaultValue, Sjm.TYPE).equals("LiteralString")) {
            processDocumentEdges(element.get(Sjm.SYSMLID).getAsString(), JsonUtil.getOptString(defaultValue, "value"),
                edges);
        }
        for (int i = 0; i < slotValues.size(); i++) {
            JsonObject val = JsonUtil.getOptObject(slotValues, i);
            if (JsonUtil.getOptString(val, Sjm.TYPE).equals("LiteralString")) {
                processDocumentEdges(element.get(Sjm.SYSMLID).getAsString(), JsonUtil.getOptString(val, "value"),
                    edges);
            }
        }
    }

    public static void processDocumentEdges(String sysmlid, String doc, List<Pair<String, String>> documentEdges) {
        if (doc != null && !doc.isEmpty()) {
            Matcher matcher = pattern.matcher(doc);

            while (matcher.find()) {
                String mmseid = matcher.group(1);
                if (mmseid != null) {
                    documentEdges.add(new Pair<>(sysmlid, mmseid));
                }
            }
        }
    }

    public static void processContentsJson(String sysmlId, JsonObject contents,
        List<Pair<String, String>> documentEdges) {
        if (contents != null) {
            if (contents.has("operand")) {
                JsonArray operand = contents.get("operand").getAsJsonArray();
                for (int ii = 0; ii < operand.size(); ii++) {
                    JsonObject value = JsonUtil.getOptObject(operand, ii);
                    if (value.has("instanceId")) {
                        documentEdges.add(new Pair<>(sysmlId, value.get("instanceId").getAsString()));
                    }
                }
            }
        }
    }

    public static void processInstanceSpecificationSpecificationJson(String sysmlId, JsonObject iss,
        List<Pair<String, String>> documentEdges) {
        if (iss != null) {
            if (iss.has("value") && iss.has("type") && iss.get("type").getAsString().equals("LiteralString")) {
                String string = iss.get("value").getAsString();
                try {
                    JsonObject json = JsonUtil.buildFromString(string);
                    StringBuilder text = new StringBuilder();
                    Set<Object> sources = findKeyValueInJsonObject(json, "source", text);
                    for (Object source : sources) {
                        if (source instanceof String) {
                            documentEdges.add(new Pair<>(sysmlId, (String) source));
                        }
                    }
                    if (text.length() != 0) {
                        Matcher matcher = pattern.matcher(text);
                        while (matcher.find()) {
                            String mmseid = matcher.group(1);
                            if (mmseid != null) {
                                documentEdges.add(new Pair<>(sysmlId, mmseid));
                            }
                        }
                    }
                } catch (JsonSyntaxException ex) {
                    logger.warn(String.format("unable to parse json in element %s", sysmlId));
                }
            }
        }
    }

    public static Set<Object> findKeyValueInJsonObject(JsonObject json, String keyMatch, StringBuilder text) {
        Set<Object> result = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (key.equals("text")) {
                text.append(value.getAsString());
            }
            if (key.equals(keyMatch)) {
                result.add(value.getAsString());
            } else if (value.isJsonObject()) {
                result.addAll(findKeyValueInJsonObject(value.getAsJsonObject(), keyMatch, text));
            } else if (value.isJsonArray()) {
                result.addAll(findKeyValueInJsonArray(value.getAsJsonArray(), keyMatch, text));
            }
        }
        return result;
    }

    public static Set<Object> findKeyValueInJsonArray(JsonArray jsonArray, String keyMatch, StringBuilder text) {
        Set<Object> result = new HashSet<>();

        for (int ii = 0; ii < jsonArray.size(); ii++) {
            JsonElement e = jsonArray.get(ii);
            if (e.isJsonObject()) {
                result.addAll(findKeyValueInJsonObject(e.getAsJsonObject(), keyMatch, text));
            } else if (e.isJsonArray()) {
                result.addAll(findKeyValueInJsonArray(e.getAsJsonArray(), keyMatch, text));
            }
        }

        return result;
    }
}
