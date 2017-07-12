package gov.nasa.jpl.view_repo.webscripts.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nasa.jpl.view_repo.util.*;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import gov.nasa.jpl.mbee.util.Utils;

public class SitePermission {

    private static Logger logger = Logger.getLogger(SitePermission.class);

    public static final String ADMIN_USER_NAME = "admin";

    public enum Permission {
        READ, WRITE
    }

    private static List<String> adminList = new ArrayList<>(Arrays.asList("ALFRESCO_ADMINISTRATOR","SITE_ADMINISTRATOR"));

    /**
     * Given a set of JSONObjects having key Sjm.SYSMLID, check whether or not each element has "read/write" access.
     *
     * @param elements   - array of JSONObjects.
     * @param refId
     * @param commitId
     * @param permission Permission enumeration (READ or WRITE)
     * @return JSONObject containing "allowedElements" JSONArray and "deniedElements" JSONArray. "allowedElements" array
     * contains those elements which have "read/write" access while "deniedElements" array contains those that don't have
     * "read/write" access.
     * @throws Throwable
     * @response StringBuffer to retain any error messages
     */
    public static JSONObject checkPermission(JSONArray elements, String projectId, String refId, String commitId,
                    Permission permission, StringBuffer response) {
        if (null == elements) {
            return null;
        }

        //TODO take workspace and timestamp into consideration
        JSONObject rtnJson = new JSONObject();
        JSONArray allowedElements = new JSONArray();
        JSONArray deniedElements = new JSONArray();
        rtnJson.put("allowedElements", allowedElements);
        rtnJson.put("deniedElements", deniedElements);

        for (int i = 0; i < elements.length(); i++) {
            JSONObject elem = elements.getJSONObject(i);
            if (null == elem) {
                logError(response, deniedElements, elem, "JSONObject not found in input array.");
                continue;
            }

            JSONObject node = findElementNode(elem, projectId, refId, commitId);
            if (null == node) {
                logError(response, deniedElements, elem, String.format("Unable to retrieve element %s", elem));
                continue;
            }

            String siteName = findSiteId(node, projectId, refId, commitId);
            if (Utils.isNullOrEmpty(siteName)) {
                logError(response, deniedElements, elem, String.format("Unable to extract site name from %s", node));
                continue;
            }

            ServiceRegistry services = NodeUtil.getServiceRegistry();
            SiteInfo siteInfo = services.getSiteService().getSite(siteName);
            EmsScriptNode siteNode = null;
            if (siteInfo != null) {
                siteNode = new EmsScriptNode(siteInfo.getNodeRef(), services, null);
            }

            if (siteNode == null) {
                logError(response, deniedElements, elem, String.format("Unable to find site '%s'", siteName));
                continue;
            }

            EmsScriptNode targetNode = siteNode.childByNamePath("/" + projectId + (refId != null ? "/" + refId : ""));

            boolean hasAccess;

            if (targetNode == null) {
                targetNode = siteNode;
            }

            if (permission == Permission.READ) {
                hasAccess = targetNode.checkPermissions("Read");
            } else {
                hasAccess = targetNode.checkPermissions("Write");
            }

            if (hasAccess) {
                allowedElements.put(elem);
            } else {
                deniedElements.put(elem);
            }
        }
        return rtnJson;
    }

    public static Boolean hasPermission(String orgId, String projectId, String refId, Permission permission) {
        boolean hasPerm = false;
        boolean isTag = isTag(projectId, refId);

        if (isAdmin() && !isTag) {
            return true;
        }
        if (Utils.isNullOrEmpty(orgId)) {
            return null;
        }
        EmsScriptNode targetNode = getSiteNode(orgId);
        if (targetNode == null) {
            return null;
        }
        if (projectId != null) {
            targetNode = targetNode.childByNamePath("/" + projectId + (refId != null ? "/refs/" + refId : ""));
        }
        if (targetNode == null) {
            return null;
        }
        if (permission == Permission.READ) {
            hasPerm = targetNode.checkPermissions("Read");
        } else {
            hasPerm = targetNode.checkPermissions("Write");
        }
        if (!hasPerm) {
            hasPerm = NodeUtil.userHasWorkspaceLdapPermissions(); //???
        }
        return hasPerm;
    }

    public static boolean hasPermissionToBranch(String orgId, String projectId, String refId) {
        EmsScriptNode targetNode = getSiteNode(orgId).childByNamePath("/" + projectId + "/refs/" + refId);
        return targetNode.checkPermissions("Read");
    }

    public static boolean isAdmin() {
        List<String> userGroups = NodeUtil.getUserGroups(NodeUtil.getUserName());
        for (String adminString : adminList) {
            for (String userGroup : userGroups) {
                if (userGroup.contains(adminString)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static String getElementSiteId(String sysmlid, String projectId, String refId) {
        if (projectId == null) {
            return sysmlid;
        } else {
            if (refId == null) {
                refId = "master";
            }
            EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
            return emsNodeUtil.getOrganization(sysmlid).getJSONObject(0).getString("orgId");
        }
    }

    /**
     * Given a JSONObject with Sjm.SYSMLID key, returns a JSONObject with corresponding workspace and timestamp
     * found from backend (postgressql and elastic search)
     *
     * @param elem      - JSONObject having key/value Sjm.SYSMLID
     * @param refId
     * @param commitId
     * @return JSONObject populated with relevant data
     */
    protected static JSONObject findElementNode(JSONObject elem, String projectId, String refId, String commitId) {
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        return emsNodeUtil.getNodeBySysmlid(elem.optString(Sjm.SYSMLID));
    }

    protected static boolean isTag(String projectId, String refId) {
        if (projectId != null && refId != null && !refId.equals("master")) {
            EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
            return emsNodeUtil.isTag();
        }
        return false;
    }

    protected static String findSiteId(JSONObject element, String projectId, String refId, String commitId) {
        if (element == null)
            return null;    //TODO error handling

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        return emsNodeUtil.getSite(element.optString(Sjm.SYSMLID));
    }

    public static EmsScriptNode getSiteNode(String sysmlid) {
        if (sysmlid == null) {
            return null;
        }
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !ADMIN_USER_NAME.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(ADMIN_USER_NAME);
        }
        ServiceRegistry services = NodeUtil.getServiceRegistry();
        SiteInfo si = services.getSiteService().getSite(sysmlid);
        if (si != null) {
            EmsScriptNode site = new EmsScriptNode(si.getNodeRef(), services, null);
            if (changeUser) {
                AuthenticationUtil.setRunAsUser(runAsUser);
            }
            return site;
        }
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }
        return null;
    }

    protected static void logError(StringBuffer response, JSONArray deniedElements, JSONObject element, String msg) {
        if (response != null) {
            response.append(msg);
            response.append(System.getProperty("line.separator"));
        } else {
            logger.warn(String.format("%s", msg));
        }

        if (deniedElements != null && element != null) {
            deniedElements.put(element);
        }
    }
}
