package gov.nasa.jpl.sysml.view;

public interface Table< E > extends List< E > {
    List< E > getColumns();
    List< E > getColumn(int i);
    Viewable< E > getCell(int i, int j);
}
