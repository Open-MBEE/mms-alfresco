/**
 *
 */
package gov.nasa.jpl.sysml.impl;

import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.InterpolatedMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.json.JSONObject;

import gov.nasa.jpl.sysml.BaseElement;
import gov.nasa.jpl.sysml.Element;
import gov.nasa.jpl.sysml.Property;
import gov.nasa.jpl.sysml.Version;
import gov.nasa.jpl.sysml.Workspace;
import gov.nasa.jpl.sysml.json_impl.JsonBaseElement;
import gov.nasa.jpl.sysml.json_impl.JsonSystemModel;

/**
 *
 */
public class BaseElementImpl implements BaseElement< String, String, Date >, Comparable<BaseElementImpl> {

    Workspace< String, String, Date > workspace;
    String id;
    String name;

    Version< String, Date, BaseElement< String, String, Date > > version = null;

    String qualifiedName = null;
    String qualifiedId = null;

    public BaseElementImpl( Workspace< String, String, Date > workspace,
          String id,
          String name,
          Version< String, Date, BaseElement< String, String, Date >> version) {

        this.workspace = workspace;
        this.id = id;
        this.name = name;
        this.version = version;
    }

    public BaseElementImpl( BaseElementImpl elem) {
       this(elem.getWorkspace(), elem.getId(), elem.getName(), elem.getVersion());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getQualifiedName() {
        // TODO: implement
        return null;
    }

    @Override
    public BaseElementImpl getProject() {
        // TODO: implement
        return null;
    }

    @Override
    public boolean isStereotypedAs(String name)
    {
       // TODO: implement
       return false;
    }

    @Override
    public String getTagValue(String name)
    {
       // TODO: implement
       return null;
    }

    @Override
    public Workspace< String, String, Date > getWorkspace() {
        return workspace;
    }

    @Override
    public BaseElement<String, String, Date> getOwner() {
        return null;
    }

    @Override
    public List<JsonBaseElement> getOwnedElements()
    {
       // TODO: implement
       return null;
    }

    @Override
    public List< Version< String, Date, BaseElement< String, String, Date > > > getVersions() {
        return getWorkspace().getVersions( getId() );
    }

    @Override
    public Map< Date, Version< String, Date, BaseElement< String, String, Date > > > getVersionMap() {
        return getWorkspace().getVersionMap( getId() );
    }

    @Override
    public Version< String, Date, BaseElement< String, String, Date > > getLatestVersion() {
        List< Version< String, Date, BaseElement< String, String, Date > > > versions =
                getVersions();
        if ( versions.isEmpty() ) {
            if ( version != null ) {
                // TODO -- REVIEW -- should this case never happen?
                return version;
            }
            return null;
        }
        return versions.get( versions.size() - 1 );
    }

    @Override
    public Version< String, Date, BaseElement< String, String, Date > > getVersion() {
        return this.version;
    }

    @Override
    public Version< String, Date, BaseElement< String, String, Date > > getVersion( Date dateTime ) {
        Map< Date, Version< String, Date, BaseElement< String, String, Date > > > map =
                getVersionMap();
        InterpolatedMap< Date, Version< String, Date, BaseElement< String, String, Date > > > imap;
        if ( map instanceof InterpolatedMap ) {
            imap = (InterpolatedMap< Date, Version< String, Date, BaseElement< String, String, Date > > >)map;
        } else {
            imap = new InterpolatedMap< Date, Version< String, Date, BaseElement< String, String, Date > > >( map );
        }
        return imap.get( dateTime );
    }

    @Override
    public Date getCreationTime() {
        version = getVersion();
        if ( version == null ) {
            return null;
        } else {
            return version.getTimestamp();
        }
    }

    @Override
    public Date getModifiedTime() {
        version = getLatestVersion();
        if ( version == null ) {
            return null;
        } else {
            return version.getTimestamp();
        }
    }

    @Override
    public void setVersion( Version< String, Date, BaseElement< String, String, Date > > version ) {
        this.version = version;
    }

    @Override
    public BaseElement<String, String, Date> clone() throws CloneNotSupportedException
    {
       return new BaseElementImpl(this);
    }

    @Override
    public int compareTo( BaseElementImpl o ) {
        if ( this == o ) return 0;
        if ( o == null ) return 1;
        int comp = CompareUtils.compare( getWorkspace(), o.getWorkspace() );
        if ( comp != 0 ) return comp;
        comp = CompareUtils.compare( getId(), o.getId() );
        if ( comp != 0 ) return comp;
        comp = CompareUtils.compare( getName(), o.getName() );
        return comp;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == null ) return false;

        if( o instanceof BaseElementImpl )
        {
           return compareTo( (BaseElementImpl)o ) == 0;
        }
        else
        {
           return false;
        }
    }

    @Override
    public int hashCode()
    {
       StringBuilder sb = new StringBuilder();
       sb.append(getWorkspace() == null ? "" : getWorkspace().getId());
       sb.append("_");
       sb.append(getId());
       sb.append("_");
       sb.append(getName());

       return sb.toString().hashCode();
    }
}
