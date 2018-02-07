/**
 *
 */
package gov.nasa.jpl.sysml.impl;

import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.InterpolatedMap;
import gov.nasa.jpl.mbee.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpl.sysml.BaseElement;
import gov.nasa.jpl.sysml.Element;
import gov.nasa.jpl.sysml.Property;
import gov.nasa.jpl.sysml.Version;
import gov.nasa.jpl.sysml.Workspace;

/**
 *
 */
public class ElementImpl extends BaseElementImpl implements Element< String, String, Date > {

    Set< Element< String, String, Date > > superClasses = null;
    Version< String, Date, BaseElement< String, String, Date > > version = null;
    Map<String, PropertyImpl> properties;
    String qualifiedName = null;
    String qualifiedId = null;

    // TODO -- create version
    public ElementImpl( Workspace< String, String, Date > workspace,
                        String id,
                        String name,
                        Version< String, Date, BaseElement< String, String, Date >> version,
                        Map<String, PropertyImpl> properties ) {

        super( workspace, id, name, version );

        this.properties = properties;
    }

    public ElementImpl( ElementImpl e ) {
        super(e.getWorkspace(), e.getId(), e.getName(), e.getVersion());

        Collection<PropertyImpl> props = e.getProperties();
        for ( Property< String, String, Date > prop : props ) {
            try {
                PropertyImpl newValue =
                        (PropertyImpl) prop.clone();
                this.getPropertyMap().put( prop.getId(), newValue );
            } catch ( CloneNotSupportedException e1 ) {
                e1.printStackTrace();
            }
        }
    }

    public ElementImpl( String string ) {
        this( null, null, string, null, null );
        // TODO Auto-generated constructor stub
    }

    @Override
    public ElementImpl clone() throws CloneNotSupportedException {
        return new ElementImpl( this );
    }

    @Override
    public Collection< Element< String, String, Date > > getSuperClasses() {
        if ( superClasses == null ) {
            superClasses = new LinkedHashSet< Element< String, String, Date > >();
        }
        return superClasses;
    }

    @Override
    public Collection<PropertyImpl> getProperties() {
        return getPropertyMap().values();
    }

    public Map<String, PropertyImpl> getPropertyMap() {
        if ( properties == null ) properties =
                new LinkedHashMap<String, PropertyImpl>();
        return properties;
    }

    @Override
    public Collection< Property< String, String, Date > >
        getProperty( Object specifier ) {
        if ( specifier == null ) return null;
        Collection< Property< String, String, Date > > props = null;
        Property< String, String, Date > prop =
                getPropertyWithIdentifier( specifier.toString() );
        if ( prop != null ) {
            props = new ArrayList< Property< String, String, Date > >();
            props.add( prop );
        }
        if ( props == null || props.isEmpty()  ) {
            props = getPropertyWithName( name );
        }
        if ( props == null || props.isEmpty()  ) {
            props = getPropertyWithValue( name );
        }
        return props;
    }

    public Property< String, String, Date > getSingleProperty( Object specifier ) {
        Collection< Property< String, String, Date > > props = getProperty( specifier );
        if ( Utils.isNullOrEmpty( props ) ) {
            return null;
        }
        return props.iterator().next();
    }

    @Override
    public Property< String, String, Date >
           getPropertyWithIdentifier( String id ) {
        if ( id == null ) return null;
        Property< String, String, Date > prop = properties.get( id );
        return prop;
    }

    @Override
    public Collection< Property< String, String, Date > >
           getPropertyWithName( String name ) {
        ArrayList< Property< String, String, Date > > list =
                new ArrayList< Property< String, String, Date > >();
        for ( Property< String, String, Date > prop : getProperties() ) {
            if ( prop.getName() != null && prop.getName().equals( name ) ) {
                list.add( prop );
            }
        }
        return list;
    }

    @Override
    public Collection< Property< String, String, Date > >
           getPropertyWithType( Element< String, String, Date > type ) {
        ArrayList< Property< String, String, Date > > list =
                new ArrayList< Property< String, String, Date > >();
        for ( Property< String, String, Date > prop : getProperties() ) {
            if ( prop.getType() != null && prop.getType().equals( type ) ) {
                list.add( prop );
            }
        }
        return list;
    }

    @Override
    public Collection< Property< String, String, Date > > getPropertyWithValue( Object value ) {
        ArrayList< Property< String, String, Date > > list =
                new ArrayList< Property< String, String, Date > >();
        for ( Property< String, String, Date > prop : getProperties() ) {
            if ( prop.getValue() != null && prop.getValue().equals( value ) ) {
                list.add( prop );
            }
        }
        return list;
    }

    public PropertyImpl addProperty( String name, ElementImpl type, Object value ) {
        // TODO Auto-generated method stub
        return null;
    }
}
