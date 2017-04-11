package gov.nasa.jpl.mbee.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Diff represents the difference between two sets of objects of type T.
 * T may have properties of type P that each have an identifier of type ID.
 *
 * @param <T>
 * @param <P>
 * @param <ID>
 */
public interface Diff< T, P, ID > {

    public abstract boolean areDifferent();

    public abstract boolean areSame();

    public abstract Set<T> get1();

    public abstract Set<T> get2();

    public abstract T get1( ID tid );

    public abstract T get2( ID tid );

    public abstract P get1( ID tid, ID pid );

    public abstract P get2( ID tid, ID pid );

    public abstract Set< T > getRemoved();

    public abstract Set< T > getAdded();

    public abstract Set< T > getUpdated();

    public abstract Map< ID, Map< ID, P > > getRemovedProperties();

    public abstract Map< ID, Map< ID, P > > getAddedProperties();

    public abstract Map< ID, Map< ID, Pair< P, P > > > getUpdatedProperties();

    public abstract Map< ID, Map< ID, Pair< P, P > > > getPropertyChanges();

    public abstract void addPropertyIdsToIgnore( Collection<ID> ids );

    /**
     * @param ids
     *            The IDs of properties that should be left out of the property
     *            diff results. This is not used to filter the objects returned
     *            by {@link #get1()} and {@link #get2()}.
     */
    public abstract Set<ID> getPropertyIdsToIgnore();

}
