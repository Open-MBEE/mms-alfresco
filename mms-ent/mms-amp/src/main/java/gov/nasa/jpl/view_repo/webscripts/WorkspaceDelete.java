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
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

public class WorkspaceDelete extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(WorkspaceDelete.class);

    public WorkspaceDelete() {
        super();
    }

    public WorkspaceDelete(Repository repositoryHelper, ServiceRegistry service) {
        super(repositoryHelper, service);
    }

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        String wsId = req.getServiceMatch().getTemplateVars().get(REF_ID);
        return checkRequestVariable(wsId, REF_ID);
    }

    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        WorkspaceDelete instance = new WorkspaceDelete(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        JSONObject object = null;
        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        try {
            if (validateRequest(req, status)) {
                String wsId = getRefId(req);
                String projectId = getProjectId(req);

                // can't delete master
                if (wsId.equals(NO_WORKSPACE_ID)) {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Cannot delete master workspace");
                } else {
                    EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, wsId);
                    emsNodeUtil.deleteRef(wsId);
                    EmsScriptNode target = getWorkspace(req);
                    if (target != null) {
                        object = printObject(target);
                        target.delete(); //this didn't actually delete the alfresco folder just added deleted aspect
                        responseStatus.setCode(HttpServletResponse.SC_OK);
                    } else {
                        log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Could not find workspace %s", wsId);
                    }
                }
            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create JSON response", e);
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal stack trace error:", e);
        }

        if (object == null) {
            model.put(Sjm.RES, createResponseJson());
        } else {
            try {
                if (!Utils.isNullOrEmpty(response.toString())) {
                    object.put("message", response.toString());
                }
                if (prettyPrint || accept.contains("webp")) {
                    model.put(Sjm.RES, object.toString(4));
                } else {
                    model.put(Sjm.RES, object);
                }
            } catch (JSONException e) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    private JSONObject printObject(EmsScriptNode workspace) throws JSONException {
        return new JSONObject();
    }
}

