package gov.nasa.jpl.view_repo.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;
import javax.xml.bind.DatatypeConverter;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptVersion;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.node.db.DbNodeServiceImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.CompareUtils.GenericComparator;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.MethodCall;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.db.Node;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.EmsScriptNode.EmsVersion;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;

public class NodeUtil {

    public enum SearchType {
        DOCUMENTATION("@sysml\\:documentation:\""), NAME("@sysml\\:name:\""), CM_NAME("@cm\\:name:\""), ID(
                        "@sysml\\:id:\""), STRING("@sysml\\:string:\""), BODY("@sysml\\:body:\""), CHECKSUM(
                                        "@view\\:cs:\""), WORKSPACE("@ems\\:workspace:\""), WORKSPACE_NAME(
                                                        "@ems\\:workspace_name:\""), OWNER("@ems\\:owner:\""), ASPECT(
                                                                        "ASPECT:\""), TYPE("TYPE:\"");

        public String prefix;

        SearchType(String prefix) {
            this.prefix = prefix;
        }
    }

    static Logger logger = Logger.getLogger(NodeUtil.class);

    /* static flags and constants */

    protected static String txMutex = "";
    protected static boolean beenInsideTransaction = false;
    protected static Map<Long, Boolean> beenInsideTransactionMap = new LinkedHashMap<Long, Boolean>();

    public static synchronized boolean hasBeenInsideTransaction() {
        Boolean b = beenInsideTransactionMap.get(Thread.currentThread().getId());
        if (b != null)
            return b;
        return beenInsideTransaction;
    }

    public static synchronized void setBeenInsideTransaction(boolean b) {
        beenInsideTransaction = b;
        beenInsideTransactionMap.put(Thread.currentThread().getId(), b);
    }

    protected static boolean beenOutsideTransaction = false;
    protected static Map<Long, Boolean> beenOutsideTransactionMap = new LinkedHashMap<Long, Boolean>();

    public static synchronized boolean hasBeenOutsideTransaction() {
        Boolean b = beenOutsideTransactionMap.get(Thread.currentThread().getId());
        if (b != null)
            return b;
        return beenOutsideTransaction;
    }

    public static synchronized void setBeenOutsideTransaction(boolean b) {
        beenOutsideTransaction = b;
        beenOutsideTransactionMap.put(Thread.currentThread().getId(), b);
    }

    protected static boolean insideTransactionNow = false;
    protected static Map<Long, Boolean> insideTransactionNowMap = new LinkedHashMap<Long, Boolean>();
    protected static Map<Long, UserTransaction> transactionMap =
                    Collections.synchronizedMap(new LinkedHashMap<Long, UserTransaction>());

    public static synchronized boolean isInsideTransactionNow() {
        Boolean b = insideTransactionNowMap.get(Thread.currentThread().getId());
        if (b != null)
            return b;
        return insideTransactionNow;
    }

    public static UserTransaction getTransaction() {
        return transactionMap.get(Thread.currentThread().getId());
    }

    public static UserTransaction createTransaction() {
        UserTransaction trx = services.getTransactionService().getNonPropagatingUserTransaction();
        transactionMap.put(Thread.currentThread().getId(), trx);
        return trx;
    }

    public static synchronized void setInsideTransactionNow(boolean b) {
        insideTransactionNow = b;
        insideTransactionNowMap.put(Thread.currentThread().getId(), b);
    }

    protected static Map<Long, StackTraceElement[]> insideTransactionStrackTrace =
                    new LinkedHashMap<>();
    protected static Map<Long, StackTraceElement[]> outsideTransactionStrackTrace =
                    new LinkedHashMap<>();

    public static void setInsideTransactionStackTrace() {
        insideTransactionStrackTrace.put(Thread.currentThread().getId(), Thread.currentThread().getStackTrace());
    }

    public static void setOutsideTransactionStackTrace() {
        outsideTransactionStrackTrace.put(Thread.currentThread().getId(), Thread.currentThread().getStackTrace());
    }


    public static boolean doFullCaching = true;
    public static boolean doSimpleCaching = true;
    public static boolean doNodeAtTimeCaching = true;
    public static boolean doHeisenCheck = true;
    public static boolean doVersionCaching = false;
    public static boolean activeVersionCaching = true;
    public static boolean doVersionHistoryCaching = true;
    public static boolean doJsonCaching = false;
    public static boolean doJsonDeepCaching = false;
    public static boolean doJsonStringCaching = false;
    public static boolean doPropertyCaching = true;
    public static boolean doGraphDb = true;
    public static boolean doPostProcessQualified = false;
    public static boolean doAutoBuildGraphDb = false;
    public static boolean skipQualified = false;

    public static boolean addEmptyEntriesToFullCache = false; // this was broken
                                                              // last tried
    public static boolean skipGetNodeRefAtTime = true;
    public static boolean skipSvgToPng = false;
    public static boolean skipWorkspacePermissionCheck = false;
    public static boolean doOptimisticJustFirst = true;

    public static boolean doorsSync = false;

    // global flag that is enabled once heisenbug is seen, so it will email
    // admins the first time heisenbug is seen
    public static boolean heisenbugSeen = false;

    /**
     * A cache of alfresco nodes stored as a map from cm:name to node for the master branch only.
     */
    public static Map<String, NodeRef> simpleCache = Collections.synchronizedMap(new HashMap<String, NodeRef>());

    /**
     * A cache of alfresco nodes stored as a map from NodeRef and time to node as determined by
     */
    public static Map<NodeRef, Map<Long, Object>> nodeAtTimeCache =
                    Collections.synchronizedMap(new HashMap<NodeRef, Map<Long, Object>>());

    /**
     * A cache of alfresco nodes stored as a map from query parameters to a set of nodes. TODO --
     * list the parameters here
     */
    public static Map<String, Map<String, Map<String, Map<Boolean, Map<Long, Map<Boolean, Map<Boolean, Map<Boolean, Map<String, ArrayList<NodeRef>>>>>>>>>> elementCache =
                    Collections.synchronizedMap(
                                    new HashMap<String, Map<String, Map<String, Map<Boolean, Map<Long, Map<Boolean, Map<Boolean, Map<Boolean, Map<String, ArrayList<NodeRef>>>>>>>>>>());

    /**
     * A cache of alfresco properties of nodes
     */
    public static Map<NodeRef, Map<String, Object>> propertyCache =
                    Collections.synchronizedMap(new HashMap<NodeRef, Map<String, Object>>());



    // public static HashMap<String, String> versionLabelCache =
    // new HashMap<String, String>();
    public static Map<String, EmsVersion> versionCache = Collections.synchronizedMap(new HashMap<String, EmsVersion>());
    public static Map<NodeRef, NodeRef> frozenNodeCache = Collections.synchronizedMap(new HashMap<NodeRef, NodeRef>());

    // public static Map< NodeRef, Collection< Version > > versionHistoryCache =
    // Collections.synchronizedMap( new HashMap< NodeRef, Collection< Version >
    // >() );
    public static Map<NodeRef, VersionHistory> versionHistoryCache =
                    Collections.synchronizedMap(new HashMap<NodeRef, VersionHistory>());

    // Set< String > filter, boolean isExprOrProp,Date dateTime, boolean
    // isIncludeQualified
    public static Map<String, Map<Long, Map<Boolean, Map<Set<String>, Map<String, JSONObject>>>>> jsonDeepCache =
                    Collections.synchronizedMap(
                                    new HashMap<String, Map<Long, Map<Boolean, Map<Set<String>, Map<String, JSONObject>>>>>());
    public static Map<String, Map<Long, JSONObject>> jsonCache =
                    Collections.synchronizedMap(new HashMap<String, Map<Long, JSONObject>>());

    // The json string cache maps JSONObjects to an integer (date in millis) to
    // a string rendering of itself paired with the date.
    public static Map<JSONObject, Map<Integer, Pair<Date, String>>> jsonStringCache =
                    Collections.synchronizedMap(new HashMap<JSONObject, Map<Integer, Pair<Date, String>>>());

    // REVIEW -- TODO -- Should we try and cache the toString() output of the
    // json, too?
    // REVIEW -- TODO -- This would mean we'd have to concatenate the json
    // REVIEW -- TODO -- strings ourselves instead of just one big toString()
    // REVIEW -- TODO -- on the collection as done currently.

    // Set the flag to time events that occur during a model post using the
    // timers
    // below
    public static boolean timeEvents = false;
    private static Timer timer = null;
    private static Timer timerByType = null;
    private static Timer timerLucene = null;

    public static ServiceRegistry services = null;
    public static Repository repository = null;

    // needed for Lucene search
    public static StoreRef SEARCH_STORE = null;
    // new StoreRef( StoreRef.PROTOCOL_WORKSPACE, "SpacesStore" );

    public static Object NULL_OBJECT = new Object() {
        @Override
        public String toString() {
            return "NULL_OBJECT";
        }
    };

    /**
     * clear or create the cache for correcting bad node refs (that refer to wrong versions)
     */
    public static void initPropertyCache() {
        propertyCache.clear();
    }

    public static Object propertyCachePut(NodeRef nodeRef, String propertyName, Object value) {
        if (!doPropertyCaching || nodeRef == null || Utils.isNullOrEmpty(propertyName)) {
            return null;
        }
        if (logger.isDebugEnabled())
            logger.debug("propertyCachePut(" + nodeRef + ", " + propertyName + ", " + value + ")");
        if (value == null)
            value = NULL_OBJECT;
        return Utils.put(propertyCache, nodeRef, propertyName, value);
    }

    public static Object propertyCacheGet(NodeRef nodeRef, String propertyName) {
        if (!doPropertyCaching || nodeRef == null || Utils.isNullOrEmpty(propertyName)) {
            return null;
        }
        Object o = Utils.get(propertyCache, nodeRef, propertyName);
        if (logger.isTraceEnabled())
            logger.trace("propertyCachePut(" + nodeRef + ", " + propertyName + ", " + o + ")");
        return o;
    }

    public static NodeRef getCurrentNodeRefFromCache(NodeRef maybeFrozenNodeRef) {
        NodeRef ref = frozenNodeCache.get(maybeFrozenNodeRef);
        if (ref != null)
            return ref;
        return null;
    }

    public static JSONObject stripJsonMetadata(JSONObject json) {
        if (json == null)
            return null;
        if (Debug.isOn())
            logger.debug("stripJsonMetadata -> " + json);
        json.remove("jsonString");
        json.remove("jsonString4");
        if (Debug.isOn())
            logger.debug("stripJsonMetadata -> " + json);
        return json;
    }

    protected static List<String> dontFilterList = Utils.newList("read", Acm.JSON_ID, "id", "creator",
                    Acm.JSON_LAST_MODIFIED, Acm.JSON_SPECIALIZATION);
    protected static Set<String> dontFilterOut = Collections.synchronizedSet(new HashSet<String>(dontFilterList));

    /**
     * Use the cached json string if appropriate; else call json.toString().
     *
     * @param json
     * @param numSpacesToIndent
     * @return
     * @throws JSONException
     */
    public static String jsonToString(JSONObject json, int numSpacesToIndent) throws JSONException {
        if (json == null)
            return null;
        String s = null;
        // If we aren't string caching, or if the string(s) are not cached in
        // the json, then call toString() the old-fashioned way.
        if (!doJsonStringCaching || !json.has("jsonString4")) {
            stripJsonMetadata(json);
            if (numSpacesToIndent < 0) {
                s = json.toString();
                if (Debug.isOn())
                    logger.debug("jsonToString( json, " + numSpacesToIndent + " ) = json.toString() = " + s);
                return s;
            }
            s = json.toString(numSpacesToIndent);
            if (Debug.isOn())
                logger.debug("jsonToString( json, " + numSpacesToIndent + " ) = json.toString( " + numSpacesToIndent
                                + " ) = " + s);
            return s;
        }
        // Get the cached json string with no newlines.
        if (numSpacesToIndent < 0) {
            if (json.has("jsonString")) {
                s = json.getString("jsonString");
                if (Debug.isOn())
                    logger.debug("jsonToString( json, " + numSpacesToIndent
                                    + " ) = json.getSString( \"jsonString\" ) = " + s);
                return s;
            }
            // TODO -- Warning! shouldn't get here!
            json = stripJsonMetadata(clone(json));
            s = json.toString();
            logger.warn("BAD! jsonToString( json, " + numSpacesToIndent + " ) = json.toString() = " + s);
            return s;
        }
        // Get the cached json string with newlines and indentation of four
        // spaces, and replace the indentation with the specified
        // numSpacesToIndent.
        if (json.has("jsonString4")) {
            String jsonString4 = json.getString("jsonString4");
            if (numSpacesToIndent == 4) {
                if (Debug.isOn())
                    logger.debug("jsonToString( json, " + numSpacesToIndent + " ) = jsonString4 = " + jsonString4);
                return jsonString4;
            }
            s = jsonString4.replaceAll("    ", Utils.repeat(" ", numSpacesToIndent));
            if (Debug.isOn())
                logger.debug("jsonToString( json, " + numSpacesToIndent
                                + " ) = jsonString4.replaceAll(\"    \", Utils.repeat( \" \", " + numSpacesToIndent
                                + " ) ) = " + s);
            return s;
        }
        // TODO -- Warning! shouldn't get here!
        json = stripJsonMetadata(clone(json));
        s = json.toString(numSpacesToIndent);
        logger.warn("BAD! jsonToString( json, " + numSpacesToIndent + " ) = json.toString( " + numSpacesToIndent
                        + " ) = " + s);
        return s;
    }

    public static class CachedJsonObject extends JSONObject {
        static String replacement = "$%%$";
        static String replacementWithQuotes = "\"$%%$\"";
        static int replacementWithQuotesLength = replacementWithQuotes.length();

        protected boolean plain = false;

        public CachedJsonObject() {
            super();
        }

        public CachedJsonObject(String s) throws JSONException {
            super(s);
        }

        @Override
        public String toString() {
            try {
                if (has("jsonString")) {
                    if (Debug.isOn())
                        logger.debug("has jsonString: " + getString("jsonString"));
                    return getString("jsonString");
                }
                if (Debug.isOn())
                    logger.debug("no jsonString: " + super.toString());

                return nonCachedToString(-1);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public String toString(int n) {
            try {
                if (has("jsonString4")) {
                    if (Debug.isOn())
                        logger.debug("has jsonString4: " + jsonToString(this, n));
                    // need to replace spaces for proper indentation
                    return jsonToString(this, n);
                }
                if (Debug.isOn())
                    logger.debug("no jsonString4: " + super.toString(n));

                return nonCachedToString(n);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Create json string for non-elements whose json is not cached but whose children may
         * include cached elements.
         *
         * @param numSpaces
         * @return
         */
        private String nonCachedToString(int numSpaces) {
            // Look for entries with CachedObjects or arrays of
            // CachedJsonObjects.
            LinkedHashMap<String, String> newEntries = new LinkedHashMap<>();
            JSONObject oldEntries = newJsonObject();
            // Temporarily replace CachedJsonObjects with fixed strings in order
            // to generate a minimal json string and later substitute back the
            // cached strings.
            for (Object k : keySet().toArray()) {
                if (!(k instanceof String))
                    continue;
                String key = (String) k;
                replaceJsonArrays(key, numSpaces, newEntries, oldEntries);
            }

            // Generate the json string with temporary replacements.
            String s = null;
            if (numSpaces < 0)
                s = super.toString();
            else
                s = super.toString(numSpaces);

            // Now substitute back in the cached json strings using a
            // StringBuffer.
            int len = newEntries.size();
            if (len > 0) {
                StringBuffer sb = new StringBuffer(s);
                String[] keysNew = new String[len];
                newEntries.keySet().toArray(keysNew);
                int pos = s.length() - replacementWithQuotesLength;

                // Loop through replacements.
                for (int i = len - 1; i >= 0; --i) {
                    String keyn = keysNew[i];
                    int pos1 = s.lastIndexOf(replacementWithQuotes, pos);
                    pos = s.lastIndexOf("\"" + keyn + "\"", pos1);
                    if (pos >= 0 && pos1 > 0) {
                        sb.replace(pos1, pos1 + replacementWithQuotesLength, newEntries.get(keyn));
                    }
                }
                s = sb.toString();

                // Put replaced json elements back.
                for (Object k : oldEntries.keySet().toArray()) {
                    if (!(k instanceof String))
                        continue;
                    String key = (String) k;
                    put(key, oldEntries.get(key));
                }
            }
            return s;
        }

        /**
         * Temporarily replace CachedJsonObjects with fixed strings in order to generate a minimal
         * json string and later substitute back the cached strings.
         *
         * @param key
         * @param numSpaces
         * @param newEntries
         * @param oldEntries
         */
        public void replaceJsonArrays(String key, int numSpaces, LinkedHashMap<String, String> newEntries,
                        JSONObject oldEntries) {
            JSONArray jarr = optJSONArray(key);
            if (jarr != null && jarr.length() > 0) {
                Object val = jarr.get(0);
                if (val instanceof CachedJsonObject) {
                    // Replace and build array string.
                    StringBuffer sb = new StringBuffer("[");
                    for (int i = 0; i < jarr.length(); ++i) {
                        val = jarr.get(i);
                        if (i > 0)
                            sb.append(", ");
                        if (numSpaces < 0) {
                            sb.append(val.toString());
                        } else {
                            if (val instanceof JSONObject) {
                                sb.append(((JSONObject) val).toString(numSpaces));
                            } else if (val instanceof JSONArray) {
                                sb.append(((JSONArray) val).toString(numSpaces));
                            } else {
                                sb.append(val.toString());
                            }
                        }
                    }
                    sb.append("]");
                    newEntries.put(key, sb.toString());
                    oldEntries.put(key, jarr);
                    put(key, replacement);
                }
            } else {
                Object o = get(key);
                if (o instanceof CachedJsonObject) {
                    CachedJsonObject cjo = (CachedJsonObject) o;
                    newEntries.put(key, cjo.toString(numSpaces));
                    oldEntries.put(key, cjo);
                    put(key, replacement);
                }
            }
        }
    }

    public static JSONObject newJsonObject() {
        if (!doJsonStringCaching)
            return new JSONObject();
        JSONObject newJson = new CachedJsonObject();
        return newJson;
    }

    public static JSONObject clone(JSONObject json) {
        if (json == null)
            return null;
        JSONObject newJson = newJsonObject();

        Iterator keys = json.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value;
            try {
                value = json.get(key);
                if (key.equals(Acm.JSON_SPECIALIZATION) && value instanceof JSONObject) {
                    value = clone((JSONObject) value);
                }
                newJson.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return newJson;
    }

    public static Repository getRepository() {
        return repository;
        // return (Repository)getApplicationContext().getBean(
        // "repositoryHelper" );
    }

    public static void setRepository(Repository repositoryHelper) {
        NodeUtil.repository = repositoryHelper;
    }

    public static ServiceRegistry getServices() {
        return getServiceRegistry();
    }

    public static void setServices(ServiceRegistry services) {
        NodeUtil.services = services;
    }

    public static ServiceRegistry getServiceRegistry() {
        return services;
        // if ( services == null ) {
        // services = (ServiceRegistry)getApplicationContext().getBean(
        // "ServiceRegistry" );
        // }
        // return services;
    }

    public static int compare(Version v1, Version v2) {
        if (v1 == v2)
            return 0;
        if (v1 == null)
            return -1;
        if (v2 == null)
            return 1;
        Date d1 = v1.getFrozenModifiedDate();
        Date d2 = v2.getFrozenModifiedDate();
        return d1.compareTo(d2);
    }


    /**
     * Given a long-form QName, this method uses the namespace service to create a short-form QName
     * string.
     * <p>
     * Copied from {@link ScriptNode#getShortQName(QName)}.
     *
     * @param longQName
     * @return the short form of the QName string, e.g. "cm:content"
     */
    public static String getShortQName(QName longQName) {
        return longQName.toPrefixString(getServices().getNamespaceService());
    }

    /**
     * Helper to create a QName from either a fully qualified or short-name QName string
     * <P>
     *
     * @param s Fully qualified or short-name QName string
     *
     * @param services ServiceRegistry for getting the service to resolve the name space
     * @return QName
     */
    public static QName createQName(String s, ServiceRegistry services) {
        if (s == null)
            return null;
        if (Acm.getJSON2ACM().keySet().contains(s)) {
            String possibleString = Acm.getACM2JSON().get(s);
            // Bad mapping, ie type, just use the original string:
            if (possibleString != null) {
                s = possibleString;
            }
        }
        QName qname;
        if (s.indexOf("{") != -1) {
            qname = QName.createQName(s);
        } else {
            if (services == null)
                services = getServices();
            qname = QName.createQName(s, getServices().getNamespaceService());
        }
        return qname;
    }


    public static EmsScriptNode getCompanyHome(ServiceRegistry services) {
        EmsScriptNode companyHome = null;
        if (services == null)
            services = getServiceRegistry();
        if (services == null || services.getNodeLocatorService() == null) {
            if (Debug.isOn())
                System.out.println("getCompanyHome() failed, no services or no nodeLocatorService: " + services);
        }
        NodeRef companyHomeNodeRef = services.getNodeLocatorService().getNode("companyhome", null, null);
        if (companyHomeNodeRef != null) {
            companyHome = new EmsScriptNode(companyHomeNodeRef, services);
        }
        return companyHome;
    }

    public static int compareVersions(NodeRef ref1, NodeRef ref2) {
        Date d1 = getLastModified(ref1);
        Date d2 = getLastModified(ref2);
        return CompareUtils.compare(d1, d2);
        // VersionService vs = getServices().getVersionService();
        // if ( vs == null ) {
        // //TODO -- BAD!
        // }
        // NodeService ns = getServices().getNodeService();
        // ns.getProperty( ref1, QName.createQName( "cm:lastModified" ) );
        // //vs.createVersion( nodeRef, versionProperties, versionChildren );
    }

    public static Date getLastModified(NodeRef ref) {
        try {
            // QName typeQName = createQName( Acm.ACM_LAST_MODIFIED );
            Date date = (Date) // services.getNodeService().getProperty( ref,
                               // typeQName );
            NodeUtil.getNodeProperty(ref, Acm.ACM_LAST_MODIFIED, services, true, false);
            return date;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    public static Object getNodeProperty(ScriptNode node, Object o, ServiceRegistry services,
                    boolean useFoundationalApi, boolean cacheOkay) {
        return getNodeProperty(node.getNodeRef(), o, services, useFoundationalApi, cacheOkay);
    }

    /**
     * Gets a specified property of a specified node. Don't use the cache (cacheOkay should be
     * false) for alfresco-managed properties like "modified" and "modifier." Handling
     * alfresco-managed properties here might require a string compare that may be expensive. Thus,
     * it is the caller's responsibility to make sure the cache is not used for these.
     *
     * @param node
     * @param key the name of the property, e.g., "cm:name"
     * @param services
     * @param cacheOkay whether to look in the cache and cache a new value, assuming the cache is
     *        turned on.
     * @return
     */
    public static Object getNodeProperty(NodeRef node, Object key, ServiceRegistry services, boolean useFoundationalApi,
                    boolean cacheOkay) {
        if (node == null || key == null)
            return null;

        boolean oIsString = key instanceof String;
        String keyStr = oIsString ? (String) key : NodeUtil.getShortQName((QName) key);
        if (keyStr.isEmpty()) {
            if (logger.isTraceEnabled())
                logger.trace("getNodeProperty(" + node + ", " + key + ", cacheOkay=" + cacheOkay
                                + ") = null.  No Key!");
            return null;
        }

        // Check cache
        if (NodeUtil.doPropertyCaching && cacheOkay) {
            Object result = NodeUtil.propertyCacheGet(node, keyStr);
            if (result != null) { // null means cache miss
                if (result == NodeUtil.NULL_OBJECT) {
                    if (logger.isTraceEnabled())
                        logger.trace("getNodeProperty(" + node + ", " + key + ", cacheOkay=" + cacheOkay + ") = "
                                        + null);
                    return null;
                }
                if (logger.isDebugEnabled()) {
                    if (logger.isTraceEnabled())
                        logger.trace("^ cache hit!  getNodeProperty(" + node + ", " + key + ", cacheOkay=" + cacheOkay
                                        + ") = " + result);
                    QName qName = oIsString ? NodeUtil.createQName(keyStr, services) : (QName) key;
                    Object result2 = services.getNodeService().getProperty(node, qName);
                    if (result == result2 || (result != null && result2 != null && result.equals(result2))) {
                        // cool
                        if (logger.isTraceEnabled())
                            logger.trace("good result = " + result + ", result2 = " + result2);
                    } else {
                        logger.warn("\nbad result = " + result + ", result2 = " + result2 + "\n");
                    }
                    if (logger.isTraceEnabled())
                        logger.trace("getNodeProperty(" + node + ", " + key + ", cacheOkay=" + cacheOkay + ") = "
                                        + result);
                }
                return result;
            }
        }

        // Not found in cache -- get normally
        QName qName = oIsString ? NodeUtil.createQName(keyStr, services) : (QName) key;
        Object result;
        if (useFoundationalApi) {
            if (services == null)
                services = NodeUtil.getServices();
            result = services.getNodeService().getProperty(node, qName);
            if (logger.isTraceEnabled())
                logger.trace("^ cache miss!  getNodeProperty(" + node + ", " + key + ", cacheOkay=" + cacheOkay + ") = "
                                + result);
        } else {
            ScriptNode sNode = new ScriptNode(node, services);
            result = sNode.getProperties().get(keyStr);
        }
        if (NodeUtil.doPropertyCaching && cacheOkay) {
            NodeUtil.propertyCachePut(node, keyStr, result);
        }
        if (logger.isTraceEnabled())
            logger.trace("getNodeProperty(" + node + ", " + key + ", cacheOkay=" + cacheOkay + ") = " + result);
        return result;
    }

    /**
     * Append onto the response for logging purposes
     *
     * @param msg Message to be appened to response TODO: fix logger for EmsScriptNode
     */
    public static void log(String msg, StringBuffer response) {
        // if (response != null) {
        // response.append(msg + "\n");
        // }
    }

    public static NodeRef getNodeRefFromNodeId(String store, String id) {
        List<NodeRef> nodeRefs = NodeRef.getNodeRefs(store + id);
        if (nodeRefs.size() > 0) {
            NodeRef ref = nodeRefs.get(0);
            if (ref != null) {
                EmsScriptNode node = new EmsScriptNode(ref, services);
                if (node.scriptNodeExists()) {
                    return ref;
                }
            }
        }
        return null;
    }

    public static boolean exists(EmsScriptNode node) {
        return exists(node, false);
    }

    public static boolean exists(EmsScriptNode node, boolean includeDeleted) {
        if (node == null)
            return false;
        return node.exists(includeDeleted);
    }

    public static boolean exists(NodeRef ref) {
        return exists(ref, false);
    }

    public static boolean exists(NodeRef ref, boolean includeDeleted) {
        if (ref == null)
            return false;
        EmsScriptNode node = new EmsScriptNode(ref, getServices());
        return node.exists(includeDeleted);
    }

    public static String getUserName() {
        String userName = AuthenticationUtil.getRunAsUser();
        return userName;
    }

    public static EmsScriptNode getUserHomeFolder(String userName, boolean createIfNotFound) {
        NodeRef homeFolderNode = null;
        EmsScriptNode homeFolderScriptNode = null;
        PersonService personService = getServices().getPersonService();
        NodeService nodeService = getServices().getNodeService();
        NodeRef personNode = personService.getPerson(userName);
        homeFolderNode = (NodeRef) getNodeProperty(personNode, ContentModel.PROP_HOMEFOLDER, getServices(), true,
                            true);
        if (homeFolderNode == null || !exists(homeFolderNode)) {
            EmsScriptNode homes = getCompanyHome(getServices());
            if (homes != null) {
                homes = homes.childByNamePath("/User Homes");
            }
            if (createIfNotFound && homes != null && homes.exists()) {
                homeFolderScriptNode = homes.createFolder(userName);
                if (homeFolderScriptNode != null)
                    homeFolderScriptNode.getOrSetCachedVersion();
                homes.getOrSetCachedVersion();
            } else {
                Debug.error("Error! No user homes folder!");
            }
        }
        if (!exists(homeFolderScriptNode) && exists(homeFolderNode)) {
            homeFolderScriptNode = new EmsScriptNode(homeFolderNode, getServices());
        }
        return homeFolderScriptNode;
    }

    /**
     * Returns a list of all the groups the passed user belongs to. Note, there is no java interface
     * for this, so this code is based on what the javascript interface does.
     *
     * See: https://svn.alfresco.com/repos/alfresco-open-mirror/alfresco/HEAD/root
     * /projects/repository/source/java/org/alfresco/repo/jscript/People.java
     *
     * @param user
     * @return
     */
    public static List<String> getUserGroups(String user) {

        List<String> authorityNames = new ArrayList<>();

        AuthorityService aService = services.getAuthorityService();
        Set<String> authorities = aService.getContainingAuthoritiesInZone(AuthorityType.GROUP, user, null, null, 1000);
        for (String authority : authorities) {
            NodeRef group = aService.getAuthorityNodeRef(authority);
            if (group != null) {
                authorityNames.add(authority);
            }
        }

        return authorityNames;
    }

    public static String getName(NodeRef ref) {
        if (ref == null)
            return null;
        EmsScriptNode node = new EmsScriptNode(ref, getServices());
        return node.getName();
    }

    /**
     * Updates or creates a artifact with the passed name/type in the specified site name/workspace
     * with the specified content.
     *
     * Only updates the artifact if found if updateIfFound is true.
     *
     * @param name
     * @param type
     * @param base64content
     * @param dateTime
     * @param response
     * @param status
     * @return
     */
    public static EmsScriptNode updateOrCreateArtifact(String name, String type, String base64content,
                    String strContent, String orgId, String projectId, String refId,
                    Date dateTime, StringBuffer response, Status status, boolean ignoreName) {

        EmsScriptNode artifactNode;
        String myType = Utils.isNullOrEmpty(type) ? "svg" : type;
        String finalType = myType.startsWith(".") ? myType.substring(1) : myType;
        String artifactId = name + "." + finalType;

        byte[] content = (base64content == null) ? null : DatatypeConverter.parseBase64Binary(base64content);

        if (content == null && strContent != null) {
            content = strContent.getBytes(Charset.forName("UTF-8"));
        }

        long cs = EmsScriptNode.getChecksum(content);

        // see if image already exists by looking up by checksum
        //ArrayList<NodeRef> refs = findNodeRefsByType("" + cs, SearchType.CHECKSUM.prefix, false, workspace, dateTime,
         //               false, false, services, false);

        //List<EmsScriptNode> nodeList = EmsScriptNode.toEmsScriptNodeList(refs, services, response, status);

        EmsScriptNode matchingNode = null;

        //if (nodeList != null && nodeList.size() > 0) {
        //    matchingNode = nodeList.iterator().next();
        //}

        // No need to update if the checksum and name match (even if it is in a
        // parent branch):
        if (matchingNode != null && (ignoreName || matchingNode.getSysmlId().equals(artifactId))) {
            return matchingNode;
        }

        EmsScriptNode targetSiteNode = EmsScriptNode.getSiteNode(orgId);

        // find site; it must exist!
        if (targetSiteNode == null || !targetSiteNode.exists()) {
            Debug.err("Can't find node for site: " + orgId + "!\n");
            return null;
        }

        // find or create subfolder
        EmsScriptNode subfolder = targetSiteNode.childByNamePath("/" + projectId +  "/refs/" + refId);
        if (subfolder == null || !subfolder.exists()) {
            return null;
        }

        // find or create node:
        artifactNode = subfolder.childByNamePath("/" + artifactId);

        // Node wasnt found, so create one:
        if (artifactNode == null) {
            artifactNode = subfolder.createNode(artifactId, "cm:content");
            subfolder.getOrSetCachedVersion();
        }

        if (artifactNode == null || !artifactNode.exists()) {
            Debug.err("Failed to create new artifact " + artifactId + "!\n");
            return null;
        }

        artifactNode.makeSureNodeRefIsNotFrozen();
        if (!artifactNode.hasAspect("cm:versionable")) {
            artifactNode.addAspect("cm:versionable");
        }
        if (!artifactNode.hasAspect("cm:indexControl")) {
            artifactNode.addAspect("cm:indexControl");
        }
        if (!artifactNode.hasAspect(Acm.ACM_IDENTIFIABLE)) {
            artifactNode.addAspect(Acm.ACM_IDENTIFIABLE);
        }
        if (!artifactNode.hasAspect("view:Checksummable")) {
            artifactNode.addAspect("view:Checksummable");
        }

        artifactNode.createOrUpdateProperty(Acm.CM_TITLE, artifactId);
        artifactNode.createOrUpdateProperty("cm:isIndexed", true);
        artifactNode.createOrUpdateProperty("cm:isContentIndexed", false);
        artifactNode.createOrUpdateProperty(Acm.ACM_ID, artifactId);
        artifactNode.createOrUpdateProperty("view:cs", cs);

        if (logger.isDebugEnabled()) {
            logger.debug("Creating artifact with indexing: " + artifactNode.getProperty("cm:isIndexed"));
        }

        ContentWriter writer = services.getContentService().getWriter(artifactNode.getNodeRef(),
                        ContentModel.PROP_CONTENT, true);
        InputStream contentStream = new ByteArrayInputStream(content);
        writer.putContent(contentStream);

        ContentData contentData = writer.getContentData();
        contentData = ContentData.setMimetype(contentData, EmsScriptNode.getMimeType(finalType));
        if (base64content == null) {
            contentData = ContentData.setEncoding(contentData, "UTF-8");
        }
        artifactNode.makeSureNodeRefIsNotFrozen();
        artifactNode.transactionCheck();
        services.getNodeService().setProperty(artifactNode.getNodeRef(), ContentModel.PROP_CONTENT, contentData);
        NodeUtil.propertyCachePut(artifactNode.getNodeRef(), NodeUtil.getShortQName(ContentModel.PROP_CONTENT),
                        contentData);

        // if only version, save dummy version so snapshots can reference
        // versioned images - need to check against 1 since if someone
        // deleted previously a "dead" version is left in its place
        Object[] versionHistory = artifactNode.getEmsVersionHistory();

        if (versionHistory == null || versionHistory.length <= 1) {
            artifactNode.makeSureNodeRefIsNotFrozen();
            artifactNode.createVersion("creating the version history", false);
        }

        artifactNode.getOrSetCachedVersion();

        return artifactNode;
    }

	public static EmsScriptNode updateOrCreateArtifactPng(EmsScriptNode svgNode, Path pngPath,
			String orgId, String projectId, String refId, Date dateTime, StringBuffer response,
			Status status, boolean ignoreName) throws Throwable {
		if(svgNode == null){
			throw new NullArgumentException("SVG script node");
		}
		if(!Files.exists(pngPath)){
			throw new NullArgumentException("PNG path");
		}

		EmsScriptNode pngNode;
		String finalType = "png";
		String artifactId = pngPath.getFileName().toString();

		byte[] content = Files.readAllBytes(pngPath);
		long cs = EmsScriptNode.getChecksum(content);

		// see if image already exists by looking up by checksum
		//ArrayList<NodeRef> refs = findNodeRefsByType("" + cs,
		//		SearchType.CHECKSUM.prefix, false, workspace, dateTime, false,
		//		false, services, false);
		// ResultSet existingArtifacts =
		// NodeUtil.findNodeRefsByType( "" + cs, SearchType.CHECKSUM,
		// services );
		// Set< EmsScriptNode > nodeSet = toEmsScriptNodeSet( existingArtifacts
		// );
		//List<EmsScriptNode> nodeList = EmsScriptNode.toEmsScriptNodeList(refs,
		//		services, response, status);
		// existingArtifacts.close();

		EmsScriptNode matchingNode = null;

		//if (nodeList != null && nodeList.size() > 0) {
		//	matchingNode = nodeList.iterator().next();
		//}

		// No need to update if the checksum and name match (even if it is in a
		// parent branch):
		if (matchingNode != null
				&& (ignoreName || matchingNode.getSysmlId().equals(artifactId))) {
			return matchingNode;
		}

        EmsScriptNode targetSiteNode = EmsScriptNode.getSiteNode(orgId);

        // find site; it must exist!
        if (targetSiteNode == null || !targetSiteNode.exists()) {
            Debug.err("Can't find node for site: " + orgId + "!\n");
            return null;
        }

        // find or create subfolder
        EmsScriptNode subfolder = targetSiteNode.childByNamePath("/" + projectId +  "/refs/" + refId);
        if (subfolder == null || !subfolder.exists()) {
            return null;
        }

        // find or create node:
        pngNode = subfolder.childByNamePath("/" + artifactId);
		// Node wasnt found, so create one:
		if (pngNode == null) {
			pngNode = subfolder.createNode(artifactId, "cm:content");
			subfolder.getOrSetCachedVersion();
		}

		if (pngNode == null || !pngNode.exists()) {
			Debug.err("Failed to create new PNG artifact " + artifactId + "!\n");
			return null;
		}

		pngNode.makeSureNodeRefIsNotFrozen();
		if (!pngNode.hasAspect("cm:versionable")) {
			pngNode.addAspect("cm:versionable");
		}
		if (!pngNode.hasAspect("cm:indexControl")) {
			pngNode.addAspect("cm:indexControl");
		}
		if (!pngNode.hasAspect(Acm.ACM_IDENTIFIABLE)) {
			pngNode.addAspect(Acm.ACM_IDENTIFIABLE);
		}
		if (!pngNode.hasAspect("view:Checksummable")) {
			pngNode.addAspect("view:Checksummable");
		}

		pngNode.createOrUpdateProperty(Acm.CM_TITLE, artifactId);
		pngNode.createOrUpdateProperty("cm:isIndexed", true);
		pngNode.createOrUpdateProperty("cm:isContentIndexed", false);
		pngNode.createOrUpdateProperty(Acm.ACM_ID, artifactId);
		pngNode.createOrUpdateProperty("view:cs", cs);

		if (logger.isDebugEnabled()) {
			logger.debug("Creating PNG artifact with indexing: "
					+ pngNode.getProperty("cm:isIndexed"));
		}

		ContentWriter writer = services.getContentService().getWriter(
				pngNode.getNodeRef(), ContentModel.PROP_CONTENT, true);
		InputStream contentStream = new ByteArrayInputStream(content);
		writer.putContent(contentStream);

		ContentData contentData = writer.getContentData();
		contentData = ContentData.setMimetype(contentData,
				EmsScriptNode.getMimeType(finalType));
		contentData = ContentData.setEncoding(contentData, "UTF-8");
		pngNode.makeSureNodeRefIsNotFrozen();
		pngNode.transactionCheck();
		services.getNodeService().setProperty(pngNode.getNodeRef(),
				ContentModel.PROP_CONTENT, contentData);
		NodeUtil.propertyCachePut(pngNode.getNodeRef(),
				NodeUtil.getShortQName(ContentModel.PROP_CONTENT), contentData);

		Object[] versionHistory = pngNode.getEmsVersionHistory();

        if ( versionHistory == null || versionHistory.length <= 1 ) {
            pngNode.makeSureNodeRefIsNotFrozen();
            pngNode.createVersion( "creating the version history", false );
        }

		pngNode.getOrSetCachedVersion();

		return pngNode;
	}

    // private static <X extends Serializable, V> void clearAlfrescoNodeCache()
    // {
    public static void clearAlfrescoNodeCache() {
        try {
            // org.alfresco.repo.cache.TransactionalCache< X, V > c;
            // getServiceRegistry().getTransactionService().
            // ((DbNodeServiceImpl)getServiceRegistry().getNodeService()).nodeDAO.clear();
            // ((DbNodeServiceImpl)((Version2ServiceImpl)getServiceRegistry().getVersionService()).dbNodeService).nodeDAO.clear();
            DbNodeServiceImpl dbNodeService = (DbNodeServiceImpl) getServiceRegistry().getNodeService();
            Object[] emptyArray = new Object[] {};
            Method method = ClassUtils.getMethodForArgs(NodeDAO.class, "clear", emptyArray);
            // Field f = ClassUtils.getField( DbNodeServiceImpl.class,
            // "nodeDAO", true );
            // NodeDAO nodeDao = (NodeDAO)f.get( dbNodeService );
            NodeDAO nodeDao = (NodeDAO) ClassUtils.getFieldValue(dbNodeService, "nodeDAO", false);
            MethodCall mc = new MethodCall(nodeDao, method, emptyArray);
            mc.invoke(false);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void transactionCheck(Logger logger, EmsScriptNode node) {
        // logger.error( "inTransaction = " + NodeUtil.isInsideTransactionNow()
        // );
        if (NodeUtil.isInsideTransactionNow()) {
            if (NodeUtil.hasBeenOutsideTransaction()) {
                /*
                Exception e = new Exception();
                logger.error("In transaction when have been outside! " + node, e);
                logger.error("Stack trace when last outside transaction:\n"
                                + Utils.toString(getOutsideTransactionStackTrace()));
                */
            }
            NodeUtil.setBeenInsideTransaction(true);
            setInsideTransactionStackTrace();
        } else {
            if (NodeUtil.hasBeenInsideTransaction()) {
                /*
                Exception e = new Exception();
                logger.error("Outside transaction when have been inside! " + node, e);
                logger.error("Stack trace when last inside transaction:\n"
                                + Utils.toString(getInsideTransactionStackTrace()));
                */
            }
            NodeUtil.setBeenOutsideTransaction(true);
            setOutsideTransactionStackTrace();
        }
    }

    /**
     * FIXME Recipients and senders shouldn't be hardcoded - need to have these spring injected
     *
     * @param subject
     * @param msg
     */
    public static void sendNotificationEvent(String subject, String msg, ServiceRegistry services) {
        // FIXME: need to base the single send on the same subject
        if (!heisenbugSeen) {
            String hostname = services.getSysAdminParams().getAlfrescoHost();

            String sender = hostname + "@jpl.nasa.gov";
            String recipient;

            if (hostname.toLowerCase().contains("europa")) {
                recipient = "kerzhner@jpl.nasa.gov";
                ActionUtil.sendEmailTo(sender, recipient, msg, subject, services);
            }
            recipient = "mbee-dev-admin@jpl.nasa.gov";
            ActionUtil.sendEmailTo(sender, recipient, msg, subject, services);
            heisenbugSeen = true;
        }
    }

    public static String getHostname() {
        return services.getSysAdminParams().getAlfrescoHost();
    }

    public static EmsScriptNode getOrCreateContentNode(EmsScriptNode parent, String cmName, ServiceRegistry services) {
        // See if node already exists.
        EmsScriptNode node = parent.childByNamePath(cmName);

        if (!exists(node)) {
            if (node != null) {
                Debug.error(true, false,
                                "Error! tried to create " + cmName + " in parent, " + parent
                                                + ", but a deleted or corrupt node of the same name exists.  Renaming to a_"
                                                + cmName + ".");
                cmName = "a_" + cmName;
                return getOrCreateContentNode(parent, cmName, services);
            }
            node = parent.createNode(cmName, "cm:content");
        }
        return node;
    }

    private static Pattern pattern = Pattern.compile("<mms-cf.*mms-element-id=\"([^\"]*)\"");
    public static void processDocumentEdges(String sysmlid, String doc, List<Pair<String, String>> documentEdges) {
        if (doc != null) {
            Matcher matcher = pattern.matcher(doc);

            while (matcher.find()) {
                String mmseid = matcher.group(1).replace("\"", "");
                if (mmseid != null)
                    documentEdges.add(new Pair<>(sysmlid, mmseid));
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
                    Set<Object> sources = findKeyValueInJsonObject(json, "source");
                    for (Object source : sources) {
                        if (source instanceof String) {
                            documentEdges.add(new Pair<>(sysmlId, (String) source));
                        }
                    }
                } catch (JSONException ex) {
                    //case if value string isn't actually a serialized jsonobject
                }
            }
        }
    }



    public static Set<Object> findKeyValueInJsonObject(JSONObject json, String keyMatch) {
        Set<Object> result = new HashSet<>();
        Iterator<?> keys = json.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value = json.get(key);
            if (key.equals(keyMatch)) {
                result.add(value);
            } else if (value instanceof JSONObject) {
                result.addAll(findKeyValueInJsonObject((JSONObject) value, keyMatch));
            } else if (value instanceof JSONArray) {
                result.addAll(findKeyValueInJsonArray((JSONArray) value, keyMatch));
            }
        }
        return result;
    }

    public static Set<Object> findKeyValueInJsonArray(JSONArray jsonArray, String keyMatch) {
        Set<Object> result = new HashSet<>();

        for (int ii = 0; ii < jsonArray.length(); ii++) {
            if (jsonArray.get(ii) instanceof JSONObject) {
                result.addAll(findKeyValueInJsonObject((JSONObject) jsonArray.get(ii), keyMatch));
            } else if (jsonArray.get(ii) instanceof JSONArray) {
                result.addAll(findKeyValueInJsonArray((JSONArray) jsonArray.get(ii), keyMatch));
            }
        }

        return result;
    }


    /**
     * getModuleService Retrieves the ModuleService of the ServiceRegistry passed in
     *
     * @param services ServiceRegistry object that contains the desired ModuleService
     * @return ModuleService
     */
    public static ModuleService getModuleService(ServiceRegistry services) {
        // Checks to see if the services passed in is null, if so, it will call
        // on class method getServices
        if (services == null) {
            services = getServices();
        }
        // Takes the ServiceRegistry and calls the ModuleService super method
        // getService(Creates an Alfresco QName using the namespace
        // service and passes in the default URI
        ModuleService moduleService = (ModuleService) services
                        .getService(QName.createQName(NamespaceService.ALFRESCO_URI, "ModuleService"));
        return moduleService;
    }

    /**
     * getServiceModules </br>
     * </br>
     * Returns a JSONArray of Module Details from the Service Modules
     *
     * @param service the service containing modules to be returned
     * @return JSONArray of ModuleDetails within the ModuleService object
     */
    public static JSONArray getServiceModulesJson(ModuleService service) {

        JSONArray jsonArray = new JSONArray();
        List<ModuleDetails> modules = service.getAllModules();
        for (ModuleDetails detail : modules) {
            JSONObject jsonModule = moduleDetailsToJson(detail);
            jsonArray.put(jsonModule);
        }
        return jsonArray;
    }

    /**
     * moduleDetailsToJson </br>
     * </br>
     * Takes a module of type ModuleDetails and retrieves all off the module's members and puts them
     * into a newly instantiated JSONObject. </br>
     * </br>
     * JSONObject will have the details : title, version, aliases, class, dependencies, editions id
     * and properties
     *
     * @param module A single module of type ModuleDetails
     * @return JSONObject which contains all the details of that module
     */
    public static JSONObject moduleDetailsToJson(ModuleDetails module) {
        JSONObject jsonModule = new JSONObject();
        try {
            jsonModule.put("mmsTitle", module.getTitle());
            jsonModule.put("mmsVersion", module.getModuleVersionNumber());
            jsonModule.put("mmsAliases", module.getAliases());
            jsonModule.put("mmsClass", module.getClass());
            jsonModule.put("mmsDependencies", module.getDependencies());
            jsonModule.put("mmsEditions", module.getEditions());
            jsonModule.put("mmsId", module.getId());
            jsonModule.put("mmsProperties", module.getProperties());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return jsonModule;
    }

    /**
     * getMMSversion </br>
     * </br>
     * Gets the version number of a module, returns a JSONObject which calls on getString with
     * 'version' as an argument. This will return a String representing the version of the
     * mms. </br>
     *
     * @return Version number of the MMS as type String
     */
    public static String getMMSversion() {
        ModuleService service = getModuleService(services);
        JSONArray moduleDetails = getServiceModulesJson(service);
        String mmsVersion = "NA";
        int moduleArrayLength = moduleDetails.length();
        if (moduleArrayLength > 0) {
            for (int i = 0; i < moduleArrayLength; i++) {
                if (moduleDetails.getJSONObject(i).getString("mmsId").equalsIgnoreCase("mms-amp")) {
                    mmsVersion = moduleDetails.getJSONObject(i).getString("mmsVersion");
                }
            }
        }

        int endIndex = mmsVersion.lastIndexOf(".");
        return endIndex > 0 ? mmsVersion.substring(0, endIndex) : mmsVersion;
    }
}
