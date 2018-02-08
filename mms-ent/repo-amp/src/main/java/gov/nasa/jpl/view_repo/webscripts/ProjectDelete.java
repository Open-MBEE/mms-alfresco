package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.Sjm;

/**
 * Created by dank on 6/26/17.
 */
public class ProjectDelete extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ProjectDelete.class);
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
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil();
        JsonArray projects = new JsonArray();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();

        try {
            if (validateRequest(req, status)) {

                String projectId = getProjectId(req);
                JsonObject project = emsNodeUtil.getProject(projectId);
                if (project != null) {
                    projects.add(project);
                }

                // Delete the site from share
                deleteProjectSiteFolder(projectId);

                // Postgres helper to commit elasticids before dropping db
                ArrayList<String> commitList = getCommitElasticIDs(projectId);

                // Search and delete for all elements in elasticsearch with project id
                deleteCommitsInElastic(commitList, projectId);
                deleteElasticElements(projectId);

                // DROP DB
                dropDatabase(projectId);
                deleteProjectFromProjectsTable(projectId);
            }
        } catch (JsonParseException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create JSON response");
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }

        if(projects.size() == 0){
            model.put(Sjm.RES, createResponseJson());
        } else {
            JsonObject json = new JsonObject();
            json.add(Sjm.PROJECTS, projects);
            if (prettyPrint) {
            	Gson gson = new GsonBuilder().setPrettyPrinting().create();
                model.put(Sjm.RES, gson.toJson(json));
            } else {
                model.put(Sjm.RES, json);
            }
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
        logger.debug("Getting Commit Elastic IDs for \n" + projectId);

        // Instantiate and configure pgh.
        pgh = new PostgresHelper();
        pgh.setProject(projectId);

        // Get all the commit ids from postgres
        List<Map<String, String>> commits = pgh.getAllCommits();
        for (Map<String, String> commit : commits) {
            commitIds.add(commit.get("commitId"));
        }
        pgh.close();
        return commitIds;
    }

    /**
     * Takes a list of Elastic IDs then performs a bulk delete.
     *
     * @param commitIds
     * @return
     */
    protected void deleteCommitsInElastic(ArrayList<String> commitIds, String projectId) {
        logger.debug("Deleting commits in Elastic");

        try {
            ElasticHelper elasticHelper = new ElasticHelper();
            elasticHelper.bulkDeleteByType("commit", commitIds, projectId);
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Uses ElasticHelper to delete all elements on ElasticSearch based on the given projectId. Performs a delete
     * by query.
     * @param projectId
     */
    protected void deleteElasticElements(String projectId) {
        logger.debug("Deleting elastic elements for " + projectId);

        try {
            ElasticHelper elasticHelper = new ElasticHelper();
            elasticHelper.deleteElasticElements("_projectId", projectId);
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Drops the Postgres Database based on the given database name;
     * @param databaseName
     */
    protected void dropDatabase(String databaseName) {
        pgh = new PostgresHelper();
        pgh.dropDatabase(databaseName);
        pgh.close();
    }

    /**
     * Deletes the project entry from the projects table.
     * @param projectId
     */
    protected void deleteProjectFromProjectsTable(String projectId) {
        pgh = new PostgresHelper();
        pgh.deleteProjectFromProjectsTable(projectId);
        pgh.close();
    }

    /**
     * Deletes the site folder from the share
     * @param projectId
     */
    private void deleteProjectSiteFolder(String projectId){
        // Folder called namedProjectId
        pgh = new PostgresHelper();
        String org = pgh.getOrganizationFromProject(projectId);
        pgh.close();
        SiteInfo orgInfo = services.getSiteService().getSite(org);
        EmsScriptNode site = new EmsScriptNode(orgInfo.getNodeRef(), services);
        EmsScriptNode docLib = site.childByNamePath("documentLibrary");

        // Delete project folder in doc library
        docLib.childByNamePath(projectId).remove();
        // Delete site folder under org
        site.childByNamePath(projectId).remove();
    }
}
