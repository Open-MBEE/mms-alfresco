package gov.nasa.jpl.sysml.view;

/**
 * an Image to embed in a {@link View}
 *
 */
public interface Image<E> extends Viewable<E> {
    String getTitle();
}
