package gov.nasa.jpl.sysml.view;

import java.util.Collection;

/**
 * A View embeds {@link Viewable}s.
 *
 */
public interface View< E > extends Viewable< E > {
    public Collection< View< E > > getChildViews();
    public E getElement();
    public void setElement( E element );
}
