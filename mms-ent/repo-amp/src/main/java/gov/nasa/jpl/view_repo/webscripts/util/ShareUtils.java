package gov.nasa.jpl.view_repo.webscripts.util;

import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.LogUtil;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class ShareUtils {
    static Logger logger = Logger.getLogger(ShareUtils.class);

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    private static final String UTF_8 = "UTF-8";
    private static String SHARE_URL = null;
    private static String REPO_URL = null;
    private static String LOGIN_URL = null;
    private static String CREATE_SITE_URL = null;
    private static String UPDATE_GROUP_URL = null;
    private static String username = "admin";
    private static String password = "admin";
    private static ServiceRegistry services = null;

    /**
     * Initialize the URLs based on the Alfresco system settings.
     */
    private static void initializeUrls() {
        // note this handling is due to the way apache serves as https proxy for
        // tomcat

        // note that on local instances, this should be set to admin/admin
        ShareUtils.setUsername(EmsConfig.get("app.user"));
        ShareUtils.setPassword(EmsConfig.get("app.pass"));

        if (SHARE_URL == null) {
            // TODO: remove dependency on deprecated method
            SysAdminParams adminParams = services.getSysAdminParams();
            String repoProtocol = adminParams.getAlfrescoProtocol();
            String repoHost = adminParams.getAlfrescoHost();
            int repoPort = adminParams.getAlfrescoPort();
            REPO_URL = String.format("%s://%s:%s/alfresco", repoProtocol, repoHost, repoPort);

            String shareProtocol = adminParams.getShareProtocol();
            String shareHost = adminParams.getShareHost();
            int sharePort = adminParams.getSharePort();
            SHARE_URL = String.format("%s://%s:%s/share", shareProtocol, shareHost, sharePort);

            LOGIN_URL = SHARE_URL + "/page/dologin";
            CREATE_SITE_URL = SHARE_URL + "/page/modules/create-site";
            UPDATE_GROUP_URL = REPO_URL + "/service/api/groups";
        }
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Repo URL: %s", REPO_URL));
            logger.info(String.format("Share URL: %s", SHARE_URL));
            logger.info(String.format("Login URL: %s", LOGIN_URL));
            logger.info(String.format("Create Site URL: %s", CREATE_SITE_URL));
            logger.info(String.format("Update Group URL: %s", UPDATE_GROUP_URL));
        }
    }


    /**
     * Create the site dashboard by using the Share createSite service, then make the current
     * user a manager for the specified site
     *
     * @param sitePreset
     * @param siteId
     * @param siteTitle
     * @param siteDescription
     * @param isPublic
     * @return
     */
    public static boolean constructSiteDashboard(String sitePreset, String siteId, String siteTitle,
        String siteDescription, boolean isPublic) {
        // only description is allowed to be null
        if (sitePreset == null || siteId == null || siteTitle == null) {
            logger.error(String
                .format("Fields cannot be null: sitePreset:%s  siteId:%s  siteTitle:%s", sitePreset, siteId,
                    siteTitle));
            return false;
        }

        if (siteDescription == null) {
            siteDescription = "";
        }

        initializeUrls();

        HttpClient httpClient = new HttpClient();
        httpClient.getParams().setCookiePolicy(CookiePolicy.RFC_2109);

        String loginData = "username=" + username + "&password=" + password;

        if (!makeSharePostCall(httpClient, LOGIN_URL, loginData, CONTENT_TYPE_FORM, "Login to Alfresco Share",
            HttpStatus.SC_MOVED_TEMPORARILY)) {
            logger.error("Could not login to share site");
            return false;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("shortName", siteId);
            json.put("sitePreset", sitePreset);
            json.put("title", siteTitle);
            json.put("description", siteDescription);
            json.put("visibility", isPublic ? "PUBLIC" : "PRIVATE");
        } catch (JSONException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            logger.error("Could not create JSON for site creation");
            return false;
        }

        if (!makeSharePostCall(httpClient, CREATE_SITE_URL, json.toString(), CONTENT_TYPE_JSON,
            "Create site with name: " + siteId, HttpStatus.SC_OK)) {
            logger.error("Could not create site - 1st pass");
            return false;
        }

        // for some reason need to do this twice unsure why this is the case
        if (!makeSharePostCall(httpClient, CREATE_SITE_URL, json.toString(), CONTENT_TYPE_JSON,
            "Create site with name: " + siteId, HttpStatus.SC_OK)) {
            logger.error("Could not create site -2nd pass");
            // return false;
        }

        // make calling user Site manger
        // NOTE: need additional site_, because short name is prepended with site_
        String role = String.format("site_%s_SiteManager", siteId);
        String currentUsername = AuthenticationUtil.getFullyAuthenticatedUser();
        String groupUrl = String.format("%s/%s/children/%s", UPDATE_GROUP_URL, role, currentUsername);

        if (!currentUsername.equals("admin") && !currentUsername.equals(username)) {
            if (!makeRepoPostCall(groupUrl, CONTENT_TYPE_JSON,
                String.format("add user, %s, as %s site manager", currentUsername, siteId), HttpStatus.SC_OK)) {
                logger.error("Could not set permissions on site");
            }
        }

        return true;
    }

    private static boolean makeSharePostCall(HttpClient httpClient, String url, String data, String dataType,
        String callName, int expectedStatus) {
        boolean success = false;
        PostMethod postMethod = null;
        try {
            postMethod = createPostMethod(url, data, dataType);
            int status = httpClient.executeMethod(postMethod);

            logger.debug(String.format("%s returned status: %s", callName, status));

            if (status == expectedStatus) {
                logger.debug(String.format("%s with user %s", callName, username));
                success = true;
            } else {
                logger.error(String.format("Could not %s, HTTP Status code : %s", callName, status));
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to %s%s", callName, LogUtil.getStackTrace(e)));
        } finally {
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
        }

        return success;
    }

    private static PostMethod createPostMethod(String url, String body, String contentType)
        throws UnsupportedEncodingException {
        PostMethod postMethod = new PostMethod(url);
        postMethod.setRequestHeader(HEADER_CONTENT_TYPE, contentType);
        postMethod.setRequestEntity(new StringRequestEntity(body, CONTENT_TYPE_TEXT_PLAIN, UTF_8));
        if (url.contains("service/alfresco")) {
            String userpass = username + ":" + password;
            String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
            postMethod.setRequestHeader("Authorization", basicAuth);
        }

        return postMethod;
    }

    public static void setUsername(String username) {
        ShareUtils.username = username;
    }

    public static void setPassword(String password) {
        ShareUtils.password = password;
    }

    public static void setServices(ServiceRegistry services) {
        ShareUtils.services = services;
    }

    /**
     * Method for posting to repository (requires basic authentication that doesn't seem to
     * work with the other call type)
     *
     * @param targetURL
     * @param dataType
     * @param callName
     * @param expectedStatus
     * @return
     */
    private static boolean makeRepoPostCall(String targetURL, String dataType, String callName, int expectedStatus) {
        logger.debug(String.format("posting to %s", targetURL));
        boolean success = false;

        HttpURLConnection connection = null;
        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty(HEADER_CONTENT_TYPE, dataType);

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            String userpass = username + ":" + password;
            String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
            connection.setRequestProperty("Authorization", basicAuth);

            //Get Response
            if (connection.getResponseCode() == expectedStatus) {
                success = true;
            } else {
                logger.error(
                    String.format("Failed request: %s with status: %s", targetURL, connection.getResponseMessage()));
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return success;
    }
}
