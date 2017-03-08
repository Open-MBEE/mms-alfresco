package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;

public class MmsSnapshotsDelete extends AbstractJavaWebScript {
	static Logger logger = Logger.getLogger(MmsSnapshotsDelete.class);

    public MmsSnapshotsDelete() {
        super();
    }

    public MmsSnapshotsDelete( Repository repository, ServiceRegistry services ) {
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
        MmsSnapshotsDelete instance = new MmsSnapshotsDelete(repository, getServices());
            return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected  Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();

        MmsSnapshotsDelete instance = new MmsSnapshotsDelete(repository, getServices());

        JSONObject jsonObject = new JSONObject();
        try {
            instance.handleRequest(req);
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

    private void handleRequest( WebScriptRequest req ) throws JSONException {
        String snapshotId = req.getServiceMatch().getTemplateVars().get("snapshotId");
        NodeRef snapshotNodeRef = NodeUtil.findNodeRefByType( snapshotId, SearchType.CM_NAME, true,
                                                           null, null, true, services, false );
        if (snapshotNodeRef == null) {
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find snapshot");
        } else {
            EmsScriptNode snapshot = new EmsScriptNode(snapshotNodeRef, services, response);
            snapshot.delete();
        }
    }

}
