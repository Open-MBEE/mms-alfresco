package gov.nasa.jpl.sysml;

import gov.nasa.jpl.mbee.util.HasId;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface Workspace< N, I, D > extends HasId<I> {
    Map< I, BaseElement< N, I, D > > getElements();
    BaseElement< N, I, D > getElement(I id);
    BaseElement< N, I, D > getElement(I id, D dateTime);

    Workspace< N, I, D > getParentWorkspace();
    Map<String, Workspace< N, I, D > > getChildWorkspaces();

    Map< D, ChangeSet > getChangeHistory();

    Workspace< N, I, D > getMaster();

    < O > Map< Date, Version< N, D, O > > getVersionMap(String id);

    List< Version< String, Date, BaseElement< N, I, D > > > getVersions(String id);

    AccessPrivileges getAccessPrivileges(String username, BaseElement<N, I, D> element);
}
