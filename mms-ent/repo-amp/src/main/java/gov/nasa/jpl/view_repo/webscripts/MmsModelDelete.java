package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.LogUtil;

// delete everything it contains
public class MmsModelDelete extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(MmsModelDelete.class);

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return false;
    }

    public MmsModelDelete() {
        super();
    }

    public MmsModelDelete(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        MmsModelDelete instance = new MmsModelDelete(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        JSONObject result = null;

        try {
            result = handleRequest(req);
            if (result != null) {
                model.put("res", result);
            } else {
                status.setCode(responseStatus.getCode());
                model.put("res", createResponseJson());
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        printFooter(user, logger, timer);

        return model;
    }

    protected JSONObject handleRequest(WebScriptRequest req) throws JSONException, IOException {
        JSONObject result = new JSONObject();
        JSONArray elements = new JSONArray();
        JSONObject commitElements = new JSONObject();
        JSONObject commit = new JSONObject();
        String user = AuthenticationUtil.getRunAsUser();
        WorkspaceNode workspace = getWorkspace(req, user);
        String projectId = getProjectId(req);
        JSONArray nodesToDelete = null;
        List<String> ids = new ArrayList<>();
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, workspace);

        String elementId = req.getServiceMatch().getTemplateVars().get("elementId");
        if (elementId != null) {
            ids.add(elementId);
        } else {
            try {
                JSONObject requestJson = (JSONObject) req.parseContent();
                if (requestJson.has("elements")) {
                    JSONArray elementsJson = requestJson.getJSONArray("elements");
                    if (elementsJson != null) {
                        for (int ii = 0; ii < elementsJson.length(); ii++) {
                            String id = elementsJson.getJSONObject(ii).getString(Sjm.SYSMLID);
                            ids.add(id);
                        }
                    }
                }
            } catch (Exception e) {
                response.append("Could not parse request body");
                responseStatus.setCode(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }
        }

        for (String id : ids) {
            if (emsNodeUtil.getNodeBySysmlid(id).length() > 0) {
                nodesToDelete = emsNodeUtil.getChildren(id);
                for (int i = 0; i < nodesToDelete.length(); i++) {
                    JSONObject node = nodesToDelete.getJSONObject(i);
                    JSONObject deletedElement = emsNodeUtil.deleteNode(node.optString(Sjm.SYSMLID));
                    if (deletedElement != null) {
                        elements.put(deletedElement);
                    }
                    logger.debug(String.format("Node: %s", node));
                }
            }
        }

        if (elements.length() > 0) {
            commitElements.put("addedElements", new JSONArray());
            commitElements.put("updatedElements", new JSONArray());
            commitElements.put("movedElements", new JSONArray());
            commitElements.put("deletedElements", elements);
            Map<String, JSONObject> foundElements = new HashMap<>();
            Map<String, String> foundParentElements = new HashMap<>();
            JSONObject formattedCommit =
                emsNodeUtil.processCommit(commitElements, user, foundElements, foundParentElements);
            String commitResults = emsNodeUtil.insertCommitIntoElastic(formattedCommit);
            emsNodeUtil.insertCommitIntoPostgres(commitResults);
            result.put("elements", elements);
            result.put("commitId", commitResults);
        }
        return result;
    }
}
