package gov.nasa.jpl.sysml;


public interface Property<N, I, D> extends BaseElement< N, I, D > {
    public Element<N, I, D> getType();

    public Object getValue();
}
