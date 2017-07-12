package gov.nasa.jpl.view_repo.webscripts.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;

/**
 * Library class for manipulating configurations
 *
 * Pattern is handle does the retrieval.
 *
 * @author cinyoung
 *
 */
public class ConfigurationsWebscript extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ConfigurationsWebscript.class);

    public ConfigurationsWebscript( Repository repository, ServiceRegistry services,
                    StringBuffer response ) {
        super( repository, services, response );
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return false;
    }


    public JSONArray handleConfigurations(WebScriptRequest req) throws JSONException {
        return handleConfigurations( req, false );
    }

    public EmsScriptNode getConfiguration(String id) {
        NodeRef configNodeRef = NodeUtil.getNodeRefFromNodeId( id );
        if (configNodeRef != null) {
            EmsScriptNode configNode = new EmsScriptNode(configNodeRef, services);
            return configNode;
        } else {
            log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Could not find configuration with id %s", id);
            return null;
        }
    }

    /**
     * Method to get Configurations within a specified context
     * @param context
     * @param timestamp
     * @param sort
     * @return
     */
    public List< EmsScriptNode > getConfigurations( EmsScriptNode siteNode,
                    WorkspaceNode workspace,
                    String timestamp,
                    boolean sort ) {
        List<EmsScriptNode> configurations = new ArrayList<EmsScriptNode>();
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

        // Note: not using searchForElements() b/c it checks if the return element has a sysml:id, which
        //       configurations do not
        String siteName = siteNode == null ? null : siteNode.getName();
        ArrayList<NodeRef> resultSet = NodeUtil.findNodeRefsByType( "ems:ConfigurationSet", NodeUtil.SearchType.TYPE.prefix,
                        false, workspace,
                        true, // onlyThisWorkspace
                        dateTime, false, false, services, false,
                        siteName );
        List< EmsScriptNode > nodeList = EmsScriptNode.toEmsScriptNodeList( resultSet, services, response,
                        responseStatus );
        if (nodeList != null) {
            Set<EmsScriptNode> nodes = new HashSet<EmsScriptNode>(nodeList);
            configurations.addAll(nodes);
        }
        if (sort) {
            Collections.sort(configurations, new EmsScriptNodeCreatedAscendingComparator());
        }

        return configurations;
    }

    public enum ConfigurationType {
        CONFIG_MMS, CONFIG_NO_MMS, CONFIG_SNAPSHOT
    }

    /**
     * Create JSONObject of Configuration sets based on WebScriptRequest
     * @param req
     * @param isMms For backwards compatibility, support whether old javawebscript or new mms format
     * @return
     * @throws JSONException
     */
    public JSONArray handleConfigurations(WebScriptRequest req, boolean isMms) throws JSONException {
        ConfigurationType configType = ConfigurationType.CONFIG_NO_MMS;
        if (isMms) {
            configType = ConfigurationType.CONFIG_MMS;
        }
        return handleConfigurations( req, configType );
    }

    /**
     *
     * @param req
     * @param configType
     * @return
     * @throws JSONException
     */
    public JSONArray handleConfigurations(WebScriptRequest req, ConfigurationType configType) throws JSONException {
        EmsScriptNode siteNode = getSiteNodeFromRequest(req, false);
        String siteNameFromReq = getSiteName( req );
        if ( siteNode == null && !Utils.isNullOrEmpty( siteNameFromReq )
                        && !siteNameFromReq.equals( NO_SITE_ID ) ) {
            log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Could not find site %s", siteNameFromReq);
            return new JSONArray();
        }
        // when we're looking for snapshots, we don't care about site
        if (ConfigurationType.CONFIG_SNAPSHOT == configType) {
            siteNode = null;
        }

        JSONArray configJSONArray = new JSONArray();

        // get timestamp if specified
        String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

        WorkspaceNode workspace = getWorkspace( req );

        boolean sort = true;
        List< EmsScriptNode > configurations =
                        getConfigurations( siteNode, workspace, timestamp, sort );

        for (EmsScriptNode config: configurations) {
            if (!config.isDeleted()) {
                switch(configType) {
                    case CONFIG_MMS:
                        configJSONArray.put( getMmsConfigJson( config,
                                        workspace, dateTime ) );
                        break;
                    case CONFIG_NO_MMS:
                        configJSONArray.put( getConfigJson( config,
                                        workspace, dateTime ) );
                        break;
                    case CONFIG_SNAPSHOT:
                        configJSONArray.put( getConfigSnapshotJson(config,
                                        workspace, dateTime) );
                        break;
                    default:
                        logger.error("No ConfigType specified!");
                }
            }
        }

        return configJSONArray;
    }


    /**
     *
     * @param req
     * @param isMms
     * @return
     * @throws JSONException
     */
    public JSONArray handleConfiguration(WebScriptRequest req, boolean isMms) throws JSONException {
        String configId = req.getServiceMatch().getTemplateVars().get("configurationId");

        JSONArray configsJSONArray = handleConfigurations(req, isMms);
        JSONArray result = new JSONArray();

        for (int ii = 0; ii < configsJSONArray.length(); ii++) {
            JSONObject configJson = configsJSONArray.getJSONObject( ii );
            if (configJson.getString( "id" ).equals(configId)) {
                result.put( configJson );
                break;
            }
        }

        return result;
    }


    /**
     * Retrieve just the MMS API configuration JSON
     * @param config
     * @param contextPath
     * @param workspace
     * @param dateTime
     * @return
     * @throws JSONException
     */
    public JSONObject getMmsConfigJson(EmsScriptNode config,
                    WorkspaceNode workspace, Date dateTime)
                                    throws JSONException {
        JSONObject json = getConfigJson( config, workspace, dateTime );

        json.remove( "snapshots" );

        return json;
    }

    /**
     * Given a configuration node, convert to JSON
     * @param config
     * @param contextPath
     * @param workspace
     * @param dateTime
     * @param includeSnapshots  Include snapshots in json if true (this can take a while, so default to false)
     * @return
     * @throws JSONException
     */
    public JSONObject getConfigJson(EmsScriptNode config,
                    WorkspaceNode workspace,
                    Date dateTime) throws JSONException {
        JSONObject json = new JSONObject();
        Date date = (Date)config.getProperty("cm:created");

        json.put("modified", EmsScriptNode.getIsoTime(date));
        Object timestampObject = config.getProperty("view2:timestamp");
        Date timestamp = null;
        if ( timestampObject instanceof Date ) {
            timestamp = (Date)timestampObject;
        } else {
            if ( timestampObject != null ) {
                //Debug.error( "timestamp is not a date! timestamp = " + timestampObject );
                log(Level.ERROR,"timestamp is not a date! timestamp = %s", timestampObject.toString());
                //logger.error( "timestamp is not a date! timestamp = " + timestampObject );
            }
            timestamp = new Date( System.currentTimeMillis() );
        }
        json.put("timestamp", EmsScriptNode.getIsoTime(timestamp));
        // for back compat with tags, need to used CM_NAME if title isn't being used
        String title = (String)config.getProperty( Acm.CM_TITLE );
        if (title == null) {
            json.put( "name", config.getProperty( Acm.CM_NAME ) );
        } else {
            json.put("name", title);
        }
        json.put("description", config.getProperty("cm:description"));
        // need to unravel id with the storeref, which by default is workspace://SpacesStore/
        json.put("id", config.getNodeRef().getId());

        return json;
    }

    private JSONObject getConfigSnapshotJson( EmsScriptNode config,
                    WorkspaceNode workspace,
                    Date dateTime ) throws JSONException {
        // fill in configurations first
        JSONObject configJson = new JSONObject();
        String name = (String)config.getProperty( Acm.CM_TITLE );
        if (null == name) {
            name = (String)config.getProperty(Acm.CM_NAME);
        }
        configJson.put("name", name);
        configJson.put( "id", config.getNodeRef().getId() );
        JSONArray configArray = new JSONArray();
        configArray.put( configJson );


        // fill in the snapshot json
        JSONObject json = new JSONObject();
        // need to kludge an ID - takes too long to actually look up snapshot, so just use config id
        json.put( "id", config.getNodeRef().getId() );

        // a lot of this repeated from getConfigJson, different keys though
        Object timestampObject = config.getProperty("view2:timestamp");
        Date timestamp = null;
        if ( timestampObject instanceof Date ) {
            timestamp = (Date)timestampObject;
        } else {
            if ( timestampObject != null ) {
                logger.error( "timestamp is not a date! timestamp = " + timestampObject );
            }
            timestamp = new Date( System.currentTimeMillis() );
        }
        json.put("created", EmsScriptNode.getIsoTime(timestamp));
        //        Date date = (Date)config.getProperty("cm:created");
        //        json.put("created", EmsScriptNode.getIsoTime(date));

        json.put("creator", config.getProperty("cm:modifier", false));

        json.put( "configurations", configArray );

        return json;
    }



    public JSONArray getSnapshots(EmsScriptNode config, WorkspaceNode workspace) throws JSONException {
        JSONArray snapshotsJson = new JSONArray();

        // Need to put in null timestamp so we always get latest version of snapshot
        List< EmsScriptNode > snapshots =
                        config.getTargetAssocsNodesByType( "ems:configuredSnapshots",
                                        workspace, null );
        for (EmsScriptNode snapshot: snapshots) {
            // getting by association is deprecated
            List< EmsScriptNode > views =
                            snapshot.getSourceAssocsNodesByType( "view2:snapshots",
                                            workspace, null );
            if (views.size() >= 1) {
                if ( !snapshot.isDeleted() ) {
                    snapshotsJson.put( getSnapshotJson(snapshot, views.get(0),
                                    workspace) );
                }
            }

            Date dateTime = (Date) snapshot.getProperty("view2:timestamp");
            NodeRef snapshotProductNodeRef;
            try {
                snapshotProductNodeRef = (NodeRef) snapshot.getNodeRefProperty( "view2:snapshotProduct",
                                dateTime, workspace);
            } catch (org.alfresco.repo.security.permissions.AccessDeniedException e) {
                // permission issue, so skip
                continue;
            }

            // TODO doing another search below may be redundant b/c getNodeRefProperty() will handle it
            if ( snapshotProductNodeRef != null ) {
                // this is the unversioned snapshot, so we need to get the versioned one
                EmsScriptNode snapshotProduct = new EmsScriptNode(snapshotProductNodeRef, services, response);

                String id = snapshotProduct.getSysmlId();

                EmsScriptNode versionedSnapshotProduct = NodeUtil.findScriptNodeById( id, workspace, dateTime, true, services, response );
                if (snapshotProduct.exists()) {
                    snapshotsJson.put( getSnapshotJson(snapshot, versionedSnapshotProduct, workspace) );
                }
            }
        }

        return snapshotsJson;
    }

    /**
     * Based on a specified snapshot node and corresponding view and configuration returns the
     * snapshot JSON - maybe put this in EmsScriptNode?
     * @param snapshot
     * @param view
     * @param config
     * @return
     * @throws JSONException
     */
    public JSONObject
    getSnapshotJson( EmsScriptNode snapshot, EmsScriptNode view,
                    WorkspaceNode workspace ) throws JSONException {
        JSONObject snapshotJson = new JSONObject();
        if (view != null) {
            snapshotJson.put(Sjm.SYSMLID, view.getSysmlId());
            snapshotJson.put("sysmlname", view.getProperty(Acm.ACM_NAME));
        } else {
            // if view isn't provided, just strip off the timestamp
            String sysmlid = (String)snapshot.getProperty(Acm.CM_NAME);
            sysmlid = sysmlid.substring( 0, sysmlid.lastIndexOf( "_" ) );
            snapshotJson.put( Sjm.SYSMLID, snapshot.getProperty( sysmlid ) );
        }
        snapshotJson.put("id", snapshot.getNodeRef().getId());
        Date timestamp = (Date) snapshot.getProperty("view2:timestamp");
        if (timestamp != null) {
            snapshotJson.put( "created",  EmsScriptNode.getIsoTime( (Date)snapshot.getProperty( "view2:timestamp" )));
        } else {
            snapshotJson.put( "created",  EmsScriptNode.getIsoTime( (Date)snapshot.getProperty( "cm:created" )));
        }
        snapshotJson.put( "creator", snapshot.getProperty( "cm:modifier", false ) );

//        @SuppressWarnings( "rawtypes" )
//        LinkedList<HashMap> list = new LinkedList<HashMap>();
//        if(SnapshotPost.hasPdf(snapshot) || SnapshotPost.hasHtmlZip(snapshot)){
//            HostnameGet hostnameGet = new HostnameGet(this.repository, this.services);
//            String contextUrl = hostnameGet.getAlfrescoUrl() + "/alfresco";
//            HashMap<String, String> transformMap;
//            if(SnapshotPost.hasPdf(snapshot)){
//                String pdfStatus = SnapshotPost.getPdfStatus(snapshot);
//                if(pdfStatus != null && !pdfStatus.isEmpty()){
//                    EmsScriptNode pdfNode = SnapshotPost.getPdfNode(snapshot, timestamp);
//                    transformMap = new HashMap<String,String>();
//                    transformMap.put("status", pdfStatus);
//                    transformMap.put("type", "pdf");
//                    if(pdfNode != null){
//                        transformMap.put("url", contextUrl + pdfNode.getUrl());
//                    }
//                    list.add(transformMap);
//                }
//            }
//            if(SnapshotPost.hasHtmlZip(snapshot)){
//                String htmlZipStatus = SnapshotPost.getHtmlZipStatus(snapshot);
//                if(htmlZipStatus != null && !htmlZipStatus.isEmpty()){
//                    EmsScriptNode htmlZipNode = SnapshotPost.getHtmlZipNode(snapshot, timestamp);
//                    transformMap = new HashMap<String,String>();
//                    transformMap.put("status", htmlZipStatus);
//                    transformMap.put("type", "html");
//                    if(htmlZipNode != null){
//                        transformMap.put("url", contextUrl + htmlZipNode.getUrl());
//                    }
//                    list.add(transformMap);
//                }
//            }
//        }
//        snapshotJson.put("formats", list);
//
        JSONArray configsJson = new JSONArray();
//        List< EmsScriptNode > configs =
//                        snapshot.getSourceAssocsNodesByType( "ems:configuredSnapshots",
//                                        workspace, null );
//        for (EmsScriptNode config: configs) {
//            JSONObject jsonObject = new JSONObject();
//            // since configuations are created in refs, may not have read permissions
//            // need to filter
//            // TODO: add into regression tests
//            if (config.hasPermission("Read")) {
//                jsonObject.put("name", config.getProperty(Acm.CM_NAME));
//                jsonObject.put("id", config.getNodeRef().getId());
//                configsJson.put( jsonObject );
//            }
//        }

        snapshotJson.put( "configurations", configsJson );

        return snapshotJson;
    }

    /**
     * Necessary for backwards compatibility with old tags and snapshots in Bender 2.0 release.
     * Since associations were still used then. Associations have all since been migrated to using
     * NodeRefs
     *
     * @param config
     * @param workspace
     * @param timestamp
     * @return
     * @throws JSONException
     */
    public JSONArray getProducts(EmsScriptNode config, WorkspaceNode workspace,
                    Date timestamp) throws JSONException {
        JSONArray productsJson = new JSONArray();

        List< EmsScriptNode > products =
                        config.getTargetAssocsNodesByType( "ems:configuredProducts",
                                        workspace, timestamp );
        for (EmsScriptNode product: products) {
            productsJson.put( product.toJSONObject(workspace, timestamp) );
        }

        return productsJson;
    }

    /**
     * Based on a specified snapshot node and corresponding view and configuration returns the
     * snapshot JSON - maybe put this in EmsScriptNode?
     * @param product
     * @param view
     * @param config
     * @return
     * @throws JSONException
     */
    @Deprecated
    public JSONObject getProductJson(EmsScriptNode product) throws JSONException {
        JSONObject productJson = new JSONObject();
        productJson.put(Sjm.SYSMLID, product.getSysmlId());
        productJson.put( "created",  EmsScriptNode.getIsoTime( (Date)product.getProperty( "cm:created" )));
        productJson.put( "creator", product.getProperty( "cm:modifier", false ) );

        return productJson;
    }


    /**
     * Updates the specified configuration with the posted JSON. Returns set of products to
     * be generated.
     * @param config
     * @param postJson
     * @param context
     * @param date
     * @return
     * @throws JSONException
     */
    public HashSet< String > updateConfiguration( EmsScriptNode config,
                    JSONObject postJson,
                    EmsScriptNode context,
                    WorkspaceNode workspace,
                    Date date )
                                    throws JSONException {
        HashSet<String> productSet = new HashSet<String>();
        if (postJson.has("name")) {
            // update the title, since title can handle special characters
            config.createOrUpdateProperty(Acm.CM_TITLE, postJson.getString("name"));
        }
        if (postJson.has("description")) {
            config.createOrUpdateProperty("cm:description", postJson.getString("description"));
        }
        if (postJson.has("timestamp")) {
            // if timestamp specified always use
            config.createOrUpdateProperty("view2:timestamp", TimeUtils.dateFromTimestamp(postJson.getString("timestamp")));
        } else if (date != null) { // && date.before( (Date)config.getProperty("cm:created") )) {
            // add in timestamp if supplied date is before the created date
            config.createOrUpdateProperty("view2:timestamp", date);
        }

        // these should be mutually exclusive
        if (postJson.has( "products")) {
            config.removeAssociations( "ems:configuredProducts" );
            JSONArray productsJson = postJson.getJSONArray( "products" );
            for (int ii = 0; ii < productsJson.length(); ii++) {
                Object productObject = productsJson.get( ii );
                String productId = "";
                if (productObject instanceof String) {
                    productId = (String) productObject;
                } else if (productObject instanceof JSONObject) {
                    productId = ((JSONObject)productObject).getString( Sjm.SYSMLID );
                }
                EmsScriptNode product = findScriptNodeById(productId, workspace, null, false);
                if (product != null) {
                    config.createOrUpdateAssociation( product, "ems:configuredProducts", true );
                    productSet.add( productId );
                }
            }
        } else if (postJson.has( "snapshots" )) {
            config.removeAssociations("ems:configuredSnapshots");
            JSONArray snapshotsJson = postJson.getJSONArray("snapshots");
            for (int ii = 0; ii < snapshotsJson.length(); ii++) {
                Object snapshotObject = snapshotsJson.get( ii );
                String snapshotId = "";
                if (snapshotObject instanceof String) {
                    snapshotId = (String) snapshotObject;
                } else if (snapshotObject instanceof JSONObject) {
                    snapshotId = ((JSONObject)snapshotObject).getString( "id" );
                }
                EmsScriptNode snapshot = findScriptNodeById(snapshotId, workspace, null, false);
                if (snapshot != null) {
                    config.createOrUpdateAssociation(snapshot, "ems:configuredSnapshots", true);
                }
            }
        }

        return productSet;
    }

    /**
     * Comparator sorts by ascending created date
     * @author cinyoung
     *
     */
    public static class EmsScriptNodeCreatedAscendingComparator implements Comparator<EmsScriptNode> {
        @Override
        public int compare(EmsScriptNode x, EmsScriptNode y) {
            Date xModified;
            Date yModified;

            xModified = (Date) x.getProperty("cm:created");
            yModified = (Date) y.getProperty("cm:created");

            if (xModified == null) {
                return -1;
            } else if (yModified == null) {
                return 1;
            } else {
                return (yModified.compareTo(xModified));
            }
        }
    }

    public void handleDeleteConfiguration( WebScriptRequest req ) {
        String configId = req.getServiceMatch().getTemplateVars().get("configurationId");

        NodeRef configNodeRef = NodeUtil.findNodeRefByAlfrescoId( configId );

        if (configNodeRef != null) {
            EmsScriptNode configNode = new EmsScriptNode(configNodeRef, services, response);
            configNode.makeSureNodeRefIsNotFrozen();
            configNode.addAspect( "ems:Deleted" );
        }
    }

    @Override
    protected Map< String, Object >
    executeImplImpl( WebScriptRequest req, Status status, Cache cache ) {
        // TODO Auto-generated method stub
        return null;
    }
}
