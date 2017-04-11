package gov.nasa.jpl.sysml.view;

/**
 * a list of {@link Viewable}s to embed in a {@link View}
 *
 */
public interface List< E > extends java.util.List< Viewable< E > >, Viewable< E > {
}
