/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory,
 *    nor the names of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.nasa.jpl.view_repo.actions;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

/**
 * Action for loading the project model in the background asynchronously
 *
 * @author cinyoung
 */
public class ModelLoadActionExecuter extends ActionExecuterAbstractBase {
    /**
     * Injected variables from Spring configuration
     */
    private ServiceRegistry services;
    private Repository repository;

    private StringBuffer response;
    private Status responseStatus;
    WorkspaceNode workspace = null;

    // Parameter values to be passed in when the action is created
    public static final String NAME = "modelLoad";
    public static final String PARAM_PROJECT_NAME = "projectName";
    public static final String PARAM_PROJECT_ID = "projectId";
    public static final String PARAM_PROJECT_NODE = "projectNode";
    public static final String PARAM_WORKSPACE_ID = "workspaceId";

    static Logger logger = Logger.getLogger(ModelLoadActionExecuter.class);

    public void setRepository(Repository rep) {
        repository = rep;
    }

    public void setServices(ServiceRegistry sr) {
        services = sr;
    }

    @Override
    protected void executeImpl(Action action, NodeRef nodeRef) {
        final Timer timer = new Timer();
        final String projectId = (String) action.getParameterValue(PARAM_PROJECT_ID);
        final String projectName = (String) action.getParameterValue(PARAM_PROJECT_NAME);
        EmsScriptNode projectNode = (EmsScriptNode) action.getParameterValue(PARAM_PROJECT_NODE);
        final String workspaceId = (String) action.getParameterValue(PARAM_WORKSPACE_ID);
        if (logger.isDebugEnabled())
            logger.debug("started execution of " + projectName + " [id: " + projectId + "]");
        clearCache();

        new EmsTransaction(services, response, responseStatus) {
            @Override
            public void run() throws Exception {
                workspace = WorkspaceNode.getWorkspaceFromId(workspaceId, services, response, responseStatus, // false
                        null);
            }
        };

        // Parse the stored file for loading
        final EmsScriptNode jsonNode = new EmsScriptNode(nodeRef, services, response);
        ContentReader reader = services.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
        JSONObject content = null;
        try {
            content = new JSONObject(reader.getContentString());
        } catch (ContentIOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Update the model

        String jobStatus = "Failed";
        if (content == null) {
            response.append("ERROR: Could not load JSON file for job\n");
        } else {
            Status status = new Status();
//            ModelContext modelContext = new ModelContext(false, workspace, true, null, projectName, projectNode, null,
//                    null);
//            ServiceContext serviceContext = new ServiceContext(false, false, 1, false, false, repository, services,
//                    response, status);
//            loadJson(content, modelContext, serviceContext);
            if (status.getCode() == HttpServletResponse.SC_OK) {
                jobStatus = "Succeeded";
            }
            // ModelPost modelService = new ModelPost(repository, services);
            // modelService.setLogLevel(Level.DEBUG);
            // modelService.setRunWithoutTransactions(false);
            // modelService.setProjectNode( projectNode );
            // Status status = new Status();
            // try {
            // // FIXME: make sure this all matches with ModelService
            // handleUpdate
            // Set<EmsScriptNode> elements =
            // modelService.createOrUpdateModel(content, status, workspace,
            // null, true);
            // modelService.addRelationshipsToProperties( elements, workspace );
            // } catch (Exception e) {
            // status.setCode(HttpServletResponse.SC_BAD_REQUEST);
            // response.append("ERROR: could not parse request\n");
            // e.printStackTrace();
            // }
            // if (status.getCode() == HttpServletResponse.SC_OK) {
            // jobStatus = "Succeeded";
            // }
            // response.append(modelService.getResponse().toString());
            if (logger.isDebugEnabled())
                logger.debug("completed model load with status [" + jobStatus + "]");
        }

        final String jobStatusFinal = jobStatus;
        new EmsTransaction(services, response, responseStatus) {
            @Override
            public void run() throws Exception {

                // set the status
                jsonNode.setProperty("ems:job_status", jobStatusFinal);

                // Send off the notification email
                String subject = "Workspace " + workspaceId + " Project " + projectName + " load completed";
                ActionUtil.sendEmailToModifier(jsonNode, subject, services, response.toString());

                if (logger.isDebugEnabled())
                    logger.debug("ModelLoadActionExecuter: " + timer);
            }
        };

    }

//    public static Set<EmsScriptNode> loadJson(JSONObject content, ModelContext modelContext,
//            ServiceContext serviceContext) {
//        if (modelContext == null)
//            modelContext = new ModelContext();
//        if (serviceContext == null)
//            serviceContext = new ServiceContext();
//        ModelPost modelService = new ModelPost(serviceContext.repository, serviceContext.services);
//        modelService.setLogLevel(Level.DEBUG);
//        // modelService.setRunWithoutTransactions(false);
//        // TODO
//        // modelService.setProjectNode(modelContext.projectNode);
//        // boolean succeeded = false;
//        Set<EmsScriptNode> elements = null;
//        try {
//            if (serviceContext.status == null)
//                serviceContext.status = new Status();
//            if (serviceContext.response == null)
//                serviceContext.response = new StringBuffer();
//            // FIXME: make sure this all matches with ModelService handleUpdate
//
//            // TODO
//            // elements = modelService.createOrUpdateModel(content,
//            // serviceContext.status, modelContext.workspace, null,
//            // true);
//
//            // TODO
//            // modelService.addRelationshipsToProperties(elements,
//            // modelContext.workspace);
//        } catch (Exception e) {
//            serviceContext.status.setCode(HttpServletResponse.SC_BAD_REQUEST);
//            serviceContext.response.append("ERROR: could not parse request\n");
//            e.printStackTrace();
//        }
//        // if (status.getCode() == HttpServletResponse.SC_OK) {
//        // succeeded = true;
//        // }
//        serviceContext.response.append(modelService.getResponse().toString());
//        return elements;
//    }

    protected void clearCache() {
        response = new StringBuffer();
        responseStatus = new Status();
        NodeUtil.setBeenInsideTransaction(false);
        NodeUtil.setBeenOutsideTransaction(false);
        NodeUtil.setInsideTransactionNow(false);
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        // TODO Auto-generated method stub

    }
}
