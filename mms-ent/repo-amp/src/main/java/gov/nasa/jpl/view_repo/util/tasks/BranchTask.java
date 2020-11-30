package gov.nasa.jpl.view_repo.util.tasks;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.db.*;
import gov.nasa.jpl.view_repo.db.DocStoreInterface;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;

public class BranchTask implements Callable<JsonObject>, Serializable {

    private static final long serialVersionUID = 561464450547556131L;

    static Logger logger = Logger.getLogger(BranchTask.class);

    private static final String NODES = "nodes";
    private static final String EDGES = "edges";
    private static final String PARENT = "parent";
    private static final String CHILD = "child";
    private static final String NODETYPE = "nodetype";
    private static final String EDGETYPE = "edgetype";
    private static final String DELETED = "deleted";
    private static final String INITIALCOMMIT = "initialcommit";
    private static final String LASTCOMMIT = "lastcommit";

    public static final String TYPE_BRANCH = "BRANCH";
    public static final String TYPE_COMMIT = "COMMIT";
    public static final String TYPE_DELTA = "DELTA";
    public static final String TYPE_MERGE = "MERGE";

    private String author;
    private String projectId;
    private String elasticId;
    private String source;
    private String commitId;
    private Boolean isTag;
    private String srcId;
    private String createdString;
    private JsonObject branchJson = null;

    private transient Timer timer;
    private transient DocStoreInterface docStoreHelper;
    private transient PostgresHelper pgh;

    public BranchTask(String projectId, String srcId, String createdString, String elasticId, Boolean isTag,
        String source, String commitId, String author) {
        this.projectId = projectId;
        this.elasticId = elasticId;
        this.source = source;
        this.commitId = commitId;
        this.isTag = isTag;
        this.srcId = srcId;
        this.createdString = createdString;
        this.author = author;
    }

    @Override public JsonObject call() {
        return createBranch();
    }

    // used in createBranch
    private static final String refScript =
        "{\"script\": {\"inline\": \"if(ctx._source.containsKey(\\\"%1$s\\\")){ctx._source.%1$s.add(params.refId)} else {ctx._source.%1$s = [params.refId]}\", \"params\":{\"refId\":\"%2$s\"}}}";

    private JsonObject createBranch() {

        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        timer = new Timer();
        JsonObject created = JsonUtil.buildFromString(createdString);

        logger.info(String.format("Starting branch %s started by %s", created.get(Sjm.SYSMLID).getAsString(),
            JsonUtil.getOptString(created, Sjm.CREATOR)));

        JsonObject bJson = new JsonObject();
        bJson.addProperty("source", source);

        boolean hasCommit = (commitId != null && !commitId.isEmpty());
        boolean success = false;

        pgh = new PostgresHelper();
        pgh.setProject(projectId);
        pgh.setWorkspace(srcId);

        logger.info("Connected to postgres");

        try {
        	docStoreHelper = DocStoreFactory.getDocStore();

            pgh.createBranchFromWorkspace(created.get(Sjm.SYSMLID).getAsString(), created.get(Sjm.NAME).getAsString(),
                elasticId, commitId, isTag);

            logger.info(String.format("Finished copying db tables for branch %s started by %s at %s",
                created.get(Sjm.SYSMLID).getAsString(), JsonUtil.getOptString(created, Sjm.CREATOR), timer));

            if (hasCommit) {
                pgh.setWorkspace(created.get(Sjm.SYSMLID).getAsString());
                EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, srcId);
                JsonObject modelFromCommit = emsNodeUtil.getModelAtCommit(commitId);

                logger.info(String.format("Finished getting elements from elastic for branch %s started by %s at %s",
                    created.get(Sjm.SYSMLID).getAsString(), JsonUtil.getOptString(created, Sjm.CREATOR), timer));

                List<Map<String, Object>> nodeInserts = new ArrayList<>();
                List<Map<String, Object>> artifactInserts = new ArrayList<>();
                List<Map<String, Object>> edgeInserts = new ArrayList<>();
                List<Map<String, Object>> childEdgeInserts = new ArrayList<>();

                processNodesAndEdgesWithoutCommit(modelFromCommit.get(Sjm.ELEMENTS).getAsJsonArray(),
                    modelFromCommit.get(Sjm.ARTIFACTS).getAsJsonArray(), nodeInserts, artifactInserts, edgeInserts,
                    childEdgeInserts);

                logger.info(String.format("Finished processing nodes and edges for branch %s started by %s at %s",
                    created.get(Sjm.SYSMLID).getAsString(), JsonUtil.getOptString(created, Sjm.CREATOR), timer));

                if (!nodeInserts.isEmpty()) {
                    insertForBranchInPast(pgh, nodeInserts, "updates", projectId);
                }

                logger.info(String
                    .format("Finished inserting nodes (%s) for branch %s started by %s at %s", nodeInserts.size(),
                        created.get(Sjm.SYSMLID).getAsString(), JsonUtil.getOptString(created, Sjm.CREATOR), timer));

                if (!artifactInserts.isEmpty()) {
                    insertForBranchInPast(pgh, artifactInserts, "artifactUpdates", projectId);
                }

                logger.info(String.format("Finished inserting artifacts (%s) for branch %s started by %s at %s",
                    artifactInserts.size(), created.get(Sjm.SYSMLID).getAsString(),
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
                    created.get(Sjm.CREATOR).getAsString(), timer));

            } else {
                pgh.setWorkspace(created.get(Sjm.SYSMLID).getAsString());
            }

            Set<String> nodesToUpdate = pgh.getElasticIdsNodes();
            String scriptToRun = String.format(refScript, Sjm.INREFIDS, created.get(Sjm.SYSMLID).getAsString());
            created.addProperty("status", "created");

            Set<String> artifactsToUpdate = pgh.getElasticIdsArtifacts();

            if (logger.isDebugEnabled()) {
                logger.debug("inRefId update: " + scriptToRun);
            }

            docStoreHelper.bulkUpdateElements(nodesToUpdate, scriptToRun, projectId, "element");
            docStoreHelper.bulkUpdateElements(artifactsToUpdate, scriptToRun, projectId, "artifact");

            created.addProperty("status", "created");

            success = true;

        } catch (Exception e) {
            logger.info("Branch creation failed");
            logger.info(String.format("%s", LogUtil.getStackTrace(e)));
            created.addProperty("status", "failed");
        }

        try {
        	docStoreHelper.updateById(elasticId, created, projectId, DocStoreInterface.REF);
        } catch (Exception e) {
            //Do nothing
        }

        branchJson = bJson;
        done();

        if (success && isTag && hasCommit) {
            pgh.setAsTag(created.get(Sjm.SYSMLID).getAsString());
        }

        bJson.add("createdRef", created);
        branchJson = bJson;
        return bJson;
    }

    public void done() {
        CommitUtil.sendJmsMsg(branchJson, TYPE_BRANCH, srcId, projectId);
        JsonObject created = JsonUtil.buildFromString(createdString);

        String body = String
            .format("Branch %s started by %s has finished at %s", created.get(Sjm.SYSMLID).getAsString(),
                JsonUtil.getOptString(created, Sjm.CREATOR), this.timer);
        String subject =
            String.format("Branch %s has finished at %s", created.get(Sjm.SYSMLID).getAsString(), this.timer);

        logger.info(subject);

        if (author != null) {
            try {

                String sender = EmsConfig.get("app.email.from");
                String smtpProtocol = EmsConfig.get("mail.protocol");
                String smtpHost = EmsConfig.get("mail.host");
                String smtpPort = EmsConfig.get("mail.port");
                String smtpUser = EmsConfig.get("mail.username");
                String smtpPass = EmsConfig.get("mail.password");

                if (smtpHost.isEmpty() || sender.isEmpty()) {
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
                } else {
                    if (prefix.equals("mail.smtp")) {
                        props.put(prefix + ".port", 25);
                    } else {
                        props.put(prefix + ".port", 587);
                    }
                }

                Authenticator auth = null;
                if (!smtpUser.isEmpty() && !smtpPass.isEmpty()) {
                    props.put(prefix + ".auth", "true");
                    auth = new Authenticator() {
                        @Override protected PasswordAuthentication getPasswordAuthentication() {
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
                emails.add(new InternetAddress(author));

                InternetAddress[] ias = emails.toArray(new InternetAddress[emails.size()]);
                msg.setRecipients(Message.RecipientType.TO, ias);

                Transport.send(msg);
            } catch (SendFailedException sfe) {
                logger.error("Could not send email: ", sfe);
            } catch (Exception e) {
                logger.error("Sending Email Exception: ", e);
            }

        }
    }

    public static void processNodesAndEdgesWithoutCommit(JsonArray elements, JsonArray artifacts,
        List<Map<String, Object>> nodeInserts, List<Map<String, Object>> artifactInserts,
        List<Map<String, Object>> edgeInserts, List<Map<String, Object>> childEdgeInserts) {

        List<Pair<String, String>> addEdges = new ArrayList<>();
        List<Pair<String, String>> viewEdges = new ArrayList<>();
        List<Pair<String, String>> childViewEdges = new ArrayList<>();
        List<String> uniqueEdge = new ArrayList<>();

        for (int i = 0; i < artifacts.size(); i++) {
            JsonObject a = artifacts.get(i).getAsJsonObject();
            Map<String, Object> artifact = new HashMap<>();
            if (a.has(Sjm.ELASTICID)) {
                artifact.put(Sjm.ELASTICID, a.get(Sjm.ELASTICID));
                artifact.put(Sjm.SYSMLID, a.get(Sjm.SYSMLID));
                artifact.put(LASTCOMMIT, a.get(Sjm.COMMITID));
                artifact.put(DELETED, false);
                artifactInserts.add(artifact);
            }
        }

        for (int i = 0; i < elements.size(); i++) {
            JsonObject e = elements.get(i).getAsJsonObject();
            Map<String, Object> node = new HashMap<>();
            int nodeType = CommitUtil.getNodeType(e).getValue();

            if (e.has(Sjm.ELASTICID)) {
                node.put(Sjm.ELASTICID, e.get(Sjm.ELASTICID).getAsString());
                node.put(Sjm.SYSMLID, e.get(Sjm.SYSMLID).getAsString());
                node.put(NODETYPE, nodeType);
                node.put(LASTCOMMIT, e.get(Sjm.COMMITID).getAsString());
                node.put(DELETED, false);
                nodeInserts.add(node);
            }

            if (e.has(Sjm.OWNERID) && !e.get(Sjm.OWNERID).isJsonNull() && !e.get(Sjm.OWNERID).getAsString().isEmpty()
                && e.has(Sjm.SYSMLID) && !e.get(Sjm.SYSMLID).isJsonNull() && !e.get(Sjm.SYSMLID).getAsString()
                .isEmpty()) {
                Pair<String, String> p = new Pair<>(e.get(Sjm.OWNERID).getAsString(), e.get(Sjm.SYSMLID).getAsString());
                addEdges.add(p);
            }

            String doc = JsonUtil.getOptString(e, Sjm.DOCUMENTATION);
            if (!doc.equals("")) {
                CommitUtil.processDocumentEdges(e.get(Sjm.SYSMLID).getAsString(), doc, viewEdges);
            }
            String type = JsonUtil.getOptString(e, Sjm.TYPE);
            if (type.equals("Slot") || type.equals("Property") || type.equals("Port")) {
                CommitUtil.processValueEdges(e, viewEdges);
            }
            if (e.has(Sjm.CONTENTS)) {
                JsonObject contents = JsonUtil.getOptObject(e, Sjm.CONTENTS);
                CommitUtil.processContentsJson(e.get(Sjm.SYSMLID).getAsString(), contents, viewEdges);
            } else if (e.has(Sjm.SPECIFICATION) && nodeType == GraphInterface.DbNodeTypes.INSTANCESPECIFICATION
                .getValue()) {
                JsonObject iss = JsonUtil.getOptObject(e, Sjm.SPECIFICATION);
                CommitUtil
                    .processInstanceSpecificationSpecificationJson(e.get(Sjm.SYSMLID).getAsString(), iss, viewEdges);
                CommitUtil.processContentsJson(e.get(Sjm.SYSMLID).getAsString(), iss, viewEdges);
            }
            if (nodeType == GraphInterface.DbNodeTypes.VIEW.getValue()
                || nodeType == GraphInterface.DbNodeTypes.DOCUMENT.getValue()) {
                JsonArray owned = JsonUtil.getOptArray(e, Sjm.OWNEDATTRIBUTEIDS);
                for (int j = 0; j < owned.size(); j++) {
                    Pair<String, String> p = new Pair<>(e.get(Sjm.SYSMLID).getAsString(), owned.get(j).getAsString());
                    childViewEdges.add(p);
                }
            }
            if (CommitUtil.isPartProperty(e)) {
                String typeid = JsonUtil.getOptString(e, Sjm.TYPEID);
                if (!typeid.isEmpty()) {
                    Pair<String, String> p = new Pair<>(e.get(Sjm.SYSMLID).getAsString(), typeid);
                    childViewEdges.add(p);
                }
            }
        }

        for (Pair<String, String> e : addEdges) {
            String edgeTest = e.first + e.second + GraphInterface.DbEdgeTypes.CONTAINMENT.getValue();
            if (!uniqueEdge.contains(edgeTest)) {
                Map<String, Object> edge = new HashMap<>();
                edge.put(PARENT, e.first);
                edge.put(CHILD, e.second);
                edge.put(EDGETYPE, GraphInterface.DbEdgeTypes.CONTAINMENT.getValue());
                edgeInserts.add(edge);
                uniqueEdge.add(edgeTest);
            }
        }

        for (Pair<String, String> e : viewEdges) {
            String edgeTest = e.first + e.second + GraphInterface.DbEdgeTypes.VIEW.getValue();
            if (!uniqueEdge.contains(edgeTest)) {
                Map<String, Object> edge = new HashMap<>();
                edge.put(PARENT, e.first);
                edge.put(CHILD, e.second);
                edge.put(EDGETYPE, GraphInterface.DbEdgeTypes.VIEW.getValue());
                childEdgeInserts.add(edge);
                uniqueEdge.add(edgeTest);
            }
        }

        for (Pair<String, String> e : childViewEdges) {
            String edgeTest = e.first + e.second + GraphInterface.DbEdgeTypes.CHILDVIEW.getValue();
            if (!uniqueEdge.contains(edgeTest)) {
                Map<String, Object> edge = new HashMap<>();
                edge.put(PARENT, e.first);
                edge.put(CHILD, e.second);
                edge.put(EDGETYPE, GraphInterface.DbEdgeTypes.CHILDVIEW.getValue());
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
                CommitUtil.updateNullEdges(nullParents, projectId);
            }
            pgh.cleanEdges();
        } catch (Exception e) {
            try {
                pgh.rollBackToSavepoint(sp);
                return false;
            } catch (SQLException se) {
            }
        } finally {
            pgh.close();
        }

        return true;
    }
}
