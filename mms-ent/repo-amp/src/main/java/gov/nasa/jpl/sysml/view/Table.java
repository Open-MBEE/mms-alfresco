package gov.nasa.jpl.sysml.view;

public interface Table< E > extends List< E > {
    public List< E > getColumns();
    public List< E > getColumn( int i );
    public Viewable< E > getCell( int i, int j );
}
