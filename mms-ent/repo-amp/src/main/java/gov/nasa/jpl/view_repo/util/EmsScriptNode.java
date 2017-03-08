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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptVersion;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.version.Version2Model;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Scriptable;
import org.springframework.extensions.webscripts.Status;

import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.HasId;
import gov.nasa.jpl.mbee.util.HasName;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Seen;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.db.PostgresHelper.DbEdgeTypes;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;

/**
 * Extension of ScriptNode to support EMS needs
 *
 * @author cinyoung
 *
 */
public class EmsScriptNode extends ScriptNode
                implements Comparator<EmsScriptNode>, Comparable<EmsScriptNode>, HasName<String>, HasId<String> {
    private static final long serialVersionUID = 9132455162871185541L;

    private PostgresHelper pgh;

    public static final String ADMIN_USER_NAME = "admin";

    static Logger logger = Logger.getLogger(ScriptNode.class);

    public static boolean expressionStuffDefault = false; // The value here is ignored.
    public boolean expressionStuff = expressionStuffDefault; // The value here is ignored.

    public static boolean optimisticAndFoolish = false;

    public static boolean tryToFlushCache = false;

    public static boolean versionCacheDebugPrint = false;

    // private members to cache qualified names, ids, and site characterizations
    // These don't work because sendCommitDeltas asks for json at two time
    // points, so the first one is cached and reused for the second timepoint.
    // If wanting to cache, cache like the deepJsonCache.
    // private String qualifiedName = null;
    // private String qualifiedId = null;

    private String siteCharacterizationId = null;

    public boolean renamed = false;
    public boolean moved = false;

    /**
     * A set of content model property names that serve as workspace metadata and whose changes are
     * not recorded in a workspace.
     */
    public static TreeSet<String> workspaceMetaProperties = new TreeSet<String>() {
        private static final long serialVersionUID = -327817873667229953L;
        {
            add("ems:workspace");
            add("ems:source");
            add("ems:parent");
            add("ems:children");
            add("ems:lastTimeSyncParent");
            add("ems:mergeSource");
        }
    };
    public static TreeSet<String> workspaceMetaAspects = new TreeSet<String>() {
        private static final long serialVersionUID = 1L;
        {
            add("ems:HasWorkspace");
            add("ems:Trashed");
            add("ems:Added");
            add("ems:Deleted");
            add("ems:Moved");
            add("ems:Updated");
            add("ems:MergeSource");
            add("ems:Committable");
            add("st:site");
            add("st:sites");
        }
    };

    public static class EmsVersion implements Comparator<EmsVersion>, Comparable<EmsVersion> {
        public NodeRef nodeRef = null;
        public NodeRef frozenNodeRef = null;
        public EmsScriptNode emsNode = null;
        public Version version = null;
        public String label = null;
        public Date date = null;

        public EmsVersion(NodeRef nodeRef, NodeRef frozenNodeRef, Version version) {
            super();
            this.nodeRef = nodeRef;
            this.frozenNodeRef = frozenNodeRef;
            this.version = version;
        }

        public NodeRef getNodeRef() {
            return nodeRef;
        }

        public void setNodeRef(NodeRef nodeRef) {
            this.nodeRef = nodeRef;
        }

        public NodeRef getFrozenNodeRef() {
            if (frozenNodeRef == null) {
                if (getVersion() != null) {
                    frozenNodeRef = getVersion().getFrozenStateNodeRef();
                }
            }
            return frozenNodeRef;
        }

        public void setFrozenNodeRef(NodeRef frozenNodeRef) {
            this.frozenNodeRef = frozenNodeRef;
        }

        public EmsScriptNode getEmsNode() {
            if (emsNode == null && getNodeRef() != null) {
                emsNode = new EmsScriptNode(getNodeRef(), NodeUtil.getServices());
            }
            return emsNode;
        }

        public void setEmsNode(EmsScriptNode emsNode) {
            this.emsNode = emsNode;
        }

        public Version getVersion() {
            return version;
        }

        public void setVersion(Version version) {
            this.version = version;
        }

        public String getLabel() {
            if (label == null) {
                if (getVersion() != null) {
                    label = getVersion().getVersionLabel();
                }
            }
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Date getDate() {
            if (date == null) {
                if (getVersion() != null) {
                    date = getVersion().getFrozenModifiedDate();
                }
            }
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        @Override
        public int compareTo(EmsVersion v2) {
            EmsVersion v1 = this;
            if (v1 == v2)
                return 0;
            if (v2 == null)
                return 1;
            Date d1 = v1.getDate();
            Date d2 = v2.getDate();
            if (d1 != null && d2 != null) {
                return d1.compareTo(d2);
            }
            if (v1.getLabel() != null && v2.getLabel() != null) {
                return v1.getLabel().compareTo(v2.getLabel());
            }
            if (v1.getEmsNode() == v1.getEmsNode())
                return 0;
            if (v1.getEmsNode() == null)
                return -1;
            if (v2.getEmsNode() == null)
                return 1;
            return v1.getEmsNode().compareTo(v2.getEmsNode());
        }

        @Override
        public int compare(EmsVersion v1, EmsVersion v2) {
            if (v1 == v2)
                return 0;
            if (v1 == null)
                return -1;
            if (v2 == null)
                return 1;
            return v1.compareTo(v2);
        }
    }

    // Flag to indicate whether we checked the nodeRef version for this script
    // node,
    // when doing getProperty().
    private boolean checkedNodeVersion = false;

    // provide logging capability of what is done
    private StringBuffer response = null;

    // provide status as necessary
    private Status status = null;

    /**
     * whether to use the foundational Alfresco Java API or ScriptNode class that uses the
     * JavaScript API
     */
    public boolean useFoundationalApi = true; // TODO this will be removed

    protected EmsScriptNode companyHome = null;

    protected EmsScriptNode siteNode = null;

    protected EmsScriptNode projectNode = null;
    protected WorkspaceNode workspace = null;
    protected WorkspaceNode parentWorkspace = null;

    /**
     * When writing out JSON, evaluate Expressions and include the results.
     */
    private boolean evaluatingExpressions;

    /**
     * Replicates the behavior of ScriptNode versions, which is private.
     */
    protected Object[] myVersions = null;

    public boolean embeddingExpressionInConstraint = true;
    public boolean embeddingExpressionInOperation = true;
    public boolean embeddingExpressionInConnector = true;

    public AbstractJavaWebScript webscript = null;

    // private boolean forceCacheUpdate = false;

    public static boolean fixOwnedChildren = false;

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

    public EmsScriptNode childByNamePath(String path, boolean ignoreWorkspace, WorkspaceNode workspace,
                    boolean onlyWorkspace) {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !ADMIN_USER_NAME.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(ADMIN_USER_NAME);
        }

        // Make sure this node is in the target workspace.
        EmsScriptNode node = this;
        if (!ignoreWorkspace && workspace != null && !workspace.equals(getWorkspace())) {
            node = findScriptNodeByName(getName(), ignoreWorkspace, workspace, null);
        }
        // See if the path/child is in this workspace.
        EmsScriptNode child = node.childByNamePath(path);
        if (child != null && child.exists()) {
            if (changeUser) {
                AuthenticationUtil.setRunAsUser(runAsUser);
            }
            return child;
        }

        // Find the path/child in a parent workspace if not constraining only to
        // the current workspace:
        if (!onlyWorkspace) {
            EmsScriptNode source = node.getWorkspaceSource();
            while (source != null && source.exists() && (child == null || !child.exists())) {
                child = source.childByNamePath(path);
                source = source.getWorkspaceSource();
            }
            if (child != null && child.exists()) {
                if (changeUser) {
                    AuthenticationUtil.setRunAsUser(runAsUser);
                }
                return child;
            }
        }
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }
        return null;
    }

    public EmsScriptNode getWorkspaceSource() {
        if (!hasAspect("ems:HasWorkspace"))
            return null;
        // ems:source is workspace meta data so dont need dateTime/workspace
        // args
        NodeRef ref = (NodeRef) getNodeRefProperty("ems:source", null, null);
        if (ref != null) {
            return new EmsScriptNode(ref, getServices());
        }
        String msg = "Error! Node has HasWorkspace aspect but no source node!";
        log(msg);
        // Debug.error( msg );
        return null;
    }

    /**
     * Gets the version history
     *
     * This is needed b/c the ScriptNode getVersionHistory() generates a NPE
     *
     * @return version history
     */
    public Object[] getEmsVersionHistory() {

        if (this.myVersions == null && getIsVersioned()) {
            VersionHistory history = this.services.getVersionService().getVersionHistory(this.nodeRef);
            if (history != null) {
                Collection<Version> allVersions = history.getAllVersions();
                Object[] versions = new Object[allVersions.size()];
                int i = 0;
                for (Version version : allVersions) {
                    versions[i++] = new ScriptVersion(version, this.services, this.scope);
                }
                this.myVersions = versions;
            }
        }
        return this.myVersions;

    }

    /**
     * Create a version of this document. Note: this will add the cm:versionable aspect.
     *
     * @param history Version history note
     * @param majorVersion True to save as a major version increment, false for minor version.
     *
     * @return ScriptVersion object representing the newly added version node
     */
    @Override
    public ScriptVersion createVersion(String history, boolean majorVersion) {
        this.myVersions = null;
        // makeSureNodeRefIsNotFrozen();
        transactionCheck();
        return super.createVersion(history, majorVersion);
    }

    /**
     * Check-in a working copy document. The current state of the working copy is copied to the
     * original node, this will include any content updated in the working node. Note that this
     * method can only be called on a working copy Node.
     *
     * @param history Version history note
     * @param majorVersion True to save as a major version increment, false for minor version.
     *
     * @return the original Node that was checked out.
     */
    @Override
    public ScriptNode checkin(String history, boolean majorVersion) {
        this.myVersions = null;
        transactionCheck();
        return super.checkin(history, majorVersion);
    }

    /**
     * Use {@link #childByNamePath(String, WorkspaceNode)} instead.
     *
     * @see org.alfresco.repo.jscript.ScriptNode#childByNamePath(java.lang.String)
     */
    @Override
    public EmsScriptNode childByNamePath(String path) {
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

        childNode.getOrSetCachedVersion();

        return childNode;
    }

    @Override
    public EmsScriptNode createFile(String name) {
        makeSureNodeRefIsNotFrozen();
        transactionCheck();
        EmsScriptNode fileNode = new EmsScriptNode(super.createFile(name).getNodeRef(), services, response, status);

        fileNode.getOrSetCachedVersion();

        return fileNode;
    }

    public Set<EmsScriptNode> getChildNodes() {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !ADMIN_USER_NAME.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(ADMIN_USER_NAME);
        }

        Set<EmsScriptNode> set = new LinkedHashSet<>();
        List<ChildAssociationRef> refs = services.getNodeService().getChildAssocs(nodeRef);
        if (refs != null) {
            // check all associations to see if there's a matching association
            for (ChildAssociationRef ref : refs) {
                if (ref.getParentRef().equals(nodeRef)) {
                    NodeRef child = ref.getChildRef();
                    EmsScriptNode node = new EmsScriptNode(child, getServices());
                    set.add(node);
                }
            }
        }

        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }

        return set;
    }

    // @Override
    // public Scriptable getChildren() {
    // Scriptable myChildren = super.getChildren();
    // //myChildren.
    // //if ( )
    // }

    @Override
    public EmsScriptNode createFolder(String name) {
        return createFolder(name, null);
    }

    @Override
    public EmsScriptNode createFolder(String name, String type) {
        return createFolder(name, type, null);
    }

    public EmsScriptNode createFolder(String name, String type, NodeRef sourceFolder) {
        if (logger.isInfoEnabled()) {
            logger.info("creating " + name + " in " + sourceFolder);
        }

        makeSureNodeRefIsNotFrozen();
        NodeRef folderRef = super.createFolder(name, type).getNodeRef();
        transactionCheck();
        EmsScriptNode folder = new EmsScriptNode(folderRef, services, response, status);
        WorkspaceNode ws = getWorkspace();

        if (ws != null && !folder.isWorkspace()) {
            folder.setWorkspace(ws, sourceFolder);
        }

        return folder;
    }

    /**
     * Check whether or not a node has the specified aspect, add it if not
     *
     * @param string Short name (e.g., sysml:View) of the aspect to look for
     * @return true if node updated with aspect
     */
    public boolean createOrUpdateAspect(String type) {
        if (Acm.getJSON2ACM().keySet().contains(type)) {
            type = Acm.getJSON2ACM().get(type);
        }

        updateBogusProperty(type);

        transactionCheck();

        return changeAspect(type);
    }

    protected void updateBogusProperty(String type) {
        // Make sure the aspect change makes it into the version history by
        // updating a bogus property.
        String bogusPropName = null;
        if (Acm.ASPECTS_WITH_BOGUS_PROPERTY.containsKey(type)) {
            bogusPropName = Acm.ASPECTS_WITH_BOGUS_PROPERTY.get(type);
        }
        if (bogusPropName == null)
            return;
        Random rand = new Random();
        int randNum = rand.nextInt(10000000);
        setProperty(Acm.ASPECTS_WITH_BOGUS_PROPERTY.get(type), randNum);
    }

    /**
     * Check whether an association exists of the specified type between source and target,
     * create/update as necessary TODO: updating associations only works for singular associations,
     * need to expand to multiple NOTE: do not use for child associations
     *
     * @param target Target node of the association
     * @param type Short name of the type of association to create
     * @return true if association updated or created
     */
    public boolean createOrUpdateAssociation(ScriptNode target, String type) {
        return createOrUpdateAssociation(target, type, false);
    }

    public boolean createOrUpdateAssociation(ScriptNode target, String type, boolean isMultiple) {
        QName typeQName = createQName(type);
        makeSureNodeRefIsNotFrozen();
        transactionCheck();
        List<AssociationRef> refs = services.getNodeService().getTargetAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);

        if (refs != null) {
            // check all associations to see if there's a matching association
            for (AssociationRef ref : refs) {
                if (ref.getTypeQName().equals(typeQName)) {
                    if (ref.getSourceRef() != null && ref.getTargetRef() != null) {
                        if (ref.getSourceRef().equals(nodeRef) && ref.getTargetRef().equals(target.getNodeRef())) {
                            // found it, no need to update
                            return false;
                        }
                    }
                    // TODO: need to check for multiple associations?
                    if (!isMultiple) {
                        // association doesn't match, no way to modify a ref, so
                        // need to remove then create
                        // transactionCheck();
                        services.getNodeService().removeAssociation(nodeRef, target.getNodeRef(), typeQName);
                        break;
                    }
                }
            }
        }

        AssociationRef ar = services.getNodeService().createAssociation(nodeRef, target.getNodeRef(), typeQName);
        if (ar == null) {
            return false;
        } else {
            return true;
        }
    }

    public void removeAssociations(String type) {
        QName typeQName = createQName(type);
        makeSureNodeRefIsNotFrozen();
        transactionCheck();
        List<AssociationRef> refs = services.getNodeService().getTargetAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);

        if (refs != null) {
            // check all associations to see if there's a matching association
            for (AssociationRef ref : refs) {
                if (ref.getTypeQName().equals(typeQName)) {
                    services.getNodeService().removeAssociation(ref.getSourceRef(), ref.getTargetRef(), typeQName);
                }
            }
        }
    }

    /**
     * Create a child association between a parent and child node of the specified type
     *
     * // TODO investigate why alfresco repo deletion of node doesn't remove its reified package
     *
     * NOTE: do not use for peer associations
     *
     * @param child Child node
     * @param type Short name of the type of child association to create
     * @return True if updated or created child relationship
     */
    public boolean createOrUpdateChildAssociation(ScriptNode child, String type) {
        List<ChildAssociationRef> refs = services.getNodeService().getChildAssocs(nodeRef);
        QName typeQName = createQName(type);

        makeSureNodeRefIsNotFrozen();
        transactionCheck();

        if (refs != null) {
            // check all associations to see if there's a matching association
            for (ChildAssociationRef ref : refs) {
                if (ref.getTypeQName().equals(typeQName)) {
                    if (ref.getParentRef().equals(nodeRef) && ref.getChildRef().equals(child.getNodeRef())) {
                        // found it, no need to update
                        return false;
                    } else {
                        services.getNodeService().removeChildAssociation(ref);
                        break;
                    }
                }
            }
        }

        services.getNodeService().addChild(nodeRef, child.getNodeRef(), typeQName, typeQName);
        return true;
    }

    /**
     * Check whether or not a node has a property, update or create as necessary
     *
     * NOTE: this only works for non-collection properties - for collections handwrite (or see how
     * it's done in ModelPost.java)
     *
     * @param acmType Short name for the Alfresco Content Model type
     * @param value Value to set property to
     * @return true if property updated, false otherwise (e.g., value did not change)
     */
    public <T extends Serializable> boolean createOrUpdateProperty(String acmType, T value) {
        if (value instanceof String) {
            @SuppressWarnings("unchecked")
            T t = (T) extractAndReplaceImageData((String) value, getWorkspace());
            t = (T) XrefConverter.convertXref((String) t);
            value = t;
        }
        @SuppressWarnings("unchecked")
        // It is important we ignore the workspace when getting the property, so
        // we make sure
        // to update this property when needed. Otherwise, property may have a
        // noderef in
        // a parent workspace, and this wont detect it; however, all the
        // getProperty() will look
        // for the correct workspace node, so perhaps this is overkill:
        T oldValue = (T) getNodeRefProperty(acmType, true, null, false, true, null);
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
                setProperty(Acm.ACM_LAST_MODIFIED, new Date(), false, 0);
            }
            if (!changed) {
                logger.warn("Failed to set property for new value in createOrUpdateProperty(" + acmType + ", " + value
                                + ")");
            }
            return changed;
        }

        return false;
    }

    public EmsScriptNode getCompanyHome() {
        if (companyHome == null) {
            companyHome = NodeUtil.getCompanyHome(services);
        }
        return companyHome;
    }

    public Set<NodeRef> getRootNodes() {
        return NodeUtil.getRootNodes(services);
    }

    public static String getMimeType(String type) {
        Field[] fields = ClassUtils.getAllFields(MimetypeMap.class);
        for (Field f : fields) {
            if (f.getName().startsWith("MIMETYPE")) {
                if (ClassUtils.isStatic(f) && f.getName().substring(8).toLowerCase().contains(type.toLowerCase())) {
                    try {
                        return (String) f.get(null);
                    } catch (IllegalArgumentException e) {
                    } catch (IllegalAccessException e) {
                    }
                }
            }
        }
        return null;
    }

    public static long getChecksum(String dataString) {
        byte[] data = null;
        data = dataString.getBytes(); // ( "UTF-8" );
        return getChecksum(data);
    }

    public static long getChecksum(byte[] data) {
        long cs = 0;
        Checksum checksum = new CRC32();
        checksum.update(data, 0, data.length);
        cs = checksum.getValue();
        return cs;
    }

    public List<EmsScriptNode> toEmsScriptNodeList(ArrayList<NodeRef> resultSet) {
        return toEmsScriptNodeList(resultSet, services, response, status);
    }

    public static List<EmsScriptNode> toEmsScriptNodeList(ArrayList<NodeRef> resultSet,
                    // Date dateTime,
                    ServiceRegistry services, StringBuffer response, Status status) {

        ArrayList<EmsScriptNode> emsNodeList = new ArrayList<>();
        if (resultSet != null)
            for (NodeRef ref : resultSet) {
                // NodeRef ref = row.getNodeRef();
                if (ref == null)
                    continue;
                EmsScriptNode node = new EmsScriptNode(ref, services, response, status);
                if (!node.exists())
                    continue;
                emsNodeList.add(node);
            }
        return emsNodeList;
    }

    public EmsScriptNode findOrCreateArtifact(String name, String type, String base64content, String targetSiteName,
                    String subfolderName, WorkspaceNode workspace, Date dateTime) {

        return NodeUtil.updateOrCreateArtifact(name, type, base64content, null, targetSiteName, subfolderName,
                        workspace, dateTime, response, status, false);
    }

    public String extractAndReplaceImageData(String value, WorkspaceNode ws) {
        if (value == null)
            return null;
        String v = value;
        Pattern p = Pattern.compile(
                        "(.*)<img[^>]*\\ssrc\\s*=\\s*[\"']data:image/([^;]*);\\s*base64\\s*,([^\"']*)[\"'][^>]*>(.*)",
                        Pattern.DOTALL);
        while (true) {
            Matcher m = p.matcher(v);
            if (!m.matches()) {
                if (Debug.isOn()) {
                    Debug.outln("no match found for v=" + v.substring(0, Math.min(v.length(), 100))
                                    + (v.length() > 100 ? " . . ." : "") + ")");
                }
                break;
            } else {
                if (Debug.isOn()) {
                    Debug.outln("match found for v=" + v.substring(0, Math.min(v.length(), 100))
                                    + (v.length() > 100 ? " . . ." : "") + ")");
                }
                if (m.groupCount() != 4) {
                    log("Expected 4 match groups, got " + m.groupCount() + "! " + m);
                    break;
                }
                String extension = m.group(2);
                String content = m.group(3);
                String name = "img_" + System.currentTimeMillis();

                // No need to pass a date since this is called in the context of
                // updating a node, so the time is the current time (which is
                // null).
                EmsScriptNode artNode = findOrCreateArtifact(name, extension, content, getSiteName(null, ws), "images",
                                ws, null);
                if (artNode == null || !artNode.exists()) {
                    log("Failed to pull out image data for value! " + value);
                    break;
                }

                String url = artNode.getUrl();
                String link = "<img src=\"" + url + "\"/>";
                link = link.replace("/d/d/", "/alfresco/service/api/node/content/");
                v = m.group(1) + link + m.group(4);
            }
        }
        // Debug.turnOff();
        return v;
    }

    public String getSiteTitle(Date dateTime, WorkspaceNode ws) {
        EmsScriptNode siteNode = getSiteNode(dateTime, ws);
        return (String) siteNode.getProperty(Acm.CM_TITLE);
    }

    public String getSiteName(Date dateTime, WorkspaceNode ws) {
        if (siteName == null) {
            if (dateTime != null || ws != null) {
                EmsScriptNode siteNode = getSiteNode(dateTime, ws);
                if (siteNode != null)
                    siteName = siteNode.getName();
            } else {
                // FIXME: need to get back to the above call at some point, but
                // currently it
                // would require a permission check on every property
                // Weird issues with permissions, lets just get site from
                // display path
                // we don't track changes if they move sites...
                String displayPath = getDisplayPath();
                boolean sitesFound = false;
                for (String path : displayPath.split("/")) {
                    if (path.equals("Sites")) {
                        sitesFound = true;
                    } else if (sitesFound) {
                        siteName = path;
                        break;
                    }
                }
            }

        }
        return siteName;
    }

    /**
     * Utility to compare lists of node refs to one another
     *
     * @param x First list to compare
     * @param y Second list to compare
     * @return true if same, false otherwise
     */
    public static <T extends Serializable> boolean checkIfListsEquivalent(List<T> x, List<T> y) {
        if (x == null || y == null) {
            return false;
        }
        if (x.size() != y.size()) {
            return false;
        }
        for (int ii = 0; ii < x.size(); ii++) {
            T xVal = x.get(ii);
            T yVal = y.get(ii);
            if (xVal != null && !xVal.equals(yVal)) {
                return false;
            }
            if (yVal != null && !yVal.equals(xVal)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param parent - this could be the reified package or the reified node
     * @param nestedNode Set to true if this is a nested node, ie embedded value spec
     */
    public EmsScriptNode setOwnerToReifiedNode(EmsScriptNode parent, WorkspaceNode ws, boolean nestedNode) {
        // everything is created in a reified package, so need to make
        // relations to the reified node rather than the package
        EmsScriptNode reifiedNode = parent.getReifiedNode(ws);
        if (reifiedNode == null)
            reifiedNode = parent; // just in case
        if (reifiedNode != null) {
            EmsScriptNode nodeInWs = NodeUtil.findScriptNodeById(reifiedNode.getSysmlId(), ws, null, false,
                            getServices(), getResponse());
            if (nodeInWs != null)
                reifiedNode = nodeInWs;
            // store owner with created node
            this.createOrUpdateAspect("ems:Owned");
            this.createOrUpdateProperty("ems:owner", reifiedNode.getNodeRef());
            if (nestedNode) {
                this.createOrUpdateAspect("ems:ValueSpecOwned");
                this.createOrUpdateProperty("ems:valueSpecOwner", reifiedNode.getNodeRef());
            }

            // add child to the parent as necessary
            reifiedNode.createOrUpdateAspect("ems:Owned");
            reifiedNode.appendToPropertyNodeRefs("ems:ownedChildren", this.getNodeRef());
            if (nestedNode) {
                reifiedNode.createOrUpdateAspect("ems:ValueSpecOwned");
                reifiedNode.appendToPropertyNodeRefs("ems:valueSpecOwnedChildren", this.getNodeRef());
            }
        }
        return reifiedNode;
    }


    public EmsScriptNode getReifiedNode(boolean findDeleted, WorkspaceNode ws, Date dateTime) {
        NodeRef nodeRef = (NodeRef) getNodeRefProperty("ems:reifiedNode", false, dateTime, findDeleted, false, ws);
        if (nodeRef != null) {
            return new EmsScriptNode(nodeRef, services, response);
        }
        return null;
    }

    public EmsScriptNode getReifiedNode(boolean findDeleted, WorkspaceNode ws) {
        return getReifiedNode(findDeleted, ws, null);
    }

    public EmsScriptNode getReifiedNode(WorkspaceNode ws) {
        return getReifiedNode(false, ws);
    }

    public EmsScriptNode getReifiedPkg(Date dateTime, WorkspaceNode ws) {
        return getReifiedPkg(dateTime, ws, true);
    }

    public EmsScriptNode getReifiedPkg(Date dateTime, WorkspaceNode ws, boolean findDeleted) {
        NodeRef nodeRef = (NodeRef) getNodeRefProperty("ems:reifiedPkg", false, dateTime, findDeleted, false, ws);
        if (nodeRef != null) {
            return new EmsScriptNode(nodeRef, services, response);
        }
        return null;
    }

    /**
     * Override createNode to return an EmsScriptNode
     *
     * @param name cm:name of node (which may also be the sysml:id)
     * @param type Alfresco Content Model type of node to create
     * @return created child EmsScriptNode
     */
    @Override
    public EmsScriptNode createNode(String name, String type) {
        // NodeRef nr = findNodeRefByType( name, SearchType.CM_NAME.prefix,
        // true,
        // workspace, null, false );
        //
        // EmsScriptNode n = new EmsScriptNode( nr, getServices() );
        // if ( !n.checkPermissions( PermissionService.ADD_CHILDREN,
        // getResponse(),
        // getStatus() ) ) {
        // log( "No permissions to add children to " + n.getName() );
        // return null;
        // }

        // System.out.println("createNode(" + name + ", " + type + ")\n" );// +
        // Debug.stackTrace() );

        EmsScriptNode result = null;
        // Date start = new Date(), end;

        // if ( type == null ) {
        // type = "sysml:Element";
        // }
        if (!useFoundationalApi) {
            makeSureNodeRefIsNotFrozen();
            ScriptNode scriptNode = super.createNode(name, type);
            transactionCheck();
            result = new EmsScriptNode(scriptNode.getNodeRef(), services, response);
        } else {
            Map<QName, Serializable> props = new HashMap<>(1, 1.0f);
            // don't forget to set the name
            props.put(ContentModel.PROP_NAME, name);

            QName typeQName = createQName(type);
            if (typeQName != null) {
                try {
                    makeSureNodeRefIsNotFrozen();
                    transactionCheck();
                    ChildAssociationRef assoc =
                                    services.getNodeService().createNode(nodeRef, ContentModel.ASSOC_CONTAINS,
                                                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
                                                                    QName.createValidLocalName(name)),
                                                    createQName(type), props);
                    result = new EmsScriptNode(assoc.getChildRef(), services, response);
                } catch (Exception e) {
                    logger.error("Got exception in " + "createNode(name=" + name + ", type=" + type
                                    + ") for EmsScriptNode(" + this + ") calling createNode(nodeRef=" + nodeRef
                                    + ", . . .)");
                    e.printStackTrace();
                }

            } else {
                log("Could not find type " + type);
            }
        }

        // Set the workspace to be the same as this one's.
        // WARNING! The parent must already be replicated in the specified
        // workspace.
        if (result != null) {
            WorkspaceNode parentWs = getWorkspace();
            if (parentWs != null && !result.isWorkspace()) {
                result.setWorkspace(parentWs, null);
            }
        }

        // end = new Date(); if (Debug.isOn())
        // System.out.println("\tcreateNode: " +
        // (end.getTime()-start.getTime()));
        return result;
    }

    public void makeSureNodeRefIsNotFrozen() {
        if (!NodeUtil.doHeisenCheck && !NodeUtil.doVersionCaching) {
            return;
        }
        NodeRef liveNodeRef = getLiveNodeRefFromVersion();
        if (liveNodeRef != null && !liveNodeRef.equals(nodeRef)) {
            EmsScriptNode liveNode = new EmsScriptNode(liveNodeRef, getServices());
            if (isAVersion()) {
                logger.error("Trying to create a node under a frozen node ref (" + nodeRef + ", v " + getVersionLabel()
                                + ")! Replacing nodeRef with (v " + liveNode.getVersionLabel() + ") live node ref ("
                                + liveNodeRef + "), which may not point to the right version! " + this);
                Debug.error(true, "Stacktrace for frozen node replacement:");
                nodeRef = liveNodeRef;
            } else {
                logger.error("Live node " + liveNode.getVersionLabel() + " is different from current "
                                + getVersionLabel() + " node ref!" + this);
            }
        }
    }

    public List<Object> getValuesFromScriptable(Scriptable values) {
        NodeValueConverter nvc = new NodeValueConverter();
        Object res = nvc.convertValueForRepo((Serializable) values);
        if (res instanceof List) {
            return (List<Object>) res;
        }
        return null;
    }

    /**
     * Return the first AssociationRef of a particular type
     *
     * @param type Short name for type to filter on
     * @return
     */
    public EmsScriptNode getFirstSourceAssociationByType(String type) {
        List<AssociationRef> assocs = services.getNodeService().getSourceAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);
        if (assocs != null) {
            // check all associations to see if there's a matching association
            for (AssociationRef ref : assocs) {
                if (ref.getTypeQName().equals(createQName(type))) {
                    return new EmsScriptNode(ref.getSourceRef(), services, response);
                }
            }
        }
        return null;
    }

    /**
     * Return the first AssociationRef of a particular type
     *
     * @param type Short name for type to filter on
     * @return
     */
    public EmsScriptNode getFirstTargetAssociationByType(String type) {
        List<AssociationRef> assocs = services.getNodeService().getTargetAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);
        if (assocs != null) {
            // check all associations to see if there's a matching association
            for (AssociationRef ref : assocs) {
                if (ref.getTypeQName().equals(createQName(type))) {
                    return new EmsScriptNode(ref.getTargetRef(), services, response);
                }
            }
        }
        return null;
    }

    /**
     * Get list of ChildAssociationRefs
     *
     * @return
     */
    public List<ChildAssociationRef> getChildAssociationRefs() {
        return services.getNodeService().getChildAssocs(nodeRef);
    }

    @Override
    public String getName() {
        super.getName();
        return (String) getProperty(Acm.CM_NAME);
    }

    public String getSysmlName() {
        return (String) getProperty(Acm.ACM_NAME);
    }

    public String getSysmlName(Date dateTime) {
        EmsScriptNode esn = this;
        if (dateTime != null) {
            NodeRef ref = NodeUtil.getNodeRefAtTime(getNodeRef(), dateTime);
            esn = new EmsScriptNode(ref, getServices());
        }
        return esn.getSysmlName();
    }

    public String getSysmlId() {
        String id = (String) getProperty(Acm.ACM_ID);
        if (id == null) {
            id = getName();
        }
        return id;
    }

    @Override
    public EmsScriptNode getParent() {
        ScriptNode myParent = super.getParent();
        if (myParent == null)
            return null;
        return new EmsScriptNode(myParent.getNodeRef(), services, response);
    }

    /**
     * This version of getParent() handles versioned nodes correctly by calling getOwningParent()
     * first.
     *
     * @param dateTime
     * @param ws
     * @param skipNodeRefCheck
     * @param checkVersionedNode
     * @return
     */
    public EmsScriptNode getParent(Date dateTime, WorkspaceNode ws, boolean skipNodeRefCheck,
                    boolean checkVersionedNode) {

        // We are not using getParent() because elementNode may be from the
        // version
        // store, which makes getParent() return a node from the
        // workspace://version2store,
        // and those nodes are equivalent to death. See CMED-702.
        EmsScriptNode owningParent = getOwningParent(dateTime, ws, skipNodeRefCheck, checkVersionedNode);

        if (owningParent != null) {
            EmsScriptNode parent = owningParent.getReifiedPkg(dateTime, ws);
            if (parent != null) {
                return parent;
            }
        }

        return getParent();
    }

    /**
     * Return the version of the parent at a specific time. This uses the ownerType property instead
     * of getParent() when it returns non-null; else, it call getParent(). For workspaces, the
     * parent should always be in the same workspace, so there is no need to specify (or use) the
     * workspace.
     *
     * @param dateTime
     * @return the parent/owning node
     */
    private EmsScriptNode getOwningParentImpl(String ownerType, Date dateTime, WorkspaceNode ws,
                    boolean skipNodeRefCheck, boolean checkVersionedNode) {

        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !EmsScriptNode.ADMIN_USER_NAME.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(EmsScriptNode.ADMIN_USER_NAME);
        }

        EmsScriptNode node = null;
        NodeRef ref = (NodeRef) getNodeRefProperty(ownerType, false, dateTime, true, skipNodeRefCheck, ws);

        if (ref == null) {
            node = getParent();
            if (!skipNodeRefCheck && dateTime != null && node != null) {
                NodeRef vref = NodeUtil.getNodeRefAtTime(node.getNodeRef(), dateTime);
                if (vref != null) {
                    node = new EmsScriptNode(vref, getServices());
                } else {
                    // Don't want the reified package at the wrong time; null is
                    // correct.
                    node = null;
                }
            }
        } else {
            node = new EmsScriptNode(ref, getServices());
        }

        if (node == null)
            return null;

        // FIXME this seraches below are not always going to return nodes from
        // the
        // SpaceStore

        // If its a version node, then it doesnt have a parent association, so
        // if it does
        // not have a owner, then we should return the non-versioned current
        // node:
        if (checkVersionedNode && !skipNodeRefCheck) {
            if (node.isAVersion() && node.getNodeRefProperty(ownerType, dateTime, ws) == null) {

                logger.warn("getOwningParent: The node " + node
                                + " is a versioned node and doesn't have a owner.  Returning the current node instead.");

                NodeRef pooRef = findNodeRefByType(node.getName(), SearchType.CM_NAME.prefix, ws, null, false);

                if (pooRef != null) {
                    node = new EmsScriptNode(pooRef, getServices());
                } else {
                    // Must do a find with dateTime as null b/c none of the
                    // other alfresco methods
                    // work for nodes in workspace://version2store
                    // Note: parents from nodes in versionStore://version2store
                    // are in
                    // workspace://version2store
                    NodeRef currentRef = findNodeRefByType(getName(), SearchType.CM_NAME.prefix, ws, null, false);
                    if (currentRef != null) {
                        EmsScriptNode currentNode = new EmsScriptNode(currentRef, getServices());

                        if (!this.equals(currentNode, false)) {
                            node = currentNode.getOwningParent(dateTime, ws, skipNodeRefCheck, checkVersionedNode);
                        }
                    }
                }

            }
        }
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }

        return node;
    }

    public EmsScriptNode getOwningParent(Date dateTime, WorkspaceNode ws, boolean skipNodeRefCheck) {
        return getOwningParent(dateTime, ws, skipNodeRefCheck, true);
    }

    public EmsScriptNode getValueSpecOwningParent(Date dateTime, WorkspaceNode ws, boolean skipNodeRefCheck) {
        return getValueSpecOwningParent(dateTime, ws, skipNodeRefCheck, true);
    }

    public boolean isAVersion() {

        VersionService vs = services.getVersionService();
        return vs.isAVersion(nodeRef) || nodeRef.getStoreRef().getIdentifier().equals(Version2Model.STORE_ID);

    }

    /**
     * Return the version of the parent at a specific time. This uses the ems:owner property instead
     * of getParent() when it returns non-null; else, it call getParent().
     *
     * @param dateTime
     * @return the parent/owning node
     */
    public EmsScriptNode getOwningParent(Date dateTime, WorkspaceNode ws, boolean skipNodeRefCheck,
                    boolean checkVersionedNode) {

        return getOwningParentImpl("ems:owner", dateTime, ws, skipNodeRefCheck, checkVersionedNode);
    }

    /**
     * Return the version of the parent at a specific time. This uses the ems:valueSpecOwner
     * property instead of getParent() when it returns non-null; else, it call getParent(). For
     * workspaces, the parent should always be in the same workspace, so there is no need to specify
     * (or use) the workspace.
     *
     * @param dateTime
     * @return the parent/owning node
     */
    public EmsScriptNode getValueSpecOwningParent(Date dateTime, WorkspaceNode ws, boolean skipNodeRefCheck,
                    boolean checkVersionedNode) {
        return getOwningParentImpl("ems:valueSpecOwner", dateTime, ws, skipNodeRefCheck, checkVersionedNode);
    }

    /**
     * Returns the children for this node. Uses the childrenType property.
     *
     * @param findDeleted Find deleted nodes also
     * @return children of this node
     */
    private ArrayList<NodeRef> getOwnedChildrenImpl(String childrenType, boolean findDeleted, Date dateTime,
                    WorkspaceNode ws) {

        ArrayList<NodeRef> ownedChildren = null;

        ArrayList<NodeRef> oldChildren = null;

        // FIXME: use DB, for some DB is slow
        // if (!(NodeUtil.doGraphDb && dateTime == null && ws == null)) {
        oldChildren = this.getPropertyNodeRefs(childrenType, false, dateTime, findDeleted, false, ws);
        // } else {
        // PostgresHelper pgh = new PostgresHelper( ws );
        // try {
        // pgh.connect();
        // List< Pair< String, Pair<String, String>>> dbChildren = pgh.getChildren(
        // this.getSysmlId(), DbEdgeTypes.REGULAR, 1 );
        // oldChildren = new ArrayList< NodeRef >();
        // for (int ii = 0; ii < dbChildren.size(); ii++) {
        // if (!dbChildren.get(ii).first.equals( this.getSysmlId() )) {
        // String childNodeRefString = dbChildren.get( ii ).second.first;
        // oldChildren.add( new NodeRef(childNodeRefString) );
        // }
        // }
        // pgh.close();
        // } catch ( ClassNotFoundException e ) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch ( SQLException e ) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // }

        if (oldChildren != null) {
            ownedChildren = oldChildren;
        } else {
            ownedChildren = new ArrayList<>();
        }

        return ownedChildren;
    }

    /**
     * Returns the children for this node. Uses the ems:ownedChildren property.
     *
     * @param findDeleted Find deleted nodes also
     * @return children of this node
     */
    public ArrayList<NodeRef> getOwnedChildren(boolean findDeleted, Date dateTime, WorkspaceNode ws) {
        return getOwnedChildrenImpl("ems:ownedChildren", findDeleted, dateTime, ws);
    }

    /**
     * Returns all of the "connected" nodes. If relationshipType is null, then will return all of
     * the nodes that this node refers to in properties. Otherwise, only returns nodes that are in
     * the passed relationshipType and are of the passed relationshipType, ie a DirectedRelationship
     * and its target, source, and owner nodes.
     *
     * @param dateTime
     * @param ws
     * @param relationshipType The desired relationship to use for filtering, ie
     *        "DirectedRelationship"
     * @return
     */
    public ArrayList<NodeRef> getConnectedNodes(Date dateTime, WorkspaceNode ws, String relationshipType) {

        // REVIEW
        // This currently does a crude job of filtering, as it will include all
        // properties of
        // the desired relationship that point to node refs, including the owner
        // and relationship
        // node itself. This may not be what is desired.
        //
        // Also, may want a way to get all relationships, not just the one type
        // specified.

        // TODO eventually will want to put some of this in
        // EmsSystemModel.getRelationship(), but
        // currently there is no dateTime, unless we use the specifier

        ArrayList<NodeRef> nodes = new ArrayList<>();

        String relationshipTypeName = relationshipType;
        if (Acm.getJSON2ACM().containsKey(relationshipType)) {
            relationshipTypeName = Acm.getJSON2ACM().get(relationshipType);
        }
        boolean checkingRelationship = !Utils.isNullOrEmpty(relationshipTypeName);
        boolean nodeHasRelationship = checkingRelationship ? this.hasAspect(relationshipTypeName) : false;

        // Loop through all of the properties of the node:
        for (Entry<String, Object> entry : this.getNodeRefProperties(dateTime, ws).entrySet()) {

            String keyShort = NodeUtil.getShortQName(NodeUtil.createQName(entry.getKey()));
            Object value = entry.getValue();

            // If a relationshipType was provided, then see if this property
            // points to a relationship:
            boolean addNode = true;
            if (checkingRelationship && !nodeHasRelationship) {
                addNode = false;
                for (String relationshipProp : Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.values()) {
                    if (relationshipProp.equals(keyShort)) {
                        addNode = true;
                        break;
                    }
                }
            }

            // Add the properties that point to node refs, and filter by
            // relationship if needed:
            if (addNode) {
                if (value instanceof NodeRef) {
                    NodeRef ref = (NodeRef) value;
                    if (checkingRelationship) {
                        EmsScriptNode node = new EmsScriptNode(ref, services);
                        if (nodeHasRelationship || node.hasAspect(relationshipTypeName)) {
                            nodes.add(ref);
                        }
                    } else {
                        nodes.add(ref);
                    }
                } else if (value instanceof List) {
                    for (Object obj : (List) value) {
                        if (obj instanceof NodeRef) {
                            NodeRef ref = (NodeRef) obj;
                            if (checkingRelationship) {
                                EmsScriptNode node = new EmsScriptNode(ref, services);
                                if (nodeHasRelationship || node.hasAspect(relationshipTypeName)) {
                                    nodes.add(ref);
                                }
                            } else {
                                nodes.add(ref);
                            }
                        }
                    }
                }
            }
        }

        return nodes;
    }

    /**
     * Returns the value spec children for this node. Uses the ems:valueSpecOwnedChildren property.
     *
     * @param findDeleted Find deleted nodes also
     * @return children of this node
     */
    public ArrayList<NodeRef> getValueSpecOwnedChildren(boolean findDeleted, Date dateTime, WorkspaceNode ws) {
        return getOwnedChildrenImpl("ems:valueSpecOwnedChildren", findDeleted, dateTime, ws);
    }

    private EmsScriptNode getUnreifiedParentImpl(Date dateTime, boolean valueSpecOwner, WorkspaceNode ws) {

        EmsScriptNode parent = valueSpecOwner ? getValueSpecOwningParent(dateTime, ws, false)
                        : getOwningParent(dateTime, ws, false);
        if (parent != null) {
            parent = parent.getUnreified(dateTime);
        }
        return parent;
    }

    public EmsScriptNode getUnreifiedParent(Date dateTime, WorkspaceNode ws) {
        return getUnreifiedParentImpl(dateTime, false, ws);
    }

    public EmsScriptNode getUnreifiedValueSpecParent(Date dateTime, WorkspaceNode ws) {
        return getUnreifiedParentImpl(dateTime, true, ws);
    }

    public EmsScriptNode getUnreified(Date dateTime) {
        if (!isReified())
            return this;
        String sysmlId = getSysmlId();
        sysmlId = sysmlId.replaceAll("^(.*)_pkg$", "$1");
        EmsScriptNode unreified = findScriptNodeByName(sysmlId, false, getWorkspace(), dateTime);
        return unreified;
    }

    public boolean isReified() {
        String sysmlId = getSysmlId();
        if (isFolder() && sysmlId != null && sysmlId.endsWith("_pkg")) {
            return true;
        }
        return false;
    }

    public String getVersionLabel() {
        Version v = getCurrentVersion();
        if (v != null) {
            return v.getVersionLabel();
        }
        return null;
    }

    public NodeRef getLiveNodeRefFromVersion() {
        if (!isAVersion())
            return nodeRef;
        Version v = getCurrentVersion();
        if (v != null) {
            NodeRef liveRef = v.getVersionedNodeRef();
            if (liveRef != null) {
                return liveRef;
            }
        }
        NodeRef ref = NodeUtil.getCurrentNodeRefFromCache(nodeRef);
        if (ref != null)
            return ref;
        // Logger.error("");
        return nodeRef;
    }

    protected boolean updateFrozenCache(Version currentVersion) {
        if (currentVersion != null) {
            NodeRef frozenRef = currentVersion.getFrozenStateNodeRef();
            if (!this.isAVersion()) {
                NodeUtil.frozenNodeCache.put(frozenRef, nodeRef);
                return true;
            }
            NodeRef versionedRef = currentVersion.getVersionedNodeRef();
            if (versionedRef != null) {
                NodeUtil.frozenNodeCache.put(frozenRef, versionedRef);
                return true;
            }
        }
        return false;
    }

    public Version getCurrentVersion() {
        VersionService versionService = services.getVersionService();

        Version currentVersion = null;
        if (versionService != null) {
            try {
                currentVersion = versionService.getCurrentVersion(nodeRef);
                updateFrozenCache(currentVersion);
            } catch (Throwable t1) {
                try {
                    currentVersion = versionService.getCurrentVersion(nodeRef);
                    updateFrozenCache(currentVersion);
                } catch (Throwable t2) {
                    logger.error("1. Got exception in getCurrentVersion(): " + t1.getLocalizedMessage());
                    t1.printStackTrace();
                    logger.error("2. Tried again and got another exception in getCurrentVersion(): "
                                    + t2.getLocalizedMessage());
                    t2.printStackTrace();
                }
            }
        }
        return currentVersion;
    }

    public boolean checkNodeRefVersion2(Date dateTime) {

        // Because of a alfresco bug, we must verify that we are getting the
        // latest version
        // of the nodeRef if not specifying a dateTime:
        if (checkedNodeVersion || dateTime != null)
            return false; // ||
                          // isAVersion())
                          // return
                          // false;

        String id = getId();
        NodeRef nr = NodeUtil.heisenCacheGet(id);
        if (nr != null) {
            if (nr.equals(nodeRef))
                return false;
            nodeRef = nr;
            return true;
        }

        // Not in cache -- need to compute
        boolean changed = checkNodeRefVersion(null);
        NodeUtil.heisenCachePut(id, nodeRef);

        return changed;
    }

    /**
     * Verifies that the nodeRef is the most recent if dateTime is null and not already checked for
     * this node. Replaces the nodeRef with the most recent if needed. This is needed b/c of a
     * alfresco bug.
     */
    public boolean checkNodeRefVersion(Date dateTime) {

        // Because of a alfresco bug, we must verify that we are getting the
        // latest version
        // of the nodeRef if not specifying a dateTime:
        if (dateTime == null && !checkedNodeVersion && !isAVersion()) {

            checkedNodeVersion = true;
            Version currentVersion = getCurrentVersion();
            Version headVersion = getHeadVersion();

            if (currentVersion != null && headVersion != null) {

                String currentVerLabel = currentVersion.getVersionLabel();
                String headVerLabel = headVersion.getVersionLabel();

                // If this is not the most current node ref, replace it with the
                // most current:
                if (currentVerLabel != null && headVerLabel != null && !currentVerLabel.equals(headVerLabel)) {

                    NodeRef fnr = headVersion.getFrozenStateNodeRef();
                    if (fnr != null) {
                        // Cache is correct -- fix esn's nodeRef
                        String msg = "Warning! Alfresco Heisenbug returning wrong current version of node, " + this
                                        + ".  Replacing node with unmodifiable versioned node, " + getId() + ".";
                        logger.warn(msg);
                        if (response != null) {
                            response.append(msg + "\n");
                        }
                        nodeRef = fnr;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void transactionCheck() {
        NodeUtil.transactionCheck(logger, this);
    }

    public boolean getOrSetCachedVersion() {
        // transactionCheck();
        if (versionCacheDebugPrint)
            System.out.println("0: getOrSetCachedVersion(): " + this + " :: " + this.getId());
        if (!NodeUtil.doVersionCaching || isAVersion()) {
            if (versionCacheDebugPrint)
                System.out.println("1: N/A " + this.getName());
            return false;
        }
        // if ( checkedNodeVersion ) {
        // System.out.println("2");
        // return false;
        // }
        // checkedNodeVersion = true;
        String id = getId();
        EmsVersion cachedVersion = NodeUtil.versionCache.get(id);
        Version thisVersion = getCurrentVersion();
        EmsVersion thisEmsVersion = null;
        if (thisVersion != null) {
            thisEmsVersion = new EmsVersion(nodeRef, null, thisVersion);
        }

        if (cachedVersion == null) {
            if (thisVersion == null) {
                if (versionCacheDebugPrint)
                    System.out.println("2: no version");
                return false;
            }
            if (optimisticAndFoolish) {
                NodeUtil.versionCache.put(id, thisEmsVersion);
                if (versionCacheDebugPrint)
                    System.out.println("9: optimisticAndFoolish");
            } else {
                cachedVersion = new EmsVersion(nodeRef, null, getHeadVersion());
                NodeUtil.versionCache.put(id, cachedVersion);
                String msg = "3: initializing version cache with node, " + this + " version: "
                                + cachedVersion.getLabel();
                if (logger.isInfoEnabled())
                    logger.info(msg);
                if (versionCacheDebugPrint)
                    System.out.println(msg);

            }
        }
        if (cachedVersion == null) {
            return false;
        }
        if (thisEmsVersion == null) {
            String msg = "6: Warning! Alfresco Heisenbug failing to return current version of node " + this.getNodeRef()
                            + ".  Replacing node with unmodifiable frozen node, " + cachedVersion.getLabel() + ".";
            logger.error(msg);
            // Debug.error( true, msg );
            // sendNotificationEvent( "Heisenbug Occurence!", "" );
            if (response != null) {
                response.append(msg + "\n");
            }
            nodeRef = cachedVersion.getFrozenNodeRef();
            if (tryToFlushCache)
                NodeUtil.clearAlfrescoNodeCache();
            return true;
        }
        int comp = thisEmsVersion.compareTo(cachedVersion);
        if (comp == 0) {
            if (versionCacheDebugPrint)
                System.out.println("3: same version " + thisEmsVersion.getLabel());
            return false;
        }
        if (comp < 0) {
            logger.error("inTransaction = " + NodeUtil.isInsideTransactionNow());
            logger.error("haveBeenInTransaction = " + NodeUtil.isInsideTransactionNow());
            logger.error("haveBeenOutsideTransaction = " + NodeUtil.isInsideTransactionNow());
            // Cache is correct -- fix esn's nodeRef
            String msg = "4: Warning! Alfresco Heisenbug returning wrong current version of node, " + this + " ("
                            + thisEmsVersion.getLabel() + ").  Replacing node with unmodifiable frozen node, " + getId()
                            + " (" + cachedVersion.getLabel() + ").";
            logger.error(msg);
            Debug.error(true, msg);
            // NodeUtil.sendNotificationEvent( "Heisenbug Occurrence!", "" );
            if (response != null) {
                response.append(msg + "\n");
            }
            nodeRef = cachedVersion.getFrozenNodeRef();
            if (tryToFlushCache)
                NodeUtil.clearAlfrescoNodeCache();
        } else { // comp > 0
            // Cache is incorrect -- update cache
            NodeUtil.versionCache.put(id, thisEmsVersion);
            String msg = "5: Updating version cache with new version of node, " + this + " version: "
                            + thisEmsVersion.getLabel();
            if (logger.isInfoEnabled())
                logger.info(msg);
            if (versionCacheDebugPrint)
                System.out.println(msg);
        }
        // // This fixes the nodeRef in esn
        // esn.checkNodeRefVersion( null );
        return true;
    }

    public Object getNodeRefProperty(String acmType, Date dateTime, WorkspaceNode ws) {
        return getNodeRefProperty(acmType, false, dateTime, ws);
    }

    public Object getNodeRefProperty(String acmType, boolean skipNodeRefCheck, Date dateTime, WorkspaceNode ws) {
        return getNodeRefProperty(acmType, false, dateTime, false, skipNodeRefCheck, ws);
    }

    /**
     * Getting a noderef property needs to be contextualized by the workspace and time This works
     * for any property type noderef or otherwise, so use this if you want to be safe.
     *
     * @param acmType
     * @param ignoreWorkspace
     * @param dateTime
     * @param findDeleted
     * @param skipNodeRefCheck
     * @param ws
     * @return
     */
    public Object getNodeRefProperty(String acmType, boolean ignoreWorkspace, Date dateTime, boolean findDeleted,
                    boolean skipNodeRefCheck, WorkspaceNode ws) {
        // Make sure we have the right node ref before getting a property from
        // it.
        if (dateTime != null && getNodeRef().getStoreRef() != null
                        && getNodeRef().getStoreRef().equals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE)) {
            NodeRef realRef = null;
            // if ( NodeUtil.workspacesEqual( ws, getWorkspace() ) ) {
            realRef = NodeUtil.getNodeRefAtTime(getNodeRef(), dateTime);
            // } else {
            // Can't do this--it causes an infinite loop. It's the caller's
            // responsibility to have a node in the right workspace.
            // realRef = NodeUtil.getNodeRefAtTime( getNodeRef(), ws, dateTime
            // );
            // }
            if (realRef != null && !realRef.equals(getNodeRef())) {
                if (realRef.getStoreRef() != StoreRef.STORE_REF_WORKSPACE_SPACESSTORE) {
                    EmsScriptNode realNode = new EmsScriptNode(realRef, getServices());
                    return realNode.getNodeRefProperty(acmType, ignoreWorkspace, dateTime, findDeleted,
                                    skipNodeRefCheck, ws);
                }
            }
        }
        Object result = getPropertyImpl(acmType, true); // TODO -- This should
                                                        // be passing in
                                                        // cacheOkay from the
                                                        // caller instead of
                                                        // true!

        // get noderefs from the proper workspace unless the property is a
        // workspace meta-property
        if (!skipNodeRefCheck && !workspaceMetaProperties.contains(acmType)) {
            if (result instanceof NodeRef) {
                result = NodeUtil.getNodeRefAtTime((NodeRef) result, ws, dateTime, ignoreWorkspace, findDeleted);
            } else if (result instanceof Collection) {
                Collection<?> resultColl = (Collection<?>) result;
                ArrayList<Object> arr = new ArrayList<>();
                for (Object o : resultColl) {
                    if (o instanceof NodeRef) {
                        NodeRef ref = NodeUtil.getNodeRefAtTime((NodeRef) o, ws, dateTime, ignoreWorkspace,
                                        findDeleted);
                        arr.add(ref);
                    } else {
                        arr.add(o);
                    }
                }
                result = arr;
            }
        }

        return result;
    }

    /**
     * Get the property of the specified type for non-noderef properties. Throws unsupported
     * operation exception otherwise (go and fix the code if that happens).
     *
     * @param acmType Short name of property to get
     * @param cacheOkay
     * @return
     */
    public Object getProperty(String acmType) {
        return getProperty(acmType, true);
    }

    /**
     * Get the property of the specified type for non-noderef properties. Throws unsupported
     * operation exception otherwise (go and fix the code if that happens).
     *
     * @param acmType Short name of property to get
     * @param cacheOkay
     * @return
     */
    public Object getProperty(String acmType, boolean cacheOkay) {
        Object result = getPropertyImpl(acmType, cacheOkay);

        // Throw an exception of the property value is a NodeRef or
        // collection of NodeRefs
        // TODO -- REVIEW -- Can the if-statements be reordered to make this
        // more efficient?
        if (!workspaceMetaProperties.contains(acmType)) {
            if (result instanceof NodeRef) {
                throw new UnsupportedOperationException();
            } else if (result instanceof Collection) {
                Collection<?> resultColl = (Collection<?>) result;
                if (!Utils.isNullOrEmpty(resultColl)) {
                    Object firstResult = resultColl.iterator().next();
                    if (firstResult instanceof NodeRef) {
                        throw new UnsupportedOperationException();
                    }
                }
            }
        }

        return result;
    }

    private Object getPropertyImpl(String acmType, boolean cacheOkay) {
        return NodeUtil.getNodeProperty(this, acmType, getServices(), useFoundationalApi, cacheOkay);
    }

    public Object getPropertyAtTime(String acmType, Date dateTime) {
        return getPropertyAtTime(acmType, dateTime, true);
    }

    public Object getPropertyAtTime(String acmType, Date dateTime, boolean cacheOkay) {
        Object result = getPropertyImpl(acmType, cacheOkay);
        if (result instanceof NodeRef) {
            result = NodeUtil.getNodeRefAtTime((NodeRef) result, dateTime);
        }
        return result;
    }

    /**
     * Last modified time of a node is the greatest of its last modified or any of its embedded
     * value specs.
     *
     * @param dateTime Time to check against
     * @return
     */
    public Pair<Date, String> getLastModifiedAndModifier(Date dateTime) {
        Set<NodeRef> dependentNodes = new HashSet<>();

        // Can't use normal getProperty because we need to bypass the cache.
        Date lastModifiedDate = (Date) NodeUtil.getNodeProperty(this, Acm.ACM_LAST_MODIFIED, getServices(),
                        useFoundationalApi, false);
        String lastModifier = (String) NodeUtil.getNodeProperty(this, "cm:modifier", getServices(), useFoundationalApi,
                        false);

        // WARNING! TODO -- It should be okay to not pass in the workspace
        // context assuming that a Property is the parent of its value. If a
        // Property's name changes, pointing to the parent workspace for the
        // ValueSpec is okay. If the ValueSpec changes, the Property should be
        // copied to the workspace since it is a parent of the changed
        // ValueSpec.
        // The elementValueOfElement of an ElementValue (embedded in the
        // ValueSpec)
        // is not checked--this may not be the desired behavior.

        // Check to see if any embedded value specs have been modified after
        // the modified time of this node:
        NodeUtil.addEmbeddedValueSpecs(getNodeRef(), dependentNodes, services, dateTime, getWorkspace());
        for (NodeRef nodeRef : dependentNodes) {
            nodeRef = NodeUtil.getNodeRefAtTime(nodeRef, dateTime);
            if (nodeRef == null)
                continue;
            EmsScriptNode oNode = new EmsScriptNode(nodeRef, services);
            if (!oNode.exists())
                continue;
            Pair<Date, String> pair = oNode.getLastModifiedAndModifier(dateTime);
            Date modified = pair.first;
            String modifier = pair.second;
            if (modified.after(lastModifiedDate)) {
                lastModifiedDate = modified;
                lastModifier = modifier;
            }
        }
        return new Pair<Date, String>(lastModifiedDate, lastModifier);
    }

    /**
     * Last modified time of a node is the greatest of its last modified or any of its embedded
     * value specs.
     *
     * @param dateTime Time to check against
     * @return
     */
    public Date getLastModified(Date dateTime) {
        Pair<Date, String> pair = this.getLastModifiedAndModifier(dateTime);
        return pair != null ? pair.first : null;
    }

    /**
     * Get the properties of this node
     *
     * @param acmType Short name of property to get
     * @return
     */
    @Override
    public Map<String, Object> getProperties() {

        if (useFoundationalApi) {
            return Utils.toMap(services.getNodeService().getProperties(nodeRef), String.class, Object.class);
        } else {
            return super.getProperties();
        }
    }

    /**
     * Gets the properties of node making sure to get the correct noderef for properties whose
     * values are noderefs.
     *
     * @param dateTime
     * @param ws
     * @return
     */
    public Map<String, Object> getNodeRefProperties(Date dateTime, WorkspaceNode ws) {

        if (useFoundationalApi) {

            Map<String, Object> returnMap = new HashMap<>();
            Map<QName, Serializable> map = services.getNodeService().getProperties(nodeRef);

            // Need to potentially replace each property with the correct
            // property value for
            // the workspace. Remember, that property that points to a node ref
            // may point to
            // one in a parent workspace, so we must do a search by id to get
            // the correct one:
            for (Entry<QName, Serializable> entry : map.entrySet()) {
                String keyShort = NodeUtil.getShortQName(entry.getKey());
                // Versioned nodes property maps include properties that map to
                // null, but normal nodes dont,
                // so filter this out:
                if (entry.getValue() != null) {
                    returnMap.put(entry.getKey().toString(), getNodeRefProperty(keyShort, dateTime, ws));
                }
            }
            return returnMap;
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
        // if (response != null) {
        // response.append(msg + "\n");
        // }
    }

    /**
     * Genericized function to set property for non-collection types
     *
     * @param acmType Property short name for alfresco content model type
     * @param value Value to set property to
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
                makeSureNodeRefIsNotFrozen();
                transactionCheck();
                services.getNodeService().setProperty(nodeRef, createQName(acmType), value);
                if (cacheOkay)
                    NodeUtil.propertyCachePut(getNodeRef(), acmType, value);
                if (acmType.equals(Acm.ACM_NAME)) {
                    renamed = true;
                    // removeChildrenFromJsonCache();
                }
            } catch (Exception e) {
                // This should never happen!
                success = false;
                // If the node is a version, then we will catch an exception.
                // Try again with the live node, but make sure it's the latest
                // version.
                NodeRef liveRef = nodeRef;
                if (isAVersion()) {
                    success = true;
                    logger.error("Tried to set property of a version nodeRef in " + "setProperty(acmType=" + acmType
                                    + ", value=" + value + ") for EmsScriptNode " + this
                                    + " calling NodeService.setProperty(nodeRef=" + nodeRef + ", " + acmType + ", "
                                    + value + ")");
                    if (count > 0) {
                        this.log("ERROR! Potential infinite recursion!");
                        return false;
                    }
                    liveRef = getLiveNodeRefFromVersion();
                    if (!nodeRef.equals(liveRef)) {
                        // make sure the version is equal or greater
                        int comp = NodeUtil.compareVersions(nodeRef, liveRef);
                        if (comp > 0) {
                            logger.error("ERROR! Live version " + liveRef + "" + " is earlier than versioned ref "
                                            + "when trying to set property of a version nodeRef in "
                                            + "setProperty(acmType=" + acmType + ", value=" + value
                                            + ") for EmsScriptNode " + this
                                            + " calling NodeService.setProperty(nodeRef=" + nodeRef + ", " + acmType
                                            + ", " + value + ")");
                            success = false;
                        } else if (comp < 0) {
                            logger.error("WARNING! Versioned node ref is not most current "
                                            + "when trying to set property of a version nodeRef in "
                                            + "setProperty(acmType=" + acmType + ", value=" + value
                                            + ") for EmsScriptNode " + this
                                            + " calling NodeService.setProperty(nodeRef=" + nodeRef + ", " + acmType
                                            + ", " + value + ")" + ".\nWARNING! Setting property using live node ref "
                                            + liveRef + "last modified at " + NodeUtil.getLastModified(liveRef));
                        }
                        nodeRef = liveRef; // this is
                        if (comp <= 0) {
                            liveRef = null;
                            success = setProperty(acmType, value, cacheOkay, count + 1);
                            if (cacheOkay)
                                NodeUtil.propertyCachePut(getNodeRef(), acmType, value);
                            success = true;
                        }
                    }
                }
                if (nodeRef.equals(liveRef)) {
                    logger.error("Got exception in " + "setProperty(acmType=" + acmType + ", value=" + value
                                    + ") for EmsScriptNode " + this + " calling NodeService.setProperty(nodeRef="
                                    + nodeRef + ", " + acmType + ", " + value + ")");
                    e.printStackTrace();
                    // StackTraceElement[] trace = e.getStackTrace();
                    // StackTraceElement s = trace[0];
                    // s.getMethodName()
                }
            }
        } else {
            makeSureNodeRefIsNotFrozen();
            transactionCheck();
            getProperties().put(acmType, value);
            save();
            if (cacheOkay)
                NodeUtil.propertyCachePut(getNodeRef(), acmType, value);
            if (acmType.equals(Acm.ACM_NAME)) {
                renamed = true;
                // removeChildrenFromJsonCache();
            }
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
     * @return the storeRef
     */
    public static StoreRef getStoreRef() {
        return NodeUtil.getStoreRef();
    }

    public String getSysmlQName(Date dateTime, WorkspaceNode ws, boolean doCache) {
        return getSysmlQPath(true, dateTime, ws, doCache);
    }

    public String getSysmlQId(Date dateTime, WorkspaceNode ws, boolean doCache) {
        return getSysmlQPath(false, dateTime, ws, doCache);
    }

    /**
     * Returns the closest site characterization to which the element belongs
     *
     * @param workspaceNode
     * @param dateTime
     * @return
     */
    public String getSiteCharacterizationId(Date date, WorkspaceNode ws) {
        if (siteCharacterizationId != null) {
            return siteCharacterizationId;
        } else {
            // the following call will get the site characterization if it
            // exists
            getSysmlQName(date, ws, true);
            return siteCharacterizationId;
        }
    }

    /**
     * Builds the SysML qualified name/id for an object - if not SysML, won't return anything
     *
     * @param isName If true, returns the names, otherwise returns ids
     *
     * @return SysML qualified name (e.g., sysml:name qualified)
     */
    public String getSysmlQPath(boolean isName, Date dateTime, WorkspaceNode ws) {
        return getSysmlQPath(isName, dateTime, ws, true);
    }

    public String getSysmlQPath(boolean isName, Date dateTime, WorkspaceNode ws, boolean doCache) {
        // TODO REVIEW
        // This is currently not called on reified packages, so as long as the
        // ems:owner always points
        // to reified nodes, as it should, then we dont need to replace
        // pkgSuffix in the qname.
        // Some elements have a name of "" and they appear to be skipped in the
        // qualified name. Do we want to treat this the same as a null name?
        // Currently, we do not.

        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !ADMIN_USER_NAME.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(ADMIN_USER_NAME);
        }

        String qualifiedName = "/" + getProperty("sysml:name");
        String qualifiedId = "/" + getProperty("sysml:id");
        // if ( qualifiedId.contains( "exposed_id" ) ) {
        // System.out.println( "Calculating qualified name and id for " +
        // qualifiedId );
        // }

        EmsScriptNode owner = this.getOwningParent(dateTime, ws, false, true);
        String ownerName = owner != null ? owner.getName() : null;

        String siteCharacterizationId = this.siteCharacterizationId;

        // Need to look up based on owner b/c the parent associations are not
        // versioned,
        // but owners only go up to the project node, so the site node must be
        // found
        // using the parent. getOwningParent() searches for parent if owner is
        // not found.
        while (owner != null && !ownerName.equals("Models")) {
            String nameProp = (String) owner.getProperty("sysml:name");
            String idProp = (String) owner.getProperty("sysml:id");
            if (idProp == null) {
                break;
            }
            // nameProp = nameProp.endsWith(pkgSuffix) ?
            // nameProp.replace(pkgSuffix, "" ) : nameProp;
            qualifiedName = "/" + nameProp + qualifiedName;

            // stop if we find a site characterization in the path
            if (owner.hasAspect("ems:SiteCharacterization") && siteCharacterizationId == null) {
                siteCharacterizationId = "site_" + idProp;
            }
            qualifiedId = "/" + idProp + qualifiedId;

            owner = owner.getOwningParent(dateTime, ws, false, true);
            ownerName = owner != null ? owner.getName() : null;
        }

        // Get the site, which is one up from the Models node:
        EmsScriptNode modelNode = (owner != null && ownerName.equals("Models")) ? owner : null;
        if (modelNode != null) {
            EmsScriptNode siteNode = modelNode.getOwningParent(dateTime, ws, false, true);
            if (siteNode != null) {
                qualifiedName = "/" + siteNode.getName() + qualifiedName;
                qualifiedId = "/" + siteNode.getName() + qualifiedId;
                if (siteCharacterizationId == null) {
                    siteCharacterizationId = siteNode.getName();
                }
            }
        }

        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }

        if (doCache) {
            // if ( qualifiedId.contains( "exposed_id" ) ) {
            // System.out.println( "Setting qualified id: " + qualifiedId );
            // }
            // this.qualifiedId = qualifiedId;
            // this.qualifiedName = qualifiedName;
            if (this.siteCharacterizationId == null) {
                this.siteCharacterizationId = siteCharacterizationId;
            }
        }

        if (isName) {
            return qualifiedName;
        } else {
            return qualifiedId;
        }
    }

    /**
     * Get the children views as a JSONArray
     *
     * @return
     */
    public JSONArray getChildrenViewsJSONArray() {
        JSONArray childrenViews = new JSONArray();
        try {
            Object property = this.getProperty(Acm.ACM_CHILDREN_VIEWS);
            if (property != null) {
                childrenViews = new JSONArray(property.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return childrenViews;
    }

    @Override
    public String toString() {
        String result = "";
        boolean wasOn = Debug.isOn();
        if (wasOn)
            Debug.turnOff();
        // try {
        // return "" + toJSONObject();
        // } catch ( JSONException e ) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // return null;
        try {
            if (!exists() && !isDeleted()) {
                return "NON-EXISTENT-NODE";
            }
            String deleted = isDeleted() ? "DELETED: " : "";
            String name = getName();
            String id = getSysmlId();
            String sysmlName = getSysmlName();
            String qualifiedName = getSysmlQName(null, getWorkspace(), false);
            String type = getTypeName();
            String workspaceName = getWorkspaceName();
            result = deleted + "{type=" + type + ", id=" + id + ", cm_name=" + name + ", sysml_name=" + sysmlName
                            + ", qualified name=" + qualifiedName + ", workspace=" + workspaceName + "}";
        } catch (Throwable t) {
            // ignore
        }
        if (wasOn)
            Debug.turnOn();
        return result;
    }

    /**
     * Convert node into our custom JSONObject with all possible keys
     *
     * @param timestamp
     *
     * @return JSONObject serialization of node
     */
    public JSONObject toJSONObject(WorkspaceNode ws, Date dateTime) throws JSONException {
        // don't include qualified except for diffs, as added by DeclarativeJavaWebScript
        boolean isQualified = true;
        if (NodeUtil.doPostProcessQualified)
            isQualified = false;
        return toJSONObject(null, ws, dateTime, isQualified, false, null);
    }

    public JSONObject toJSONObject(WorkspaceNode ws, Date dateTime, boolean isIncludeQualified,
                    boolean isIncludeDocument, List<EmsScriptNode> ownedProperties) throws JSONException {
        return toJSONObject(null, ws, dateTime, isIncludeQualified, isIncludeDocument, ownedProperties);
    }

    /**
     * Convert node into our custom JSONObject, showing qualifiedName and editable keys
     *
     * @param renderType Type of JSONObject to render, this filters what keys are in JSONObject
     * @return JSONObject serialization of node
     */
    public JSONObject toJSONObject(Set<String> filter, WorkspaceNode ws, Date dateTime, boolean isIncludeQualified,
                    boolean isIncludeDocument, List<EmsScriptNode> ownedProperties) throws JSONException {
        return toJSONObject(filter, false, ws, dateTime, isIncludeQualified, isIncludeDocument, null, ownedProperties);
    }

    public String nodeRefToSysmlId(NodeRef ref) throws JSONException {
        EmsScriptNode node = new EmsScriptNode(ref, services);
        Object sysmlId = node.getSysmlId();
        if (sysmlId != null) {
            return "" + sysmlId;
        } else {
            Debug.error(true, "elementValue has no sysml id: " + ref);
            return "" + ref.getId();
        }
    }

    public JSONArray nodeRefsToJSONArray(Collection<?> nodeRefs) throws JSONException {
        JSONArray jarr = new JSONArray();
        for (Object o : nodeRefs) {
            if (!(o instanceof NodeRef)) {
                jarr.put("" + o);
                Debug.error(true, "object is not a nodeRef, adding to json: " + o);
            } else {
                jarr.put(nodeRefToSysmlId((NodeRef) o));
            }
        }
        return jarr;
    }

    // add in all the properties
    protected static TreeSet<String> acmPropNames = new TreeSet<>(Acm.getACM2JSON().keySet());

    private EmsScriptNode getNodeAtAtime(Date dateTime) {
        NodeRef nodeRef = NodeUtil.getNodeRefAtTime(getNodeRef(), dateTime);
        if (nodeRef != null) {
            return new EmsScriptNode(nodeRef, services, response);
        }
        // return latest if not found
        return this;
    }

    private void putInJson(JSONObject jsonObject, String key, Object value, Set<String> filter) throws JSONException {
        if (key == null || value == null)
            return;
        if (filter == null || filter.size() == 0 || filter.contains(key)) {
            jsonObject.put(key, value);
        }
    }

    public Date getCreationDate() {
        Date date = (Date) getProperty("cm:created");
        return date;
    }

    /**
     * This method assumes that the node that it's acting on is already found at the correct time in
     * the correct workspace.
     *
     * Workspace is never needed in this context since there are no node references.
     *
     * @param elementJson JSON to update with element information
     * @param filter List of keys to exclude from the elementJson
     * @param dateTime Time of to retrieve information for
     * @param isIncludeQualified Toggle to include qualifiedId/Name (since its expensive)
     * @param version Deprecated
     * @throws JSONException
     */
    protected void addElementJSON(JSONObject elementJson, Set<String> filter, Date dateTime, boolean isIncludeQualified,
                    Version version) throws JSONException {
        if (this == null || !this.exists())
            return;
        // mandatory elements put in directly
        elementJson.put(Acm.JSON_ID, this.getProperty(Acm.ACM_ID));
        EmsScriptNode originalNode = this.getOriginalNode();
        elementJson.put("creator", originalNode.getProperty("cm:creator"));
        elementJson.put("created", originalNode.getProperty("cm:created"));
        elementJson.put("nodeRefId", originalNode.getNodeRef().toString());
        elementJson.put("versionedRefId", NodeUtil.getVersionedRefId(this));

        Pair<Date, String> pair = this.getLastModifiedAndModifier(dateTime);
        if (pair != null) {
            elementJson.put("modifier", pair.second);
            elementJson.put(Acm.JSON_LAST_MODIFIED, TimeUtils.toTimestamp(pair.first));
        }

        putInJson(elementJson, Acm.JSON_NAME, this.getProperty(Acm.ACM_NAME), filter);
        putInJson(elementJson, Acm.JSON_DOCUMENTATION, this.getProperty(Acm.ACM_DOCUMENTATION), filter);

        // check properties rather than aspect since aspect may not be applied
        // on creation
        Object appliedMetatypes = this.getProperty(Acm.ACM_APPLIED_METATYPES);
        Object isMetatype = this.getProperty(Acm.ACM_IS_METATYPE);
        Object metatypes = this.getProperty(Acm.ACM_METATYPES);
        if (appliedMetatypes != null)
            elementJson.put(Acm.JSON_APPLIED_METATYPES, appliedMetatypes);
        if (isMetatype != null)
            elementJson.put(Acm.JSON_IS_METATYPE, isMetatype);
        if (metatypes != null)
            elementJson.put(Acm.JSON_METATYPES, metatypes);

        ArrayList<NodeRef> nodeRefsOwnedAttribute = (ArrayList<NodeRef>) this
                        .getNodeRefProperty(Acm.ACM_OWNED_ATTRIBUTE, true, dateTime, this.getWorkspace());
        if (!Utils.isNullOrEmpty(nodeRefsOwnedAttribute)) {
            JSONArray ownedAttributeIds = addNodeRefIdsJSON(nodeRefsOwnedAttribute);
            putInJson(elementJson, Acm.JSON_OWNED_ATTRIBUTE, ownedAttributeIds, filter);
        }

        // NOTE: DeclarativeJavaWebScript does this when isIncludeQualified is false
        // isIncludeQualified should only be called for diffs
        if (isIncludeQualified) {
            if (filter == null || filter.isEmpty() || filter.contains("qualifiedName")) {
                putInJson(elementJson, "qualifiedName", this.getSysmlQName(dateTime, getWorkspace(), true), filter);
            }
            if (filter == null || filter.isEmpty() || filter.contains("qualifiedId")) {
                putInJson(elementJson, "qualifiedId", this.getSysmlQId(dateTime, getWorkspace(), true), filter);
            }
            if (filter == null || filter.isEmpty() || filter.contains("siteCharacterizationId")) {
                putInJson(elementJson, "siteCharacterizationId",
                                this.getSiteCharacterizationId(dateTime, getWorkspace()), filter);
            }
        }
        if (filter == null || filter.size() == 0 || filter.contains(Sjm.OWNERID)) {

            // not passing in dateTime/workspace since sysml id is immutable
            EmsScriptNode owner = this.getOwningParent(null, null, true);

            String ownerId = null;
            Object owernIdObj = null;
            if (owner != null) {
                ownerId = (String) owner.getProperty("sysml:id");
                if (ownerId != null) {
                    if (ownerId.endsWith("_pkg")) {
                        ownerId = ownerId.replace("_pkg", "");
                        // FIXME: need to make sure the owner exists
                        NodeRef ref = findNodeRefByType(ownerId, SearchType.ID.prefix, getWorkspace(), dateTime, false);
                        if (ref == null) {
                            ownerId = null;
                            // TODO -- create reified node?
                        }
                    }
                }

            }

            // No longer using "null". This works better.
            owernIdObj = ownerId == null ? JSONObject.NULL : ownerId;
            putInJson(elementJson, Sjm.OWNERID, owernIdObj, filter);
        }

        // putInJson( json, "evaluation", "Hi, Erik!", null );
        // elementJson.put( "evaluation", "Hi, Erik!" );

        // Add version information - can't be used for reverting since things
        // may be in
        // different workspaces, but informative nonetheless
        if (version != null) {
            EmsScriptNode vNode = new EmsScriptNode(version.getVersionedNodeRef(), getServices(), getResponse());

            // for reverting need to keep track of noderef and versionLabel
            // if ( filter == null || filter.isEmpty() || filter.contains( "id"
            // ) ) {
            elementJson.put("id", vNode.getId());
            // }
            // if ( filter == null || filter.isEmpty() || filter.contains(
            // "version" ) ) {
            elementJson.put("version", version.getVersionLabel());
            // }
        } else {
            // // If the passed-in version is null, then use the current version
            // // and existing id. The "id" and "version" must be explicit in
            // the
            // // filter to be added.
            // if ( filter != null && !filter.isEmpty() ) {
            // if ( filter.contains( "id" ) ) {
            // elementJson.put( "id", getId() );
            // }
            // if ( filter.contains( "version" ) ) {
            // Version v = getCurrentVersion();
            // if ( v != null ) {
            // String label = v.getVersionLabel();
            // if ( label != null ) {
            // elementJson.put( "version", label );
            // }
            // }
            // }
            // }
        }
    }

    public enum SpecEnum {
        Association, Binding, Characterizes, Conform, Connector, Constraint, Dependency, DirectedRelationship, DurationInterval, Duration, ElementValue, Expose, Expression, Generalization, InstanceSpecification, InstanceValue, Interval, LiteralBoolean, LiteralInteger, LiteralNull, LiteralReal, LiteralSet, LiteralString, LiteralUnlimitedNatural, MagicDrawData, OpaqueExpression, Operation, Package, Parameter, Product, Property, StringExpression, Succession, TimeExpression, TimeInterval, ValueSpecification, View, Viewpoint
    };

    public static Map<String, SpecEnum> aspect2Key = new HashMap<String, SpecEnum>() {
        private static final long serialVersionUID = -2080928480362524333L;

        {
            put("Association", SpecEnum.Association);
            put("Binding", SpecEnum.Binding);
            put("Characterizes", SpecEnum.Characterizes);
            put("Conform", SpecEnum.Conform);
            put("Connector", SpecEnum.Connector);
            put("Constraint", SpecEnum.Constraint);
            put("Dependency", SpecEnum.Dependency);
            put("DirectedRelationship", SpecEnum.DirectedRelationship);
            put("DurationInterval", SpecEnum.DurationInterval);
            put("Duration", SpecEnum.Duration);
            put("ElementValue", SpecEnum.ElementValue);
            put("Expose", SpecEnum.Expose);
            put("Expression", SpecEnum.Expression);
            put("Generalization", SpecEnum.Generalization);
            put("InstanceSpecification", SpecEnum.InstanceSpecification);
            put("InstanceValue", SpecEnum.InstanceValue);
            put("Interval", SpecEnum.Interval);
            put("LiteralBoolean", SpecEnum.LiteralBoolean);
            put("LiteralInteger", SpecEnum.LiteralInteger);
            put("LiteralNull", SpecEnum.LiteralNull);
            put("LiteralReal", SpecEnum.LiteralReal);
            put("LiteralSet", SpecEnum.LiteralSet);
            put("LiteralString", SpecEnum.LiteralString);
            put("LiteralUnlimitedNatural", SpecEnum.LiteralUnlimitedNatural);
            put("MagicDrawData", SpecEnum.MagicDrawData);
            put("OpaqueExpression", SpecEnum.OpaqueExpression);
            put("Operation", SpecEnum.Operation);
            put("Package", SpecEnum.Package);
            put("Parameter", SpecEnum.Parameter);
            put("Product", SpecEnum.Product);
            put("Property", SpecEnum.Property);
            put("StringExpression", SpecEnum.StringExpression);
            put("Succession", SpecEnum.StringExpression);
            put("TimeExpression", SpecEnum.TimeExpression);
            put("TimeInterval", SpecEnum.TimeInterval);
            put("ValueSpecification", SpecEnum.ValueSpecification);
            put("View", SpecEnum.View);
            put("Viewpoint", SpecEnum.Viewpoint);

        }
    };

    private void addSpecializationJSON(JSONObject json, Set<String> filter, WorkspaceNode ws, Date dateTime)
                    throws JSONException {
        addSpecializationJSON(json, filter, ws, dateTime, false);
    }

    private void addSpecializationJSON(JSONObject json, Set<String> filter, WorkspaceNode ws, Date dateTime,
                    boolean justTheType) throws JSONException {
        String typeName = getTypeName();
        if (typeName == null) {
            // TODO: error logging
            return;
        }

        if (filter == null || filter.isEmpty() || filter.contains(Acm.JSON_TYPE)) {
            json.put(Acm.JSON_TYPE, typeName);
        }

        if (justTheType)
            return;

        for (QName aspectQname : this.getAspectsSet()) {
            // reflection is too slow?
            String cappedAspectName = Utils.capitalize(aspectQname.getLocalName());
            SpecEnum aspect = aspect2Key.get(cappedAspectName);
            if (aspect == null) {// || node == null ||
                                 // !node.scriptNodeExists() ) {

            } else {
                switch (aspect) {
                    case Association:
                        addAssociationJSON(json, this, filter, dateTime);
                        break;
                    case Binding:
                        addBindingJSON(json, this, filter, ws, dateTime);
                        break;
                    case Characterizes:
                        addCharacterizesJSON(json, this, filter, dateTime);
                        break;
                    case Conform:
                        addConformJSON(json, this, filter, dateTime);
                        break;
                    case Connector:
                        addConnectorJSON(json, this, filter, ws, dateTime);
                        break;
                    case Constraint:
                        addConstraintJSON(json, this, filter, ws, dateTime);
                        break;
                    case Dependency:
                        addDependencyJSON(json, this, filter, dateTime);
                        break;
                    case DirectedRelationship:
                        addDirectedRelationshipJSON(json, this, filter, dateTime);
                        break;
                    case Duration:
                        addDurationJSON(json, this, filter, dateTime);
                        break;
                    case DurationInterval:
                        addDurationIntervalJSON(json, this, filter, dateTime);
                        break;
                    case ElementValue:
                        addElementValueJSON(json, this, filter, dateTime);
                        break;
                    case LiteralSet:
                        addLiteralSetJSON(json, this, filter, ws, dateTime);
                        break;
                    case Expose:
                        addExposeJSON(json, this, filter, dateTime);
                        break;
                    case Expression:
                        addExpressionJSON(json, this, filter, ws, dateTime);
                        break;
                    case Generalization:
                        addGeneralizationJSON(json, this, filter, dateTime);
                        break;
                    case InstanceSpecification:
                        addInstanceSpecificationJSON(json, this, filter, ws, dateTime);
                        break;
                    case InstanceValue:
                        addInstanceValueJSON(json, this, filter, dateTime);
                        break;
                    case Interval:
                        addIntervalJSON(json, this, filter, dateTime);
                        break;
                    case LiteralBoolean:
                        addLiteralBooleanJSON(json, this, filter, dateTime);
                        break;
                    case LiteralInteger:
                        addLiteralIntegerJSON(json, this, filter, dateTime);
                        break;
                    case LiteralNull:
                        addLiteralNullJSON(json, this, filter, dateTime);
                        break;
                    case LiteralReal:
                        addLiteralRealJSON(json, this, filter, dateTime);
                        break;
                    case LiteralString:
                        addLiteralStringJSON(json, this, filter, dateTime);
                        break;
                    case LiteralUnlimitedNatural:
                        addLiteralUnlimitedNaturalJSON(json, this, filter, dateTime);
                        break;
                    case MagicDrawData:
                        addMagicDrawDataJSON(json, this, filter, dateTime);
                        break;
                    case OpaqueExpression:
                        addOpaqueExpressionJSON(json, this, filter, dateTime);
                        break;
                    case Operation:
                        addOperationJSON(json, this, filter, ws, dateTime);
                        break;
                    case Package:
                        addPackageJSON(json, this, filter, dateTime);
                        break;
                    case Parameter:
                        addParameterJSON(json, this, filter, dateTime);
                        break;
                    case Product:
                        addProductJSON(json, this, filter, ws, dateTime);
                        break;
                    case Property:
                        addPropertyJSON(json, this, filter, ws, dateTime);
                        break;
                    case StringExpression:
                        addStringExpressionJSON(json, this, filter, dateTime);
                        break;
                    case Succession:
                        addSuccessionJSON(json, this, filter, ws, dateTime);
                        break;
                    case TimeExpression:
                        addTimeExpressionJSON(json, this, filter, dateTime);
                        break;
                    case TimeInterval:
                        addTimeIntervalJSON(json, this, filter, dateTime);
                        break;
                    case ValueSpecification:
                        addValueSpecificationJSON(json, this, filter, dateTime);
                        break;
                    case View:
                        addViewJSON(json, this, filter, ws, dateTime);
                        break;
                    case Viewpoint:
                        addViewpointJSON(json, this, filter, dateTime);
                        break;
                    default:

                } // end switch
            } // end if aspect == null
        }
    }

    /**
     * Convert node into our custom JSONObject. This calls
     * {@link #toJSONObjectImplImpl(Set, boolean, Date, boolean)}.
     *
     * @param isExprOrProp If true, does not add specialization key, as it is nested call to process
     *        the Expression operand or Property value
     * @param dateTime The time of the specialization, specifying the version. This should
     *        correspond the this EmsScriptNode's version, but that is not checked.
     * @param isIncludeQualified whether to include the qualified name and qualified id in the json
     * @param filter Set of keys that should be displayed (plus the mandatory fields)
     *
     * @return JSONObject serialization of node
     * @throws JSONException
     */
    public JSONObject toJSONObject(Set<String> jsonFilter, boolean isExprOrProp, WorkspaceNode ws, Date dateTime,
                    boolean isIncludeQualified, boolean isIncludeDocument, Version version,
                    List<EmsScriptNode> ownedProperties) throws JSONException {
        JSONObject json = toJSONObjectImpl(jsonFilter, isExprOrProp, ws, dateTime, isIncludeQualified,
                        isIncludeDocument, version);
//        if (!isExprOrProp)
//            addEditableJson(json, jsonFilter);

        // Add owned Properties to properties key:
        if (!Utils.isNullOrEmpty(ownedProperties)) {
            JSONArray props = new JSONArray();
            for (EmsScriptNode prop : ownedProperties) {
                props.put(prop.toJSONObject(ws, dateTime, isIncludeQualified, isIncludeDocument, null));
                putInJson(json, "properties", props, jsonFilter);
            }
        }

        return json;
    }

    public void addEditableJson(JSONObject json, Set<String> jsonFilter) throws JSONException {
        putInJson(json, Sjm.EDITABLE, hasPermission(PermissionService.WRITE), jsonFilter);
    }

    public JSONObject toJSONObjectImpl(Set<String> jsonFilter, boolean isExprOrProp, WorkspaceNode ws, Date dateTime,
                    boolean isIncludeQualified, boolean isIncludeDocument, Version version) throws JSONException {
        if (Debug.isOn())
            Debug.outln("$ $ $ $ toJSONObject(jsonFilter=" + jsonFilter + ", isExprOrProp=" + isExprOrProp
                            + ", dateTime=" + dateTime + ", isIncludeQualified=" + isIncludeQualified + ") on " + this);
        boolean forceCacheUpdate = false;

        // Return empty json if this element does not exist.
        JSONObject element = NodeUtil.newJsonObject();
        if (!exists()) {
            if (Debug.isOn())
                Debug.outln("node doesn't exist; returning  " + element);
            return element;
        }

        // If not caching, generate and return the json for this element.
        JSONObject cachedJson = null;
        JSONObject json = null;
        long millis = 0L;

        jsonFilter = jsonFilter == null ? new TreeSet<String>() : jsonFilter;

        boolean tryCache = NodeUtil.doJsonCaching && !isExprOrProp;
        if (!tryCache) {
            json = toJSONObjectImplImpl(jsonFilter, isExprOrProp, ws, dateTime, isIncludeQualified, isIncludeDocument,
                            version);
            if (Debug.isOn())
                Debug.outln("not trying cache returning json " + (json == null ? "null" : json.toString(4)));
            return json;
        }

        if (dateTime != null)
            millis = dateTime.getTime();

        boolean deepMatch = false;

        // Check the cache unless forcing an update.
        String versionLabel = null;
        if (!forceCacheUpdate) {
            if (version != null)
                versionLabel = version.getVersionLabel();
            cachedJson = !NodeUtil.doJsonDeepCaching ? null
                            : NodeUtil.jsonDeepCacheGet(getId(), millis, isIncludeQualified, jsonFilter, versionLabel,
                                            false);
            if (cachedJson != null && cachedJson.length() > 0) {
                deepMatch = true;
            } else {
                cachedJson = NodeUtil.jsonCacheGet(id, millis, false);
            }
            if (Debug.isOn())
                Debug.outln("cachedJson = " + (cachedJson == null ? "null" : cachedJson.toString(4)));
            if (cachedJson != null && cachedJson.length() > 0) {
                // Will clone json later to avoid changing cached information.
                json = cachedJson;
            }
        }

        // Look at last modified time in json and compare with actual last
        // modified.
        // Force an update if the json is old. Otherwise, if a deep match was
        // found,
        // then the json is already filtered, so go ahead and return.
        if (cachedJson != null && cachedJson.has("modified")) {
            String cachedModifiedStr = cachedJson.getString("modified");
            Date cachedModified = TimeUtils.dateFromTimestamp(cachedModifiedStr);
            Date lastModified;
            lastModified = getLastModified(dateTime);
            if (cachedModified == null || lastModified == null || lastModified.after(cachedModified)) {
                json = null;
                forceCacheUpdate = true;
            } else if (deepMatch) {
                return json;
            }
        }

        // If not using cached json, generate the json and put it in the cache
        // if forcing an update or if there is no cached json. Cache the full
        // json, and then filter afterwards.
        if (json != null) {
            ++NodeUtil.jsonCacheHits;
        } else {
            // get full json without filtering
            json = toJSONObjectImplImpl(null, isExprOrProp, ws, dateTime, isIncludeQualified, isIncludeDocument,
                            version);
            if (Debug.isOn())
                Debug.outln("json = " + (json == null ? "null" : json.toString(4)));
            if (tryCache && (forceCacheUpdate || cachedJson == null || cachedJson.length() == 0) && json != null
                            && json.length() > 0) {
                ++NodeUtil.jsonCacheMisses;
                NodeUtil.jsonCachePut(json, getId(), millis);
                if (Debug.isOn())
                    Debug.outln("put json = " + (json == null ? "null" : json.toString(4)));
            }
        }

        if (json == null || json.length() == 0) {
            return element;
        }

        // Filter full json
        if (jsonFilter != null && !jsonFilter.isEmpty()) {
            // If using a filter, only include json with keys in the filter.
            JSONObject newJson = NodeUtil.filterJson(json, jsonFilter, isIncludeQualified);
            json = newJson;
        } else {
            // If not using a filter, check to remove qualifiedId/Name.
            if (!isIncludeQualified) {
                boolean hasId = json.get("qualifiedId") != null;
                boolean hasName = json.get("qualifiedName") != null;
                if (hasId || hasName) {
                    json = NodeUtil.clone(json);
                    if (Debug.isOn())
                        Debug.outln("remove qualifiedId? " + hasId + ", remove qualifiedName? " + hasName);
                    if (hasId)
                        json.remove("qualifiedId");
                    if (hasName)
                        json.remove("qualifiedName");
                }
            }
        }

        // // add read time again -- otherwise, the cached read time is used
        // if ( !isExprOrProp && jsonFilter.contains( Acm.JSON_READ ) ) {
        // System.out.println("put read in newJson");
        // putInJson( newJson, Acm.JSON_READ, getIsoTime( new Date( readTime )
        // ), null );
        // }

        if (NodeUtil.doJsonDeepCaching) {
            if (version != null && versionLabel == null)
                versionLabel = version.getVersionLabel();
            json = NodeUtil.jsonDeepCachePut(json, getId(), millis, isIncludeQualified, jsonFilter, versionLabel);
        }

        if (Debug.isOn())
            Debug.outln("return json " + (json == null ? "null" : json.toString(4)));
        return json;
    }

    /**
     * Convert node into our custom JSONObject
     *
     * @param filter Set of keys that should be displayed (plus the mandatory fields)
     * @param isExprOrProp If true, does not add specialization key, as it is nested call to process
     *        the Expression operand or Property value
     * @param dateTime The time of the specialization, specifying the version. This should
     *        correspond the this EmsScriptNode's version, but that is not checked.
     * @param isIncludeQualified whether to include the qualified name and qualified id in the json
     * @return JSONObject serialization of node
     * @throws JSONException
     */
    public JSONObject toJSONObjectImplImpl(Set<String> filter, boolean isExprOrProp, WorkspaceNode ws, Date dateTime,
                    boolean isIncludeQualified, boolean isIncludeDocument, Version version) throws JSONException {
        JSONObject element = NodeUtil.newJsonObject();
        if (!exists())
            return element;
        JSONObject specializationJSON = new JSONObject();

        Long readTime = System.currentTimeMillis();

        if (isExprOrProp) {
            addSpecializationJSON(element, filter, ws, dateTime);
        } else {
            addElementJSON(element, filter, dateTime, isIncludeQualified, version);
            addSpecializationJSON(specializationJSON, filter, ws, dateTime);
            if (specializationJSON.length() > 0) {
                element.put(Acm.JSON_SPECIALIZATION, specializationJSON);
            }
        }

        // add read time
        if (!isExprOrProp) {
            putInJson(element, Acm.JSON_READ, getIsoTime(new Date(readTime)), filter);
        }


        // lets add in the document information
        if (isIncludeDocument) { // && NodeUtil.doGraphDb ) { // always use graph database for
                                 // documents
            JSONArray relatedDocuments = new JSONArray();

            // if document, just add itself as related doc, otherwise use postgres helper
            if (this.hasAspect(Acm.ACM_PRODUCT)) {
                String sysmlid = this.getSysmlId();
                JSONObject relatedDoc = new JSONObject();
                relatedDoc.put(Sjm.SYSMLID, sysmlid);
                relatedDoc.put("name", this.getSysmlName());
                relatedDoc.put("siteCharacterizationId", this.getSiteCharacterizationId(null, ws));
                JSONObject parentView = new JSONObject();
                parentView.put(Sjm.SYSMLID, sysmlid);
                parentView.put("name", this.getSysmlName());

                JSONArray parentViews = new JSONArray();
                parentViews.put(parentView);

                relatedDoc.put("parentViews", parentViews);

                relatedDocuments.put(relatedDoc);
            } else {
                try {
                    pgh = new PostgresHelper(workspace);

                    // need to recurse of getImmediateParents recursively, since root parent
                    // may not be a document.

                    Map<String, Set<String>> root2immediate = new HashMap<>();

                    Set<Pair<String, String>> immediateParents =
                                    pgh.getImmediateParents(this.getSysmlId(), DbEdgeTypes.VIEW);
                    Set<Pair<String, String>> viewImmediateParents = new HashSet<>();
                    for (Pair<String, String> immediateParent : immediateParents) {
                        viewImmediateParents.addAll(getDbGraphDoc(immediateParent, Acm.ACM_VIEW, pgh, null));
                    }
                    for (Pair<String, String> immediateParent : viewImmediateParents) {
                        Set<Pair<String, String>> rootIds = getDbGraphDoc(immediateParent, Acm.ACM_PRODUCT, pgh, null);
                        for (Pair<String, String> rootId : rootIds) {
                            if (!root2immediate.containsKey(rootId.first)) {
                                root2immediate.put(rootId.first, new HashSet<>());
                            }
                            root2immediate.get(rootId.first).add(immediateParent.first);
                        }
                    }

                    // create JSON by traversing root 2 immediate parents map
                    for (String rootParentId : root2immediate.keySet()) {
                        EmsScriptNode rootParentNode =
                                        NodeUtil.getNodeFromPostgresNode(pgh.getNodeFromSysmlId(rootParentId));

                        // transclusions are stored in postgres document graph, but may not
                        // actually show up in a document (can just dead end in a transclusion)
                        // so filter results based on this
                        if (rootParentNode.hasAspect(Acm.ACM_PRODUCT)) {
                            JSONObject relatedDoc = new JSONObject();
                            relatedDoc.put(Sjm.SYSMLID, rootParentId);
                            relatedDoc.put("name", rootParentNode.getProperty(Acm.ACM_NAME));
                            relatedDoc.put("siteCharacterizationId",
                                            rootParentNode.getSiteCharacterizationId(null, ws));

                            JSONArray parentViews = new JSONArray();
                            if (this.hasAspect(Acm.ACM_VIEW)) {
                                JSONObject parentView = new JSONObject();
                                parentView.put(Sjm.SYSMLID, this.getSysmlId());
                                parentView.put("name", this.getSysmlName());
                                parentViews.put(parentView);
                            } else {
                                for (String immediateParentId : root2immediate.get(rootParentId)) {
                                    EmsScriptNode immediateParentNode = NodeUtil
                                                    .getNodeFromPostgresNode(pgh.getNodeFromSysmlId(immediateParentId));
                                    JSONObject parentView = new JSONObject();
                                    parentView.put(Sjm.SYSMLID, immediateParentId);
                                    parentView.put("name", immediateParentNode.getProperty(Acm.ACM_NAME));
                                    parentViews.put(parentView);
                                }
                            }

                            relatedDoc.put("parentViews", parentViews);
                            relatedDocuments.put(relatedDoc);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (relatedDocuments.length() > 0) {
                element.put("relatedDocuments", relatedDocuments);
            }
        }

        // fix the artifact urls
        String elementString = element.toString();
        elementString = fixArtifactUrls(elementString, true);
        element = NodeUtil.newJsonObject(elementString);

        return element;
    }

    private Set<Pair<String, String>> getDbGraphDoc(Pair<String, String> child, String acmType, PostgresHelper pgh,
                    Set<String> visited) {
        Set<Pair<String, String>> result = new HashSet<>();
        if (visited == null) {
            visited = new HashSet<String>();
        }

        Set<Pair<String, String>> immediateParents = pgh.getImmediateParents(child.first, DbEdgeTypes.VIEW);

        for (Pair<String, String> immediateParent : immediateParents) {
            EmsScriptNode parentNode = new EmsScriptNode(new NodeRef(immediateParent.second), services, null);
            if (parentNode.hasAspect(acmType)) {
                result.add(immediateParent);
            } else {
                // break if we hit cycle, e.g. result already has the immediateParent
                if (!visited.contains(immediateParent.first)) {
                    visited.add(immediateParent.first);
                    result.addAll(getDbGraphDoc(immediateParent, acmType, pgh, visited));
                }
            }
        }

        return result;
    }

    public JSONObject toSimpleJSONObject(WorkspaceNode ws, Date dateTime) throws JSONException {
        JSONObject element = new JSONObject();
        element.put(Sjm.SYSMLID, getSysmlId());
        if (dateTime == null) {
            element.put("name", getSysmlName());
        } else {
            element.put("name", getSysmlName(dateTime));
        }
        JSONObject specializationJSON = new JSONObject();
        addSpecializationJSON(specializationJSON, null, ws, dateTime, true);
        if (specializationJSON.length() > 0) {
            element.put(Acm.JSON_SPECIALIZATION, specializationJSON);
        }
        return element;
    }

    public boolean isView() {
        boolean isView = hasAspect(Acm.ACM_VIEW) || hasAspect(Acm.ACM_PRODUCT);
        return isView;
    }

    public String getTypeName() {
        String typeName = null;

        for (String aspect : Acm.ACM_ASPECTS) {
            if (hasAspect(aspect)) {
                // statement below is safe if no ':' since -1 + 1 = 0
                typeName = aspect.substring(aspect.lastIndexOf(':') + 1);
                if (!Utils.isNullOrEmpty(typeName))
                    break;
            }
        }
        if (typeName == null) {
            // typeName = this.getQNameType().getLocalName();

            String acmType = getTypeShort();

            // Return type w/o sysml prefix:
            if (acmType != null) {
                typeName = Acm.getACM2JSON().get(acmType);
            }
        }
        return typeName;
    }

    public JSONArray getTargetAssocsIdsByType(String acmType) {
        boolean isSource = false;
        return getAssocsIdsByDirection(acmType, isSource);
    }

    public JSONArray getSourceAssocsIdsByType(String acmType) {
        boolean isSource = true;
        return getAssocsIdsByDirection(acmType, isSource);
    }

    /**
     * Returns a JSONArray of the sysml:ids of the found associations
     *
     * @param acmType
     * @param isSource
     * @return JSONArray of the sysml:ids found
     */
    protected JSONArray getAssocsIdsByDirection(String acmType, boolean isSource) {
        JSONArray array = new JSONArray();
        List<AssociationRef> assocs;
        if (isSource) {
            assocs = services.getNodeService().getSourceAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);
        } else {
            assocs = services.getNodeService().getTargetAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);
        }
        for (AssociationRef aref : assocs) {
            QName typeQName = createQName(acmType);
            if (aref.getTypeQName().equals(typeQName)) {
                NodeRef targetRef;
                if (isSource) {
                    targetRef = aref.getSourceRef();
                } else {
                    targetRef = aref.getTargetRef();
                }
                Object p = NodeUtil.getNodeProperty(targetRef, Acm.ACM_ID, services, true, true);
                array.put(p);
            }
        }

        return array;
    }

    public List<EmsScriptNode> getTargetAssocsNodesByType(String acmType, WorkspaceNode workspace, Date dateTime) {
        boolean isSource = false;
        return getAssocsNodesByDirection(acmType, isSource, workspace, dateTime);
    }

    public List<EmsScriptNode> getSourceAssocsNodesByType(String acmType, WorkspaceNode workspace, Date dateTime) {
        boolean isSource = true;
        return getAssocsNodesByDirection(acmType, isSource, workspace, dateTime);
    }

    /**
     * Get a list of EmsScriptNodes of the specified association type
     *
     * @param acmType
     * @param isSource
     * @param workspace
     * @param dateTime
     * @return
     */
    protected List<EmsScriptNode> getAssocsNodesByDirection(String acmType, boolean isSource, WorkspaceNode workspace,
                    Date dateTime) {
        List<EmsScriptNode> list = new ArrayList<>();
        List<AssociationRef> assocs;
        if (isSource) {
            assocs = services.getNodeService().getSourceAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);
        } else {
            assocs = services.getNodeService().getTargetAssocs(nodeRef, RegexQNamePattern.MATCH_ALL);
        }
        for (AssociationRef aref : assocs) {
            QName typeQName = createQName(acmType);
            if (aref.getTypeQName().equals(typeQName)) {
                NodeRef targetRef;
                if (isSource) {
                    targetRef = aref.getSourceRef();
                } else {
                    targetRef = aref.getTargetRef();
                }
                if (targetRef == null)
                    continue;
                if (dateTime != null || workspace != null) {
                    targetRef = NodeUtil.getNodeRefAtTime(targetRef, workspace, dateTime, true, false);
                }
                if (targetRef == null) {
                    String msg = "Error! Target of association " + aref + " did not exist in workspace "
                                    + WorkspaceNode.getName(workspace) + " at " + dateTime + ".\n";
                    if (getResponse() == null || getStatus() == null) {
                        logger.error(msg);
                    } else {
                        getResponse().append(msg);
                        getStatus().setCode(HttpServletResponse.SC_BAD_REQUEST, msg);
                    }
                    continue; // TODO -- error?!
                }
                list.add(new EmsScriptNode(targetRef, services, response));
            }
        }

        return list;
    }

    /**
     * Given an JSONObject, filters it to find the appropriate relationships to be provided into
     * model post TODO: filterRelationsJSONObject probably doesn't need to be in EmsScriptNode
     *
     * @param jsonObject
     * @return
     * @throws JSONException
     */
    public static JSONObject filterRelationsJSONObject(JSONObject jsonObject) throws JSONException {
        JSONObject relations = new JSONObject();
        JSONObject elementValues = new JSONObject();
        JSONObject propertyTypes = new JSONObject();
        JSONObject annotatedElements = new JSONObject();
        JSONObject relationshipElements = new JSONObject();
        JSONArray array;

        if (jsonObject.has(Acm.JSON_VALUE_TYPE)) {
            Object object = jsonObject.get(Acm.JSON_VALUE);
            if (object instanceof String) {
                array = new JSONArray();
                array.put(object);
            } else {
                array = jsonObject.getJSONArray(Acm.JSON_VALUE);
            }
            if (jsonObject.get(Acm.JSON_VALUE_TYPE).equals(Acm.JSON_ELEMENT_VALUE)) {
                elementValues.put(jsonObject.getString(Acm.JSON_ID), array);
            }
        }

        if (jsonObject.has(Acm.JSON_PROPERTY_TYPE)) {
            Object o = jsonObject.get(Acm.JSON_PROPERTY_TYPE);
            String propertyType = "" + o;// jsonObject.getString(Acm.JSON_PROPERTY_TYPE);
            if (!propertyType.equals("null")) {
                propertyTypes.put(jsonObject.getString(Acm.JSON_ID), propertyType);
            }
        }

        if (jsonObject.has(Acm.JSON_SOURCE) && jsonObject.has(Acm.JSON_TARGET)) {
            JSONObject relJson = new JSONObject();
            String source = jsonObject.getString(Acm.JSON_SOURCE);
            String target = jsonObject.getString(Acm.JSON_TARGET);
            relJson.put(Acm.JSON_SOURCE, source);
            relJson.put(Acm.JSON_TARGET, target);
            relationshipElements.put(jsonObject.getString(Acm.JSON_ID), relJson);
        } else if (jsonObject.has(Acm.JSON_ANNOTATED_ELEMENTS)) {
            array = jsonObject.getJSONArray(Acm.JSON_ANNOTATED_ELEMENTS);
            annotatedElements.put(jsonObject.getString(Acm.JSON_ID), array);
        }

        relations.put("annotatedElements", annotatedElements);
        relations.put("relationshipElements", relationshipElements);
        relations.put("propertyTypes", propertyTypes);
        relations.put("elementValues", elementValues);

        return relations;
    }

    public boolean isSite() {
        EmsScriptNode parent = getParent(null, getWorkspace(), false, true);
        return (parent != null && (parent.getName().toLowerCase().equals("sites") || isWorkspaceTop()));
    }

    /**
     * Retrieve the site folder containing this node. It is the parent folder contained by the Sites
     * folder. This does not look for site package nodes.
     *
     * @return the site folder containing this node
     */
    public EmsScriptNode getSiteNode(Date dateTime, WorkspaceNode ws) {
        if (siteNode != null)
            return siteNode;

        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !ADMIN_USER_NAME.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(ADMIN_USER_NAME);
        }

        EmsScriptNode parent = this;
        String parentName = parent.getName();
        Set<String> seen = new TreeSet<>();
        while (!parentName.equals("Models") && !seen.contains(parentName)) {
            if (seen.contains(parentName)) {
                logger.error("Folder " + parentName + " contains self!", new Exception());
                return null;
                // NodeRef ref = findNodeRefByType( "Sites",
                // SearchType.CM_NAME.prefix,
                // null, null, false );
                // if ( ref == null ) return null;
                // return new EmsScriptNode( ref, getServices() );
            }
            EmsScriptNode oldparent = parent;
            parent = oldparent.getOwningParent(dateTime, ws, false, true);
            if (parent == null)
                return null; // site not found!
            parentName = parent.getName();
            if (parentName.toLowerCase().equals("sites")) {
                siteNode = oldparent;
                if (changeUser) {
                    AuthenticationUtil.setRunAsUser(runAsUser);
                }
                return siteNode;
            }
        }
        // The site is the folder containing the Models folder!
        siteNode = parent.getOwningParent(dateTime, ws, false, true);

        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }
        return siteNode;
    }

    public EmsScriptNode getProjectNode(WorkspaceNode ws) {
        if (projectNode != null)
            return projectNode;

        EmsScriptNode parent = this;
        EmsScriptNode projectPkg = null;
        EmsScriptNode models = null;
        EmsScriptNode oldparent = null;
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !ADMIN_USER_NAME.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(ADMIN_USER_NAME);
        }
        Set<EmsScriptNode> seen = new HashSet<>();
        while (parent != null && parent.getSysmlId() != null && !seen.contains(parent)) {
            if (models == null && parent.getName().equals("Models")) {
                models = parent;
                projectPkg = oldparent;

                // IMPORTANT!! DON'T TAKE THIS OUT
                // EMS was pushed when all model data was in Project reified
                // node, not in
                // the Project reified project, so need to do both checks
                if (projectPkg.isSubType("sysml:Project")) {
                    projectNode = projectPkg;
                } else {
                    projectNode = projectPkg.getReifiedNode(ws);
                }
                break;
            }
            seen.add(parent);
            oldparent = parent;
            parent = parent.getParent(null, ws, false, true);
        }
        if (seen.contains(parent)) {
            String msg = "ERROR! recursive parent hierarchy detected for " + parent.getName() + " having visited "
                            + seen + ".\n";
            if (getResponse() == null || getStatus() == null) {
                Debug.error(msg);
            } else {
                getResponse().append(msg);
                getStatus().setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
            }
        }
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }
        return projectNode;
    }

    public String getProjectId(WorkspaceNode ws) {
        EmsScriptNode projectNode = getProjectNode(ws);
        if (projectNode == null || projectNode.getSysmlId() == null) {
            return "null";
        }
        return projectNode.getSysmlId().replace("_pkg", "");
    }

    private EmsScriptNode convertIdToEmsScriptNode(String valueId, boolean ignoreWorkspace, WorkspaceNode workspace,
                    Date dateTime) {
        return convertIdToEmsScriptNode(valueId, ignoreWorkspace, workspace, dateTime, services, response, status);
    }

    public static EmsScriptNode convertIdToEmsScriptNode(String valueId, boolean ignoreWorkspace,
                    WorkspaceNode workspace, Date dateTime, ServiceRegistry services, StringBuffer response,
                    Status status) {

        EmsScriptNode value = null;

        // REVIEW Is it safe to assume that a sysmlid of one element is equal to
        // the alfresco id of another?
        // First search by alfresco id:
        NodeRef ref = NodeUtil.findNodeRefByAlfrescoId(valueId, false, false);

        if (ref != null) {
            value = new EmsScriptNode(ref, services);
        }
        // If could not find it by alfresco id, then search by sysml id:
        else {
            ArrayList<NodeRef> refs = NodeUtil.findNodeRefsById(valueId, ignoreWorkspace, workspace, dateTime, services,
                            false, false);

            List<EmsScriptNode> nodeList = toEmsScriptNodeList(refs, services, response, status);

            value = (nodeList == null || nodeList.size() <= 0) ? null : nodeList.get(0);
        }

        return value;
    }

    /**
     * Update or create element values (multiple noderefs ordered in a list)
     *
     * @param jsonArray Array of the IDs that house the values for the element
     * @param acmProperty The property to update or create
     * @throws JSONException
     */
    public boolean createOrUpdateProperties(JSONArray array, String acmProperty) throws JSONException {
        boolean changed = false;
        // Need to check if we're trying to stuff an array into a single-valued
        // property. This happens with the contains and other properties of
        // view.
        DictionaryService dServ = services.getDictionaryService();
        PropertyDefinition propDef = dServ.getProperty(createQName(acmProperty));
        boolean singleValued = propDef != null && !propDef.isMultiValued();

        if (singleValued) {
            return createOrUpdateProperty(acmProperty, array.toString(4));
        }

        ArrayList<Serializable> values = getPropertyValuesFromJson(propDef, array, getWorkspace(), null);

        // special handling for valueType == ElementValue
        if (values == null) {
            if (logger.isDebugEnabled())
                logger.debug("null property values for " + acmProperty);
            if (Acm.ACM_ELEMENT_VALUE.equals(acmProperty)) {
                values = getPropertyValuesFromJson(PropertyType.NODE_REF, array, getWorkspace(), null);
            } else {
                return changed;
            }
        }

        // only change if old list is different than new
        if (checkPermissions(PermissionService.WRITE, response, status)) {
            // It is important we ignore the workspace when getting the
            // property, so we make sure
            // to update this property when needed. Otherwise, property may have
            // a noderef in
            // a parent workspace, and this wont detect it; however, all the
            // getProperty() will look
            // for the correct workspace node, so perhaps this is overkill::
            List<Serializable> oldValues = Utils.asList(getNodeRefProperty(acmProperty, true, null, false, true, null),
                            Serializable.class);
            if (!EmsScriptNode.checkIfListsEquivalent(values, oldValues)) {
                setProperty(acmProperty, values);
                changed = true;
            }
        } else {
            log("no write permissions " + id + "\n");
        }

        return changed;
    }

    public EmsScriptNode findScriptNodeByName(String id, boolean ignoreWorkspace, WorkspaceNode workspace,
                    Date dateTime) {
        return convertIdToEmsScriptNode(id, ignoreWorkspace, workspace, dateTime, services, response, status);
    }

    private enum PropertyType {
        INT, LONG, DOUBLE, BOOLEAN, TEXT, DATE, NODE_REF, UNKNOWN
    };

    /**
     * Get an ArrayList of property value objects of the proper type according to the property
     * definition.
     *
     * @param propDef the property definition
     * @param jsonArray the array of values in JSON
     * @param jsonKey the name of the property for
     * @return the list of properties
     * @throws JSONException
     */
    public ArrayList<Serializable> getPropertyValuesFromJson(PropertyDefinition propDef, JSONArray jsonArray,
                    WorkspaceNode workspace, Date dateTime) throws JSONException {
        // ArrayList<Serializable> properties = new ArrayList<Serializable>();

        if (propDef == null) {
            return null;
            // Object o = jsonObject.get( jsonKey );
            // if ( o instanceof Serializable ) return (Serializable)o;
            // return "" + o;
        }

        QName name = propDef.getDataType().getName();
        PropertyType type;

        if (name.equals(DataTypeDefinition.INT)) {
            type = PropertyType.INT;
        } else if (name.equals(DataTypeDefinition.LONG)) {
            type = PropertyType.LONG;
        } else if (name.equals(DataTypeDefinition.DOUBLE)) {
            type = PropertyType.DOUBLE;
        } else if (name.equals(DataTypeDefinition.BOOLEAN)) {
            type = PropertyType.BOOLEAN;
        } else if (name.equals(DataTypeDefinition.TEXT)) {
            type = PropertyType.TEXT;
            // properties of type date include timestamp and
            // creation/modified dates and are not stored by MMS
            // } else if ( name.equals( DataTypeDefinition.DATE ) ) {
            // type = PropertyType.DATE;
            // } else if ( name.equals( DataTypeDefinition.DATETIME ) ) {
            // type = PropertyType.DATE;
        } else if (name.equals(DataTypeDefinition.NODE_REF)) {
            type = PropertyType.NODE_REF;
        } else {
            type = PropertyType.UNKNOWN;
        }
        return getPropertyValuesFromJson(type, jsonArray, workspace, dateTime);
    }

    public ArrayList<Serializable> getPropertyValuesFromJson(PropertyType type, JSONArray jsonArray,
                    WorkspaceNode workspace, Date dateTime) throws JSONException {
        if (logger.isDebugEnabled())
            logger.debug("getPropertyValuesFromJson(" + type + ", " + jsonArray + ", " + dateTime + ")");

        ArrayList<Serializable> properties = new ArrayList<>();

        Serializable property;
        for (int i = 0; i < jsonArray.length(); ++i) {
            property = null;
            // The json object get methods will throw a NPE if the value is
            // null,
            // so must check for it
            if (!jsonArray.isNull(i)) {
                switch (type) {
                    case INT:
                        property = jsonArray.getInt(i);
                        break;
                    case LONG:
                        property = jsonArray.getLong(i);
                        break;
                    case DOUBLE:
                        property = jsonArray.getDouble(i);
                        break;
                    case BOOLEAN:
                        property = jsonArray.getBoolean(i);
                        break;
                    case TEXT:
                    case DATE:
                        try {
                            property = jsonArray.getString(i);
                        } catch (JSONException e) {
                            Object val = jsonArray.get(i);
                            property = val != null ? "" + val : null;
                        }
                        break;
                    case NODE_REF:
                        String sysmlId = null;
                        try {
                            sysmlId = jsonArray.getString(i);
                        } catch (JSONException e) {
                            Object val = jsonArray.get(i);
                            sysmlId = val != null ? "" + val : null;
                        }

                        if (!Utils.isNullOrEmpty(sysmlId)) {
                            EmsScriptNode node = convertIdToEmsScriptNode(sysmlId, false, workspace, dateTime);
                            if (node != null) {
                                property = node.getNodeRef();
                            } else {
                                String msg = "Error! No element found for " + sysmlId + ".\n";
                                if (getResponse() == null || getStatus() == null) {
                                    Debug.error(msg);
                                } else {
                                    getResponse().append(msg);
                                    getStatus().setCode(HttpServletResponse.SC_BAD_REQUEST, msg);
                                }
                                return null; // REVIEW this may be overkill, can
                                             // still proceed
                            }
                        }
                        // A null sysmId indicates we should store null as the
                        // property value
                        else {
                            property = null;
                        }
                        break;
                    case UNKNOWN:
                        property = jsonArray.getString(i);
                        break;
                    default:
                        String msg = "Error! Bad property type = " + type + ".\n";
                        if (getResponse() == null || getStatus() == null) {
                            Debug.error(msg);
                        } else {
                            getResponse().append(msg);
                            getStatus().setCode(HttpServletResponse.SC_BAD_REQUEST, msg);
                        }
                        return null;
                };
            }

            // Note: No harm comes from adding null to the array, as alfresco
            // wont store it
            properties.add(property);
        }
        return properties;
    }

    protected static Serializable badValue = new Serializable() {
        private static final long serialVersionUID = -357325810740259362L;
    };

    public Serializable getPropertyValueFromJson(PropertyDefinition propDef, JSONObject jsonObject, String jsonKey,
                    WorkspaceNode workspace, Date dateTime) throws JSONException {
        Serializable property = null;
        QName name = null;
        if (propDef != null) {
            name = propDef.getDataType().getName();
        } else {
            // skips property type
            return badValue;
            // Debug.error("*$*$*$ null prop def for " + jsonKey );
            // Object o = jsonObject.get( jsonKey );
            // if ( o instanceof Serializable ) return (Serializable)o;
            // return "" + o;
        }

        // The json object get methods will throw a NPE if the value is null,
        // so must check for it
        if (!jsonObject.isNull(jsonKey)) {
            if (name != null) {
                if (name.equals(DataTypeDefinition.INT)) {
                    property = jsonObject.getInt(jsonKey);
                } else if (name.equals(DataTypeDefinition.LONG)) {
                    property = jsonObject.getLong(jsonKey);
                } else if (name.equals(DataTypeDefinition.DOUBLE)) {
                    property = jsonObject.getDouble(jsonKey);
                } else if (name.equals(DataTypeDefinition.BOOLEAN)) {
                    property = jsonObject.getBoolean(jsonKey);
                } else if (name.equals(DataTypeDefinition.TEXT)) {
                    property = jsonObject.getString(jsonKey);
                    // properties of type date include timestamp and
                    // creation/modified dates and are not stored by MMS
                    // } else if ( name.equals( DataTypeDefinition.DATE ) ) {
                    // property = jsonObject.getString( jsonKey );
                    // } else if ( name.equals( DataTypeDefinition.DATETIME ) )
                    // {
                    // property = jsonObject.getString( jsonKey );
                } else if (name.equals(DataTypeDefinition.NODE_REF)) {
                    String sysmlId = null;
                    try {
                        sysmlId = jsonObject.getString(jsonKey);
                    } catch (JSONException e) {
                        Object val = jsonObject.get(jsonKey);
                        sysmlId = val != null ? "" + val : null;
                    }

                    if (!Utils.isNullOrEmpty(sysmlId)) {
                        EmsScriptNode node = convertIdToEmsScriptNode(sysmlId, false, workspace, dateTime);
                        if (node != null) {
                            property = node.getNodeRef();
                        } else {
                            String msg = "Error! Could not find element for sysml id = " + sysmlId + ".\n";
                            if (getResponse() == null || getStatus() == null) {
                                Debug.error(false, msg);
                            } else {
                                getResponse().append(msg);
                                getStatus().setCode(HttpServletResponse.SC_BAD_REQUEST, msg);
                            }
                        }
                    }
                    // A null sysmId indicates we should store null as the
                    // property value,
                    else {
                        property = null;
                    }

                } else {
                    property = jsonObject.getString(jsonKey);
                }
            } else {
                property = jsonObject.getString(jsonKey);
            }
        }

        // Per CMED-461, we are allowing properties to be set to null
        // if ( property == null ) {
        // String msg =
        // "Error! Couldn't get property " + propDef + "=" + property
        // + ".\n";
        // if ( getResponse() == null || getStatus() == null ) {
        // Debug.error( false, msg );
        // } else {
        // getResponse().append( msg );
        // getStatus().setCode( HttpServletResponse.SC_BAD_REQUEST, msg );
        // }
        // }
        return property;
    }

    /**
     * Update the node with the properties from the jsonObject
     *
     * @param jsonObject
     *
     *        return true if Element was changed, false otherwise
     * @throws JSONException
     */
    public boolean ingestJSON(JSONObject jsonObject) throws JSONException {
        boolean changed = false;
        // fill in all the properties
        if (logger.isDebugEnabled())
            logger.debug("ingestJSON(" + jsonObject + ")");

        DictionaryService dServ = services.getDictionaryService();

        Iterator<?> iter = jsonObject.keys();
        while (iter.hasNext()) {
            String key = "" + iter.next();
            String acmType = Acm.getJSON2ACM().get(key);
            if (Utils.isNullOrEmpty(acmType)) {
                // skips owner
                // Debug.error( "No content model type found for \"" + key +
                // "\"!" );
                continue;
            } else {
                QName qName = createQName(acmType);
                if (logger.isDebugEnabled()) {
                    if (acmType.equals(Acm.ACM_VALUE)) {
                        logger.debug("qName of " + acmType + " = " + qName.toString());
                    }
                }

                // If it is a specialization, then process the json object it
                // maps to:
                if (acmType.equals(Acm.ACM_SPECIALIZATION)) {

                    JSONObject specializeJson = jsonObject.getJSONObject(Acm.JSON_SPECIALIZATION);

                    if (specializeJson != null) {
                        if (logger.isDebugEnabled())
                            logger.debug("processing " + acmType);
                        if (ingestJSON(specializeJson)) {
                            changed = true;
                        }
                    }
                } else {
                    PropertyDefinition propDef = dServ.getProperty(qName);
                    if (propDef == null) {
                        if (logger.isDebugEnabled())
                            logger.debug("null PropertyDefinition for " + acmType);
                        continue; // skips type
                    }
                    boolean isArray = (Acm.JSON_ARRAYS.contains(key) || (propDef != null && propDef.isMultiValued()));
                    if (isArray) {
                        JSONArray array = jsonObject.getJSONArray(key);
                        if (createOrUpdateProperties(array, acmType)) {
                            changed = true;
                        }
                    } else {
                        // REVIEW -- Passing null for workspace; this assumes
                        // that the
                        // workspace of any NodeRefs referenced by the property
                        // are fixed
                        // by the caller.
                        Serializable propVal = getPropertyValueFromJson(propDef, jsonObject, key, getWorkspace(), null);
                        if (propVal == badValue) {
                            Debug.error("Got bad property value!");
                        } else {
                            if (createOrUpdateProperty(acmType, propVal)) {
                                changed = true;
                            }
                        }
                    }
                } // ends else (not a Specialization)
            }
        }

        return changed;
    }

    /**
     * Wrapper for replaceArtifactUrl with different patterns if necessary
     *
     * @param content
     * @param escape
     * @return
     */
    public String fixArtifactUrls(String content, boolean escape) {
        String result = content;
        result = replaceArtifactUrl(result, "src=\\\\\"/editor/images/docgen/",
                        "src=\\\\\"/editor/images/docgen/.*?\\\\\"", escape);

        return result;
    }

    /**
     * Utility method that replaces the image links with references to the repository urls
     *
     * @param content
     * @param prefix
     * @param pattern
     * @param escape
     * @return
     */
    public String replaceArtifactUrl(String content, String prefix, String pattern, boolean escape) {
        if (content == null) {
            return content;
        }

        String result = content;
        Pattern p = Pattern.compile(pattern);
        Matcher matcher = p.matcher(content);

        while (matcher.find()) {
            String filename = matcher.group(0);
            // not sure why this can't be chained correctly
            filename = filename.replace("\"", "");
            filename = filename.replace("_latest", "");
            filename = filename.replace("\\", "");
            filename = filename.replace("src=/editor/images/docgen/", "");
            NodeRef nodeRef = findNodeRefByType(filename, SearchType.CM_NAME.prefix, getWorkspace(), null, false);
            if (nodeRef != null) {
                // this should grab whatever is the latest versions purl - so
                // fine for snapshots
                EmsScriptNode node = new EmsScriptNode(nodeRef, getServices());
                NodeRef versionedNodeRef = node.getHeadVersion().getVersionedNodeRef();
                EmsScriptNode versionedNode = new EmsScriptNode(versionedNodeRef, services, response);
                String nodeurl = "";
                if (prefix.indexOf("src") >= 0) {
                    nodeurl = "src=\\\"";
                }
                // TODO: need to map context out in case we aren't at alfresco
                String context = "/alfresco";
                nodeurl += context + versionedNode.getUrl() + "\\\"";
                // this is service api for getting the content information
                nodeurl = nodeurl.replace("/d/d/", "/service/api/node/content/");
                result = result.replace(matcher.group(0), nodeurl);
            }
        }

        return result;
    }

    public EmsScriptNode getVersionAtTime(String timestamp) {
        return getVersionAtTime(TimeUtils.dateFromTimestamp(timestamp));
    }

    public EmsScriptNode getVersionAtTime(Date dateTime) {
        NodeRef versionedRef = NodeUtil.getNodeRefAtTime(getNodeRef(), dateTime);
        if (versionedRef == null) {
            return null;
            // return new EmsScriptNode(getNodeRef(), getServices());
        }
        return new EmsScriptNode(versionedRef, getServices());
    }

    protected NodeRef findNodeRefByType(String name, String type, WorkspaceNode workspace, Date dateTime,
                    boolean findDeleted) {
        return NodeUtil.findNodeRefByType(name, type, false, workspace, dateTime, true, services, findDeleted, null);
    }

    // protected static ResultSet findNodeRefsByType( String name, String type,
    // ServiceRegistry services ) {
    // return NodeUtil.findNodeRefsByType( name, type, services );
    // }

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
                Object property = getProperty(Acm.ACM_ID);
                if (property == null) {
                    property = getProperty(Acm.CM_NAME);
                }

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
     *
     * @return whether the user has the permission for this node
     *
     * @see org.alfresco.repo.jscript.ScriptNode#hasPermission(java.lang.String)
     */
    @Override
    public boolean hasPermission(String permission) {
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

    public static class EmsScriptNodeComparator implements Comparator<EmsScriptNode> {
        @Override
        public int compare(EmsScriptNode x, EmsScriptNode y) {
            Date xModified;
            Date yModified;

            xModified = x.getLastModified(null);
            yModified = y.getLastModified(null);

            if (xModified == null) {
                return -1;
            } else if (yModified == null) {
                return 1;
            } else {
                return (xModified.compareTo(yModified));
            }
        }
    }

    public static Date dateFromIsoTime(String timestamp) {
        return TimeUtils.dateFromTimestamp(timestamp);
    }

    public static String getIsoTime(Date date) {
        return TimeUtils.toTimestamp(date);
    }

    /**
     * Override equals for EmsScriptNodes
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return equals(obj, true);
    }

    /**
     * @return the head or current version of the node ref if it exists; otherwise return the
     *         existing node ref
     */
    public NodeRef normalizedNodeRef() {// NodeRef ref, ServiceRegistry services
                                        // ) {
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
    @Override
    public boolean exists() {
        return exists(false);
    }

    public boolean exists(boolean includeDeleted) {
        // if (NodeUtil.doFullCaching) {
        // // full caching doesn't cache permissions, just references to script
        // node
        // // so make sure we can read before doing actual check
        // if (!checkPermissions( "Read" ))
        // return false;
        // }
        if (!scriptNodeExists())
            return false;
        if (!includeDeleted && hasAspect("ems:Deleted")) {
            return false;
        }
        return true;
    }

    public boolean scriptNodeExists() {
        return super.exists();
    }

    public boolean isDeleted() {
        // if (NodeUtil.doFullCaching) {
        // // full caching doesn't cache permissions, just references to script
        // node
        // // so make sure we can read before doing actual check
        // if (!checkPermissions("Read")) return false;
        // }
        if (super.exists()) {
            return hasAspect("ems:Deleted");
        }
        // may seem counterintuitive, but if it doesn't exist, it isn't deleted
        return false;
    }

    /**
     * this is a soft delete that is used to internally track "deleted" elements
     */
    public void delete() {
        if (!isDeleted()) {
            makeSureNodeRefIsNotFrozen();
            createOrUpdateAspect("ems:Deleted");
        }
    }

    public boolean isFolder() {
        try {
            services.getNodeService().getType(this.getNodeRef());
        } catch (Throwable e) {
            logger.warn("Call to services.getNodeService().getType(nodeRef=" + this.getNodeRef() + ") for this = "
                            + this + " failed!");
            e.printStackTrace();
        }
        try {
            if (isSubType("cm:folder"))
                return true;
            return false;
        } catch (Throwable e) {
            logger.warn("Call to isSubType() on this = " + this + " failed!");
            e.printStackTrace();
        }
        try {
            QName type = null;
            type = parent.getQNameType();
            if (type != null && !services.getDictionaryService().isSubClass(type, ContentModel.TYPE_FOLDER)) {
                return true;
            }
            return false;
        } catch (Throwable e) {
            logger.warn("Trying to call getQNameType() on parent = " + parent + ".");
            e.printStackTrace();
        }
        try {
            String type = getTypeShort();
            if (type.equals("folder") || type.endsWith(":folder")) {
                return true;
            }
            return false;
        } catch (Throwable e) {
            logger.warn("Trying to call getQNameType() on parent = " + parent + ".");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public int compare(EmsScriptNode arg0, EmsScriptNode arg1) {
        if (arg0 == arg1)
            return 0;
        if (arg0 == null)
            return -1;
        else if (arg1 == null)
            return 1;
        return arg0.getNodeRef().getId().compareTo(arg1.getNodeRef().getId());
    }

    @Override
    public int compareTo(EmsScriptNode o) {
        return this.compare(this, o);
    }

    public ServiceRegistry getServices() {
        return services;
    }

    public WorkspaceNode getWorkspace() {
        if (workspace == null) {
            if (hasAspect("ems:HasWorkspace")) {
                // ems:workspace is workspace meta data so dont need
                // dateTime/workspace args
                NodeRef ref = (NodeRef) getNodeRefProperty("ems:workspace", null, null);
                if (ref != null) {
                    WorkspaceNode ws = new WorkspaceNode(ref, getServices());
                    workspace = ws;
                }
            }
        }
        return workspace;
    }

    /**
     * @return the sysml id (cm name) of the workspace
     */
    public String getWorkspaceName() {
        String workspaceName = null;
        EmsScriptNode ws = getWorkspace();
        if (ws != null) {
            workspaceName = ws.getName();
        }
        return workspaceName;
    }

    public void setWorkspace(WorkspaceNode workspace) {
        setWorkspace(workspace, null);
    }

    // Warning: this may not work if the cm:name is not unique!
    public NodeRef findSourceInParentWorkspace() {
        if (getWorkspace() == null)
            return null;
        WorkspaceNode parentWs = getParentWorkspace();
        // NodeRef r = NodeUtil.findNodeRefById( getSysmlId(), false, parentWs,
        // null, getServices(), false );
        ArrayList<NodeRef> refs =
                        NodeUtil.findNodeRefsById(getSysmlId(), false, parentWs, null, getServices(), false, false);
        NodeRef r = null;
        for (NodeRef ref : refs) {
            EmsScriptNode node = new EmsScriptNode(ref, getServices());
            EmsScriptNode parent1 = getParent(null, getWorkspace(), false, true);
            EmsScriptNode parent2 = node.getParent(null, parentWs, false, true);
            boolean failed = false;
            boolean e1 = NodeUtil.exists(parent1);
            boolean e2 = NodeUtil.exists(parent2);
            while (e1 && e2 && !parent1.isWorkspaceTop() && !parent2.isWorkspaceTop()) {
                // if ( parent1 == parent2 || ( grandParent != null && gp !=
                // null && grandParent.getName().equals( gp.getName() ) ) ) {
                if (!parent1.getName().equals(parent2.getName())) {
                    failed = true;
                    break;
                } else {
                    if (parent1.equals(parent2))
                        break;
                }
                parent1 = parent1.getParent(null, getWorkspace(), false, true);
                parent2 = parent2.getParent(null, parentWs, false, true);
                e1 = NodeUtil.exists(parent1);
                e2 = NodeUtil.exists(parent2);
            }
            if (!failed && e1 == e2 && (!e1 || parent1.isWorkspaceTop() == parent2.isWorkspaceTop())) {
                r = ref;
                break;
            }
        }
        return r;
    }

    // public NodeRef findSourceInParentWorkspace() {
    // EmsScriptNode node = this;
    // // make sure the folder's parent is replicated
    // EmsScriptNode parent = node.getParent();
    //
    // if ( parent == null || parent.isWorkspaceTop() ) {
    // parent = this; // put in the workspace
    // }
    // String parentName = parent != null && parent.exists() ? parent.getName()
    // : null;
    //
    // // Get the parent in this workspace. In case there are multiple nodes
    // // with the same cm:name, use the grandparent to disambiguate where it
    // // should be.
    // if ( parent != null && parent.exists() && !this.equals(
    // parent.getWorkspace() ) ) {
    // EmsScriptNode grandParent = parent.getParent();
    // ArrayList< NodeRef > arr = NodeUtil.findNodeRefsByType( parentName,
    // SearchType.CM_NAME.prefix, false, false, this, null, false, true,
    // getServices(), false );
    // for ( NodeRef ref : arr ) {
    // EmsScriptNode p = new EmsScriptNode( ref, getServices() );
    // EmsScriptNode gp = p.getParent();
    // if ( grandParent == gp || ( grandParent != null && gp != null &&
    // grandParent.getName().equals( gp.getName() ) ) ) {
    // parent = p;
    // break;
    // }
    // }
    //
    // if ( !this.equals( parent.getWorkspace() ) ) {
    // parent = replicateWithParentFolders( parent );
    // }
    // } else if ( parent == null || !parent.exists() ) {
    // Debug.error("Error! Bad parent when replicating folder chain! " + parent
    // );
    // }
    // }

    /**
     * @param workspace the workspace to set
     */
    public void setWorkspace(WorkspaceNode workspace, NodeRef source) {
        if (Debug.isOn()) {
            Debug.outln("setWorkspace( workspace=" + workspace + ", source=" + source + " ) for node " + this);
        }

        this.workspace = workspace;
        createOrUpdateAspect("ems:HasWorkspace");
        // ems:workspace is workspace meta data so dont need dateTime/workspace
        // args
        // QName qname = createQName( "ems:workspace" );
        // Serializable propInAlf = services.getNodeService().getProperty(
        // getNodeRef(), qname );
        // System.out.println(getNodeRef() + " workspace before setting: " +
        // propInAlf);
        NodeRef ref = (NodeRef) getNodeRefProperty("ems:workspace", null, null);
        if (workspace != null && !workspace.getNodeRef().equals(ref)) {
            setProperty("ems:workspace", workspace.getNodeRef());
            // System.out.println("set workspace");
        } else if (workspace == null && ref != null) {
            removeAspect("ems:HasWorkspace");
            // System.out.println("did not set workspace; removed HasWorkspace aspect");
        }
        // propInAlf = services.getNodeService().getProperty( getNodeRef(),
        // qname );
        // System.out.println(getNodeRef() + " workspace after setting: " +
        // propInAlf);

        if (source == null) {
            source = findSourceInParentWorkspace();
        }
        if (source != null) {
            setProperty("ems:source", source);
        }
    }

    /**
     * @return the parentWorkspace
     */
    public WorkspaceNode getParentWorkspace() {
        WorkspaceNode ws = getWorkspace();
        // if( ws == null)
        // return null;
        return ws.getParentWorkspace();
    }

    // delete later
    public WorkspaceNode getSourceWorkspace() {
        WorkspaceNode ws = getWorkspace();
        if (ws == null)
            return null;
        return ws.getSourceWorkspace();
    }

    public EmsScriptNode(NodeRef nodeRef, ServiceRegistry services) {
        super(nodeRef, services);
    }

    public EmsScriptNode clone(EmsScriptNode parent) {
        if (!exists()) {
            Debug.error(true, false, "Warning! cloning non-existent node!");
        }
        if (Debug.isOn()) {
            Debug.outln("making clone() of " + this + " under parent " + parent);
        }
        if (parent == null || !parent.exists()) {
            Debug.error("Error! Trying to clone a node under a bad parent: " + parent
                            + "; changing parent to node being cloned " + this);
            parent = this;
            if (!exists()) {
                Debug.error("Error! Can't clone under non-existent parent!");
            }
        }

        // create node of same type, except a site will be of type cm:folder.
        String type = getTypeShort();
        boolean isSiteOrSites = type.startsWith("st:site");
        if (isSiteOrSites) {
            type = "cm:folder";
        }

        parent.makeSureNodeRefIsNotFrozen();
        EmsScriptNode node = parent.createNode(getName(), type);
        // EmsScriptNode node = parent.createSysmlNode( getName(), type,
        // modStatus, workspace );

        if (node == null) {
            Debug.error("Could not create node in parent " + parent.getName());
            return null;
        }

        // add missing aspects
        NodeService nodeService = getServices().getNodeService();
        Set<QName> myAspects = nodeService.getAspects(getNodeRef());
        for (QName qName : myAspects) {
            if (qName == null)
                continue;
            node.createOrUpdateAspect(qName.toString());
        }

        // copy properties except those of a site.
        Map<QName, Serializable> properties = nodeService.getProperties(getNodeRef());
        if (isSiteOrSites) {
            properties.remove(createQName("st:sitePreset"));
            properties.remove(createQName("sys:undeletable"));
        }
        // makeSureNodeRefIsNotFrozen(); // Doesnt make sense to always call
        // this on "this"
        transactionCheck();
        NodeRef ref = node.getNodeRef();
        nodeService.setProperties(ref, properties);
        NodeUtil.propertyCachePut(ref, properties);

        // THIS MUST BE CALLED AFTER setProperties()!
        if (parent.getWorkspace() != null) {
            node.setWorkspace(parent.getWorkspace(), this.getNodeRef());
        }

        node.checkedNodeVersion = false;

        return node;
    }


    /**
     * Remove the property with the given name.
     *
     * @param acmProperty the name of the property in short format (e.g. sysml:value)
     * @return true if and only if the property was successfully removed
     */
    public boolean removeProperty(String acmProperty) {
        NodeService ns = getServices().getNodeService();
        try {
            makeSureNodeRefIsNotFrozen();
            ns.removeProperty(getNodeRef(), createQName(acmProperty));
            return true;
        } catch (InvalidNodeRefException e) {
            // ignore
        }
        return false;
    }

    public void appendToPropertyNodeRefs(String acmProperty, NodeRef ref) {
        if (checkPermissions(PermissionService.WRITE, response, status)) {
            // when doing set or append, we don't need the contextualized
            // nodeRef, so we can skipNodeRef
            ArrayList<NodeRef> relationships = getPropertyNodeRefs(acmProperty, true, null, null);
            if (Utils.isNullOrEmpty(relationships)) {
                relationships = Utils.newList(ref);
            } else if (!relationships.contains(ref)) {
                relationships.add(ref);
            }
            setProperty(acmProperty, relationships);
        } else {
            log("no write permissions to append " + acmProperty + " to " + id + "\n");
        }
    }

    // TODO -- It would be nice to return a boolean here. Same goes to many of
    // the callers of this method.
    public void removeFromPropertyNodeRefs(String acmProperty, NodeRef ref) {
        if (checkPermissions(PermissionService.WRITE, response, status)) {
            // when doing set or append, we don't need the contextualized
            // nodeRef, so we can skipNodeRef
            ArrayList<NodeRef> relationships = getPropertyNodeRefs(acmProperty, true, null, null);
            if (!Utils.isNullOrEmpty(relationships)) {
                relationships.remove(ref);
                setProperty(acmProperty, relationships);
            }
        } else {
            log("no write permissions to remove " + acmProperty + " from " + id + "\n");
        }
    }

    @Override
    public boolean move(ScriptNode destination) {

        boolean status = false;
        EmsScriptNode parent = getParent(null, getWorkspace(), false, true);
        EmsScriptNode oldParentReifiedNode = parent.getReifiedNode(parent.getWorkspace());

        // Create new parent if the parent is not correct:
        if (!parent.equals(destination)) {

            // in a move we need to track the parent, the current node, and the
            // destination, just in case
            parent.makeSureNodeRefIsNotFrozen();
            makeSureNodeRefIsNotFrozen();
            EmsScriptNode dest = new EmsScriptNode(destination.getNodeRef(), services, response);
            dest.makeSureNodeRefIsNotFrozen();
            status = super.move(dest);

            if (status) {
                // keep track of owners and children
                if (oldParentReifiedNode != null) {
                    oldParentReifiedNode.removeFromPropertyNodeRefs("ems:ownedChildren", this.getNodeRef());
                }

                EmsScriptNode newParent = dest;

                if (newParent != null) {
                    setOwnerToReifiedNode(newParent, newParent.getWorkspace(), false);
                }

                // make sure to move package as well
                EmsScriptNode reifiedPkg = getReifiedPkg(null, getWorkspace());
                if (reifiedPkg != null) {
                    reifiedPkg.move(destination);
                }

                moved = true;
                // removeChildrenFromJsonCache();
            }

        }
        // The parent was equal, but may need to update owner/ownedChildren in
        // case the parent was
        // cloned already in this workspace:
        else {
            // REVIEW -- Can we pass true in for skipNodeRefCheck below?
            EmsScriptNode owningParent = getOwningParent(null, getWorkspace(), false);
            if (oldParentReifiedNode != null && !oldParentReifiedNode.equals(owningParent)) {
                owningParent.removeFromPropertyNodeRefs("ems:ownedChildren", this.getNodeRef());
                setOwnerToReifiedNode(parent, parent.getWorkspace(), false);
                status = true;
            }

            // moved = true;

            // removeChildrenFromJsonCache();
        }

        return status;
    }

    /**
     * Recursively get values from a potentially nested map, returning only leaf values, values that
     * are not maps.
     *
     * @param map
     * @return leaf values from the nested map
     */
    public static <T> List<T> getValues(Map<?, ?> map, Class<T> cls) {
        ArrayList<T> arr = new ArrayList<T>();
        if (map == null)
            return arr;
        for (Object v : map.values()) {
            if (v == null)
                continue;
            if (cls != null && cls.isInstance(v)) {
                arr.add((T) v); // This is safe--ignore warning.
            } else if (v instanceof Map) {
                arr.addAll(getValues((Map<?, ?>) v, cls));
            } else {
                if (cls != null && !cls.isInstance(v))
                    continue;
                try {
                    T t = (T) v; // This is safe--ignore warning.
                    arr.add(t);
                } catch (ClassCastException e) {
                }
            }
        }
        return arr;
    }

    public static void removeFromJsonCache(NodeRef ref) {
        // NodeRef ref = node.getNodeRef();
        Map<Long, JSONObject> oldEntries = NodeUtil.jsonCache.remove(ref.getId());
        List<JSONObject> removedJson = null;
        if (NodeUtil.doJsonStringCaching) {
            removedJson = getValues(oldEntries, JSONObject.class);
        }
        if (NodeUtil.doJsonDeepCaching) {
            Map<Long, Map<Boolean, Map<Set<String>, Map<String, JSONObject>>>> oldEntriesDeep =
                            NodeUtil.jsonDeepCache.remove(ref.getId());
            if (NodeUtil.doJsonStringCaching) {
                removedJson.addAll(getValues(oldEntriesDeep, JSONObject.class));
            }
        }
        if (NodeUtil.doJsonStringCaching) {
            Utils.removeAll(NodeUtil.jsonStringCache, removedJson);
        }

    }

    static boolean delayed = true;

    public void removeFromJsonCache(boolean recursive) {
        removeFromJsonCache(getNodeRef());
        if (recursive)
            removeChildrenFromJsonCache(recursive);
    }

    public void removeChildrenFromJsonCache(boolean recursive) {
        if (!NodeUtil.doJsonCaching)
            return;
        // No need to pass dateTime/workspace b/c Brad says so
        ArrayList<NodeRef> childs = getOwnedChildren(true, null, null);
        for (NodeRef ref : childs) {
            // FIXME: graph db uses noderef and versioned ref, so need to remove both
            removeFromJsonCache(ref);
            if (recursive) {
                EmsScriptNode n = new EmsScriptNode(ref, getServices());
                n.removeChildrenFromJsonCache(true);
            }
        }
    }

    // // HERE!! REVIEW -- Is this right?
    // public Object getPropertyValue( String propertyName ) {
    // // Debug.error("ERROR! EmsScriptNode.getPropertyValue() doesn't work!");
    // Object o = getProperty( propertyName );
    // Object value = o; // default if case is not handled below
    //
    // if ( o instanceof NodeRef ) {
    // EmsScriptNode property =
    // new EmsScriptNode( (NodeRef)o, getServices() );
    // if ( property.hasAspect( "Property" ) ) {
    // value = property.getProperty( "value" );
    // }
    // }
    // return value;
    // }

    public static class NodeByTypeComparator implements Comparator<Object> {
        public static final NodeByTypeComparator instance = new NodeByTypeComparator();

        @Override
        public int compare(Object o1, Object o2) {
            if (o1 == o2)
                return 0;
            if (o1 == null)
                return -1;
            if (o2 == null)
                return 1;
            if (o1.equals(o2))
                return 0;
            String type1 = null;
            String type2 = null;
            if (o1 instanceof String) {
                type1 = (String) o1;
            } else if (o1 instanceof NodeRef) {
                EmsScriptNode n1 = new EmsScriptNode((NodeRef) o1, NodeUtil.getServices());
                type1 = n1.getTypeName();
            }
            if (o2 instanceof String) {
                type2 = (String) o2;
            } else if (o2 instanceof NodeRef) {
                EmsScriptNode n2 = new EmsScriptNode((NodeRef) o2, NodeUtil.getServices());
                type2 = n2.getTypeName();
            }
            int comp = CompareUtils.GenericComparator.instance().compare(type1, type2);
            if (comp != 0)
                return comp;
            if (o1.getClass() != o2.getClass()) {
                if (o1.getClass().equals(String.class)) {
                    return -1;
                }
                if (o2.getClass().equals(String.class)) {
                    return 1;
                }
                type1 = o1.getClass().getSimpleName();
                type2 = o2.getClass().getSimpleName();
                comp = CompareUtils.GenericComparator.instance().compare(type1, type2);
                return comp;
            }
            comp = CompareUtils.GenericComparator.instance().compare(o1, o2);
            return comp;
        }

    }

    public ArrayList<NodeRef> getPropertyNodeRefs(String acmProperty, Date date, WorkspaceNode ws) {
        return getPropertyNodeRefs(acmProperty, false, date, ws);
    }

    public ArrayList<NodeRef> getPropertyNodeRefs(String acmProperty, boolean skipNodeRefCheck, Date date,
                    WorkspaceNode ws) {
        return getPropertyNodeRefs(acmProperty, false, date, false, skipNodeRefCheck, ws);
    }

    public ArrayList<NodeRef> getPropertyNodeRefs(String acmProperty, boolean ignoreWorkspace, Date dateTime,
                    boolean findDeleted, boolean skipNodeRefCheck, WorkspaceNode ws) {
        Object o = getNodeRefProperty(acmProperty, ignoreWorkspace, dateTime, findDeleted, skipNodeRefCheck, ws);
        ArrayList<NodeRef> refs = null;
        if (!(o instanceof Collection)) {
            if (o instanceof NodeRef) {
                refs = new ArrayList<>();
                refs.add((NodeRef) o);
            } else {
                // return Utils.getEmptyArrayList(); // FIXME The array returned
                // here is later modified, and a common empty array is
                // is used. This makes it non-empty.
                return new ArrayList<NodeRef>();

            }
        } else {
            refs = Utils.asList((Collection<?>) o, NodeRef.class, false);
        }
        return refs;
    }

    public EmsScriptNode getPropertyElement(String acmProperty, Date date, WorkspaceNode ws) {
        return getPropertyElement(acmProperty, false, date, ws);
    }

    public List<EmsScriptNode> getPropertyElements(String acmProperty, Date date, WorkspaceNode ws) {
        List<NodeRef> refs = getPropertyNodeRefs(acmProperty, date, ws);
        List<EmsScriptNode> elements = new ArrayList<>();
        for (NodeRef ref : refs) {
            elements.add(new EmsScriptNode(ref, services));
        }
        return elements;
    }

    public EmsScriptNode getPropertyElement(String acmProperty, boolean skipNodeRefCheck, Date date, WorkspaceNode ws) {
        Object e = getNodeRefProperty(acmProperty, skipNodeRefCheck, date, ws);
        if (e instanceof NodeRef) {
            return new EmsScriptNode((NodeRef) e, getServices());
        } else if (e == null) {
        } else {
            Debug.error(true, false, "ERROR! Getting a property as a noderef!");
        }
        return null;
    }

    public Set<EmsScriptNode> getRelationships(Date date, WorkspaceNode ws) {
        Set<EmsScriptNode> set = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.entrySet()) {
            set.addAll(getPropertyElements(e.getValue(), date, ws));
        }
        return set;
    }

    public Set<EmsScriptNode> getRelationshipsOfType(String typeName, Date dateTime, WorkspaceNode ws) {
        Set<EmsScriptNode> set = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.entrySet()) {
            ArrayList<EmsScriptNode> relationships = getRelationshipsOfType(typeName, e.getKey(), dateTime, ws);
            if (!Utils.isNullOrEmpty(relationships)) {
                set.addAll(relationships);
            }
        }
        return set;
    }

    public ArrayList<EmsScriptNode> getRelationshipsOfType(String typeName, String acmAspect, Date dateTime,
                    WorkspaceNode ws) {
        if (!hasAspect(acmAspect))
            return new ArrayList<>();
        String acmProperty = Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.get(acmAspect);
        ArrayList<NodeRef> relationships = getPropertyNodeRefs(acmProperty, dateTime, ws);
        // Searching for the beginning of the relationships with this typeName
        // b/c relationships are ordered by typeName. Therefore, the search
        // is expected to not find a matching element.
        int index = Collections.binarySearch(relationships, typeName,
                        // this.getNodeRef(),
                        NodeByTypeComparator.instance);
        if (Debug.isOn())
            Debug.outln("binary search returns index " + index);
        if (index >= 0) {
            Debug.error(true, true, "Index " + index + " for search for " + typeName + " in " + relationships
                            + " should be negative!");
        }
        if (index < 0) {
            // binarySearch returns index = -(insertion point) - 1
            // So, insertion point = -index - 1.
            index = -index - 1;
        }
        ArrayList<EmsScriptNode> matches = new ArrayList<>();
        for (; index < relationships.size(); ++index) {
            NodeRef ref = relationships.get(index);
            EmsScriptNode node = new EmsScriptNode(ref, getServices());
            int comp = typeName.compareTo(node.getTypeName());
            if (comp < 0)
                break;
            if (comp == 0) {
                matches.add(node);
            }
        }
        return matches;
    }

    /**
     * Add the relationship NodeRef to an assumed array of NodeRefs in the specified property.
     *
     * @param relationship
     * @param acmProperty
     * @return true if the NodeRef is already in the property or was successfully added.
     */
    public boolean addRelationshipToProperty(NodeRef relationship, String acmAspect) {
        createOrUpdateAspect(acmAspect);
        String acmProperty = Acm.PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS.get(acmAspect);
        // Skip the check for dateTime and workspace since this operation
        // assumes that the context has already been resolved.
        ArrayList<NodeRef> relationships = getPropertyNodeRefs(acmProperty, true, null, null);
        int index = Collections.binarySearch(relationships,
                        // this.getNodeRef(),
                        relationship, NodeByTypeComparator.instance);
        if (Debug.isOn())
            Debug.outln("binary search returns index " + index);
        if (index >= 0) {
            // the relationship is already in the list, so nothing to do.
            return true;
        }
        // binarySearch returns index = -(insertion point) - 1
        // So, insertion point = -index - 1.
        index = -index - 1;
        if (Debug.isOn())
            Debug.outln("index converted to lowerbound " + index);
        if (index < 0) {
            Debug.error(true, true, "Error! Expecting an insertion point >= 0 but got " + index + "!");
            return false;
        } else if (index > relationships.size()) {
            Debug.error(true, true, "Error! Insertion point is beyond the length of the list: point = " + index
                            + ", length = " + relationships.size());
            return false;
        } else {
            relationships.add(index, relationship);
        }

        setProperty(acmProperty, relationships);
        return true;
    }

    public void addRelationshipToPropertiesOfParticipants(WorkspaceNode ws) {
        if (hasAspect(Acm.ACM_DIRECTED_RELATIONSHIP) || hasAspect(Acm.ACM_DEPENDENCY) || hasAspect(Acm.ACM_EXPOSE)
                        || hasAspect(Acm.ACM_CONFORM) || hasAspect(Acm.ACM_GENERALIZATION)) {

            // No need to pass a date since this is called in the context of
            // updating a node, so the time is the current time (which is null).
            NodeRef source = (NodeRef) getNodeRefProperty(Acm.ACM_SOURCE, null, ws);
            NodeRef target = (NodeRef) getNodeRefProperty(Acm.ACM_TARGET, null, ws);

            if (source != null) {
                EmsScriptNode sNode = new EmsScriptNode(source, services);
                sNode.addRelationshipToProperty(getNodeRef(), Acm.ACM_RELATIONSHIPS_AS_SOURCE);
            }
            if (target != null) {
                EmsScriptNode tNode = new EmsScriptNode(target, services);
                tNode.addRelationshipToProperty(getNodeRef(), Acm.ACM_RELATIONSHIPS_AS_TARGET);
            }
        }
    }

    private Set<QName> getAllAspectsAndInherited() {
        Set<QName> aspects = new LinkedHashSet<>();
        aspects.addAll(getAspectsSet());
        ArrayList<QName> queue = new ArrayList<>(aspects);
        DictionaryService ds = getServices().getDictionaryService();
        while (!queue.isEmpty()) {
            QName a = queue.get(0);
            queue.remove(0);
            AspectDefinition aspect = ds.getAspect(a);
            if (aspect != null) {
                QName p = aspect.getParentName();
                if (p != null && !aspects.contains(p)) {
                    aspects.add(p);
                    queue.add(p);
                }
            }
        }
        return aspects;
    }

    public boolean hasOrInheritsAspect(String aspectName) {
        if (hasAspect(aspectName))
            return true;
        QName qName = NodeUtil.createQName(aspectName);
        return getAllAspectsAndInherited().contains(qName);
        // QName qn = NodeUtil.createQName( aspectName );
        // if ( getAspectsSet().contains( qn ) ) return true;
        //
        // DictionaryService ds = getServices().getDictionaryService();
        // AspectDefinition aspect = ds.getAspect( qn );
        //
        // //aspect.
        //
        // NodeService ns = NodeUtil.getServices().getNodeService();
        // ns.ge
        // if ( ns.hasAspect( getNodeRef(), NodeUtil.createQName( aspectName ) )
        // ) {
        //
        // }
        // return false;
    }

    /**
     * Changes the aspect of the node to the one specified, taking care to save off and re-apply
     * properties from current aspect if downgrading. Handles downgrading to a Element, by removing
     * all the needed aspects. Also removing old sysml aspects if changing the sysml aspect.
     *
     * @param aspectName The aspect to change to
     */
    private boolean changeAspect(String aspectName) {

        Set<QName> aspects = new LinkedHashSet<>();
        boolean retVal = false;
        Map<String, Object> oldProps = null;
        DictionaryService dServ = services.getDictionaryService();
        AspectDefinition aspectDef;
        boolean saveProps = false;

        if (aspectName == null) {
            return false;
        }

        QName qName = NodeUtil.createQName(aspectName);

        // If downgrading to an Element, then need to remove
        // all aspects without saving any properties or adding
        // any aspects:
        if (aspectName.equals(Acm.ACM_ELEMENT)) {
            for (String aspect : Acm.ACM_ASPECTS) {
                if (hasAspect(aspect)) {
                    boolean myRetVal = removeAspect(aspect);
                    retVal = retVal || myRetVal;
                }
            }

            return retVal;
        }

        // Get all the aspects for this node, find all of their parents, and see
        // if
        // the new aspect is any of the parents.
        makeSureNodeRefIsNotFrozen();
        aspects.addAll(getAspectsSet());
        ArrayList<QName> queue = new ArrayList<>(aspects);
        QName name;
        QName parentQName;
        while (!queue.isEmpty()) {
            name = queue.get(0);
            queue.remove(0);
            aspectDef = dServ.getAspect(name);
            parentQName = aspectDef.getParentName();
            if (parentQName != null) {
                if (parentQName.equals(qName)) {
                    saveProps = true;
                    break;
                }
                if (!queue.contains(parentQName)) {
                    queue.add(parentQName);
                }
            }
        }

        // If changing aspects to a parent aspect (ie downgrading), then we must
        // save off the
        // properties before removing the current aspect, and then re-apply
        // them:
        if (saveProps) {
            oldProps = getProperties();
        }
        // No need to go any further if it already has the aspect and the new
        // aspect is not
        // a parent of the current aspect:
        else if (hasAspect(aspectName)) {
            return false;
        }

        // Remove all the existing sysml aspects if the aspect is not a parent
        // of the new aspect,
        // and it is a sysml aspect:
        List<String> sysmlAspects = Arrays.asList(Acm.ACM_ASPECTS);
        if (sysmlAspects.contains(aspectName)) {

            Set<QName> parentAspectNames = new LinkedHashSet<>();
            parentAspectNames.add(qName);
            name = qName; // The new aspect QName
            while (name != null) {
                aspectDef = dServ.getAspect(name);
                parentQName = aspectDef.getParentName();
                if (parentQName != null) {
                    parentAspectNames.add(parentQName);
                }
                name = parentQName;
            }

            for (String aspect : Acm.ACM_ASPECTS) {
                if (hasAspect(aspect) && !parentAspectNames.contains(NodeUtil.createQName(aspect))) {
                    boolean removeVal = removeAspect(aspect);
                    retVal = retVal || removeVal;
                }
            }
        }

        // Apply the new aspect if needed:
        if (!hasAspect(aspectName)) {
            // if makeSureNodeRefIsNotFrozen() is called earlier, below is not
            // necessary
            makeSureNodeRefIsNotFrozen();
            retVal = addAspect(aspectName);
        }

        // Add the saved properties if needed:
        if (oldProps != null) {
            aspectDef = dServ.getAspect(qName);
            Set<QName> aspectProps = aspectDef.getProperties().keySet();

            // Only add the properties that are valid for the new aspect:
            String propName;
            QName propQName;
            for (Entry<String, Object> entry : oldProps.entrySet()) {
                propName = entry.getKey();
                propQName = NodeUtil.createQName(propName);

                if (aspectProps.contains(propQName)) {
                    setProperty(propName, (Serializable) entry.getValue());
                }
            }
        }

        return retVal;
    }

    @Override
    public Set<QName> getAspectsSet() {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !ADMIN_USER_NAME.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(ADMIN_USER_NAME);
        }
        Set<QName> set = super.getAspectsSet();
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }
        return set;
    }

    public boolean isWorkspace() {
        return hasAspect("ems:Workspace");
    }

    public boolean isWorkspaceTop() {
        return isWorkspaceTop(null);
    }

    public boolean isWorkspaceTop(Date dateTime) {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !ADMIN_USER_NAME.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(ADMIN_USER_NAME);
        }

        boolean isTop = false;
        EmsScriptNode myParent = getParent(dateTime, getWorkspace(), false, true);
        if (myParent == null) {
            if (Debug.isOn()) {
                Debug.outln("isWorkspaceTop() = true for node with null parent: " + this);
            }
            isTop = true;
        } else if (myParent.isWorkspace()) {
            if (Debug.isOn()) {
                Debug.outln("isWorkspaceTop() = true for since node is a workspace: " + this);
            }
            isTop = true;
        } else if (equals(getCompanyHome())) {
            if (Debug.isOn()) {
                Debug.outln("isWorkspaceTop() = true for company home node: " + this);
            }
            isTop = true;
        } else if (Debug.isOn()) {
            Debug.outln("isWorkspaceTop() = false for node " + this);
        }

        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }
        return isTop;
    }

    // private String getSysmlIdOfProperty( String propertyName ) {
    // NodeRef elementRef = (NodeRef)this.getProperty( propertyName );
    // return getSysmlIdFromNodeRef( elementRef );
    // }

    private String getSysmlIdFromNodeRef(NodeRef nodeRef) {
        if (nodeRef != null) {
            EmsScriptNode node = new EmsScriptNode(nodeRef, services, response);
            if (node != null && node.exists()) {
                return node.getSysmlId();
            }
        }
        return null;
    }

    private ArrayList<String> getSysmlIdsFromNodeRefs(ArrayList<NodeRef> nodeRefs) {
        ArrayList<String> ids = new ArrayList<>();
        if (nodeRefs != null) {
            for (NodeRef nodeRef : nodeRefs) {
                String id = getSysmlIdFromNodeRef(nodeRef);
                // Allowing id of null per CMED-461:
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * TODO: make this static since it doesn't depend on the node calling it
     *
     * @param nRef
     * @param ws
     * @param dateTime
     * @param jsonArray
     * @throws JSONException
     */
    private void addVersionToArray(NodeRef nRef, WorkspaceNode ws, Date dateTime, JSONArray jsonArray)
                    throws JSONException {
        EmsScriptNode node = new EmsScriptNode(nRef, getServices(), getResponse());
        if (dateTime != null) {
            node = node.findScriptNodeByName(node.getSysmlId(), false, ws, dateTime);
        }
        if (node != null && node.exists()) {
            jsonArray.put(node.toJSONObject(null, true, ws, dateTime, false, false, null, null));
        }
    }

    /**
     * Add json embedded in
     *
     * @param nodeRefs
     * @param dateTime
     * @return
     * @throws JSONException
     */
    private Object addInternalJSON(Object nodeRefs, WorkspaceNode ws, Date dateTime) throws JSONException {
        if (nodeRefs == null) {
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        if (nodeRefs instanceof Collection) {
            Collection<NodeRef> nodeRefColl = (Collection<NodeRef>) nodeRefs;
            for (NodeRef nRef : nodeRefColl) {
                if (nRef != null) {
                    addVersionToArray(nRef, ws, dateTime, jsonArray);
                }
            }
        } else if (nodeRefs instanceof NodeRef) {
            addVersionToArray((NodeRef) nodeRefs, ws, dateTime, jsonArray);
            return jsonArray.length() > 0 ? jsonArray.get(0) : null;
        }
        return jsonArray;
    }

    private JSONArray addNodeRefIdsJSON(ArrayList<NodeRef> nodeRefs) {
        ArrayList<String> nodeIds = getSysmlIdsFromNodeRefs(nodeRefs);
        JSONArray ids = new JSONArray();
        for (String nodeId : nodeIds) {
            // Per CMED-461, allowing null; however, alfresco will never store
            // this
            // so this code is essentially dead.
            if (nodeId == null) {
                ids.put(JSONObject.NULL);
            } else {
                ids.put(nodeId);
            }
        }
        return ids;
    }

    private Object addNodeRefIdJSON(NodeRef nodeRef) {
        String id = getSysmlIdFromNodeRef(nodeRef);
        // Per CMED-461, allowing null:
        return id == null ? JSONObject.NULL : id;
    }

    /***************************************************************************************
     *
     * Methods that follow are called reflectively to add aspect metadata to JSON object.
     *
     * Follows same order as aspects in sysmlModel.xml. Use protected so warnings about unused
     * private methods don't occur.
     *
     **************************************************************************************/
    protected void addPackageJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        putInJson(json, Acm.JSON_IS_SITE, node.getProperty(Acm.ACM_IS_SITE), filter);
    }

    protected void addViewpointJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {

        // Dont need the correct workspace b/c sysml ids are immutable:
        NodeRef methodNode = (NodeRef) node.getNodeRefProperty(Acm.ACM_METHOD, true, dateTime, node.getWorkspace());
        json.put(Acm.JSON_METHOD, addNodeRefIdJSON(methodNode));
    }

    protected void addViewJSON(JSONObject json, EmsScriptNode node, Set<String> filter, WorkspaceNode ws, Date dateTime)
                    throws JSONException {
        String property;
        property = (String) node.getProperty("view2:contains");
        boolean noFilter = filter == null || filter.size() == 0;
        if (expressionStuff && (property == null || property.length() <= 0)) {
//            if (noFilter || filter.contains("contains")) {
//                json.put("contains", getView().getContainsJson(true, dateTime, ws));
//            }
//            JSONArray displayedElements = null;
//            if (noFilter || filter.contains("displayedElements")) {
//                displayedElements = toJsonArrayOfSysmlIds(getView().getDisplayedElements());
//                json.put("displayedElements", displayedElements);
//            }
//            if (noFilter || filter.contains("allowedElements")) {
//                if (displayedElements == null) {
//                    displayedElements = toJsonArrayOfSysmlIds(getView().getDisplayedElements());
//                }
//                json.put("allowedElements", displayedElements);
//            }
//            if (noFilter || filter.contains("childrenViews")) {
//                JSONArray childViews = toJsonArrayOfSysmlIds(getNodesOfViews(getView().getChildViews()));
//                json.put("childrenViews", childViews);
//            }
        } else {
            if (!Utils.isNullOrEmpty(property)) {
                putInJson(json, "contains", new JSONArray(property), filter);
            }
            property = (String) node.getProperty("view2:displayedElements");
            if (!Utils.isNullOrEmpty(property)) {
                putInJson(json, "displayedElements", new JSONArray(property), filter);
            }
            property = (String) node.getProperty("view2:allowedElements");
            if (!Utils.isNullOrEmpty(property)) {
                putInJson(json, "allowedElements", new JSONArray(property), filter);
            }
            property = (String) node.getProperty("view2:childrenViews");
            if (!Utils.isNullOrEmpty(property)) {
                putInJson(json, "childrenViews", new JSONArray(property), filter);
            }
        }
        // TODO: Snapshots?
        NodeRef contentsNode = (NodeRef) node.getNodeRefProperty(Acm.ACM_CONTENTS, dateTime, ws);
        putInJson(json, Acm.JSON_CONTENTS, addInternalJSON(contentsNode, ws, dateTime), filter);

    }

    public ArrayList<EmsScriptNode> getNodesOfViews(Collection<sysml.view.View<EmsScriptNode>> views) {
        ArrayList<EmsScriptNode> nodes = new ArrayList<>();
        if (views == null)
            return nodes;
        for (sysml.view.View<EmsScriptNode> view : views) {
            if (view == null) {
                logger.error("viewsToSysmlIds() trying to get non-existent sysmlid of null view");
                continue;
            }
            EmsScriptNode node = view.getElement();
            if (node == null) {
                logger.error("viewsToSysmlIds() trying to get non-existent sysmlid of view: " + view);
            } else {
                nodes.add(node);
            }
        }
        return nodes;
    }

    public JSONArray toJsonArrayOfSysmlIds(Collection<EmsScriptNode> nodes) {
        JSONArray jarr = new JSONArray();
        if (nodes == null)
            return jarr;
        for (EmsScriptNode node : nodes) {
            if (node != null && node.exists()) {
                String id = node.getSysmlId();
                if (Utils.isNullOrEmpty(id)) {
                    logger.error("toJsonArrayOfSysmlIds() trying to get non-existent sysmlid of node: " + node);
                } else {
                    jarr.put(node.getSysmlId());
                }
            }
        }
        return jarr;
    }

    protected void addProductJSON(JSONObject json, EmsScriptNode node, Set<String> filter, WorkspaceNode ws,
                    Date dateTime) throws JSONException {
        JSONArray jarr = new JSONArray();
        String v2v = (String) node.getProperty("view2:view2view");
        if (!Utils.isNullOrEmpty(v2v)) {
            jarr.put(v2v);
            putInJson(json, "view2view", new JSONArray((String) node.getProperty("view2:view2view")), filter);
        }
        jarr = new JSONArray();
        String noSections = (String) node.getProperty("view2:noSections");
        if (!Utils.isNullOrEmpty(noSections)) {
            jarr.put(noSections);
            putInJson(json, "noSections", new JSONArray((String) node.getProperty("view2:noSections")), filter);
        }
        addViewJSON(json, node, filter, ws, dateTime);
    }

    protected void addPropertyJSON(JSONObject json, EmsScriptNode node, Set<String> filter, WorkspaceNode ws,
                    Date dateTime) throws JSONException {
        putInJson(json, "isDerived", node.getProperty("sysml:isDerived"), filter);
        putInJson(json, "isSlot", node.getProperty("sysml:isSlot"), filter);
        putInJson(json, "value", addInternalJSON(node.getNodeRefProperty("sysml:value", dateTime, ws), ws, dateTime),
                        filter);
        NodeRef propertyType = (NodeRef) node.getNodeRefProperty("sysml:propertyType", dateTime, ws);
        putInJson(json, "propertyType", addNodeRefIdJSON(propertyType), filter);

        putInJson(json, Acm.JSON_LOWER,
                        addInternalJSON(node.getNodeRefProperty(Acm.ACM_LOWER, dateTime, ws), ws, dateTime), filter);

        putInJson(json, Acm.JSON_UPPER,
                        addInternalJSON(node.getNodeRefProperty(Acm.ACM_LOWER, dateTime, ws), ws, dateTime), filter);

        putInJson(json, Acm.JSON_MULTIPLICITY_MIN, node.getProperty(Acm.ACM_MULTIPLICITY_MIN), filter);
        putInJson(json, Acm.JSON_MULTIPLICITY_MAX, node.getProperty(Acm.ACM_MULTIPLICITY_MAX), filter);

        ArrayList<NodeRef> redefinedNodes = (ArrayList<NodeRef>) this.getNodeRefProperty(Acm.ACM_REDEFINES, true,
                        dateTime, this.getWorkspace());
        if (!Utils.isNullOrEmpty(redefinedNodes)) {
            JSONArray redefinedIds = addNodeRefIdsJSON(redefinedNodes);
            putInJson(json, Acm.JSON_REDEFINES, redefinedIds, filter);
        }

        putInJson(json, Acm.JSON_AGGREGATION, node.getProperty(Acm.ACM_AGGREGATION), filter);
    }

    protected void addDirectedRelationshipJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {

        // Dont need the correct workspace b/c sysml ids are immutable:
        NodeRef sourceNode = (NodeRef) node.getNodeRefProperty("sysml:source", true, dateTime, node.getWorkspace());
        putInJson(json, "source", addNodeRefIdJSON(sourceNode), filter);

        NodeRef targetNode = (NodeRef) node.getNodeRefProperty("sysml:target", true, dateTime, node.getWorkspace());
        putInJson(json, "target", addNodeRefIdJSON(targetNode), filter);

    }

    protected void addDependencyJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addDirectedRelationshipJSON(json, node, filter, dateTime);
    }

    protected void addExposeJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addDirectedRelationshipJSON(json, node, filter, dateTime);
    }

    protected void addConformJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addDirectedRelationshipJSON(json, node, filter, dateTime);
    }

    protected void addGeneralizationJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addDirectedRelationshipJSON(json, node, filter, dateTime);
    }

    protected void addValueSpecificationJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        // Dont need the correct workspace b/c sysml ids are immutable:
        NodeRef valExprNode = (NodeRef) node.getNodeRefProperty(Acm.ACM_VALUE_EXPRESSION, true, dateTime,
                        node.getWorkspace());
        putInJson(json, Acm.JSON_VALUE_EXPRESSION, addNodeRefIdJSON(valExprNode), filter);

    }

    protected void addDurationJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);
    }

    protected void addDurationIntervalJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);

        // Dont need the correct workspace b/c sysml ids are immutable:
        NodeRef durMaxNode =
                        (NodeRef) node.getNodeRefProperty("sysml:durationMax", true, dateTime, node.getWorkspace());
        putInJson(json, "max", addNodeRefIdJSON(durMaxNode), filter);

        NodeRef durMinNode =
                        (NodeRef) node.getNodeRefProperty("sysml:durationMin", true, dateTime, node.getWorkspace());
        putInJson(json, "min", addNodeRefIdJSON(durMinNode), filter);

    }

    protected void addElementValueJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {

        NodeRef elementNode =
                        (NodeRef) node.getNodeRefProperty(Acm.ACM_ELEMENT_VALUE_ELEMENT, dateTime, node.getWorkspace());
        addValueSpecificationJSON(json, node, filter, dateTime);
        putInJson(json, "element", addNodeRefIdJSON(elementNode), filter);

    }

    protected void addLiteralSetJSON(JSONObject json, EmsScriptNode node, Set<String> filter, WorkspaceNode ws,
                    Date dateTime) throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);

        putInJson(json, Acm.JSON_SET, addInternalJSON(node.getNodeRefProperty(Acm.ACM_SET, dateTime, ws), ws, dateTime),
                        filter);

        putInJson(json, Acm.JSON_SET_OPERAND,
                        addInternalJSON(node.getNodeRefProperty(Acm.ACM_SET_OPERAND, dateTime, ws), ws, dateTime),
                        filter);

    }

    protected void addExpressionJSON(JSONObject json, EmsScriptNode node, Set<String> filter, WorkspaceNode ws,
                    Date dateTime) throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);

        putInJson(json, "operand",
                        addInternalJSON(node.getNodeRefProperty(Acm.ACM_OPERAND, dateTime, ws), ws, dateTime), filter);
        putInJson(json, "display", getExpressionDisplayString(), filter);
        if (evaluatingExpressions) {
            putInJson(json, "evaluation", getExpressionEvaluation(), filter);
        }
    }

    public Object getExpressionEvaluation() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getExpressionDisplayString() {
        // TODO Auto-generated method stub
        return null;
    }

    protected void addInstanceValueJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);

        // Dont need the correct workspace b/c sysml ids are immutable:
        NodeRef instanceNode = (NodeRef) node.getNodeRefProperty(Acm.ACM_INSTANCE, true, dateTime, node.getWorkspace());
        putInJson(json, "instance", addNodeRefIdJSON(instanceNode), filter);

    }

    protected void addIntervalJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);
    }

    protected void addLiteralBooleanJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);
        putInJson(json, "boolean", node.getProperty("sysml:boolean"), filter);
    }

    protected void addLiteralIntegerJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);
        putInJson(json, "integer", node.getProperty("sysml:integer"), filter);
    }

    protected void addLiteralNullJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);
    }

    protected void addLiteralRealJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);
        putInJson(json, "double", node.getProperty("sysml:double"), filter);
    }

    protected void addLiteralStringJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);
        putInJson(json, "string", node.getProperty("sysml:string"), filter);
    }

    protected void addLiteralUnlimitedNaturalJSON(JSONObject json, EmsScriptNode node, Set<String> filter,
                    Date dateTime) throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);
        putInJson(json, "naturalValue", node.getProperty("sysml:naturalValue"), filter);
    }

    protected void addMagicDrawDataJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        String data = (String) node.getProperty(Acm.ACM_MD_DATA);
        if (data != null) {
            putInJson(json, Acm.JSON_MD_DATA, data, filter);
        }
    }

    protected void addOpaqueExpressionJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);
        putInJson(json, "expressionBody", node.getProperty("sysml:expressionBody"), filter);
    }

    protected void addStringExpressionJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);
    }

    protected void addTimeExpressionJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);
    }

    protected void addTimeIntervalJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        addValueSpecificationJSON(json, node, filter, dateTime);

        // Dont need the correct workspace b/c sysml ids are immutable:
        NodeRef timeMaxNode =
                        (NodeRef) node.getNodeRefProperty("sysml:timeIntervalMax", true, dateTime, node.getWorkspace());
        putInJson(json, "max", addNodeRefIdJSON(timeMaxNode), filter);

        NodeRef timeMinNode =
                        (NodeRef) node.getNodeRefProperty("sysml:timeIntervalMin", true, dateTime, node.getWorkspace());
        putInJson(json, "min", addNodeRefIdJSON(timeMinNode), filter);

    }

    protected void addOperationJSON(JSONObject json, EmsScriptNode node, Set<String> filter, WorkspaceNode ws,
                    Date dateTime) throws JSONException {
        ArrayList<NodeRef> nodeRefs =
                        (ArrayList<NodeRef>) node.getNodeRefProperty("sysml:operationParameter", dateTime, ws);
        JSONArray ids = addNodeRefIdsJSON(nodeRefs);
        putInJson(json, "parameters", ids, filter);

        if (!embeddingExpressionInOperation) {
            NodeRef opExpNode = (NodeRef) node.getNodeRefProperty("sysml:operationExpression", dateTime, ws);
            putInJson(json, "expression", addNodeRefIdJSON(opExpNode), filter);

        } else {
            Object property = node.getNodeRefProperty("sysml:operationExpression", dateTime, ws);
            if (property != null) {
                putInJson(json, "expression", addInternalJSON(property, ws, dateTime), filter);
            }
        }
    }

    protected void addInstanceSpecificationJSON(JSONObject json, EmsScriptNode node, Set<String> filter,
                    WorkspaceNode ws, Date dateTime) throws JSONException {

        putInJson(json, Acm.JSON_INSTANCE_SPECIFICATION_SPECIFICATION, addInternalJSON(
                        node.getNodeRefProperty(Acm.ACM_INSTANCE_SPECIFICATION_SPECIFICATION, dateTime, ws), ws,
                        dateTime), filter);

        ArrayList<NodeRef> nodeRefs = (ArrayList<NodeRef>) node.getNodeRefProperty(Acm.ACM_CLASSIFIER, dateTime, ws);
        JSONArray ids = addNodeRefIdsJSON(nodeRefs);
        putInJson(json, Acm.JSON_CLASSIFIER, ids, filter);

        nodeRefs = (ArrayList<NodeRef>) node.getNodeRefProperty(Acm.ACM_SLOTS, dateTime, ws);
        ids = addNodeRefIdsJSON(nodeRefs);
        putInJson(json, Acm.JSON_SLOTS, ids, filter);
    }

    protected void addConstraintJSON(JSONObject json, EmsScriptNode node, Set<String> filter, WorkspaceNode ws,
                    Date dateTime) throws JSONException {
        if (!embeddingExpressionInConstraint) {
            NodeRef specNode = (NodeRef) node.getNodeRefProperty("sysml:constraintSpecification", dateTime, ws);
            putInJson(json, "specification", addNodeRefIdJSON(specNode), filter);

        } else {
            Object property = node.getNodeRefProperty("sysml:constraintSpecification", dateTime, ws);
            if (property != null) {
                putInJson(json, "specification", addInternalJSON(property, ws, dateTime), filter);
            }
        }
    }

    protected void addParameterJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {
        putInJson(json, "direction", node.getProperty("sysml:parameterDirection"), filter);
        putInJson(json, "parameterType", node.getProperty("sysml:parameterType"), filter);

        // Dont need the correct workspace b/c sysml ids are immutable:
        NodeRef paramValNode = (NodeRef) node.getNodeRefProperty("sysml:parameterDefaultValue", true, dateTime,
                        node.getWorkspace());
        putInJson(json, "defaultValue", addNodeRefIdJSON(paramValNode), filter);

    }

    protected void addConnectorJSON(JSONObject json, EmsScriptNode node, Set<String> filter, WorkspaceNode ws,
                    Date dateTime) throws JSONException {

        addDirectedRelationshipJSON(json, node, filter, dateTime);

        ArrayList<NodeRef> nodeRefsSource =
                        (ArrayList<NodeRef>) node.getNodeRefProperty(Acm.ACM_SOURCE_PATH, dateTime, ws);
        JSONArray sourceIds = addNodeRefIdsJSON(nodeRefsSource);
        putInJson(json, Acm.JSON_SOURCE_PATH, sourceIds, filter);

        ArrayList<NodeRef> nodeRefsTarget =
                        (ArrayList<NodeRef>) node.getNodeRefProperty(Acm.ACM_TARGET_PATH, dateTime, ws);
        JSONArray targetIds = addNodeRefIdsJSON(nodeRefsTarget);
        putInJson(json, Acm.JSON_TARGET_PATH, targetIds, filter);

        String kind = (String) node.getProperty(Acm.ACM_CONNECTOR_KIND);
        if (kind != null) {
            putInJson(json, Acm.JSON_CONNECTOR_KIND, kind, filter);
        }

        NodeRef connectorType = (NodeRef) node.getNodeRefProperty(Acm.ACM_CONNECTOR_TYPE, dateTime, ws);
        putInJson(json, Acm.JSON_CONNECTOR_TYPE, addNodeRefIdJSON(connectorType), filter);

        putInJson(json, Acm.JSON_CONNECTOR_VALUE,
                        addInternalJSON(node.getNodeRefProperty(Acm.ACM_CONNECTOR_VALUE, dateTime, ws), ws, dateTime),
                        filter);

        putInJson(json, Acm.JSON_TARGET_LOWER,
                        addInternalJSON(node.getNodeRefProperty(Acm.ACM_TARGET_LOWER, dateTime, ws), ws, dateTime),
                        filter);

        putInJson(json, Acm.JSON_TARGET_UPPER,
                        addInternalJSON(node.getNodeRefProperty(Acm.ACM_TARGET_UPPER, dateTime, ws), ws, dateTime),
                        filter);

        putInJson(json, Acm.JSON_SOURCE_LOWER,
                        addInternalJSON(node.getNodeRefProperty(Acm.ACM_SOURCE_LOWER, dateTime, ws), ws, dateTime),
                        filter);

        putInJson(json, Acm.JSON_SOURCE_UPPER,
                        addInternalJSON(node.getNodeRefProperty(Acm.ACM_SOURCE_UPPER, dateTime, ws), ws, dateTime),
                        filter);

    }

    protected void addAssociationJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {

        addDirectedRelationshipJSON(json, node, filter, dateTime);

        // Dont need the correct workspace b/c sysml ids are immutable:
        ArrayList<NodeRef> nodeRefsOwnedEnd = (ArrayList<NodeRef>) node.getNodeRefProperty(Acm.ACM_OWNED_END, true,
                        dateTime, node.getWorkspace());
        JSONArray ownedEndIds = addNodeRefIdsJSON(nodeRefsOwnedEnd);
        putInJson(json, Acm.JSON_OWNED_END, ownedEndIds, filter);

        putInJson(json, Acm.JSON_SOURCE_AGGREGATION, node.getProperty(Acm.ACM_SOURCE_AGGREGATION), filter);
        putInJson(json, Acm.JSON_TARGET_AGGREGATION, node.getProperty(Acm.ACM_TARGET_AGGREGATION), filter);

    }

    protected void addCharacterizesJSON(JSONObject json, EmsScriptNode node, Set<String> filter, Date dateTime)
                    throws JSONException {

        addDirectedRelationshipJSON(json, node, filter, dateTime);
    }

    protected void addSuccessionJSON(JSONObject json, EmsScriptNode node, Set<String> filter, WorkspaceNode ws,
                    Date dateTime) throws JSONException {

        addConnectorJSON(json, node, filter, ws, dateTime);
    }

    protected void addBindingJSON(JSONObject json, EmsScriptNode node, Set<String> filter, WorkspaceNode ws,
                    Date dateTime) throws JSONException {

        addConnectorJSON(json, node, filter, ws, dateTime);
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

    public static List<String> getNames(List<EmsScriptNode> nodes) {
        return getNamesOrIdsImpl(nodes, true);
    }

    public static List<String> getSysmlIds(List<EmsScriptNode> nodes) {
        return getNamesOrIdsImpl(nodes, false);
    }

    private static List<String> getNamesOrIdsImpl(List<EmsScriptNode> nodes, boolean getName) {

        List<String> names = new ArrayList<>();
        for (EmsScriptNode node : nodes) {
            String name = getName ? node.getName() : node.getSysmlId();
            if (!Utils.isNullOrEmpty(name)) {
                names.add(name);
            }
        }
        return names;
    }

    public static Collection<? extends NodeRef> getNodeRefs(List<EmsScriptNode> nodes) {
        List<NodeRef> refs = new ArrayList<>();
        for (EmsScriptNode node : nodes) {
            NodeRef ref = node.getNodeRef();
            refs.add(ref);
        }
        return refs;
    }

    public static List<EmsScriptNode> toEmsScriptNodeList(Collection<NodeRef> refs) {
        ArrayList<EmsScriptNode> nodes = new ArrayList<>();
        if (refs == null)
            return nodes;
        for (NodeRef ref : refs) {
            nodes.add(new EmsScriptNode(ref, NodeUtil.getServices()));
        }
        return nodes;
    }

    @Override
    public boolean removeAspect(String type) {
        if (hasAspect(type)) {
            makeSureNodeRefIsNotFrozen();
            transactionCheck();

            updateBogusProperty(type);

            return super.removeAspect(type);
        }
        return true;
    }

    public static boolean isModelElement(NodeRef ref) {
        EmsScriptNode node = new EmsScriptNode(ref, NodeUtil.getServices());
        return node.isModelElement();
    }

    public boolean isModelElement() {
        if (getTypeShort().equals("sysml:Element")) {
            return true;
        }
        return false;
    }

    public boolean hasValueSpecProperty(EmsScriptNode propVal, Date dateTime, WorkspaceNode ws) {

        ArrayList<NodeRef> children = getValueSpecOwnedChildren(false, dateTime, ws);

        // To remian backwards compatible, search the old way if needed:
        if (Utils.isNullOrEmpty(children)) {
            children = getOwnedChildren(false, dateTime, ws);

            // No point of checking this if there are no children at all:
            if (!Utils.isNullOrEmpty(children)) {
                for (String acmType : Acm.TYPES_WITH_VALUESPEC.keySet()) {
                    if (hasOrInheritsAspect(acmType)) {
                        for (String acmProp : Acm.TYPES_WITH_VALUESPEC.get(acmType)) {
                            if (propVal != null) {
                                Object propValFnd = getNodeRefProperty(acmProp, dateTime, ws);
                                if (propValFnd instanceof NodeRef) {
                                    EmsScriptNode node = new EmsScriptNode((NodeRef) propValFnd, services);
                                    if (node.equals(propVal, true))
                                        return true;
                                } else if (propValFnd instanceof List) {
                                    List<NodeRef> nrList = (ArrayList<NodeRef>) propValFnd;
                                    for (NodeRef ref : nrList) {
                                        if (ref != null) {
                                            EmsScriptNode node = new EmsScriptNode(ref, services);
                                            if (node.equals(propVal, true))
                                                return true;
                                        }
                                    }
                                }
                            } else {
                                if (getNodeRefProperty(acmProp, dateTime, ws) != null)
                                    return true;
                            }
                        }
                    }
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public boolean hasValueSpecProperty(Date dateTime, WorkspaceNode ws) {
        return hasValueSpecProperty(null, dateTime, ws);
    }

    /**
     * Returns true if the passed property name maps to a value spec
     *
     * @param propName
     * @return
     */
    public static boolean isValueSpecProperty(String propName) {
        for (Set<String> valueSpecSet : Acm.TYPES_WITH_VALUESPEC.values()) {
            if (valueSpecSet.contains(propName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the direct parent of this node has a value spec property that points to this
     * node. Does not trace up the parent tree as getValueSpecOwner() does.
     */
    public boolean parentOwnsValueSpec(Date dateTime, WorkspaceNode ws) {
        EmsScriptNode parent = getUnreifiedParent(dateTime, ws);
        return parent != null && parent.hasValueSpecProperty(this, dateTime, ws);
    }

    /**
     * @return the parent/owner (preferably not a ValueSpecification itself, like Expression) with
     *         an aspect that has a property whose value is a ValueSpecification.
     */
    public EmsScriptNode getValueSpecOwner(Date dateTime, WorkspaceNode ws) {
        if (Debug.isOn())
            Debug.outln("getValueSpecOwner(" + this + ")");
        EmsScriptNode parent = this;

        String runAsUser = AuthenticationUtil.getRunAsUser();
        boolean changeUser = !ADMIN_USER_NAME.equals(runAsUser);
        if (changeUser) {
            AuthenticationUtil.setRunAsUser(ADMIN_USER_NAME);
        }

        while (parent != null && parent.isOwnedValueSpec(dateTime, ws)) {
            if (Debug.isOn())
                Debug.outln("parent = " + parent);
            EmsScriptNode oldParent = parent;
            parent = oldParent.getUnreifiedValueSpecParent(dateTime, ws);

            // For backwards compatibility:
            if (parent == null) {
                parent = oldParent.getUnreifiedParent(dateTime, ws);
            }
        }

        if (changeUser) {
            AuthenticationUtil.setRunAsUser(runAsUser);
        }

        if (Debug.isOn())
            Debug.outln("returning " + parent);
        return parent;
    }

    public boolean isOwnedValueSpec(Date dateTime, WorkspaceNode ws) {
        if (hasOrInheritsAspect("sysml:ValueSpecification")) {
            if (getNodeRefProperty("ems:valueSpecOwner", dateTime, ws) != null) {
                return true;
            }
            // This line is to make sure it is backwards compatible:
            else {
                return parentOwnsValueSpec(dateTime, ws);
            }
        }
        return false;
    }

    /**
     * Utility to find the original node - needed for getting creation time
     *
     * @param node
     * @return
     */
    public EmsScriptNode getOriginalNode() {
        EmsScriptNode node = this;
        while (true) {
            NodeRef ref = (NodeRef) node.getNodeRefProperty("ems:source", true, null, true, true, null);
            if (ref != null) {
                node = new EmsScriptNode(ref, services, response);
            } else {
                break;
            }
        }

        return node;
    }

    public static String getQualifiedId( EmsScriptNode ws, Seen<EmsScriptNode> seen ) {
        if ( ws == null ) {
            return "master";
        }
        Pair< Boolean, Seen< EmsScriptNode > > p = Utils.seen( ws, true, seen );
        if ( p.first ) return null;
        seen = p.second;
        return getQualifiedId( ws.getParent(), seen ) + "/" + ws.getId();
    }

    public static String getQualifiedName( EmsScriptNode ws, Seen<EmsScriptNode> seen ) {
        if ( ws == null ) {
            return "master";
        }
        Pair< Boolean, Seen< EmsScriptNode > > p = Utils.seen( ws, true, seen );
        if ( p.first ) return null;
        seen = p.second;
        return getQualifiedName( ws.getParent(), seen ) + "/" + ws.getWorkspaceName();
    }
}
