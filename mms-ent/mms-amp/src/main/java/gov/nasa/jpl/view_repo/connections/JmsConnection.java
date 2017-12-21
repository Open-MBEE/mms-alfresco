package gov.nasa.jpl.view_repo.connections;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
//import org.json.JSONArray;
import org.json.JSONException;
//import org.json.JSONObject;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.JSONObject;
import gov.nasa.jpl.view_repo.util.JSONArray;

/**
 *
 * @author cinyoung
 */
public class JmsConnection implements ConnectionInterface {
    private static Logger logger = Logger.getLogger(JmsConnection.class);
    private long sequenceId = 0;
    private String refId = null;
    private String projectId = null;

    private static Map<String, ConnectionInfo> connectionMap = null;

    protected static Map<String, ConnectionInfo> getConnectionMap() {
        if (Utils.isNullOrEmpty(connectionMap)) {
            connectionMap = new HashMap<String, ConnectionInfo>();
            initConnectionInfo(CommitUtil.TYPE_BRANCH);
            initConnectionInfo(CommitUtil.TYPE_DELTA);
            initConnectionInfo(CommitUtil.TYPE_MERGE);
        }
        return connectionMap;
    }


    public enum DestinationType {
        TOPIC, QUEUE
    }

    static class ConnectionInfo {
        public InitialContext ctx = null;
        public String ctxFactory = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
        public String connFactory = "ConnectionFactory";
        public String username = null;
        public String password = null;
        public String destination = "master";
        public String uri = "tcp://localhost:61616";
        public ConnectionFactory connectionFactory = null;
        public DestinationType destType = DestinationType.TOPIC;

        public ConnectionInfo() {
        	if (EmsConfig.get("jms.ctxfactory") != null) {
        		ctxFactory = EmsConfig.get("jms.ctxfactory");
        		System.out.println(ctxFactory);
        	}
        	if (EmsConfig.get("jms.connfactory") != null) {
        		connFactory = EmsConfig.get("jms.connfactory");
        		System.out.println(connFactory);
        	}
        	if (EmsConfig.get("jms.username") != null) {
        		username = EmsConfig.get("jms.username");
        	}
        	if (EmsConfig.get("jms.password") != null) {
        		password = EmsConfig.get("jms.password");
        	}
        	if (EmsConfig.get("jms.destination") != null) {
        		destination = EmsConfig.get("jms.destination");
        	}
        	if (EmsConfig.get("jms.uri") != null) {
        		uri = EmsConfig.get("jms.uri");
        	}
        }
    }


    protected boolean init(String eventType) {
        ConnectionInfo ci = getConnectionMap().get(eventType);
        if (ci == null)
            return false;

        System.setProperty("weblogic.security.SSL.ignoreHostnameVerification", "true");
        System.setProperty("jsse.enableSNIExtension", "false");
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, ci.ctxFactory);
        properties.put(Context.PROVIDER_URL, ci.uri);
        if (ci.username != null && ci.password != null) {
            properties.put(Context.SECURITY_PRINCIPAL, ci.username);
            properties.put(Context.SECURITY_CREDENTIALS, ci.password);
        }

        try {
            ci.ctx = new InitialContext(properties);
        } catch (NamingException ne) {
            ne.printStackTrace(System.err);
            return false;
        }

        try {
            ci.connectionFactory = (ConnectionFactory) ci.ctx.lookup(ci.connFactory);
        } catch (NamingException ne) {
            ne.printStackTrace(System.err);
            return false;
        }

        return true;
    }

    @Override
    /**
     * Changed method to synchronized to JMS message can be sent out with appropriate refId and projectIds
     * without race condition.
     */ public synchronized boolean publish(JSONObject json, String eventType, String refId, String projectId) {
        boolean result = false;
        try {
            json.put("sequence", sequenceId++);
            // don't do null check since null is applicable and want to reset
            this.refId = refId;
            this.projectId = projectId;
            result = publishMessage(json.toString(), eventType);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    protected static ConnectionInfo initConnectionInfo(String eventType) {
        ConnectionInfo ci = new ConnectionInfo();
        if (connectionMap == null) {
            connectionMap = new HashMap<>();
        }
        connectionMap.put(eventType, ci);
        return ci;
    }


    public boolean publishMessage(String msg, String eventType) {
        ConnectionInfo ci = getConnectionMap().get(eventType);

        if (ci.uri == null)
            return false;

        if (init(eventType) == false)
            return false;

        boolean status = true;
        try {
            // Create a Connection
            Connection connection = ci.connectionFactory.createConnection();
            connection.start();

            // Create a Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // lookup the destination
            Destination destination;
            try {
                destination = (Destination) ci.ctx.lookup(ci.destination);
            } catch (NameNotFoundException nnfe) {
                switch (ci.destType) {
                    case QUEUE:
                        destination = session.createQueue(ci.destination);
                        break;
                    case TOPIC:
                    default:
                        destination = session.createTopic(ci.destination);
                }
            }

            // Create a MessageProducer from the Session to the Topic or Queue
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // Create a message
            TextMessage message = session.createTextMessage(msg);
            if (refId != null) {
                message.setStringProperty("refId", refId);
            } else {
                message.setStringProperty("refId", "master");
            }
            if (projectId != null) {
                message.setStringProperty("projectId", projectId);
            }
            message.setLongProperty("MessageID", sequenceId++);
            message.setStringProperty("MessageSource", NodeUtil.getHostname());
            message.setStringProperty("MessageRecipient", "TMS");
            message.setStringProperty("MessageType", eventType.toUpperCase());

            // Tell the producer to send the message
            producer.send(message);

            // Clean up
            session.close();
            connection.close();
        } catch (Exception e) {
            logger.error("JMS exception caught, probably means JMS broker not up");
            status = false;
        }

        return status;
    }

    @Override public void setRefId(String refId) {
        this.refId = refId;
    }

    @Override public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override public JSONObject toJson() {
        JSONArray connections = new JSONArray();

        for (String eventType : getConnectionMap().keySet()) {
            ConnectionInfo ci = getConnectionMap().get(eventType);
            if (ci.uri.contains("localhost")) {
                ci.uri = ci.uri.replace("localhost", NodeUtil.getHostname());
                getConnectionMap().put(eventType, ci);
            }

            JSONObject connJson = new JSONObject();
            connJson.put("uri", ci.uri);
            connJson.put("connFactory", ci.connFactory);
            connJson.put("ctxFactory", ci.ctxFactory);
            connJson.put("password", ci.password);
            connJson.put("username", ci.username);
            connJson.put("destName", ci.destination);
            connJson.put("destType", ci.destType.toString());
            connJson.put("eventType", eventType);

            connections.put(connJson);
        }

        JSONObject json = new JSONObject();
        json.put("connections", connections);

        return json;
    }

    /**
     * Handle single and multiple connections embedded as connections array or not
     */
    @Override public void ingestJson(JSONObject json) {
        if (json.has("connections")) {
            JSONArray connections = json.getJSONArray("connections");
            for (int ii = 0; ii < connections.length(); ii++) {
                JSONObject connection = connections.getJSONObject(ii);
                ingestConnectionJson(connection);
            }
        } else {
            ingestConnectionJson(json);
        }
    }

    public void ingestConnectionJson(JSONObject json) {
        String eventType = null;
        if (json.has("eventType")) {
            eventType = json.isNull("eventType") ? null : json.getString("eventType");
        }
        if (eventType == null) {
            eventType = CommitUtil.TYPE_DELTA;
        }

        ConnectionInfo ci;
        if (getConnectionMap().containsKey(eventType)) {
            ci = getConnectionMap().get(eventType);
        } else {
            ci = new ConnectionInfo();
        }

        if (json.has("uri")) {
            ci.uri = json.isNull("uri") ? null : json.getString("uri");
        }
        if (json.has("connFactory")) {
            ci.connFactory = json.isNull("connFactory") ? null : json.getString("connFactory");
        }
        if (json.has("ctxFactory")) {
            ci.ctxFactory = json.isNull("ctxFactory") ? null : json.getString("ctxFactory");
        }
        if (json.has("password")) {
            ci.password = json.isNull("password") ? null : json.getString("password");
        }
        if (json.has("username")) {
            ci.username = json.isNull("username") ? null : json.getString("username");
        }
        if (json.has("destName")) {
            ci.destination = json.isNull("destName") ? null : json.getString("destName");
        }
        if (json.has("destType")) {
            if (json.isNull("destType")) {
                ci.destType = null;
            } else {
                String type = json.getString("destType");
                if (type.equalsIgnoreCase("topic")) {
                    ci.destType = DestinationType.TOPIC;
                } else if (type.equalsIgnoreCase("queue")) {
                    ci.destType = DestinationType.QUEUE;
                } else {
                    ci.destType = DestinationType.TOPIC;
                }
            }
        }

        getConnectionMap().put(eventType, ci);
    }

}
