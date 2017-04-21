package gov.nasa.jpl.view_repo.webscripts.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

public class SitePermission {

    private static Logger logger = Logger.getLogger(SitePermission.class);


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

    public static boolean hasPermission(String orgId, JSONArray elements, String projectId, String refId, String commitId,
                    Permission permission, StringBuffer response, Map<String, Map<Permission, Boolean>> permCache) {

        if (isAdmin()) {
            return true;
        }

        int perms = 0;
        if (elements != null && elements.length() > 0) {
            // Adding in a temp fix to check only the first element
            for (int i = 0; i < 1; i++) {
                JSONObject elem = elements.getJSONObject(i);
                if (!hasPermission(orgId, elem, projectId, refId, commitId, permission, response, permCache)) {
                    perms++;
                }
            }
        }
        return perms < 1;
    }

    public static boolean hasPermission(String orgId, JSONObject element, String projectId, String refId, String commitId,
                    Permission permission, StringBuffer response, Map<String, Map<Permission, Boolean>> permCache) {
        boolean hasPerm = false;
        boolean isTag = isTag(projectId, refId);

        if (isAdmin() && !isTag) {
            return true;
        }

        if (element != null && element.has(Sjm.SYSMLID)) {
            if (Utils.isNullOrEmpty(orgId)) {
                orgId = getElementSiteId(element.getString(Sjm.SYSMLID), projectId, refId);
            }
            if (!Utils.isNullOrEmpty(orgId)) {
                if (permCache.containsKey(orgId) && permCache.get(orgId).containsKey(permission)) {
                    return permCache.get(orgId).get(permission);
                } else {
                    if (orgId.equalsIgnoreCase("holding_bin")) {
                        return true;
                    } else {
                        EmsScriptNode siteNode = getSiteNode(orgId);
                        if (siteNode == null) {
                            return false;
                        } else {
                            EmsScriptNode targetNode = siteNode.childByNamePath("/" + projectId + (refId != null ? "/refs/" + refId : ""));

                            if (targetNode == null) {
                                targetNode = siteNode;
                            }

                            String targetId = targetNode.getSysmlId();
                            if (permission == Permission.READ) {
                                if (permCache.containsKey(targetId) && permCache.get(targetId).containsKey(Permission.READ)) {
                                    hasPerm = permCache.get(targetId).get(Permission.READ);
                                } else if (element.optString(Sjm.SYSMLID).contains("master_")) {
                                    hasPerm = true;
                                } else {
                                    hasPerm = siteNode.checkPermissions("Read");
                                    Map<Permission, Boolean> permMap = new HashMap<>();
                                    permMap.put(Permission.READ, hasPerm);
                                    permCache.put(targetId, permMap);
                                }
                            } else {
                                if (permCache.containsKey(targetId) && permCache.get(targetId).containsKey(Permission.WRITE)) {
                                    hasPerm = permCache.get(targetId).get(Permission.WRITE);
                                } else {
                                    Map<Permission, Boolean> permMap = new HashMap<>();
                                    if (isTag) {
                                        return false;
                                    } else {
                                        hasPerm = siteNode.checkPermissions("Write");
                                    }
                                    permMap.put(Permission.WRITE, hasPerm);
                                    permCache.put(targetId, permMap);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!hasPerm) {
            hasPerm = NodeUtil.userHasWorkspaceLdapPermissions();
        }
        return hasPerm;
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

    protected static String getElementSiteId(String orgId, String projectId, String refId) {
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        return emsNodeUtil.getOrganization(orgId).getJSONObject(0).getString("orgId");
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
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        return emsNodeUtil.isTag();
    }

    protected static String findSiteId(JSONObject element, String projectId, String refId, String commitId) {
        if (element == null)
            return null;    //TODO error handling

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        return emsNodeUtil.getSite(element.optString(Sjm.SYSMLID));
    }

    public static EmsScriptNode getSiteNode(String sysmlid) {
        if (sysmlid == null)
            return null;
        ServiceRegistry services = NodeUtil.getServiceRegistry();
        SiteInfo si = services.getSiteService().getSite(sysmlid);
        if (si != null) {
            return new EmsScriptNode(si.getNodeRef(), services, null);
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
