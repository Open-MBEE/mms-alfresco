/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech"). U.S. Government sponsorship
 * acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. - Neither the name of Caltech nor its operating
 * division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.util;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Set;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptVersion;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.springframework.extensions.webscripts.Status;

import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;

/**
 * Extension of ScriptNode to support EMS needs
 *
 * @author cinyoung
 */
public class EmsScriptNode extends ScriptNode {
    private static final long serialVersionUID = 9132455162871185541L;

    public static final String ADMIN_USER_NAME = "admin";

    static Logger logger = Logger.getLogger(ScriptNode.class);

    public static Repository repository = null;
    public static ServiceRegistry services = null;

    public EmsScriptNode(NodeRef nodeRef, ServiceRegistry services) {
        super(nodeRef, services);
    }

    public static Repository getRepository() {
        return repository;
    }

    public static void setRepository(Repository repositoryHelper) {
        EmsScriptNode.repository = repositoryHelper;
    }

    public static void setServices(ServiceRegistry services) {
        EmsScriptNode.services = services;
    }

    /**
     * @see org.alfresco.repo.jscript.ScriptNode#childByNamePath(java.lang.String)
     */
    @Override public EmsScriptNode childByNamePath(String path) {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !ADMIN_USER_NAME.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(ADMIN_USER_NAME);
        }

        ScriptNode child = super.childByNamePath(path);

        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }

        if (child == null || !child.exists()) {
            return null;
        }
        EmsScriptNode childNode = new EmsScriptNode(child.getNodeRef(), services);
        return childNode;
    }

    @Override public EmsScriptNode createFile(String name) {
        EmsScriptNode fileNode = new EmsScriptNode(super.createFile(name).getNodeRef(), services);
        return fileNode;
    }

    public EmsScriptNode createFolder(String name, String type) {
        NodeRef folderRef = super.createFolder(name, type).getNodeRef();
        EmsScriptNode folder = new EmsScriptNode(folderRef, services);
        return folder;
    }

    /**
     * Override createNode to return an EmsScriptNode
     *
     * @param name cm:name of node (which may also be the sysml:id)
     * @param type Alfresco Content Model type of node to create
     * @return created child EmsScriptNode
     */
    @Override public EmsScriptNode createNode(String name, String type) {
        EmsScriptNode result = null;

        Map<QName, Serializable> props = new HashMap<>(1, 1.0f);
        props.put(ContentModel.PROP_NAME, name);

        QName typeQName = createQName(type);
        if (typeQName != null) {
            try {
                ChildAssociationRef assoc = services.getNodeService().createNode(nodeRef, ContentModel.ASSOC_CONTAINS,
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(name)),
                    createQName(type), props);
                result = new EmsScriptNode(assoc.getChildRef(), services);
            } catch (Exception e) {
                logger.error(
                    "Got exception in " + "createNode(name=" + name + ", type=" + type + ") for EmsScriptNode(" + this
                        + ") calling createNode(nodeRef=" + nodeRef + ", . . .)");
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override public EmsScriptNode getParent() {
        ScriptNode myParent = super.getParent();
        if (myParent == null)
            return null;
        return new EmsScriptNode(myParent.getNodeRef(), services);
    }

    /**
     * Get the property of the specified type for non-noderef properties. Throws unsupported
     * operation exception otherwise (go and fix the code if that happens).
     *
     * @param acmType   Short name of property to get
     * @return
     */
    public Object getProperty(String acmType) {
        NodeRef nodeRef = this.getNodeRef();
        if (nodeRef == null || acmType == null) {
            return null;
        }

        if (acmType.isEmpty()) {
            return null;
        }
        QName qName = createQName(acmType, services);
        Object result;
        if (services == null) {
            services = getServices();
        }
        result = services.getNodeService().getProperty(nodeRef, qName);
        return result;
    }

    /**
     * Get the properties of this node
     *
     * @return
     */
    @Override public Map<String, Object> getProperties() {
        Map<String, Object> result = new HashMap<>();
        Map<QName, Serializable> map = services.getNodeService().getProperties(nodeRef);
        for (Map.Entry<QName, Serializable> entry : map.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }

    /**
     * Genericized function to set property for non-collection types
     *
     * @param acmType Property short name for alfresco content model type
     * @param value   Value to set property to
     */
    public <T extends Serializable> boolean setProperty(String acmType, T value) {
        if (logger.isDebugEnabled())
            logger.debug("setProperty(acmType=" + acmType + ", value=" + value + ")");
        boolean success = true;
        try {
            services.getNodeService().setProperty(nodeRef, createQName(acmType), value);
        } catch (Exception e) {
            // This should never happen!
            success = false;
        }
        return success;
    }

    /**
     * Checks whether user has permissions to the node and logs results and status as appropriate
     *
     * @param permissions Permissions to check
     * @return true if user has specified permissions to node, false otherwise
     */
    public boolean checkPermissions(String permissions) {
        return checkPermissions(permissions, null, null);
    }

    /**
     * Checks whether the user making the web request (not the run-as user) has permissions to the
     * node and logs results and status as appropriate. If a response object is supplied, a warning
     * is generated when the user does not have the permission.
     *
     * @param permissions Permissions to check
     * @param response
     * @param status
     * @return true if user has specified permissions to node, false otherwise
     */
    public boolean checkPermissions(String permissions, StringBuffer response, Status status) {
        if (!hasPermission(permissions)) {
            if (response != null) {

                // Assume admin role to make sure getProperty() doesn't fail
                // because of permissions.
                String runAsUser = AuthenticationUtil.getRunAsUser();
                boolean changeUser = !ADMIN_USER_NAME.equals(runAsUser);
                if (changeUser) {
                    AuthenticationUtil.setRunAsUser(ADMIN_USER_NAME);
                }

                // Get sysmlid
                Object property = getProperty(Acm.CM_NAME);

                // Return to original running user.
                if (changeUser) {
                    AuthenticationUtil.setRunAsUser(runAsUser);
                }

                // Log warning for missing permissions.
                if (property != null) {
                    String msg = String.format("Warning! No %s privileges to sysmlid: %s.", permissions.toUpperCase(),
                        property.toString());
                    response.append(msg);
                    if (status != null) {
                        status.setCode(HttpServletResponse.SC_FORBIDDEN, msg);
                    }
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Checks whether the user making the web request (not the run-as user) has permissions to the
     * node and logs results and status as appropriate.
     *
     * @param permission the permission to check
     * @return whether the user has the permission for this node
     * @see org.alfresco.repo.jscript.ScriptNode#hasPermission(java.lang.String)
     */
    @Override public boolean hasPermission(String permission) {
        String realUser = AuthenticationUtil.getFullyAuthenticatedUser();
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !realUser.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(realUser);
        }
        boolean b = super.hasPermission(permission);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }
        return b;
    }
    
    public static ServiceRegistry getServices() {
        return services;
    }


    /**************************
     * Miscellaneous functions
     **************************/
    public Version getHeadVersion() {
        VersionService vs = getServices().getVersionService();
        Version headVersion = null;
        if (getIsVersioned()) {
            VersionHistory history = vs.getVersionHistory(this.nodeRef);
            if (history != null) {
                headVersion = history.getHeadVersion();
            }
        }
        return headVersion;
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
        SiteInfo si = services.getSiteService().getSite(sysmlid);
        if (si != null) {
            EmsScriptNode site = new EmsScriptNode(si.getNodeRef(), services);
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

    /**
     * Returns a list of all the groups the passed user belongs to. Note, there is no java interface
     * for this, so this code is based on what the javascript interface does.
     *
     * See: https://svn.alfresco.com/repos/alfresco-open-mirror/alfresco/HEAD/root
     * /projects/repository/source/java/org/alfresco/repo/jscript/People.java
     *
     * @param user
     * @return
     */
    public static List<String> getUserGroups(String user) {

        List<String> authorityNames = new ArrayList<>();

        AuthorityService aService = getServices().getAuthorityService();
        Set<String> authorities = aService.getContainingAuthoritiesInZone(AuthorityType.GROUP, user, null, null, 1000);
        for (String authority : authorities) {
            NodeRef group = aService.getAuthorityNodeRef(authority);
            if (group != null) {
                authorityNames.add(authority);
            }
        }

        return authorityNames;
    }

    /**
     * Helper to create a QName from either a fully qualified or short-name QName string
     * <P>
     *
     * @param s        Fully qualified or short-name QName string
     * @param services ServiceRegistry for getting the service to resolve the name space
     * @return QName
     */
    public static QName createQName(String s, ServiceRegistry services) {
        if (s == null)
            return null;
        QName qname;
        if (s.contains("{")) {
            qname = QName.createQName(s);
        } else {
            qname = QName.createQName(s, services.getNamespaceService());
        }
        return qname;
    }

    public static EmsScriptNode updateOrCreateArtifact(String artifactId, Path filePath, String fileType, String orgId, String projectId, String refId) {

        EmsScriptNode artifactNode;
        String finalType = null;
        Tika tika = new Tika();
        File content = filePath.toFile();

        try {
            finalType = tika.detect(content);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("updateOrCreateArtifact: ", e);
            }
        }

        if (finalType == null) {
            // Fallback if getting content type fails
            if (fileType != null) {
                finalType = fileType;
            } else {
                logger.error("Could not determine type of artifact: " + filePath.getFileName().toString());
                return null;
            }
        }

        EmsScriptNode targetSiteNode = EmsScriptNode.getSiteNode(orgId);

        // find site; it must exist!
        if (targetSiteNode == null || !targetSiteNode.exists()) {
            logger.error("Can't find node for site: " + orgId + "!");
            return null;
        }

        // find or create subfolder
        EmsScriptNode subfolder = targetSiteNode.childByNamePath("/" + projectId + "/refs/" + refId);
        if (subfolder == null || !subfolder.exists()) {
            return null;
        }

        // find or create node:
        artifactNode = subfolder.childByNamePath("/" + artifactId);
        // Node wasnt found, so create one:
        if (artifactNode == null) {
            artifactNode = subfolder.createNode(artifactId, "cm:content");
        }

        if (artifactNode == null || !artifactNode.exists()) {
            logger.error("Failed to create new PNG artifact " + artifactId + "!");
            return null;
        }

        if (!artifactNode.hasAspect("cm:versionable")) {
            artifactNode.addAspect("cm:versionable");
        }
        if (!artifactNode.hasAspect("cm:indexControl")) {
            artifactNode.addAspect("cm:indexControl");
        }

        artifactNode.setProperty(Acm.CM_TITLE, artifactId);
        artifactNode.setProperty("cm:isIndexed", true);
        artifactNode.setProperty("cm:isContentIndexed", false);

        if (logger.isDebugEnabled()) {
            logger.debug("Creating artifact with indexing: " + artifactNode.getProperty("cm:isIndexed"));
        }

        ContentWriter writer =
            services.getContentService().getWriter(artifactNode.getNodeRef(), ContentModel.PROP_CONTENT, true);
        writer.putContent(content);

        ContentData contentData = writer.getContentData();
        contentData = ContentData.setMimetype(contentData, finalType);
        contentData = ContentData.setEncoding(contentData, "UTF-8");
        services.getNodeService().setProperty(artifactNode.getNodeRef(), ContentModel.PROP_CONTENT, contentData);

        return artifactNode;
    }

}
