package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.db.Artifact;
import gov.nasa.jpl.view_repo.util.*;
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
import gov.nasa.jpl.view_repo.util.LogUtil;

// delete everything it contains
public class ArtifactDelete extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ModelDelete.class);

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return false;
    }

    public ArtifactDelete() {
        super();
    }

    public ArtifactDelete(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ArtifactDelete instance = new ArtifactDelete(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        JsonObject result = null;

        try {
            result = handleRequest(req, status, user);
            if (result != null) {
                model.put(Sjm.RES, result);
            } else {
                status.setCode(responseStatus.getCode());
                model.put(Sjm.RES, createResponseJson());
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        printFooter(user, logger, timer);

        return model;
    }

    protected JsonObject handleRequest(WebScriptRequest req, final Status status, String user)
        throws JSONException, IOException {
        JsonObject result = new JsonObject();
        String date = TimeUtils.toTimestamp(new Date().getTime());

        JsonObject res = new JsonObject();
        String commitId = UUID.randomUUID().toString();
        JsonObject commit = new JsonObject();
        commit.addProperty(Sjm.ELASTICID, commitId);
        JsonArray commitDeleted = new JsonArray();
        JsonArray deletedElements = new JsonArray();

        String projectId = getProjectId(req);
        String refId = getRefId(req);

        Set<String> ids = new HashSet<>();
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        String artifactId = req.getServiceMatch().getTemplateVars().get("artifactId");
        if (artifactId != null && !artifactId.contains("holding_bin") && !artifactId.contains("view_instances_bin")) {
            ids.add(artifactId);
        } else {
            try {
                JsonObject requestJson = JsonUtil.buildFromString(req.getContent().getContent());
                this.populateSourceApplicationFromJson(requestJson);
                if (requestJson.has(Sjm.ARTIFACTS)) {
                    JsonArray elementsJson = requestJson.get(Sjm.ARTIFACTS).getAsJsonArray();
                    if (elementsJson != null) {
                        for (int ii = 0; ii < elementsJson.size(); ii++) {
                            String id = elementsJson.get(ii).getAsJsonObject().get(Sjm.SYSMLID).getAsString();
                            if (!id.contains("holding_bin") && !id.contains("view_instances_bin")) {
                                ids.add(id);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                response.append("Could not parse request body");
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, String.format("%s", LogUtil.getStackTrace(e)));
                return null;
            }
        }
        for (String id : ids) {
            Artifact artifact = emsNodeUtil.getArtifact(id, false);
            JsonObject obj = new JsonObject();
            obj.addProperty(Sjm.SYSMLID, artifact.getSysmlId());
            deletedElements.add(obj);
            obj.addProperty(Sjm.ELASTICID, artifact.getId());
            commitDeleted.add(obj);
        }

        if (deletedElements.size() > 0) {
            result.add("addedElements", new JsonArray());
            result.add("updatedElements", new JsonArray());
            result.add("deletedElements", deletedElements);
            commit.add("added", new JsonArray());
            commit.add("updated", new JsonArray());
            commit.add("deleted", commitDeleted);
            commit.addProperty(Sjm.CREATOR, user);
            commit.addProperty(Sjm.CREATED, date);
            result.add("commit", commit);
            if (CommitUtil.sendDeltas(result, projectId, refId, requestSourceApplication, services, false, true)) {
                res.add(Sjm.ARTIFACTS, deletedElements);
                res.addProperty(Sjm.CREATOR, user);
                res.addProperty(Sjm.COMMITID, commitId);
            } else {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
                    "Commit failed, please check server logs for failed items");
                return null;
            }
        }
        return res;
    }
}
