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

    @Override
    public I getId();

    public Workspace< N, I, D >  getWorkspace();

    public N getName();
    public N getQualifiedName();

    public List< Version< N, D, BaseElement< N, I, D > > > getVersions();
    public Map< D, Version< N, D, BaseElement< N, I, D > > > getVersionMap();
    public Version< N, D, BaseElement< N, I, D > > getLatestVersion();
    public Version< N, D, BaseElement< N, I, D > > getVersion();
    public Version< N, D, BaseElement< N, I, D > > getVersion( D dateTime );

    public D getCreationTime(); // ??
    public D getModifiedTime(); // ??

    public void setVersion( Version< N, D, BaseElement< N, I, D > > version );

    public BaseElement< N, I, D > clone() throws CloneNotSupportedException;

    public BaseElement< N, I, D > getOwner();
    public List<? extends BaseElement< N, I, D >> getOwnedElements();

    public BaseElement< N, I, D > getProject();

    public boolean isStereotypedAs(String name);

    public String getTagValue(String name);
}
