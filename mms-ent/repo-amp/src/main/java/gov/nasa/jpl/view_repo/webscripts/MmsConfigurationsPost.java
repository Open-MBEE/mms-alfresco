package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil;

public class MmsConfigurationsPost extends AbstractJavaWebScript {
	static Logger logger = Logger.getLogger(MmsConfigurationsPost.class);

    public MmsConfigurationsPost() {
        super();
    }

    public MmsConfigurationsPost( Repository repository, ServiceRegistry services ) {
        this.repository = repository;
        this.services = services;
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected  Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        MmsConfigurationsPost instance = new MmsConfigurationsPost(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected  Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();

        MmsConfigurationsPost instance = new MmsConfigurationsPost(repository, getServices());

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject = instance.handleUpdate( req );
            appendResponseStatusInfo( instance );
            if (!Utils.isNullOrEmpty(response.toString())) jsonObject.put("message", response.toString());
            model.put("res", NodeUtil.jsonToString( jsonObject, 2 ));
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

    private JSONObject handleUpdate(WebScriptRequest req) throws JSONException {
       /* WorkspaceNode workspace = getWorkspace( req );

        EmsScriptNode siteNode = getSiteNodeFromRequest(req, false);
        String siteNameFromReq = getSiteName( req );
        if ( siteNode == null && !Utils.isNullOrEmpty( siteNameFromReq )
             && !siteNameFromReq.equals( NO_SITE_ID ) ) {
            log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Could not find site");
            return new JSONObject();
        }

        String configId = req.getServiceMatch().getTemplateVars().get( "configurationId" );
        NodeRef configNode = NodeUtil.getNodeRefFromNodeId( configId );
        if (configNode == null) {
            log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Could not find configuration with id: %s",configId);
            return new JSONObject();
        }
        EmsScriptNode config = new EmsScriptNode(configNode, services);

        ConfigurationsWebscript configWs = new ConfigurationsWebscript( repository, services, response );
        JSONObject requestJson = //JSONObject.make(
                                                  (JSONObject)req.parseContent(); //);
        HashSet<String> productSet = configWs.updateTag( config, requestJson, siteNode, workspace, null );
        ConfigurationPost configPost = new ConfigurationPost( repository, services );
        String siteName = siteNode == null ? null : siteNode.getName();
        configPost.startAction( config, siteName, productSet, workspace, null );

        return configWs.getConfigJson( config, workspace, null );
        */
        return null;
    }
}
