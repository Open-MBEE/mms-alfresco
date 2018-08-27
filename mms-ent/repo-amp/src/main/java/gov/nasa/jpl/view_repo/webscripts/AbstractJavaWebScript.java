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

import com.google.gson.JsonArray;
import gov.nasa.jpl.view_repo.util.Sjm;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
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

    protected void updateResponse(int code, String msg) {
        response.append(msg);
        responseStatus.setCode(code);
        responseStatus.setMessage(msg);
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
                if (logger.isDebugEnabled()) {
                    logger.debug(msg);
                }
                break;
            default:
                // TODO: investigate if this the default thing to do
                if (logger.isDebugEnabled()) {
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

    protected JsonArray parseErrors(Map<String, Set<String>> errors) {
        JsonArray errorMessages = new JsonArray();

        for (Map.Entry<String, Integer> level : Sjm.ERROR_LEVELS.entrySet()) {
            if (errors.get(level.getKey()) != null && !errors.get(level.getKey()).isEmpty()) {
                for (String sysmlid : errors.get(level.getKey())) {
                    JsonObject errorPayload = new JsonObject();
                    errorPayload.addProperty("code", level.getValue());
                    errorPayload.addProperty(Sjm.SYSMLID, sysmlid);
                    errorPayload.addProperty("message",
                        String.format("Element %s was not found", sysmlid));
                    errorMessages.add(errorPayload);
                }
            }
        }
        return errorMessages;
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
        return this.services;
    }

    protected boolean checkRequestContent(WebScriptRequest req) {
        if (req.getContent() == null) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "No content provided.");
            return false;
        }
        return true;
    }

    protected boolean checkRequestVariable(Object value, String type) {
        if (value == null) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "%s not found.", type);
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
        if (!Arrays.asList(req.getParameterNames()).contains(name)) {
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

    protected void printFooter(String user, Logger logger, Timer timer) {
        logger.info(String.format("%d %s %s", responseStatus.getCode(), user, timer));
    }

    protected void printHeader(String user, Logger logger, WebScriptRequest req) {
        printHeader(user, logger, req, false);
    }

    protected void printHeader(String user, Logger logger, WebScriptRequest req, boolean skipReq) {
        logger.info(String.format("%s %s", user, req.getURL()));
        try {
            if (!skipReq) {
                JsonParser parser = new JsonParser();
                String content = req.getContent().getContent();
                if (content != null && !content.isEmpty()) {
                    parser.parse(content);
                    logger.info(String.format("%s", content));
                }
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

    public static String getRefId(WebScriptRequest req) {
        String refId = req.getServiceMatch().getTemplateVars().get(REF_ID);
        if (refId == null || refId.length() <= 0) {
            refId = NO_WORKSPACE_ID;
        }
        return refId;
    }

    /**
     * This needs to be called with the incoming JSON request to populate the local source
     * variable that is used in the sendDeltas call.
     *
     * @param postJson
     */
    protected void populateSourceApplicationFromJson(JsonObject postJson) {
        requestSourceApplication = JsonUtil.getOptString(postJson, "source");
    }

    /**
     * Creates a json like object in a string and puts the response in the message key
     *
     * @return The resulting string, ie "{'message':response}" or "{}"
     */
    public String createResponseJson() {
        String resToString = response.toString();
        String resStr = !Utils.isNullOrEmpty(resToString) ? resToString.replaceAll(System.lineSeparator(), "") : "";
        return !Utils.isNullOrEmpty(resStr) ? String.format("{\"message\":\"%s\"}", resStr) : "{}";
    }
}
