package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;

public class AllFlagsGet extends FlagSet {

    public static String[] flags =
            new String[] { "debug",
                           "fullCache",
                           "nodeAtTimeCache",
                           "heisenCache",
                           "jsonCache",
                           "jsonDeepCache",
                           "jsonStringCache",
                           "modelPostTimeEvents",
                           "propertyCache",
                           "simpleCache",
                           "syncTransactions",
                           "timeEvents",
                           "versionCacheDebug",
                           "versionCache",
                           "versionHistoryCache",
                           "skipWorkspacePermissionCheck",
                           "optimisticJustFirst",
                           "cleanJson",
                           "viewPointExpressions",
                           "diffDefaultIsMerge",
                           "cacheSnapshots",
                           "checkMmsVersions",
                           "graphDb",
                           "postProcessQualified",
                           "doorsSync",
                           "autoBuildGraphDb",
                           "skipQualified",
                           "skipSvgToPng"};

    @Override
	public String[] getAllFlags() {
        return flags;
    }

    protected String getPath() {
        String path = req.getPathInfo();
        logger.debug(String.format("%s", path));
        String result = path.replace("/flags/","").replace("/","");
        if ( result.equals( "" ) || result.equals( "flags" ) ) result = "all";
        return result;
    }

    @Override
    protected boolean set( boolean val ) {
        String path = getPath();

        if (path.equalsIgnoreCase( "all" )) {
            return false;
        }

        if (path.equalsIgnoreCase ("debug")) {
            if ( val ) Debug.turnOn();
            else Debug.turnOff();
        } else if (path.equalsIgnoreCase("fullCache")) {
            NodeUtil.doFullCaching = val;
        } else if (path.equalsIgnoreCase("nodeAtTimeCache")) {
            NodeUtil.doNodeAtTimeCaching = val;
        } else if (path.equalsIgnoreCase("heisenCache")) {
            NodeUtil.doHeisenCheck = val;
        } else if (path.equalsIgnoreCase("jsonCache")) {
            // if turning on, flush cache since it might be wrong
            if ( !NodeUtil.doJsonCaching && val ) {
                NodeUtil.jsonCache.clear();
                NodeUtil.jsonDeepCache.clear();
            }
            NodeUtil.doJsonCaching = val;
        } else if (path.equalsIgnoreCase("jsonDeepCache")) {
            // if turning on, flush cache since it might be wrong
            if ( !NodeUtil.doJsonDeepCaching && val ) {
                NodeUtil.jsonCache.clear();
                //NodeUtil.jsonDeepCache.clear(); // simple json cache does not depend on deep cache
            }
            NodeUtil.doJsonCaching = val;
        } else if (path.equalsIgnoreCase("jsonStringCache")) {
            NodeUtil.doJsonStringCaching = val;
        } else if (path.equalsIgnoreCase("modelPostTimeEvents")) {
            ModelPost.timeEvents = val;
        } else if (path.equalsIgnoreCase("propertyCache")) {
            if ( !NodeUtil.doPropertyCaching && val ) {
                NodeUtil.propertyCache.clear();
            }
            NodeUtil.doPropertyCaching = val;
        } else if (path.equalsIgnoreCase("simpleCache")) {
            NodeUtil.doSimpleCaching = val;
        } else if (path.equalsIgnoreCase("syncTransactions")) {
            EmsTransaction.syncTransactions = val;
        } else if (path.equalsIgnoreCase("timeEvents")) {
            NodeUtil.timeEvents = val;
        } else if (path.equalsIgnoreCase("versionCacheDebug")) {
            EmsScriptNode.versionCacheDebugPrint = val;
        }  else if (path.equalsIgnoreCase("viewpointExpressions")) {
            if ( val && !EmsScriptNode.expressionStuffDefault ) {
                NodeUtil.jsonCache.clear();
            }
            EmsScriptNode.expressionStuffDefault = val;
            //EmsScriptNode.addingAffectedIds = val;
        } else if (path.equalsIgnoreCase("versionCache")) {
            NodeUtil.doVersionCaching = val;
        } else if (path.equalsIgnoreCase("versionHistoryCache")) {
            NodeUtil.doVersionHistoryCaching = val;
        } else if (path.equalsIgnoreCase("skipWorkspacePermissionCheck")) {
            NodeUtil.skipWorkspacePermissionCheck = val;
        } else if (path.equalsIgnoreCase("optimisticJustFirst")) {
            NodeUtil.doOptimisticJustFirst = val;
        } else if (path.equalsIgnoreCase("cleanJson")) {
                CommitUtil.cleanJson = val;
        } else if (path.equalsIgnoreCase("cacheSnapshots")) {
            DeclarativeJavaWebScript.cacheSnapshotsFlag = val;
        } else if (path.equalsIgnoreCase("checkMmsVersions")){
            AbstractJavaWebScript.checkMmsVersions = val;
        } else if (path.equalsIgnoreCase("graphDb")) {
            NodeUtil.doGraphDb = val;
        } else if (path.equalsIgnoreCase("postProcessQualified")) {
            NodeUtil.doPostProcessQualified = val;
        } else if (path.equalsIgnoreCase("doorsSync")) {
            NodeUtil.doorsSync = val;
        } else if (path.equalsIgnoreCase("autoBuildGraphDb")) {
            NodeUtil.doAutoBuildGraphDb = val;
        } else if (path.equalsIgnoreCase("skipQualified")) {
            NodeUtil.skipQualified = val;
        } else if (path.equalsIgnoreCase("skipSvgToPng")){
            NodeUtil.skipSvgToPng = val;
        }
        return true;
    }

    @Override
    protected boolean get() {
        String path = getPath();
        return get( path );
    }
    @Override
    protected boolean get( String path ) {
        if (path.equalsIgnoreCase( "all" )) {
            return true;
        }

        if (path.equalsIgnoreCase ("debug")) {
            return Debug.isOn();
        } else if (path.equalsIgnoreCase("fullCache")) {
            return NodeUtil.doFullCaching;
        } else if (path.equalsIgnoreCase("nodeAtTimeCache")) {
            return NodeUtil.doNodeAtTimeCaching;
        } else if (path.equalsIgnoreCase("heisenCache")) {
            return NodeUtil.doHeisenCheck;
        } else if (path.equalsIgnoreCase("jsonCache")) {
            return NodeUtil.doJsonCaching;
        } else if (path.equalsIgnoreCase("jsonDeepCache")) {
            return NodeUtil.doJsonDeepCaching;
        } else if (path.equalsIgnoreCase("jsonStringCache")) {
            return NodeUtil.doJsonStringCaching;
        } else if (path.equalsIgnoreCase("modelPostTimeEvents")) {
            return ModelPost.timeEvents;
        } else if (path.equalsIgnoreCase("propertyCache")) {
            return NodeUtil.doPropertyCaching;
        } else if (path.equalsIgnoreCase("simpleCache")) {
            return NodeUtil.doSimpleCaching;
        } else if (path.equalsIgnoreCase("syncTransactions")) {
            return EmsTransaction.syncTransactions;
        } else if (path.equalsIgnoreCase("timeEvents")) {
            return NodeUtil.timeEvents;
        } else if (path.equalsIgnoreCase("versionCacheDebug")) {
            return EmsScriptNode.versionCacheDebugPrint;
        }  else if (path.equalsIgnoreCase("viewPointExpressions")) {
            return EmsScriptNode.expressionStuffDefault;
        }  else if (path.equalsIgnoreCase("versionCache")) {
            return NodeUtil.doVersionCaching;
        } else if (path.equalsIgnoreCase("versionHistoryCache")) {
            return NodeUtil.doVersionHistoryCaching;
        } else if (path.equalsIgnoreCase("skipWorkspacePermissionCheck")) {
            return NodeUtil.skipWorkspacePermissionCheck;
        } else if (path.equalsIgnoreCase("optimisticJustFirst")) {
            return NodeUtil.doOptimisticJustFirst;
        } else if (path.equalsIgnoreCase("cleanJson")) {
                return CommitUtil.cleanJson;
        } else if (path.equalsIgnoreCase( "graphDb" )) {
            return NodeUtil.doGraphDb;
        } else if (path.equalsIgnoreCase("checkMmsVersions")){
                return AbstractJavaWebScript.checkMmsVersions;
        } else if (path.equalsIgnoreCase( "postProcessQualified" )) {
            return NodeUtil.doPostProcessQualified;
        } else if (path.equalsIgnoreCase( "doorsSync" )) {
            return NodeUtil.doorsSync;
        } else if (path.equalsIgnoreCase( "autoBuildGraphDb" )) {
            return NodeUtil.doAutoBuildGraphDb;
        } else if (path.equalsIgnoreCase( "skipQualified" )) {
            return NodeUtil.skipQualified;
		} else if (path.equalsIgnoreCase( "skipSvgToPng" )) {
			return NodeUtil.skipSvgToPng;
        }
        return false;
    }

    @Override
    protected boolean clear() {
        String path = getPath();

        if (path.equalsIgnoreCase( "all" )) {
            NodeUtil.elementCache.clear();
            NodeUtil.nodeAtTimeCache.clear();
            NodeUtil.jsonCache.clear();
            NodeUtil.jsonDeepCache.clear();
            NodeUtil.jsonStringCache.clear();
            NodeUtil.propertyCache.clear();
            NodeUtil.simpleCache.clear();
            NodeUtil.versionCache.clear();
            NodeUtil.versionHistoryCache.clear();
            return true;
        }

        if (path.equalsIgnoreCase("fullCache")) {
            NodeUtil.elementCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("nodeAtTimeCache")) {
            NodeUtil.nodeAtTimeCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("jsonCache")) {
            NodeUtil.jsonCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("jsonDeepCache")) {
            NodeUtil.jsonDeepCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("jsonStringCache")) {
            NodeUtil.jsonStringCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("propertyCache")) {
            NodeUtil.propertyCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("simpleCache")) {
            NodeUtil.simpleCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("versionCache")) {
            NodeUtil.versionCache.clear();
            return true;
        } else if (path.equalsIgnoreCase("versionHistoryCache")) {
            NodeUtil.versionHistoryCache.clear();
            return true;
        } else if (path.equalsIgnoreCase ("debug")) {
            return false;
        } else if (path.equalsIgnoreCase("heisenCheck")) {
            return false;
        } else if (path.equalsIgnoreCase("modelPostTimeEvents")) {
            return false;
        } else if (path.equalsIgnoreCase("syncTransactions")) {
            return false;
        } else if (path.equalsIgnoreCase("timeEvents")) {
            return false;
        } else if (path.equalsIgnoreCase("versionCacheDebug")) {
            return false;
        } else if (path.equalsIgnoreCase("viewPointExpressions")) {
                return false;
        } else if (path.equalsIgnoreCase("skipWorkspacePermissionCheck")) {
            return false;
        } else if (path.equalsIgnoreCase("optimisticJustFirst")) {
            return false;
        } else if (path.equalsIgnoreCase("cleanJson")) {
                return false;
        } else if (path.equalsIgnoreCase("checkMmsVersions")){
                return false;
        } else if (path.equalsIgnoreCase( "doGraphDb" )) {
            return false;
        } else if (path.equalsIgnoreCase( "doPostProcessQualified" )) {
            return false;
        } else if (path.equalsIgnoreCase( "doorsSync" )) {
            return false;
        } else if (path.equalsIgnoreCase( "autoBuildGraphDb" )) {
            return false;
        } else if (path.equalsIgnoreCase( "skipQualified" )) {
            return false;
        } else if (path.equalsIgnoreCase( "skipSvgToPng" )) {
        	    return false;
        }
        return false;
    }


    @Override
    protected String flag() {
        return getPath();
    }

    @Override
    protected String flagName() {
        String path = getPath();

        if (path.equalsIgnoreCase( "all" )) {
            return "all";
        }

        if (path.equalsIgnoreCase ("debug")) {
            return "debug";
        } else if (path.equalsIgnoreCase("fullCache")) {
            return "doFullCaching";
        } else if (path.equalsIgnoreCase("nodeAtTimeCache")) {
            return "doNodeAtTimeCaching";
        } else if (path.equalsIgnoreCase("heisenCache")) {
            return "doHeisenCheck";
        } else if (path.equalsIgnoreCase("jsonCache")) {
            return "doJsonCaching";
        } else if (path.equalsIgnoreCase("jsonDeepCache")) {
            return "doJsonDeepCaching";
        } else if (path.equalsIgnoreCase("jsonStringCache")) {
            return "doJsonStringCaching";
        } else if (path.equalsIgnoreCase("modelPostTimeEvents")) {
            return "timeEvents";
        } else if (path.equalsIgnoreCase("propertyCache")) {
            return "doPropertyCaching";
        } else if (path.equalsIgnoreCase("simpleCache")) {
            return "doSimpleCaching";
        } else if (path.equalsIgnoreCase("syncTransactions")) {
            return "syncTransactions";
        } else if (path.equalsIgnoreCase("timeEvents")) {
            return "timeEvents";
        } else if (path.equalsIgnoreCase("versionCacheDebug")) {
            return "versionCacheDebugPrint";
        } else if (path.equalsIgnoreCase("viewPointExpressions")) {
            return "expressionStuff";
        } else if (path.equalsIgnoreCase("versionCache")) {
            return "doVersionCaching";
        } else if (path.equalsIgnoreCase("versionHistoryCache")) {
            return "doVersionHistoryCaching";
        } else if (path.equalsIgnoreCase("skipWorkspacePermissionCheck")) {
            return "skipWorkspacePermissionCheck";
        } else if (path.equalsIgnoreCase("optimisticJustFirst")) {
            return "doOptimisticJustFirst";
        } else if (path.equalsIgnoreCase("cleanJson")) {
                return "cleanJson";
        } else if (path.equalsIgnoreCase("cacheSnapshots")) {
            return "cacheSnapshotsFlag";
        } else if (path.equalsIgnoreCase("checkMmsVersions")){
                return "checkMmsVersions";
        } else if (path.equalsIgnoreCase("graphDb")) {
            return "graphDb";
        } else if (path.equalsIgnoreCase("postProcessQualified")) {
            return "postProcessQualified";
        } else if (path.equalsIgnoreCase("doorsSync")) {
            return "doorsSync";
        } else if (path.equalsIgnoreCase( "autoBuildGraphDb" )) {
            return "autoBuildGraphDb";
        } else if (path.equalsIgnoreCase("skipQualified")) {
            return "skipQualified";
        } else if (path.equalsIgnoreCase( "skipSvgToPng")) {
        	    return "skipSvgToPg";
        }
        return null;
    }

}
