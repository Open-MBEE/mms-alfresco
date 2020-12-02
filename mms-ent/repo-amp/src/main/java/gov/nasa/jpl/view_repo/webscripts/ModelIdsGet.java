package gov.nasa.jpl.view_repo.webscripts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.db.GraphInterface;
import gov.nasa.jpl.view_repo.db.Node;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

public class ModelIdsGet extends ModelGet {

    static Logger logger = Logger.getLogger(CfIdsGet.class);

    private static final String ELEMENTID = "elementId";

    public ModelIdsGet() {
        super();
    }

    public ModelIdsGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(
        WebScriptRequest req, Status status, Cache cache) {
        ModelIdsGet instance = new ModelIdsGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = handleElementGet(req, status);
        printFooter(user, logger, timer);
        return model;
    }

    protected Map<String, Object> handleElementGet(WebScriptRequest req, Status status) {

        Map<String, Object> model = new HashMap<>();
        JsonObject top = new JsonObject();

        if (validateRequest(req, status)) {
            try {
                top = getAllElementIds(req);
                if (!Utils.isNullOrEmpty(response.toString())) {
                    top.addProperty("message", response.toString());
                }
            } catch (Exception e) {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
            }
            status.setCode(responseStatus.getCode());
        }
        model.put(Sjm.RES, top);
        return model;
    }


    private JsonObject getAllElementIds(WebScriptRequest req) {
        String refId = getRefId(req);
        String projectId = getProjectId(req);
        String commit = req.getParameter(Sjm.COMMITID.replace("_", ""));
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        if (commit.isEmpty()) {
            commit = emsNodeUtil.getHeadCommit();
        }

        List<String> sysmlIds = emsNodeUtil.getModelAtCommit(commit);
        JsonArray array = new JsonArray();
        JsonObject result = new JsonObject();
        for (String s: sysmlIds) {
            JsonObject o = new JsonObject();
            o.addProperty("id", s);
            array.add(o);
        }
        result.add("elements", array);
        result.addProperty("commitId", commit);
        return result;
    }
}
