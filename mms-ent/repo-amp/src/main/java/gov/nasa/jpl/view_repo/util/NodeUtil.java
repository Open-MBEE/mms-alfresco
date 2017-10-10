package gov.nasa.jpl.view_repo.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.transaction.UserTransaction;
import javax.xml.bind.DatatypeConverter;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.node.db.DbNodeServiceImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.NullArgumentException;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;

import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.MethodCall;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;

public class NodeUtil {

    static Logger logger = Logger.getLogger(NodeUtil.class);

    /* static flags and constants */


    protected static boolean insideTransactionNow = false;
    protected static Map<Long, Boolean> insideTransactionNowMap = new LinkedHashMap<>();
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

    public static boolean skipSvgToPng = false;

    // Set the flag to time events that occur during a model post using the
    // timers
    // below
    public static boolean timeEvents = false;

    public static ServiceRegistry services = null;
    public static Repository repository = null;

    public static Repository getRepository() {
        return repository;
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
            qname = QName.createQName(s, getServices().getNamespaceService());
        }
        return qname;
    }


    public static EmsScriptNode getCompanyHome(ServiceRegistry services) {
        EmsScriptNode companyHome = null;
        if (services == null)
            services = getServiceRegistry();
        if (services == null || services.getNodeLocatorService() == null) {
            if (Debug.isOn()) {
                logger.debug("getCompanyHome() failed, no services or no nodeLocatorService: " + services);
            }
        }
        NodeRef companyHomeNodeRef = services != null ? services.getNodeLocatorService().getNode("companyhome", null, null): null;
        if (companyHomeNodeRef != null) {
            companyHome = new EmsScriptNode(companyHomeNodeRef, services);
        }
        return companyHome;
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
        if (logger.isTraceEnabled())
            logger.trace("getNodeProperty(" + node + ", " + key + ", cacheOkay=" + cacheOkay + ") = " + result);
        return result;
    }

    public static NodeRef getNodeRefFromNodeId(String store, String id) {
        List<NodeRef> nodeRefs = NodeRef.getNodeRefs(store + id);
        if (!nodeRefs.isEmpty()) {
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
        return AuthenticationUtil.getRunAsUser();
    }

    public static EmsScriptNode getUserHomeFolder(String userName, boolean createIfNotFound) {
        NodeRef homeFolderNode;
        EmsScriptNode homeFolderScriptNode = null;
        PersonService personService = getServices().getPersonService();
        NodeRef personNode = personService.getPerson(userName);
        homeFolderNode = (NodeRef) getNodeProperty(personNode, ContentModel.PROP_HOMEFOLDER, getServices(), true,
                            true);
        if (homeFolderNode == null || !exists(homeFolderNode)) {
            EmsScriptNode homes = getCompanyHome(getServices());
            if (homes != null) {
                homes = homes.childByNamePath("/User Homes");
            }
            if (createIfNotFound && homes != null && homes.exists()) {
                homeFolderScriptNode = homes.createFolder(userName, null, null);
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
        }

        if (artifactNode == null || !artifactNode.exists()) {
            Debug.err("Failed to create new artifact " + artifactId + "!\n");
            return null;
        }

        if (!artifactNode.hasAspect("cm:versionable")) {
            artifactNode.addAspect("cm:versionable");
        }
        if (!artifactNode.hasAspect("cm:indexControl")) {
            artifactNode.addAspect("cm:indexControl");
        }
        if (!artifactNode.hasAspect(Acm.ACM_IDENTIFIABLE)) {
            artifactNode.addAspect(Acm.ACM_IDENTIFIABLE);
        }

        artifactNode.createOrUpdateProperty(Acm.CM_TITLE, artifactId);
        artifactNode.createOrUpdateProperty("cm:isIndexed", true);
        artifactNode.createOrUpdateProperty("cm:isContentIndexed", false);
        artifactNode.createOrUpdateProperty(Acm.ACM_ID, artifactId);

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
        services.getNodeService().setProperty(artifactNode.getNodeRef(), ContentModel.PROP_CONTENT, contentData);

        // if only version, save dummy version so snapshots can reference
        // versioned images - need to check against 1 since if someone
        // deleted previously a "dead" version is left in its place
        Object[] versionHistory = artifactNode.getEmsVersionHistory();

        if (versionHistory == null || versionHistory.length <= 1) {
            artifactNode.createVersion("creating the version history", false);
        }
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
		}

		if (pngNode == null || !pngNode.exists()) {
			Debug.err("Failed to create new PNG artifact " + artifactId + "!\n");
			return null;
		}

		if (!pngNode.hasAspect("cm:versionable")) {
			pngNode.addAspect("cm:versionable");
		}
		if (!pngNode.hasAspect("cm:indexControl")) {
			pngNode.addAspect("cm:indexControl");
		}
		if (!pngNode.hasAspect(Acm.ACM_IDENTIFIABLE)) {
			pngNode.addAspect(Acm.ACM_IDENTIFIABLE);
		}

		pngNode.createOrUpdateProperty(Acm.CM_TITLE, artifactId);
		pngNode.createOrUpdateProperty("cm:isIndexed", true);
		pngNode.createOrUpdateProperty("cm:isContentIndexed", false);
		pngNode.createOrUpdateProperty(Acm.ACM_ID, artifactId);

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
		services.getNodeService().setProperty(pngNode.getNodeRef(),
				ContentModel.PROP_CONTENT, contentData);

		/*
		Object[] versionHistory = pngNode.getEmsVersionHistory();

        if ( versionHistory == null || versionHistory.length <= 1 ) {
            pngNode.makeSureNodeRefIsNotFrozen();
            pngNode.createVersion( "creating the version history", false );
        }
        */

		return pngNode;
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
     * getServiceModules
     *
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
        } catch (Exception e) {
            logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
        }
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
