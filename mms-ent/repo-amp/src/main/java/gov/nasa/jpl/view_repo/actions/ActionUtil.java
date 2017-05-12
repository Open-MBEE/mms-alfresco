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

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.springframework.extensions.webscripts.Status;

/**
 * Static class of Action utilities for saving log, sending email, etc.
 * @author cinyoung
 *
 */
public class ActionUtil {
    private static String hostname = null;

    // defeat instantiation
    private ActionUtil() {
        // do nothing
    }

    private static String getContextUrl() {

        String hostname = getHostName();
        if (hostname.endsWith("/" )) {
            hostname = hostname.substring( 0, hostname.lastIndexOf( "/" ) );
        }
        if (!hostname.contains( "jpl.nasa.gov" )) {
            hostname += ".jpl.nasa.gov";
        }
        return "https://" + hostname + "/alfresco";
    }

    /**
     * Send off an email to the modifier of the node
     * @param node      Node whose modifier should be sent an email
     * @param subject   Subjecto of message
     * @param services
     * @param response
     */
    public static void sendEmailToModifier(EmsScriptNode node, String subject, ServiceRegistry services,
                                           String logString, String ts1, String ts2, WorkspaceNode ws1,
                                           WorkspaceNode ws2) {

        // Save off the log
        EmsScriptNode logNode = ActionUtil.saveLogToFile(node, "text/plain", services, logString);

        String contextUrl = getContextUrl();
        String ws1id = WorkspaceNode.getId( ws1 );
        String ws2id = WorkspaceNode.getId( ws2 );

        String msg = "Log URL: " + contextUrl + logNode.getUrl();
        if (!Utils.isNullOrEmpty( contextUrl ) && !Utils.isNullOrEmpty( ws1id ) && !Utils.isNullOrEmpty( ws2id ) && !Utils.isNullOrEmpty( ts1 ) && !Utils.isNullOrEmpty( ts2 )) {
            String diffPath = "Diff Results: " + contextUrl + "/mmsapp/mms.html#/workspaces/"+ws2id+"/diff/"+ws2id+"/"+ts2+"/"+ws1id+"/"+ts1;
            msg = msg + "\n\n" + diffPath;
        }

        // Send off the notification email
        sendEmailToModifier(node, msg, subject, services);
    }

    /**
     * Send off an email to the modifier of the node
     * @param node      Node whose modifier should be sent an email
     * @param subject   Subjecto of message
     * @param services
     * @param response
     */
    public static void sendEmailToModifier(EmsScriptNode node, String subject, ServiceRegistry services,
                                           String logString) {

        // Save off the log
        EmsScriptNode logNode = ActionUtil.saveLogToFile(node, "text/plain", services, logString);

        String contextUrl = getContextUrl();

        // Send off the notification email
        String msg = "Log URL: " + contextUrl + logNode.getUrl();
        sendEmailToModifier(node, msg, subject, services);
    }

    /**
     * Send off an email to the modifier of the node
     * @param node      Node whose modifier should be sent an email
     * @param msg       Message to send modifier
     * @param subject   Subjecto of message
     * @param services
     * @param response
     */
    public static void sendEmailToModifier(EmsScriptNode node, String msg, String subject, ServiceRegistry services) {
        String username = (String)node.getProperty("cm:modifier", false);
        EmsScriptNode user = new EmsScriptNode(services.getPersonService().getPerson(username), services, new StringBuffer());
        String recipient = (String) user.getProperty("cm:email");

        String sender = NodeUtil.getHostname() + "@jpl.nasa.gov";
        sendEmailTo(sender, recipient, msg, subject, services);
    }

    /**
     * Send email to recipient
     * @param sender
     * @param recipient
     * @param msg
     * @param subject
     * @param services
     */
    public static void sendEmailTo(String sender, String recipient, String msg, String subject, ServiceRegistry services) {
        try {
            Action mailAction = services.getActionService().createAction(MailActionExecuter.NAME);
            mailAction.setParameterValue(MailActionExecuter.PARAM_SUBJECT, subject);
            mailAction.setParameterValue(MailActionExecuter.PARAM_TO, recipient);
            // strip out origin from message and sender as necessary, since url won't match
            sender = sender.replace("-origin", "");
            mailAction.setParameterValue(MailActionExecuter.PARAM_FROM, sender);
            msg = msg.replace("-origin", "");
            mailAction.setParameterValue(MailActionExecuter.PARAM_TEXT, msg);
            mailAction.setExecuteAsynchronously( true );
            services.getActionService().executeAction(mailAction, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves log file for the specified node (creates the log file if necessary)
     * @param node      Node to save file to
     * @param mimeType  Mimetype of file
     * @param services
     * @param data      Stringbuffer to save
     * @return  Log file
     */
    public static EmsScriptNode saveLogToFile(EmsScriptNode node, String mimeType, ServiceRegistry services, String data) {
        String logName = node.getProperty("cm:name") + ".log";

        // create logNode if necessary
        EmsScriptNode parent = node.getParent(null, node.getWorkspace(), false, true);
        EmsScriptNode logNode = parent.childByNamePath(logName);
        if (logNode == null) {
            logNode = parent.createNode(logName, "cm:content");
            if (!logNode.hasAspect("cm:versionable")) {
                logNode.makeSureNodeRefIsNotFrozen();
                logNode.addAspect("cm:versionable");
            }
            // TODO: check if node for log is of type Job
            node.createOrUpdateProperty("ems:job_log", logNode);
        }

        saveStringToFile(logNode, mimeType, services, data);

        node.getOrSetCachedVersion();
        if ( logNode != null ) logNode.getOrSetCachedVersion();

        return logNode;
    }

    public static void saveStringToFile(EmsScriptNode node, String mimeType, ServiceRegistry services, String data) {
        ContentWriter writer = services.getContentService().getWriter(node.getNodeRef(), ContentModel.PROP_CONTENT, true);
        node.transactionCheck();
        writer.putContent(data.toString());
        setContentDataMimeType(writer, node, mimeType, services);
    }

    /**
     * Set the content mimetype so Alfresco knows how to deliver the HTTP response header correctly
     * @param writer
     * @param node
     * @param mimetype
     * @param sr
     */
    public static void setContentDataMimeType(ContentWriter writer, EmsScriptNode node, String mimetype, ServiceRegistry sr) {
        ContentData contentData = writer.getContentData();
        contentData = ContentData.setMimetype(contentData, mimetype);
        node.makeSureNodeRefIsNotFrozen();
        node.transactionCheck();
        sr.getNodeService().setProperty(node.getNodeRef(), ContentModel.PROP_CONTENT, contentData);
        NodeUtil.propertyCachePut( node.getNodeRef(), NodeUtil.getShortQName( ContentModel.PROP_CONTENT ), contentData );
    }


    public static boolean jobExists( EmsScriptNode contextFolder, String jobName ) {
        EmsScriptNode jobPkgNode = contextFolder.childByNamePath("Jobs");
        if (jobPkgNode == null) return false;
        EmsScriptNode jobNode = jobPkgNode.childByNamePath(jobName);
        if (jobNode == null) return false;
        return jobNode.exists();
    }

    public static EmsScriptNode getOrCreateJob(EmsScriptNode contextFolder,
                                               String jobName, String jobType,
                                               Status status, StringBuffer response) {
        return getOrCreateJob(contextFolder, jobName, jobType, status, response, false);
    }

    private static EmsScriptNode getOrCreateJobImpl(EmsScriptNode jobPkgNode,
                                                  String jobName, String jobType,
                                                  Status status,
                                                  boolean generateName) {

        EmsScriptNode jobNode = jobPkgNode.childByNamePath(jobName);
        if (jobNode == null) {
            String filename = jobName;
            if (generateName) {
                Date now = new Date();
                filename = jobName + now.toString();
                filename = Integer.toString( filename.hashCode() );
            }
            jobNode = jobPkgNode.createNode(filename, jobType);
            if ( jobNode == null ) {
                status.setCode( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Could not create job, " + jobName + "." );
                return null;
            }
            jobNode.createOrUpdateProperty( Acm.CM_TITLE, jobName );
            if (generateName) {
                jobNode.createOrUpdateProperty( Acm.CM_NAME, "cm_" + jobNode.getId() );
            }
            jobNode.createOrUpdateProperty("cm:isContentIndexed", false);
            // Element cache does not support jobs
            //NodeUtil.addElementToCache( snapshotNode );
        } else if ( jobNode.isDeleted() ) {
            // resurrect
            jobNode.removeAspect( "ems:Deleted" );
        } else if ( !jobNode.exists() ) {
            // TODO -- REVIEW -- Don't know if this works or if it's possible to get here.
            jobNode = jobPkgNode.createNode(jobName, jobType);
            jobNode.createOrUpdateProperty("cm:isContentIndexed", false);
        } else {
            // don't use getProperty since cache isn't outdated for jobs at the moment.
            String jobStatus = (String)jobNode.getProperties().get("{http://jpl.nasa.gov/model/ems/1.0}job_status");
            if (jobStatus != null && jobStatus.equals("Active")) {
                Date modified = (Date)jobNode.getProperty("cm:modified");
                status.setCode(HttpServletResponse.SC_CONFLICT,
                               String.format( "Background job for project started at %s " +
                                              "is still active. If job is hung, use share " +
                                              "to modify state at https://%s" +
                                              "/share/page/document-details?nodeRef=%s",
                                              modified, NodeUtil.getHostname(),
                                              jobNode.getNodeRef()));
                return null;
            }
        }

        jobNode.createOrUpdateProperty("ems:job_status", "Active");

        jobNode.getOrSetCachedVersion();
        jobPkgNode.getOrSetCachedVersion();

        // set the owner to original user say they can modify
        jobNode.setOwner( AuthenticationUtil.getFullyAuthenticatedUser() );

        return jobNode;
    }

    /**
     * Create a diff job inside contextFolder/Jobs/ws1Name/ws2Name
     *
     * @param contextFolder Folder to create the job node
     * @param jobName       String of the filename
     * @param jobType       The type of job being created
     * @param status        Initial status to put the job node in
     * @param response      Response buffer to return to client
     * @param generateName  If true, jobName will be used as cm:title and the uuid will be
     *                      returned as the cm:name. If false, jobName is cm:name. This is
     *                      necessary when the jobName can handle special characters.
     * @return The created job node
     */
    public static EmsScriptNode getOrCreateDiffJob(EmsScriptNode contextFolder,
                                                   String ws1Name, String ws2Name,
                                                   Date timestamp1, Date timestamp2,
                                                   String timeString1, String timeString2,
                                                   String jobName,
                                                   Status status, StringBuffer response,
                                                   boolean generateName) {

        EmsScriptNode jobNode = null;

        // to make sure no permission issues, run as admin
        String origUser = AuthenticationUtil.getFullyAuthenticatedUser();
        AuthenticationUtil.setRunAsUser( "admin" );

        EmsScriptNode jobPkgNode = contextFolder.childByNamePath("Jobs");
        if (jobPkgNode == null) {
            jobPkgNode = contextFolder.createFolder("Jobs", "cm:folder");
            jobPkgNode.setPermission( "SiteCollaborator", "GROUP_EVERYONE" );
            contextFolder.getOrSetCachedVersion();
        }

        if (jobPkgNode != null) {
            // Create workspace folders:
            EmsScriptNode ws1Folder = jobPkgNode.childByNamePath(ws1Name);
            if (ws1Folder == null) {
                ws1Folder = jobPkgNode.createFolder(ws1Name, "cm:folder");
                ws1Folder.setPermission( "SiteCollaborator", "GROUP_EVERYONE" );
                ws1Folder.getOrSetCachedVersion();
            }

            if (ws1Folder != null) {
                EmsScriptNode ws2Folder = ws1Folder.childByNamePath(ws2Name);
                if (ws2Folder == null) {
                    ws2Folder = ws1Folder.createFolder(ws2Name, "cm:folder");
                    ws2Folder.setPermission( "SiteCollaborator", "GROUP_EVERYONE" );
                    ws2Folder.getOrSetCachedVersion();
                }

                if (ws2Folder != null) {
                    jobNode = getOrCreateJobImpl(ws2Folder, jobName, "ems:DiffJob", status, generateName);
                    if (jobNode != null && timeString1 != null && timeString2 != null) {
                        jobNode.createOrUpdateProperty( "ems:timestamp1", timeString1 );
                        jobNode.createOrUpdateProperty( "ems:timestamp2", timeString2 );

                    }
                }
            }
        }

        // make sure we're running back as the originalUser
        AuthenticationUtil.setRunAsUser( origUser );

        return jobNode;
    }

    /**
     * Get a diff job inside contextFolder/Jobs/ws1Name/ws2Name
     * Must supply jobName.
     *
     * @param contextFolder Folder to create the job node
     * @param jobName       String of the filename
     * @param services
     * @param response
     * @return The found diff job node
     */
    public static EmsScriptNode getDiffJob(EmsScriptNode contextFolder,
                                           WorkspaceNode ws1, WorkspaceNode ws2,
                                           String jobName,
                                           ServiceRegistry services,
                                           StringBuffer response) {

        EmsScriptNode jobNode = null;
        String ws1Name = WorkspaceNode.getWorkspaceName(ws1);
        String ws2Name = WorkspaceNode.getWorkspaceName(ws2);

        // to make sure no permission issues, run as admin
        String origUser = AuthenticationUtil.getFullyAuthenticatedUser();
        AuthenticationUtil.setRunAsUser( "admin" );

        EmsScriptNode jobPkgNode = contextFolder.childByNamePath("Jobs");

        if (jobPkgNode != null) {
            EmsScriptNode ws1Folder = jobPkgNode.childByNamePath(ws1Name);
            if (ws1Folder != null) {
                EmsScriptNode ws2Folder = ws1Folder.childByNamePath(ws2Name);
                if (ws2Folder != null) {
                    // Find the correct diff job node based on the job name:
                    jobNode = ws2Folder.childByNamePath(jobName);
                }
            }
        }

        // make sure we're running back as the originalUser
        AuthenticationUtil.setRunAsUser( origUser );

        return jobNode;
    }

    /**
     * Create a job inside a particular site
     * @param contextFolder Folder to create the job node
     * @param jobName       String of the filename
     * @param jobType       The type of job being created
     * @param status        Initial status to put the job node in
     * @param response      Response buffer to return to client
     * @param generateName  If true, jobName will be used as cm:title and the uuid will be
     *                      returned as the cm:name. If false, jobName is cm:name. This is
     *                      necessary when the jobName can handle special characters.
     * @return The created job node
     */
    public static EmsScriptNode getOrCreateJob(EmsScriptNode contextFolder,
                                               String jobName, String jobType,
                                               Status status, StringBuffer response,
                                               boolean generateName) {

        EmsScriptNode jobNode = null;

        // to make sure no permission issues, run as admin
        String origUser = AuthenticationUtil.getFullyAuthenticatedUser();
        AuthenticationUtil.setRunAsUser( "admin" );

        EmsScriptNode jobPkgNode = contextFolder.childByNamePath("Jobs");
        if (jobPkgNode == null) {
            jobPkgNode = contextFolder.createFolder("Jobs", "cm:folder");
            jobPkgNode.setPermission( "SiteCollaborator", "GROUP_EVERYONE" );
            contextFolder.getOrSetCachedVersion();
            WorkspaceNode ws = contextFolder.getWorkspace();
            if ( ws != null ) {
                jobPkgNode.setWorkspace( ws );
            }
        }

        if (jobPkgNode != null) {
            jobNode = getOrCreateJobImpl(jobPkgNode, jobName, jobType, status, generateName);
        }

        // make sure we're running back as the originalUser
        AuthenticationUtil.setRunAsUser( origUser );

        return jobNode;
    }

    public static EmsScriptNode getJob(EmsScriptNode siteNode, String jobName) {
        EmsScriptNode jobPkgNode = siteNode.childByNamePath("Jobs");
        if (jobPkgNode != null) {
            return jobPkgNode.childByNamePath( jobName );
        }

        return null;
    }

    public static void setJobStatus(EmsScriptNode jobNode, String value) {
        jobNode.createOrUpdateProperty("ems:job_status", value);
    }

    public static String getHostName() {
            if (hostname == null) {
                Process tr;
            try {
                tr = Runtime.getRuntime().exec( new String[]{ "hostname" } );
                    BufferedReader rd = new BufferedReader( new InputStreamReader( tr.getInputStream() ) );
                    hostname = rd.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            }
        return hostname;
    }
}
