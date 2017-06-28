package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
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
import gov.nasa.jpl.view_repo.util.LogUtil;

/**
 * Created by dank on 6/26/17.
 */
public class ProjectDelete extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ProjectGet.class);
    private PostgresHelper pgh;

    public ProjectDelete() {
        super();
    }

    public ProjectDelete(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Webscript entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ProjectDelete instance = new ProjectDelete(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        JSONObject json = null;

        try {
            if (validateRequest(req, status)) {

                String projectId = getProjectId(req);

                // Postgres helper to commit elasticids before dropping db
                ArrayList<String> commitList = getCommitElasticIDs(projectId);

                // Search and delete for all elements in elasticsearch with project id
                deleteCommitsInElastic(commitList);
                deleteElasticElements(projectId);
                // DROP DB
            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON could not be created\n");
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error stack trace:\n %s \n",
                e.getLocalizedMessage());
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        if (json == null) {
            model.put("res", createResponseJson());
        } else {
            model.put("res", json);
        }
        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    /**
     * Validate the request and check some permissions
     */
    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        return checkRequestContent(req);
    }

    /**
     * Finds and returns the elasticsearch IDS for the commit elements in the PostgreSQL Database
     *
     * @param projectId
     * @return CommitElasticIDs
     */
    protected ArrayList<String> getCommitElasticIDs(String projectId) {
        ArrayList<String> commitIds = new ArrayList<>();
        logger.info("PROJECT ID \n" + projectId);

        // Instantiate and configure pgh.
        pgh = new PostgresHelper();
        pgh.setProject(projectId);

        // Get all the commit ids from postgres
        List<Map<String, String>> commits = pgh.getAllCommits();
        for (Map<String, String> commit : commits) {
            commitIds.add(commit.get("commitId"));
        }
        return commitIds;
    }

    /**
     * Takes a list of Elastic IDs then performs a bulk delete.
     *
     * @param commitIds
     * @return
     */
    protected void deleteCommitsInElastic(ArrayList<String> commitIds) {
        logger.info("Deleting commits!");

        try {
            ElasticHelper elasticHelper = new ElasticHelper();
            elasticHelper.bulkDeleteByType("commit", commitIds);
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    protected void deleteElasticElements(String projectId) {
        logger.info("Deleting commits!");

        try {
            ElasticHelper elasticHelper = new ElasticHelper();
            elasticHelper.deleteElasticElements("_projectId", projectId);
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
