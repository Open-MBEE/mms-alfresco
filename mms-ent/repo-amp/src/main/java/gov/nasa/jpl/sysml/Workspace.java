package gov.nasa.jpl.sysml;

import gov.nasa.jpl.mbee.util.HasId;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface Workspace< N, I, D > extends HasId<I> {
    public Map< I, BaseElement< N, I, D > > getElements();
    public BaseElement< N, I, D > getElement( I id );
    public BaseElement< N, I, D > getElement( I id, D dateTime );

    public Workspace< N, I, D > getParentWorkspace();
    public Map<String, Workspace< N, I, D > > getChildWorkspaces();

    public Map< D, ChangeSet > getChangeHistory();

    public Workspace< N, I, D > getMaster();

    public < O > Map< Date, Version< N, D, O > > getVersionMap( String id );

    public List< Version< String, Date, BaseElement< N, I, D > > > getVersions( String id );

    public AccessPrivileges getAccessPrivileges( String username,
                                                 BaseElement< N, I, D > element );
}
