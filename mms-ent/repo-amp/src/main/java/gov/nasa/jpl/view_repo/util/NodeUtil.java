package gov.nasa.jpl.view_repo.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDependency;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class NodeUtil {

    static Logger logger = Logger.getLogger(NodeUtil.class);

    /* static flags and constants */

    protected static Map<Long, UserTransaction> transactionMap =
        Collections.synchronizedMap(new LinkedHashMap<Long, UserTransaction>());

    public static UserTransaction getTransaction() {
        return transactionMap.get(Thread.currentThread().getId());
    }

    public static boolean skipSvgToPng = false;

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
     * @param s        Fully qualified or short-name QName string
     * @param services ServiceRegistry for getting the service to resolve the name space
     * @return QName
     */
    public static QName createQName(String s, ServiceRegistry services) {
        if (s == null)
            return null;
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
            logger.error("getCompanyHome() failed, no services or no nodeLocatorService: " + services);
        }
        NodeRef companyHomeNodeRef =
            services != null ? services.getNodeLocatorService().getNode("companyhome", null, null) : null;
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
     * @param key       the name of the property, e.g., "cm:name"
     * @param services
     * @param cacheOkay whether to look in the cache and cache a new value, assuming the cache is
     *                  turned on.
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
                logger
                    .trace("getNodeProperty(" + node + ", " + key + ", cacheOkay=" + cacheOkay + ") = null.  No Key!");
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
        homeFolderNode = (NodeRef) getNodeProperty(personNode, ContentModel.PROP_HOMEFOLDER, getServices(), true, true);
        if (homeFolderNode == null || !exists(homeFolderNode)) {
            EmsScriptNode homes = getCompanyHome(getServices());
            if (homes != null) {
                homes = homes.childByNamePath("/User Homes");
            }
            if (createIfNotFound && homes != null && homes.exists()) {
                homeFolderScriptNode = homes.createFolder(userName, null, null);
            } else {
                logger.error("Error! No user homes folder!");
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

    public static EmsScriptNode updateOrCreateArtifact(String artifactId, Path filePath, String fileType, String orgId, String projectId, String refId) {

        EmsScriptNode artifactNode;
        String finalType = null;
        File content = filePath.toFile();

        try {
            finalType = Files.probeContentType(filePath);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("updateOrCreateArtifact: ", e);
            }
        }

        if (finalType == null) {
            // Fallback if getting content type fails
            if (fileType != null) {
                finalType = fileType;
            } else {
                logger.error("Could not determine type of artifact: " + filePath.getFileName().toString());
                return null;
            }
        }

        EmsScriptNode targetSiteNode = EmsScriptNode.getSiteNode(orgId);

        // find site; it must exist!
        if (targetSiteNode == null || !targetSiteNode.exists()) {
            logger.error("Can't find node for site: " + orgId + "!");
            return null;
        }

        // find or create subfolder
        EmsScriptNode subfolder = targetSiteNode.childByNamePath("/" + projectId + "/refs/" + refId);
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
            logger.error("Failed to create new PNG artifact " + artifactId + "!");
            return null;
        }

        if (!artifactNode.hasAspect("cm:versionable")) {
            artifactNode.addAspect("cm:versionable");
        }
        if (!artifactNode.hasAspect("cm:indexControl")) {
            artifactNode.addAspect("cm:indexControl");
        }

        artifactNode.createOrUpdateProperty(Acm.CM_TITLE, artifactId);
        artifactNode.createOrUpdateProperty("cm:isIndexed", true);
        artifactNode.createOrUpdateProperty("cm:isContentIndexed", false);

        if (logger.isDebugEnabled()) {
            logger.debug("Creating artifact with indexing: " + artifactNode.getProperty("cm:isIndexed"));
        }

        ContentWriter writer =
            services.getContentService().getWriter(artifactNode.getNodeRef(), ContentModel.PROP_CONTENT, true);
        writer.putContent(content);

        ContentData contentData = writer.getContentData();
        contentData = ContentData.setMimetype(contentData, finalType);
        contentData = ContentData.setEncoding(contentData, "UTF-8");
        services.getNodeService().setProperty(artifactNode.getNodeRef(), ContentModel.PROP_CONTENT, contentData);

        return artifactNode;
    }

    public static String getHostname() {
        return services.getSysAdminParams().getAlfrescoHost();
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
        ModuleService moduleService =
            (ModuleService) services.getService(QName.createQName(NamespaceService.ALFRESCO_URI, "ModuleService"));
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
    public static JsonArray getServiceModulesJson(ModuleService service) {

        JsonArray jsonArray = new JsonArray();
        List<ModuleDetails> modules = service.getAllModules();
        for (ModuleDetails detail : modules) {
            JsonObject jsonModule = moduleDetailsToJson(detail);
            jsonArray.add(jsonModule);
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
    public static JsonObject moduleDetailsToJson(ModuleDetails module) {
    	JsonObject jsonModule = new JsonObject();
    	jsonModule.addProperty("mmsTitle", module.getTitle());
    	jsonModule.addProperty("mmsVersion", module.getModuleVersionNumber().toString());
    	JsonUtil.addStringList(jsonModule, "mmsAliases", module.getAliases());
    	jsonModule.addProperty("mmsClass", module.getClass().toString());
    	JsonArray depArray = new JsonArray();
    	for (ModuleDependency depend: module.getDependencies())
            depArray.add(depend.toString());
    	jsonModule.add("mmsDependencies", depArray);
    	JsonUtil.addStringList(jsonModule, "mmsEditions", module.getEditions());
    	jsonModule.addProperty("mmsId", module.getId());
    	JsonObject propObj = new JsonObject();
    	Enumeration<?> enumerator = module.getProperties().propertyNames();
    	while (enumerator.hasMoreElements()) {
            String key = (String)enumerator.nextElement();
            propObj.addProperty(key, module.getProperties().getProperty(key));
    	}
    	jsonModule.add("mmsProperties", propObj);
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
        JsonArray moduleDetails = getServiceModulesJson(service);
        String mmsVersion = "NA";
        for (int i = 0; i < moduleDetails.size(); i++) {
            JsonObject o = moduleDetails.get(i).getAsJsonObject();
            if (o.get("mmsId").getAsString().equalsIgnoreCase("mms-amp")) {
                mmsVersion = o.get("mmsVersion").getAsString();
            }
        }

        // Remove appended tags from version
        int endIndex = mmsVersion.indexOf('-');
        return endIndex > -1 ? mmsVersion.substring(0, endIndex) : mmsVersion;
    }
}
