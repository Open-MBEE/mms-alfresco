package gov.nasa.jpl.mbee.util;


import gov.nasa.jpl.mbee.util.CompareUtils.MappedValueComparator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MethodCall {
    /**
     * Create a new MethodCall, fully specifying its attributes.
     * 
     * @param objectOfCall
     *            This is the Object whose method is called. If it is null
     *            and the method is not static, the indexOfObjectArgument
     *            must be 0 to indicate that the objects will be substituted
     *            such that the method is called from each of them. If the
     *            method is static, then objectOfCall is ignored.
     * @param method
     *            Java Method either of Class O or with a parameter that is
     *            or extends O (for the objects).
     * @param arguments
     *            arguments to be passed into the call of the method
     */
    public MethodCall( java.lang.Object objectOfCall, Method method,
                       java.lang.Object... arguments ) {
        this.objectOfCall = objectOfCall;
        this.method = method;
        this.arguments = arguments;
    }
    /**
     * This is the Object whose method is called. If it is null and the
     * method is not static, the indexOfObjectArgument must be 0 to indicate
     * that the objects will be substituted such that the method is called
     * from each of them. If the method is static, then objectOfCall is
     * ignored.
     */
    public Object objectOfCall;
    /**
     * Java Method either of Class O or with a parameter that is or extends
     * O (for the objects).
     */
    public Method method;
    /**
     * arguments to be passed into the call of the method
     */
    public Object[] arguments;
    
    public Pair< Boolean, Object > invoke() {
        return invoke( true );
    }
    public Pair< Boolean, Object > invoke( boolean suppressErrors ) {
        boolean objectIsMethodCall = objectOfCall instanceof MethodCall;
        Pair< Boolean, Object > result =
                ClassUtils.runMethod( suppressErrors && !objectIsMethodCall,
                                      objectOfCall, method, arguments );
        if ( result.first == false && objectIsMethodCall ) {
            MethodCall objectMethodCall = (MethodCall)objectOfCall;
            Pair< Boolean, Object > prevResult = objectMethodCall.invoke( suppressErrors );
            if ( prevResult.first ) {
                result = ClassUtils.runMethod( suppressErrors && !objectIsMethodCall,
                                               prevResult.second, method, arguments );
            }
        }
        return result;
    }
    
    /**
     * Substitute an object for a specified argument in this MethodCall.
     * 
     * @param indexOfArg
     *            the index of the argument to be replaced
     * @param obj
     *            the replacement for the argument
     */
    protected void sub( int indexOfArg, Object obj ) {
        if ( indexOfArg < 0 ) Debug.error("bad indexOfArg " + indexOfArg );
        else if ( indexOfArg == 0 ) objectOfCall = obj;
        else if ( indexOfArg > arguments.length ) Debug.error( "bad index "
                                                               + indexOfArg
                                                               + "; only "
                                                               + arguments.length
                                                               + " arguments!" );
        else arguments[indexOfArg-1] = obj;
    }
    
    /**
     * @param objects
     * @param methodCall
     * @param indexOfObjectArgument
     *            where in the list of arguments an object from the collection
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the objects are each substituted for
     *            methodCall.objectOfCall).
     * @return the subset of objects for which the method call returns true
     */
    public static < XX > Collection<XX> filter( Collection< XX > objects,
                                                MethodCall methodCall,
                                                int indexOfObjectArgument ) {
        return methodCall.filter( objects, indexOfObjectArgument );
    }
    /**
     * @param objects
     * @param indexOfObjectArgument
     *            where in the list of arguments an object from the collection
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the objects are each substituted for
     *            methodCall.objectOfCall).
     * @return the subset of objects for which the method call returns true
     */
    public < XX > Collection<XX> filter( Collection< XX > objects,
                                         int indexOfObjectArgument ) {
        Collection< XX > coll = new ArrayList< XX >();
        for ( XX o : objects ) {
            sub( indexOfObjectArgument, o );
            Pair< Boolean, Object > result = invoke();
            if ( result != null && result.first && Utils.isTrue( result.second, false ) ) {
                coll.add( o );
            }
        }
        return coll;
    }
    /**
     * @param objects
     * @param methodCall the MethodCall to invoke on each object in the Collection
     * @param indexOfObjectArgument
     *            where in the list of arguments an Object from the Collection
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the objects are each substituted for
     *            methodCall.objectOfCall).
     * @return the results of the methodCall on each of the objects
     */
    public static < XX > Collection< XX > map( Collection< ? > objects,
                                               MethodCall methodCall,
                                               int indexOfObjectArgument ) {
        return methodCall.map( objects, indexOfObjectArgument );
    }
    /**
     * @param objects
     * @param indexOfObjectArgument
     *            where in the list of arguments an object from the Collection
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the objects are each substituted for
     *            methodCall.objectOfCall).
     * @return the results of the methodCall on each of the objects
     */
    public  < XX > Collection< XX > map( Collection< ? > objects,
                                         int indexOfObjectArgument ) {
        Collection< XX > coll = new ArrayList<XX>();
        for ( Object o : objects ) {
            sub( indexOfObjectArgument, o );
            Pair< Boolean, Object > result = invoke();
            if ( result != null && result.first ) {
                coll.add( (XX)result.second );
            } else {
                coll.add( null );
            }
        }
        return coll;
    }
    
    /**
     * Inductively combine the results of applying the method to each of the
     * elements and the return results for the prior element.
     * <p>
     * For example, fold() is used below to sum an array of numbers.<br>
     * {@code int plus(int a, int b) ( return a+b; )} <br>
     * {@code MethodCall plusCall = new MethodCall( null, ClassUtils.getMethodForName(this.getClass(), "plus"), 0, 0} <br>
     * {@code int[] array = new int[] ( 2, 5, 6, 5 );}<br>
     * {@code int result = fold(Arrays.asList(array), 0, 1, 2); // result = 18}
     * 
     * @param objects collection of Objects
     * @param methodCall the MethodCall to invoke on each Object in the Collection 
     * @param initialValue
     *            an initial value to act as the initial argument to the first
     *            invocation of this MethodCall.
     * @param indexOfObjectArgument
     *            where in the list of arguments an Object from the Collection
     *            is substituted (1 to total number of args) or 0 to indicate
     *            that the elements are each substituted for
     *            methodCall.objectOfCall.
     * @param indexOfPriorResultArgument
     *            where in the list of arguments the prior result value is
     *            substituted (1 to total number of args or 0 to indicate that
     *            the prior results are each substituted for methodCall.objectOfCall).
     * @return the result of calling the method on the last Object after calling
     *         the method on each prior Object (in order), passing the prior
     *         return value into the call on each element.
     */
     public static < XX > XX fold( Collection< ? > objects, MethodCall methodCall, XX initialValue,
                             int indexOfObjectArgument, int indexOfPriorResultArgument ) {
         return methodCall.fold( objects, initialValue, indexOfObjectArgument, indexOfPriorResultArgument );
     }
     
    /**
     * Inductively combine the results of applying the method to each of the
     * elements and the return results for the prior element.
     * <p>
     * For example, fold() is used below to sum an array of numbers.<br>
     * {@code int plus(int a, int b) ( return a+b; )} <br>
     * {@code MethodCall plusCall = new MethodCall( null, ClassUtils.getMethodForName(this.getClass(), "plus"), 0, 0} <br>
     * {@code int[] array = new int[] ( 2, 5, 6, 5 );}<br>
     * {@code int result = fold(Arrays.asList(array), 0, 1, 2); // result = 18}
     * 
     * @param objects collection of Objects
     * @param initialValue
     *            an initial value to act as the initial argument to the first
     *            invocation of this MethodCall.
     * @param indexOfObjectArgument
     *            where in the list of arguments an Object from the collection
     *            is substituted (1 to total number of args) or 0 to indicate
     *            that the elements are each substituted for
     *            methodCall.objectOfCall.
     * @param indexOfPriorResultArgument
     *            where in the list of arguments the prior result value is
     *            substituted (1 to total number of args or 0 to indicate that
     *            the prior results are each substituted for methodCall.objectOfCall).
     * @return the result of calling the method on the last Object after calling
     *         the method on each prior Object (in order), passing the prior
     *         return value into the call on each element.
     */
     public  < XX > XX fold( Collection< ? > objects, XX initialValue,
                             int indexOfObjectArgument, int indexOfPriorResultArgument ) {
        XX priorResult = initialValue;
        for ( Object o : objects ) {
            sub( indexOfPriorResultArgument, priorResult );
            sub( indexOfObjectArgument, o );
            Pair< Boolean, Object > result = invoke();
            if ( result.first ) {
                priorResult = (XX)result.second;
            } 
        }
        return priorResult;
    }
    
    /**
     * Sort and return a copy of the input Collection of Objects according to
     * the results of invoking the MethodCall on each Object.
     * 
     * @param objects
     *            to be sorted
     * @param comparator
     *            specifies precedence relation on a pair of methodCall return
     *            values; null defaults to {@link CompareUtils.GenericComparator}.
     * @param methodCall
     *            a MethodCall to invoke on each Object
     * @param indexOfElementArgument
     *            where in the list of arguments an Object from the collection
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the Objects are each substituted for
     *            methodCall.objectOfCall).
     * @return the input Objects in a new Collection sorted according to the
     *         method and comparator
     */
    public static < XX > Collection< XX > sort( Collection< XX > objects,
                                                Comparator< ? > comparator,
                                                MethodCall methodCall,
                                                int indexOfElementArgument ) {
        return methodCall.sort( objects, comparator, indexOfElementArgument );
    }

    /**
     * Sort and return a copy of the input Collection of Objects according to
     * the results of invoking this MethodCall on each Object.
     * 
     * @param objects
     *            to be sorted
     * @param comparator
     *            specifies precedence relation on a pair of MethodCall return
     *            values; null defaults to {@link CompareUtils.GenericComparator}.
     * @param indexOfElementArgument
     *            where in the list of arguments an Object from the collection
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the Objects are each substituted for
     *            methodCall.objectOfCall).
     * @return the input Objects in a new Collection sorted according to the
     *         method and comparator
     */
    public < K, V > Collection< K > sort( Collection< K > objects,
                                          Comparator< V > comparator,
                                          int indexOfObjectArgument ) {
        List< K > result = new ArrayList< K >( objects );
        Map< K, V > map = new HashMap< K, V >();
        for ( K o : objects ) {
            sub( indexOfObjectArgument, o );
            Pair< Boolean, Object > r = invoke();
            map.put( o, (V)r.second );
        }
        MappedValueComparator< K, V > mapComparator =
                new CompareUtils.MappedValueComparator< K, V >( map, comparator );
        Collections.sort( result, mapComparator );
        return result;
    }
         
    /**
     * Compute a transitive closure of a set using this MethodCall as a relation from an argument to the return value.
     * @param initialSet the Set of initial items to be substituted for an argument or the object of this MethodCall
     * @param indexOfObjectArgument
     *            where in the list of arguments an object from the set
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the objects are each substituted for
     *            methodCall.objectOfCall).
     * @param maximumSetSize the size of the resulting set will be limited to the maximum of this argument and the size of initialSet 
     * @return a new Set that includes the initialSet and the results of applying the methodCall on each item (substituting the argument for the given index) in the new Set  
     */
    public < XX > Set< XX > closure( Set< XX > initialSet,
                                     int indexOfObjectArgument, int maximumSetSize ) {
        Set< XX > closedSet = new TreeSet< XX >( CompareUtils.GenericComparator.instance() );
        closedSet.addAll( initialSet );
        ArrayList< XX > queue =
                new ArrayList< XX >( initialSet );
        Set< XX > seen = new HashSet< XX >();
        while ( !queue.isEmpty() ) {
            XX item = queue.get( 0 );
            queue.remove( 0 );
            sub( indexOfObjectArgument, item );
            if ( seen.contains( item ) ) continue;
            seen.add( item );
            Pair< Boolean, Object > result = invoke( true );
            if ( !result.first ) continue;
            Collection< XX > newItems = null;
            try {
                if ( result.second instanceof Collection ) {
                    newItems = (Collection< XX >)result.second;
                } else {
                    newItems = (Collection< XX >)Utils.newSet( result.second );
                }
            } catch ( ClassCastException e ) {
                continue;
            }
            if ( !Utils.isNullOrEmpty( newItems ) ) {
                Utils.addN( closedSet, maximumSetSize - closedSet.size(), newItems );
            }
        }
        return closedSet;
    }
    
    /**
     * Compute a transitive closure of a map using this MethodCall to specify for each key in the map a set of items that should have a superset of related items in the map.
     * @param initialSet the Set of initial items to be substituted for an argument or the object of this MethodCall
     * @param indexOfObjectArgument
     *            where in the list of arguments an object from the set
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the objects are each substituted for
     *            methodCall.objectOfCall).
     * @param maximumSetSize the size of the resulting set will be limited to the maximum of this argument and the size of initialSet 
     * @return a new Set that includes the initialSet and the results of applying the methodCall on each item (substituting the argument for the given index) in the new Set  
     */
    public < XX, C extends Map< XX, Set< XX > > > C mapClosure( C relationMapToClose, int indexOfObjectArgument, int maximumSetSize ) {
        ArrayList< XX > queue =
                new ArrayList< XX >( relationMapToClose.keySet() );
//        Set< XX > seen = new HashSet< XX >();
        while ( !queue.isEmpty() ) {
            XX item = queue.get( 0 );
            queue.remove( 0 );
            sub( indexOfObjectArgument, item );
//            if ( seen.contains( item ) ) continue;
//            seen.add( item );
//            Method method =
//                    ClassUtils.getMethodForArgs( AbstractSystemModel.class, "isA",
//                                                 item, item );
//            MethodCall methodCall =
//                    new MethodCall( null, method,
//                                    new Object[] { null, item } );
            Pair< Boolean, Object > result = invoke( true );
            if ( !result.first ) continue;
            Collection< XX > isItemSet = null;
            try {
                if ( result.second instanceof Collection ) {
                    isItemSet = (Collection< XX >)result.second;
                } else {
                    isItemSet = (Collection< XX >)Utils.newSet( result.second );
                }
            } catch ( ClassCastException e ) {
                continue;
            }
            Set< XX > relatedToItem = relationMapToClose.get( item );
            for ( XX isA : isItemSet ) {
                Set< XX > related = relationMapToClose.get( isA );
                int ct = 0;
                if ( related == null ) {
                    related = new TreeSet< XX >(CompareUtils.GenericComparator.instance());
                    relationMapToClose.put( isA, related );
                } else {
                    ct = related.size();
                }
                related.addAll( relatedToItem );
                if ( related.size() > ct ) {
                    queue.add( isA );
                }
                if ( relationMapToClose.size() >= maximumSetSize ) break;
            }
        }
        return relationMapToClose;
    }
    
    static void main( String args[] ) {
        // TODO -- put tests here or in JUnit tests
    }

}