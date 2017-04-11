package gov.nasa.jpl.sysml.impl;

import gov.nasa.jpl.mbee.util.CompareUtils;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import gov.nasa.jpl.sysml.BaseElement;
import gov.nasa.jpl.sysml.Element;
import gov.nasa.jpl.sysml.Property;
import gov.nasa.jpl.sysml.Version;
import gov.nasa.jpl.sysml.Workspace;

public class PropertyImpl extends BaseElementImpl implements Property< String, String, Date > {

    protected Element< String, String, Date > type;
    protected Object value;

    public PropertyImpl( PropertyImpl p ) {
        super( p );
        type = p.type;
        value = p.value;
    }

    public PropertyImpl( Workspace< String, String, Date > workspace,
                         String id, String name, Version< String, Date, BaseElement< String, String, Date >> version,
                         Element< String, String, Date > type,
                         Object value ) {
        super( workspace, id, name, version );
        this.type = type;
        this.value = value;
    }

    @Override
    public PropertyImpl clone() {
        return new PropertyImpl( this );
    }

    @Override
    public Element<String, String, Date> getType() {
        return type;
    }

    @Override
    public Object getValue() {
        return value;
    }
}
