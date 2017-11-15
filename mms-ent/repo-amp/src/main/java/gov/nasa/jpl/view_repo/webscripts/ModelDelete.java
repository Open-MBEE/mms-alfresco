package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.util.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import gov.nasa.jpl.view_repo.util.SerialJSONArray;
import org.json.JSONException;
import gov.nasa.jpl.view_repo.util.SerialJSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.LogUtil;

// delete everything it contains
public class ModelDelete extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ModelDelete.class);

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return false;
    }

    public ModelDelete() {
        super();
    }

    public ModelDelete(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ModelDelete instance = new ModelDelete(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        SerialJSONObject result = null;

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

    protected SerialJSONObject handleRequest(WebScriptRequest req, final Status status, String user) throws JSONException, IOException {
        SerialJSONObject result = new SerialJSONObject();
        String date = TimeUtils.toTimestamp(new Date().getTime());

        SerialJSONObject res = new SerialJSONObject();
        String commitId = UUID.randomUUID().toString();
        SerialJSONObject commit = new SerialJSONObject();
        commit.put(Sjm.ELASTICID, commitId);
        SerialJSONArray commitDeleted = new SerialJSONArray();
        SerialJSONArray deletedElements = new SerialJSONArray();

        String projectId = getProjectId(req);
        String refId = getRefId(req);

        Set<String> ids = new HashSet<>();
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        Set<String> elasticIds = new HashSet<>();
        String elementId = req.getServiceMatch().getTemplateVars().get("elementId");
        if (elementId != null && !elementId.contains("holding_bin") && !elementId.contains("view_instances_bin")) {
            ids.add(elementId);
        } else {
            try {
                SerialJSONObject requestJson = new SerialJSONObject(req.getContent().getContent());
                this.populateSourceApplicationFromJson(requestJson);
                if (requestJson.has(Sjm.ELEMENTS)) {
                    SerialJSONArray elementsJson = requestJson.getJSONArray(Sjm.ELEMENTS);
                    if (elementsJson != null) {
                        for (int ii = 0; ii < elementsJson.length(); ii++) {
                            String id = elementsJson.getJSONObject(ii).getString(Sjm.SYSMLID);
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
            if (emsNodeUtil.getNodeBySysmlid(id).length() > 0) {
                SerialJSONArray nodesToDelete = emsNodeUtil.getChildren(id);
                for (int i = 0; i < nodesToDelete.length(); i++) {
                    SerialJSONObject node = nodesToDelete.getJSONObject(i);
                    if (node != null && !elasticIds.contains(node.getString(Sjm.ELASTICID))) {
                        deletedElements.put(node);
                        SerialJSONObject obj = new SerialJSONObject();
                        obj.put(Sjm.SYSMLID, node.getString(Sjm.SYSMLID));
                        obj.put(Sjm.ELASTICID, node.getString(Sjm.ELASTICID));
                        commitDeleted.put(obj);
                        elasticIds.add(node.getString(Sjm.ELASTICID));
                    }
                    log(Level.DEBUG, String.format("Node: %s", node));
                }
            }
        }

        if (deletedElements.length() > 0) {
            result.put("addedElements", new SerialJSONArray());
            result.put("updatedElements", new SerialJSONArray());
            result.put("deletedElements", deletedElements);
            commit.put("added", new SerialJSONArray());
            commit.put("updated", new SerialJSONArray());
            commit.put("deleted", commitDeleted);
            commit.put(Sjm.CREATOR, user);
            commit.put(Sjm.CREATED, date);
            result.put("commit", commit);
            if (CommitUtil.sendDeltas(result, projectId, refId, requestSourceApplication, services, false)) {
                if (!elasticIds.isEmpty()) {
                    emsNodeUtil.updateElasticRemoveRefs(elasticIds);
                }
                res.put(Sjm.ELEMENTS, deletedElements);
                res.put(Sjm.CREATOR, user);
                res.put(Sjm.COMMITID, commitId);
            } else {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Commit failed, please check server logs for failed items");
                return null;
            }
        }
        return res;
    }
}
