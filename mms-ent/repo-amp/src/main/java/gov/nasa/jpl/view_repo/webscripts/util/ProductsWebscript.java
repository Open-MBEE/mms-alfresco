package gov.nasa.jpl.view_repo.webscripts.util;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
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
 * Library class for manipulating products
 *
 * Pattern is handle does the retrieval.
 *
 * @author cinyoung
 *
 */
public class ProductsWebscript extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ProductsWebscript.class);

    public boolean simpleJson = false;

    // Cached keyed by Workspace, Timestamp, then Site to the JSON
    private static Map<String, Map<String, Map<String, JSONArray>>> productCache =
                    Collections.synchronizedMap( new HashMap<String, Map<String, Map<String, JSONArray>>>() );

    public ProductsWebscript( Repository repository, ServiceRegistry services,
                    StringBuffer response ) {
        super( repository, services, response );
    }

    public JSONArray handleProducts( WebScriptRequest req )
                    throws JSONException {
        JSONArray productsJson = new JSONArray();

        EmsScriptNode siteNode = null;
        EmsScriptNode mySiteNode = getSiteNodeFromRequest( req, false );

        if (!NodeUtil.exists( mySiteNode )) {
            log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Could not find site");
            return productsJson;
        }

        String configurationId = req.getServiceMatch().getTemplateVars().get( "configurationId" );
        if (configurationId == null) {
            // if no configuration id, get all products for site/context
            return handleContextProducts(req, siteNode);
        } else {
            // if configuration exists, get products for configuration
            NodeRef configNodeRef = NodeUtil.getNodeRefFromNodeId( configurationId );
            if (configNodeRef != null) {
                EmsScriptNode configNode = new EmsScriptNode(configNodeRef, services);
                return handleConfigurationProducts(req, configNode);
            } else {
                log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Could not find configuration with id %s", configurationId);
                return productsJson;
            }
        }
    }


    public JSONArray handleConfigurationProducts( WebScriptRequest req, EmsScriptNode config) throws JSONException {
        String timestamp = req.getParameter( "timestamp" );
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );

        WorkspaceNode workspace = getWorkspace( req );

        ConfigurationsWebscript configWs = new ConfigurationsWebscript( repository, services, response );
        return configWs.getProducts( config, workspace, dateTime );
    }

    public JSONArray handleContextProducts( WebScriptRequest req, EmsScriptNode siteNode) throws JSONException {
        JSONArray productsJson = new JSONArray();

        // get timestamp if specified
        String timestamp = req.getParameter( "timestamp" );
        Date dateTime = TimeUtils.dateFromTimestamp( timestamp );
        WorkspaceNode workspace = getWorkspace( req );
        String workspaceId = NodeUtil.getWorkspaceId( workspace, false );

        // check if it's already in the cache
        String siteName = getSiteName(req);
        if (timestamp == null) {
            productsJson = getLatestProducts( workspace, dateTime, siteName );
        } else {
            productsJson = getProductsFromCache( workspace, workspaceId, dateTime, timestamp, siteName );
            if (productsJson != null) {
                return productsJson;
            } else {
                productsJson = new JSONArray();
            }
        }

        return productsJson;
    }

    public JSONArray
    getProductSnapshots( String productId, String contextPath,
                    WorkspaceNode workspace, Date dateTime ) throws JSONException {
        EmsScriptNode product = findScriptNodeById( productId, workspace,
                        dateTime, false );

        JSONArray snapshotsJson = new JSONArray();
        List< EmsScriptNode > snapshotsList =
                        product.getTargetAssocsNodesByType( "view2:snapshots",
                                        workspace, null );
        // lets add products from node refs
        List<NodeRef> productSnapshots = product.getPropertyNodeRefs( "view2:productSnapshots", dateTime, workspace );
        for (NodeRef productSnapshotNodeRef: productSnapshots) {
            EmsScriptNode productSnapshot = new EmsScriptNode(productSnapshotNodeRef, services, response);
            snapshotsList.add( productSnapshot );
        }

        Collections.sort( snapshotsList,
                        new EmsScriptNode.EmsScriptNodeComparator() );
        for ( EmsScriptNode snapshot : snapshotsList ) {
            if (!snapshot.isDeleted()) {
                String id = snapshot.getSysmlId();
                Date date = snapshot.getLastModified( dateTime );

                JSONObject jsonObject = new JSONObject();
                jsonObject.put( "id", id );
                jsonObject.put( "created", EmsScriptNode.getIsoTime( date ) );
                jsonObject.put( "creator",
                                snapshot.getProperty( "cm:modifier", false ) );
                jsonObject.put( "url", contextPath + "/service/snapshots/"
                                + snapshot.getSysmlId() );
                jsonObject.put( "tag", getConfigurationSet( snapshot, workspace, dateTime ) );
                snapshotsJson.put( jsonObject );
            }
        }

        return snapshotsJson;
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return false;
    }

    public JSONArray handleProduct( String productId, boolean recurse,
                    WorkspaceNode workspace,
                    Date dateTime,
                    boolean gettingDisplayedElements,
                    boolean gettingContainedViews ) {
        JSONArray productsJson = new JSONArray();
        EmsScriptNode product = findScriptNodeById( productId, workspace, dateTime, false );

        if ( product == null ) {
            log( Level.ERROR,
                            HttpServletResponse.SC_NOT_FOUND, "Product not found with ID: %s", productId);
        }

        if ( checkPermissions( product, PermissionService.READ ) ) {
            try {
//                View v = new View( product );
//                if ( gettingDisplayedElements ) {
//                    Collection< EmsScriptNode > elems =
//                                    v.getDisplayedElements();
//                    elems = NodeUtil.getVersionAtTime( elems, dateTime );
//                    for ( EmsScriptNode n : elems ) {
//                        if ( simpleJson ) {
//                            productsJson.put( n.toSimpleJSONObject( workspace,dateTime ) );
//                        } else {
//                            productsJson.put( n.toJSONObject( workspace, dateTime ) );
//                        }
//                    }
////                } else if ( gettingContainedViews ) {
////                    Collection< EmsScriptNode > elems =
////                                    v.getContainedViews( recurse, workspace, dateTime, null );
////                    elems.add( product );
////                    for ( EmsScriptNode n : elems ) {
////                        if ( simpleJson ) {
////                            productsJson.put( n.toSimpleJSONObject( workspace, dateTime ) );
////                        } else {
////                            productsJson.put( n.toJSONObject( workspace, dateTime ) );
////                        }
////                    }
//                } else {
                    productsJson.put( product.toJSONObject( workspace, dateTime ) );
//                }
            } catch ( JSONException e ) {
                log( Level.ERROR,
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create JSON for product");
                e.printStackTrace();
            }
        }

        return productsJson;
    }

    @Override
    protected Map< String, Object >
    executeImplImpl( WebScriptRequest req, Status status, Cache cache ) {
        // TODO Auto-generated method stub
        return null;
    }

    protected synchronized JSONArray getProductsFromCache(WorkspaceNode workspace, String workspaceId,
                    Date dateTime, String timestamp, String siteName) {
        Map<String, JSONArray> timestampCheck = Utils.get(productCache, workspaceId, timestamp);
        JSONArray productsJson = Utils.get(productCache, workspaceId, timestamp, siteName);

        // it's possible a site has no documents, in that case, need to check that timestamp doesn't exist
        if (timestampCheck != null) {

        } else {
            if (productsJson == null) {
                buildCache(workspace, dateTime, timestamp);
            }

            // now that cache is built can look it up for returning, if not there, return empty
            productsJson = Utils.get(productCache, workspaceId, timestamp, siteName);

            if (productsJson == null) {
                productsJson = new JSONArray();
            }
        }

        return productsJson;
    }


    protected void buildCache(WorkspaceNode workspace, Date dateTime, String timestamp) {
        String workspaceId = NodeUtil.getWorkspaceId( workspace, false );

        // Search for all products within the project site:
        // don't specify a site, since this is running into issues and filter later
        Map< String, EmsScriptNode > nodeList = searchForElements(NodeUtil.SearchType.ASPECT.prefix,
                        Acm.ACM_PRODUCT, false,
                        workspace, dateTime,
                        null);
        Set<String> visitedNodes = new HashSet<String>();
        if (nodeList != null) {
            Set<EmsScriptNode> nodes = new HashSet<EmsScriptNode>(nodeList.values());
            for ( EmsScriptNode node : nodes) {
                if (node != null) {
                    JSONObject nodeJson = node.toJSONObject(workspace, dateTime);
                    String nodeSiteName = node.getSiteCharacterizationId(dateTime, workspace);

                    if (timestamp != null && nodeJson != null) {
                        JSONArray siteCache = Utils.get( productCache, workspaceId, timestamp, nodeSiteName );
                        if (siteCache == null) {
                            siteCache = new JSONArray();
                            if (nodeSiteName == null) nodeSiteName = "null";
                            Utils.put( productCache, workspaceId, timestamp, nodeSiteName, siteCache);
                        }

                        // in case we have duplicates in search result - seems to be some weird
                        // behavior going on
                        String sysmlid = nodeJson.getString( Sjm.SYSMLID );
                        if (sysmlid != null && !visitedNodes.contains( sysmlid )) {
                            siteCache.put( nodeJson );
                            visitedNodes.add( nodeJson.getString( Sjm.SYSMLID ) );
                        }
                    }

                }
            }
        }
    }

    protected JSONArray getLatestProducts(WorkspaceNode workspace, Date dateTime, String siteName) {
        JSONArray productsJson = new JSONArray();

        // Search for all products within the project site:
        // don't specify a site, since this is running into issues and filter later
        Map< String, EmsScriptNode > nodeList = searchForElements(NodeUtil.SearchType.ASPECT.prefix,
                        Acm.ACM_PRODUCT, false,
                        workspace, dateTime,
                        null);
        if (nodeList != null) {
            Set<EmsScriptNode> nodes = new HashSet<EmsScriptNode>(nodeList.values());
            for ( EmsScriptNode node : nodes) {
                if (node != null) {
                    String nodeSiteName = node.getSiteCharacterizationId(dateTime, workspace);
                    if (nodeSiteName != null && siteName.equals( nodeSiteName)) {
                        productsJson.put( node.toJSONObject( workspace, dateTime ) );
                    } else if (nodeSiteName == null) {
                        if (logger.isInfoEnabled()) {
                            logger.info( String.format("couldn't get node site name for sysmlid[%s] id[%s]",
                                            node.getSysmlId(), node.getId()));
                        }
                    }
                }
            }
        }

        return productsJson;
    }


    /**
     * Get the configuration set name associated with the snapshot, if available
     * @param dateTime
     * @param snapshotId
     * @return
     */
    protected String getConfigurationSet( EmsScriptNode snapshot,
                    WorkspaceNode workspace,
                    Date dateTime ) {
        if (snapshot != null) {
            List< EmsScriptNode > configurationSets =
                            snapshot.getSourceAssocsNodesByType( "ems:configuredSnapshots",
                                            workspace, null );
            if (!configurationSets.isEmpty()) {
                EmsScriptNode configurationSet = configurationSets.get(0);
                String configurationSetName = (String) configurationSet.getProperty(Acm.CM_NAME);
                return configurationSetName;
            }
        }

        return "";
    }

}
