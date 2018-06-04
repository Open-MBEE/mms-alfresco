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
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

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
import gov.nasa.jpl.mbee.util.Timer;
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

    private static final String HOLDING_BIN_PREFIX = "holding_bin_";

    private static ElasticHelper eh = null;
    private static JmsConnection jmsConnection = null;

    private static String user = null;

    private Map<String, LinkedList<LinkedBlockingDeque<String>>> commitQueue = new HashMap<>();

    public static void setJmsConnection(JmsConnection jmsConnection) {
        if (logger.isInfoEnabled()) {
            logger.info("Setting jms");
        }
        CommitUtil.jmsConnection = jmsConnection;
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

    private static boolean bulkElasticEntry(JSONArray elements, String operation, boolean refresh, String index) {
        if (elements.length() > 0) {
            try {
                boolean bulkEntry = eh.bulkIndexElements(elements, operation, refresh, index);
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

    private static boolean isPartProperty(JSONObject e) {
        if (!e.has(Sjm.TYPE) || !e.getString(Sjm.TYPE).equals("Property")) {
            return false;
        }
        if (!e.has(Sjm.AGGREGATION) || e.getString(Sjm.AGGREGATION).equals("none")) {
            return false;
        }
        if (e.has(Sjm.DEFAULTVALUE) && !e.getString(Sjm.DEFAULTVALUE).equals(JSONObject.NULL)) {
            return false;
        }
        JSONArray appliedS = e.optJSONArray(Sjm.APPLIEDSTEREOTYPEIDS);
        if (appliedS != null) {
            for (int i = 0; i < appliedS.length(); i++) {
                String s = appliedS.getString(i);
                if (s.equals(Sjm.VALUEPROPERTY) || s.equals(Sjm.CONSTRAINTPROPERTY)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean processDeltasForDb(JSONObject delta, String projectId, String refId, JSONObject jmsPayload,
        boolean withChildViews, ServiceRegistry services) {
        // :TODO write to elastic for elements, write to postgres, write to elastic for commits
        // :TODO should return a 500 here to stop writes if one insert fails
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
        List<Pair<String, String>> addEdges = new ArrayList<>();
        List<Pair<String, String>> viewEdges = new ArrayList<>();
        List<Pair<String, String>> childViewEdges = new ArrayList<>();

        if (bulkElasticEntry(added, "added", withChildViews, projectId) && bulkElasticEntry(updated, "updated",
            withChildViews, projectId)) {

            try {
                List<Map<String, Object>> nodeInserts = new ArrayList<>();
                List<Map<String, Object>> edgeInserts = new ArrayList<>();
                List<Map<String, Object>> childEdgeInserts = new ArrayList<>();
                List<Map<String, Object>> nodeUpdates = new ArrayList<>();
                Set<String> uniqueEdge = new HashSet<>();

                for (int i = 0; i < added.length(); i++) {
                    JSONObject e = added.getJSONObject(i);
                    Map<String, Object> node = new HashMap<>();
                    jmsAdded.put(e.getString(Sjm.SYSMLID));
                    int nodeType = getNodeType(e).getValue();

                    if (e.has(Sjm.ELASTICID)) {
                        node.put(Sjm.ELASTICID, e.getString(Sjm.ELASTICID));
                        node.put(Sjm.SYSMLID, e.getString(Sjm.SYSMLID));
                        node.put(NODETYPE, nodeType);
                        node.put(INITIALCOMMIT, e.getString(Sjm.ELASTICID));
                        node.put(LASTCOMMIT, commitElasticId);
                        nodeInserts.add(node);
                    }

                    if (e.has(Sjm.OWNERID) && e.getString(Sjm.OWNERID) != null && e.getString(Sjm.SYSMLID) != null) {
                        Pair<String, String> p = new Pair<>(e.getString(Sjm.OWNERID), e.getString(Sjm.SYSMLID));
                        addEdges.add(p);
                    }

                    String doc = e.optString(Sjm.DOCUMENTATION);
                    processDocumentEdges(e.getString(Sjm.SYSMLID), doc, viewEdges);

                    if (nodeType == DbNodeTypes.SITEANDPACKAGE.getValue()) {
                        createOrUpdateSiteChar(e, projectId, refId, services);
                    }
                    String type = e.optString(Sjm.TYPE);
                    if (type.equals("Slot") || type.equals("Property") || type.equals("Port")) {
                        processValueEdges(e, viewEdges);
                    }
                    if (e.has(Sjm.CONTENTS)) {
                        JSONObject contents = e.optJSONObject(Sjm.CONTENTS);
                        processContentsJson(e.getString(Sjm.SYSMLID), contents, viewEdges);
                    } else if (e.has(Sjm.SPECIFICATION) && nodeType == DbNodeTypes.INSTANCESPECIFICATION.getValue()) {
                        JSONObject iss = e.optJSONObject(Sjm.SPECIFICATION);
                        processInstanceSpecificationSpecificationJson(e.getString(Sjm.SYSMLID), iss, viewEdges);
                        processContentsJson(e.getString(Sjm.SYSMLID), iss, viewEdges);
                    }
                    if (nodeType == DbNodeTypes.VIEW.getValue() || nodeType == DbNodeTypes.DOCUMENT.getValue()) {
                        JSONArray owned = e.optJSONArray(Sjm.OWNEDATTRIBUTEIDS);
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
                    JSONObject e = deleted.getJSONObject(i);
                    jmsDeleted.put(e.getString(Sjm.SYSMLID));
                    pgh.deleteEdgesForNode(e.getString(Sjm.SYSMLID));
                    pgh.deleteNode(e.getString(Sjm.SYSMLID));
                    deletedSysmlIds.add(e.getString(Sjm.SYSMLID));
                }

                for (int i = 0; i < updated.length(); i++) {
                    JSONObject e = updated.getJSONObject(i);
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
                    processDocumentEdges(e.getString(Sjm.SYSMLID), doc, viewEdges);


                    if (nodeType == DbNodeTypes.SITEANDPACKAGE.getValue()) {
                        createOrUpdateSiteChar(e, projectId, refId, services);
                    }
                    String type = e.optString(Sjm.TYPE);
                    if (type.equals("Slot") || type.equals("Property") || type.equals("Port")) {
                        processValueEdges(e, viewEdges);
                    }
                    if (e.has(Sjm.CONTENTS)) {
                        JSONObject contents = e.optJSONObject(Sjm.CONTENTS);
                        processContentsJson(e.getString(Sjm.SYSMLID), contents, viewEdges);
                    } else if (e.has(Sjm.SPECIFICATION) && nodeType == DbNodeTypes.INSTANCESPECIFICATION.getValue()) {
                        JSONObject iss = e.optJSONObject(Sjm.SPECIFICATION);
                        processInstanceSpecificationSpecificationJson(e.getString(Sjm.SYSMLID), iss, viewEdges);
                        processContentsJson(e.getString(Sjm.SYSMLID), iss, viewEdges);
                    }
                    if (nodeType == DbNodeTypes.VIEW.getValue() || nodeType == DbNodeTypes.DOCUMENT.getValue()) {
                        JSONArray owned = e.optJSONArray(Sjm.OWNEDATTRIBUTEIDS);
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
                        Map<String, Object> updatedNode = new HashMap<>();
                        updatedNode.put(Sjm.ELASTICID, e.getString(Sjm.ELASTICID));
                        updatedNode.put(Sjm.SYSMLID, e.getString(Sjm.SYSMLID));
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
                    pgh.updateLastCommits(commitElasticId, deletedSysmlIds);
                    pgh.commitTransaction();
                    pgh.insertCommit(commitElasticId, DbCommitTypes.COMMIT, creator);
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

        if (!commitElasticId.isEmpty()) {
            jmsPayload.put(Sjm.COMMITID, commitElasticId);
        }

        return true;
    }

    public static void processNodesAndEdgesWithoutCommit(JSONArray elements, List<Map<String, Object>> nodeInserts,
        List<Map<String, Object>> edgeInserts, List<Map<String, Object>> childEdgeInserts) {

        List<Pair<String, String>> addEdges = new ArrayList<>();
        List<Pair<String, String>> viewEdges = new ArrayList<>();
        List<Pair<String, String>> childViewEdges = new ArrayList<>();
        List<String> uniqueEdge = new ArrayList<>();

        for (int i = 0; i < elements.length(); i++) {
            JSONObject e = elements.getJSONObject(i);
            Map<String, Object> node = new HashMap<>();
            int nodeType = getNodeType(e).getValue();

            if (e.has(Sjm.ELASTICID)) {
                node.put(Sjm.ELASTICID, e.getString(Sjm.ELASTICID));
                node.put(Sjm.SYSMLID, e.getString(Sjm.SYSMLID));
                node.put(NODETYPE, nodeType);
                node.put(LASTCOMMIT, e.getString(Sjm.COMMITID));
                node.put(DELETED, false);
                nodeInserts.add(node);
            }

            if (e.has(Sjm.OWNERID) && e.getString(Sjm.OWNERID) != null && e.getString(Sjm.SYSMLID) != null) {
                Pair<String, String> p = new Pair<>(e.getString(Sjm.OWNERID), e.getString(Sjm.SYSMLID));
                addEdges.add(p);
            }

            String doc = e.optString(Sjm.DOCUMENTATION);
            processDocumentEdges(e.getString(Sjm.SYSMLID), doc, viewEdges);

            String type = e.optString(Sjm.TYPE);
            if (type.equals("Slot") || type.equals("Property") || type.equals("Port")) {
                processValueEdges(e, viewEdges);
            }
            if (e.has(Sjm.CONTENTS)) {
                JSONObject contents = e.optJSONObject(Sjm.CONTENTS);
                processContentsJson(e.getString(Sjm.SYSMLID), contents, viewEdges);
            } else if (e.has(Sjm.SPECIFICATION) && nodeType == DbNodeTypes.INSTANCESPECIFICATION.getValue()) {
                JSONObject iss = e.optJSONObject(Sjm.SPECIFICATION);
                processInstanceSpecificationSpecificationJson(e.getString(Sjm.SYSMLID), iss, viewEdges);
                processContentsJson(e.getString(Sjm.SYSMLID), iss, viewEdges);

            }
            if (nodeType == DbNodeTypes.VIEW.getValue() || nodeType == DbNodeTypes.DOCUMENT.getValue()) {
                JSONArray owned = e.optJSONArray(Sjm.OWNEDATTRIBUTEIDS);
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

        for (Pair<String, String> e : addEdges) {
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

        for (Pair<String, String> e : viewEdges) {
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

        for (Pair<String, String> e : childViewEdges) {
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

    public static boolean insertForBranchInPast(PostgresHelper pgh, List<Map<String, Object>> list, String type,
        String projectId) {
        Savepoint sp = null;
        List<String> nullParents;
        try {
            sp = pgh.startTransaction();
            pgh.runBatchQueries(list, type);
            pgh.commitTransaction();
            nullParents = pgh.findNullParents();
            if (nullParents != null) {
                updateNullEdges(nullParents, projectId);
            }
            pgh.cleanEdges();
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            try {
                pgh.rollBackToSavepoint(sp);
                return false;
            } catch (SQLException se) {
                logger.error(String.format("%s", LogUtil.getStackTrace(se)));
            }
        } finally {
            pgh.close();
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
            eh = new ElasticHelper();
            Set<String> updateSet = new HashSet<>(updateParents);
            String owner = HOLDING_BIN_PREFIX + projectId;
            JSONObject query = new JSONObject();
            query.put("doc", new JSONObject().put("ownerId", owner));
            eh.bulkUpdateElements(updateSet, query.toString(), projectId);
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
    public static boolean sendDeltas(JSONObject deltaJson, String projectId, String workspaceId, String source,
        ServiceRegistry services, boolean withChildViews) {

        JSONObject jmsPayload = new JSONObject();
        try {
            eh = new ElasticHelper();
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        if (!processDeltasForDb(deltaJson, projectId, workspaceId, jmsPayload, withChildViews, services)) {
            return false;
        }

        if (source != null) {
            jmsPayload.put("source", source);
        }

        sendJmsMsg(jmsPayload, TYPE_DELTA, workspaceId, projectId);

        return true;
    }

    public static JSONObject sendOrganizationDelta(String orgId, String orgName, JSONObject orgJson) throws PSQLException
    {
        PostgresHelper pgh = new PostgresHelper();
        pgh.createOrganization(orgId, orgName);
        String defaultIndex = EmsConfig.get("elastic.index.element");
        try {
            ElasticHelper eh = new ElasticHelper();
            eh.createIndex(defaultIndex);
            orgJson.put(Sjm.ELASTICID, orgId);
            ElasticResult result = eh.indexElement(orgJson, defaultIndex);
            return result.current;
        } catch (Exception e) {
            logger.error(e);
        }

        return null;
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
                pgh.updateElasticId(projectSysmlid, eProject.elasticId);
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }
    }

    // make sure only one branch is made at a time
    public static synchronized JSONObject sendBranch(String projectId, JSONObject src, JSONObject created,
        String elasticId, Boolean isTag, String source, ServiceRegistry services) {
        return sendBranch(projectId, src, created, elasticId, isTag, source, null, services);
    }

    // make sure only one branch is made at a time
    public static synchronized JSONObject sendBranch(String projectId, JSONObject src, JSONObject created,
        String elasticId, Boolean isTag, String source, String commitId, ServiceRegistry services) {
        // FIXME: need to include branch in commit history
        JSONObject branchJson = new JSONObject();

        branchJson.put("source", source);

        NodeRef person = services.getPersonService().getPersonOrNull(created.optString(Sjm.CREATOR));
        if (person != null) {
            user = services.getNodeService().getProperty(person, ContentModel.PROP_EMAIL).toString();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

            Timer timer = new Timer();
            logger.info(String.format("Starting branch %s started by %s", created.getString(Sjm.SYSMLID),
                created.optString(Sjm.CREATOR)));

            boolean hasCommit = (commitId != null && !commitId.isEmpty());
            boolean success = false;

            String srcId = src.optString(Sjm.SYSMLID);

            PostgresHelper pgh = new PostgresHelper();
            pgh.setProject(projectId);
            pgh.setWorkspace(srcId);

            try {

                pgh.createBranchFromWorkspace(created.getString(Sjm.SYSMLID), created.getString(Sjm.NAME), elasticId,
                    commitId, isTag);
                eh = new ElasticHelper();
                logger.info(String.format("Finished copying db tables for branch %s started by %s at %s", created.getString(Sjm.SYSMLID),
                    created.optString(Sjm.CREATOR), timer));
                if (hasCommit) {
                    pgh.setWorkspace(created.getString(Sjm.SYSMLID));
                    EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, srcId);
                    JSONObject modelFromCommit = emsNodeUtil.getModelAtCommit(commitId);

                    logger.info(String.format("Finished getting elements from elastic for branch %s started by %s at %s", created.getString(Sjm.SYSMLID),
                        created.optString(Sjm.CREATOR), timer));

                    List<Map<String, Object>> nodeInserts = new ArrayList<>();
                    List<Map<String, Object>> edgeInserts = new ArrayList<>();
                    List<Map<String, Object>> childEdgeInserts = new ArrayList<>();

                    processNodesAndEdgesWithoutCommit(modelFromCommit.getJSONArray(Sjm.ELEMENTS), nodeInserts,
                        edgeInserts, childEdgeInserts);

                    logger.info(String.format("Finished processing nodes and edges for branch %s started by %s at %s", created.getString(Sjm.SYSMLID),
                        created.optString(Sjm.CREATOR), timer));

                    if (!nodeInserts.isEmpty() || !edgeInserts.isEmpty() || !childEdgeInserts.isEmpty()) {
                        if (!nodeInserts.isEmpty()) {
                            insertForBranchInPast(pgh, nodeInserts, "updates", projectId);
                        }
                        logger.info(String.format("Finished inserting nodes (%s) for branch %s started by %s at %s", nodeInserts.size(), created.getString(Sjm.SYSMLID),
                            created.optString(Sjm.CREATOR), timer));
                        if (!edgeInserts.isEmpty()) {
                            insertForBranchInPast(pgh, edgeInserts, EDGES, projectId);
                        }
                        logger.info(String.format("Finished inserting containment edges (%s) for branch %s started by %s at %s", edgeInserts.size(), created.getString(Sjm.SYSMLID),
                            created.optString(Sjm.CREATOR), timer));
                        if (!childEdgeInserts.isEmpty()) {
                            insertForBranchInPast(pgh, childEdgeInserts, EDGES, projectId);
                        }
                        logger.info(String.format("Finished inserting other edges (%s) for branch  %s started by %s at %s", childEdgeInserts.size(), created.getString(Sjm.SYSMLID),
                            created.optString(Sjm.CREATOR), timer));
                    } else {
                        executor.shutdown();
                        executor.awaitTermination(60L, TimeUnit.SECONDS);
                    }
                } else {
                    pgh.setWorkspace(created.getString(Sjm.SYSMLID));
                }

                Set<String> elementsToUpdate = pgh.getElasticIds();
                String payload = new JSONObject().put("script", new JSONObject().put("inline",
                    "if(ctx._source.containsKey(\"" + Sjm.INREFIDS + "\")){ctx._source." + Sjm.INREFIDS
                        + ".add(params.refId)} else {ctx._source." + Sjm.INREFIDS + " = [params.refId]}")
                    .put("params", new JSONObject().put("refId", created.getString(Sjm.SYSMLID)))).toString();
                eh.bulkUpdateElements(elementsToUpdate, payload, projectId);
                created.put("status", "created");

                success = true;

            } catch (Exception e) {
                created.put("status", "failed");
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
                }
            }

            try {
                eh.updateElement(elasticId, new JSONObject().put("doc", created), projectId);
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
                }
            }

            if (success && isTag && hasCommit) {
                pgh.setAsTag(created.getString(Sjm.SYSMLID));
            }

            branchJson.put("createdRef", created);
            sendJmsMsg(branchJson, TYPE_BRANCH, src.optString(Sjm.SYSMLID), projectId);
            logger.info(String.format("Finished branch %s started by %s finished at %s", created.getString(Sjm.SYSMLID),
                created.optString(Sjm.CREATOR), timer));

            String body = String.format("Branch %s started by %s has finished at %s", created.getString(Sjm.SYSMLID), created.optString(Sjm.CREATOR), timer);
            String subject = String.format("Branch %s has finished at %s", created.getString(Sjm.SYSMLID), timer);

            if (user != null) {
                try {
                    logger.debug("User email: " + user);

                    String sender = EmsConfig.get("app.email.from");
                    String smtpProtocol = EmsConfig.get("mail.protocol");
                    String smtpHost = EmsConfig.get("mail.host");
                    String smtpPort = EmsConfig.get("mail.port");
                    String smtpUser = EmsConfig.get("mail.username");
                    String smtpPass = EmsConfig.get("mail.password");

                    if (smtpHost.isEmpty() || sender.isEmpty()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("No smtp host");
                        }
                        executor.shutdown();
                        executor.awaitTermination(60L, TimeUnit.SECONDS);
                    }

                    Properties props = System.getProperties();

                    String prefix = "mail.smtp";
                    if (!smtpProtocol.isEmpty()) {
                        props.put("mail.transport.protocol", smtpProtocol);
                        prefix = "mail." + smtpProtocol;
                    }

                    props.put(prefix + ".host", smtpHost);
                    if (!smtpPort.isEmpty()) {
                        props.put(prefix + ".port", smtpPort);
                    }

                    Authenticator auth = null;
                    if(!smtpUser.isEmpty() && !smtpPass.isEmpty()) {
                        props.put(prefix + ".auth", "true");
                        auth = new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(smtpUser, smtpPass);
                            }
                        };
                    }

                    Session session = Session.getInstance(props, auth);

                    MimeMessage msg = new MimeMessage(session);
                    msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
                    msg.addHeader("format", "flowed");
                    msg.addHeader("Content-Transfer-Encoding", "8bit");

                    msg.setFrom(new InternetAddress(sender));
                    msg.setReplyTo(InternetAddress.parse(sender, false));
                    msg.setSubject(subject, "UTF-8");
                    msg.setText(body, "UTF-8");
                    msg.setSentDate(new Date());

                    List<InternetAddress> emails = new ArrayList<>();
                    emails.add(new InternetAddress(user));

                    InternetAddress[] ias = emails.toArray(new InternetAddress[emails.size()]);
                    msg.setRecipients(Message.RecipientType.TO, ias);

                    Transport.send(msg);
                } catch (SendFailedException sfe) {
                    logger.error("Send failed: ", sfe);
                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Error sending email: ", e);
                    }
                }

            }
        });

        executor.shutdown();

        return branchJson;
    }

    protected static boolean sendJmsMsg(JSONObject json, String eventType, String refId, String projectId) {
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

    public static void processValueEdges(JSONObject element, List<Pair<String, String>> edges) {
        JSONObject defaultValue = element.optJSONObject(Sjm.DEFAULTVALUE);
        JSONArray slotValues = element.optJSONArray("value");
        if (defaultValue != null && defaultValue.optString(Sjm.TYPE).equals("LiteralString")) {
            processDocumentEdges(element.getString(Sjm.SYSMLID), defaultValue.optString("value"), edges);
        }
        if (slotValues != null) {
            for (int i = 0; i < slotValues.length(); i++) {
                JSONObject val = slotValues.optJSONObject(i);
                if (val != null && val.optString(Sjm.TYPE).equals("LiteralString")) {
                    processDocumentEdges(element.getString(Sjm.SYSMLID), val.optString("value"), edges);
                }
            }
        }
    }

    public static void processDocumentEdges(String sysmlid, String doc, List<Pair<String, String>> documentEdges) {
        if (doc != null && doc.length() != 0) {
            Matcher matcher = pattern.matcher(doc);

            while (matcher.find()) {
                String mmseid = matcher.group(1);
                if (mmseid != null) {
                    documentEdges.add(new Pair<>(sysmlid, mmseid));
                }
            }
        }
    }

    public static void processContentsJson(String sysmlId, JSONObject contents,
        List<Pair<String, String>> documentEdges) {
        if (contents != null) {
            if (contents.has("operand")) {
                JSONArray operand = contents.getJSONArray("operand");
                for (int ii = 0; ii < operand.length(); ii++) {
                    JSONObject value = operand.optJSONObject(ii);
                    if (value != null && value.has("instanceId")) {
                        documentEdges.add(new Pair<>(sysmlId, value.getString("instanceId")));
                    }
                }
            }
        }
    }

    public static void processInstanceSpecificationSpecificationJson(String sysmlId, JSONObject iss,
        List<Pair<String, String>> documentEdges) {
        if (iss != null) {
            if (iss.has("value") && iss.has("type") && iss.getString("type").equals("LiteralString")) {
                String string = iss.getString("value");
                try {
                    JSONObject json = new JSONObject(string);
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
                } catch (JSONException ex) {
                    //case if value string isn't actually a serialized jsonobject
                }
            }
        }
    }

    public static Set<Object> findKeyValueInJsonObject(JSONObject json, String keyMatch, StringBuilder text) {
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
                result.addAll(findKeyValueInJsonObject((JSONObject) value, keyMatch, text));
            } else if (value instanceof JSONArray) {
                result.addAll(findKeyValueInJsonArray((JSONArray) value, keyMatch, text));
            }
        }
        return result;
    }

    public static Set<Object> findKeyValueInJsonArray(JSONArray jsonArray, String keyMatch, StringBuilder text) {
        Set<Object> result = new HashSet<>();

        for (int ii = 0; ii < jsonArray.length(); ii++) {
            if (jsonArray.get(ii) instanceof JSONObject) {
                result.addAll(findKeyValueInJsonObject((JSONObject) jsonArray.get(ii), keyMatch, text));
            } else if (jsonArray.get(ii) instanceof JSONArray) {
                result.addAll(findKeyValueInJsonArray((JSONArray) jsonArray.get(ii), keyMatch, text));
            }
        }

        return result;
    }
}
