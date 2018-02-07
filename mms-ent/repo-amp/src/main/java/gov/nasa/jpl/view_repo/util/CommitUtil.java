package gov.nasa.jpl.view_repo.util;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
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
import org.postgresql.util.PSQLException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

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

    public static DbNodeTypes getNodeType(JsonObject e) {

        String type = e.get(Sjm.TYPE).getAsString().toLowerCase();

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

    public static boolean isSite(JsonObject element) {
        return element.has(Sjm.ISSITE) && element.get(Sjm.ISSITE).getAsBoolean();
    }

    private static boolean bulkElasticEntry(JsonArray elements, String operation, boolean refresh, String index) {
        if (elements.size() > 0) {
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

    private static boolean isPartProperty(JsonObject e) {
        if (!e.has(Sjm.TYPE) || !e.get(Sjm.TYPE).getAsString().equals("Property")) {
            return false;
        }
        JsonArray appliedS = JsonUtil.getOptArray(e, Sjm.APPLIEDSTEREOTYPEIDS);
        if (appliedS.size() == 0) {
            return false;
        }
        for (int i = 0; i < appliedS.size(); i++) {
            String s = appliedS.get(i).getAsString();
            if (Sjm.PROPERTYSIDS.containsValue(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean processDeltasForDb(JsonObject delta, String projectId, String refId, JsonObject jmsPayload,
        boolean withChildViews, ServiceRegistry services) {
        // :TODO write to elastic for elements, write to postgres, write to elastic for commits
        // :TODO should return a 500 here to stop writes if one insert fails
        PostgresHelper pgh = new PostgresHelper();
        pgh.setProject(projectId);
        pgh.setWorkspace(refId);

        JsonArray added = JsonUtil.getOptArray(delta, "addedElements");
        JsonArray updated = JsonUtil.getOptArray(delta, "updatedElements");
        JsonArray deleted = JsonUtil.getOptArray(delta, "deletedElements");

        String creator = delta.get("commit").getAsJsonObject().get(Sjm.CREATOR).getAsString();
        String commitElasticId = delta.get("commit").getAsJsonObject().get(Sjm.ELASTICID).getAsString();

        JsonObject jmsWorkspace = new JsonObject();
        JsonArray jmsAdded = new JsonArray();
        JsonArray jmsUpdated = new JsonArray();
        JsonArray jmsDeleted = new JsonArray();

        List<String> deletedSysmlIds = new ArrayList<>();
        List<Pair<String, String>> addEdges = new ArrayList<>();
        List<Pair<String, String>> viewEdges = new ArrayList<>();
        List<Pair<String, String>> childViewEdges = new ArrayList<>();

        if (bulkElasticEntry(added, "added", withChildViews, projectId) && bulkElasticEntry(updated, "updated",
            withChildViews, projectId)) {

            try {
                List<Map<String, String>> nodeInserts = new ArrayList<>();
                List<Map<String, String>> edgeInserts = new ArrayList<>();
                List<Map<String, String>> childEdgeInserts = new ArrayList<>();
                List<Map<String, String>> nodeUpdates = new ArrayList<>();
                Set<String> uniqueEdge = new HashSet<>();

                for (int i = 0; i < added.size(); i++) {
                    JsonObject e = added.get(i).getAsJsonObject();
                    Map<String, String> node = new HashMap<>();
                    jmsAdded.add(e.get(Sjm.SYSMLID).getAsString());
                    int nodeType = getNodeType(e).getValue();

                    if (e.has(Sjm.ELASTICID)) {
                        node.put(Sjm.ELASTICID, e.get(Sjm.ELASTICID).getAsString());
                        node.put(Sjm.SYSMLID, e.get(Sjm.SYSMLID).getAsString());
                        node.put(NODETYPE, Integer.toString(nodeType));
                        node.put(INITIALCOMMIT, e.get(Sjm.ELASTICID).getAsString());
                        node.put(LASTCOMMIT, commitElasticId);
                        nodeInserts.add(node);
                    }

                    if (e.has(Sjm.OWNERID) && !e.get(Sjm.OWNERID).isJsonNull() 
                                    && e.has(Sjm.SYSMLID) && !e.get(Sjm.SYSMLID).isJsonNull()) {
                        Pair<String, String> p = new Pair<>(e.get(Sjm.OWNERID).getAsString(), 
                                        e.get(Sjm.SYSMLID).getAsString());
                        addEdges.add(p);
                    }

                    String doc = JsonUtil.getOptString(e, Sjm.DOCUMENTATION);
                    if (!doc.equals("")) {
                        processDocumentEdges(e.get(Sjm.SYSMLID).getAsString(), doc, viewEdges);
                    }

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
                            Pair<String, String> p = new Pair<>(e.get(Sjm.SYSMLID).getAsString(), owned.get(j).getAsString());
                            childViewEdges.add(p);
                        }
                    }
                    if (isPartProperty(e)) {
                        String typeid = JsonUtil.getOptString(e, Sjm.TYPEID);
                        Pair<String, String> p = new Pair<>(e.get(Sjm.SYSMLID).getAsString(), typeid);
                        childViewEdges.add(p);
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

                    if (e.has(Sjm.OWNERID) && !e.get(Sjm.OWNERID).isJsonNull() && !e.get(Sjm.SYSMLID).isJsonNull()) {
                        Pair<String, String> p = new Pair<>(e.get(Sjm.OWNERID).getAsString(), 
                                        e.get(Sjm.SYSMLID).getAsString());
                        addEdges.add(p);
                    }
                    String doc = JsonUtil.getOptString(e, Sjm.DOCUMENTATION);
                    if (!doc.equals("")) {
                        processDocumentEdges(e.get(Sjm.SYSMLID).getAsString(), doc, viewEdges);
                    }

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
                            Pair<String, String> p = new Pair<>(e.get(Sjm.SYSMLID).getAsString(), 
                                            owned.get(j).getAsString());
                            childViewEdges.add(p);
                        }
                    }
                    if (isPartProperty(e)) {
                        String typeid = JsonUtil.getOptString(e, Sjm.TYPEID);
                        if (typeid != null) {
                            Pair<String, String> p = new Pair<>(e.get(Sjm.SYSMLID).getAsString(), typeid);
                            childViewEdges.add(p);
                        }
                    }
                    if (e.has(Sjm.ELASTICID)) {
                        Map<String, String> updatedNode = new HashMap<>();
                        updatedNode.put(Sjm.ELASTICID, e.get(Sjm.ELASTICID).getAsString());
                        updatedNode.put(Sjm.SYSMLID, e.get(Sjm.SYSMLID).getAsString());
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

        jmsWorkspace.add("addedElements", jmsAdded);
        jmsWorkspace.add("updatedElements", jmsUpdated);
        jmsWorkspace.add("deletedElements", jmsDeleted);

        jmsPayload.add("refs", jmsWorkspace);

        return true;
    }

    public static void processNodesAndEdgesWithoutCommit(JsonArray elements, List<Map<String, String>> nodeInserts,
        List<Map<String, String>> edgeInserts, List<Map<String, String>> childEdgeInserts) {

        List<Pair<String, String>> addEdges = new ArrayList<>();
        List<Pair<String, String>> viewEdges = new ArrayList<>();
        List<Pair<String, String>> childViewEdges = new ArrayList<>();
        List<String> uniqueEdge = new ArrayList<>();

        for (int i = 0; i < elements.size(); i++) {
            JsonObject e = elements.get(i).getAsJsonObject();
            Map<String, String> node = new HashMap<>();
            int nodeType = getNodeType(e).getValue();

            if (e.has(Sjm.ELASTICID)) {
                node.put(Sjm.ELASTICID, e.get(Sjm.ELASTICID).getAsString());
                node.put(Sjm.SYSMLID, e.get(Sjm.SYSMLID).getAsString());
                node.put(NODETYPE, Integer.toString(nodeType));
                node.put(LASTCOMMIT, e.get(Sjm.COMMITID).getAsString());
                node.put(DELETED, "false");
                nodeInserts.add(node);
            }

            if (e.has(Sjm.OWNERID) && !e.get(Sjm.OWNERID).isJsonNull() && !e.get(Sjm.SYSMLID).isJsonNull()) {
                Pair<String, String> p = new Pair<>(e.get(Sjm.OWNERID).getAsString(), 
                                e.get(Sjm.SYSMLID).getAsString());
                addEdges.add(p);
            }

            String doc = JsonUtil.getOptString(e, Sjm.DOCUMENTATION);
            if (!doc.equals("")) {
                processDocumentEdges(e.get(Sjm.SYSMLID).getAsString(), doc, viewEdges);
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
                    Pair<String, String> p = new Pair<>(e.get(Sjm.SYSMLID).getAsString(), 
                                    owned.get(j).getAsString());
                    childViewEdges.add(p);
                }
            }
            if (isPartProperty(e)) {
                String typeid = JsonUtil.getOptString(e, Sjm.TYPEID);
                Pair<String, String> p = new Pair<>(e.get(Sjm.SYSMLID).getAsString(), typeid);
                childViewEdges.add(p);
            }
        }

        for (Pair<String, String> e : addEdges) {
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

        for (Pair<String, String> e : viewEdges) {
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

        for (Pair<String, String> e : childViewEdges) {
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

    public static boolean insertForBranchInPast(PostgresHelper pgh, List<Map<String, String>> list, String type,
        String projectId) {
        Savepoint sp = null;
        List<String> nullParents;
        try {
            sp = pgh.startTransaction();
            pgh.runBulkQueries(list, type);
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
            JsonObject query = new JsonObject();
            JsonObject doc = new JsonObject();
            doc.addProperty(Sjm.OWNERID, owner);
            query.add("doc", doc);
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
    public static boolean sendDeltas(JsonObject deltaJson, String projectId, String workspaceId, String source,
        ServiceRegistry services, boolean withChildViews) {

        JsonObject jmsPayload = new JsonObject();
        try {
            eh = new ElasticHelper();
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        if (!processDeltasForDb(deltaJson, projectId, workspaceId, jmsPayload, withChildViews, services)) {
            return false;
        }

        if (source != null) {
            jmsPayload.addProperty("source", source);
        }

        sendJmsMsg(jmsPayload, TYPE_DELTA, workspaceId, projectId);

        return true;
    }

    public static void sendOrganizationDelta(String orgId, String orgName, String user) throws PSQLException
    {
        PostgresHelper pgh = new PostgresHelper();
        pgh.createOrganization(orgId, orgName);
    }

    public static void sendProjectDelta(JsonObject o, String orgId, String user) {

        PostgresHelper pgh = new PostgresHelper();
        String date = TimeUtils.toTimestamp(new Date().getTime());
        JsonObject jmsMsg = new JsonObject();
        JsonObject siteElement = new JsonObject();

        JsonObject projectHoldingBin;
        JsonObject viewInstanceBin;
        JsonObject project;
        ElasticResult eProject;
        ElasticResult eProjectHoldingBin;
        ElasticResult eViewInstanceBin;

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
        projectHoldingBin.addProperty(Sjm.ISSITE, false);
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
        viewInstanceBin.addProperty(Sjm.ISSITE, false);
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

                JsonObject addedElements = new JsonObject();
                JsonArray elementsArray = new JsonArray();
                elementsArray.add(projectHoldingBin);
                elementsArray.add(viewInstanceBin);
                addedElements.add("addedElements", elementsArray);
                jmsMsg.add("refs", addedElements);
                jmsMsg.addProperty("source", "mms");
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
    public static synchronized JsonObject sendBranch(String projectId, JsonObject src, JsonObject created,
        String elasticId, Boolean isTag, String source, ServiceRegistry services) {
        return sendBranch(projectId, src, created, elasticId, isTag, source, null, services);
    }

    // make sure only one branch is made at a time
    public static synchronized JsonObject sendBranch(String projectId, JsonObject src, JsonObject created,
        String elasticId, Boolean isTag, String source, String commitId, ServiceRegistry services) {
        // FIXME: need to include branch in commit history
        JsonObject branchJson = new JsonObject();

        branchJson.addProperty("source", source);

        NodeRef person = services.getPersonService().getPersonOrNull(JsonUtil.getOptString(created, Sjm.CREATOR));
        if (person != null) {
            user = services.getNodeService().getProperty(person, ContentModel.PROP_EMAIL).toString();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

            Timer timer = new Timer();
            logger.info(String.format("Starting branch %s started by %s", created.get(Sjm.SYSMLID).getAsString(),
                JsonUtil.getOptString(created, Sjm.CREATOR)));

            boolean hasCommit = (commitId != null && !commitId.isEmpty());
            boolean success = false;

            String srcId = JsonUtil.getOptString(src, Sjm.SYSMLID);

            PostgresHelper pgh = new PostgresHelper();
            pgh.setProject(projectId);
            pgh.setWorkspace(srcId);

            try {

                pgh.createBranchFromWorkspace(created.get(Sjm.SYSMLID).getAsString(), 
                                created.get(Sjm.NAME).getAsString(), elasticId,
                                commitId, isTag);
                eh = new ElasticHelper();
                logger.info(String.format("Finished copying db tables for branch %s started by %s at %s", 
                                created.get(Sjm.SYSMLID).getAsString(),
                                JsonUtil.getOptString(created, Sjm.CREATOR), timer));
                if (hasCommit) {
                    pgh.setWorkspace(created.get(Sjm.SYSMLID).getAsString());
                    EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, srcId);
                    JsonObject modelFromCommit = emsNodeUtil.getModelAtCommit(commitId);

                    logger.info(String.format("Finished getting elements from elastic for branch %s started by %s at %s", 
                                    created.get(Sjm.SYSMLID).getAsString(),
                                    JsonUtil.getOptString(created, Sjm.CREATOR), timer));

                    List<Map<String, String>> nodeInserts = new ArrayList<>();
                    List<Map<String, String>> edgeInserts = new ArrayList<>();
                    List<Map<String, String>> childEdgeInserts = new ArrayList<>();

                    processNodesAndEdgesWithoutCommit(modelFromCommit.get(Sjm.ELEMENTS).getAsJsonArray(), nodeInserts,
                        edgeInserts, childEdgeInserts);

                    logger.info(String.format("Finished processing nodes and edges for branch %s started by %s at %s", 
                                    created.get(Sjm.SYSMLID).getAsString(),
                                    JsonUtil.getOptString(created, Sjm.CREATOR), timer));

                    if (!nodeInserts.isEmpty() || !edgeInserts.isEmpty() || !childEdgeInserts.isEmpty()) {
                        if (!nodeInserts.isEmpty()) {
                            insertForBranchInPast(pgh, nodeInserts, "updates", projectId);
                        }
                        logger.info(String.format("Finished inserting nodes (%s) for branch %s started by %s at %s", 
                                        nodeInserts.size(), created.get(Sjm.SYSMLID).getAsString(),
                                        JsonUtil.getOptString(created, Sjm.CREATOR), timer));
                        if (!edgeInserts.isEmpty()) {
                            insertForBranchInPast(pgh, edgeInserts, EDGES, projectId);
                        }
                        logger.info(String.format("Finished inserting containment edges (%s) for branch %s started by %s at %s", 
                                        edgeInserts.size(), created.get(Sjm.SYSMLID).getAsString(),
                                        JsonUtil.getOptString(created, Sjm.CREATOR), timer));
                        if (!childEdgeInserts.isEmpty()) {
                            insertForBranchInPast(pgh, childEdgeInserts, EDGES, projectId);
                        }
                        logger.info(String.format("Finished inserting other edges (%s) for branch  %s started by %s at %s", 
                                        childEdgeInserts.size(), created.get(Sjm.SYSMLID).getAsString(),
                                        JsonUtil.getOptString(created, Sjm.CREATOR), timer));
                    } else {
                        executor.shutdown();
                        executor.awaitTermination(60L, TimeUnit.SECONDS);
                    }
                } else {
                    pgh.setWorkspace(created.get(Sjm.SYSMLID).getAsString());
                }

                Set<String> elementsToUpdate = pgh.getElasticIds();
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
                params.add("refId", created.get(Sjm.SYSMLID));
                eh.bulkUpdateElements(elementsToUpdate, script.toString(), projectId);
                created.addProperty("status", "created");

                success = true;

            } catch (Exception e) {
                created.addProperty("status", "failed");
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
                }
            }

            try {
                JsonObject doc = new JsonObject();
                doc.add("doc", created);
                eh.updateElement(elasticId, doc, projectId);
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
                }
            }

            if (success && isTag && hasCommit) {
                pgh.setAsTag(created.get(Sjm.SYSMLID).getAsString());
            }

            branchJson.add("createdRef", created);
            sendJmsMsg(branchJson, TYPE_BRANCH, JsonUtil.getOptString(src, Sjm.SYSMLID), projectId);
            logger.info(String.format("Finished branch %s started by %s finished at %s", 
                            created.get(Sjm.SYSMLID).getAsString(),
                            JsonUtil.getOptString(created, Sjm.CREATOR), timer));

            String body = String.format("Branch %s started by %s has finished at %s", 
                            created.get(Sjm.SYSMLID).getAsString(), 
                            JsonUtil.getOptString(created, Sjm.CREATOR), timer);
            String subject = String.format("Branch %s has finished at %s", 
                            created.get(Sjm.SYSMLID).getAsString(), timer);

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

    protected static boolean sendJmsMsg(JsonObject json, String eventType, String refId, String projectId) {
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

    public static void processValueEdges(JsonObject element, List<Pair<String, String>> edges) {
        JsonObject defaultValue = JsonUtil.getOptObject(element, Sjm.DEFAULTVALUE);
        JsonArray slotValues = JsonUtil.getOptArray(element, "value");
        if (JsonUtil.getOptString(defaultValue, Sjm.TYPE).equals("LiteralString")) {
            processDocumentEdges(element.get(Sjm.SYSMLID).getAsString(), 
                            JsonUtil.getOptString(defaultValue, "value"), edges);
        }
        for (int i = 0; i < slotValues.size(); i++) {
            JsonObject val = JsonUtil.getOptObject(slotValues, i);
            if (val != null && JsonUtil.getOptString(val, Sjm.TYPE).equals("LiteralString")) {
                processDocumentEdges(element.get(Sjm.SYSMLID).getAsString(), 
                                JsonUtil.getOptString(val, "value"), edges);
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

    public static void processContentsJson(String sysmlId, JsonObject contents,
        List<Pair<String, String>> documentEdges) {
        if (contents != null) {
            if (contents.has("operand")) {
                JsonArray operand = contents.get("operand").getAsJsonArray();
                for (int ii = 0; ii < operand.size(); ii++) {
                    JsonObject value = JsonUtil.getOptObject(operand, ii);
                    if (value != null && value.has("instanceId")) {
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
                    JsonParser parser = new JsonParser();
                    JsonObject json = parser.parse(string).getAsJsonObject();
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
                } catch (JsonSyntaxException ex) {
                    //case if value string isn't actually a serialized jsonobject
                }
            }
        }
    }

    public static Set<Object> findKeyValueInJsonObject(JsonObject json, String keyMatch, StringBuilder text) {
        Set<Object> result = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            Object value = json.get(key);
            if (key.equals("text")) {
                text.append(value);
            }
            if (key.equals(keyMatch)) {
                result.add(value);
            } else if (value instanceof JsonObject) {
                result.addAll(findKeyValueInJsonObject((JsonObject) value, keyMatch, text));
            } else if (value instanceof JsonArray) {
                result.addAll(findKeyValueInJsonArray((JsonArray) value, keyMatch, text));
            }
        }
        return result;
    }

    public static Set<Object> findKeyValueInJsonArray(JsonArray jsonArray, String keyMatch, StringBuilder text) {
        Set<Object> result = new HashSet<>();

        for (int ii = 0; ii < jsonArray.size(); ii++) {
            if (jsonArray.get(ii).isJsonObject()) {
                result.addAll(findKeyValueInJsonObject((JsonObject) jsonArray.get(ii), keyMatch, text));
            } else if (jsonArray.get(ii).isJsonArray()) {
                result.addAll(findKeyValueInJsonArray((JsonArray) jsonArray.get(ii), keyMatch, text));
            }
        }

        return result;
    }
}
