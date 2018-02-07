/**
 *
 */
package gov.nasa.jpl.sysml;

import gov.nasa.jpl.mbee.util.HasId;

import java.util.List;
import java.util.Map;

/**
 *
 */
public interface BaseElement<N, I, D> extends HasId<I>, Cloneable {

    @Override I getId();

    Workspace< N, I, D >  getWorkspace();

    N getName();
    N getQualifiedName();

    List< Version< N, D, BaseElement< N, I, D > > > getVersions();
    Map< D, Version< N, D, BaseElement< N, I, D > > > getVersionMap();
    Version< N, D, BaseElement< N, I, D > > getLatestVersion();
    Version< N, D, BaseElement< N, I, D > > getVersion();
    Version< N, D, BaseElement< N, I, D > > getVersion(D dateTime);

    D getCreationTime(); // ??
    D getModifiedTime(); // ??

    void setVersion(Version<N, D, BaseElement<N, I, D>> version);

    BaseElement< N, I, D > clone() throws CloneNotSupportedException;

    BaseElement< N, I, D > getOwner();
    List<? extends BaseElement< N, I, D >> getOwnedElements();

    BaseElement< N, I, D > getProject();

    boolean isStereotypedAs(String name);

    String getTagValue(String name);
}
