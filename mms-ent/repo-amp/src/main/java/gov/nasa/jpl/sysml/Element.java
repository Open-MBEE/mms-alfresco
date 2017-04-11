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

    public Collection< ? extends Element< N, I, D > > getSuperClasses();

    public Collection< ? extends Property< N, I, D > > getProperties();
    public Collection< ? extends Property< N, I, D > > getProperty( Object specifier );
    public Property< N, I, D > getPropertyWithIdentifier( I specifier );
    public Collection< ? extends Property< N, I, D > > getPropertyWithName( N specifier );
    public Collection< ? extends Property< N, I, D > > getPropertyWithType( Element< N, I, D > specifier );
    public Collection< ? extends Property< N, I, D > > getPropertyWithValue( Object specifier );
}
