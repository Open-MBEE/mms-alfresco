package gov.nasa.jpl.view_repo.webscripts;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.Sjm;
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

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;

public class WorkspaceHistoryGet extends AbstractJavaWebScript{
    static Logger logger = Logger.getLogger(WorkspaceHistoryGet.class);

    public WorkspaceHistoryGet(){
        super();
    }

    public WorkspaceHistoryGet(Repository repositoryHelper, ServiceRegistry service){
        super(repositoryHelper, service);
    }

    @Override
    protected Map<String, Object> executeImpl (WebScriptRequest req, Status status, Cache cache) {
        WorkspaceHistoryGet instance = new WorkspaceHistoryGet(repository, getServices());
        return instance.executeImplImpl( req, status, cache );
    }

    @Override
    protected Map<String, Object> executeImplImpl (WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        JSONObject object = null;
        String projectId = getProjectId(req);
        String limit = req.getParameter("limit");
        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        try{
            if(validateRequest(req, status)){
                String wsID = req.getServiceMatch().getTemplateVars().get(REF_ID);
                object = getRefHistory(req, projectId, wsID);
            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON object could not be created \n");
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error stack trace \n");
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        if(object == null){
            model.put("res", createResponseJson());
        } else {
            try{
                if (!Utils.isNullOrEmpty(response.toString())) {
                    object.put("message", response.toString());
                }
                if (prettyPrint || accept.contains("webp")) {
                    model.put("res", object.toString(4));
                } else {
                    model.put("res", object);
                }
            } catch (JSONException e){
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                model.put("res", createResponseJson());
            }
        }
        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;

    }

    protected JSONObject getRefHistory(WebScriptRequest req, String projectId, String refId) {
        JSONObject json = new JSONObject();
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, NO_WORKSPACE_ID);
        JSONArray commits = emsNodeUtil.getRefHistory(refId);
        String timestamp = req.getParameter("maxTimestamp");

        if (timestamp != null && !timestamp.isEmpty()) {
            for (int i = 0; i < commits.length(); i++) {
                JSONObject current = commits.getJSONObject(i);
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                Date requestedTime;
                Date currentTime;
                try {
                    requestedTime = df.parse(timestamp);
                    currentTime = df.parse(current.getString(Sjm.CREATED));
                    if (requestedTime.getTime() >= currentTime.getTime()) {
                        commits = new JSONArray().put(current);
                        break;
                    }
                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
                    }
                }
            }
        }

        json.put("commits" , commits);
        return json;
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        String wsId = req.getServiceMatch().getTemplateVars().get(REF_ID);
        return checkRequestContent(req) && checkRequestVariable(wsId, REF_ID);
    }

}
