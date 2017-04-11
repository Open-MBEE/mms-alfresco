package gov.nasa.jpl.sysml;

public interface AccessPrivileges {
    public boolean canRead();
    public boolean canWrite();
    public boolean canAdd();
    public boolean canDelete();
    public boolean canExecute();
}
