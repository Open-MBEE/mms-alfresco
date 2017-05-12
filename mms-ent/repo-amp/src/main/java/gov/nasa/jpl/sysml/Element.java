/**
 *
 */
package gov.nasa.jpl.sysml;

import java.util.Collection;
import java.util.Date;

/**
 *
 */
public interface Element<N, I, D> extends BaseElement<N, I, D> {

    Collection< ? extends Element< N, I, D > > getSuperClasses();

    Collection< ? extends Property< N, I, D > > getProperties();
    Collection< ? extends Property< N, I, D > > getProperty(Object specifier);
    Property< N, I, D > getPropertyWithIdentifier(I specifier);
    Collection< ? extends Property< N, I, D > > getPropertyWithName(N specifier);
    Collection< ? extends Property< N, I, D > > getPropertyWithType(Element<N, I, D> specifier);
    Collection< ? extends Property< N, I, D > > getPropertyWithValue(Object specifier);
}
