package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;

public class MmsSnapshotGet extends AbstractJavaWebScript {
	static Logger logger = Logger.getLogger(MmsSnapshotGet.class);

    public MmsSnapshotGet() {
        super();
    }

    public MmsSnapshotGet( Repository repository, ServiceRegistry services ) {
        this.repository = repository;
        this.services = services;
    }

    @Override
    protected boolean validateRequest( WebScriptRequest req, Status status ) {
        return false;
    }

    @Override
    protected  Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        MmsSnapshotGet instance = new MmsSnapshotGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected  Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        if (checkMmsVersions) {
            if (compareMmsVersions(req, getResponse(), getResponseStatus())) {
                model.put("res", createResponseJson());
                return model;
            }
        }

        MmsSnapshotGet instance = new MmsSnapshotGet(repository, getServices());

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("snapshots", instance.handleRequest(req));
            appendResponseStatusInfo( instance );
            if (!Utils.isNullOrEmpty(response.toString())) jsonObject.put("message", response.toString());
            model.put("res", NodeUtil.jsonToString( jsonObject, 2 ));
        } catch (InvalidNodeRefException inre) {
            model.put( "res", "{\"msg\": \"Snapshot not found\"}" );
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Snapshot does not exist");
        } catch (Exception e) {
            model.put("res", createResponseJson());
            if (e instanceof JSONException) {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"JSON creation error");
            } else {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    private JSONArray handleRequest( WebScriptRequest req ) throws JSONException, InvalidNodeRefException {
        String snapshotId = req.getServiceMatch().getTemplateVars().get( "snapshotId" );
        ConfigurationsWebscript configWs = new ConfigurationsWebscript(repository, services, response);

        WorkspaceNode workspace = getWorkspace( req );
        NodeRef snapshotNr = new NodeRef("workspace://SpacesStore/" + snapshotId);
        EmsScriptNode snapshot = new EmsScriptNode( snapshotNr, services, response );

        JSONObject jsonObject = configWs.getSnapshotJson( snapshot, null, workspace );
        JSONArray jsonArray = new JSONArray();
        jsonArray.put( jsonObject );
        return jsonArray;
    }
}
