package gov.nasa.jpl.sysml;


public interface Property<N, I, D> extends BaseElement< N, I, D > {
    Element<N, I, D> getType();

    Object getValue();
}
