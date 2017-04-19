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

    public static StackTraceElement[] getInsideTransactionStackTrace() {
        return insideTransactionStrackTrace.get(Thread.currentThread().getId());
    }

    public static StackTraceElement[] getOutsideTransactionStackTrace() {
        return outsideTransactionStrackTrace.get(Thread.currentThread().getId());
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

    public final static String sitePkgPrefix = "site_";

    /**
     * A cache of alfresco nodes stored as a map from cm:name to node for the master branch only.
     */
    public static Map<String, NodeRef> simpleCache = Collections.synchronizedMap(new HashMap<String, NodeRef>());

    /**
     * A cache of alfresco nodes stored as a map from NodeRef and time to node as determined by
     * {@link #getNodeRefAtTime(NodeRef, Date)}.
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

    /**
     * A cache of the most nodeRefs, keyed by the nodes' alfresco ids. This is to get around the
     * "Heisenbug" where alfresco's current version is sometimes tied to an old version.
     */
    protected static Map<String, NodeRef> heisenCache = Collections.synchronizedMap(new HashMap<String, NodeRef>());

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
    public static long jsonCacheHits = 0;
    public static long jsonCacheMisses = 0;

    // The json string cache maps JSONObjects to an integer (date in millis) to
    // a string rendering of itself paired with the date.
    public static Map<JSONObject, Map<Integer, Pair<Date, String>>> jsonStringCache =
                    Collections.synchronizedMap(new HashMap<JSONObject, Map<Integer, Pair<Date, String>>>());
    public static long jsonStringCacheHits = 0;
    public static long jsonStringCacheMisses = 0;

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

    public static final Comparator<? super NodeRef> nodeRefComparator = GenericComparator.instance();

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

    public static NodeRef nodeAtTimeCachePut(NodeRef nodeRef, Date date, NodeRef refAtTime) {
        if (!doNodeAtTimeCaching || nodeRef == null) {
            return null;
        }
        // Get an integer for the date, the number if milliseconds since
        // 1/1/1970. Zero means now.
        Long millis = date == null ? 0 : date.getTime();
        // Get the current time to see if the date is in the future.
        Long now = null;
        if (date != null) {
            now = (new Date()).getTime();
        }
        // If the date is in the future, set to zero, which means now.
        // TODO -- Do other caches need to check for this?
        if (now != null && millis > now)
            millis = 0l;
        if (logger.isDebugEnabled())
            logger.debug("nodeAtTimeCachePut(" + nodeRef + ", " + millis + ", " + refAtTime + ")");
        // logger.warn( "nodeAtTimeCachePut("
        // + nodeRef + ", " + millis
        // + ", " + refAtTime + ")" );
        Object value = refAtTime;
        if (refAtTime == null)
            value = NULL_OBJECT;
        Object oldValue = Utils.put(nodeAtTimeCache, nodeRef, millis, value);
        if (oldValue == null)
            return null;
        if (oldValue == NULL_OBJECT)
            return null;
        if (oldValue instanceof NodeRef) {
            return (NodeRef) oldValue;
        }
        logger.error("Bad value stored in nodeAtTimeCache(" + nodeRef + ", " + millis + "): " + oldValue);
        return null;
    }

    public static Object nodeAtTimeCacheGet(NodeRef nodeRef, Date date) {
        if (!doNodeAtTimeCaching || nodeRef == null) {
            return null;
        }
        Long millis = date == null ? 0 : date.getTime();
        if (logger.isDebugEnabled())
            logger.debug("nodeAtTimeCacheGet(" + nodeRef + ", " + millis + ")");
        Object o = Utils.get(nodeAtTimeCache, nodeRef, millis);
        // logger.warn( "nodeAtTimeCacheGet("
        // + nodeRef + ", " + millis
        // + ") = " + o );
        return o;
        // if ( o == NULL_OBJECT ) return null;
        // try {
        // return (NodeRef)o;
        // } catch (ClassCastException e) {
        // e.printStackTrace();
        // }
        // return null;
    }

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

    public static void propertyCachePut(NodeRef nodeRef, Map<QName, Serializable> properties) {
        if (!doPropertyCaching || nodeRef == null || Utils.isNullOrEmpty(properties)) {
            return;
        }
        Map<String, Object> cachedProps = NodeUtil.propertyCacheGetProperties(nodeRef);
        if (cachedProps == null) {
            cachedProps = new HashMap<>();
            NodeUtil.propertyCache.put(nodeRef, cachedProps);
        } else {
            cachedProps.clear();
        }
        for (QName qName : properties.keySet()) {
            String key = getShortQName(qName);
            Serializable value = properties.get(key);
            cachedProps.put(key, value);
            if (logger.isTraceEnabled())
                logger.trace("propertyCachePut(" + nodeRef + ", " + key + ", " + value + ") from map");
        }
    }

    public static boolean propertyCacheHas(NodeRef nodeRef, String propertyName) {
        if (!doPropertyCaching || nodeRef == null || Utils.isNullOrEmpty(propertyName)) {
            return false;
        }
        Map<String, Object> props = NodeUtil.propertyCache.get(nodeRef);
        if (props != null) {
            if (props.containsKey(propertyName)) {
                return true;
            }
        }
        return false;
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

    public static Map<String, Object> propertyCacheGetProperties(NodeRef nodeRef) {
        if (!doPropertyCaching || nodeRef == null)
            return null;
        return propertyCache.get(nodeRef);
    }

    /**
     * clear or create the cache for correcting bad node refs (that refer to wrong versions)
     */
    public static void initHeisenCache() {
        // TODO -- the problem with this is that concurrent services could clear
        // this common cache. It would be better to pass around a context or
        // something with a service identifier as a key to the cache. So, each
        // web service invocation would have its own cache.
        heisenCache.clear();
    }

    public static NodeRef heisenCachePut(String id, NodeRef nodeRef) {
        return heisenCache.put(id, nodeRef);
    }

    public static NodeRef heisenCacheGet(String id) {
        return heisenCache.get(id);
    }

    public static NodeRef getCurrentNodeRefFromCache(NodeRef maybeFrozenNodeRef) {
        NodeRef ref = frozenNodeCache.get(maybeFrozenNodeRef);
        if (ref != null)
            return ref;
        return null;
    }

    public static JSONObject jsonCacheGet(String id, long millis, boolean noMetadata) {
        JSONObject json = Utils.get(jsonCache, id, millis);
        if (logger.isDebugEnabled()) {
            logger.debug("jsonCacheGet(" + id + ", " + millis + ", " + noMetadata + ") = " + json);
        }
        if (doJsonStringCaching && noMetadata) {
            json = clone(json);
            stripJsonMetadata(json);
        } else if (!doJsonStringCaching) {
            stripJsonMetadata(json);
        }
        return json;
    }

    public static JSONObject jsonCachePut(JSONObject json, String id, long millis) {
        json = clone(json);
        if (doJsonStringCaching) {
            json = addJsonMetadata(json, id, millis, true, null);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("jsonCachePut(" + id + ", " + millis + ", " + json + ")");
        }
        Utils.put(jsonCache, id, millis, json);
        return json;
    }

    public static JSONObject jsonDeepCacheGet(String id, long millis, boolean isIncludeQualified,
                    Set<String> jsonFilter, String versionLabel, boolean noMetadata) {
        jsonFilter = jsonFilter == null ? new TreeSet<>() : jsonFilter;
        if (versionLabel == null)
            versionLabel = "";
        JSONObject json = Utils.get(jsonDeepCache, id, millis, isIncludeQualified, jsonFilter, versionLabel);
        if (doJsonStringCaching && noMetadata) {
            json = clone(json);
            stripJsonMetadata(json);
        } else if (!doJsonStringCaching) {
            stripJsonMetadata(json);
        }
        if (Debug.isOn())
            logger.debug("jsonDeepCacheGet(" + id + ", " + millis + ", " + isIncludeQualified + ", " + jsonFilter + ", "
                            + noMetadata + ") = " + json);
        return json;
    }

    public static JSONObject jsonDeepCachePut(JSONObject json, String id, long millis, boolean isIncludeQualified,
                    Set<String> jsonFilter, String versionLabel) {
        json = clone(json);
        jsonFilter = jsonFilter == null ? new TreeSet<String>() : jsonFilter;
        json = addJsonMetadata(json, id, millis, isIncludeQualified, jsonFilter);
        if (versionLabel == null)
            versionLabel = "";
        if (Debug.isOn()) {
            logger.debug("jsonDeepCachePut(" + id + ", " + millis + ", " + isIncludeQualified + ", " + jsonFilter + ", "
                            + versionLabel + ", " + json + ")");
        }
        Utils.put(jsonDeepCache, id, millis, isIncludeQualified, jsonFilter, versionLabel, json);
        return json;
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

    /**
     * Put json.toString() in the json.
     *
     * @param json
     * @param id
     * @param millis
     * @param isIncludeQualified
     * @param jsonFilter
     * @return
     */
    public static JSONObject addJsonMetadata(JSONObject json, String id, long millis, boolean isIncludeQualified,
                    Set<String> jsonFilter) {
        if (json == null)
            return null;
        if (!doJsonStringCaching)
            return json;

        if (json.has("jsonString4"))
            return json;

        jsonFilter = jsonFilter == null ? new TreeSet<>() : jsonFilter;
        String jsonString4 = null;

        try {
            jsonString4 = json.toString(4);
        } catch (JSONException e1) {
            e1.printStackTrace();
            return null;
        }
        if (jsonString4 == null) {
            logger.warn("json.toString(4) returned empty for " + json);
            return null;
        }
        String jsonString = jsonString4.replaceAll("( |\n)+", " ");
        try {
            json.put("jsonString", jsonString);
            json.put("jsonString4", jsonString4);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("addJsonMetadata(" + id + ", " + millis + ", " + isIncludeQualified + ", " + jsonFilter
                            + ") -> " + json);
        }
        return json;
    }

    protected static List<String> dontFilterList = Utils.newList("read", Acm.JSON_ID, "id", "creator",
                    Acm.JSON_LAST_MODIFIED, Acm.JSON_SPECIALIZATION);
    protected static Set<String> dontFilterOut = Collections.synchronizedSet(new HashSet<String>(dontFilterList));

    protected static JSONObject filterJson(JSONObject json, Set<String> jsonFilter, boolean isIncludeQualified)
                    throws JSONException {
        JSONObject newJson = new JSONObject();
        Iterator keys = json.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (jsonFilter.contains(key) || dontFilterOut.contains(key)) {
                Object value = json.get(key);
                if (key.equals(Acm.JSON_SPECIALIZATION) && value instanceof JSONObject) {
                    JSONObject newSpec = filterJson((JSONObject) value, jsonFilter, isIncludeQualified);
                    if (newSpec != null && newSpec.length() > 0) {
                        newJson.put(key, newSpec);
                    }
                } else if (isIncludeQualified || (!key.equals("qualifiedId") && !key.equals("qualifiedName"))) {
                    if (Debug.isOn())
                        Debug.outln("add to newJson = " + key + ":" + value);
                    newJson.put(key, value);
                }
            }
        }
        return newJson;
    }

    public static String jsonToString(JSONObject json) {
        if (!doJsonStringCaching)
            return json.toString();
        try {
            String s = jsonToString(json, -1);

            return s;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

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

    public static String oldJsonToString(JSONObject json, int numSpacesToIndent) throws JSONException {
        if (!doJsonStringCaching)
            return json.toString(numSpacesToIndent);
        String result = null;
        String modString = json.optString("modified");
        Date mod = null;
        // Only cache json with a modified date so that we know when to update
        // it.
        if (modString != null) {
            mod = TimeUtils.dateFromTimestamp(modString);
            if (mod != null && jsonStringCache.containsKey(json)) {
                Pair<Date, String> p = Utils.get(jsonStringCache, json, numSpacesToIndent);// stringCache.get(
                                                                                           // this
                                                                                           // );
                if (p != null) {
                    if (p.first != null && !mod.after(p.first)) {
                        result = p.second;
                        // cache hit
                        ++jsonStringCacheHits;
                        if (logger.isTraceEnabled())
                            logger.trace("string cache hit : " + result);
                        return result;
                    }
                }
            }
        }
        if (numSpacesToIndent < 0) {
            result = json.toString();
        } else {
            result = json.toString(numSpacesToIndent);
        }
        if (mod == null) {
            // cache not applicable
        } else {
            // cache miss; add to cache
            ++jsonStringCacheMisses;
            Utils.put(jsonStringCache, json, numSpacesToIndent, new Pair<>(mod, result));
        }
        return result;
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

    public static JSONObject newJsonObject(String s) throws JSONException {
        if (!doJsonStringCaching)
            return new JSONObject(s);
        JSONObject newJson = new CachedJsonObject(s);
        return newJson;
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

    public static StoreRef getStoreRef() {
        if (SEARCH_STORE == null) {
            SEARCH_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
        }
        return SEARCH_STORE;
    }

    // public static ApplicationContext getApplicationContext() {
    // String[] contextPath =
    // new String[] { "classpath:alfresco/application-context.xml" };
    // ApplicationContext applicationContext =
    // ApplicationContextHelper.getApplicationContext( contextPath );
    // return applicationContext;
    // }

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

    public static Collection<EmsScriptNode> luceneSearchElements(String queryPattern) {
        ResultSet resultSet = null;
        List<EmsScriptNode> resultList = new ArrayList<>();
        try {
            resultSet = luceneSearch(queryPattern, (SearchService) null);
            if (Debug.isOn())
                logger.debug("luceneSearch(" + queryPattern + ") returns " + resultSet.length() + " matches.");
            resultList = resultSetToList(resultSet);
        } catch (Exception e) {
            logger.error("Could not get results");
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return resultList;
    }

    public static ResultSet luceneSearch(String queryPattern) {
        return luceneSearch(queryPattern, (SearchService) null);
    }

    public static ResultSet luceneSearch(String queryPattern, ServiceRegistry services) {
        if (services == null)
            services = getServices();
        return luceneSearch(queryPattern, services == null ? null : services.getSearchService());
    }

    public static ResultSet luceneSearch(String queryPattern, SearchService searchService) {

        timerLucene = Timer.startTimer(timerLucene, timeEvents);

        if (searchService == null) {
            if (getServiceRegistry() != null) {
                searchService = getServiceRegistry().getSearchService();
            }
        }
        ResultSet results = null;
        if (searchService != null) {
            try {
                results = searchService.query(getSearchParameters(queryPattern));
            } catch (Exception e) {
                logger.warn(e.getMessage());
                results = null;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("luceneSearch(%s): returned %d nodes.", queryPattern, results.length()));
        }

        Timer.stopTimer(timerLucene, "***** luceneSearch(): time", timeEvents);

        return results;
    }

    public static SearchParameters getSearchParameters(String queryPattern) {
        final SearchParameters params = new SearchParameters();
        params.addStore(getStoreRef());
        params.setLanguage(SearchService.LANGUAGE_LUCENE);
        params.setQuery(queryPattern);
        params.setLimitBy(LimitBy.UNLIMITED);
        params.setLimit(0);
        params.setMaxPermissionChecks(100000);
        params.setMaxPermissionCheckTimeMillis(100000);
        params.setMaxItems(-1);
        return params;
    }

    public static List<EmsScriptNode> resultSetToList(ResultSet results) {
        ArrayList<EmsScriptNode> nodes = new ArrayList<>();
        for (ResultSetRow row : results) {
            EmsScriptNode node = new EmsScriptNode(row.getNodeRef(), getServices());
            nodes.add(node);
        }
        return nodes;

    }

    public static ArrayList<NodeRef> resultSetToNodeRefList(ResultSet results) {
        ArrayList<NodeRef> nodes = new ArrayList<>();
        for (ResultSetRow row : results) {
            if (row.getNodeRef() != null) {
                nodes.add(row.getNodeRef());
            }
        }
        return nodes;
    }

    public static String resultSetToString(ResultSet results) {
        return "" + resultSetToList(results);
    }

    public static List<NodeRef> findNodeRefsByType(String name, SearchType type, ServiceRegistry services) {
        return findNodeRefsByType(name, type.prefix, services);
    }

    private static ArrayList<NodeRef> searchInElastic() {
        try {
            ArrayList<NodeRef> nodes = new ArrayList<>();
            DefaultHttpClient client = new DefaultHttpClient();
            HttpPost request = new HttpPost("http://localhost:9200/mms/element/_search");
            StringEntity postData = new StringEntity(
                            "{\"query\": {\"filtered\" : {\"query\" : {\"query_string\" : {\"query\" : \"paper\" } }, "
                                            + "\"filter\" : {\"term\" : { \"creator\" : \"admin\" }   }   } }}");
            request.setEntity(postData);

            HttpResponse response = client.execute(request);
            InputStream ips = response.getEntity().getContent();
            BufferedReader buf = new BufferedReader(new InputStreamReader(ips, "UTF-8"));
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new Exception(response.getStatusLine().getReasonPhrase());
            }
            StringBuilder sb = new StringBuilder();
            String s;
            while (true) {
                s = buf.readLine();
                if (s == null || s.length() == 0)
                    break;
                sb.append(s);
            }
            buf.close();
            ips.close();

            JSONObject responseObject = new JSONObject(sb.toString());
            JSONArray hits = responseObject.getJSONObject("hits").getJSONArray("hits");

            for (int i = 0; i < hits.length(); i++) {
                JSONObject hit = hits.getJSONObject(i);
                String nodeRefId = hit.getJSONObject("_source").getString("nodeRefId");
                NodeRef node = findNodeRefByAlfrescoId(nodeRefId);
                nodes.add(node);
            }
            return nodes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ArrayList<NodeRef> findNodeRefsByType(String name, String prefix, ServiceRegistry services) {
        ResultSet results = null;
        String queryPattern = prefix + name + "\"";
        results = luceneSearch(queryPattern, services);
        ArrayList<NodeRef> resultList = resultSetToNodeRefList(results);
        results.close();
        return resultList;
    }

    /*
     * rahulku: original public static ArrayList< NodeRef > findNodeRefsByType(String name, String
     * prefix, ServiceRegistry services) { ResultSet results = null; String queryPattern = prefix +
     * name + "\""; results = luceneSearch( queryPattern, services ); //if ( (results == null ||
     * results.length() == 0 ) && pref) ArrayList< NodeRef > resultList = resultSetToNodeRefList(
     * results ); results.close(); return resultList; }
     */

    public static NodeRef findNodeRefByType(String name, SearchType type, boolean ignoreWorkspace,
                    WorkspaceNode workspace, Date dateTime, boolean exactMatch, ServiceRegistry services,
                    boolean findDeleted) {
        return findNodeRefByType(name, type.prefix, ignoreWorkspace, workspace, dateTime, exactMatch, services,
                        findDeleted, null);
    }

    public static NodeRef findNodeRefByType(String name, SearchType type, boolean ignoreWorkspace,
                    WorkspaceNode workspace, Date dateTime, boolean exactMatch, ServiceRegistry services,
                    boolean findDeleted, String siteName) {
        return findNodeRefByType(name, type.prefix, ignoreWorkspace, workspace, dateTime, exactMatch, services,
                        findDeleted, siteName);
    }

    public static NodeRef findNodeRefByType(String specifier, String prefix,
                    // String parentScopeName,
                    boolean ignoreWorkspace, WorkspaceNode workspace, Date dateTime, boolean exactMatch,
                    ServiceRegistry services, boolean findDeleted, String siteName) {
        ArrayList<NodeRef> refs = findNodeRefsByType(specifier, prefix,
                        // parentScopeName,
                        ignoreWorkspace, workspace, dateTime, true, exactMatch, services, findDeleted, siteName);
        if (Utils.isNullOrEmpty(refs))
            return null;
        NodeRef ref = refs.get(0);
        return ref;
    }

    // public Set<NodeRef> findNodeRefsInDateRange( Date fromDate, Date toDate,
    // // String parentScopeName,
    // boolean ignoreWorkspace,
    // WorkspaceNode workspace, Date dateTime,
    // boolean justFirst, boolean exactMatch,
    // ServiceRegistry services, boolean includeDeleted ) {
    // }

    public static ArrayList<NodeRef> findNodeRefsByType(String specifier, String prefix, boolean ignoreWorkspace,
                    WorkspaceNode workspace, Date dateTime, boolean justFirst, boolean exactMatch,
                    ServiceRegistry services, boolean includeDeleted) {

        return findNodeRefsByType(specifier, prefix, ignoreWorkspace, workspace, dateTime, justFirst, exactMatch,
                        services, includeDeleted, null);
    }

    public static ArrayList<NodeRef> findNodeRefsByType(String specifier, String prefix, boolean ignoreWorkspace,
                    WorkspaceNode workspace, Date dateTime, boolean justFirst, boolean exactMatch,
                    ServiceRegistry services, boolean includeDeleted, String siteName) {
        return findNodeRefsByType(specifier, prefix, ignoreWorkspace, workspace, false, // onlyThisWorkspace
                        dateTime, justFirst, exactMatch, services, includeDeleted, siteName);
    }

    public static ArrayList<NodeRef> findNodeRefsByType(String specifier, String prefix, boolean ignoreWorkspace,
                    WorkspaceNode workspace, Date dateTime, boolean justFirst, boolean optimisticJustFirst,
                    boolean exactMatch, ServiceRegistry services, boolean includeDeleted, String siteName) {
        return findNodeRefsByType(specifier, prefix, ignoreWorkspace, workspace, false, // onlyThisWorkspace
                        dateTime, justFirst, optimisticJustFirst, exactMatch, services, includeDeleted, siteName);
    }

    public static ArrayList<NodeRef> findNodeRefsByType(String specifier, String prefix, boolean ignoreWorkspace,
                    WorkspaceNode workspace, boolean onlyThisWorkspace, Date dateTime, boolean justFirst,
                    boolean exactMatch, ServiceRegistry services, boolean includeDeleted, String siteName) {
        // If justFirst == true, then we can try to get
        // lucky by setting optimisticJustFirst=true and
        // always quit iterating through results after
        // the first match. If that single result gets
        // filtered when there are multiple candidates,
        // return null instead of an empty list to
        // signal that calling again with
        // optimisticJustFirst=false may find other
        // candidates that pass the filter.
        boolean optimisticJustFirst = doOptimisticJustFirst && justFirst;
        ArrayList<NodeRef> refs = findNodeRefsByType(specifier, prefix, ignoreWorkspace, workspace, onlyThisWorkspace,
                        dateTime, justFirst, optimisticJustFirst, exactMatch, services, includeDeleted, siteName);
        if (!justFirst || !Utils.isNullOrEmpty(refs))
            return refs;
        if (justFirst && refs == null) {
            optimisticJustFirst = false;
            refs = findNodeRefsByType(specifier, prefix, ignoreWorkspace, workspace, onlyThisWorkspace, dateTime,
                            justFirst, optimisticJustFirst, exactMatch, services, includeDeleted, siteName);
        }
        return refs;
    }

    public static ArrayList<NodeRef> findNodeRefsByType(String specifier, String prefix, boolean ignoreWorkspace,
                    WorkspaceNode workspace, boolean onlyThisWorkspace, Date dateTime, boolean justFirst,
                    // If justFirst == true, then we can try to get
                    // lucky by setting optimisticJustFirst=true and
                    // always quit iterating through results after
                    // the first match. If that single result gets
                    // filtered when there are multiple candidates,
                    // return null instead of an empty list to
                    // signal that calling again with
                    // optimisticJustFirst=false may find other
                    // candidates that pass the filter.
                    boolean optimisticJustFirst, boolean exactMatch, ServiceRegistry services, boolean includeDeleted,
                    String siteName) {

        // Run as admin
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !EmsScriptNode.ADMIN_USER_NAME.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(EmsScriptNode.ADMIN_USER_NAME);
        }

        ArrayList<NodeRef> results = null;

        timerByType = Timer.startTimer(timerByType, timeEvents);

        ArrayList<NodeRef> nodeRefs = new ArrayList<>();
        if (services == null)
            services = getServices();

        optimisticJustFirst = optimisticJustFirst && doOptimisticJustFirst;

        // look in cache first
        boolean useSimpleCache = false;
        boolean useFullCache = false;
        boolean usedSimpleCache = false;
        boolean usedFullCache = false;
        boolean emptyEntriesInFullCacheOk = true;
        CacheResults cacheResults = getNodesInCache(specifier, prefix, ignoreWorkspace, workspace, onlyThisWorkspace,
                        dateTime, justFirst, exactMatch, services, includeDeleted, siteName);
        results = cacheResults.results;
        useSimpleCache = cacheResults.useSimpleCache;
        useFullCache = cacheResults.useFullCache;
        usedSimpleCache = cacheResults.cachedUsed == CacheUsed.SIMPLE;
        usedFullCache = cacheResults.cachedUsed == CacheUsed.FULL;
        emptyEntriesInFullCacheOk = cacheResults.emptyEntriesInFullCacheOk;
        exactMatch = cacheResults.exactMatch;
        if (logger.isDebugEnabled())
            logger.debug("cache results = " + results);

        boolean wasCached = false;
        boolean caching = false;
        boolean hadMultipleCandidates = false; // need to know whether we had
                                               // any results before filtering
        try {
            if (results != null && (emptyEntriesInFullCacheOk || !results.isEmpty())) {
                wasCached = true; // doCaching must be true here
            } else {
                // search using lucene
                results = findNodeRefsByType(specifier, prefix, services);
                if (logger.isDebugEnabled())
                    logger.debug("lucene results = " + results);
                if ((doSimpleCaching || doFullCaching) && !Utils.isNullOrEmpty(results)) {
                    caching = true;
                }
            }

            hadMultipleCandidates = !Utils.isNullOrEmpty(results) && results.size() > 1;

            // clean up results
            if (results != null) {
                if (doHeisenCheck) {
                    results = fixVersions(results);
                    if (logger.isDebugEnabled())
                        logger.debug("fixVersions results = " + results);
                }
                // In the case that dateTime is null and there are cache
                // results,
                // it must be the case that the search is on an id in the master
                // workspace. The assumption here is that the results have been
                // pre-filtered and need not be re-filtered.
                if (wasCached && dateTime == null) {
                    nodeRefs = results;
                } else {
                    hadMultipleCandidates = !Utils.isNullOrEmpty(nodeRefs);
                    // logger.warn("filterResults( results=" + results +
                    // ") = ");
                    nodeRefs = filterResults(results, specifier, prefix, usedFullCache || usedSimpleCache,
                                    ignoreWorkspace, workspace, onlyThisWorkspace, dateTime, justFirst,
                                    optimisticJustFirst, exactMatch, services, includeDeleted, siteName);
                    // logger.warn("" + nodeRefs);
                    if (logger.isDebugEnabled())
                        logger.debug("filterResults = " + nodeRefs);
                }

                // Always want to check for deleted nodes, even if using the
                // cache:
                nodeRefs = correctForDeleted(nodeRefs, specifier, prefix, useSimpleCache, ignoreWorkspace, workspace,
                                onlyThisWorkspace, dateTime, justFirst, exactMatch, services, includeDeleted, siteName);
                if (logger.isDebugEnabled())
                    logger.debug("correctForDeleted nodeRefs = " + nodeRefs);
                // logger.warn("correctForDeleted nodeRefs = " + nodeRefs );

            } // ends if (results != null)

            // Update cache with results
            boolean putInFullCache = false;
            // Don't add to cache if failed to get just the first node
            // optimistically.
            if (caching && (!justFirst || !optimisticJustFirst || !Utils.isNullOrEmpty(nodeRefs))) {
                CacheUsed cacheUsed = putNodesInCache(nodeRefs, specifier, prefix, ignoreWorkspace, workspace,
                                onlyThisWorkspace, dateTime, justFirst, exactMatch, includeDeleted, siteName,
                                emptyEntriesInFullCacheOk, useSimpleCache, useFullCache);
                putInFullCache = cacheUsed == CacheUsed.FULL;
            }

            // Check permissions on results. This is now done after the cache
            // operations so that different users will get the appropriate
            // results without having user-specific entries in the cache.
            nodeRefs = filterForPermissions(nodeRefs, PermissionService.READ, justFirst, putInFullCache // copy
                                                                                                        // items
                                                                                                        // if
                                                                                                        // they
                                                                                                        // were
                                                                                                        // added
                                                                                                        // to
                                                                                                        // the
                                                                                                        // full
                                                                                                        // cache
            );
            if (logger.isDebugEnabled())
                logger.debug("filterForPermissions nodeRefs = " + nodeRefs);
            // logger.warn("filterForPermissions nodeRefs = " + nodeRefs );

        } finally {
            // Debug output
            if (Debug.isOn() && !Debug.isOn()) {
                if (results != null) {
                    List<EmsScriptNode> set = EmsScriptNode.toEmsScriptNodeList(nodeRefs, services, null, null);
                    Debug.outln("findNodeRefsByType(" + specifier + ", " + prefix + ", " + workspace + ", " + dateTime
                                    + ", justFirst=" + justFirst + ", exactMatch=" + exactMatch + "): returning "
                                    + set);
                }
            }
        }

        Timer.stopTimer(timerByType, "***** findNodeRefsByType(): time ", timeEvents);

        // Change running user from admin back to what it was.
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }

        // If optimisticJustFirst=true and there were no results, set the result
        // null instead of an empty list if calling again with
        // optimisticJustFirst=false may produce
        // results. This is a signal to the caller.
        if (justFirst && optimisticJustFirst && Utils.isNullOrEmpty(nodeRefs) && hadMultipleCandidates) {
            nodeRefs = null;
        }
        return nodeRefs;
    }

    public static ArrayList<NodeRef> filterForPermissions(ArrayList<NodeRef> nodeRefs, String permissionType,
                    boolean justFirst, boolean copyIfModified) {
        if (!Utils.isNullOrEmpty(nodeRefs)) {
            ArrayList<NodeRef> noReadPermissions = null;
            boolean gotOne = false;
            for (NodeRef r : nodeRefs) {
                EmsScriptNode esn = new EmsScriptNode(r, getServices());
                // If just getting the first and have already got one, then can
                // go ahead and remove the rest.
                if ((justFirst && gotOne) || !esn.checkPermissions(PermissionService.READ)) {
                    if (noReadPermissions == null) {
                        noReadPermissions = new ArrayList<NodeRef>();
                    }
                    noReadPermissions.add(r);
                } else {
                    gotOne = true;
                    // If not changing the passed in list, and we're just
                    // getting the first, we can stop here.
                    if (justFirst && copyIfModified) {
                        return Utils.newList(r);
                    }
                }
            }
            if (!Utils.isNullOrEmpty(noReadPermissions)) {
                // make a copy if original list should be unaffected
                if (copyIfModified) {
                    nodeRefs = new ArrayList<>(nodeRefs);
                }
                nodeRefs.removeAll(noReadPermissions);
            }
        }
        return nodeRefs;
    }

    public static ArrayList<NodeRef> fixVersions(ArrayList<NodeRef> nodeRefs) {
        // check for alfresco bug where SpacesStore ref is the wrong version
        // if ( !doVersionCaching ) return nodeRefs;
        if (!doHeisenCheck)
            return nodeRefs;
        ArrayList<NodeRef> newNodeRefs = null;
        for (NodeRef nr : nodeRefs) {
            EmsScriptNode esn = new EmsScriptNode(nr, getServices());
            if (!esn.scriptNodeExists())
                continue;
            if (esn.getOrSetCachedVersion()) {
                // if ( !esn.checkNodeRefVersion2( null ) ) {
                if (newNodeRefs == null) {
                    newNodeRefs = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    // Back up and copy nodes we already skipped
                    ArrayList<NodeRef> copy = (ArrayList<NodeRef>) nodeRefs.clone();
                    for (NodeRef earlier : copy) {
                        if (!scriptNodeExists(earlier))
                            continue;
                        if (earlier.equals(nr)) {
                            break;
                        }
                        newNodeRefs.add(earlier);
                    }
                }
                newNodeRefs.add(esn.getNodeRef());
            } else if (newNodeRefs != null) {
                newNodeRefs.add(nr);
            }
        }
        if (newNodeRefs == null) {
            newNodeRefs = nodeRefs;
        }
        return newNodeRefs;
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
     * @param results input list of results to be filtered
     * @param specifier
     * @param prefix
     * @param usedCache whether the results came out of the cache, which means the results have been
     *        pre-filtered
     * @param ignoreWorkspace
     * @param workspace
     * @param onlyThisWorkspace
     * @param dateTime
     * @param justFirst
     * @param exactMatch
     * @param services
     * @param includeDeleted
     * @param siteName
     * @return the new results filtered except for permissions
     */
    protected static ArrayList<NodeRef> filterResults(ArrayList<NodeRef> results, String specifier, String prefix,
                    boolean usedCache, boolean ignoreWorkspace, WorkspaceNode workspace, boolean onlyThisWorkspace,
                    Date dateTime, boolean justFirst, boolean optimisticJustFirst, boolean exactMatch,
                    ServiceRegistry services, boolean includeDeleted, String siteName) {
        ArrayList<NodeRef> nodeRefs = new ArrayList<>();
        NodeRef lowest = null;
        NodeRef nodeRef = null;

        for (NodeRef nr : results) {
            // int minParentDistance = Integer.MAX_VALUE;
            if (nr == null)
                continue;
            EmsScriptNode esn = new EmsScriptNode(nr, getServices());

            if (Debug.isOn() && !Debug.isOn()) {
                Debug.outln("findNodeRefsByType(" + specifier + ", " + prefix + ", " + workspace + ", " + dateTime
                                + ", justFirst=" + justFirst + ", exactMatch=" + exactMatch + "): candidate" + " "
                                + esn.getWorkspaceName() + "::" + esn.getName());
            }

            // Get the version for the date/time if specified.
            // This can be skipped if the results are from the cache by setting
            // skipGetNodeRefAtTime = true.
            // REVIEW -- However, the rest of this method can't be skipped in
            // this case. Why?
            if (dateTime != null && (!skipGetNodeRefAtTime || !usedCache)) {
                nr = getNodeRefAtTime(nr, dateTime);

                // null check
                if (nr == null) {
                    if (Debug.isOn()) {
                        Debug.outln("findNodeRefsByType(): no nodeRef at time " + dateTime);
                    }
                    continue;
                }

                // get EmsScriptNode for versioned node ref
                esn = new EmsScriptNode(nr, getServices());
            }

            // make sure the node still exists
            if (esn != null && !esn.scriptNodeExists()) {
                continue;
            }

            try {
                // Make sure it's in the right workspace.
                if (!ignoreWorkspace && esn != null) {
                    WorkspaceNode esnWs = esn.getWorkspace();
                    if ((onlyThisWorkspace && !workspacesEqual(workspace, esnWs))
                                    || (workspace == null && esnWs != null)
                                    || (workspace != null && !workspace.contains(esn))) {
                        if (Debug.isOn() && !Debug.isOn()) {
                            if (logger.isTraceEnabled())
                                logger.trace("findNodeRefsByType(): wrong workspace " + workspace);
                        }
                        continue;
                    }
                }
            } catch (InvalidNodeRefException e) {
                if (Debug.isOn())
                    e.printStackTrace();
                continue;
            } catch (Throwable e) {
                e.printStackTrace();
            }

            // Make sure we didn't just get a near match.
            try {
                boolean match = true;
                if (exactMatch) {
                    String acmType = Utils.join(prefix.split("[\\W]+"), ":").replaceFirst("^:", "");
                    Object o = esn.getNodeRefProperty(acmType, dateTime, workspace);
                    if (!("" + o).equals(specifier)) {
                        match = false;
                    }
                }
                // Check that it is from the desired site:
                // MBEE-260: sites are case insensitive per Alfresco file naming
                // conventions, so do comparison ignoring case
                if (siteName != null && !siteName.equalsIgnoreCase(esn.getSiteName(dateTime, workspace))) {
                    match = false;
                }
                if (!match) {
                    if (Debug.isOn())
                        Debug.outln("findNodeRefsByType(): not an exact match or incorrect site");
                } else {
                    // Make sure the lowest/deepest element in the workspace
                    // chain is first in the list. We do this by tracking the
                    // lowest so far, and if it is upstream from current, then
                    // the current becomes the lowest and put in front of the
                    // list. This assumes that the elements have the same
                    // sysmlId, but it is not checked. We fix for isDeleted()
                    // later.
                    nodeRef = nr;
                    if (exists(workspace) && (lowest == null || isWorkspaceAncestor(lowest, nodeRef, true))) {
                        lowest = nodeRef;
                        nodeRefs.add(0, nodeRef);
                    } else {
                        nodeRefs.add(nodeRef);
                    }
                    if (Debug.isOn())
                        Debug.outln("findNodeRefsByType(): matched!");
                    // If only wanting the first matching element, we try to
                    // break out of the loop when we find it. There are many
                    // conditions under which we may not be able to do this.
                    if (justFirst &&
                    // (optimisticJustFirst || (

                    // This isn't necessary since we check earlier for
                    // this. Just being robust by re-checking.
                                    scriptNodeExists(lowest) &&
                                    // If we care about the workspace and it is a branch
                                    // in
                                    // time, then it's possible that this (and other)
                                    // noderefs will change due to post processing, so
                                    // we
                                    // don't break in fear of the unknown. We assume the
                                    // workspace will not change, so the lowest should
                                    // still be valid during post-processing.
                                    (ignoreWorkspace || !exists(workspace) || workspace.getCopyTime() == null) &&
                                    // Since we clean up for deleted nodes later, we
                                    // can't
                                    // break unless we don't care whether it's deleted,
                                    // or
                                    // it's not deleted.
                                    (includeDeleted || !isDeleted(lowest)) &&
                                    // We cannot break early if looking in a specific
                                    // workspace unless we found one that is only in the
                                    // target workspace.
                                    (!exists(workspace) || workspace.equals(getWorkspace(nodeRef)))) {// )
                                                                                                      // )
                                                                                                      // {
                        break;
                    }
                }

            } catch (Throwable e) {
                e.printStackTrace();
            }
        } // ends else for

        // correctForWorkspaceCopyTime() can be slow when it gets version
        // history, so we want to try and avoid doing this for more than one
        // node if justFirst = true. In the case that we're just getting . . .
        nodeRefs = correctForWorkspaceCopyTime(nodeRefs, specifier, prefix, ignoreWorkspace, workspace,
                        onlyThisWorkspace, dateTime, justFirst, optimisticJustFirst, exactMatch, services,
                        includeDeleted, siteName);

        return nodeRefs;
    }

    protected static ArrayList<NodeRef> correctForWorkspaceCopyTime(ArrayList<NodeRef> nodeRefs, String specifier,
                    String prefix, boolean ignoreWorkspace, WorkspaceNode workspace, boolean onlyThisWorkspace,
                    Date dateTime, boolean justFirst, boolean optimisticJustFirst, boolean exactMatch,
                    ServiceRegistry services, boolean includeDeleted, String siteName) {
        // If the workspace is copied at a time point (as opposed to
        // floating with the parent), then we need to check each element
        // found to see if it is in some parent workspace and last modified
        // after the copy time. If so, then we need to get the element in
        // the parent workspace at the time of the copy.
        if (nodeRefs == null || workspace == null || ignoreWorkspace) {
            return nodeRefs;
        }
        // loop through each result
        ArrayList<NodeRef> correctedRefs = new ArrayList<>();
        for (NodeRef r : nodeRefs) {
            if (r == null)
                continue;
            WorkspaceNode resultWs = getWorkspace(r);
            EmsScriptNode esn;
            // If a native member of the workspace, no need to correct.
            boolean added = false;
            if (workspace.equals(resultWs)) {
                correctedRefs.add(r);
                added = true;
            } else {
                esn = new EmsScriptNode(r, getServices());
                Date copyTime = workspace.getCopyTime(esn.getWorkspace());
                if (copyTime == null || (dateTime != null && !copyTime.before(dateTime))) {
                    correctedRefs.add(r);
                    added = true;
                } else {
                    Date lastModified = esn.getLastModified(dateTime);
                    // Check if modified after the copyTime.
                    if (lastModified == null || !lastModified.after(copyTime)) {
                        if (lastModified == null) {
                            Debug.error("ERROR!  Should never have null modified date!");
                        }
                        correctedRefs.add(r);
                        added = true;
                    } else {
                        // Replace with the versioned ref at the copy time
                        ArrayList<NodeRef> refs = findNodeRefsByType(esn.getSysmlId(), SearchType.ID.prefix,
                                        ignoreWorkspace, resultWs, copyTime, true, // justOne
                                        optimisticJustFirst, exactMatch, services, includeDeleted, siteName);
                        if (!Utils.isNullOrEmpty(refs)) {
                            // only asked for one
                            NodeRef newRef = refs.get(0);
                            correctedRefs.add(newRef);
                            added = true;
                        }
                        // r = getNodeRefAtTime( r, resultWs, copyTime );
                        // if ( r != null ) {
                        // esn = new EmsScriptNode( r, getServices() );
                        // } else {
                        // esn = null;
                        // }
                    }
                    // if ( exists( esn ) || ( includeDeleted && esn.isDeleted()
                    // &&
                    // ()!exactMatch ) ) {
                    // correctedRefs.add( r );
                    // }
                }
            }
            if (added && justFirst && optimisticJustFirst) {
                break;
            }
        }
        nodeRefs = correctedRefs;

        return nodeRefs;
    }

    protected static ArrayList<NodeRef> correctForDeleted(ArrayList<NodeRef> nodeRefs, String specifier, String prefix,
                    boolean useSimpleCache, boolean ignoreWorkspace, WorkspaceNode workspace, boolean onlyThisWorkspace,
                    Date dateTime, boolean justFirst, boolean exactMatch, ServiceRegistry services,
                    boolean includeDeleted, String siteName) {
        // Remove isDeleted elements unless includeDeleted.
        if (includeDeleted || nodeRefs == null) {
            return nodeRefs;
        }

        // initialize local variables
        ArrayList<NodeRef> correctedRefs = new ArrayList<>();
        ArrayList<NodeRef> deletedRefs = null;
        boolean workspaceMatters = !ignoreWorkspace && workspace != null;
        if (workspaceMatters) {
            deletedRefs = new ArrayList<NodeRef>();
        }

        // Remove from the results the nodes that are flagged as deleted since
        // we are not including deleted nodes. If the workspace matters, then we
        // need to remove nodes that are upstream from its deletion, so we keep
        // track of which are deleted in this case.
        for (NodeRef r : nodeRefs) {
            if (isDeleted(r)) {
                if (workspaceMatters) {
                    deletedRefs.add(r);
                }
            } else {
                correctedRefs.add(r);
            }
        }
        if (workspaceMatters) {
            // Remove from the results the nodes that are upstream from their
            // deletion.
            for (NodeRef deleted : deletedRefs) {
                EmsScriptNode dnode = new EmsScriptNode(deleted, getServices());
                String dId = dnode.getSysmlId(); // assumes cm_name as backup to
                                                 // sysmlid for non-model
                                                 // elements
                // Remove all nodes with the same ID in parent workspaces.
                ArrayList<NodeRef> correctedRefsCopy = new ArrayList<>(correctedRefs);
                for (NodeRef corrected : correctedRefsCopy) {
                    EmsScriptNode cnode = new EmsScriptNode(corrected, services);
                    String cId = cnode.getSysmlId();
                    // TODO -- REVIEW -- If the nodes are not model elements,
                    // and
                    // the cm:name is used, then the ids may not be the same
                    // across
                    // workspaces, in which case this fails.
                    if (dId.equals(cId)) {
                        // Remove the upstream deleted node. Pass true since it
                        // doesn't hurt to remove nodes that should have already
                        // been removed, and we want to make sure we don't trip
                        // over a deleted intermediate source.
                        if (isWorkspaceSource(corrected, deleted, true)) {
                            correctedRefs.remove(corrected);
                        }
                    }
                }
            }
        }
        nodeRefs = correctedRefs;

        return nodeRefs;
    }

    // protected static Map< String, Map< EmsScriptNode, Integer > > parentCache
    // =
    // new HashMap< String, Map< EmsScriptNode, Integer > >();
    //
    // protected static int parentDistance( NodeRef nodeRef, String
    // parentScopeName,
    // boolean cache ) {
    // EmsScriptNode parent = new EmsScriptNode( nodeRef, getServices() );
    // Integer cachedValue = Utils.get( parentCache, parentScopeName, parent );
    // if ( cache ) {
    // if ( cachedValue != null ) {
    // return cachedValue.intValue();
    // }
    // } else if ( cachedValue != null ) {
    // parentCache.remove( parentScopeName );
    // }
    // int distance = 0;
    // while ( parent != null ) {
    // if ( parent.getName().equals(parentScopeName) ) {
    // if ( cache ) {
    // Utils.put( parentCache, parentScopeName, parent, distance );
    // }
    // return distance;
    // }
    // parent = parent.getOwningParent( null ); // REVIEW -- need to pass in
    // timestamp?
    // ++distance;
    // }
    // return Integer.MAX_VALUE;
    // }

    protected static boolean isIdSearch(String prefix, boolean currentVal) {
        return (currentVal || SearchType.ID.prefix.equals(prefix) || SearchType.CM_NAME.prefix.equals(prefix));
    }

    protected static class CacheResults {
        public boolean useSimpleCache = false;
        public boolean useFullCache = false;
        public CacheUsed cachedUsed = CacheUsed.NONE;
        // public boolean usedSimpleCache = false;
        // public boolean usedFullCache = false;
        public boolean emptyEntriesInFullCacheOk = true;
        public ArrayList<NodeRef> results = null;
        public boolean knowIsIdSearch = false;
        public boolean knowIsNotIdSearch = false;
        public boolean exactMatch = true;
        protected String prefix;
        public boolean checkIsValidExactMatch = false;

        public CacheResults() {}

        public CacheResults(boolean useSimpleCache, boolean useFullCache, CacheUsed cacheUsed,
                        boolean emptyEntriesInFullCacheOk, ArrayList<NodeRef> results, String prefix,
                        boolean knowIsIdSearch, boolean knowIsNotIdSearch, boolean exactMatch) {
            super();
            this.useSimpleCache = useSimpleCache;
            this.useFullCache = useFullCache;
            this.cachedUsed = cacheUsed;
            this.emptyEntriesInFullCacheOk = emptyEntriesInFullCacheOk;
            this.results = results;
            this.knowIsIdSearch = knowIsIdSearch;
            this.knowIsNotIdSearch = knowIsNotIdSearch;
            this.exactMatch = exactMatch;
        }

        public boolean isIdSearch() {
            if (knowIsIdSearch)
                return true;
            if (knowIsNotIdSearch)
                return false;
            if (Utils.isNullOrEmpty(prefix))
                return false;
            if (NodeUtil.isIdSearch(prefix, false)) {
                knowIsIdSearch = true;
                return true;
            }
            knowIsNotIdSearch = true;
            return false;
        }

        boolean isExactMatch() {
            if (checkIsValidExactMatch)
                return exactMatch;
            if (exactMatch == false)
                return false;

            exactMatch = (knowIsIdSearch
                            || (!SearchType.ASPECT.prefix.equals(prefix) && !SearchType.TYPE.prefix.equals(prefix)));
            checkIsValidExactMatch = true;
            return exactMatch;
        }
    }

    // full cache
    public static ArrayList<NodeRef> putInFullCache(String specifier, String prefix, boolean ignoreWorkspaces,
                    WorkspaceNode workspace, boolean onlyThisWorkspace, Date dateTime, boolean justFirst,
                    boolean exactMatch, boolean includeDeleted, String siteName, ArrayList<NodeRef> results) {
        String wsId = getWorkspaceId(workspace, ignoreWorkspaces);
        Long dateLong = dateTime == null ? 0 : dateTime.getTime();
        return Utils.put(elementCache, specifier, prefix, wsId, onlyThisWorkspace, dateLong, justFirst, exactMatch,
                        includeDeleted, "" + siteName, results);
    }

    // full cache
    public static ArrayList<NodeRef> getFullCachedElements(String specifier, String prefix, boolean ignoreWorkspaces,
                    WorkspaceNode workspace, boolean onlyThisWorkspace, Date dateTime, boolean justFirst,
                    boolean exactMatch, boolean includeDeleted, String siteName) {
        String wsId = getWorkspaceId(workspace, ignoreWorkspaces);
        Long dateLong = dateTime == null ? 0 : dateTime.getTime();
        ArrayList<NodeRef> results = Utils.get(elementCache, specifier, prefix, wsId, onlyThisWorkspace, dateLong,
                        justFirst, exactMatch, includeDeleted, "" + siteName);
        return results;
    }

    public static enum CacheUsed {
        NONE, SIMPLE, FULL
    };

    /**
     * Whether the query options are applicable to using the simple element cache.
     *
     * @param prefix
     * @param knowIsIdSearch pass in true if it is known that the search is by id (see
     *        {@link #isIdSearch(String, boolean)}
     * @param ignoreWorkspace
     * @param workspace
     * @param dateTime
     * @param justFirst
     * @param includeDeleted
     * @param siteName
     * @param cacheResults
     * @return
     */
    public static boolean simpleCacheOkay(String prefix, boolean ignoreWorkspace, WorkspaceNode workspace,
                    Date dateTime, boolean justFirst, boolean includeDeleted, String siteName,
                    CacheResults cacheResults) {

        // Only use the simple cache if in the master workspace, just getting a
        // single node, not
        // looking for deleted nodes, and searching by cm:name or sysml:id.
        // Otherwise, we
        // may want multiple nodes in our results, or they could have changed
        // since we added
        // them to the cache:
        boolean useSimpleCache = doSimpleCaching && !ignoreWorkspace && !includeDeleted && workspace == null
                        && dateTime == null && justFirst && siteName == null && cacheResults.isIdSearch();
        return useSimpleCache;
    }

    /**
     * Whether the query options are applicable to using the full element cache.
     *
     * @param usingSimpleCache if using the simple cache, the full cache is not used
     * @param prefix the {@link SearchType} prefix
     * @param knowIsIdSearch pass in true if it is known that the search is by id (see
     *        {@link #isIdSearch(String, boolean)}
     * @param ignoreWorkspace
     * @param workspace
     * @param onlyThisWorkspace
     * @param dateInPast
     * @param siteName
     * @return
     */
    public static boolean fullCacheOkay(boolean usingSimpleCache, String prefix, boolean ignoreWorkspace,
                    WorkspaceNode workspace, boolean onlyThisWorkspace, boolean dateInPast, String siteName,
                    CacheResults cacheResults) {

        // Conditions under which the full cache can be used:
        // 1. If dateTime != null and dateTime < now then the cache may be
        // used for all other argument combinations.
        // 2. Otherwise, the cache may be used for finding nodes by cm:name
        // and sysml:id if ignoreWorkspace == false and either workspace
        // == null or onlyThisWorkspace == true.
        //
        // There are other things we can do to expand the applicability of
        // the cache, but they require more code changes.
        //
        // One idea is to track a lastModified time for the database. In
        // this case, we could store results for dateTime == null by
        // inserting dateTime == now in the cache and if there are results
        // for a dateTime after the lastModified time, then they are valid
        // (I think). We need to be careful not to pollute the cache with
        // entries for dateTime == null; a purge of these entries may be
        // necessary.
        boolean useFullCache = doFullCaching && !usingSimpleCache && (dateInPast
                        || (!ignoreWorkspace && (workspace == null || onlyThisWorkspace) && cacheResults.isIdSearch()));
        return useFullCache;
    }

    // both simple and full caches
    public static CacheResults getNodesInCache(String specifier, String prefix, boolean ignoreWorkspace,
                    WorkspaceNode workspace, boolean onlyThisWorkspace, Date dateTime, boolean justFirst,
                    boolean exactMatch, ServiceRegistry services, boolean includeDeleted, String siteName) {
        boolean useSimpleCache = false;
        boolean useFullCache = false;
        CacheUsed cacheUsed = CacheUsed.NONE;
        // boolean usedSimpleCache = false;
        // boolean usedFullCache = false;
        boolean emptyEntriesInFullCacheOk = false;
        ArrayList<NodeRef> results = null;
        CacheResults cacheResults = new CacheResults();
        cacheResults.prefix = prefix;
        cacheResults.exactMatch = exactMatch;
        if (doSimpleCaching || doFullCaching) {

            // boolean idSearch = false;
            boolean dateInPast = (dateTime != null && dateTime.before(new Date()));

            useSimpleCache = simpleCacheOkay(prefix, ignoreWorkspace, workspace, dateTime, justFirst, includeDeleted,
                            siteName, cacheResults);

            useFullCache = fullCacheOkay(useSimpleCache, prefix, ignoreWorkspace, workspace, onlyThisWorkspace,
                            dateInPast, siteName, cacheResults);

            emptyEntriesInFullCacheOk = addEmptyEntriesToFullCache && useFullCache && dateInPast;

            if (useSimpleCache && doSimpleCaching) {
                NodeRef ref = simpleCache.get(specifier);
                if (exists(ref)) {
                    results = Utils.newList(ref);
                    // usedSimpleCache = true;
                    cacheUsed = CacheUsed.SIMPLE;
                }
            } else if (doFullCaching && useFullCache) {
                results = getFullCachedElements(specifier, prefix, ignoreWorkspace, workspace, onlyThisWorkspace,
                                dateTime, justFirst, cacheResults.isExactMatch(), includeDeleted, siteName);
                if (results != null && (emptyEntriesInFullCacheOk || !results.isEmpty())) {
                    // usedFullCache = true;
                    cacheUsed = CacheUsed.FULL;
                }
            } else {
                cacheResults.isExactMatch();
            }
        }
        cacheResults.cachedUsed = cacheUsed;
        cacheResults.results = results;
        cacheResults.useFullCache = useFullCache;
        cacheResults.useSimpleCache = useSimpleCache;
        cacheResults.emptyEntriesInFullCacheOk = emptyEntriesInFullCacheOk;
        // CacheResults newCacheResults = new CacheResults( useSimpleCache,
        // useFullCache, cacheUsed, emptyEntriesInFullCacheOk, results, prefix,
        // cacheResults.knowIsIdSearch, cacheResults.knowIsNotIdSearch,
        // cacheResults.isExactMatch() );
        // newCacheResults.checkIsValidExactMatch =
        // cacheResults.checkIsValidExactMatch;
        return cacheResults;
    }

    // both simple and full caches
    public static CacheUsed putNodesInCache(ArrayList<NodeRef> nodeRefs, String specifier, String prefix,
                    boolean ignoreWorkspace, WorkspaceNode workspace, boolean onlyThisWorkspace, Date dateTime,
                    boolean justFirst, boolean exactMatch, boolean includeDeleted, String siteName,
                    boolean emptyEntriesInFullCacheOk, boolean useSimpleCache, boolean useFullCache) {
        CacheUsed cacheUsed = CacheUsed.NONE;
        // Update cache with results
        if ((doSimpleCaching || doFullCaching) // && caching
                        && nodeRefs != null && (emptyEntriesInFullCacheOk || !nodeRefs.isEmpty())) {
            if (useSimpleCache && doSimpleCaching) {
                // only put in cache if size is 1 (otherwise can be give bad
                // results)
                // force the user to filter
                if (nodeRefs != null && nodeRefs.size() == 1) {
                    NodeRef r = nodeRefs.get(0);
                    simpleCache.put(specifier, r);
                    cacheUsed = CacheUsed.SIMPLE;
                }
            } else if (useFullCache && doFullCaching) {
                putInFullCache(specifier, prefix, ignoreWorkspace, workspace, onlyThisWorkspace, dateTime, justFirst,
                                exactMatch, includeDeleted, siteName, nodeRefs);
                cacheUsed = CacheUsed.FULL;
            }
        }
        return cacheUsed;
    }

    /**
     * Adding an entry to the simple or full cache for the element (assumed to be current version)
     * so that findNodeRefById() will find it in the cache.
     *
     * @param node
     * @param siteName
     * @return
     */
    public static CacheUsed addElementToCache(EmsScriptNode node) {
        CacheUsed cacheUsed = CacheUsed.NONE;
        if (!exists(node, true)) {
            return cacheUsed;
        }
        ArrayList<NodeRef> nodeRefs = Utils.newList(node.getNodeRef());

        // Specify arguments such that when calling findNodeRefsById(), they
        // match those passed to findNodeRefsByType() and used to search the
        // cache.
        String specifier = node.getSysmlId();
        String prefix = SearchType.ID.prefix;
        boolean ignoreWorkspace = false;
        WorkspaceNode workspace = node.getWorkspace();
        boolean onlyThisWorkspace = false;
        Date dateTime = null; // TODO -- might want to check if node is from
                              // version store.
        boolean justFirst = true; // true and false
        boolean exactMatch = true;
        boolean includeDeleted = node.isDeleted();
        String siteName = null;
        boolean emptyEntriesInFullCacheOk = NodeUtil.addEmptyEntriesToFullCache;

        CacheResults cacheResults = new CacheResults(); // just for computing
                                                        // exactMatch in this
                                                        // case
        cacheResults.prefix = prefix;
        cacheResults.exactMatch = exactMatch;

        boolean useSimpleCache = simpleCacheOkay(prefix, ignoreWorkspace, workspace, dateTime, justFirst,
                        includeDeleted, siteName, cacheResults);
        boolean useFullCache = fullCacheOkay(useSimpleCache, prefix, ignoreWorkspace, workspace, onlyThisWorkspace,
                        false, siteName, cacheResults);
        cacheUsed = putNodesInCache(nodeRefs, specifier, prefix, ignoreWorkspace, workspace, onlyThisWorkspace,
                        dateTime, justFirst, cacheResults.isExactMatch(), includeDeleted, siteName,
                        emptyEntriesInFullCacheOk, useSimpleCache, useFullCache);
        // onlyThisWorkspace = !onlyThisWorkspace;
        //
        // onlyThisWorkspace = !onlyThisWorkspace;
        // justFirst = !justFirst;
        //
        // onlyThisWorkspace = !onlyThisWorkspace;
        return cacheUsed;
    }

    public static String getWorkspaceId(WorkspaceNode workspace, boolean ignoreWorkspaces) {
        if (ignoreWorkspaces)
            return "all";
        return getWorkspaceId(workspace);
    }

    public static String getWorkspaceId(WorkspaceNode workspace) {
        if (workspace == null)
            return "master";
        return workspace.getName();
    }

    public static WorkspaceNode getWorkspace(NodeRef nodeRef) {
        EmsScriptNode node = new EmsScriptNode(nodeRef, getServices());
        return node.getWorkspace();
    }

    public static boolean isWorkspaceAncestor(WorkspaceNode ancestor, WorkspaceNode child, boolean includeDeleted) {
        if (ancestor == null)
            return true;
        if (!exists(ancestor, includeDeleted) || !exists(child, includeDeleted))
            return false;
        WorkspaceNode parent = child.getParentWorkspace();
        if (!exists(parent, includeDeleted))
            return false;
        if (ancestor.equals(parent, true))
            return true;
        return isWorkspaceAncestor(ancestor, parent, includeDeleted);
    }

    /**
     * Determine whether the workspace of the source is an ancestor of that of the changed node.
     *
     * @param source
     * @param changed
     * @param includeDeleted whether to consider deleted workspaces
     * @return
     */
    public static boolean isWorkspaceAncestor(NodeRef source, NodeRef changed, boolean includeDeleted) {
        if (!exists(source, true) || !exists(changed, true))
            return false;
        WorkspaceNode ancestor = getWorkspace(source);
        WorkspaceNode child = getWorkspace(changed);
        return isWorkspaceAncestor(ancestor, child, includeDeleted);
    }

    public static boolean isWorkspaceSource(EmsScriptNode source, EmsScriptNode changed, boolean includeDeleted) {
        // TODO: removed exists so we can include ems:Deleted nodes in results,
        // may need to revisit
        // if (!exists(source) || !exists(changed)) return false;
        // if ( changed.equals( source ) ) return true;
        if (!exists(source, includeDeleted) || !exists(changed, includeDeleted))
            return false;
        if (!changed.hasAspect("ems:HasWorkspace"))
            return false;
        EmsScriptNode directSource = changed.getWorkspaceSource();
        // if ( !exists(directSource) ) return false;
        if (directSource == null || !directSource.scriptNodeExists())
            return false;
        // We pass true for tryCurrentVersions since they might be different
        // versions, and we don't care which versions.
        if (source.equals(directSource, true))
            return true;
        return isWorkspaceSource(source, directSource, includeDeleted);
    }

    public static boolean isWorkspaceSource(NodeRef source, NodeRef changed, boolean includeDeleted) {
        if (!exists(source, includeDeleted) || !exists(changed, includeDeleted))
            return false;
        // if ( source == null || changed == null ) return false;
        EmsScriptNode sourceNode = new EmsScriptNode(source, getServices());
        EmsScriptNode changedNode = new EmsScriptNode(changed, getServices());
        return isWorkspaceSource(sourceNode, changedNode, includeDeleted);
    }

    /**
     * Find a NodeReference by id (returns first match, assuming things are unique).
     *
     * @param id Node sysml:id or cm:name to search for
     * @param workspace
     * @param dateTime the time specifying which version of the NodeRef to find; null defaults to
     *        the latest version
     * @return NodeRef of first match, null otherwise
     */
    public static NodeRef findNodeRefById(String id, // String parentScopeName,
                    boolean ignoreWorkspace, WorkspaceNode workspace, Date dateTime, ServiceRegistry services,
                    boolean findDeleted) {
        return findNodeRefById(id, ignoreWorkspace, workspace, dateTime, services, findDeleted, null);
    }

    /**
     * Find a NodeReference by id (returns first match, assuming things are unique).
     *
     * @param id Node sysml:id or cm:name to search for
     * @param workspace
     * @param dateTime the time specifying which version of the NodeRef to find; null defaults to
     *        the latest version
     * @return NodeRef of first match, null otherwise
     */
    public static NodeRef findNodeRefById(String id, // String parentScopeName,
                    boolean ignoreWorkspace, WorkspaceNode workspace, Date dateTime, ServiceRegistry services,
                    boolean findDeleted, String siteName) {
        ArrayList<NodeRef> array = findNodeRefsById(id, ignoreWorkspace, workspace, dateTime, services, findDeleted,
                        true, siteName);

        return !Utils.isNullOrEmpty(array) ? array.get(0) : null;
    }

    /**
     * Find a NodeReferences by id
     *
     * @param id Node sysml:id or cm:name to search for
     * @param workspace
     * @param dateTime the time specifying which version of the NodeRef to find; null defaults to
     *        the latest version
     * @return Array of NodeRefs found or empty list
     */
    public static ArrayList<NodeRef> findNodeRefsById(String id, boolean ignoreWorkspace, WorkspaceNode workspace,
                    Date dateTime, ServiceRegistry services, boolean findDeleted, boolean justFirst) {

        return findNodeRefsById(id, ignoreWorkspace, workspace, dateTime, services, findDeleted, justFirst, null);
    }

    /**
     * Find a NodeReferences by id
     *
     * @param id Node sysml:id or cm:name to search for
     * @param workspace
     * @param dateTime the time specifying which version of the NodeRef to find; null defaults to
     *        the latest version
     * @return Array of NodeRefs found or empty list
     */
    public static ArrayList<NodeRef> findNodeRefsById(String id, boolean ignoreWorkspace, WorkspaceNode workspace,
                    Date dateTime, ServiceRegistry services, boolean findDeleted, boolean justFirst, String siteName) {

        ArrayList<NodeRef> returnArray = new ArrayList<>();
        ArrayList<NodeRef> array = findNodeRefsByType(id, SearchType.ID.prefix, ignoreWorkspace, workspace, dateTime,
                        justFirst, true, services, findDeleted, siteName);

        EmsScriptNode esn = null;
        if (!Utils.isNullOrEmpty(array)) {
            for (NodeRef r : array) {
                if (r != null) {
                    esn = new EmsScriptNode(r, getServices());
                }
                if (r == null || (!esn.exists() && !esn.isDeleted())) {
                    r = findNodeRefByType(id, SearchType.CM_NAME.prefix, ignoreWorkspace, workspace, dateTime, true,
                                    services, findDeleted, siteName);
                }
                returnArray.add(r);
            }
        } else {
            returnArray = findNodeRefsByType(id, SearchType.CM_NAME.prefix, ignoreWorkspace, workspace, dateTime,
                            justFirst, true, services, findDeleted, siteName);
        }

        return returnArray;
    }

    /**
     * Find a NodeReferences by sysml:name
     *
     * @param name
     * @param workspace
     * @param dateTime the time specifying which version of the NodeRef to find; null defaults to
     *        the latest version
     * @return Array of NodeRefs found or empty list
     */
    public static ArrayList<NodeRef> findNodeRefsBySysmlName(String name, boolean ignoreWorkspace,
                    WorkspaceNode workspace, Date dateTime, ServiceRegistry services, boolean findDeleted,
                    boolean justFirst) {

        ArrayList<NodeRef> returnArray = new ArrayList<>();
        ArrayList<NodeRef> array = findNodeRefsByType(name, SearchType.NAME.prefix, ignoreWorkspace, workspace,
                        dateTime, justFirst, true, services, findDeleted);

        if (!Utils.isNullOrEmpty(array)) {
            for (NodeRef r : array) {
                if (r != null) {
                    returnArray.add(r);
                }
            }
        }

        return returnArray;
    }

    /**
     * Find EmsScriptNodes by sysml:name
     *
     * @param name
     * @param workspace
     * @param dateTime the time specifying which version of the NodeRef to find; null defaults to
     *        the latest version
     * @return Array of EmsScriptNodes found or empty list
     */
    public static ArrayList<EmsScriptNode> findScriptNodesBySysmlName(String name, boolean ignoreWorkspace,
                    WorkspaceNode workspace, Date dateTime, ServiceRegistry services, boolean findDeleted,
                    boolean justFirst) {

        ArrayList<EmsScriptNode> returnArray = new ArrayList<>();
        ArrayList<NodeRef> array = findNodeRefsBySysmlName(name, ignoreWorkspace, workspace, dateTime, services,
                        findDeleted, justFirst);

        if (!Utils.isNullOrEmpty(array)) {
            for (NodeRef r : array) {
                if (r != null) {
                    returnArray.add(new EmsScriptNode(r, services));
                }
            }
        }

        return returnArray;
    }

    /**
     * Perform Lucene search for the specified pattern and ACM type TODO: Scope Lucene search by
     * adding either parent or path context
     *
     * @param type escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
     * @param pattern Pattern to look for
     */
    public static Map<String, EmsScriptNode> searchForElements(String pattern, boolean ignoreWorkspace,
                    WorkspaceNode workspace, Date dateTime, ServiceRegistry services, StringBuffer response,
                    Status status) {
        Map<String, EmsScriptNode> elementsFound = new HashMap<>();
        for (SearchType searchType : SearchType.values()) {
            elementsFound.putAll(searchForElements(searchType.prefix, pattern, ignoreWorkspace, workspace, dateTime,
                            services, response, status));
        }
        return elementsFound;
    }

    /**
     * Perform Lucene search for the specified pattern and ACM type TODO: Scope Lucene search by
     * adding either parent or path context
     *
     * @param type escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
     * @param pattern Pattern to look for
     */
    public static Map<String, EmsScriptNode> searchForElements(String type, String pattern, boolean ignoreWorkspace,
                    WorkspaceNode workspace, Date dateTime, ServiceRegistry services, StringBuffer response,
                    Status status, String siteName) {
        Map<String, EmsScriptNode> searchResults = new HashMap<>();

        // ResultSet resultSet = null;
        ArrayList<NodeRef> resultSet = null;
        // try {

        resultSet = findNodeRefsByType(pattern, type, /* false, */
                        ignoreWorkspace, workspace, dateTime, false, false, getServices(), false, siteName);
        for (NodeRef nodeRef : resultSet) {
            EmsScriptNode node = new EmsScriptNode(nodeRef, services, response);
            if (node.checkPermissions(PermissionService.READ, response, status)) {
                String id = node.getSysmlId();
                // We assume that order matters and that if two nodes have the
                // same id, then the first is preferred (for example, because it
                // is in the closest workspace).
                if (id != null && !searchResults.containsKey(id)) {
                    searchResults.put(id, node);
                }
            }
        }

        return searchResults;
    }

    /**
     * Perform Lucene search for the specified pattern and ACM type TODO: Scope Lucene search by
     * adding either parent or path context
     *
     * @param type escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
     * @param pattern Pattern to look for
     */
    public static Map<String, EmsScriptNode> searchForElements(String type, String pattern, boolean ignoreWorkspace,
                    WorkspaceNode workspace, Date dateTime, ServiceRegistry services, StringBuffer response,
                    Status status) {

        return searchForElements(type, pattern, ignoreWorkspace, workspace, dateTime, services, response, status, null);
    }

    /**
     * This method behaves the same as if calling {@link #isType(String, ServiceRegistry)} while
     * passing null as the ServiceRegistry.
     *
     * @param typeName
     * @return true if and only if a type is defined in the content model with a matching name.
     */
    public static boolean isType(String typeName) {
        return isType(typeName, null);
    }

    /**
     * Determine whether the input type name is a type in the content model (as opposed to an
     * aspect, for example).
     *
     * @param typeName
     * @param services
     * @return true if and only if a type is defined in the content model with a matching name.
     */
    public static boolean isType(String typeName, ServiceRegistry services) {
        if (typeName == null)
            return false;
        // quick and dirty - using DictionaryService results in WARNINGS
        typeName = typeName.replace("sysml:", "");
        if (typeName.equals("Element") || typeName.equals("Project")) {
            return true;
        }
        return false;
        // if ( Acm.getJSON2ACM().keySet().contains( typeName ) ) {
        // typeName = Acm.getJSON2ACM().get( typeName );
        // }
        // if ( services == null ) services = getServices();
        // DictionaryService dServ = services.getDictionaryService();
        // QName qName = createQName( typeName, services );
        // if ( qName != null ) {
        // // TODO: this prints out a warning, maybe better way to check?
        // TypeDefinition t = dServ.getType( qName );
        // if ( t != null ) {
        // return true;
        // }
        // }
        // if (Debug.isOn()) System.out.println("isType(" + typeName +
        // ") = false");

    }

    /**
     * This method behaves the same as if calling {@link #isAspect(String, ServiceRegistry)} while
     * passing null as the ServiceRegistry.
     *
     * @param aspectName
     * @return true if and only if an aspect is defined in the content model with a matching name.
     */
    public static boolean isAspect(String aspectName) {
        return isAspect(aspectName, null);
    }

    /**
     * Determine whether the input aspect name is an aspect in the content model (as opposed to a
     * type, for example).
     *
     * @param aspectName
     * @param services
     * @return true if and only if an aspect is defined in the content model with a matching name.
     */
    public static boolean isAspect(String aspectName, ServiceRegistry services) {
        AspectDefinition aspect = getAspect(aspectName, services);
        return aspect != null;
    }

    public static AspectDefinition getAspect(String aspectName, ServiceRegistry services) {
        if (Acm.getJSON2ACM().keySet().contains(aspectName)) {
            aspectName = Acm.getACM2JSON().get(aspectName);
        }
        if (services == null)
            services = getServices();
        DictionaryService dServ = services.getDictionaryService();

        QName qName = createQName(aspectName, services);
        if (qName != null) {
            AspectDefinition t = dServ.getAspect(qName);
            if (t != null) {
                if (logger.isTraceEnabled())
                    logger.trace("*** getAspect(" + aspectName + ") worked!!!");
                return t;
            }
        }

        Collection<QName> aspects = dServ.getAllAspects();
        if (logger.isTraceEnabled())
            logger.trace("all aspects: " + aspects);

        for (QName aspect : aspects) {
            if (aspect.getPrefixString().equals(aspectName)) {
                AspectDefinition t = dServ.getAspect(aspect);
                if (t != null) {
                    if (logger.isTraceEnabled())
                        logger.trace("*** getAspect(" + aspectName + ") returning " + t + "");
                    return t;
                }
            }
        }
        if (logger.isTraceEnabled())
            logger.trace("*** getAspect(" + aspectName + ") returning null");
        return null;

    }

    public static Collection<PropertyDefinition> getAspectProperties(String aspectName, ServiceRegistry services) {
        AspectDefinition aspect = getAspect(aspectName, services);
        if (aspect == null) {
            return Utils.newList();
        }
        Map<QName, PropertyDefinition> props = aspect.getProperties();
        if (props == null)
            return Utils.getEmptyList();
        return props.values();
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
     * Copied from {@link ScriptNode#createQName(QName)}.
     *
     * @param s Fully qualified or short-name QName string
     *
     * @return QName
     */
    public static QName createQName(String s) {
        return createQName(s, null);
    }

    /**
     * Helper to create a QName from either a fully qualified or short-name QName string
     * <P>
     * Copied from {@link ScriptNode#createQName(QName)}.
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

    // public static QName makeContentModelQName( String cmName ) {
    // return makeContentModelQName( cmName, null );
    // }
    // public static QName makeContentModelQName( String cmName, ServiceRegistry
    // services ) {
    // if ( services == null ) services = getServices();
    //
    // if ( cmName == null ) return null;
    // if ( Acm.getJSON2ACM().keySet().contains( cmName ) ) {
    // cmName = Acm.getACM2JSON().get( cmName );
    // }
    // String[] split = cmName.split( ":" );
    //
    // String nameSpace = null;
    // String localName = null;
    // if ( split.length == 2 ) {
    // nameSpace = split[0];
    // localName = split[1];
    // } else if ( split.length == 1 ) {
    // localName = split[0];
    // } else {
    // return null;
    // }
    // if ( localName == null ) {
    // return null;
    // }
    // DictionaryService dServ = services.getDictionaryService();
    // QName qName = null;
    // if ( nameSpace != null ) {
    // if ( nameSpace.equals( "sysml" ) ) {
    // qName = QName.createQName( "{http://jpl.nasa.gov/model/sysml-lite/1.0}"
    // + localName );
    // } else if ( nameSpace.equals( "view2" ) ) {
    // qName = QName.createQName( "{http://jpl.nasa.gov/model/view/2.0}"
    // + localName );
    // } else if ( nameSpace.equals( "view" ) ) {
    // qName = QName.createQName( "{http://jpl.nasa.gov/model/view/1.0}"
    // + localName );
    // }
    // }
    // return qName;
    // }

    /**
     * This method behaves the same as if calling
     * {@link #getContentModelTypeName(String, ServiceRegistry)} while passing null as the
     * ServiceRegistry.
     *
     * @param typeOrAspectName
     * @return the input type name if it matches a type; otherwise, return the Element type,
     *         assuming that all nodes are Elements
     */
    public static String getContentModelTypeName(String typeOrAspectName) {
        return getContentModelTypeName(typeOrAspectName, null);
    }

    /**
     * Get the type defined in the Alfresco content model. Return the input type name if it matches
     * a type; otherwise, return the Element type, assuming that all nodes are Elements.
     *
     * @param typeOrAspectName
     * @return
     */
    public static String getContentModelTypeName(String typeOrAspectName, ServiceRegistry services) {
        if (services == null)
            services = getServices();
        if (Acm.getJSON2ACM().keySet().contains(typeOrAspectName)) {
            typeOrAspectName = Acm.getJSON2ACM().get(typeOrAspectName);
        }
        String type = typeOrAspectName;
        boolean notType = !isType(type, services);
        if (notType) {
            type = Acm.ACM_ELEMENT;
        }
        return type;
    }

    /**
     * Get site of specified short name
     *
     * @param siteName
     * @return ScriptNode of site with name siteName
     */
    @Deprecated
    public static EmsScriptNode getSiteNode(String siteName, boolean ignoreWorkspace, WorkspaceNode workspace,
                    Date dateTime, ServiceRegistry services, StringBuffer response) {
        if (Utils.isNullOrEmpty(siteName))
            return null;

        EmsScriptNode siteNode = null;

        // Run this as admin user since there are permission issues popping up
        // String origUser = AuthenticationUtil.getRunAsUser();
        // AuthenticationUtil.setRunAsUser( "admin" );

        // Try to find the site in the workspace first.
        ArrayList<NodeRef> refs = findNodeRefsByType(siteName, SearchType.CM_NAME.prefix, ignoreWorkspace, workspace,
                        dateTime, true, true, getServices(), false);

        for (NodeRef ref : refs) {
            EmsScriptNode node = new EmsScriptNode(ref, services, response);
            if (node.isSite()) {
                siteNode = node;
                break;
            }
        }

        if (siteNode == null) {
            // Get the site from SiteService.
            SiteInfo siteInfo = services.getSiteService().getSite(siteName);
            if (siteInfo != null) {
                NodeRef siteRef = siteInfo.getNodeRef();
                if (dateTime != null) {
                    siteRef = getNodeRefAtTime(siteRef, dateTime);
                }
                if (siteRef != null) {
                    siteNode = new EmsScriptNode(siteRef, services, response);
                    if (siteNode != null && (workspace == null || workspace.contains(siteNode))) {
                        return siteNode;
                    }
                }
            }
        }

        // AuthenticationUtil.setRunAsUser(origUser);
        return siteNode;
    }

    /**
     * Get site of specified short name. This also checks that the returned node is in the specified
     * workspace, not just whether its in the workspace or any of its parents.
     *
     * @param siteName
     * @return ScriptNode of site with name siteName
     */
    public static EmsScriptNode getSiteNodeForWorkspace(String siteName, boolean ignoreWorkspace,
                    WorkspaceNode workspace, Date dateTime, ServiceRegistry services, StringBuffer response) {

        EmsScriptNode siteNode = getSiteNode(siteName, ignoreWorkspace, workspace, dateTime, services, response);
        return (siteNode != null && workspacesEqual(siteNode.getWorkspace(), workspace)) ? siteNode : null;
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

    public static Set<NodeRef> getRootNodes(ServiceRegistry services) {
        return services.getNodeService().getAllRootNodes(getStoreRef());
    }

    /**
     * Find an existing NodeRef with the input Alfresco id.
     *
     * @param id an id similar to workspace://SpacesStore/e297594b-8c24-427a-9342-35ea702b06ff
     * @return If there a node exists with the input id in the Alfresco database, return a NodeRef
     *         to it; otherwise return null. An error is printed if the id doesn't have the right
     *         syntax.
     */
    public static NodeRef findNodeRefByAlfrescoId(String id) {
        return findNodeRefByAlfrescoId(id, false);
    }

    public static NodeRef findNodeRefByAlfrescoId(String id, boolean includeDeleted) {
        return findNodeRefByAlfrescoId(id, includeDeleted, true);
    }

    public static NodeRef findNodeRefByAlfrescoId(String id, boolean includeDeleted, boolean giveError) {
        if (!id.contains("://")) {
            id = "workspace://SpacesStore/" + id;
        }
        if (!NodeRef.isNodeRef(id)) {
            if (giveError) {
                Debug.error("Bad NodeRef id: " + id);
            }
            return null;
        }
        NodeRef n = new NodeRef(id);
        EmsScriptNode node = new EmsScriptNode(n, getServices());
        if (!node.exists()) {
            if (includeDeleted && node.isDeleted()) {
                return n;
            } else {
                return null;
            }
        }
        return n;
    }

    public static class VersionLowerBoundComparator implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {
            Date d1 = null;
            Date d2 = null;
            if (o1 instanceof Version) {
                d1 = ((Version) o1).getFrozenModifiedDate();
            } else if (o1 instanceof Date) {
                d1 = (Date) o1;
            } else if (o1 instanceof Long) {
                d1 = new Date((Long) o1);
            } else if (o1 instanceof String) {
                d1 = TimeUtils.dateFromTimestamp((String) o1);
            }
            if (o2 instanceof Version) {
                d2 = ((Version) o2).getFrozenModifiedDate();
            } else if (o2 instanceof Date) {
                d2 = (Date) o2;
            } else if (o2 instanceof Long) {
                d2 = new Date((Long) o2);
            } else if (o1 instanceof String) {
                d2 = TimeUtils.dateFromTimestamp((String) o2);
            }
            // more recent first
            return -GenericComparator.instance().compare(d1, d2);
        }
    }

    public static VersionLowerBoundComparator versionLowerBoundComparator = new VersionLowerBoundComparator();

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
     * @param useFoundationalApi see {@link EmsScriptNode.useFoundationalApi}
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

    public static Object getPropertyAtTime(NodeRef nodeRef, String acmType, Date dateTime, ServiceRegistry services,
                    boolean cacheOkay) {
        EmsScriptNode node = new EmsScriptNode(nodeRef, services);
        Object result = node.getPropertyAtTime(acmType, dateTime, cacheOkay);
        return result;
    }

    public static NodeRef getNodeRefAtTime(NodeRef nodeRef, WorkspaceNode workspace, Date dateTime) {
        return getNodeRefAtTime(nodeRef, workspace, dateTime, false, false);
    }

    public static NodeRef getNodeRefAtTime(NodeRef nodeRef, WorkspaceNode workspace, Date dateTime,
                    boolean ignoreWorkspace, boolean findDeleted) {
        EmsScriptNode node = new EmsScriptNode(nodeRef, getServices());
        String id = node.getSysmlId();
        return findNodeRefById(id, ignoreWorkspace, workspace, dateTime, getServices(), findDeleted);
    }

    public static NodeRef getNodeRefAtTime(String id, WorkspaceNode workspace, Object timestamp) {
        Date dateTime = null;
        if (timestamp instanceof String) {
            dateTime = TimeUtils.dateFromTimestamp((String) timestamp);
        } else if (timestamp instanceof Date) {
            dateTime = (Date) timestamp;
        } else if (timestamp != null) {
            Debug.error("getNodeRefAtTime() was not expecting a timestamp of type "
                            + timestamp.getClass().getSimpleName());
        }
        NodeRef ref = findNodeRefById(id, false, workspace, dateTime, getServices(), false);
        // return getNodeRefAtTime( ref, timestamp );
        return ref;
    }

    public static NodeRef getNodeRefAtTime(NodeRef ref, String timestamp) {
        Date dateTime = TimeUtils.dateFromTimestamp(timestamp);
        return getNodeRefAtTime(ref, dateTime);
    }

    /**
     * Get the version of the NodeRef that was the most current at the specified date/time.
     *
     * @param ref some version of the NodeRef
     * @param dateTime the date/time, specifying the version
     * @return the version of the NodeRef that was the latest version at the specified time, or if
     *         before any version
     */
    public static NodeRef getNodeRefAtTime(NodeRef ref, Date dateTime) {
        if (Debug.isOn())
            Debug.outln("getNodeRefAtTime( " + ref + ", " + dateTime + " )");
        if (!NodeUtil.exists(ref) && !NodeUtil.isDeleted(ref)) {
            return null;
        }
        if (ref == null || dateTime == null) {
            return ref;
        }

        // check cache
        if (doNodeAtTimeCaching) {
            Object o = nodeAtTimeCacheGet(ref, dateTime);
            if (o == NULL_OBJECT)
                return null;
            if (o != null) {
                try {
                    return (NodeRef) o;
                } catch (ClassCastException e) {
                    e.printStackTrace();
                }
            }
        }

        EmsScriptNode esn = new EmsScriptNode(ref, services);

        // logger.warn( "getting history for " + esn.getSysmlId() + " - " +
        // esn.getSysmlName() + " at time " + dateTime );

        // Check cache for version history
        // Collection< Version > history = versionHistoryCache.get( ref );
        VersionHistory history = null;
        if (doVersionHistoryCaching)
            history = versionHistoryCache.get(ref);
        if (history != null) {
            // Check if the history is potentially out of date with respect to
            // the
            // date passed in; i.e. check if the date is after the latest in the
            // history.
            Version latest = history.getHeadVersion();
            if (latest == null || latest.getFrozenModifiedDate() == null
                            || latest.getFrozenModifiedDate().before(dateTime)) {
                history = null;
            }
        }
        if (history == null) {
            // VersionHistory
            history = getServices().getVersionService().getVersionHistory(ref);
            if (doVersionHistoryCaching)
                versionHistoryCache.put(ref, history);
        }

        // logger.warn( "got history for " + esn.getSysmlId() + " - " +
        // esn.getSysmlName() + " at time " + dateTime );

        if (history == null) {
            // Versioning doesn't make versions until the first save...
            Date createdTime = (Date) esn.getProperty("cm:created");
            if (dateTime != null && createdTime != null && dateTime.compareTo(createdTime) < 0) {
                if (Debug.isOn())
                    Debug.outln("no history! dateTime " + dateTime + " before created " + createdTime);
                if (doNodeAtTimeCaching) {
                    nodeAtTimeCachePut(ref, dateTime, null);
                }
                return null;
            }
            if (Debug.isOn() && createdTime != null)
                Debug.outln("no history! created " + createdTime);
            // Can't cache until history is created. :-(
            // On second thought, let's cache if this node is from the version
            // store :-)
            // if ( doNodeAtTimeCaching ) {
            // if ( ref.getStoreRef() !=
            // StoreRef.STORE_REF_WORKSPACE_SPACESSTORE ) {
            // nodeAtTimeCachePut( ref, dateTime, null );
            // }
            // }
            return ref;
        }

        Collection<Version> versions = history.getAllVersions();
        Vector<Version> vv = new Vector<>(versions);
        if (Debug.isOn())
            Debug.outln("versions = " + vv);
        if (Utils.isNullOrEmpty(vv)) {
            // TODO - throw error?!
            // Don't cache an error.
            return null;
        }
        int index = Collections.binarySearch(vv, dateTime, versionLowerBoundComparator);
        if (Debug.isOn())
            Debug.outln("binary search returns index " + index);
        Version version = null;
        if (index < 0) {
            // search returned index = -(lowerBound+1), so lowerbound = -index-1
            index = -index - 1;
            if (Debug.isOn())
                Debug.outln("index converted to lowerbound " + index);
        }
        // This is to work around what looks like a bug in Java's binarysearch.
        if (index < 0) {// || index == vv.size() ) {
            version = vv.get(0);
            if (version != null) {
                Date d = version.getFrozenModifiedDate();
                if (Debug.isOn())
                    Debug.outln("first frozen modified date " + d);
                if (d != null && d.after(dateTime)) {
                    // // The date is before the first version, so return null;
                    // if ( true ) {
                    // if ( doNodeAtTimeCaching ) {
                    // nodeAtTimeCachePut( ref, dateTime, null );
                    // }
                    // return null;
                    // }
                    NodeRef fnr = version.getFrozenStateNodeRef();
                    if (Debug.isOn())
                        Debug.outln("returning first frozen node ref " + fnr);
                    if (doNodeAtTimeCaching) {
                        nodeAtTimeCachePut(ref, dateTime, fnr);
                    }
                    return fnr;
                }
            }
            if (index == vv.size()) {
                // In this case the date may be later than all
                // versions, in which case returning null seems to work at
                // present.
                // TODO -- Don't we need to include the current SpacesStore live
                // node ref with it's modified time since there will be no
                // version for it?
                if (Debug.isOn())
                    Debug.outln("Date is later than all versions--not sure if it makes sense to return the last version or refer to the latest in memory.  Getting the ");
                // Don't cache an error.
                return null;
            }
            // TODO -- throw error?!
            if (Debug.isOn())
                Debug.outln("version is null; returning null!");
            // Don't cache an error.
            return null;
            // } else if ( index == vv.size() ) {
            // // Time is later than all versions, so get the latest.
            // // The line below is commented out because binarysearch returns
            // the
            // // wrong index for cases where the date is earlier than all
            // // versions.
            // // version = vv.get( index - 1 );
            // // TODO -- don't we need to include the current SpacesStore live
            // // node ref with it's modified time since there will be no
            // // version for it?
            // // For now, returning null seems to help.
            // return null;
        } else if (index >= vv.size()) {
            if (Debug.isOn())
                Debug.outln("index is too large, outside bounds!");
            // TODO -- throw error?!
            // Don't cache an error.
            return null;
        } else {
            version = vv.get(index);
        }
        if (Debug.isOn()) {
            if (Debug.isOn())
                Debug.outln("picking version " + version);
            if (Debug.isOn())
                Debug.outln("version properties " + version.getVersionProperties());
            String versionLabel = version.getVersionLabel();
            EmsScriptNode emsNode = new EmsScriptNode(ref, getServices());
            ScriptVersion scriptVersion = emsNode.getVersion(versionLabel);
            if (Debug.isOn())
                Debug.outln("scriptVersion " + scriptVersion);
            ScriptNode scriptVersionNode = scriptVersion.getNode();
            if (Debug.isOn())
                Debug.outln("script node " + scriptVersionNode);
            // can't get script node properties--generates exception
            // if (Debug.isOn()) Debug.outln( "script node properties " +
            // node.getProperties() );
            NodeRef scriptVersionNodeRef = scriptVersion.getNodeRef();
            if (Debug.isOn())
                Debug.outln("ScriptVersion node ref " + scriptVersionNodeRef);
            NodeRef vnr = version.getVersionedNodeRef();
            if (Debug.isOn())
                Debug.outln("versioned node ref " + vnr);
        }
        NodeRef fnr = version.getFrozenStateNodeRef();
        if (Debug.isOn())
            Debug.outln("frozen node ref " + fnr);
        if (Debug.isOn())
            Debug.outln("frozen node ref properties: " + getServices().getNodeService().getProperties(fnr));
        if (Debug.isOn())
            Debug.outln("frozen node ref " + getServices().getNodeService().getProperties(fnr));

        if (Debug.isOn())
            Debug.outln("returning frozen node ref " + fnr);

        if (doNodeAtTimeCaching) {
            nodeAtTimeCachePut(ref, dateTime, fnr);
        }
        return fnr;
    }

    /**
     * Find or create a folder
     *
     * @param source node within which folder will have been created; may be null
     * @param path folder path as folder-name1 + '/' + folder-name2 . . .
     * @return the existing or new folder
     */
    public static EmsScriptNode mkdir(EmsScriptNode source, String path, ServiceRegistry services,
                    StringBuffer response, Status status) {
        EmsScriptNode result = null;
        // if no source is specified, see if path include the site
        if (source == null) {
            Pattern p = Pattern.compile("(Sites)?/?(\\w*)");
            Matcher m = p.matcher(path);
            if (m.matches()) {
                String siteName = m.group(1);
                source = getSiteNode(siteName, true, null, null, services, response);
                if (source != null) {
                    result = mkdir(source, path, services, response, status);
                    if (result != null)
                        return result;
                    source = null;
                }
            }
        }
        // if no source is identified, see if path is from the company home
        if (source == null) {
            source = getCompanyHome(services);
            if (source != null) {
                result = mkdir(source, path, services, response, status);
                if (result != null)
                    return result;
                source = null;
            }
        }
        // if no source is identified, see if path is from a root node
        if (source == null) {
            Set<NodeRef> roots = getRootNodes(services);
            for (NodeRef ref : roots) {
                source = new EmsScriptNode(ref, services, response, status);
                result = mkdir(source, path, services, response, status);
                if (result != null)
                    return result;
                source = null;
            }
        }
        if (source == null) {
            log("Can't determine source node of path " + path + "!", response);
            return null;
        }
        // find the folder corresponding to the path beginning from the source
        // node
        EmsScriptNode folder = source.childByNamePath(path);
        if (folder == null) {
            String[] arr = path.split("/");
            for (String p : arr) {
                folder = source.childByNamePath(p);
                if (folder == null) {
                    folder = source.createFolder(p);
                    source.getOrSetCachedVersion();
                    if (folder == null) {
                        log("Can't create folder for path " + path + "!", response);
                        return null;
                    } else {
                        folder.getOrSetCachedVersion();
                    }
                }
                source = folder;
            }
        }
        return folder;
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

    public static NodeRef getNodeRefFromNodeId(String id) {
        return getNodeRefFromNodeId("workspace://SpacesStore/", id);
    }

    public static EmsScriptNode getVersionAtTime(EmsScriptNode element, Date dateTime) {
        EmsScriptNode versionedNode = null;
        NodeRef ref = getNodeRefAtTime(element.getNodeRef(), dateTime);
        if (ref != null) {
            versionedNode = new EmsScriptNode(ref, element.getServices());
        }
        return versionedNode;
    }

    public static Collection<EmsScriptNode> getVersionAtTime(Collection<EmsScriptNode> moreElements, Date dateTime) {
        ArrayList<EmsScriptNode> elements = new ArrayList<>();
        for (EmsScriptNode n : moreElements) {
            EmsScriptNode versionedNode = getVersionAtTime(n, dateTime);
            if (versionedNode != null) {
                elements.add(versionedNode);
            }
        }
        return elements;
    }

    public static String getVersionedRefId(EmsScriptNode n) {
        String versionString = n.getNodeRef().toString();
        Version headVersionNode = n.getHeadVersion();
        if (headVersionNode != null) {
            NodeRef versionNode = headVersionNode.getFrozenStateNodeRef();
            if (versionNode != null)
                versionString = versionNode.toString();
        }
        return versionString;
    }

    public static boolean isDeleted(EmsScriptNode node) {
        if (node == null)
            return false;
        return node.isDeleted();
    }

    public static boolean isDeleted(NodeRef ref) {
        if (ref == null)
            return false;
        EmsScriptNode node = new EmsScriptNode(ref, getServices());
        return node.isDeleted();
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

    public static boolean scriptNodeExists(NodeRef ref) {
        if (ref == null)
            return false;
        EmsScriptNode node = new EmsScriptNode(ref, getServices());
        return node.scriptNodeExists();
    }

    public static String getUserName() {
        String userName = AuthenticationUtil.getRunAsUser();
        return userName;
    }

    public static EmsScriptNode getUserHomeFolder() {
        String userName = getUserName();
        return getUserHomeFolder(userName);
    }

    public static EmsScriptNode getUserHomeFolder(String userName) {
        return getUserHomeFolder(userName, false);
    }

    public static EmsScriptNode getUserHomeFolder(String userName, boolean createIfNotFound) {
        NodeRef homeFolderNode = null;
        EmsScriptNode homeFolderScriptNode = null;
        if (userName.equals("admin")) {
            homeFolderNode = findNodeRefByType(userName, SearchType.CM_NAME, /*
                                                                              * true,
                                                                              */
                            true, null, null, true, getServices(), false);
        } else {
            PersonService personService = getServices().getPersonService();
            NodeService nodeService = getServices().getNodeService();
            NodeRef personNode = personService.getPerson(userName);
            homeFolderNode = (NodeRef) getNodeProperty(personNode, ContentModel.PROP_HOMEFOLDER, getServices(), true,
                            true);
        }
        if (homeFolderNode == null || !exists(homeFolderNode)) {
            NodeRef ref = findNodeRefById("User Homes", true, null, null, getServices(), false);
            EmsScriptNode homes = new EmsScriptNode(ref, getServices());
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

    // REVIEW -- should this be in AbstractJavaWebScript?
    public static String createId(ServiceRegistry services) {
        for (int i = 0; i < 10; ++i) {
            String id = "MMS_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString();
            // Make sure id is not already used (extremely unlikely)
            if (findNodeRefById(id, true, null, null, services, false) == null) {
                return id;
            }
        }
        Debug.error(true, "Could not create a unique id!");
        return null;
    }

    public static Set<QName> getAspects(NodeRef ref) {
        EmsScriptNode node = new EmsScriptNode(ref, getServices());
        return node.getAspectsSet();
    }

    public static List<String> qNamesToStrings(Collection<QName> qNames) {
        List<String> names = new ArrayList<>();
        for (QName qn : qNames) {
            names.add(qn.toPrefixString());
        }
        return names;
    }

    public static Set<String> getNames(Collection<NodeRef> refs) {
        List<EmsScriptNode> nodes = EmsScriptNode.toEmsScriptNodeList(refs);
        TreeSet<String> names = new TreeSet<String>(EmsScriptNode.getNames(nodes));
        return names;
    }

    public static Set<String> getSysmlIds(Collection<NodeRef> refs) {
        List<EmsScriptNode> nodes = EmsScriptNode.toEmsScriptNodeList(refs);
        TreeSet<String> names = new TreeSet<>(EmsScriptNode.getSysmlIds(nodes));
        return names;
    }

    public static String getName(NodeRef ref) {
        if (ref == null)
            return null;
        EmsScriptNode node = new EmsScriptNode(ref, getServices());
        return node.getName();
    }

    public static String getSysmlId(NodeRef ref) {
        if (ref == null)
            return null;
        EmsScriptNode node = new EmsScriptNode(ref, getServices());
        return node.getSysmlId();
    }

    public static Set<NodeRef> getModelElements(Set<NodeRef> s1) {
        Set<NodeRef> newSet1 = new LinkedHashSet<>();
        if (s1 != null) {
            for (NodeRef ref : s1) {
                if (EmsScriptNode.isModelElement(ref)) {
                    newSet1.add(ref);
                }
            }
        }
        return newSet1;
    }

    public static List<NodeRef> getNodeRefs(Collection<EmsScriptNode> nodes, boolean checkReadPermissions) {
        List<NodeRef> refs = new ArrayList<>();
        for (EmsScriptNode node : nodes) {
            if (!checkReadPermissions || node.checkPermissions(PermissionService.READ)) {
                NodeRef ref = node.getNodeRef();
                if (ref != null)
                    refs.add(ref);
            }
        }
        return refs;
    }

    public static EmsScriptNode findScriptNodeById(String id, WorkspaceNode workspace, Date dateTime,
                    boolean findDeleted,
                    // Map< String,
                    // EmsScriptNode >
                    // simpleCache,
                    ServiceRegistry services, StringBuffer response) {

        return findScriptNodeById(id, workspace, dateTime, findDeleted, services, response, null);
    }

    public static EmsScriptNode findScriptNodeById(String id, WorkspaceNode workspace, Date dateTime,
                    boolean findDeleted,
                    // Map< String,
                    // EmsScriptNode >
                    // simpleCache,
                    ServiceRegistry services, StringBuffer response, String siteName) {

        timer = Timer.startTimer(timer, timeEvents);
        NodeRef nodeRef = findNodeRefById(id, false, workspace, dateTime, services, findDeleted, siteName);
        if (nodeRef == null) {
            Timer.stopTimer(timer, "====== findScriptNodeById(): failed end time", timeEvents);
            return null;
        }
        Timer.stopTimer(timer, "====== findScriptNodeById(): end time", timeEvents);
        return new EmsScriptNode(nodeRef, services);
    }

    public static EmsScriptNode findScriptNodeByIdForWorkspace(String id, WorkspaceNode workspace, Date dateTime,
                    boolean findDeleted, ServiceRegistry services, StringBuffer response) {

        EmsScriptNode node = findScriptNodeById(id, workspace, dateTime, findDeleted, services, response);
        return (node != null && workspacesEqual(node.getWorkspace(), workspace)) ? node : null;

    }

    /**
     * Returns true if the passed workspaces are equal, checks for master (null) workspaces also
     *
     * @param ws1
     * @param ws2
     * @return
     */
    public static boolean workspacesEqual(WorkspaceNode ws1, WorkspaceNode ws2) {
        return ((ws1 == null && ws2 == null) || (ws1 != null && ws1.equals(ws2)));
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
     * @param targetSiteName
     * @param subfolderName
     * @param workspace
     * @param dateTime
     * @param updateIfFound
     * @param response
     * @param status
     * @return
     */
    public static EmsScriptNode updateOrCreateArtifact(String name, String type, String base64content,
                    String strContent, String targetSiteName, String subfolderName, WorkspaceNode workspace,
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
        ArrayList<NodeRef> refs = findNodeRefsByType("" + cs, SearchType.CHECKSUM.prefix, false, workspace, dateTime,
                        false, false, services, false);
        // ResultSet existingArtifacts =
        // NodeUtil.findNodeRefsByType( "" + cs, SearchType.CHECKSUM,
        // services );
        // Set< EmsScriptNode > nodeSet = toEmsScriptNodeSet( existingArtifacts
        // );
        List<EmsScriptNode> nodeList = EmsScriptNode.toEmsScriptNodeList(refs, services, response, status);
        // existingArtifacts.close();

        EmsScriptNode matchingNode = null;

        if (nodeList != null && nodeList.size() > 0) {
            matchingNode = nodeList.iterator().next();
        }

        // No need to update if the checksum and name match (even if it is in a
        // parent branch):
        if (matchingNode != null && (ignoreName || matchingNode.getSysmlId().equals(artifactId))) {
            return matchingNode;
        }

        // Create new artifact:
        // find subfolder in site or create it
        String artifactFolderName = Utils.isNullOrEmpty(subfolderName) ? "" : "/" + subfolderName;

        EmsScriptNode targetSiteNode =
                        getSiteNodeForWorkspace(targetSiteName, false, workspace, dateTime, services, response);

        // find site; it must exist!
        if (targetSiteNode == null || !targetSiteNode.exists()) {
            Debug.err("Can't find node for site: " + targetSiteName + "!\n");
            return null;
        }

        // find or create subfolder
        EmsScriptNode subfolder = mkdir(targetSiteNode, artifactFolderName, services, response, status);
        if (subfolder == null || !subfolder.exists()) {
            Debug.err("Can't create subfolder for site, " + targetSiteName + ", in artifact folder, "
                            + artifactFolderName + "!\n");
            return null;
        }

        // find or create node:
        artifactNode = findScriptNodeByIdForWorkspace(artifactId, workspace, dateTime, false, services, response);

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
			String targetSiteName, String subfolderName,
			WorkspaceNode workspace, Date dateTime, StringBuffer response,
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
		ArrayList<NodeRef> refs = findNodeRefsByType("" + cs,
				SearchType.CHECKSUM.prefix, false, workspace, dateTime, false,
				false, services, false);
		// ResultSet existingArtifacts =
		// NodeUtil.findNodeRefsByType( "" + cs, SearchType.CHECKSUM,
		// services );
		// Set< EmsScriptNode > nodeSet = toEmsScriptNodeSet( existingArtifacts
		// );
		List<EmsScriptNode> nodeList = EmsScriptNode.toEmsScriptNodeList(refs,
				services, response, status);
		// existingArtifacts.close();

		EmsScriptNode matchingNode = null;

		if (nodeList != null && nodeList.size() > 0) {
			matchingNode = nodeList.iterator().next();
		}

		// No need to update if the checksum and name match (even if it is in a
		// parent branch):
		if (matchingNode != null
				&& (ignoreName || matchingNode.getSysmlId().equals(artifactId))) {
			return matchingNode;
		}

		// Create new artifact:
		// find subfolder in site or create it
		String artifactFolderName = Utils.isNullOrEmpty(subfolderName) ? "" : "/" + subfolderName;

		EmsScriptNode targetSiteNode = getSiteNodeForWorkspace(targetSiteName,
				false, workspace, dateTime, services, response);

		// find site; it must exist!
		if (targetSiteNode == null || !targetSiteNode.exists()) {
			Debug.err("Can't find node for site: " + targetSiteName + "!\n");
			return null;
		}

		// find or create subfolder
		EmsScriptNode subfolder = mkdir(targetSiteNode, artifactFolderName,
				services, response, status);
		if (subfolder == null || !subfolder.exists()) {
			Debug.err("Can't create subfolder for site, " + targetSiteName
					+ ", in artifact folder, " + artifactFolderName + "!\n");
			return null;
		}

		// find or create node:
		pngNode = findScriptNodeByIdForWorkspace(artifactId, workspace,
				dateTime, false, services, response);

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

    /**
     * Given a parent and path, builds the path recursively as necessary
     *
     * @param parent
     * @param path
     * @return
     */
    public static EmsScriptNode getOrCreatePath(EmsScriptNode parent, String path) {
        if (parent == null)
            return null;
        String tokens[] = path.split("/");

        String childPath = null;
        for (int ii = 0; ii < tokens.length; ii++) {
            if (!tokens[ii].isEmpty()) {
                childPath = tokens[ii];
                break;
            }
        }

        if (childPath != null) {
            EmsScriptNode child = parent.childByNamePath(childPath);
            if (child == null) {
                child = parent.createFolder(childPath);
                if (child != null)
                    child.getOrSetCachedVersion();
                parent.getOrSetCachedVersion();
            }

            if (child != null) {
                if (path.startsWith("/")) {
                    if (path.length() >= 1) {
                        path = path.substring(1);
                    }
                }
                path = path.replaceFirst(childPath, "");
                if (!path.isEmpty()) {
                    return getOrCreatePath(child, path);
                } else {
                    return child;
                }
            }
        }

        return null;
    }

    /**
     * Get or create the date folder based on /year/month/day_of_month provided a parent folder
     *
     * @param parent
     * @return
     */
    public static EmsScriptNode getOrCreateDateFolder(EmsScriptNode parent) {
        Calendar cal = Calendar.getInstance();

        String year = Integer.toString(cal.get(Calendar.YEAR));
        String month = Integer.toString(cal.get(Calendar.MONTH) + 1);
        String day = Integer.toString(cal.get(Calendar.DAY_OF_MONTH));

        String path = String.format("/%s/%s/%s", year, month, day);
        return getOrCreatePath(parent, path);
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

    public static void transactionCheck(Log logger, EmsScriptNode node) {
        // logger.error( "inTransaction = " + NodeUtil.isInsideTransactionNow()
        // );
        if (NodeUtil.isInsideTransactionNow()) {
            if (NodeUtil.hasBeenOutsideTransaction()) {
                Exception e = new Exception();
                logger.error("In transaction when have been outside! " + node, e);
                logger.error("Stack trace when last outside transaction:\n"
                                + Utils.toString(getOutsideTransactionStackTrace()));
            }
            NodeUtil.setBeenInsideTransaction(true);
            setInsideTransactionStackTrace();
        } else {
            if (NodeUtil.hasBeenInsideTransaction()) {
                Exception e = new Exception();
                logger.error("Outside transaction when have been inside! " + node, e);
                logger.error("Stack trace when last inside transaction:\n"
                                + Utils.toString(getInsideTransactionStackTrace()));
            }
            NodeUtil.setBeenOutsideTransaction(true);
            setOutsideTransactionStackTrace();
        }
    }

    public static void transactionCheck(Logger logger, EmsScriptNode node) {
        // logger.error( "inTransaction = " + NodeUtil.isInsideTransactionNow()
        // );
        if (NodeUtil.isInsideTransactionNow()) {
            if (NodeUtil.hasBeenOutsideTransaction()) {
                Exception e = new Exception();
                logger.error("In transaction when have been outside! " + node, e);
                logger.error("Stack trace when last outside transaction:\n"
                                + Utils.toString(getOutsideTransactionStackTrace()));
            }
            NodeUtil.setBeenInsideTransaction(true);
            setInsideTransactionStackTrace();
        } else {
            if (NodeUtil.hasBeenInsideTransaction()) {
                Exception e = new Exception();
                logger.error("Outside transaction when have been inside! " + node, e);
                logger.error("Stack trace when last inside transaction:\n"
                                + Utils.toString(getInsideTransactionStackTrace()));
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

    /**
     * Adds embedded values specs in node if present to the nodes set.
     *
     * @param node Node to check for embedded value spec
     * @param nodes Adds found value spec nodes to this set
     * @param services
     */
    public static void addEmbeddedValueSpecs(NodeRef ref, Set<NodeRef> nodes, ServiceRegistry services, Date dateTime,
                    WorkspaceNode ws) {

        if (ref != null) {

            EmsScriptNode node = new EmsScriptNode(ref, services);
            ArrayList<NodeRef> children = node.getValueSpecOwnedChildren(true, dateTime, ws);

            if (!Utils.isNullOrEmpty(children)) {
                nodes.addAll(children);
            }
            // The following is for backwards compatibility:
            else {

                children = node.getOwnedChildren(true, dateTime, ws);

                // No point of checking this if there are no children at all:
                if (!Utils.isNullOrEmpty(children)) {
                    Object propVal;
                    for (String acmType : Acm.TYPES_WITH_VALUESPEC.keySet()) {
                        // It has a apsect that has properties that map to value
                        // specs:
                        if (node.hasOrInheritsAspect(acmType)) {

                            for (String acmProp : Acm.TYPES_WITH_VALUESPEC.get(acmType)) {
                                propVal = node.getNodeRefProperty(acmProp, dateTime, ws);

                                if (propVal != null) {
                                    // Note: We want to include deleted nodes
                                    // also, so no need to check for that
                                    if (propVal instanceof NodeRef) {
                                        NodeRef propValRef = (NodeRef) propVal;
                                        if (NodeUtil.scriptNodeExists(propValRef)) {
                                            nodes.add(propValRef);
                                        }
                                    } else if (propVal instanceof List) {
                                        @SuppressWarnings("unchecked")
                                        List<NodeRef> nrList = (ArrayList<NodeRef>) propVal;
                                        for (NodeRef propValRef : nrList) {
                                            if (propValRef != null && NodeUtil.scriptNodeExists(propValRef)) {
                                                nodes.add(propValRef);
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }

            } // ends else

        } // ends if (ref != null)
    }

    /**
     * Retrieves the LDAP group that is given permision to do workspace operations Looks in
     * companyhome/MMS/branch_perm. Returns default LDAP group if no node is found.
     *
     */
    public static String getWorkspaceLdapGroup() {

        String ldapGroup = null;
        EmsScriptNode context = NodeUtil.getCompanyHome(services);

        if (context != null) {
            EmsScriptNode mmsFolder = context.childByNamePath("MMS");

            if (mmsFolder != null) {
                EmsScriptNode branchPermNode = mmsFolder.childByNamePath("branch_perm");

                if (branchPermNode != null) {
                    ldapGroup = (String) branchPermNode.getProperty("ems:ldapGroup");
                }
            }
        }

        return Utils.isNullOrEmpty(ldapGroup) ? "mbee-dev-admin" : ldapGroup;
    }

    /**
     * Returns true if the user is part of the LDAP group that has permissions to perform workspace
     * operations.
     *
     * @return
     */
    public static boolean userHasWorkspaceLdapPermissions() {

        if (skipWorkspacePermissionCheck) {
            return true;
        }

        String ldapGroup = getWorkspaceLdapGroup();
        String user = NodeUtil.getUserName();

        if (!Utils.isNullOrEmpty(user)) {

            // Get all the groups (authorities) for the user:
            List<String> authorityNames = NodeUtil.getUserGroups(user);

            for (String group : authorityNames) {
                // Check against default alfresco admin group:
                if (group.equals("GROUP_ALFRESCO_ADMINISTRATORS")) {
                    return true;
                }

                // Check against LDAP group:
                if (group.equals("GROUP_" + ldapGroup)) {
                    return true;
                }
            }

        }

        return false;
    }

    /**
     * This is an experiment to see if deadlock can occur with concurrent transactions. Launch two
     * threads, each starting a transaction in parallel. Thread 1 writes to node 1, waits a few
     * seconds, and then writes to node 2. At the same time, thread 2 writes to node 2, waits a few
     * seconds, and then writes to node 1.
     */
    public static void concurrencyTest() {

    }

    public static String getHostname() {
        return services.getSysAdminParams().getAlfrescoHost();
    }

    public static EmsScriptNode getOrCreateContentNode(EmsScriptNode parent, String cmName, ServiceRegistry services) {
        // See if node already exists.
        EmsScriptNode node = parent.childByNamePath(cmName);

        if (!exists(node)) {
            // ArrayList<NodeRef> nodeRefs = findNodeRefsByType( cmName,
            // SearchType.CM_NAME.prefix, services );
            // if (nodeRefs != null && nodeRefs.size() == 1) {
            // node = new EmsScriptNode(nodeRefs.get( 0 ), services, new
            // StringBuffer());
            // } else if ( Utils.isNullOrEmpty( nodeRefs ) ) {
            // In case the node is "deleted" or corrupt, rename to something
            // else.
            if (node != null) {
                Debug.error(true, false,
                                "Error! tried to create " + cmName + " in parent, " + parent
                                                + ", but a deleted or corrupt node of the same name exists.  Renaming to a_"
                                                + cmName + ".");
                cmName = "a_" + cmName;
                return getOrCreateContentNode(parent, cmName, services);
            }
            node = parent.createNode(cmName, "cm:content");
            // } else {
            // // TODO -- ERROR! Multiple matches
            // Debug.error(true, false, "Found multiple " + cmName +
            // " nodes whe expecting just one: " + nodeRefs );
            // }
        }
        return node;
    }

    public static void processDocumentEdges(String sysmlid, String doc, List<Pair<String, String>> documentEdges) {
        if (doc != null) {
            String MMS_TRANSCLUDE_PATTERN = "<mms-transclude.*eid=\"([^\"]*)\"";
            Pattern pattern = Pattern.compile(MMS_TRANSCLUDE_PATTERN);
            Matcher matcher = pattern.matcher(doc);

            while (matcher.find()) {
                String mmseid = matcher.group(1).replace("\"", "");
                if (mmseid != null)
                    documentEdges.add(new Pair<>(sysmlid, mmseid));
            }
        }
    }

    public static void processV2VEdges(String sysmlid, JSONArray v2v, List<Pair<String, String>> documentEdges) {
        if (v2v != null) {
            for (int i2 = 0; i2 < v2v.length(); i2++) {
                JSONObject o = v2v.getJSONObject(i2);
                String id = null;
                if (o.has(Sjm.SYSMLID))
                    id = o.getString(Sjm.SYSMLID);
                else if (o.has("id"))
                    id = o.getString("id");
                else
                    continue;

                if (o.has("childrenViews")) {
                    JSONArray childViews = o.getJSONArray("childrenViews");
                    for (int j = 0; j < childViews.length(); j++) {
                        documentEdges.add(new Pair<>(id, childViews.getString(j)));
                    }
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
                    JSONObject value = operand.getJSONObject(ii);
                    if (value.has("instance")) {
                        documentEdges.add(new Pair<>(sysmlId, value.getString("instance")));
                    }
                }
            }
        }
    }

    /**
     * Contents -> expression -> operand -> instance specification -> instance
     */
    public static void processContentsNodeRef(String sysmlId, NodeRef contents,
                    List<Pair<String, String>> documentEdges) {
        if (contents != null) {
            EmsScriptNode node = new EmsScriptNode(contents, services, null);
            ArrayList<NodeRef> operands =
                            (ArrayList<NodeRef>) node.getNodeRefProperty(Acm.ACM_OPERAND, true, null, null);
            if (operands != null) {
                for (int ii = 0; ii < operands.size(); ii++) {
                    EmsScriptNode operand = new EmsScriptNode(operands.get(ii), services, null);
                    NodeRef instanceNr = (NodeRef) operand.getNodeRefProperty(Acm.ACM_INSTANCE, true, null, null);
                    if (instanceNr != null) {
                        EmsScriptNode instance = new EmsScriptNode(instanceNr, services, null);
                        documentEdges.add(new Pair<>(sysmlId, instance.getSysmlId()));
                    }
                }
            }
        }
    }

    public static void processInstanceSpecificationSpecificationJson(String sysmlId, JSONObject iss,
                    List<Pair<String, String>> documentEdges) {
        if (iss != null) {
            if (iss.has("string")) {
                String string = iss.getString("string");
                JSONObject json = new JSONObject(string);
                Set<Object> sources = findKeyValueInJsonObject(json, "source");
                for (Object source : sources) {
                    if (source instanceof String) {
                        documentEdges.add(new Pair<>(sysmlId, (String) source));
                    }
                }
            }
        }
    }

    /**
     * InstanceSpecificationSpecification -> string -> any source key
     *
     * @param sysmlId
     * @param iss
     * @param documentEdges
     */
    public static void processInstanceSpecificationSpecificationNodeRef(String sysmlId, NodeRef iss,
                    List<Pair<String, String>> documentEdges) {
        if (iss != null) {
            EmsScriptNode issNode = new EmsScriptNode(iss, services, null);
            String string = (String) issNode.getProperty(Acm.ACM_STRING);
            if (string != null) {
                JSONObject json = new JSONObject(string);
                Set<Object> sources = findKeyValueInJsonObject(json, "source");
                for (Object source : sources) {
                    if (source instanceof String) {
                        documentEdges.add(new Pair<>(sysmlId, (String) source));
                    }
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

    public static EmsScriptNode getNodeFromPostgresNode(Node pgnode) {
        return new EmsScriptNode(new NodeRef(pgnode.getElasticId()), // TODO (used to be get node
                                                                     // ref)
                        services, null);
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
     * moduleDetailsToJson </br>
     * --------------</br>
     * Takes a module of type ModuleDetails and retrieves the module members specified in the
     * details array. This will create a new JSONObject and iterate through the details array for
     * each specified member.</br>
     * </br>
     * Accepted keys (Details) are:</br>
     * </br>
     * 1. title </br>
     * 2. version </br>
     * 3. aliases </br>
     * 4. class </br>
     * 5. dependencies</br>
     * 6. editions </br>
     * 7. id </br>
     * 8. properties.</br>
     *
     * @param module A single module of type ModuleDetails
     * @param details
     * @return JSONObject containing the data members specified by the string arguments passed in
     *         the String[] details array.
     */
    public static JSONObject moduleDetailsToJson(ModuleDetails module, String[] details) {
        JSONObject jsonModule = new JSONObject();
        int index;

        for (index = 0; index < details.length; index++) {
            switch (details[index].toLowerCase()) {
                case "title":
                    jsonModule.put("mmsTitle", module.getTitle());
                    break;
                case "versions":
                    jsonModule.put("mmsVersion", module.getModuleVersionNumber());
                    break;
                case "aliases":
                    jsonModule.put("mmsAliases", module.getAliases());
                    break;
                case "class":
                    jsonModule.put("mmsClass", module.getClass());
                    break;
                case "dependencies":
                    jsonModule.put("mmsDependecies", module.getDependencies());
                    break;
                case "editions":
                    jsonModule.put("mmsEditions", module.getEditions());
                    break;
                case "id":
                    jsonModule.put("mmsId", module.getId());
                    break;
                case "properties":
                    jsonModule.put("mmsProperties", module.getProperties());
                    break;
            }
        }
        return jsonModule;
    }

    /**
     * getModuleVerionsJson </br>
     * --------------</br>
     * Overloaded method to return a JSONObject of the module version by passing the
     *
     * @param module ModuleDetails
     * @return JSONObject
     */
    public static JSONObject getModuleVersionJson(ModuleDetails module) {
        return getSingleModuleDetailJson(module, "version");
    }

    /**
     * getSingleModuleDetailJson </br>
     * ------------------------</br>
     * Accepts a ModuleDetails object and a key of string type, and will return the a JSON object of
     * a specific detail within the ModuleDetails. Calls on the ModuleDetails . get*****() methods
     * to retrieve ModuleDetails and the JSONObject's put("key","value") method. </br>
     * Accepted keys (Details) are:</br>
     * </br>
     * 1. title </br>
     * 2. version </br>
     * 3. aliases </br>
     * 4. class </br>
     * 5. dependencies</br>
     * 6. editions </br>
     * 7. id </br>
     * 8. properties.</br>
     *
     * @param module ModuleDetails
     * @param key String
     * @return JSONObject
     */
    public static JSONObject getSingleModuleDetailJson(ModuleDetails module, String key) {
        JSONObject jsonDetail = new JSONObject();
        if (module == null) {
            jsonDetail.put("mmsTitle", "");
            jsonDetail.put("mmsVersion", "");
            jsonDetail.put("mmsAliases", "");
            jsonDetail.put("mmsClass", "");
            jsonDetail.put("mmsDependencies", "");
            jsonDetail.put("mmsEditions", "");
            jsonDetail.put("mmsId", "");
            jsonDetail.put("mmsProperties", "");
        } else {
            switch (key.toLowerCase()) {
                case "title":
                    jsonDetail.put("mmsTitle", module.getTitle());
                    break;
                case "versions":
                    jsonDetail.put("mmsVersion", module.getModuleVersionNumber());
                    break;
                case "aliases":
                    jsonDetail.put("mmsAliases", module.getAliases());
                    break;
                case "class":
                    jsonDetail.put("mmsClass", module.getClass());
                    break;
                case "dependencies":
                    jsonDetail.put("mmsDependencies", module.getDependencies());
                    break;
                case "editions":
                    jsonDetail.put("mmsEditions", module.getEditions());
                    break;
                case "id":
                    jsonDetail.put("mmsId", module.getId());
                    break;
                case "properties":
                    jsonDetail.put("mmsProperties", module.getProperties());
                    break;
            }
        }
        return jsonDetail;
    }

    /**
     * Compares 2 modules using the key value passed in
     *
     * @param module1
     * @param module2
     * @param key
     * @return boolean value whether or not the modules have equal values in the field they are
     *         comparing against
     */
    public static boolean compareModuleDetails(ModuleDetails module1, ModuleDetails module2, String key) {
        boolean haveEqualDetails = false;

        // TODO:Need to write a check if either objects have the key field
        // requested.
        JSONObject obj1 = getSingleModuleDetailJson(module1, key);
        JSONObject obj2 = getSingleModuleDetailJson(module2, key);
        if (obj1.get(key) == null || obj2.get(key) == null) {
            System.out.println("The key + " + key + " does not exist in one of the modules!");
            System.out.println("Object 1 value: " + obj1.get(key).toString());
            System.out.println("Object 2 value: " + obj2.get(key).toString());
            return false;
        }
        // TODO: If correct, they should both be a JSONObject with only 1 key
        // and value field, to which they
        // should compare true or false.
        if (obj1 != null && obj2 != null && obj1.equals(obj2)) {
            System.out.println("Module1 is " + obj1.toString());
            System.out.println("");
            System.out.println("Module2 is " + obj2.toString());
            haveEqualDetails = true;
        }

        return haveEqualDetails;
    }

    /**
     * compareModuleVersions </br>
     * </br>
     * This will take 2 modules as arguments and compare their versions to each other returning a
     * boolean value of this comparison. </br>
     *
     * @param module1
     * @param module2
     * @return
     */
    public static boolean compareModuleVersions(ModuleDetails module1, ModuleDetails module2) {
        if (module1 == null || module2 == null) {
            return false;
        }
        return compareModuleDetails(module1, module2, "version");
    }

    /**
     * getMMSversion </br>
     * </br>
     * Gets the version number of a module, returns a JSONObject which calls on getString with
     * 'version' as an argument. This will return a String representing the version of the
     * mms. </br>
     *
     * @param module
     * @return Version number of the MMS as type String
     */
    public static String getMMSversion() {
        ModuleService service = getModuleService(services);
        JSONArray moduleDetails = getServiceModulesJson(service);
        String mmsVersion = "NA";
        int moduleArrayLength = moduleDetails.length();
        JSONObject jsonModule;
        if (moduleArrayLength > 0) {
            jsonModule = moduleDetails.getJSONObject(0);
            mmsVersion = jsonModule.get("mmsVersion").toString();
        }

        int endIndex = mmsVersion.lastIndexOf(".");

        return mmsVersion.substring(0, endIndex);
    }

    public static void addEditable(Map<String, Object> model, boolean editable){
    	if(!model.containsKey("res")) return;

    	Object res = model.get("res");
    	if(!(res instanceof String)) return;

    	JSONObject json;
    	try{
    		json = new JSONObject((String)res);
    	}
    	catch(Exception e){
    		return;
    	}

    	String topLevelKeys[] = {"elements", "products", "views", "workspace1"};
        for (int ii = 0; ii < topLevelKeys.length; ii++) {
            String key = topLevelKeys[ii];
            if (json.has(key)) {
                if (!key.equals("workspace1")) {
                    JSONArray elementsJson = json.getJSONArray(key);
                    try {
                        handleEditable(elementsJson, editable);
                    } catch (Exception e) {
                        logger.error("Could not set editable property.");
                        e.printStackTrace();
                    }
                }
            }
        }
        model.put("res", json.toString(4));
    }

    private static void handleEditable(JSONArray elementsJson, boolean editable){
    	for(int i=0; i < elementsJson.length(); i++){
    		JSONObject elementJson = elementsJson.getJSONObject(i);
    		if(!elementJson.has(Sjm.EDITABLE))
    			elementJson.put(Sjm.EDITABLE, editable);
    	}
    }

    /**
     * Post processing utility: Given webscript response (model), looks at JSON and injects
     * qualified names.
     *
     * @param req
     * @param model
     */
    public static void ppAddQualifiedNameId2Json(WebScriptRequest req, Map<String, Object> model) {
        if (!doPostProcessQualified)
            return;

        if (!model.containsKey("res"))
            return;
        Object res = model.get("res");
        if (!(res instanceof String))
            return;
        JSONObject json;
        try {
            json = new JSONObject((String) res);
        } catch (Exception e) {
            // not json, return
            return;
        }

        Map<String, String> id2name = new HashMap<>();
        Map<String, String> id2siteName = new HashMap<>();
        Map<String, Set<String>> owner2children = new HashMap<>();
        Map<String, String> child2owner = new HashMap<>();

        String topLevelKeys[] = {"elements", "products", "views", "workspace1"};
        for (int ii = 0; ii < topLevelKeys.length; ii++) {
            String key = topLevelKeys[ii];
            if (json.has(key)) {
                if (!key.equals("workspace1")) {
                    JSONArray elementsJson = json.getJSONArray(key);
                    try {
                        ppHandleElements(req, elementsJson, id2name, id2siteName, owner2children, child2owner);
                    } catch (Exception e) {
                        logger.error("Could not get qualified path");
                        e.printStackTrace();
                    }
                } else {
                    // FIXME: enable later since difficult to track qualifiedIds
                    // in time, let EmsScriptNode do the job
                    // JSONObject ws1Json = json.getJSONObject( "workspace1" );
                    // JSONArray elementsJson = ws1Json.getJSONArray( "elements"
                    // );
                    // handleElements( req, elementsJson, id2name, id2siteName,
                    // owner2children, child2owner );
                    //
                    // JSONObject ws2Json = json.getJSONObject( "workspace2" );
                    // // deleted doesn't require any qualified ids, so ignore
                    // for now
                    // String innerKeys[] =
                    // { "elements", "updatedElements", /* "deletedElements", */
                    // "conflictedElements", "addedElements", "movedElements" };
                    // for ( int jj = 0; jj < innerKeys.length; jj++ ) {
                    // if ( ws2Json.has( innerKeys[ jj ] ) ) {
                    // JSONArray keyArrayJson = ws2Json.getJSONArray( innerKeys[
                    // jj ] );
                    // handleElements( req, keyArrayJson, id2name, id2siteName,
                    // owner2children, child2owner );
                    // }
                    // }
                }
            }
        }
        model.put("res", json.toString(4));
    }

    /**
     * Post processing for handling JSON elements after the fact. Creates all the maps necessary to
     * create qualified names.
     *
     */
    private static void ppHandleElements(WebScriptRequest req, JSONArray elementsJson, Map<String, String> id2name,
                    Map<String, String> id2siteName, Map<String, Set<String>> owner2children,
                    Map<String, String> child2owner) {
        for (int ii = 0; ii < elementsJson.length(); ii++) {
            JSONObject elementJson = elementsJson.getJSONObject(ii);

            ppParseJsonElement(elementJson, id2name, id2siteName, owner2children, child2owner);
        }

        ppBuildOwnerTrees(req, id2name, id2siteName, owner2children, child2owner);

        for (int ii = 0; ii < elementsJson.length(); ii++) {
            JSONObject elementJson = elementsJson.getJSONObject(ii);
            ppUpdateElementJson(elementJson, id2name, id2siteName, owner2children, child2owner);
        }
    }

    /**
     * Post processing utility to update element JSON after the fact with qualified ids, names, and
     * site characterizations.
     */
    private static void ppUpdateElementJson(JSONObject elementJson, Map<String, String> id2name,
                    Map<String, String> id2siteName, Map<String, Set<String>> owner2children,
                    Map<String, String> child2owner) {
        String sysmlid = elementJson.getString(Sjm.SYSMLID);
        Map<String, String> qpathMap = new HashMap<>();

        ppBuildQualifiedPath(sysmlid, qpathMap, id2name, id2siteName, owner2children, child2owner);
        elementJson.put("qualifiedId", qpathMap.get("qid"));
        elementJson.put("qualifiedName", qpathMap.get("qname"));
        elementJson.put("siteCharacterizationId", qpathMap.get("site"));

        if (elementJson.has("properties")) {
            JSONArray propsJson = elementJson.getJSONArray("properties");
            for (int jj = 0; jj < propsJson.length(); jj++) {
                ppUpdateElementJson(propsJson.getJSONObject(jj), id2name, id2siteName, owner2children, child2owner);
            }
        }
    }

    /**
     * Generates the qualified path for the specified sysmlid
     */
    private static void ppBuildQualifiedPath(String sysmlId, Map<String, String> qpathMap, Map<String, String> id2name,
                    Map<String, String> id2siteName, Map<String, Set<String>> owner2children,
                    Map<String, String> child2owner) {
        String childId = sysmlId;
        String childName = id2name.get(childId);
        String qname = "/" + childName;
        String qid = "/" + childId;
        String site = null;
        while (child2owner.containsKey(childId)) {
            childId = child2owner.get(childId);
            childName = id2name.get(childId);
            qname = "/" + childName + qname;
            qid = "/" + childId + qid;
            if (site == null && id2siteName.containsKey(childId)) {
                site = id2siteName.get(childId);
            }
        }
        qpathMap.put("qid", qid);
        qpathMap.put("qname", qname);
        qpathMap.put("site", site);
    }

    /**
     * Post processing utility to generate owner trees based on a request
     */
    private static void ppBuildOwnerTrees(WebScriptRequest req, Map<String, String> id2name,
                    Map<String, String> id2siteName, Map<String, Set<String>> owner2children,
                    Map<String, String> child2owner) {
        // copy out owners, since we iterate over it and potentially add to it
        Set<String> owners = new HashSet<>();
        owners.addAll(owner2children.keySet());

        // traverse owners to get qualified names and site characterizations
        Set<String> visitedOwners = new HashSet<>();

        String wsId = AbstractJavaWebScript.getRefId(req);
        String timestamp = req.getServiceMatch().getTemplateVars().get("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp(timestamp);

        // if no owners, walk up to site to get appropriate information
        if (owners.size() <= 0) {
            Set<String> ids = new HashSet<>();
            ids.addAll(id2name.keySet());
            PostgresHelper pgh = new PostgresHelper(wsId);

            for (String id : ids) {
                EmsScriptNode node = null;
                if (NodeUtil.doGraphDb && wsId.equals("master") && dateTime == null) {
                    node = NodeUtil.getNodeFromPostgresNode(pgh.getNodeFromSysmlId(id));
                } else {
                    WorkspaceNode ws = WorkspaceNode.getWorkspaceFromId(wsId, services, null, null, null);
                    node = findScriptNodeById(id, ws, dateTime, false, services, null);
                }
                ppAddSiteOwnerInfo(id, node, id2name, id2siteName, owner2children, child2owner, visitedOwners);
            }
        }

        // for all owners, recurse to build up all the owner paths
        for (String owner : owners) {
            if (!visitedOwners.contains(owner)) {
                if (NodeUtil.doGraphDb && wsId.equals("master") && dateTime == null) {
                    PostgresHelper pgh = new PostgresHelper(wsId);
                    ppRecurseOwnersDb(owner, pgh, id2name, id2siteName, owner2children, child2owner, visitedOwners,
                                    wsId, dateTime);
                } else {
                    ppRecurseOwnersOriginal(owner, id2name, id2siteName, owner2children, child2owner, visitedOwners,
                                    wsId, dateTime);
                }
            }
        }
    }

    /**
     * Post processing utility for traversing owners using Alfresco solely
     */
    private static void ppRecurseOwnersOriginal(String sysmlId, Map<String, String> id2name,
                    Map<String, String> id2siteName, Map<String, Set<String>> owner2children,
                    Map<String, String> child2owner, Set<String> visitedOwners, String wsId, Date dateTime) {
        Status status = new Status();
        WorkspaceNode ws = WorkspaceNode.getWorkspaceFromId(wsId, services, null, null, null);
        if (status.getCode() == HttpServletResponse.SC_OK) {
            EmsScriptNode childNode = findScriptNodeById(sysmlId, ws, dateTime, false, services, null);
            String qid = childNode.getSysmlQId(dateTime, ws, true);
            String siteId = childNode.getSiteCharacterizationId(dateTime, ws);
            id2siteName.put(sysmlId, siteId);
            String qname = childNode.getSysmlQName(dateTime, ws, true);

            String[] qids = qid.split("/");
            String[] qnames = qname.split("/");
            if (qids.length <= 1)
                return;
            // start from 1 since the first token is empty
            for (int ii = 1; ii < qids.length; ii++) {
                String id = qids[ii];
                String name = qnames[ii];
                id2name.put(id, name);

                if (ii < qids.length - 1) {
                    if (!owner2children.containsKey(id)) {
                        owner2children.put(id, new HashSet<String>());
                    }
                    owner2children.get(id).add(qids[ii + 1]);
                    child2owner.put(qids[ii + 1], id);
                    visitedOwners.add(qids[ii + 1]);
                }
            }
        }
    }

    /**
     * Post processing utility for traversing owners using graphDb
     */
    private static void ppRecurseOwnersDb(String sysmlId, PostgresHelper pgh, Map<String, String> id2name,
                    Map<String, String> id2siteName, Map<String, Set<String>> owner2children,
                    Map<String, String> child2owner, Set<String> visitedOwners, String wsId, Date dateTime) {
        EmsScriptNode node = NodeUtil.getNodeFromPostgresNode(pgh.getNodeFromSysmlId(sysmlId));
        String sysmlName = node.getSysmlName();

        id2name.put(sysmlId, sysmlName);
        Boolean isSite = (Boolean) node.getProperty(Acm.ACM_IS_SITE);
        if (isSite != null && isSite) {
            id2siteName.put(sysmlId, sysmlName);
        }

        String parentId = sysmlId;
        EmsScriptNode parentNode = node;
        List<Pair<String, String>> parentTree = pgh.getContainmentParents(sysmlId, 10000);
        if (parentTree.size() > 0) {
            for (int ii = 0; ii < parentTree.size() - 1; ii++) {
                String childId = parentTree.get(ii).first;
                parentId = parentTree.get(ii + 1).first;
                String parentRef = parentTree.get(ii + 1).second;

                // update id2name maps
                parentNode = new EmsScriptNode(new NodeRef(parentRef), services, null);
                String parentName = parentNode.getSysmlName();
                id2name.put(parentId, parentName);
                Boolean isParentSite = (Boolean) node.getProperty(Acm.ACM_IS_SITE);
                if (isParentSite != null && isParentSite) {
                    id2siteName.put(parentId, parentName);
                }

                // update trees
                if (!owner2children.containsKey(parentId)) {
                    owner2children.put(parentId, new HashSet<>());
                }
                owner2children.get(parentId).add(childId);
                child2owner.put(childId, parentId);
                visitedOwners.add(parentId);
            }
        }

        ppAddSiteOwnerInfo(parentId, parentNode, id2name, id2siteName, owner2children, child2owner, visitedOwners);
    }

    /**
     * Post processing utility to get site owner information if no parents are available
     */
    private static void ppAddSiteOwnerInfo(String sysmlId, EmsScriptNode node, Map<String, String> id2name,
                    Map<String, String> id2siteName, Map<String, Set<String>> owner2children,
                    Map<String, String> child2owner, Set<String> visitedOwners) {
        if (node == null)
            return;
        EmsScriptNode parent = node.getParent();
        while (!parent.getName().equals("Sites") || parent == null) {
            node = parent;
            parent = node.getParent();
        }
        String parentId = node.getName();
        if (!owner2children.containsKey(parentId)) {
            owner2children.put(parentId, new HashSet<>());
        }
        owner2children.get(parentId).add(sysmlId);
        child2owner.put(sysmlId, parentId);
        visitedOwners.add(parentId);
        id2name.put(parentId, parentId);
        id2siteName.put(sysmlId, parentId);
    }

    /**
     * Post processing utility that parses JSON element and sets up maps for usage
     */
    private static void ppParseJsonElement(JSONObject element, Map<String, String> id2name,
                    Map<String, String> id2siteName, Map<String, Set<String>> owner2children,
                    Map<String, String> child2owner) {
        if (!element.has(Sjm.SYSMLID))
            return;
        String sysmlid = element.getString(Sjm.SYSMLID);

        String name = null;
        if (element.has("name")) {
            name = element.getString("name");
        }
        id2name.put(sysmlid, name);

        if (element.has(Sjm.OWNERID) && !element.isNull(Sjm.OWNERID)) {
            String owner = element.getString(Sjm.OWNERID);
            child2owner.put(sysmlid, owner);
            if (!owner2children.containsKey(owner)) {
                owner2children.put(owner, new HashSet<>());
            }
            owner2children.get(owner).add(sysmlid);
        }

        if (element.has("isSite")) {
            if (element.getBoolean("isSite")) {
                id2siteName.put(sysmlid, name);
            }
        }

        // handle properties
        if (element.has("properties")) {
            JSONArray properties = element.getJSONArray("properties");
            for (int ii = 0; ii < properties.length(); ii++) {
                JSONObject property = properties.getJSONObject(ii);
                ppParseJsonElement(property, id2name, id2siteName, owner2children, child2owner);
            }
        }
    }

	public static String getSiteName(JSONObject element, String projectId, String workspaceId) {
		if (element == null)
			return null; // TODO error handling

		String qualifiedId = null;
		try {
			EmsNodeUtil enu = new EmsNodeUtil(projectId, workspaceId);
			enu.addExtendedInformationForElement(element);
			qualifiedId = element.optString(Sjm.QUALIFIEDID);
		} catch (JSONException ex) {
			return null; // TODO error handling
		}

		String siteName = null;
		int startIdx = qualifiedId.indexOf('/');
		int endIdx = qualifiedId.indexOf('/', startIdx + 1);
		if (endIdx <= startIdx) {
			siteName = element.optString(Sjm.QUALIFIEDID);
			if (!StringUtils.isEmpty(siteName))
				siteName = siteName.replace("/", "");
		} else
			siteName = qualifiedId.substring(startIdx + 1, endIdx);

		return siteName;
	}
}
