package gov.nasa.jpl.view_repo.webscripts.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.log4j.Logger;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;

public class SitePermission {

    private static Logger logger = Logger.getLogger(SitePermission.class);

    public static final String ADMIN_USER_NAME = "admin";

    public enum Permission {
        READ, WRITE
    }

    private static List<String> adminList = new ArrayList<>(Arrays.asList("ALFRESCO_ADMINISTRATOR","SITE_ADMINISTRATOR"));

    public static Boolean hasPermission(String orgId, String projectId, String refId, Permission permission) {
        boolean hasPerm = false;
        boolean isTag = isTag(projectId, refId);

        if (isAdmin() && !isTag) {
            return true;
        }
        if (Utils.isNullOrEmpty(orgId)) {
            return null;
        }
        EmsScriptNode targetNode = EmsScriptNode.getSiteNode(orgId);
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
        return hasPerm;
    }

    public static boolean hasPermissionToBranch(String orgId, String projectId, String refId) {
        EmsScriptNode targetNode = EmsScriptNode.getSiteNode(orgId).childByNamePath("/" + projectId + "/refs/" + refId);
        return targetNode.checkPermissions("Read");
    }

    public static boolean isAdmin() {
        List<String> userGroups = EmsScriptNode.getUserGroups(AuthenticationUtil.getRunAsUser());
        for (String adminString : adminList) {
            for (String userGroup : userGroups) {
                if (userGroup.contains(adminString)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static boolean isTag(String projectId, String refId) {
        if (projectId != null && refId != null && !refId.equals("master")) {
            EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
            return emsNodeUtil.isTag();
        }
        return false;
    }
}
