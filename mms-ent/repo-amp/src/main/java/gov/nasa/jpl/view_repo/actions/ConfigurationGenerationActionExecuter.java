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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Status;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.HostnameGet;

/**
 * Action for loading a configuration in the background asynchronously
 * @author cinyoung
 */
public class ConfigurationGenerationActionExecuter extends ActionExecuterAbstractBase {
	static Logger logger = Logger.getLogger(ConfigurationGenerationActionExecuter.class);

    /**
     * Injected variables from Spring configuration
     */
    private ServiceRegistry services;
    private Repository repository;

    private StringBuffer response;
    private Status responseStatus;

    // Parameter values to be passed in when the action is created
    public static final String NAME = "configurationGeneration";
    public static final String PARAM_SITE_NAME = "siteName";
    public static final String PARAM_PRODUCT_LIST = "docList";
    public static final String PARAM_TIME_STAMP = "timeStamp";
    public static final String PARAM_WORKSPACE = "workspace";

    public void setRepository(Repository rep) {
        repository = rep;
    }

    public void setServices(ServiceRegistry sr) {
        services = sr;
    }

    @Override
    protected void executeImpl(Action action, NodeRef nodeRef) {
        clearCache();
/*
//        new EmsTransaction(services, response, responseStatus) {
//            @Override
//            public void run() throws Exception {
//                executeImplImpl(action, nodeRef);
//            }
//        };
//    }
//
//    private void executeImplImpl(Action action, NodeRef nodeRef) {

        // Do not get an older version of the node based on the timestamp since
        // new snapshots should be associated with a new configuration. The
        // timestamp refers to the products, not the snapshots themselves.
        EmsScriptNode jobNode = new EmsScriptNode(nodeRef, services, response);
        // clear out any existing associated snapshots
        jobNode.removeAssociations("ems:configuredSnapshots");

        // Get timestamp if specified. This is for the products, not the
        // snapshots or configuration.
        Date dateTime = null;
        dateTime = (Date)action.getParameterValue(PARAM_TIME_STAMP);

        WorkspaceNode workspace = null;
        workspace = (WorkspaceNode)action.getParameterValue(PARAM_WORKSPACE);

        @SuppressWarnings("unchecked")
        HashSet<String> productList = (HashSet<String>) action.getParameterValue(PARAM_PRODUCT_LIST);

        String siteName = (String) action.getParameterValue(PARAM_SITE_NAME);
        String fndSiteName = null;
        // If siteName is null then we're searching for elements across all
        // sites in the workspace.
        if ( !Utils.isNullOrEmpty( siteName ) ) {
            if (logger.isDebugEnabled()) {
            	logger.debug("ConfigurationGenerationActionExecuter started execution of " + siteName);
            }
            SiteInfo siteInfo = services.getSiteService().getSite(siteName);
            if (siteInfo == null) {
            	if (logger.isDebugEnabled()) {
                	logger.debug("[ERROR]: could not find site: " + siteName);
            	}
                return;
            }
            NodeRef siteRef = siteInfo.getNodeRef();

            // If the version of the site ever changes, its products may also
            // change, so get the products for the version of the site according to
            // the specified date/time. Actually, this probably doesn't matter based
            // on how the site node is used.
           if ( dateTime != null ) {
                NodeRef vRef = NodeUtil.getNodeRefAtTime( siteRef, dateTime );
                if ( vRef != null ) siteRef = vRef;
            }
           EmsScriptNode site = new EmsScriptNode(siteRef, services, response);
           fndSiteName = site.getSiteName(dateTime, workspace);
        }

        Set<EmsScriptNode> productSet = new HashSet<EmsScriptNode>();

        Map< String, EmsScriptNode > nodeList = NodeUtil.searchForElements(NodeUtil.SearchType.ASPECT.prefix,
                                                                          Acm.ACM_PRODUCT, false,
                                                                          workspace, dateTime, services, response,
                                                                          responseStatus, fndSiteName);
        if (nodeList != null) {
            productSet.addAll( nodeList.values() );
        }

        // create snapshots of all documents
        // TODO: perhaps roll these in their own transactions
        String jobStatus = "Succeeded";
//        Set<EmsScriptNode> snapshots = new HashSet<EmsScriptNode>();
//        for (EmsScriptNode product: productSet) {
//                // only create the filtered list of documents
//                if (productList.isEmpty() || productList.contains(product.getSysmlId())) {
//                SnapshotPost snapshotService = new SnapshotPost(repository, services);
//                snapshotService.setRepositoryHelper(repository);
//                snapshotService.setServices(services);
//                snapshotService.setLogLevel(Level.DEBUG);
//                Status status = new Status();
//                EmsScriptNode snapshot =
//                        snapshotService.createSnapshot( product,
//                                                        product.getSysmlId(),
//                                                        workspace, dateTime );
//                response.append(snapshotService.getResponse().toString());
//                if (snapshot == null || status.getCode() != HttpServletResponse.SC_OK) {
//                    jobStatus = "Failed";
//                    response.append("[ERROR]: could not make snapshot for \t" + product.getProperty(Acm.ACM_NAME) + "\n");
//                }
//                else {
//                    response.append("[INFO]: Successfully created snapshot: \t" + snapshot.getProperty(Acm.CM_NAME) + "\n");
//                }
//                if (snapshot != null) {
//                    snapshots.add(snapshot);
//                }
//                }
//        }
//        // make relationships between configuration node and all the snapshots
//        for (EmsScriptNode snapshot: snapshots) {
//            jobNode.createOrUpdateAssociation(snapshot, "ems:configuredSnapshots", true);
//        }

        // save off the status of the job
        jobNode.setProperty("ems:job_status", jobStatus);

        // Save off the log
        EmsScriptNode logNode = ActionUtil.saveLogToFile(jobNode, "text/plain", services, response.toString());

        String hostname = ActionUtil.getHostName();
        if (!hostname.endsWith( ".jpl.nasa.gov" )) {
            hostname += ".jpl.nasa.gov";
        }
        HostnameGet hostnameGet = new HostnameGet(this.repository, this.services);
        String contextUrl = hostnameGet.getAlfrescoUrl() + "/alfresco";

        // Send off notification email
        String subject =
                "[EuropaEMS] Tag generation in workspace "
                        + WorkspaceNode.getWorkspaceName( workspace )
                        + ( siteName == null ? "" : ", siteName " + siteName )
                        + ": status = " + jobStatus;
        String msg = "Log URL: " + contextUrl + logNode.getUrl();
        // TODO: NOTE!!! The following needs to be commented out for local testing....
        ActionUtil.sendEmailToModifier(jobNode, msg, subject, services);

        if (logger.isDebugEnabled()) {
        	logger.debug("Completed configuration set");
        }*/
    }

    protected void clearCache() {
        response = new StringBuffer();
        responseStatus = new Status();
        NodeUtil.setBeenInsideTransaction( false );
        NodeUtil.setBeenOutsideTransaction( false );
        NodeUtil.setInsideTransactionNow( false );
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        // TODO Auto-generated method stub

    }
}
