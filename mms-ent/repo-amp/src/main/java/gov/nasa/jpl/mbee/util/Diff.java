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

    boolean areDifferent();

    boolean areSame();

    Set<T> get1();

    Set<T> get2();

    T get1(ID tid);

    T get2(ID tid);

    P get1(ID tid, ID pid);

    P get2(ID tid, ID pid);

    Set< T > getRemoved();

    Set< T > getAdded();

    Set< T > getUpdated();

    Map< ID, Map< ID, P > > getRemovedProperties();

    Map< ID, Map< ID, P > > getAddedProperties();

    Map< ID, Map< ID, Pair< P, P > > > getUpdatedProperties();

    Map< ID, Map< ID, Pair< P, P > > > getPropertyChanges();

    void addPropertyIdsToIgnore(Collection<ID> ids);

    /**
     * @param ids
     *            The IDs of properties that should be left out of the property
     *            diff results. This is not used to filter the objects returned
     *            by {@link #get1()} and {@link #get2()}.
     */
    Set<ID> getPropertyIdsToIgnore();

}
