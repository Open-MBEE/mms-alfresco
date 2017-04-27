package gov.nasa.jpl.sysml;

public interface Version<N, D, O> {
    // public I getId(); // REVIEW ??????
    N getLabel();
    D getTimestamp();
    O getData();
}
