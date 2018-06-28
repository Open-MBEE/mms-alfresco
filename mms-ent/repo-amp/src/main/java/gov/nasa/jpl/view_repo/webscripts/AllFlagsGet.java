package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

public class AllFlagsGet extends FlagSet {

    private static final String[] flags =
            new String[] { "debug",

                           "checkMmsVersions",

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
       if (path.equalsIgnoreCase("checkMmsVersions")){
            AbstractJavaWebScript.checkMmsVersions = val;
        } else if (path.equalsIgnoreCase("skipSvgToPng")){
           EmsScriptNode.skipSvgToPng = val;
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

        if (path.equalsIgnoreCase("checkMmsVersions")){
                return AbstractJavaWebScript.checkMmsVersions;
		} else if (path.equalsIgnoreCase( "skipSvgToPng" )) {
			return EmsScriptNode.skipSvgToPng;
        }
        return false;
    }

    @Override
    protected boolean clear() {
        String path = getPath();

        if (path.equalsIgnoreCase ("debug")) {
            return false;
        } else if (path.equalsIgnoreCase("checkMmsVersions")){
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

        } else if (path.equalsIgnoreCase("checkMmsVersions")){
                return "checkMmsVersions";
        } else if (path.equalsIgnoreCase( "skipSvgToPng")) {
        	    return "skipSvgToPg";
        }
        return null;
    }

}
