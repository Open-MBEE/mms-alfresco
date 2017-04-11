package gov.nasa.jpl.sysml;

public interface Version<N, D, O> {
    // public I getId(); // REVIEW ??????
    public N getLabel();
    public D getTimestamp();
    public O getData();
}
