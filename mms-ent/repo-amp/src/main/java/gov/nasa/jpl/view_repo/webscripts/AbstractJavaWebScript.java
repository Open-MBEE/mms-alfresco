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
package gov.nasa.jpl.view_repo.webscripts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.util.SitePermission;


/**
 * Base class for all EMS Java backed webscripts. Provides helper functions and
 * key variables necessary for execution. This provides most of the capabilities
 * that were in utils.js
 *
 * @author cinyoung
 *
 */
public abstract class AbstractJavaWebScript extends DeclarativeJavaWebScript {
    private static Logger logger = Logger.getLogger(AbstractJavaWebScript.class);

    public static boolean checkMmsVersions = false;
    private JSONObject privateRequestJSON = null;

    // injected members
    protected ServiceRegistry services;        // get any of the Alfresco services
    protected Repository repository;        // used for lucene search

    protected ScriptNode companyhome;

    // response to HTTP request, made as class variable so all methods can update
    protected StringBuffer response = new StringBuffer();
    protected Status responseStatus = new Status();

    // keeps track of who made the call to the service
    protected String requestSourceApplication = null;

    protected boolean prettyPrint = false;

    protected void initMemberVariables(String siteName) {
        companyhome = new ScriptNode(repository.getCompanyHome(), services);
    }

    public void setRepositoryHelper(Repository repositoryHelper) {
        if ( repositoryHelper == null ) return;
        this.repository = repositoryHelper;
    }

    public void setServices(ServiceRegistry registry) {
        if ( registry == null ) return;
        this.services = registry;
    }

    public AbstractJavaWebScript( Repository repository,
                                  ServiceRegistry services,
                                  StringBuffer response ) {
        this.setRepositoryHelper( repository );
        this.setServices( services );
        this.response = response ;
    }

    public AbstractJavaWebScript(Repository repositoryHelper, ServiceRegistry registry) {
        this.setRepositoryHelper(repositoryHelper);
        this.setServices(registry);
    }

    public AbstractJavaWebScript() {
        // default constructor for spring
        super();
    }


    abstract protected Map< String, Object > executeImplImpl( final WebScriptRequest req,
                                                              final Status status,
                                                              final Cache cache );


    // Don't use this - we don't need to handle transactions any more
    @Deprecated
    protected Map< String, Object > executeImplImpl( final WebScriptRequest req,
                                                     final Status status, final Cache cache,
                                                     boolean withoutTransactions ) {

        final Map< String, Object > model = new HashMap<>();

        if(checkMmsVersions)
        {
            if(compareMmsVersions(req, getResponse(), status))
            {
                model.put("res", createResponseJson());
                return model;
            }
        }

        new EmsTransaction( getServices(), getResponse(), getResponseStatus(), withoutTransactions ) {
            @Override
            public void run() throws Exception {
                Map< String, Object > m = executeImplImpl( req, status, cache );
                if ( m != null ) {
                    model.putAll( m );
                }
            }
        };
        if ( !model.containsKey( "res" ) && response != null && response.toString().length() > 0 ) {
            model.put( "res", response.toString() );

        }
        // need to check if the transaction resulted in rollback, if so change the status code
        // TODO: figure out how to get the response message in (response is always empty)
        if (getResponseStatus().getCode() != HttpServletResponse.SC_ACCEPTED) {
            status.setCode( getResponseStatus().getCode() );
        }

        return model;
    }


    /**
     * Parse the request and do validation checks on request
     * TODO: Investigate whether or not to deprecate and/or remove
     * @param req        Request to be parsed
     * @param status    The status to be returned for the request
     * @return            true if request valid and parsed, false otherwise
     */
    abstract protected boolean validateRequest(WebScriptRequest req, Status status);

    protected EmsScriptNode getSiteNode(String siteName, WorkspaceNode workspace, String commitId) {
        return getSiteNode(siteName, workspace != null ? workspace.getSysmlId() : "master", commitId, false);
    }

    protected EmsScriptNode getSiteNode(String siteName) {
        return getSiteNodeImpl(siteName, null, null, false, true);
    }

    /**
     * Get site by name, workspace, and time
     *
     * @param siteName
     *            short name of site
     * @param refId
     *            the workspace of the version of the site to return
     * @param commitId
     *            the point in time for the version of the site to return
     * @return
     */
    protected EmsScriptNode getSiteNode(String siteName, String refId,
                                        String commitId ) {
        return getSiteNode(siteName, refId, commitId, true);
    }

    protected EmsScriptNode getSiteNode(String siteName, String refId,
                                        String commitId, boolean errorOnNull) {
        return getSiteNodeImpl(siteName, refId, commitId, false, errorOnNull);
    }

    /**
     * Helper method for getSideNode* methods
     *
     * @param siteName
     * @param refId
     * @param commitId
     * @param forWorkspace
     * @return
     */
    private EmsScriptNode getSiteNodeImpl(String siteName, String refId,
                                            String commitId, boolean forWorkspace, boolean errorOnNull) {

        EmsScriptNode siteNode = null;

        if (siteName == null) {
            //log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "No sitename provided" );
        	// FIXME: do nothing for now as updates on elements are running afoul here
        } else {
            siteNode = SitePermission.getSiteNode(siteName);
        }
        return siteNode;
    }

    /**
     * Get site by name, workspace, and time.  This also checks that the returned node is
     * in the specified workspace, not just whether its in the workspace or any of its parents.
     *
     * @param siteName
     *            short name of site
     * @param workspace
     *            the workspace of the version of the site to return
     * @param commitId
     *            the point in time for the version of the site to return
     * @return
     */
    protected EmsScriptNode getSiteNodeForWorkspace(String siteName, String refId,
                                                    String commitId) {
        return getSiteNode( siteName, refId, commitId, true );
    }

    protected EmsScriptNode getSiteNodeForWorkspace(String siteName, String refId,
                                                    String commitId, boolean errorOnNull) {

        return getSiteNodeImpl(siteName, refId, commitId, true, errorOnNull);
    }

    protected EmsScriptNode getSiteNodeFromRequest(WebScriptRequest req, boolean errorOnNull) {
        String siteName = null;
        // get timestamp if specified
        String commitId = req.getParameter("commitId");

        String refId = getRefId( req );

        String[] siteKeys = {"id", "siteId", "siteName"};

        for (String siteKey: siteKeys) {
            siteName = req.getServiceMatch().getTemplateVars().get( siteKey );
            if (siteName != null) break;
        }

        return getSiteNode( siteName, refId, commitId, errorOnNull );
    }

    /**
     * Find node of specified name (returns first found) - so assume uniquely named ids - this checks sysml:id rather than cm:name
     * This does caching of found elements so they don't need to be looked up with a different API each time.
     * This also checks that the returned node is in the specified workspace, not just whether its in the workspace
     * or any of its parents.
     *
     * @param id    Node id to search for
     * @param workspace
     * @param dateTime
     * @return        ScriptNode with name if found, null otherwise
     */
    protected EmsScriptNode findScriptNodeByIdForWorkspace(String id,
                                                               WorkspaceNode workspace,
                                                               Date dateTime, boolean findDeleted) {
        return NodeUtil.findScriptNodeByIdForWorkspace( id, workspace, dateTime, findDeleted,
                                                        services, response );
    }

    /**
     * Find node of specified name (returns first found) - so assume uniquely named ids - this checks sysml:id rather than cm:name
     * This does caching of found elements so they don't need to be looked up with a different API each time.
     *
     * TODO extend so search context can be specified
     * @param id    Node id to search for
     * @param workspace
     * @param dateTime
     * @return        ScriptNode with name if found, null otherwise
     */
    protected EmsScriptNode findScriptNodeById(String id,
                                               WorkspaceNode workspace,
                                               Date dateTime, boolean findDeleted) {
        return findScriptNodeById( id, workspace, dateTime, findDeleted, null );
    }

    /**
     * Find node of specified name (returns first found) - so assume uniquely named ids - this checks sysml:id rather than cm:name
     * This does caching of found elements so they don't need to be looked up with a different API each time.
     *
     * TODO extend so search context can be specified
     * @param id    Node id to search for
     * @param workspace
     * @param dateTime
     * @return      ScriptNode with name if found, null otherwise
     */
    protected EmsScriptNode findScriptNodeById(String id,
                                               WorkspaceNode workspace,
                                               Date dateTime, boolean findDeleted,
                                               String siteName) {
        return NodeUtil.findScriptNodeById( id, workspace, dateTime, findDeleted,
                                            services, response, siteName );
    }

    /**
     * Find nodes of specified sysml:name
     *
     */
    protected ArrayList<EmsScriptNode> findScriptNodesBySysmlName(String name,
                                                       WorkspaceNode workspace,
                                                       Date dateTime, boolean findDeleted) {
        return NodeUtil.findScriptNodesBySysmlName( name, false, workspace, dateTime, services, findDeleted, false );
    }


    // Updated log methods with log4j methods (still works with old log calls)
    // String concatenation replaced with C formatting; only for calls with parameters
    protected void log (Level level, int code, String msg, Object...params) {
        if (level.toInt() >= logger.getLevel().toInt()) {
            String formattedMsg = formatMessage(msg,params);
            //String formattedMsg = formatter.format (msg,params).toString();
            log (level,code,formattedMsg);
        }
    }

    // If no need for string formatting (calls with no string concatenation)
    protected void log(Level level, int code, String msg) {
        String levelMessage = addLevelInfoToMsg (level,msg);
        updateResponse(code, levelMessage);
        if (level.toInt() >= logger.getLevel().toInt()) {
            // print to response stream if >= existing log level
            log (level, levelMessage);
        }
    }

    // only logging loglevel and a message (no code)
    protected void log(Level level, String msg, Object...params) {
        if (level.toInt() >= logger.getLevel().toInt()) {
            String formattedMsg = formatMessage(msg,params); //formatter.format (msg,params).toString();
            String levelMessage = addLevelInfoToMsg (level,formattedMsg);
            //TODO: unsure if need to call responseStatus.setMessage(...) since there is no code
            response.append(levelMessage);
            log (level, levelMessage);
        }
    }

    // only logging code and a message (no loglevel, and thus, no check for log level status)
    protected void log(int code, String msg, Object...params) {
        String formattedMsg = formatMessage(msg,params); //formatter.format (msg,params).toString();
        updateResponse (code,formattedMsg);
    }

    protected void log(String msg, Object...params) {
        String formattedMsg = formatMessage(msg,params); //formatter.format (msg,params).toString();
        log (formattedMsg);
    }

    protected void updateResponse ( int code, String msg) {
        response.append(msg);
        responseStatus.setCode(code);
        responseStatus.setMessage(msg);
    }

    protected void log(String msg) {
        response.append(msg + "\n");
        //TODO: add to responseStatus too (below)?
        //responseStatus.setMessage(msg);
    }

    protected static void log(Level level, String msg) {
        switch(level.toInt()) {
            case Level.FATAL_INT:
                logger.fatal(msg);
                break;
            case Level.ERROR_INT:
                logger.error( msg );
                break;
            case Level.WARN_INT:
                logger.warn(msg);
                break;
            case Level.INFO_INT:
                logger.info( msg );
                break;
            case Level.DEBUG_INT:
                if (Debug.isOn()){ logger.debug( msg );}
                break;
            default:
                // TODO: investigate if this the default thing to do
                if (Debug.isOn()){ logger.debug( msg ); }
                break;
        }
    }

    protected String addLevelInfoToMsg (Level level, String msg){
        if (level.toInt() != Level.WARN_INT){
            return String.format("[%s]: %s\n",level.toString(),msg);
        }
        else{
            return String.format("[WARNING]: %s\n",msg);
        }

    }

    // formatMessage function is used to catch certain objects that must be dealt with individually
    // formatter.format() is avoided because it applies toString() directly to objects which provide unreadable outputs
    protected String formatMessage (String initMsg,Object...params){
        String formattedMsg = initMsg;
        Pattern p = Pattern.compile("(%s)");
        Matcher m = p.matcher(formattedMsg);

        for (Object obj: params){
            if (obj != null && obj.getClass().isArray()){
                String arrString = "";
                if (obj instanceof int []) { arrString = Arrays.toString((int [])obj);}
                else if (obj instanceof double []) { arrString = Arrays.toString((double [])obj);}
                else if (obj instanceof float []) { arrString = Arrays.toString((float [])obj);}
                else if (obj instanceof boolean []) { arrString = Arrays.toString((boolean [])obj);}
                else if (obj instanceof char []) { arrString = Arrays.toString((char [])obj);}
                else {arrString = Arrays.toString((Object[])obj);}
                formattedMsg = m.replaceFirst(arrString);
            }
            else { // captures Timer, EmsScriptNode, Date, primitive types, NodeRef, JSONObject type objects; applies toString() on all
                formattedMsg = m.replaceFirst(obj == null ? "null" : obj.toString());
            }
            m = p.matcher(formattedMsg);
//            if (obj.getClass().isArray()){
//                Arrays.toString(obj);
//                String formattedString = m.replaceFirst(o)
//            }

        }
        return formattedMsg;
    }

    /**
     * Checks whether user has permissions to the node and logs results and status as appropriate
     * @param node         EmsScriptNode to check permissions on
     * @param permissions  Permissions to check
     * @return             true if user has specified permissions to node, false otherwise
     */
    protected boolean checkPermissions(EmsScriptNode node, String permissions) {
        return node != null && node.checkPermissions( permissions, response, responseStatus );
    }


    protected static final String REF_ID = "refId";
    protected static final String PROJECT_ID = "projectId";
    protected static final String ORG_ID = "orgId";
    protected static final String ARTIFACT_ID = "artifactId";
    protected static final String SITE_NAME = "siteName";
    protected static final String SITE_NAME2 = "siteId";
    protected static final String WORKSPACE1 = "workspace1";
    protected static final String WORKSPACE2 = "workspace2";
    protected static final String TIMESTAMP1 = "timestamp1";
    protected static final String TIMESTAMP2 = "timestamp2";

    public static final String NO_WORKSPACE_ID = "master"; // default is master if unspecified
    public static final String NO_PROJECT_ID = "no_project";
    public static final String NO_SITE_ID = "no_site";

    protected static final String COMMITID = "commitId";

    public String getSiteName( WebScriptRequest req ) {
        return getSiteName( req, false );
    }

    public String getSiteName( WebScriptRequest req, boolean createIfNonexistent ) {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !EmsScriptNode.ADMIN_USER_NAME.equals( runAsUser );
        if ( changeUser ) {
            AuthenticationUtil.setRunAsUser( EmsScriptNode.ADMIN_USER_NAME );
        }

        String siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME);
        if ( siteName == null ) {
            siteName = req.getServiceMatch().getTemplateVars().get(SITE_NAME2);
        }
        if ( siteName == null || siteName.length() <= 0 ) {
            siteName = NO_SITE_ID;
        }

        if ( changeUser ) {
            AuthenticationUtil.setRunAsUser( runAsUser );
        }

        if ( createIfNonexistent ) {
            WorkspaceNode workspace = getWorkspace( req );
            createSite( siteName, workspace.getSysmlId() );
        }
        return siteName;
    }

    public static EmsScriptNode getSitesFolder( WorkspaceNode workspace ) {
        EmsScriptNode sitesFolder = null;
        // check and see if the Sites folder already exists
        NodeRef sitesNodeRef = NodeUtil.findNodeRefByType( "Sites", SearchType.CM_NAME, false,
                                                           workspace, null, true, NodeUtil.getServices(), false );
        if ( sitesNodeRef != null ) {
            sitesFolder = new EmsScriptNode( sitesNodeRef, NodeUtil.getServices() );
        }

        // This case should never occur b/c the master workspace will always
        // have a Sites folder:
        else {
            Debug.error( "Can't find Sites folder in the workspace " + workspace );
        }
        return sitesFolder;
//        EmsScriptNode sf = NodeUtil.getCompanyHome( getServices() ).childByNamePath( "Sites" );
//        return sf;
    }

    public static EmsScriptNode getSitesFolder( String refId ) {
        EmsScriptNode sitesFolder = null;
        WorkspaceNode workspace = WorkspaceNode.getWorkspaceFromId(refId, NodeUtil.getServices(), null, null,
                        NodeUtil.getUserName());
        NodeRef sitesNodeRef = NodeUtil.findNodeRefByType("Sites", SearchType.CM_NAME, false,
                        workspace, null, true, NodeUtil.getServices(), false );
        if ( sitesNodeRef != null ) {
            sitesFolder = new EmsScriptNode( sitesNodeRef, NodeUtil.getServices() );
        } else {
            Debug.error( "Can't find Sites folder in the workspace " + workspace );
        }
        return sitesFolder;
    }

    public EmsScriptNode createSite( String siteName, String refId ) {

        EmsScriptNode siteNode = getSiteNode( siteName, refId, null, false );
        if (refId != null) return siteNode; // sites can only be made in master
        boolean invalidSiteNode = siteNode == null || !siteNode.exists();

        // Create a alfresco Site if creating the site on the master and if the site does not exists:
        if ( invalidSiteNode ) {
            NodeUtil.transactionCheck( logger, null );
            SiteInfo foo = services.getSiteService().createSite( siteName, siteName, siteName, siteName, SiteVisibility.PUBLIC );
            siteNode = new EmsScriptNode( foo.getNodeRef(), services );
            siteNode.createOrUpdateAspect( "cm:taggable" );
            siteNode.createOrUpdateAspect(Acm.ACM_SITE);
            // this should always be in master so no need to check workspace
//            if (workspace == null) { // && !siteNode.getName().equals(NO_SITE_ID)) {
                // default creation adds GROUP_EVERYONE as SiteConsumer, so remove
                siteNode.removePermission( "SiteConsumer", "GROUP_EVERYONE" );
                if ( siteNode.getName().equals(NO_SITE_ID)) {
                    siteNode.setPermission( "SiteCollaborator", "GROUP_EVERYONE" );
                }
//            }
        }

        // If this site is supposed to go into a non-master workspace, then create the site folders
        // there if needed:
        if ((invalidSiteNode || (!invalidSiteNode && !refId.equals(siteNode.getWorkspace().getSysmlId())))) {

            EmsScriptNode sitesFolder = getSitesFolder(refId);

            // Now, create the site folder:
            if (sitesFolder == null ) {
                Debug.error("Could not create site " + siteName + "!");
            } else {
                siteNode = sitesFolder.createFolder( siteName, null, !invalidSiteNode ? siteNode.getNodeRef() : null );
                if ( siteNode != null ) siteNode.getOrSetCachedVersion();
            }
        }

        return siteNode;
    }

    public ServiceRegistry getServices() {
        if ( services == null ) {
            services = NodeUtil.getServices();
        }
        return services;
    }

    protected boolean checkRequestContent(WebScriptRequest req) {
        if (req.getContent() == null) {
            log(Level.ERROR,  HttpServletResponse.SC_BAD_REQUEST, "No content provided.\n");
            return false;
        }
        return true;
    }

    /**
     * Returns true if the user has permission to do workspace operations, which is determined
     * by the LDAP group or if the user is admin.
     *
     */
    protected boolean userHasWorkspaceLdapPermissions() {
        // TODO: Fix this to not check the workspace node, since there isn't any.
        /*
        if (!NodeUtil.userHasWorkspaceLdapPermissions()) {
            log(Level.ERROR, HttpServletResponse.SC_FORBIDDEN, "User %s does not have LDAP permissions to perform workspace operations.  LDAP group with permissions: %s",
                    NodeUtil.getUserName(), NodeUtil.getWorkspaceLdapGroup());
            return false;
        }
        */
        return true;
    }


    protected boolean checkRequestVariable(Object value, String type) {
        if (value == null) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "%s not found.\n",type);
            return false;
        }
        return true;
    }

    protected Map< String, EmsScriptNode >
            searchForElements( String type, String pattern,
                                       boolean ignoreWorkspace,
                                       WorkspaceNode workspace, Date dateTime ) {

        // always use DB for search on latest
        //if (NodeUtil.doGraphDb) {
            if (workspace == null && dateTime == null) {
                return searchForElementsPostgres( type, pattern, ignoreWorkspace, workspace, dateTime );
            } else {
                return searchForElementsOriginal( type, pattern, ignoreWorkspace, workspace, dateTime );
            }
//        } else {
//            return searchForElementsOriginal( type, pattern, ignoreWorkspace, workspace, dateTime );
//        }
    }

    /**
     * Perform Lucene search for the specified pattern and ACM type
     * TODO: Scope Lucene search by adding either parent or path context
     * @param type        escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
     * @param pattern   Pattern to look for
     */
    protected Map<String, EmsScriptNode> searchForElementsOriginal(String type,
                                                           String pattern,
                                                           boolean ignoreWorkspace,
                                                           WorkspaceNode workspace,
                                                           Date dateTime) {
        return this.searchForElements( type, pattern, ignoreWorkspace,
                                       workspace, dateTime, null );
    }

    /**
     * Perform Lucene search for the specified pattern and ACM type
     * As opposed to searchForElementsOriginal, this returns a Map of noderef ids to
     * script nodes - since postgres db needs this to look up properly
     *
     * @param type
     * @param pattern
     * @param ignoreWorkspace
     * @param workspace
     * @param dateTime
     * @return
     */
    protected Map<String, EmsScriptNode> searchForElementsPostgres(String type,
                                                                  String pattern,
                                                                  boolean ignoreWorkspace,
                                                                  WorkspaceNode workspace,
                                                                  Date dateTime) {
        Map<String, EmsScriptNode> resultsMap = new HashMap<>();
        ResultSet results = null;
        String queryPattern = type + pattern + "\"";
        results = NodeUtil.luceneSearch( queryPattern, services );
        if (results != null) {
            ArrayList< NodeRef > resultList =
                    NodeUtil.resultSetToNodeRefList( results );
            results.close();
            for (NodeRef nr: resultList) {
                EmsScriptNode node = new EmsScriptNode(nr, services, response);
                resultsMap.put( node.getNodeRef().toString(), node );
            }
        }
        return resultsMap;
    }

    /**
     * Perform Lucene search for the specified pattern and ACM type for the specified
     * siteName.
     *
     * TODO: Scope Lucene search by adding either parent or path context
     * @param type      escaped ACM type for lucene search: e.g. "@sysml\\:documentation:\""
     * @param pattern   Pattern to look for
     */
    protected Map<String, EmsScriptNode> searchForElements(String type,
                                                                  String pattern,
                                                                  boolean ignoreWorkspace,
                                                                  WorkspaceNode workspace,
                                                                  Date dateTime,
                                                                  String siteName) {

        return NodeUtil.searchForElements( type, pattern, ignoreWorkspace,
                                                          workspace,
                                                          dateTime, services,
                                                          response,
                                                          responseStatus,
                                                          siteName);

    }

    /**
     * Helper utility to check the value of a request parameter
     *
     * @param req
     *            WebScriptRequest with parameter to be checked
     * @param name
     *            String of the request parameter name to check
     * @param value
     *            String of the value the parameter is being checked for
     * @return True if parameter is equal to value, False otherwise
     */
    public static boolean checkArgEquals(WebScriptRequest req, String name,
            String value) {
        if (req.getParameter(name) == null) {
            return false;
        }
        return req.getParameter(name).equals(value);
    }

    /**
     * Helper utility to get the value of a Boolean request parameter
     *
     * @param req
     *            WebScriptRequest with parameter to be checked
     * @param name
     *            String of the request parameter name to check
     * @param defaultValue
     *            default value if there is no parameter with the given name
     * @return true if the parameter is assigned no value, if it is assigned
     *         "true" (ignoring case), or if it's default is true and it is not
     *         assigned "false" (ignoring case).
     */
    public static boolean getBooleanArg(WebScriptRequest req, String name,
                                        boolean defaultValue) {
        if ( !Utils.toSet( req.getParameterNames() ).contains( name ) ) {
            return defaultValue;
        }
        String paramVal = req.getParameter(name);
        if ( Utils.isNullOrEmpty( paramVal ) ) return true;
        Boolean b = Utils.isTrue( paramVal, false );
        if ( b != null ) return b;
        return defaultValue;
    }


    public StringBuffer getResponse() {
        return response;
    }

    public Status getResponseStatus() {
        return responseStatus;
    }

    /**
     * Should create the new instances with the response in constructor, so
     * this can be removed every where
     * @param instance
     */
    public void appendResponseStatusInfo(AbstractJavaWebScript instance) {
        response.append(instance.getResponse());
        responseStatus.setCode(instance.getResponseStatus().getCode());
    }

    protected void printFooter(String user, Logger logger, Timer timer) {
        logger.info(String.format("%s %s", user, timer));
    }

    protected void printHeader( String user, Logger logger, WebScriptRequest req ) {
        logger.info(String.format("%s %s", user, req.getURL()));
        try {
	        if (req.parseContent() != null) {
	            logger.info(String.format("%s", req.parseContent()));
	        }
        } catch (Exception e) {
        	// do nothing, just means no content when content-type was specified
        }
    }

    protected static String getIdFromRequest( WebScriptRequest req ) {
        String[] ids = new String[] { "id", "modelid", "modelId", "productid", "productId",
                                      "viewid", "viewId", "refid", "refId",
                                      "elementid", "elementId" };
        String id = null;
        for ( String idv : ids ) {
            id = req.getServiceMatch().getTemplateVars().get(idv);
            if ( id != null ) break;
        }
        logger.debug(String.format("Got id = %s", id));
        if ( id == null ) return null;
        boolean gotElementSuffix  = ( id.toLowerCase().trim().endsWith("/elements") );
        if ( gotElementSuffix ) {
            id = id.substring( 0, id.lastIndexOf( "/elements" ) );
        } else {
            boolean gotViewSuffix  = ( id.toLowerCase().trim().endsWith("/views") );
            if ( gotViewSuffix ) {
                id = id.substring( 0, id.lastIndexOf( "/views" ) );
            }
        }
        if (logger.isDebugEnabled()) logger.debug("id = " + id);
        return id;
    }

    public static String getOrgId( WebScriptRequest req ) {
        String orgId = req.getServiceMatch().getTemplateVars().get(ORG_ID);
        if (orgId != null) {
            return orgId;
        }
        return null;
    }

    public static String getProjectId( WebScriptRequest req ) {
        String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
        if (projectId != null) {
            return projectId;
        }
        return null;
    }

    public static String getProjectId( WebScriptRequest req, String siteName ) {
        String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
        if ( projectId == null || projectId.length() <= 0 ) {
            if (siteName == null) {
                siteName = NO_SITE_ID;
            }
            projectId = siteName + "_" + NO_PROJECT_ID;
        }
        return projectId;
    }

    public static String getRefId( WebScriptRequest req ) {
        String refId = req.getServiceMatch().getTemplateVars().get(REF_ID);
        if ( refId == null || refId.length() <= 0 ) {
            refId = NO_WORKSPACE_ID;
        }
        return refId;
    }

    public static String getCommitId( WebScriptRequest req ) {
        return req.getServiceMatch().getTemplateVars().get(COMMITID);
    }

    private static String getWorkspaceNum( WebScriptRequest req, boolean isWs1 ) {
        String key = isWs1 ? WORKSPACE1 : WORKSPACE2;
        String refId = req.getServiceMatch().getTemplateVars().get(key);
        if ( refId == null || refId.length() <= 0 ) {
            refId = NO_WORKSPACE_ID;
        }
        return refId;
    }

    public static String getWorkspace1( WebScriptRequest req) {
        return getWorkspaceNum(req, true);
    }

    public static String getWorkspace2( WebScriptRequest req) {
        return getWorkspaceNum(req, false);
    }

    public WorkspaceNode getWorkspace( WebScriptRequest req ) {
        return getWorkspace( req, //false,
                        null );
    }

    public WorkspaceNode getWorkspace( WebScriptRequest req,
                    //                                       boolean createIfNotFound,
                    String userName ) {
        return getWorkspace( req, services, response, responseStatus, //createIfNotFound,
                        userName );
    }

    public static WorkspaceNode getWorkspace( WebScriptRequest req,
                    ServiceRegistry services,
                    StringBuffer response,
                    Status responseStatus,
                    //boolean createIfNotFound,
                    String userName ) {
        String nameOrId = getRefId( req );
        return WorkspaceNode.getWorkspaceFromId( nameOrId, services, response, responseStatus,
                        //createIfNotFound,
                        userName );
    }

    protected static boolean urlEndsWith( String url, String suffix ) {
        if ( url == null ) return false;
        url = url.toLowerCase().trim();
        suffix = suffix.toLowerCase().trim();
        if ( suffix.startsWith( "/" ) ) suffix = suffix.substring( 1 );
        int pos = url.lastIndexOf( '/' );
        if (url.substring( pos+1 ).startsWith( suffix ) ) return true;
        return false;
    }


    protected static boolean isDisplayedElementRequest( WebScriptRequest req ) {
        if ( req == null ) return false;
        String url = req.getURL();
        boolean gotSuffix = urlEndsWith( url, Sjm.ELEMENTS );
        return gotSuffix;
    }

    protected static boolean isContainedViewRequest( WebScriptRequest req ) {
        if ( req == null ) return false;
        String url = req.getURL();
        boolean gotSuffix = urlEndsWith( url, "views" );
        return gotSuffix;
    }


    /**
     * This needs to be called with the incoming JSON request to populate the local source
     * variable that is used in the sendDeltas call.
     * @param postJson
     * @throws JSONException
     */
    protected void populateSourceApplicationFromJson(JSONObject postJson) throws JSONException {
        requestSourceApplication = postJson.optString( "source" );
    }

    /**
     * Send progress messages to the log, JMS, and email.
     *
     * @param msg  The message
     * @param projectSysmlId  The project sysml id
     * @param refName  The workspace name
     * @param sendEmail Set to true to send a email also
     */
    public void sendProgress( String msg, String projectSysmlId, String refName,
                              boolean sendEmail) {

        String projectId = Utils.isNullOrEmpty(projectSysmlId) ? "unknown_project" : projectSysmlId;
        String refId = Utils.isNullOrEmpty(refName) ? "unknown_workspace" : refName;
        String subject = "Progress for project: "+projectId+" ref: "+ refId;

        // Log the progress:
        log(Level.INFO,"%s msg: %s\n",subject,msg);
        //logger.info(subject+" msg: "+msg+"\n");

        // Send the progress over JMS:
        CommitUtil.sendProgress(msg, refId, projectId);

        // Email the progress (this takes a long time, so only do it for critical events):
        if (sendEmail) {
            String hostname = NodeUtil.getHostname();
            if (!Utils.isNullOrEmpty( hostname )) {
                String sender = hostname + "@jpl.nasa.gov";
                String username = NodeUtil.getUserName();
                if (!Utils.isNullOrEmpty( username )) {
                    EmsScriptNode user = new EmsScriptNode(services.getPersonService().getPerson(username),
                                                           services);
                    if (user != null) {
                        String recipient = (String) user.getProperty("cm:email");
                        if (!Utils.isNullOrEmpty( recipient )) {
                            ActionUtil.sendEmailTo( sender, recipient, msg, subject, services );
                        }
                    }
                }
            }
        }

    }

    /**
     * Creates a json like object in a string and puts the response in the message key
     *
     * @return The resulting string, ie "{'message':response}" or "{}"
     */
    public String createResponseJson() {
        String resToString = response.toString();
        String resStr = !Utils.isNullOrEmpty( resToString ) ? resToString.replaceAll( "\n", "" ) : "";
        return !Utils.isNullOrEmpty( resStr ) ? String.format("{\"message\":\"%s\"}", resStr) : "{}";
    }


    /**
     * compareMmsVersions
     * <br>
     * <h3>Note: Returns true if this compare fails for either incorrect versions or if there is an error with the request.<br/>
     * Returns false if the check is successful and the versions match.</h3>
     * <pre>
     * Takes a request created when a service is called and will retrieve the mmsVersion that is sent with it.
     *  <b>The flag checkMmsVersions needs to be set to true for this service to work.</b>
     *  <br/><b>1. </b>Check if there the request comes with the parameter mmsVersion=2.#. If the global flag
     *  is set to check for mmsVersion it will then return either none if either invalid input or if none has been
     *  specified, or the value of the version the service is being called with.
     *  <br/><b>2. </b>If the value that is received after checking for mmsVersion in the request, is 'none' then
     *  it will call parseContent of the request to create a JSONObject. If that fails, an exception is thrown
     *  and the boolean value 'true' is returned to the calling method to signify failure of the check. Else it
     *  will try to grab the mmsVersion from where ever it may lie within the JSONObject.
     *  <br/><b>3. </b>
     * </pre>
     * @param req WebScriptRequest
     * @param response StringBuffer response
     * @param status Status of the request
     * @author EDK
     * @return boolean false if versions match, true if they do not match or if is an incorrect request.
     */
    public boolean compareMmsVersions(WebScriptRequest req,
            StringBuffer response, Status status) {
        // Calls getBooleanArg to check if they have request for mms version
        // TODO: Possibly remove this and implement as an aspect?
        boolean incorrectVersion = true;
        JSONObject jsonRequest = null;
        char logCase = '0';
        JSONObject jsonVersion = null;
        String mmsVersion = null;

        // Checks if the argument is mmsVersion and returns the value specified
        // by the request
        // if there is no request it will return 'none'
        String paramVal = getStringArg(req, "mmsVersion", "none");
        String paramArg = paramVal;
        // Checks data member requestJSON to see if it is not null and if
        // paramVal is none

//     // Check if input is K or JSON
        String contentType = req.getContentType() == null ? ""
                : req.getContentType().toLowerCase();

        boolean jsonNotK = !contentType.contains("application/k");


        if (!jsonNotK && paramVal.equals("none")) {
                jsonRequest = getRequestJSON(req);

            if (jsonRequest != null) {
                paramVal = jsonRequest.optString("mmsVersion");
            }
        }

        if (paramVal != null && !paramVal.equals("none") && paramVal.length() > 0) {
            // Calls NodeUtil's getMMSversion
            jsonVersion = getMMSversion();
            mmsVersion = jsonVersion.get("mmsVersion").toString();

            log(Level.INFO, HttpServletResponse.SC_OK, "Comparing Versions....");
            if (mmsVersion.equals(paramVal)) {
                // Compared versions matches
                logCase = '1';
                incorrectVersion = false;
            } else {
                // Versions do not match
                logCase = '2';
            }
        } else if (Utils.isNullOrEmpty(paramVal) || paramVal.equals("none")) {
            // Missing MMS Version parameter
            logCase = '3';
        } else {
            // Wrong MMS Version or Invalid input
            logCase = '4';
        }
        switch (logCase) {
            case '1' :
                log(Level.INFO, HttpServletResponse.SC_OK, "Correct Versions");
                break;
            case '2' :
                log(Level.WARN, HttpServletResponse.SC_CONFLICT,
                        "Versions do not match! Expected Version " + mmsVersion
                                + ". Instead received " + paramVal);
                break;
            case '3' :
                log(Level.ERROR, HttpServletResponse.SC_CONFLICT,
                        "Missing MMS Version or invalid parameter. Received parameter:" + paramArg + " and argument:" + mmsVersion + ". Request was: " + jsonRequest);
                break;
            // TODO: This should be removed but for the moment I am leaving this
            // in as a contingency if anything else may break this.
            case '4' :
                log(Level.ERROR, HttpServletResponse.SC_CONFLICT,
                        "Wrong MMS Version or invalid input. Expected mmsVersion="
                                + mmsVersion + ". Instead received "
                                + paramVal);
                break;
        }
        // Returns true if it is either the wrong version or if it failed to
        // compare it
        // Returns false if it was successful in retrieving the mmsVersions from
        // both the MMS and the request and
        return incorrectVersion;
    }

    /**
     * getMMSversion<br>
     * Returns a JSONObject representing the mms version being used. It's format
     * will be
     *
     * <pre>
     *  {
     *     "mmsVersion":"2.2"
     * }
     * </pre>
     *
     * @return JSONObject mmsVersion
     */
    public static JSONObject getMMSversion() {
        JSONObject version = new JSONObject();
        version.put("mmsVersion", NodeUtil.getMMSversion());
        return version;
    }
    /**
     * getMMSversion <br/>
     * getMMSversion wraps
     *
     * @param req
     * @return
     */
    public static JSONObject getMMSversion(WebScriptRequest req) {
        // Calls getBooleanArg to check if they have request for mms version
        // TODO: Possibly remove this and implement as an aspect?
        JSONObject jsonVersion = null;
        boolean paramVal = getBooleanArg(req, "mmsVersion", false);
        if (paramVal) {
            jsonVersion = getMMSversion();
        }

        return jsonVersion;
    }

    /**
     * Helper utility to get the String value of a request parameter, calls on
     * getParameterNames from the WebScriptRequest object to compare the
     * parameter name passed in that is desired from the header.
     *
     * @param req
     *            WebScriptRequest with parameter to be checked
     * @param name
     *            String of the request parameter name to check
     * @param defaultValue
     *            default value if there is no parameter with the given name
     * @author dank
     * @return 'empty' if the parameter is assigned no value, if it is assigned
     *         "parameter value" (ignoring case), or if it's default is default
     *         value and it is not assigned "empty" (ignoring case).
     */
    public static String getStringArg(WebScriptRequest req, String name,
            String defaultValue) {
        if (!Utils.toSet(req.getParameterNames()).contains(name)) {
            return defaultValue;
        }
        return req.getParameter(name);
    }

    /**
     * setRequestJSON <br>
     * This will set the AbstractJavaWebScript data member requestJSON. It will
     * make the parsedContent JSONObject remain within the scope of the
     * AbstractJavaWebScript. {@link}
     *
     * @param req
     *            WebScriptRequest
     */
    public void setRequestJSON(WebScriptRequest req) {

        try {
//            privateRequestJSON = new JSONObject();
            privateRequestJSON = (JSONObject) req.parseContent();
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not retrieve JSON");
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    public JSONObject getRequestJSON() {
        return privateRequestJSON;
    }

    public JSONObject getRequestJSON(WebScriptRequest req) {
        // Returns immediately if requestJSON has already been set before checking MMS Versions
        if(privateRequestJSON == null) return privateRequestJSON;
        // Sets privateRequestJSON
        setRequestJSON(req);
        return privateRequestJSON;
    }
}
