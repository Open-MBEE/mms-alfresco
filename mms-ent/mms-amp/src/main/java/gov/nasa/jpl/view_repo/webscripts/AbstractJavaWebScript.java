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

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
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
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;

/**
 * Base class for all EMS Java backed webscripts. Provides helper functions and
 * key variables necessary for execution. This provides most of the capabilities
 * that were in utils.js
 *
 * @author cinyoung
 */
public abstract class AbstractJavaWebScript extends DeclarativeJavaWebScript {
    private static Logger logger = Logger.getLogger(AbstractJavaWebScript.class);

    static boolean checkMmsVersions = false;
    private JSONObject privateRequestJSON = null;

    // injected members
    protected ServiceRegistry services;        // get any of the Alfresco services
    protected Repository repository;        // used for lucene search

    // response to HTTP request, made as class variable so all methods can update
    protected StringBuffer response = new StringBuffer();
    protected Status responseStatus = new Status();

    // keeps track of who made the call to the service
    String requestSourceApplication = null;

    boolean prettyPrint = false;

    public void setRepositoryHelper(Repository repositoryHelper) {
        if (repositoryHelper == null)
            return;
        this.repository = repositoryHelper;
    }

    public void setServices(ServiceRegistry registry) {
        if (registry == null)
            return;
        this.services = registry;
    }

    public AbstractJavaWebScript(Repository repository, ServiceRegistry services, StringBuffer response) {
        this.setRepositoryHelper(repository);
        this.setServices(services);
        this.response = response;
    }

    public AbstractJavaWebScript(Repository repositoryHelper, ServiceRegistry registry) {
        this.setRepositoryHelper(repositoryHelper);
        this.setServices(registry);
    }

    public AbstractJavaWebScript() {
        super();
    }


    abstract protected Map<String, Object> executeImplImpl(final WebScriptRequest req, final Status status,
        final Cache cache);


    /**
     * Parse the request and do validation checks on request
     * TODO: Investigate whether or not to deprecate and/or remove
     *
     * @param req    Request to be parsed
     * @param status The status to be returned for the request
     * @return true if request valid and parsed, false otherwise
     */
    abstract protected boolean validateRequest(WebScriptRequest req, Status status);

    /**
     * Helper method for getSideNode* methods
     *
     * @param siteName
     * @return
     */
    protected EmsScriptNode getSiteNode(String siteName) {
        EmsScriptNode siteNode = null;

        if (siteName != null) {
            siteNode = EmsScriptNode.getSiteNode(siteName);
        }

        return siteNode;
    }

    // Updated log methods with log4j methods (still works with old log calls)
    // String concatenation replaced with C formatting; only for calls with parameters
    protected void log(Level level, int code, String msg, Object... params) {
        if (level.toInt() >= logger.getLevel().toInt()) {
            String formattedMsg = formatMessage(msg, params);
            //String formattedMsg = formatter.format (msg,params).toString();
            log(level, code, formattedMsg);
        }
    }

    // If no need for string formatting (calls with no string concatenation)
    protected void log(Level level, int code, String msg) {
        String levelMessage = addLevelInfoToMsg(level, msg);
        updateResponse(code, levelMessage);
        if (level.toInt() >= logger.getLevel().toInt()) {
            // print to response stream if >= existing log level
            log(level, levelMessage);
        }
    }

    protected void log(Level level, int code, String msg, Exception e) {
        String levelMessage = addLevelInfoToMsg(level, msg);
        updateResponse(code, levelMessage);
        if (level.toInt() >= logger.getLevel().toInt()) {
            // print to response stream if >= existing log level
            log(level, e);
        }
    }

    // If no need for string formatting (calls with no string concatenation)
    protected void log(Level level, int code, Exception e) {
        String levelMessage = addLevelInfoToMsg(level, LogUtil.getStackTrace(e));
        updateResponse(code, levelMessage);
        if (level.toInt() >= logger.getLevel().toInt()) {
            // print to response stream if >= existing log level
            log(level, levelMessage);
        }
    }

    // only logging loglevel and a message (no code)
    protected void log(Level level, String msg, Object... params) {
        if (level.toInt() >= logger.getLevel().toInt()) {
            String formattedMsg = formatMessage(msg, params); //formatter.format (msg,params).toString();
            String levelMessage = addLevelInfoToMsg(level, formattedMsg);
            //TODO: unsure if need to call responseStatus.setMessage(...) since there is no code
            response.append(levelMessage);
            log(level, levelMessage);
        }
    }

    // only logging code and a message (no loglevel, and thus, no check for log level status)
    protected void log(int code, String msg, Object... params) {
        String formattedMsg = formatMessage(msg, params); //formatter.format (msg,params).toString();
        updateResponse(code, formattedMsg);
    }

    protected void log(String msg, Object... params) {
        String formattedMsg = formatMessage(msg, params); //formatter.format (msg,params).toString();
        log(formattedMsg);
    }

    protected void updateResponse(int code, String msg) {
        response.append(msg);
        responseStatus.setCode(code);
        responseStatus.setMessage(msg);
    }

    protected void log(String msg) {
        response.append(msg + "\n");
        //TODO: add to responseStatus too (below)?
        //responseStatus.setMessage(msg);
    }

    protected static void log(Level level, Exception e) {
        log(level, LogUtil.getStackTrace(e));
    }

    protected static void log(Level level, String msg) {
        switch (level.toInt()) {
            case Level.FATAL_INT:
                logger.fatal(msg);
                break;
            case Level.ERROR_INT:
                logger.error(msg);
                break;
            case Level.WARN_INT:
                logger.warn(msg);
                break;
            case Level.INFO_INT:
                logger.info(msg);
                break;
            case Level.DEBUG_INT:
                if (Debug.isOn()) {
                    logger.debug(msg);
                }
                break;
            default:
                // TODO: investigate if this the default thing to do
                if (Debug.isOn()) {
                    logger.debug(msg);
                }
                break;
        }
    }

    protected String addLevelInfoToMsg(Level level, String msg) {
        if (level.toInt() != Level.WARN_INT) {
            return String.format("[%s]: %s%n", level.toString(), msg);
        } else {
            return String.format("[WARNING]: %s%n", msg);
        }

    }

    // formatMessage function is used to catch certain objects that must be dealt with individually
    // formatter.format() is avoided because it applies toString() directly to objects which provide unreadable outputs
    protected String formatMessage(String initMsg, Object... params) {
        String formattedMsg = initMsg;
        Pattern p = Pattern.compile("(%s)");
        Matcher m = p.matcher(formattedMsg);

        for (Object obj : params) {
            if (obj != null && obj.getClass().isArray()) {
                String arrString = "";
                if (obj instanceof int[]) {
                    arrString = Arrays.toString((int[]) obj);
                } else if (obj instanceof double[]) {
                    arrString = Arrays.toString((double[]) obj);
                } else if (obj instanceof float[]) {
                    arrString = Arrays.toString((float[]) obj);
                } else if (obj instanceof boolean[]) {
                    arrString = Arrays.toString((boolean[]) obj);
                } else if (obj instanceof char[]) {
                    arrString = Arrays.toString((char[]) obj);
                } else {
                    arrString = Arrays.toString((Object[]) obj);
                }
                formattedMsg = m.replaceFirst(arrString);
            } else { // captures Timer, EmsScriptNode, Date, primitive types, NodeRef, JSONObject type objects; applies toString() on all
                formattedMsg = m.replaceFirst(obj == null ? "null" : obj.toString());
            }
            m = p.matcher(formattedMsg);

        }
        return formattedMsg;
    }

    /**
     * Checks whether user has permissions to the node and logs results and status as appropriate
     *
     * @param node        EmsScriptNode to check permissions on
     * @param permissions Permissions to check
     * @return true if user has specified permissions to node, false otherwise
     */
    protected boolean checkPermissions(EmsScriptNode node, String permissions) {
        return node != null && node.checkPermissions(permissions, response, responseStatus);
    }

    public ServiceRegistry getServices() {
        if (services == null) {
            services = NodeUtil.getServices();
        }
        return services;
    }

    protected boolean checkRequestContent(WebScriptRequest req) {
        if (req.getContent() == null) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "No content provided.\n");
            return false;
        }
        return true;
    }

    protected boolean checkRequestVariable(Object value, String type) {
        if (value == null) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "%s not found.\n", type);
            return false;
        }
        return true;
    }

    /**
     * Helper utility to get the value of a Boolean request parameter
     *
     * @param req          WebScriptRequest with parameter to be checked
     * @param name         String of the request parameter name to check
     * @param defaultValue default value if there is no parameter with the given name
     * @return true if the parameter is assigned no value, if it is assigned
     * "true" (ignoring case), or if it's default is true and it is not
     * assigned "false" (ignoring case).
     */
    public static boolean getBooleanArg(WebScriptRequest req, String name, boolean defaultValue) {
        if (!Utils.toSet(req.getParameterNames()).contains(name)) {
            return defaultValue;
        }
        String paramVal = req.getParameter(name);
        if (Utils.isNullOrEmpty(paramVal))
            return true;
        Boolean b = Utils.isTrue(paramVal, false);
        if (b != null)
            return b;
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
     *
     * @param instance
     */
    public void appendResponseStatusInfo(AbstractJavaWebScript instance) {
        response.append(instance.getResponse());
        responseStatus.setCode(instance.getResponseStatus().getCode());
    }

    protected void printFooter(String user, Logger logger, Timer timer) {
        logger.info(String.format("%s %s", user, timer));
    }

    protected void printHeader(String user, Logger logger, WebScriptRequest req) {
        printHeader(user, logger, req, false);
    }

    protected void printHeader(String user, Logger logger, WebScriptRequest req, boolean skipReq) {
        logger.info(String.format("%s %s", user, req.getURL()));
        try {
            if (!skipReq && req.parseContent() != null) {
                logger.info(String.format("%s", req.parseContent()));
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }
    }

    public static String getOrgId(WebScriptRequest req) {
        String orgId = req.getServiceMatch().getTemplateVars().get(ORG_ID);
        if (orgId != null) {
            return orgId;
        }
        return null;
    }

    public static String getProjectId(WebScriptRequest req) {
        String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
        if (projectId != null) {
            return projectId;
        }
        return null;
    }

    public static String getProjectId(WebScriptRequest req, String siteName) {
        String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
        if (projectId == null || projectId.length() <= 0) {
            if (siteName == null) {
                siteName = NO_SITE_ID;
            }
            projectId = siteName + "_" + NO_PROJECT_ID;
        }
        return projectId;
    }

    public static String getRefId(WebScriptRequest req) {
        String refId = req.getServiceMatch().getTemplateVars().get(REF_ID);
        if (refId == null || refId.length() <= 0) {
            refId = NO_WORKSPACE_ID;
        }
        return refId;
    }

    public static String getArtifactId(WebScriptRequest req) {
        String artifactId = req.getServiceMatch().getTemplateVars().get(ARTIFACT_ID);
        if (artifactId == null || artifactId.length() <= 0) {
            artifactId = null;
        }
        return artifactId;
    }

    public EmsScriptNode getWorkspace(WebScriptRequest req) {
        String refId = getRefId(req);
        String projectId = getProjectId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        String orgId = emsNodeUtil.getOrganizationFromProject(projectId);
        EmsScriptNode node = EmsScriptNode.getSiteNode(orgId);
        node = node.childByNamePath("/" + projectId + "/refs/" + refId);
        if (node != null) {
            return new EmsScriptNode(node.getNodeRef(), services);
        }

        return null;
    }

    /**
     * This needs to be called with the incoming JSON request to populate the local source
     * variable that is used in the sendDeltas call.
     *
     * @param postJson
     * @throws JSONException
     */
    protected void populateSourceApplicationFromJson(JSONObject postJson) throws JSONException {
        requestSourceApplication = postJson.optString("source");
    }

    /**
     * Creates a json like object in a string and puts the response in the message key
     *
     * @return The resulting string, ie "{'message':response}" or "{}"
     */
    public String createResponseJson() {
        String resToString = response.toString();
        String resStr = !Utils.isNullOrEmpty(resToString) ? resToString.replaceAll("\n", "") : "";
        return !Utils.isNullOrEmpty(resStr) ? String.format("{\"message\":\"%s\"}", resStr) : "{}";
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
     *
     * @param req      WebScriptRequest
     * @param response StringBuffer response
     * @param status   Status of the request
     * @return boolean false if versions match, true if they do not match or if is an incorrect request.
     * @author EDK
     */
    public boolean compareMmsVersions(WebScriptRequest req, StringBuffer response, Status status) {
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
        String contentType = req.getContentType() == null ? "" : req.getContentType().toLowerCase();

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
            case '1':
                log(Level.INFO, HttpServletResponse.SC_OK, "Correct Versions");
                break;
            case '2':
                log(Level.WARN, HttpServletResponse.SC_CONFLICT,
                    "Versions do not match! Expected Version " + mmsVersion + ". Instead received " + paramVal);
                break;
            case '3':
                log(Level.ERROR, HttpServletResponse.SC_CONFLICT,
                    "Missing MMS Version or invalid parameter. Received parameter:" + paramArg + " and argument:"
                        + mmsVersion + ". Request was: " + jsonRequest);
                break;
            // TODO: This should be removed but for the moment I am leaving this
            // in as a contingency if anything else may break this.
            case '4':
                log(Level.ERROR, HttpServletResponse.SC_CONFLICT,
                    "Wrong MMS Version or invalid input. Expected mmsVersion=" + mmsVersion + ". Instead received "
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
     * Helper utility to get the String value of a request parameter, calls on
     * getParameterNames from the WebScriptRequest object to compare the
     * parameter name passed in that is desired from the header.
     *
     * @param req          WebScriptRequest with parameter to be checked
     * @param name         String of the request parameter name to check
     * @param defaultValue default value if there is no parameter with the given name
     * @return 'empty' if the parameter is assigned no value, if it is assigned
     * "parameter value" (ignoring case), or if it's default is default
     * value and it is not assigned "empty" (ignoring case).
     * @author dank
     */
    public static String getStringArg(WebScriptRequest req, String name, String defaultValue) {
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
     * @param req WebScriptRequest
     */
    private void setRequestJSON(WebScriptRequest req) {

        try {
            privateRequestJSON = (JSONObject) req.parseContent();
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not retrieve JSON");
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
    }

    private JSONObject getRequestJSON(WebScriptRequest req) {
        // Returns immediately if requestJSON has already been set before checking MMS Versions
        if (privateRequestJSON == null) {
            return null;
        }
        // Sets privateRequestJSON
        setRequestJSON(req);
        return privateRequestJSON;
    }
}
