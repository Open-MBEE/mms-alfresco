package gov.nasa.jpl.sysml.impl;

import java.util.Date;

import gov.nasa.jpl.sysml.BaseElement;
import gov.nasa.jpl.sysml.Version;

public class VersionImpl implements Version< String, Date, BaseElement< String, String, Date > > {

    protected String label;
    protected Date timestamp;
    protected BaseElement< String, String, Date > element;

    public VersionImpl( String label, Date timestamp,
                        BaseElement< String, String, Date > element ) {
        super();
        this.label = label;
        this.timestamp = timestamp;
        this.element = element;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public BaseElement< String, String, Date > getData() {
        return element;
    }

}
