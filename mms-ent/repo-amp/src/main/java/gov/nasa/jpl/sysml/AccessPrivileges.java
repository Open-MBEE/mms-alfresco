package gov.nasa.jpl.sysml;

public interface AccessPrivileges {
    boolean canRead();
    boolean canWrite();
    boolean canAdd();
    boolean canDelete();
    boolean canExecute();
}
