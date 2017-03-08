package gov.nasa.jpl.view_repo.webscripts;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.Node;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

public class MmsSnapshotsGet extends AbstractJavaWebScript {
	static Logger logger = Logger.getLogger(MmsSnapshotsGet.class);

    public MmsSnapshotsGet() {
        super();
    }

    public MmsSnapshotsGet(Repository repository, ServiceRegistry services) {
        this.repository = repository;
        this.services = services;
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        MmsSnapshotsGet instance = new MmsSnapshotsGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();

        MmsSnapshotsGet instance = new MmsSnapshotsGet(repository, getServices());

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("snapshots", instance.handleRequest(req));
            appendResponseStatusInfo(instance);
            if (!Utils.isNullOrEmpty(response.toString()))
                jsonObject.put("message", response.toString());
            model.put("res", NodeUtil.jsonToString(jsonObject, 2));
        } catch (Exception e) {
            model.put("res", createResponseJson());
            if (e instanceof JSONException) {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON creation error");
            } else {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    private JSONArray handleRequest(WebScriptRequest req) throws JSONException {
        String configurationId = req.getServiceMatch().getTemplateVars().get("configurationId");
        if (configurationId != null) {
            return handleConfigurationSnapshot(req, configurationId);
        } else {
            String productId = req.getServiceMatch().getTemplateVars().get("productId");
            return handleProductSnapshot(req, productId);
        }
    }

    private JSONArray handleConfigurationSnapshot(WebScriptRequest req, String configurationId) throws JSONException {
        EmsScriptNode siteNode = getSiteNodeFromRequest(req, false);
        String siteNameFromReq = getSiteName(req);
        if (siteNode == null && !Utils.isNullOrEmpty(siteNameFromReq) && !siteNameFromReq.equals(NO_SITE_ID)) {
            log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Could not find site");
            return new JSONArray();
        }
        ConfigurationsWebscript configWs = new ConfigurationsWebscript(repository, services, response);

        WorkspaceNode workspace = getWorkspace(req);

        EmsScriptNode config = configWs.getConfiguration(configurationId);
        if (config == null) {
            return new JSONArray();
        }

        return configWs.getSnapshots(config, workspace);
    }

    private JSONArray handleProductSnapshot(WebScriptRequest req, String productId) throws JSONException {
        Date timestamp = TimeUtils.dateFromTimestamp(req.getParameter("timestamp"));
        WorkspaceNode workspace = getWorkspace(req);
        // TODO: need to search via timestamp
        // EmsScriptNode product = findScriptNodeById( productId, workspace, timestamp, false);
        JSONArray snapshotsJson = null;
        PostgresHelper pgh = new PostgresHelper(workspace);
        try {
            Node node = pgh.getNodeFromSysmlId(productId);
            ElasticHelper eh = new ElasticHelper();
            JSONObject jsonObject = eh.getElementByElasticId(node.getElasticId());
            if (jsonObject == null) {
                log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Could not find product");
                return new JSONArray();
            }
        } catch (Exception e) {

        }

        snapshotsJson = new JSONArray();

        // ConfigurationsWebscript configWs = new ConfigurationsWebscript(repository, services,
        // response);
        //
        // JSONArray snapshotsJson = new JSONArray();
        //
        // // for backwards compatibility, keep deprecated targetAssocsNodesByType
        // List< EmsScriptNode > snapshots =
        // product.getTargetAssocsNodesByType( "view2:snapshots",
        // workspace, null );
        // for (EmsScriptNode snapshot: snapshots) {
        // if ( !snapshot.isDeleted() ) {
        // snapshotsJson.put( configWs.getSnapshotJson( snapshot, product,
        // workspace ) );
        // }
        // }
        //
        // // for backwards compatibility, keep noderefs
        // List< NodeRef > productSnapshots = product.getPropertyNodeRefs( "view2:productSnapshots",
        // timestamp, workspace );
        // for (NodeRef productSnapshotNodeRef: productSnapshots) {
        // EmsScriptNode productSnapshot = new EmsScriptNode(productSnapshotNodeRef, services,
        // response);
        // if ( !productSnapshot.isDeleted() ) {
        // snapshotsJson.put( configWs.getSnapshotJson( productSnapshot, product, workspace ) );
        // }
        // }
        //
        // // all we really need to do is grab all the configurations and place them in as snapshots
        // // looking for all the snapshots without the associations will take too long otherwise
        // ConfigurationsWebscript configService = new ConfigurationsWebscript( repository,
        // services, response );
        // JSONArray configSnapshots = configService.handleConfigurations( req,
        // ConfigurationType.CONFIG_SNAPSHOT );
        // // need to filter out configurations that didn't exist before product existed
        // Date productDate = product.getCreationDate();
        // for (int ii = 0; ii < configSnapshots.length(); ii++) {
        // JSONObject snapshotJson = configSnapshots.getJSONObject(ii);
        // Date snapshotDate = TimeUtils.dateFromTimestamp( snapshotJson.getString( "created" ) );
        // if (snapshotDate.after( productDate )) {
        // snapshotsJson.put( snapshotJson );
        // }
        // }

        return snapshotsJson;
    }

}
