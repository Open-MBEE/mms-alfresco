/**
 *
 */
package gov.nasa.jpl.view_repo.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Seen;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;


/**
 * WorkspaceNode is an EmsScriptNode and a folder containing changes to a parent
 * workspace.
 *
 */
public class WorkspaceNode extends EmsScriptNode {

    private static final long serialVersionUID = -7143644366531706115L;
    private static final boolean checkingForEmsSource = true;  // FIXME -- at some point, this should turned off and removed along with the code that uses it.

    /**
     * @param nodeRef
     * @param services
     * @param response
     * @param status
     */
    public WorkspaceNode( NodeRef nodeRef, ServiceRegistry services,
                          StringBuffer response, Status status ) {
        super( nodeRef, services, response, status );
    }

    /**
     * @param nodeRef
     * @param services
     * @param response
     */
    public WorkspaceNode( NodeRef nodeRef, ServiceRegistry services,
                          StringBuffer response ) {
        super( nodeRef, services, response );
    }

    /**
     * @param nodeRef
     * @param services
     */
    public WorkspaceNode( NodeRef nodeRef, ServiceRegistry services ) {
        super( nodeRef, services );
    }

    @Override
    public WorkspaceNode getWorkspace() {
        log( "Warning! calling getWorkspace on a workspace! " + getName() );
        return this;
    }

    @Override
    public WorkspaceNode getParentWorkspace() {
        // ems:parent is workspace meta data, so dont need dateTime/workspace args
        NodeRef ref = (NodeRef)getNodeRefProperty("ems:parent", null, null);
        if ( ref == null ) {
            // Handle data corrupted by a bug (now fixed)
            if ( checkingForEmsSource ) {
                try {
                    ref = (NodeRef)getNodeRefProperty("ems:source", null, null);
                    if ( ref != null ) {
                        // clean up
                        setProperty( "ems:parent", ref );
                        removeProperty( "ems:source" );
                    }
                } catch ( Throwable e ) {}
            }
            return null;
        }
        WorkspaceNode parentWs = new WorkspaceNode( ref, getServices() );
        return parentWs;
    }
    // delete later
    @Override
    public WorkspaceNode getSourceWorkspace() {
        // ems:source is workspace meta data, so dont need dateTime/workspace args
        NodeRef ref = (NodeRef)getNodeRefProperty("ems:source", null, null);
        if ( ref == null ) return null;
        WorkspaceNode sourceWs = new WorkspaceNode( ref, getServices() );
        return sourceWs;
    }

    @Override
    public void setWorkspace( WorkspaceNode workspace, NodeRef source ) {
        String msg = "Cannot set the workspace of a workspace!";
        if ( getResponse() != null ) {
            getResponse().append( msg + "\n" );
            if ( getStatus() != null ) {
                getStatus().setCode( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                     msg );
            }
        }
        Debug.error( msg );
    }

    public Date getCopyTime() {
        Date time = (Date)getProperty("ems:copyTime");
        return time;
    }

    public Date getCopyOrCreationTime() {
        Date copyTime = getCopyTime();
        return copyTime != null ? copyTime : getCreationDate();
    }

//    /**
//     * Create a workspace folder within the specified folder or (if the folder
//     * is null) within the specified user's home folder.
//     *
//     * @param wsName
//     *            the short name of the workspace
//     * @param userName
//     *            the name of the user that is creating the workspace
//     * @param folder
//     *            the folder within which to create the workspace
//     * @param services
//     * @param response
//     * @param status
//     * @return the new workspace or null if the workspace could not be created
//     *         because both the containing folder and the user name were both
//     *         unspecified (non-existent)
//     */
//    public static WorkspaceNode createWorskpaceInFolder( String wsName,
//                                                         EmsScriptNode sourceWs,
//                                                         String userName,
//                                                         EmsScriptNode folder,
//                                                         ServiceRegistry services,
//                                                         StringBuffer response,
//                                                         Status status ) {
//        if ( wsName == null ) {
//            wsName = NodeUtil.createId( services );
//        }
//        if ( folder == null || !folder.exists() ) {
//            //String userName = ws.getOwner();
//            if ( userName != null && userName.length() > 0 ) {
//                folder = NodeUtil.getUserHomeFolder( userName, true );
//                if ( Debug.isOn() ) Debug.outln( "user home folder: " + folder );
//            }
//        }
//        if ( folder == null || !folder.exists() ) {
//            Debug.error( true, false, "\n%%% Error! no folder, " + folder
//                                      + ", within which to create workspace, "
//                                      + wsName );
//        }
//
//        String cmName = null;
//
//        WorkspaceNode ws = new WorkspaceNode( folder.createFolder( wsName ).getNodeRef(),
//                                              services, response, status );
//        ws.addAspect( "ems:Workspace" );
//
//        ws.setProperty( "ems:parent", folder );
//        if ( folder.isWorkspace() ) {
//            if ( Debug.isOn() ) Debug.outln( "folder is a workspace: " + folder );
//            WorkspaceNode parentWorkspace =
//                    new WorkspaceNode( folder.getNodeRef(), services, response,
//                                       status );
//            if ( Debug.isOn() ) Debug.outln( "parent workspace: " + parentWorkspace );
//            parentWorkspace.appendToPropertyNodeRefs( "ems:children", ws.getNodeRef() );
//        }
//        ws.setProperty( "ems:lastTimeSyncParent", new Date() );
//        if ( Debug.isOn() ) Debug.outln( "created workspace " + ws + " in folder " + folder );
//        return ws;
//    }

    /**
     * Create a workspace folder within the specified folder or (if the folder
     * is null) within the specified user's home folder.
     *
     * @param wsName
     *            the short name of the workspace
     * @param userName
     *            the name of the user that is creating the workspace
     * @param sourceNameOrId
     *            the name or id of the workspace that will be a parent to the new one
     * @param folder
     *            the folder within which to create the workspace
     * @param services
     * @param response
     * @param status
     * @return the new workspace or null if the workspace could not be created
     *         because both the containing folder and the user name were both
     *         unspecified (non-existent)
     */
    public static WorkspaceNode createWorkspaceFromSource( String wsName,
                                                           String orgId,
                                                           String projectId,
                                                           String sourceNameOrId,
                                                           Date copyTime,
                                                           EmsScriptNode folder,
                                                           ServiceRegistry services,
                                                           StringBuffer response,
                                                           Status status,
                                                           String description) {
        if (logger.isDebugEnabled()) {
            logger.debug( "createWorkspaceFromSource(wsName=" + wsName
                          + "orgId=" + orgId + " projectId=" + projectId + " sourceNameOrId="
                          + sourceNameOrId + ", copyTime=" + copyTime
                          + ", folder="
                          + ( folder == null ? "null" : folder.getSysmlId() )
                          + ")" );
        }

        if ( Utils.isNullOrEmpty( wsName ) ) {
            wsName = NodeUtil.createId( services );
        }

        EmsScriptNode parent = null;

        if ( folder == null || !folder.exists() ) {
            //String userName = ws.getOwner();
            EmsScriptNode org = new EmsScriptNode(services.getSiteService().getSite(orgId).getNodeRef(), services);
            EmsScriptNode projectContainerNode = org.childByNamePath(projectId, false, null, true);
            EmsScriptNode refContainerNode = projectContainerNode.childByNamePath("/refs", false, null, true);
            parent = refContainerNode.childByNamePath(sourceNameOrId);

            if ( parent != null && !refContainerNode.getSysmlId().equals(null) ) {
                folder = refContainerNode.createFolder(wsName);
                if ( Debug.isOn() ) Debug.outln( "user home folder: " + folder );
            }
        }
        if ( folder == null || !folder.exists() ) {
            Debug.error( true, false, "\n%%% Error! no folder, " + folder
                    + ", within which to create workspace, "
                    + wsName );
        }

        WorkspaceNode parentWorkspace = WorkspaceNode.getWorkspaceFromId(parent.getId(), services, null, null, null);
        String cmName = wsName + '_' + getName( parentWorkspace );
        String cmTitle = cmName;

        // Make sure the workspace does not already exist in the target folder with the same
        // parent workspace:
        Set<EmsScriptNode> childs = folder.getChildNodes();
        for (EmsScriptNode child : childs) {
            if ( child != null && child.exists() ) {
                String childWsName = (String)child.getProperty("ems:workspace_name");
                // ems:parent is workspace meta property, so dateTime/workspace args dont matter
                NodeRef childWsParentRef = (NodeRef)child.getNodeRefProperty("ems:parent", null, null);
                EmsScriptNode childWsParent = childWsParentRef != null ? new EmsScriptNode(childWsParentRef, services) : null;

                String childWsParentName = childWsParent != null ? childWsParent.getId() : null;

                if (childWsName != null && childWsName.equals( wsName ) &&
                    ((childWsParentName == null && sourceNameOrId.equals( "master" )) || (childWsParentName != null && childWsParentName.equals( sourceNameOrId )))) {
                    String msg = "ERROR! Trying to create an workspace with the same user: "+folder.getName()+", the same name: "+wsName+", and same parent workspace: "+sourceNameOrId+"\n";
                    response.append( msg );
                    if ( status != null ) {
                        status.setCode( HttpServletResponse.SC_BAD_REQUEST, msg );
                    }
                    return null;
                }
            }
        }

        // Make sure the workspace does not already exist otherwise
        // So refs can be named the same, we store the name as the title, then update the
        // name to be unique with the nodeID as the name
    NodeRef ref = NodeUtil.findNodeRefById( cmName, true, null, null, services, false );
    // FIXME -- This does not find refs that are not visible to the user!
    if ( ref != null ) {
        String msg = "ERROR! Trying to create an existing workspace, " + cmName + "!\n";
        response.append( msg );
        if ( status != null ) {
            status.setCode( HttpServletResponse.SC_BAD_REQUEST, msg );
        }
        return null;
    }

        WorkspaceNode ws = new WorkspaceNode( folder.createFolder( cmName ).getNodeRef(),
                                              services, response, status );

    ws.setProperty("cm:title", cmTitle);
    cmName = ws.getId() + "_" + getId( parentWorkspace );
    ws.setProperty( "cm:name", cmName );

    ws.addAspect( "ems:HasWorkspace" );
    ws.setProperty("ems:workspace", ws.getNodeRef() );

        ws.addAspect( "ems:Workspace" );
    ws.setProperty("ems:workspace_name", wsName );
        ws.createOrUpdateProperty( "ems:lastTimeSyncParent", new Date() );
        if ( copyTime != null ) {
            ws.createOrUpdateProperty( "ems:copyTime", copyTime );
        }

        if ( Debug.isOn() ) Debug.outln( "parent workspace: " + parentWorkspace );
        if(parentWorkspace != null) {
            parentWorkspace.appendToPropertyNodeRefs( "ems:children", ws.getNodeRef() );
            ws.setProperty( "ems:parent", parentWorkspace.getNodeRef() );
        }
        if ( Debug.isOn() ) Debug.outln( "created workspace " + ws + " in folder " + folder );

        if (description != null) {
            ws.setProperty("ems:description", description );
        }

        ws.getOrSetCachedVersion();

        return ws;
    }

    public void delete( boolean deleteChildWorkspaces ) {
        if ( !checkPermissions( PermissionService.WRITE, getResponse(), getStatus() ) ) {
            log( "no write permissions to delete workpsace " + getName() );
            return;
        }

        // Add the delete aspect to mark as "deleted"
        makeSureNodeRefIsNotFrozen();
        addAspect( "ems:Deleted" );

        // FIXME -- REVIEW -- Is that enough?! What about the contents? Don't we
        // need to purge? Or is a "deleted" workspaceNode enough?

        // Update parent/child workspace references

        // Remove this workspace from parent's children
//        WorkspaceNode source = getParentWorkspace();
//        if ( Debug.isOn() ) Debug.outln( "deleted workspace " + this + " from source " + getName(source) );
//        if ( source == null || !source.exists() ) {
//            // TODO -- do we keep the master's children anywhere?
//            if ( !source.exists() ) {
//                log( "no write permissions to remove reference to child workpsace, " + getName() + ", from parent, " + getName(source) );
//            }
//        } else {
//            if ( !source.checkPermissions( PermissionService.WRITE, getResponse(), getStatus() ) ) {
//                String msg = "Warning! No write permissions to delete workpsace " + getName() + ".\n";
//                getResponse().append( msg );
//                log( msg );
////                if ( getStatus() != null ) {
////                    getStatus().setCode( HttpServletResponse.SC_, msg );
////                }
//            } else {
//                source.removeFromPropertyNodeRefs( "ems:children", getNodeRef() );
//            }
//        }

        // Not bothering to remove this workspace's ems:parent or ems:children

        // Delete children if requested
        if ( deleteChildWorkspaces ) {
            deleteChildWorkspaces( true );
        }
    }

    public void deleteChildWorkspaces( boolean recursive ) {
        // getting a copy in case it's the same list from which the children will remove themselves
        ArrayList< NodeRef > children = new ArrayList<NodeRef>(getPropertyNodeRefs( "ems:children", true, null, null ));
        for ( NodeRef ref : children ) {
            WorkspaceNode childWs = new WorkspaceNode( ref, getServices(),
                                                       getResponse(),
                                                       getStatus() );
            if ( !NodeUtil.exists( childWs ) ) {
                log( "trying to delete non-existent child workspace " +
                     ( childWs == null ? "" : "," + childWs.getName() + ", " ) +
                     " from parent, " + getName() );
            } else {
                childWs.delete( recursive );
            }
        }
    }

    /**
     * Determine whether the given node is correct for this workspace, meaning
     * that it is either modified in this workspace or is contained by the
     * parent workspace and unmodified in this workspace.
     *
     * @param node
     * @return true iff the node is in this workspace
     */
    public boolean contains( EmsScriptNode node  ) {
        WorkspaceNode nodeWs = node.getWorkspace();
        if ( this.equals( nodeWs ) ) return true;

        WorkspaceNode parentWs = getParentWorkspace();
        if ( parentWs == null ) return ( nodeWs == null );
        return parentWs.contains( node );
    }



    public static String getId( WorkspaceNode ws ) {
        if ( ws == null ) return "master";
        return ws.getNodeRef().getId();
    }

    public static String getWorkspaceName( WorkspaceNode ws ) {
        if ( ws == null ) return "master";
        return ws.getWorkspaceName();
    }

    public static String getName( WorkspaceNode ws ) {
        if ( ws == null ) return "master";
        return ws.getName();
    }

    // don't want to override getName() in case that causes problems for
    // alfresco's code
    @Override
    public String getWorkspaceName() {
        return (String)getProperty("ems:workspace_name");
    }

    public static String getQualifiedIdForWorkspace( WorkspaceNode ws ) {
        return getQualifiedIdForWorkspace( ws, null );
    }

    public static String getQualifiedIdForWorkspace( WorkspaceNode ws,
                                         Seen<WorkspaceNode> seen ) {
        if ( ws == null ) {
            return getId( ws );
        }
        Pair< Boolean, Seen< WorkspaceNode > > p = Utils.seen( ws, true, seen );
        if ( p.first ) return null;
        seen = p.second;
        return getQualifiedIdForWorkspace( ws.getParentWorkspace(), seen ) + "/" + ws.getId();
    }

    public static String getQualifiedNameForWorkspace( WorkspaceNode ws ) {
        return getQualifiedNameForWorkspace( ws, null );
    }
    public static String getQualifiedNameForWorkspace( WorkspaceNode ws,
                                           Seen<WorkspaceNode> seen ) {
        if ( ws == null ) {
            return getWorkspaceName( ws );
        }
        Pair< Boolean, Seen< WorkspaceNode > > p = Utils.seen( ws, true, seen );
        if ( p.first ) return null;
        seen = p.second;
        return getQualifiedNameForWorkspace( ws.getParentWorkspace(), seen ) + "/" + ws.getWorkspaceName();
    }

    public WorkspaceNode getCommonParent(WorkspaceNode other) {
        return getCommonParent( this, other );
    }
    public Pair< WorkspaceNode, WorkspaceNode > getChildrenOfCommonParent(WorkspaceNode other) {
        return getChildrenOfCommonParent( this, other );
    }

    public static WorkspaceNode getCommonParent( WorkspaceNode ws1,
                                                 WorkspaceNode ws2 ) {
        Set<WorkspaceNode> parents = new TreeSet<WorkspaceNode>();

        // brute force walk up one branch, then the other checking for matches along the way
        while ( ws1 != null ) {
            parents.add( ws1 );
            ws1 = ws1.getParentWorkspace();
        }

        while ( ws2 != null ) {
            if (parents.contains( ws2 )) break;
            ws2 = ws2.getParentWorkspace();
        }
        if ( ws2 != null && parents.contains( ws2 ) ) {
            return ws2;
        }
        return null;
    }

    /**
     * Return the earliest ancestor of each workspace that is the immediate
     * child of the common parent workspace. If one is the parent of the other,
     * the child returned for the parent is null. If the refs are equal,
     * return null;
     *
     * @param ws1
     * @param ws2
     * @return
     */
    public static Pair<WorkspaceNode,WorkspaceNode> getChildrenOfCommonParent( WorkspaceNode ws1,
                                                                               WorkspaceNode ws2 ) {
        WorkspaceNode w = getCommonParent(ws1, ws2);
        WorkspaceNode child1 = null;
        WorkspaceNode child2 = null;
        while ( ws1 != null && !ws1.equals( w ) ) {
            child1 = ws1;
            ws1 = ws1.getParentWorkspace();
        }
        while ( ws2 != null && !ws2.equals( w ) ) {
            child2 = ws2;
            ws2 = ws2.getParentWorkspace();
        }
        Pair< WorkspaceNode, WorkspaceNode > result = new Pair<WorkspaceNode,WorkspaceNode>(child1, child2);
        return result;
//        if ( ws1 == null ) return null;
//        WorkspaceNode child1 = ws1;
//        WorkspaceNode child2 = ws2;
//        Set<WorkspaceNode> parents = new TreeSet<WorkspaceNode>();
//        List<WorkspaceNode> parents1 = new ArrayList<WorkspaceNode>();
//        List<WorkspaceNode> parents2 = new ArrayList<WorkspaceNode>();
//        if ( ws1 != null ) parents1.add( ws1 );
//        if ( ws2 != null ) parents2.add( ws2 );
//        while ( ( ws1 != null || ws2 != null )
//                && ( ws1 == null ? !ws2.equals( ws1 ) : !ws1.equals( ws2 ) )
//                && ( ws1 == null || !parents.contains( ws1 ) )
//                && ( ws2 == null || !parents.contains( ws2 ) ) ) {
//            if ( ws1 != null ) {
//                parents.add( ws1 );
//                child1 = ws1;
//                ws1 = ws1.getParentWorkspace();
//                if ( ws1 != null ) parents1.add( ws1 );
//            }
//            if ( ws2 != null ) {
//                parents.add( ws2 );
//                //child2 = ws2;
//                ws2 = ws2.getParentWorkspace();
//                if ( ws2 != null ) parents2.add( ws2 );
//            }
//        }
//        if ( ws1 != null && ( ws1.equals( ws2 ) || parents.contains( ws1 ) ) ) {
//            return child1;
//        }
//        if ( ws2 != null && parents.contains( ws2 ) ) {
//            // find ws2 in parents1 and return the ws below it.
//            WorkspaceNode child = null;
//            for ( WorkspaceNode w : parents1 ) {
//                if ( ws2.equals( w ) ) {
//                    if ( child == null ) {
//                        return w;
//                    }
//                    return child;
//                }
//                child = w;
//            }
//            return ws2; // Error?!
//        }
//        return null;
    }

    /**
     * Return all node refs changed in this workspace before or at the specified
     * date-time. The node refs returned are checked for permissions by the user
     * invoking the service irrespective of the run-as user.
     *
     * @param dateTime
     *            the date-time by which the nodes have changed
     * @return the changed node refs
     */
    public Set< NodeRef > getChangedNodeRefs( Date dateTime ) {
        Set< NodeRef > changedNodeRefs = new TreeSet< NodeRef >(NodeUtil.nodeRefComparator);
        if ( dateTime != null && dateTime.before( getCopyOrCreationTime() ) ) {
            return changedNodeRefs;
        }
        ArrayList< NodeRef > refs =
                NodeUtil.findNodeRefsByType( getNodeRef().toString(),
                                             SearchType.WORKSPACE.prefix,
                                             true, null, dateTime, false, true,
                                             getServices(), true );
        changedNodeRefs.addAll( refs );

//        // remove commits
//        ArrayList< EmsScriptNode > commits =
//                CommitUtil.getCommits( this, getServices(), getResponse() );
//        commits.add( CommitUtil.getCommitPkg( this, getServices(), getResponse() ) );
//        List<NodeRef> commitRefs = NodeUtil.getNodeRefs( commits, true );
//        changedNodeRefs.removeAll(commitRefs);

        return changedNodeRefs;
    }

    /**
     * @param dateTime
     * @return the IDs of the elements returned by {@link #getChangedNodeRefs(Date)}.
     */
    public Set< String > getChangedElementIds( Date dateTime ) {
        Set< String > changedElementIds = new TreeSet< String >();
        Set< NodeRef > refs = getChangedNodeRefs( dateTime );
        List< EmsScriptNode > nodes = toEmsScriptNodeList( refs );
        changedElementIds.addAll( EmsScriptNode.getNames( nodes ) );
        return changedElementIds;
    }


//    /**
//     * Get the NodeRefs of this workspace that have changed with respect to
//     * another workspace. This method need not check the actual changes to see
//     * if they are different and may be a superset of those actually changed.
//     *
//     * @param other
//     * @param dateTime
//     * @param otherTime
//     * @return
//     */
//    public Set< NodeRef > getChangedNodeRefsWithRespectTo( WorkspaceNode other,
//                                                           Date dateTime,
//                                                           Date otherTime ) {
//        return getChangedNodeRefsWithRespectTo( this, other, dateTime, otherTime );
//    }


    /**
     * Get the NodeRefs of this workspace that have changed with respect to
     * another workspace. This method need not check the actual changes to see
     * if they are different and may be a superset of those actually changed.
     * Results are filtered according to the nodes' read permissions for the
     * user making the request (not the run-as user).
     *
     * @param thisWs
     *            the workspace in which the nodes were changed
     * @param otherWs
     *            the workspace with which the changed nodes result in a
     *            difference
     * @param dateTime
     *            the time by which the nodes were changed (null means now)
     * @param otherTime
     *            the time at which the nodes result in a diff with the other
     *            workspace
     * @return the changed NodeRefs
     */
    public static Set< NodeRef > getChangedNodeRefsWithRespectTo( WorkspaceNode thisWs,
                                                                  WorkspaceNode otherWs,
                                                                  Date dateTime,
                                                                  Date otherTime,
//                                                                  boolean checkReadPermissions,
                                                                  ServiceRegistry services,
                                                                  StringBuffer response,
                                                                  Status status ) {

        Set< NodeRef > changedNodeRefs =
                new TreeSet< NodeRef >(NodeUtil.nodeRefComparator);//getChangedNodeRefs());
        WorkspaceNode targetParent = getCommonParent( thisWs, otherWs );
        WorkspaceNode parent = thisWs;
        WorkspaceNode lastParent = parent;

        Date commonBranchPoint = thisWs != null ? thisWs.getCopyTime(otherWs) : null;
        if (commonBranchPoint == null && thisWs != null)
        {
            commonBranchPoint = thisWs.getCopyTime();
        }
        Pair<WorkspaceNode, WorkspaceNode> p = getChildrenOfCommonParent(thisWs, otherWs);
        Date thisChildOfCommonParentCopyTime = p != null && p.first != null ? p.first.getCopyTime() : null;
        boolean amChildOfCommonParent = thisWs == p.first;

        //Date thisCopyDate = thisWs != null ? thisWs.getCopyTime() : null;
        //Date otherCopyDate = otherWs != null ? otherWs.getCopyTime() : null;
        Date thisCopyOrCreateDate = thisWs != null ? thisWs.getCopyOrCreationTime() : null;

        // Error if the timestamp is before the copy/creation time of the workspace:
        if ( dateTime != null && thisCopyOrCreateDate != null &&
             dateTime.before( thisCopyOrCreateDate ) ) {
            String msg = "ERROR! Timestamp given: "+dateTime+" is before the branch/creation time of the workspace: "+thisCopyOrCreateDate;
            if ( response != null ) {
                response.append( msg + "\n" );
                if ( status != null ) {
                    status.setCode( HttpServletResponse.SC_BAD_REQUEST,
                                    msg );
                }
            }
            Debug.error( false, msg );
            return null;
        }

        // Get nodes in the workspace that have changed with respect to the
        // common parent. To avoid computation, these do not take time into
        // account except to rule out refs with changes only after
        // dateTime.
        while ( parent != null && !parent.equals( targetParent ) ) {
            // These changes are already filtered for read permissions.
            Set< NodeRef > changes = parent.getChangedNodeRefs( dateTime );
            changedNodeRefs.addAll( changes );
            parent = parent.getParentWorkspace();
            if ( parent != null ) lastParent = parent;
        }

        // Determine the min/max times to search for commits for.  We must
        // accommodate both copyTime and following branches.
        // When looking for commits on the common branch,
        // for "following" branches want look over the time range of
        // [max(T1,T2),min(T1,T2)], and [max(C1,C2),min(C1,C2)] for
        // copyTime branches.
        // Where Ti is the timestamp and Ci is the copy time of the workspace

        // If it is a copy time branch then look at the copy time, otherwise
        // look at the time stamp:

        //Date thisCompareTime = thisCopyDate != null ? thisCopyDate : dateTime;
        //Date otherCompareTime = otherCopyDate != null ? otherCopyDate : otherTime;

        //This does not work for follow branches
        Date thisCompareTime = amChildOfCommonParent || thisWs == getCommonParent(thisWs, otherWs) ? dateTime : thisChildOfCommonParentCopyTime;
        Date otherCompareTime = commonBranchPoint == null || (otherTime != null && commonBranchPoint.before(otherTime)) ? otherTime : commonBranchPoint;
        // If one of the times is null, then interpret it as now:
        if (thisCompareTime == null && otherCompareTime != null) {
            thisCompareTime = new Date();
        }
        else if (thisCompareTime != null && otherCompareTime == null) {
            otherCompareTime = new Date();
        }

        // If both times are null then dont need to get commits on common parent

        // Now gather nodes in the common parent chain after otherCompareTime and
        // before thisCompareTime. We need to get these from the transaction history
        // (or potentially the version history) to only include those that
        // changed within a timeframe. Otherwise, we would have to include the
        // entire workspace, which could be master, and that would be too big.
        if ( otherCompareTime != null && thisCompareTime != null &&
             thisCompareTime.after( otherCompareTime ) ) {
//            ArrayList< EmsScriptNode > commits =
//                    CommitUtil.getCommitsInDateTimeRange( otherCompareTime,
//                                                          thisCompareTime,
//                                                          lastParent,
//                                                          targetParent,
//                                                          services,
//                                                          response);
//
//            // TODO -- REVIEW -- The created time of the commit is after the
//            // modified times of the items in the diff (right?). Thus, it is
//            // unclear whether any commits after the later time point can be
//            // ruled out since the nodes in the diff may have been modified long
//            // before the commit was created. For instance if a transaction
//            // includes posting 100 elements serially, then the commit time
//            // could be many seconds after the first element was modified. One
//            // solution would be to give the commit both a start and end time
//            // bounding the times that changes we made in the transaction. We
//            // should not assume that transactions are all atomic (one at a
//            // time); thus, the time interval of one commit may overlap with
//            // others'.
//            for ( EmsScriptNode commit : commits ) {
//                String type = (String)commit.getProperty( "ems:commitType" );
//                if ( "COMMIT".equals( type ) || "MERGE".equals( type )) {
//                    String diffStr = (String)commit.getProperty( "ems:commit" );
//                    if ( Utils.isNullOrEmpty( diffStr ) ) continue;
//                    try {
//                        JSONObject diff = new JSONObject( diffStr );
//
//                        Set< NodeRef > elements =
//                                WorkspaceDiff.getAllChangedElementsInDiffJson( diff,
//                                                                               services,
//                                                                               dateTime);
//                        if ( elements != null )
//                            changedNodeRefs.addAll( elements );
//                    } catch ( JSONException e ) {
//                        String msg = "ERROR! Could not parse json from CommitUtil: \"" + diffStr + "\"";
//                        if ( response != null ) {
//                            response.append( msg + "\n" );
//                            if ( status != null ) {
//                                status.setCode( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
//                                                msg );
//                            }
//                        }
//                        Debug.error( false, msg );
//                        e.printStackTrace();
//                        changedNodeRefs = null;
//                        break;
//                    }
//                }
//            }
        }
        return changedNodeRefs;
    }

    public static JSONObject getChangeJsonWithRespectTo( WorkspaceNode ws1,
                                                         WorkspaceNode ws2,
                                                         Date timestamp1,
                                                         Date timestamp2,
                                                         ServiceRegistry services,
                                                         StringBuffer response,
                                                         Status status ) {
        //TODO This method is only called when getting changes between two times on the same workspace
        // Replace this method with a more specific method since this one doesn't work for the general case anyway
//        ArrayList< EmsScriptNode > commits = null;
//        if (timestamp2 != null && (timestamp1 == null ? timestamp1 == null : timestamp1.after(timestamp2)))
//        {
//            commits = CommitUtil.getCommitsInDateTimeRange( timestamp2, timestamp1,
//                                                            ws1, ws2, services,
//                                                            response );
//        }
//        ArrayList< JSONObject > commitsJson = new ArrayList< JSONObject >();
//        if ( !Utils.isNullOrEmpty( commits ) ) {
//            for ( EmsScriptNode commitNode : commits ) {
//
//                // TODO wrap in transaction if we choose to migrate the commit node here
//                //CommitUtil.migrateCommitNode( commitNode, response, status );
//
//                // If the commit node is not migrated, then return error and stop diff:
//                if (!CommitUtil.checkMigrateCommitNode( commitNode, response, status )) {
//                    status.setCode( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
//                    response.append("The following commit node has not been migrated.  Aborting the diff! "+commitNode );
//                    return null;
//                }
//
//                String content = (String) commitNode.getProperty( "ems:commit" );
//                try {
//                    JSONObject changeJson = new JSONObject(content);
//                    if ( changeJson != null ) {
//                        commitsJson.add( changeJson );
//                    }
//                } catch ( JSONException e ) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        JSONObject glommedCommit = JsonDiffDiff.glom( commitsJson, true );
//        return glommedCommit;
    	return new JSONObject();
    }

    public Set< String > getChangedElementIdsWithRespectTo( WorkspaceNode other, Date dateTime ) {
        Set< String > changedElementIds = new TreeSet< String >();//getChangedElementIds());
        if ( NodeUtil.exists( other ) ) {
            WorkspaceNode targetParent = getCommonParent( other );
            WorkspaceNode parent = this;
            while ( parent != null && !parent.equals( targetParent ) ) {
                changedElementIds.addAll( parent.getChangedElementIds( dateTime ) );
                parent = parent.getParentWorkspace();
            }
        }
        return changedElementIds;
    }

    /**
     * Add the workspace metadata onto the provided JSONObject
     * @param jsonObject
     * @param ws
     * @param dateTime
     * @throws JSONException
     */
    public static void addWorkspaceMetadata(JSONObject jsonObject, WorkspaceNode ws, Date dateTime) throws JSONException {
        addWorkspaceNamesAndIds( jsonObject, ws, false );
        if (dateTime != null) {
            jsonObject.put( "timestamp", TimeUtils.toTimestamp( dateTime ) );
        }
    }

    /**
     * Checks all the sites and sees if the current user is the manager of any of those sites.
     * If this is the case, set "siteManagerPermission" to true in the json.  Otherwise, sets
     * it to false.
     *
     * @param json
     * @param services
     */
    private static void checkSiteManagerPermissions(JSONObject json, ServiceRegistry services)
    {
        boolean siteMgrPerm = false;
        NodeRef siteRef;
        List<SiteInfo> sites = services.getSiteService().listSites(null);
        String user = NodeUtil.getUserName();

        if (!Utils.isNullOrEmpty( user )) {

            // Get all the groups (authorities) for the user:
            List<String> authorityNames = NodeUtil.getUserGroups( user );

            // Loop through all the sites:
            for (SiteInfo siteInfo : sites ) {

                if (siteMgrPerm) {
                    break;
                }

                siteRef = siteInfo.getNodeRef();
                if (siteRef != null) {
                    // Get the permissions for the site:
                    Set< AccessPermission > permList = services.getPermissionService().getAllSetPermissions(siteRef);
                    for (AccessPermission perm : permList) {

                        if (siteMgrPerm) {
                            break;
                        }

                        AccessStatus access = perm.getAccessStatus(); // ALLOWED|DENIED
                        String userOrGrp = perm.getAuthority();
                        String permission = perm.getPermission();

                        if ("SiteManager".equals( permission ) &&
                            AccessStatus.ALLOWED.equals( access )) {

                            // If it is this user:
                            if (user.equals( userOrGrp )) {
                                siteMgrPerm = true;
                            }
                            // If the user is part of the group:
                            else if (authorityNames.contains( userOrGrp )){
                                siteMgrPerm = true;
                            }
                        }
                    }
                }
            }
        }

        json.put( "siteManagerPermission", siteMgrPerm );
    }

    /**
     * Add the workspace name and id metadata onto the provided JSONObject
     * @param jsonObject
     * @param ws
     * @throws JSONException
     */
    public static void
            addWorkspaceNamesAndIds( JSONObject json, WorkspaceNode ws,
                                     boolean chkPermissions ) throws JSONException {
        json.put( "name",  getWorkspaceName(ws) );
        json.put( "id", getId(ws) );
        json.put( "qualifiedName", getQualifiedNameForWorkspace( ws ) );
        json.put( "qualifiedId", getQualifiedIdForWorkspace( ws ) );

        // If it is the master workspace, then determine if the user has permissions,
        // and add a indication to the json:
        if (ws == null && chkPermissions) {
            // Decided not to do this using the site manger, but rather with the ldap group
            //checkSiteManagerPermissions(json, services);
            json.put( "workspaceOperationsPermission", NodeUtil.userHasWorkspaceLdapPermissions());
        }
    }

    @Override
    public JSONObject toJSONObject( WorkspaceNode ws, Date dateTime ) throws JSONException {
        JSONObject json = new JSONObject();

        addWorkspaceNamesAndIds(json, this, false );
        json.put( "creator", getProperty( "cm:modifier", false ) );
        // REVIEW -- This assumes that the workspace does not changed after it
        // is created, but wouldn't it's ems:lastTimeSyncParent property be
        // expected to change?
        json.put( "created", TimeUtils.toTimestamp( (Date)getProperty("cm:created") ) );
        json.put( "modified", TimeUtils.toTimestamp( getLastModified( null ) ) );  // REVIEW -- should we be passing in the date here?
        Date copyTime = getCopyTime();
        if ( copyTime != null ) {
            json.put( "branched", TimeUtils.toTimestamp( copyTime ) );
        }
        json.put( "parent", getId(getParentWorkspace())); // this handles null as master
        String desc = (String)getProperty("ems:description");
        json.put( "description", Utils.isNullOrEmpty( desc ) ? "" : desc );
        String permission = (String)getProperty("ems:permission");
        json.put( "permission", Utils.isNullOrEmpty( permission ) ? "read" : permission );

        // REVIEW -- Why is ems:lastTimeSyncParent called the "branched"
        // date? Shouldn't the branched date always be the same as the created
        // date? This is for future functionality when we track when the child pulls from the
        // parent last.
//        Date lastTimeSyncParent = (Date)getProperty("ems:lastTimeSyncParent");
//        if ( lastTimeSyncParent != null ) {
//            json.put( "branched", TimeUtils.toTimestamp( lastTimeSyncParent ) );
//        }
        return json;
    }

    /**
     * Get the workspace by name, but since two refs can have the same
     * name as long as their parents are different, we need to check the results
     * and at least try to match to the user.
     *
     * @param workspaceName
     * @param services
     * @param response
     * @param responseStatus
     * @param userName
     * @return
     */
    public static WorkspaceNode getWorkspaceFromName( String workspaceName,
                                                    ServiceRegistry services,
                                                    StringBuffer response,
                                                    Status responseStatus,
                                                    //boolean createIfNotFound,
                                                    String userName ) {
        WorkspaceNode workspace = null;

        // Get the workspace by name, but since two refs can have
        // the same name as long as their parents are different, we need
        // to check the results and at least try to match to the user.
        ArrayList< NodeRef > refs =
                NodeUtil.findNodeRefsByType( workspaceName, SearchType.WORKSPACE_NAME.prefix,
                                             /*true,*/ true, null, null,
                                             true, true, services,
                                             false );
        if ( Utils.isNullOrEmpty( refs ) ) {
            return null;
        }
        if ( refs.size() == 1 ) {
            NodeRef ref = refs.get( 0 );
            return existingReadableWorkspaceFromNodeRef( ref, services, response,
                                                         responseStatus );
        }
        boolean matchedUser = false;
        boolean multipleNonMatches = false;
        for ( NodeRef nr : refs ) {
            WorkspaceNode ws = new WorkspaceNode( nr, services );
            EmsScriptNode p = ws.getParent(null, ws, false, true);
            boolean matches = p != null && p.getName().equals( userName );
            if ( !matchedUser ) matchedUser = matches;
            else if ( matches ) {
                String msg = "Warning! Matched multiple refs with name "
                             + workspaceName + " for user " + userName;
                response.append( msg );
                break;
            }
            ws = existingReadableWorkspaceFromNodeRef( nr, services,
                                                       response, responseStatus );
            if ( ws != null ) {
                if ( workspace == null ) {
                    workspace = ws;
                } else if ( matches && !matchedUser ) {
                    workspace = ws;
                    matchedUser = true;
                } else if ( !matches && !matchedUser ) {
                    multipleNonMatches = true;
                }
            }
        }
        if ( !matchedUser && multipleNonMatches ) {
            String msg = "Warning! Matched multiple refs with name "
                        + workspaceName + " but not in user home, " + userName;
            response.append( msg );
        }

        return workspace;
    }

    public static WorkspaceNode existingReadableWorkspaceFromNodeRef( NodeRef ref,
                                                                      ServiceRegistry services,
                                                                      StringBuffer response,
                                                                      Status responseStatus ) {
        if ( ref != null ) {
            WorkspaceNode workspace = new WorkspaceNode( ref, services, response,
                                                         responseStatus );
            // workspace exists should have been checked already
            if ( workspace.hasAspect( "ems:Workspace" ) ) {
                if ( workspace.checkPermissions( PermissionService.READ ) ) {
                    if ( Debug.isOn() ) Debug.outln( "workspace exists: " + workspace );
                    return workspace;
                }
            }
        }
        return null;
    }

    public static WorkspaceNode getWorkspaceFromId( String nameOrId,
                                                    ServiceRegistry services,
                                                    StringBuffer response,
                                                    Status responseStatus,
                                                    //boolean createIfNotFound,
                                                    String userName ) {
        if ( Utils.isNullOrEmpty( nameOrId ) ) {
            if ( Debug.isOn() ) {
                Debug.outln( "no workspace for bad id: " + nameOrId );
            }
            if (responseStatus != null) {
                responseStatus.setCode( HttpServletResponse.SC_BAD_REQUEST, "Workspace not found" );
            }
            return null;
        }
        // Use null to indicate master workspace
        if ( nameOrId.toLowerCase().equals( "master" ) ) {
            return null;
        }
        WorkspaceNode workspace = null;

        // Try to match the alfresco id
        NodeRef ref = NodeUtil.findNodeRefByAlfrescoId( nameOrId, true );
        if ( ref != null ) {
            workspace = existingReadableWorkspaceFromNodeRef( ref, services,
                                                              response,
                                                              responseStatus );
            if ( workspace != null ) return workspace;
        }

        // Try to match the workspace name
        workspace = getWorkspaceFromName( nameOrId, services, response,
                                          responseStatus, userName );

        if ( workspace != null ) return workspace;

        if ( Debug.isOn() ) {
            Debug.outln( "workspace does not exist and is not to be created: "
                         + nameOrId );
        }
        // FIXME: throw exception since nothing is found and null indicates master
        return null;
    }

    /**
     * Get the time after which this workspace does not recognize changes in the
     * input workspace. This the time from which this workspace is
     * copied/branched with respect to its common parent with the input
     * relativeWorkspace.
     *
     * @param relativeWorkspace
     * @return the time from which this and the
     */
    public Date getCopyTime( WorkspaceNode relativeWorkspace ) {

        // WARNING!  This doesn't work for refs that do not have a copyTime!

        Pair< WorkspaceNode, WorkspaceNode > childrenOfCommonParent =
                getChildrenOfCommonParent( relativeWorkspace );
        if ( childrenOfCommonParent == null ) return null;
        if ( childrenOfCommonParent.first == null ) return null;
        Date t = childrenOfCommonParent.first.getCopyTime();
        if ( childrenOfCommonParent.second != null ) {
            Date t2 = childrenOfCommonParent.second.getCopyTime();
            if ( t == null || ( t2 != null && t2.before( t ) ) ) {
                t = t2;
            }
        }
        return t;
    }

}

