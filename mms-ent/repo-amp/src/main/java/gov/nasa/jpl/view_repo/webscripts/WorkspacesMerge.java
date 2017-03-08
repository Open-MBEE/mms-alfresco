//package gov.nasa.jpl.view_repo.webscripts;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Set;
//
//import javax.servlet.http.HttpServletResponse;
////import javax.transaction.UserTransaction;
//
//import org.alfresco.repo.model.Repository;
//import org.alfresco.service.ServiceRegistry;
//import org.apache.log4j.Level;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.springframework.extensions.webscripts.Cache;
//import org.springframework.extensions.webscripts.Status;
//import org.springframework.extensions.webscripts.WebScriptRequest;
//
//import gov.nasa.jpl.mbee.util.Utils;
//import gov.nasa.jpl.view_repo.util.EmsScriptNode;
//import gov.nasa.jpl.view_repo.util.EmsTransaction;
//import gov.nasa.jpl.view_repo.util.NodeUtil;
//import gov.nasa.jpl.view_repo.util.WorkspaceDiff;
//import gov.nasa.jpl.view_repo.util.WorkspaceNode;
//
//@Deprecated
//public class WorkspacesMerge extends AbstractJavaWebScript{
//
//    public WorkspacesMerge(){
//        super();
//    }
//
//    public WorkspacesMerge(Repository repositoryHelper, ServiceRegistry registry){
//        super(repositoryHelper, registry);
//    }
//
//    @Override
//    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache){
//        WorkspacesMerge instance = new WorkspacesMerge(repository, getServices());
//        // Run without transactions since WorkspacesMerge breaks them up itself.
//        return instance.executeImplImpl( req, status, cache );
//    }
//
//    private JSONObject tmpResult;
//    @Override
//    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache){
//        printHeader(req);
//        //clearCaches();
//        Map<String, Object> model = new HashMap<>();
//        JSONObject result = new JSONObject();
//        try{
//            if(validateRequest(req, status)){
//                final String targetId = req.getParameter("target");
//                final WorkspaceNode targetWS =
//                        WorkspaceNode.getWorkspaceFromId( targetId,
//                                                                  getServices(),
//                                                                  getResponse(),
//                                                                  status,
//                                                                  //false,
//                                                                  null );
//
//                final String sourceId = req.getParameter("source");
//                WorkspaceNode sourceWS =
//                        WorkspaceNode.getWorkspaceFromId( sourceId,
//                                                                  getServices(),
//                                                                  getResponse(),
//                                                                  status,
//                                                                  //false,
//                                                                  null );
//
//                wsDiff = new WorkspaceDiff(targetWS, sourceWS, null /*time*/, null /*time*/);
//
//                //For the nodes here, we delete them from the source
//                Map<String, EmsScriptNode> deletedElements = wsDiff.getDeletedElements();
//                final Collection <EmsScriptNode> deletedCollection = deletedElements.values();
//
//
//                // Prints out the differences after merging.
//                JSONObject top = wsDiff.toJSONObject(null, null /*time*/, false);
//                Iterator< ? > iter = top.keys();
//                while ( iter.hasNext() ) {
//                    String key = "" + iter.next();
//                    JSONObject object = top.optJSONObject(key);
//                    Iterator< ? > iter2 = object.keys();
//                    while(iter2.hasNext() ) {
//                        String key2 = "" + iter2.next();
//                        JSONArray jArray = object.optJSONArray( key2 );
//                        if(jArray != null) {
//                            for(int i = 0; i < jArray.length(); i++) {
//                                JSONObject obj = jArray.getJSONObject(i);
//                                if(obj.has("read")) obj.remove( "read" );
//                            }
//                        }
//                    }
//                }
//
//                // Retrieving the arrays for all the added elements
//                ModelPost instance = new ModelPost(repository, services);
//
//                // Error here, projectNode isn't 123456, but rather no_project.
//                // TODO
//                EmsScriptNode projectNode = null; //instance.getProjectNodeFromRequest(req, true);
//                if (projectNode != null) {
//                    final Set< EmsScriptNode > elements = null;
//                            //instance.createOrUpdateModel( top.getJSONObject("workspace2"), status,
//                              //                            targetWS, sourceWS, true );
//                    // REVIEW -- TODO -- shouldn't this be called from instance?
//                    //instance.addRelationshipsToProperties( elements, targetWS );
//
//                    tmpResult = null;
//                    new EmsTransaction(services, response, null, runWithoutTransactions ) {
//
//                        @Override
//                        public void run() throws Exception {
//                            if ( !Utils.isNullOrEmpty( elements ) ) {
//                                NodeUtil.setInsideTransactionNow( true );
//                                // Create JSON object of the elements to return:
//                                JSONArray elementsJson = new JSONArray();
//                                for ( EmsScriptNode element : elements ) {
//                                    elementsJson.put( element.toJSONObject(targetWS, null) );
//                                }
//                           //top.put( "elements", elementsJson );
//                            //model.put( "res", NodeUtil.jsonToString( top, 4 ) );
//                            }
//                            tmpResult = handleDelete(deletedCollection, targetWS, targetId, null /*time*/, wsDiff);
//                        }
//                    };
//                    if ( tmpResult != null ) result = tmpResult;
//
//                    // FIXME!! We can't just leave the changes on the merged
//                    // branch! If an element is changed in the parent, it could
//                    // result in a conflict! But we can't mark them deleted since
//                    // that would be making changes in the workspace. Can we
//                    // purge???!!! Do we need another aspect, ems:Purged? Do we
//                    // check to see if the last commit in the history is before the
//                    // lastTimeSync?
//
//
//                    // keep history of the branch
//                //CommitUtil.merge( sourceWS, targetWS, "", false,
//                  //                    services, response );
//                    // TODO
//                }
//            }
//         } catch (JSONException e) {
//               log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create JSON\n");
//               e.printStackTrace();
//            } catch (Exception e) {
//               log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error\n");
//               e.printStackTrace();
//            }
//        if (result == null) {
//             model.put( "res", createResponseJson() );
//         } else {
//             try {
//                 result.put("message", response.toString());
//                 model.put("res",  NodeUtil.jsonToString( result, 2 ));
//             } catch (JSONException e) {
//                 e.printStackTrace();
//                 model.put( "res", createResponseJson() );
//             }
//         }
//        status.setCode(responseStatus.getCode());
//        return model;
//    }
//
//    // Essentially the same executeImpl code from MmsModelDelete
//
//    protected JSONObject handleDelete(Collection <EmsScriptNode> collection, WorkspaceNode workspace, String wsId, Date time, WorkspaceDiff workspaceDiff) {
//        return null;
//
//        /*
//        JSONObject result = null;
//        MmsModelDelete deleteInstance = new MmsModelDelete(repository, services);
//        long start = System.currentTimeMillis();
//        Collection <EmsScriptNode> tempCollection = new ArrayList< EmsScriptNode >();
//        for( EmsScriptNode node : collection)
//            tempCollection.add(node);
//        for( EmsScriptNode node : tempCollection){
//            if(node != null && node.exists()){
//                deleteInstance.delete(node, workspace, workspaceDiff);
//                EmsScriptNode pkgnode = findScriptNodeById(node.getSysmlId() + "_pkg", workspace, time, false);
//                // After this step, my collection has an increased element
//                deleteInstance.handleElementHierarchy(pkgnode, workspace, true);
//            } else {
//                log( Level.ERROR,
//                        HttpServletResponse.SC_NOT_FOUND, "Could not find node %s in workspace %s", node.getSysmlId(), wsId);
//                return result;
//            }
//        }
//        //String siteName = node.getSiteName();
//        long end = System.currentTimeMillis();
//        try{
//            result = workspaceDiff.toJSONObject(new Date(start),new Date(end), false);
//            for( EmsScriptNode node: collection) {
//                // editting the JSON
//                node.removeAspect( "ems:Added" );
//                node.removeAspect( "ems:Updated" );
//                node.removeAspect( "ems:Moved" );
//                node.createOrUpdateAspect( "ems:Deleted" );
//                }
//        } catch (JSONException e) {
//            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Malformed JSON Object");
//            e.printStackTrace();
//        }
//
//        return result;
//        */
//    }
//
//    protected Collection<EmsScriptNode> setsToCollection(Collection< Collection <EmsScriptNode> > sets){
//        Collection <EmsScriptNode> collection = new ArrayList<EmsScriptNode>();
//        for(Collection <EmsScriptNode> set : sets){
//            collection.addAll(set);
//            }
//        return collection;
//    }
//
//    @Override
//    protected boolean validateRequest(WebScriptRequest req, Status status) {
//        // TODO Auto-generated method stub
//        String targetId = req.getParameter( "target" );
//        String sourceId = req.getParameter( "source" );
//        WorkspaceNode ws1 =
//                WorkspaceNode.getWorkspaceFromId( targetId, getServices(), response, status, //false
//                                    null );
//        WorkspaceNode ws2 =
//                WorkspaceNode.getWorkspaceFromId( sourceId, getServices(), response, status, //false
//                                    null );
//        boolean wsFound1 = ( ws1 != null || ( targetId != null && targetId.equalsIgnoreCase( "master" ) ) );
//        boolean wsFound2 = ( ws2 != null || ( sourceId != null && sourceId.equalsIgnoreCase( "master" ) ) );
//
//        if ( !wsFound1 ) {
//            log( Level.ERROR,
//                 HttpServletResponse.SC_NOT_FOUND, "Workspace 1 id , %s , not found", targetId);
//            return false;
//        }
//        if ( !wsFound2 ) {
//            log( Level.ERROR,
//                 HttpServletResponse.SC_NOT_FOUND,  "Workspace 2 id, %s, not found", sourceId);
//            return false;
//        }
//        return true;
//    }
//}
