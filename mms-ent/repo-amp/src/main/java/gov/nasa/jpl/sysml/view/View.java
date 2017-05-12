package gov.nasa.jpl.sysml.view;

import java.util.Collection;

/**
 * A View embeds {@link Viewable}s.
 *
 */
public interface View< E > extends Viewable< E > {
    Collection< View< E > > getChildViews();
    E getElement();
    void setElement(E element);
}
