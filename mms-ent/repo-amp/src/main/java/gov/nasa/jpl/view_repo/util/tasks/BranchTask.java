package gov.nasa.jpl.view_repo.util.tasks;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.GraphInterface;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

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

public class BranchTask implements Callable<JSONObject>, Serializable {

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
    private JSONObject branchJson = new JSONObject();

    private transient Timer timer;
    private transient ElasticHelper eh;
    private transient PostgresHelper pgh;

    public BranchTask(String projectId, String srcId, String createdString, String elasticId, Boolean isTag, String source, String commitId, String author) {
        this.projectId = projectId;
        this.elasticId = elasticId;
        this.source = source;
        this.commitId = commitId;
        this.isTag = isTag;
        this.srcId = srcId;
        this.createdString = createdString;
        this.author = author;
    }

    @Override
    public JSONObject call () {
        return createBranch();
    }

    private JSONObject createBranch() {

        JSONObject created = new JSONObject(createdString);
        this.timer = new Timer();

        branchJson.put("source", source);

        boolean hasCommit = (commitId != null && !commitId.isEmpty());
        boolean success = false;

        pgh = new PostgresHelper();
        pgh.setProject(projectId);
        pgh.setWorkspace(srcId);

        logger.info("Connected to postgres");

        try {
            eh = new ElasticHelper();
            pgh.createBranchFromWorkspace(created.getString(Sjm.SYSMLID), created.getString(Sjm.NAME), elasticId,
                commitId, isTag);

            logger.info("Created branch");

            if (hasCommit) {
                pgh.setWorkspace(created.getString(Sjm.SYSMLID));
                EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, srcId);
                JSONObject modelFromCommit = emsNodeUtil.getModelAtCommit(commitId);

                List<Map<String, String>> nodeInserts = new ArrayList<>();
                List<Map<String, String>> edgeInserts = new ArrayList<>();
                List<Map<String, String>> childEdgeInserts = new ArrayList<>();

                processNodesAndEdgesWithoutCommit(modelFromCommit.getJSONArray(Sjm.ELEMENTS), nodeInserts,
                    edgeInserts, childEdgeInserts);

                if (!nodeInserts.isEmpty() || !edgeInserts.isEmpty() || !childEdgeInserts.isEmpty()) {
                    if (!nodeInserts.isEmpty()) {
                        insertForBranchInPast(pgh, nodeInserts, "updates", projectId);
                    }
                    if (!edgeInserts.isEmpty()) {
                        insertForBranchInPast(pgh, edgeInserts, EDGES, projectId);
                    }
                    if (!childEdgeInserts.isEmpty()) {
                        insertForBranchInPast(pgh, childEdgeInserts, EDGES, projectId);
                    }
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
            done();

        } catch (Exception e) {
            logger.info("Branch creation failed");
            logger.info(String.format("%s", LogUtil.getStackTrace(e)));
            created.put("status", "failed");
        }

        try {
            eh.updateElement(elasticId, new JSONObject().put("doc", created), projectId);
        } catch (Exception e) {
            //Do nothing
        }

        if (success && isTag && hasCommit) {
            pgh.setAsTag(created.getString(Sjm.SYSMLID));
        }

        branchJson.put("createdRef", created);
        return branchJson;
    }

    public void done() {

        JSONObject created = new JSONObject(createdString);
        CommitUtil.sendJmsMsg(branchJson, TYPE_BRANCH, srcId, projectId);
        String body = String.format("Branch %s started by %s has finished at %s", created.getString(Sjm.SYSMLID), created.optString(Sjm.CREATOR), this.timer);
        String subject = String.format("Branch %s has finished at %s", created.getString(Sjm.SYSMLID), this.timer);

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
                emails.add(new InternetAddress(author));

                InternetAddress[] ias = emails.toArray(new InternetAddress[emails.size()]);
                msg.setRecipients(Message.RecipientType.TO, ias);

                Transport.send(msg);
            } catch (SendFailedException sfe) {
            } catch (Exception e) {
            }

        }
    }

    public static void processNodesAndEdgesWithoutCommit(JSONArray elements, List<Map<String, String>> nodeInserts,
        List<Map<String, String>> edgeInserts, List<Map<String, String>> childEdgeInserts) {

        List<Pair<String, String>> addEdges = new ArrayList<>();
        List<Pair<String, String>> viewEdges = new ArrayList<>();
        List<Pair<String, String>> childViewEdges = new ArrayList<>();
        List<String> uniqueEdge = new ArrayList<>();

        for (int i = 0; i < elements.length(); i++) {
            JSONObject e = elements.getJSONObject(i);
            Map<String, String> node = new HashMap<>();
            int nodeType = CommitUtil.getNodeType(e).getValue();

            if (e.has(Sjm.ELASTICID)) {
                node.put(Sjm.ELASTICID, e.getString(Sjm.ELASTICID));
                node.put(Sjm.SYSMLID, e.getString(Sjm.SYSMLID));
                node.put(NODETYPE, Integer.toString(nodeType));
                node.put(LASTCOMMIT, e.getString(Sjm.COMMITID));
                node.put(DELETED, "false");
                nodeInserts.add(node);
            }

            if (e.has(Sjm.OWNERID) && e.getString(Sjm.OWNERID) != null && e.getString(Sjm.SYSMLID) != null) {
                Pair<String, String> p = new Pair<>(e.getString(Sjm.OWNERID), e.getString(Sjm.SYSMLID));
                addEdges.add(p);
            }

            String doc = e.optString(Sjm.DOCUMENTATION);
            if (doc != null && !doc.equals("")) {
                CommitUtil.processDocumentEdges(e.getString(Sjm.SYSMLID), doc, viewEdges);
            }
            String type = e.optString(Sjm.TYPE);
            if (type.equals("Slot") || type.equals("Property") || type.equals("Port")) {
                CommitUtil.processValueEdges(e, viewEdges);
            }
            if (e.has(Sjm.CONTENTS)) {
                JSONObject contents = e.optJSONObject(Sjm.CONTENTS);
                if (contents != null) {
                    CommitUtil.processContentsJson(e.getString(Sjm.SYSMLID), contents, viewEdges);
                }
            } else if (e.has(Sjm.SPECIFICATION) && nodeType == GraphInterface.DbNodeTypes.INSTANCESPECIFICATION.getValue()) {
                JSONObject iss = e.optJSONObject(Sjm.SPECIFICATION);
                if (iss != null) {
                    CommitUtil.processInstanceSpecificationSpecificationJson(e.getString(Sjm.SYSMLID), iss, viewEdges);
                    CommitUtil.processContentsJson(e.getString(Sjm.SYSMLID), iss, viewEdges);
                }
            }
            if (nodeType == GraphInterface.DbNodeTypes.VIEW.getValue() || nodeType == GraphInterface.DbNodeTypes.DOCUMENT.getValue()) {
                JSONArray owned = e.optJSONArray(Sjm.OWNEDATTRIBUTEIDS);
                if (owned != null) {
                    for (int j = 0; j < owned.length(); j++) {
                        Pair<String, String> p = new Pair<>(e.getString(Sjm.SYSMLID), owned.getString(j));
                        childViewEdges.add(p);
                    }
                }
            }
            if (CommitUtil.isPartProperty(e)) {
                String typeid = e.optString(Sjm.TYPEID);
                if (typeid != null) {
                    Pair<String, String> p = new Pair<>(e.getString(Sjm.SYSMLID), typeid);
                    childViewEdges.add(p);
                }
            }
        }

        for (Pair<String, String> e : addEdges) {
            String edgeTest = e.first + e.second + GraphInterface.DbEdgeTypes.CONTAINMENT.getValue();
            if (!uniqueEdge.contains(edgeTest)) {
                Map<String, String> edge = new HashMap<>();
                edge.put(PARENT, e.first);
                edge.put(CHILD, e.second);
                edge.put(EDGETYPE, Integer.toString(GraphInterface.DbEdgeTypes.CONTAINMENT.getValue()));
                edgeInserts.add(edge);
                uniqueEdge.add(edgeTest);
            }
        }

        for (Pair<String, String> e : viewEdges) {
            String edgeTest = e.first + e.second + GraphInterface.DbEdgeTypes.VIEW.getValue();
            if (!uniqueEdge.contains(edgeTest)) {
                Map<String, String> edge = new HashMap<>();
                edge.put(PARENT, e.first);
                edge.put(CHILD, e.second);
                edge.put(EDGETYPE, Integer.toString(GraphInterface.DbEdgeTypes.VIEW.getValue()));
                childEdgeInserts.add(edge);
                uniqueEdge.add(edgeTest);
            }
        }

        for (Pair<String, String> e : childViewEdges) {
            String edgeTest = e.first + e.second + GraphInterface.DbEdgeTypes.CHILDVIEW.getValue();
            if (!uniqueEdge.contains(edgeTest)) {
                Map<String, String> edge = new HashMap<>();
                edge.put(PARENT, e.first);
                edge.put(CHILD, e.second);
                edge.put(EDGETYPE, Integer.toString(GraphInterface.DbEdgeTypes.CHILDVIEW.getValue()));
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
