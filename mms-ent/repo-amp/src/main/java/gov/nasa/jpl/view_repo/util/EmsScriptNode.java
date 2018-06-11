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

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptVersion;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
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

    public boolean renamed = false;

    // provide logging capability of what is done
    private StringBuffer response = null;

    // provide status as necessary
    private Status status = null;

    /**
     * whether to use the foundational Alfresco Java API or ScriptNode class that uses the
     * JavaScript API
     */
    public boolean useFoundationalApi = true; // TODO this will be removed

    protected EmsScriptNode workspace = null;

    /**
     * Replicates the behavior of ScriptNode versions, which is private.
     */
    protected Object[] myVersions = null;

    public AbstractJavaWebScript webscript = null;

    // TODO add nodeService and other member variables when no longer
    // subclassing ScriptNode
    // extend Serializable after removing ScriptNode extension

    public EmsScriptNode(NodeRef nodeRef, ServiceRegistry services, StringBuffer response, Status status) {
        this(nodeRef, services);
        setStatus(status);
    }

    public EmsScriptNode(NodeRef nodeRef, ServiceRegistry services, StringBuffer response) {
        this(nodeRef, services);
        setResponse(response);
    }

    /**
     * Create a version of this document. Note: this will add the cm:versionable aspect.
     *
     * @param history      Version history note
     * @param majorVersion True to save as a major version increment, false for minor version.
     * @return ScriptVersion object representing the newly added version node
     */
    @Override public ScriptVersion createVersion(String history, boolean majorVersion) {
        this.myVersions = null;
        return super.createVersion(history, majorVersion);
    }

    /**
     * Check-in a working copy document. The current state of the working copy is copied to the
     * original node, this will include any content updated in the working node. Note that this
     * method can only be called on a working copy Node.
     *
     * @param history      Version history note
     * @param majorVersion True to save as a major version increment, false for minor version.
     * @return the original Node that was checked out.
     */
    @Override public ScriptNode checkin(String history, boolean majorVersion) {
        this.myVersions = null;
        return super.checkin(history, majorVersion);
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
        EmsScriptNode childNode = new EmsScriptNode(child.getNodeRef(), services, response);
        return childNode;
    }

    @Override public EmsScriptNode createFile(String name) {
        EmsScriptNode fileNode = new EmsScriptNode(super.createFile(name).getNodeRef(), services, response, status);
        return fileNode;
    }

    public EmsScriptNode createFolder(String name, String type, NodeRef sourceFolder) {
        if (logger.isInfoEnabled()) {
            logger.info("creating " + name + " in " + sourceFolder);
        }

        NodeRef folderRef = super.createFolder(name, type).getNodeRef();
        EmsScriptNode folder = new EmsScriptNode(folderRef, services, response, status);

        return folder;
    }

    /**
     * Check whether or not a node has a property, update or create as necessary
     *
     * NOTE: this only works for non-collection properties - for collections handwrite (or see how
     * it's done in ModelPost.java)
     *
     * @param acmType Short name for the Alfresco Content Model type
     * @param value   Value to set property to
     * @return true if property updated, false otherwise (e.g., value did not change)
     */
    public <T extends Serializable> boolean createOrUpdateProperty(String acmType, T value) {

        T oldValue = (T) getNodeRefProperty(acmType);
        if (oldValue != null && value != null) {
            if (!value.equals(oldValue)) {
                setProperty(acmType, value);
                log(getName() + ": " + acmType + " property updated to value = " + value);
                return true;
            }
        }
        // Note: Per CMED-461, we are allowing properties to be set to null
        else {
            log(getName() + ": " + acmType + " property created with value = " + value);
            boolean changed = setProperty(acmType, value);
            // If setting the property to null, the modified time is not changed
            // by alfresco if
            // it was previously null, which is the initial state of the
            // property, but we want
            // the modification time to be altered in this case too:
            if (oldValue == null && value == null) {
                setProperty("cm:modified", new Date(), false, 0);
            }
            if (!changed) {
                logger.warn(
                    "Failed to set property for new value in createOrUpdateProperty(" + acmType + ", " + value + ")");
            }
            return changed;
        }

        return false;
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

        if (!useFoundationalApi) {
            ScriptNode scriptNode = super.createNode(name, type);
            result = new EmsScriptNode(scriptNode.getNodeRef(), services, response);
        } else {
            Map<QName, Serializable> props = new HashMap<>(1, 1.0f);
            props.put(ContentModel.PROP_NAME, name);

            QName typeQName = createQName(type);
            if (typeQName != null) {
                try {
                    ChildAssociationRef assoc = services.getNodeService()
                        .createNode(nodeRef, ContentModel.ASSOC_CONTAINS,
                            QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(name)),
                            createQName(type), props);
                    result = new EmsScriptNode(assoc.getChildRef(), services, response);
                } catch (Exception e) {
                    logger.error(
                        "Got exception in " + "createNode(name=" + name + ", type=" + type + ") for EmsScriptNode("
                            + this + ") calling createNode(nodeRef=" + nodeRef + ", . . .)");
                    e.printStackTrace();
                }

            } else {
                log("Could not find type " + type);
            }
        }

        return result;
    }

    @Override public String getName() {
        super.getName();
        return (String) getProperty(Acm.CM_NAME);
    }

    @Override public EmsScriptNode getParent() {
        ScriptNode myParent = super.getParent();
        if (myParent == null)
            return null;
        return new EmsScriptNode(myParent.getNodeRef(), services, response);
    }

    /**
     * Getting a noderef property needs to be contextualized by the workspace and time This works
     * for any property type noderef or otherwise, so use this if you want to be safe.
     *
     * @param acmType
     * @return
     */
    public Object getNodeRefProperty(String acmType) {
        Object result = getPropertyImpl(acmType, true);
        return result;
    }

    /**
     * Get the property of the specified type for non-noderef properties. Throws unsupported
     * operation exception otherwise (go and fix the code if that happens).
     *
     * @param acmType   Short name of property to get
     * @return
     */
    public Object getProperty(String acmType) {
        return getProperty(acmType, true);
    }

    /**
     * Get the property of the specified type for non-noderef properties. Throws unsupported
     * operation exception otherwise (go and fix the code if that happens).
     *
     * @param acmType   Short name of property to get
     * @param cacheOkay
     * @return
     */
    public Object getProperty(String acmType, boolean cacheOkay) {
        Object result = getPropertyImpl(acmType, cacheOkay);
        return result;
    }

    private Object getPropertyImpl(String acmType, boolean cacheOkay) {
        return NodeUtil.getNodeProperty(this, acmType, getServices(), useFoundationalApi, cacheOkay);
    }

    /**
     * Get the properties of this node
     *
     * @return
     */
    @Override public Map<String, Object> getProperties() {

        if (useFoundationalApi) {
            Map<String, Object> result = new HashMap<>();
            Map<QName, Serializable> map = services.getNodeService().getProperties(nodeRef);
            for (Map.Entry<QName, Serializable> entry: map.entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
            return result;
        } else {
            return super.getProperties();
        }
    }

    public StringBuffer getResponse() {
        return response;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Append onto the response for logging purposes
     *
     * @param msg Message to be appened to response TODO: fix logger for EmsScriptNode
     */
    public void log(String msg) {
    }

    /**
     * Genericized function to set property for non-collection types
     *
     * @param acmType Property short name for alfresco content model type
     * @param value   Value to set property to
     */
    public <T extends Serializable> boolean setProperty(String acmType, T value) {
        return setProperty(acmType, value, true, 0);
    }

    public <T extends Serializable> boolean setProperty(String acmType, T value, boolean cacheOkay,
        // count prevents inf
        // loop
        int count) {
        if (logger.isDebugEnabled())
            logger.debug("setProperty(acmType=" + acmType + ", value=" + value + ")");
        boolean success = true;
        if (useFoundationalApi) {
            try {
                services.getNodeService().setProperty(nodeRef, createQName(acmType), value);
            } catch (Exception e) {
                // This should never happen!
                success = false;
            }
        } else {
            getProperties().put(acmType, value);
            save();
        }
        return success;
    }

    public void setResponse(StringBuffer response) {
        this.response = response;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    /**
     * Override equals for EmsScriptNodes
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override public boolean equals(Object obj) {
        return equals(obj, true);
    }

    @Override public int hashCode() {
        return parent.hashCode();
    }

    /**
     * @return the head or current version of the node ref if it exists; otherwise return the
     * existing node ref
     */
    public NodeRef normalizedNodeRef() {
        VersionService vs = getServices().getVersionService();
        Version thisHeadVersion = this.getHeadVersion();
        NodeRef thisCurrent = thisHeadVersion == null ? null : thisHeadVersion.getVersionedNodeRef();
        if (thisCurrent == null) {
            Version thisCurrentVersion = vs.getCurrentVersion(this.nodeRef);
            thisCurrent = thisCurrentVersion == null ? null : thisCurrentVersion.getVersionedNodeRef();
        }
        if (thisCurrent == null)
            return nodeRef;
        return thisCurrent;
    }

    /**
     * Check to see if the nodes are the same or (if tryCurrentVersions is true) if their
     * currentVersions are the same.
     *
     * @param obj
     * @param tryCurrentVersions
     * @return true iff equal
     */
    public boolean equals(Object obj, boolean tryCurrentVersions) {

        if (!(obj instanceof EmsScriptNode))
            return false;
        EmsScriptNode that = (EmsScriptNode) obj;
        boolean same = this.nodeRef.equals(that.nodeRef);
        if (same || !tryCurrentVersions)
            return same;

        // See if they are different versions of the same node.
        VersionService vs = getServices().getVersionService();
        boolean isThisV = vs.isAVersion(this.nodeRef);
        boolean isThatV = vs.isAVersion(that.nodeRef);
        if (!isThisV && !isThatV)
            return same;
        NodeRef thisCurrent = this.normalizedNodeRef();
        NodeRef thatCurrent = that.normalizedNodeRef();
        if (thisCurrent == thatCurrent)
            return true;
        if (thisCurrent == null || thatCurrent == null)
            return false;
        return thisCurrent.equals(thatCurrent);
    }

    /**
     * Override exists for EmsScriptNodes
     *
     * @see org.alfresco.repo.jscript.ScriptNode#exists()
     */
    @Override public boolean exists() {
        return exists(false);
    }

    public boolean exists(boolean includeDeleted) {
        if (!scriptNodeExists())
            return false;
        return !(!includeDeleted && hasAspect("ems:Deleted"));
    }

    public boolean scriptNodeExists() {
        return super.exists();
    }

    public ServiceRegistry getServices() {
        return services;
    }

    public EmsScriptNode(NodeRef nodeRef, ServiceRegistry services) {
        super(nodeRef, services);
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

    @Override public boolean removeAspect(String type) {
        if (hasAspect(type)) {
            return super.removeAspect(type);
        }
        return true;
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

    public void delete() {
        if (!checkPermissions(PermissionService.WRITE, getResponse(), getStatus())) {
            log("no write permissions to delete workpsace " + getName());
            return;
        }
    }

}
