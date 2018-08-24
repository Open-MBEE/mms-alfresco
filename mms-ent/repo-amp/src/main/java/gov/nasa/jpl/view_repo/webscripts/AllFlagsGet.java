package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;

public class AllFlagsGet extends FlagSet {

    private static final String[] flags =
            new String[] { "debug"};

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
        return false;
    }

    @Override
    protected boolean clear() {
        String path = getPath();
        if (path.equalsIgnoreCase ("debug")) {
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
        }
        return null;
    }

}
