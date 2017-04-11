/**
 *
 */
package gov.nasa.jpl.sysml;

import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.CompareUtils;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.MethodCall;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * An abstract SystemModel that provides some straightforward implementations of
 * the more abstract methods, such as op(), get(), set(), map(), and filter(),
 * based on more specific methods, like {@link getName(E, V)}, that overlap in
 * functionality.
 */
public abstract class AbstractSystemModel< E, C, T, P, N, I, U, R, V, W, CT >
                      implements SystemModel< E, C, T, P, N, I, U, R, V, W, CT > {

    static String getGenericSymbol( ModelItem itemType ) {
        switch ( itemType ) {
            //case CONTEXT:
            case IDENTIFIER:
            case NAME:
            case ELEMENT:
            case PROPERTY:
            case RELATIONSHIP:
            case TYPE:
            case VERSION:
            case WORKSPACE:
                return itemType.toString().substring( 0, 1 );
            case CONSTRAINT:
                return "CT";
            case VALUE:
                return "U";
            case VIEW:
                return "E";
            case VIEWPOINT:
                return "E";
            default:
                Debug.error( "Unexpected ModelItem: " + itemType );
        }
        return null;
    }

    /* (non-Javadoc)
     * @see SystemModel#op(SystemModel.Operation, java.util.Collection, java.util.Collection, java.util.Collection, java.lang.Object, java.lang.Boolean)
     */
    @Override
    public Collection< Object > op( SystemModel.Operation operation,
                                    Collection< SystemModel.ModelItem > itemTypes,
                                    Collection< SystemModel.Item > contexts,
                                    Collection< SystemModel.Item > specifiers,
                                    U newValue,
                                    Boolean failForMultipleItemMatches ) {
        if ( operation == null ) {
            Debug.error( "Operation must not be null!" );
            return Collections.emptyList();
        }
        SystemModel.Item newValueItem = new Item( newValue, ModelItem.VALUE );
        if ( !isAllowed( operation, itemTypes, contexts, specifiers, newValueItem, failForMultipleItemMatches ) ) {
            return Collections.emptyList();
        }
        Collection< Object > results = Utils.getEmptyList();
        Collection< Object > res = null;
        if ( Utils.isNullOrEmpty( contexts ) ) {
//            SystemModel.Item[] oneNullArg = new SystemModel.Item[1];
//            oneNullArg[0] = null;
            contexts = Utils.newListWithOneNull();
        }
        if ( Utils.isNullOrEmpty( itemTypes ) ) {
            itemTypes = Utils.newListWithOneNull();
        }
        if ( Utils.isNullOrEmpty( specifiers ) ) {
            specifiers = Utils.newListWithOneNull();
        }
        boolean someResultsWereNull = false;
        boolean allResultsWereNull = true;
        for ( SystemModel.Item context : contexts ) {
            //SystemModel.ModelItem contextType = context == null ? null : context.kind;
            for ( SystemModel.Item specifier : specifiers ) {
                //SystemModel.ModelItem specifierType = specifier == null ? null : specifier.kind;
                for ( SystemModel.ModelItem itemType : itemTypes ) {
                    try {
                        MethodCall mc =
                                getMethodCall( this, operation, itemType,
                                               context, specifier, newValue, false );
                        Pair< Boolean, Object > p = mc.invoke( true );
                        if ( p.first ) {
                            res = null;
                            if ( p.second instanceof Collection ) {
                                res = (Collection< Object >)p.second;
                            }
                            if ( res == null ) {
                                someResultsWereNull = true;
                                // TODO -- should return null here?
                                // if the method call invoked properly, but
                                // returned null, then the itemType, context,
                                // and specifier are incompatible.
                                // return null;
                            } else {
                                allResultsWereNull = false;
                                results.addAll( res );
                            }
                        }
                        if ( Utils.isTrue( failForMultipleItemMatches )
                             && results.size() > 1 ) return null;
                    } catch (Throwable e ) {
                        // ignore!
                    }
                }
            }
        }
        if ( allResultsWereNull && someResultsWereNull ) return null;
        return results;
//        switch( operation ) {
//            case CREATE:
//                return create(itemTypes, context, specifier, newValue, failForMultipleItemMatches );
//            case DELETE:
//                return delete(itemTypes, context, specifier, failForMultipleItemMatches );
//            case GET:
//                return get(itemTypes, context, specifier, failForMultipleItemMatches );
//            case READ:
//                return get(itemTypes, context, specifier, failForMultipleItemMatches );
//            case SET:
//                return set(itemTypes, context, specifier, newValue, failForMultipleItemMatches );
//            case UPDATE:
//                return set(itemTypes, context, specifier, newValue, failForMultipleItemMatches );
//            default:
//                Debug.error( "Unexpected SystemModel.Operation: " + operation );
//        }
//        return null;
    }

    protected static boolean
            anyAllowed( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                        SystemModel.Operation operation,
                        SystemModel.ModelItem itemType,
                        Collection< SystemModel.Item > contexts,
                        Collection< SystemModel.Item > specifiers,
                        SystemModel.ModelItem newValueType,
                        Boolean failForMultipleItemMatches ) {
        return model.anyAllowed( operation, itemType, contexts, specifiers,
                                 newValueType, failForMultipleItemMatches );

    }

    protected boolean anyAllowed( SystemModel.Operation operation,
                                  SystemModel.ModelItem itemType,
                                  Collection< SystemModel.Item > contexts,
                                  Collection< SystemModel.Item > specifiers,
                                  SystemModel.ModelItem newValueType,
                                  Boolean failForMultipleItemMatches ) {
        if ( Utils.isNullOrEmpty( contexts ) ) {
            return anyAllowed( operation, itemType,
                               (SystemModel.ModelItem)null, specifiers,
                               newValueType, failForMultipleItemMatches );
        } else
        for ( SystemModel.Item context : contexts ) {
            ModelItem contextType = context.kind;
            if ( anyAllowed( operation, itemType, contextType, specifiers,
                             newValueType, failForMultipleItemMatches ) ) {
                return true;
            }
        }
        return false;
    }

    protected static boolean
            anyAllowed( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                        SystemModel.Operation operation,
                        SystemModel.ModelItem itemType,
                        SystemModel.ModelItem contextType,
                        Collection< SystemModel.Item > specifiers,
                        SystemModel.ModelItem newValueType,
                        Boolean failForMultipleItemMatches ) {
        return model.anyAllowed( operation, itemType, contextType, specifiers,
                                 newValueType, failForMultipleItemMatches );
    }

    protected boolean anyAllowed( SystemModel.Operation operation,
                                  SystemModel.ModelItem itemType,
                                  SystemModel.ModelItem contextType,
                                  Collection< SystemModel.Item > specifiers,
                                  SystemModel.ModelItem newValueType,
                                  Boolean failForMultipleItemMatches ) {
        if ( Utils.isNullOrEmpty( specifiers ) ) {
            return isAllowed( operation, itemType, contextType,
                               (SystemModel.ModelItem)null, newValueType,
                               failForMultipleItemMatches );
        } else
        for ( SystemModel.Item specifier : specifiers ) {
            ModelItem specifierType = specifier.kind;
            if ( isAllowed( operation, itemType, contextType, specifierType,
                            newValueType, failForMultipleItemMatches ) ) {
                return true;
            }
        }
        return false;
    }

    protected static boolean
            anyAllowed( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                        SystemModel.Operation operation,
                        Collection< SystemModel.ModelItem > itemTypes,
                        Collection< SystemModel.Item > contexts,
                        Collection< SystemModel.Item > specifiers,
                        SystemModel.Item newValue,
                        Boolean failForMultipleItemMatches ) {
        return model.anyAllowed( operation, itemTypes, contexts, specifiers,
                                 newValue, failForMultipleItemMatches );
    }

    protected boolean anyAllowed( SystemModel.Operation operation,
                                  Collection< SystemModel.ModelItem > itemTypes,
                                  Collection< SystemModel.Item > contexts,
                                  Collection< SystemModel.Item > specifiers,
                                  SystemModel.Item newValue,
                                  Boolean failForMultipleItemMatches ) {
        boolean allowed = false;
        SystemModel.ModelItem newValueType = newValue == null ? null : newValue.kind;
        //for ( boolean nullItem : new boolean[] { false, true } )

        if ( Utils.isNullOrEmpty( itemTypes ) ) {
            return anyAllowed( operation, (SystemModel.ModelItem)null,
                               contexts, specifiers, newValueType,
                               failForMultipleItemMatches );
        } else for ( SystemModel.ModelItem itemType : itemTypes ) {
            if ( anyAllowed( operation, itemType, contexts, specifiers,
                             newValueType, failForMultipleItemMatches ) ) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see SystemModel#isAllowed(SystemModel.Operation, java.util.Collection, java.util.Collection, java.util.Collection, SystemModel.Item, java.lang.Boolean)
     */
    @Override
    public boolean
            isAllowed( SystemModel.Operation operation,
                       Collection< SystemModel.ModelItem > itemTypes,
                       Collection< SystemModel.Item > contexts,
                       Collection< SystemModel.Item > specifiers,
                       SystemModel.Item newValue,
                       Boolean failForMultipleItemMatches ) {
        // put most general tests here and delegate others to operation-specific functions

        // we must know what kind of thing is being operated on
        if ( Utils.isNullOrEmpty( itemTypes ) ) return false;
        if ( anyAllowed( operation, itemTypes, contexts, specifiers, newValue, failForMultipleItemMatches ) ) {
            return true;
        }
        switch ( operation ) {
            case CREATE:
                return mayCreate( itemTypes, contexts, specifiers, newValue,
                                  failForMultipleItemMatches );
            case DELETE:
                return mayDelete( itemTypes, contexts, specifiers,
                                  failForMultipleItemMatches );
            case GET:
            case READ:
                return mayGet( itemTypes, contexts, specifiers,
                               failForMultipleItemMatches );
            case SET:
            case UPDATE:
                return maySet( itemTypes, contexts, specifiers, newValue,
                               failForMultipleItemMatches );
            default:
                Debug.error( "Unexpected SystemModel.Operation: " + operation );
        }
        return false;
    }

    protected static Set< ModelItem > itemTypeNotEqualContextTypeSet =
            Utils.newSet( //ModelItem.CONTEXT,
                          ModelItem.IDENTIFIER,
                          ModelItem.NAME,
                          ModelItem.TYPE,
                          ModelItem.VALUE,
                          ModelItem.VERSION,
                          ModelItem.WORKSPACE );

    protected static < KK, VV > Pair< KK, VV > pair( KK k, VV v ) {
        return new Pair< KK, VV >( k, v );
    }

    protected static Set<ModelItem> itemSet(ModelItem...items) {
        return Utils.newSet(items);
    }

    @SuppressWarnings( "unchecked" )
    protected static Map< ModelItem, Set< ModelItem > > isA =
            Utils.newMap( pair( ModelItem.CONSTRAINT,
                                itemSet( ModelItem.ELEMENT ) ), //, ModelItem.PROPERTY,
                                         //ModelItem.RELATIONSHIP ) ), // ???
//                          pair( ModelItem.CONTEXT, itemSet( ModelItem.OBJECT ) ),
                          pair( ModelItem.IDENTIFIER, itemSet() ),
                          pair( ModelItem.NAME, itemSet() ),
                          pair( ModelItem.ELEMENT, itemSet() ),
                          pair( ModelItem.PROPERTY, itemSet( ModelItem.ELEMENT ) ),
                          pair( ModelItem.RELATIONSHIP,
                                itemSet( ModelItem.ELEMENT ) ),
                          pair( ModelItem.TYPE, itemSet( ModelItem.ELEMENT ) ),
                          pair( ModelItem.VALUE, itemSet( ModelItem.ELEMENT ) ),
                          pair( ModelItem.VERSION, itemSet() ),
                          pair( ModelItem.VIEW, itemSet( ModelItem.ELEMENT ) ),
                          pair( ModelItem.VIEWPOINT, itemSet( ModelItem.ELEMENT ) ),
                          pair( ModelItem.WORKSPACE, itemSet() ) );

    public static boolean isA( ModelItem kind1, ModelItem kind2 ) {
        Set< ModelItem > list = isA.get( kind1 );
        return ( list != null && list.contains( kind2 ) );
    }

//    protected static Map< ModelItem, Set< ModelItem > > isSometimes =
//            Utils.newMap( pair( ModelItem.CONSTRAINT,
//                                itemSet( ModelItem.PROPERTY ) ) );
//
//    public static boolean isSometimes( ModelItem kind1, ModelItem kind2 ) {
//        Set< ModelItem > list = isSometimes.get( kind1 );
//        return ( list != null && list.contains( kind2 ) );
//    }

    @SuppressWarnings( "unchecked" )
    protected static Map< ModelItem, Set< ModelItem > > canContain =
            Utils.newMap( pair( ModelItem.ELEMENT, //itemSet( ModelItem.OBJECT ) ),
                                Utils.minus(itemSet( ModelItem.values() ), itemSet( //ModelItem.CONTEXT,
                                                                                                    ModelItem.RELATIONSHIP, ModelItem.VALUE, ModelItem.TYPE, ModelItem.VERSION, ModelItem.WORKSPACE  ) ) ),
                          pair( ModelItem.WORKSPACE, itemSet( ModelItem.ELEMENT ) ) );
    @SuppressWarnings( "unchecked" )
    protected static Map< ModelItem, Set< ModelItem > > canHave =
            Utils.newMap(
//                          pair( ModelItem.CONSTRAINT,
//                                Utils.newSet( ModelItem.CONSTRAINT,
//                                              ModelItem.IDENTIFIER,
//                                              ModelItem.NAME,
//                                              ModelItem.OBJECT,
//                                              ModelItem.PROPERTY,
//                                              ModelItem.RELATIONSHIP ) ),
//                          pair( ModelItem.CONTEXT,
//                                Utils.newSet( ModelItem.values() ) ),
//                          pair( ModelItem.IDENTIFIER, Utils.getEmptySetOfType(ModelItem.class) ),
//                          pair( ModelItem.NAME, Utils.getEmptySetOfType(ModelItem.class) ),
                          pair( ModelItem.ELEMENT,
                                Utils.minus(itemSet( ModelItem.values() ), itemSet( ModelItem.ELEMENT) ) )
//                          pair( ModelItem.PROPERTY, Utils.getEmptySetOfType(ModelItem.class) ),
//                          pair( ModelItem.RELATIONSHIP, Utils.getEmptySetOfType(ModelItem.class) )
                                      );

    public static boolean canHave( ModelItem kind1, ModelItem kind2 ) {
        Set< ModelItem > list = canHaveClosure.get( kind1 );
        return ( list != null && list.contains( kind2 ) );
    }

    /**
     * @param item
     * @return the set of ModelItems that each have item in their isA set
     */
    public static Collection< ModelItem > whatIsA( ModelItem item ) {
        Method method =
                ClassUtils.getMethodForArgs( AbstractSystemModel.class, "isA",
                                             item, item );
        MethodCall methodCall =
                new MethodCall( null, method,
                                new Object[] { null, item } );
        Collection< ModelItem > isItemSet =
                MethodCall.filter( Arrays.asList( ModelItem.values() ),
                                   methodCall, 1 );
        if ( Debug.isOn() ) Debug.outln( "whatIsA(" + item + ") = " + isItemSet );
        return isItemSet;
    }

    protected static Map< ModelItem, Set< ModelItem > > canHaveClosure =
            new TreeMap< ModelItem, Set< ModelItem > >( canHave ) {
                private static final long serialVersionUID = 1L;
                {
                    if ( !keySet().isEmpty() ) {
                        ModelItem item = keySet().iterator().next();
                        Method method =
                                ClassUtils.getMethodForArgs( AbstractSystemModel.class, "whatIsA",
                                                             item );
                        MethodCall methodCall =
                                new MethodCall( null, method,
                                                new Object[] { null } );
                        methodCall.mapClosure(this, 1, ModelItem.values().length+1);
                        if ( Debug.isOn() ) Debug.out( "canHaveClosure = " + this );
                    }
                }
            };

    protected static Map< ModelItem, Set< ModelItem > > canContainClosure =
            new TreeMap< ModelItem, Set< ModelItem > >( canContain ) {
                private static final long serialVersionUID = 1L;
                {
                    if ( !keySet().isEmpty() ) {
                        ModelItem item = keySet().iterator().next();
                        Method method =
                                ClassUtils.getMethodForArgs( AbstractSystemModel.class, "whatIsA",
                                                             item );
                        MethodCall methodCall =
                                new MethodCall( null, method,
                                                new Object[] { null } );
                        methodCall.mapClosure(this, 1, ModelItem.values().length+1);
                        if ( Debug.isOn() ) Debug.outln( "canContainClosure = " + this );
                    }
                }
            };

            public static boolean canContain( ModelItem kind1, ModelItem kind2 ) {
                Set< ModelItem > list = canContainClosure.get( kind1 );
                return ( list != null && list.contains( kind2 ) );
            }

    public static boolean isNecessaryInAPI( //AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                       SystemModel.Operation operation,
                       SystemModel.ModelItem itemType,
                       SystemModel.ModelItem contextType,
                       SystemModel.ModelItem specifierType,
                       SystemModel.ModelItem newValueType,
                       Boolean failForMultipleItemMatches ) {
        if ( !isAllowed( null, operation, itemType, contextType, specifierType,
                         newValueType, failForMultipleItemMatches ) ) return false;
        if ( contextType != null ) return false;
        switch ( operation ) {
            case GET:
            case READ:
                if ( newValueType != null ) return false;
                break;
            case CREATE:
            case DELETE:
            case SET:
            case UPDATE:
                if ( specifierType != null ) return false;
                break;
            default:
                Debug.error( "Unexpected Operation! " + operation );
        }
        return true;
    }
    public static boolean
            isAllowed( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                       SystemModel.Operation operation,
                       SystemModel.ModelItem itemType,
                       SystemModel.ModelItem contextType,
                       SystemModel.ModelItem specifierType,
                       SystemModel.ModelItem newValueType,
                       Boolean failForMultipleItemMatches ) {
        // operation independent criteria

        // we must know what kind of thing is being operated on
        if ( itemType == null ) return false;

                // incompatible itemType and contextType when equal;
        // ex. getNameOfName() doesn't make sense, so it shouldn't be allowed.
        if ( itemType == contextType ) {
            if ( itemTypeNotEqualContextTypeSet.contains( itemType ) ) {
                return false;
            }
        }

        // incompatible itemType and contextType

        // TODO -- what other operation independent rules can we assume?
        if ( contextType != null && !canHave( contextType, itemType ) ) return false;
        if ( specifierType != null && !canHave( itemType, specifierType ) ) return false;

        switch ( operation ) {
            case CREATE:
                return mayCreate( null, itemType, contextType, specifierType,
                                  newValueType, failForMultipleItemMatches );
            case DELETE:
                return mayDelete( null, itemType, contextType, specifierType,
                                  failForMultipleItemMatches );
            case GET:
            case READ:
                return mayGet( null, itemType, contextType, specifierType,
                               failForMultipleItemMatches );
            case SET:
            case UPDATE:
                return maySet( null, itemType, contextType, specifierType,
                               newValueType, failForMultipleItemMatches );
            default:
                Debug.error( "Unexpected SystemModel.Operation: " + operation );
        }
        return false;
//        return model.isAllowed( operation, itemType, contextType,
//                                specifierType, newValueType,
//                                failForMultipleItemMatches );
    }

    public boolean isAllowed( SystemModel.Operation operation,
                              SystemModel.ModelItem itemType,
                              SystemModel.ModelItem contextType,
                              SystemModel.ModelItem specifierType,
                              SystemModel.ModelItem newValueType,
                              Boolean failForMultipleItemMatches ) {
        return isAllowed( this, operation, itemType, contextType,
                specifierType, newValueType,
                failForMultipleItemMatches );
    }


    /**
     * @param operation
     * @return a lower case name for the operation using "get" for READ and "set" for UPDATE.
     */
    public static String getOperationName(SystemModel.Operation operation ) {
        switch( operation ) {
            case CREATE:
            case DELETE:
            case GET:
            case SET:
                return operation.toString().toLowerCase();
            case READ:
                return getOperationName( Operation.GET );
            case UPDATE:
                return getOperationName( Operation.SET );
            default:
                Debug.error( "Unexpected SystemModel.Operation: " + operation );
        }
        return null;
    }

    private boolean equals( Item context, ModelItem itemType, Item specifier,
                            Object value, boolean complain ) {
        MethodCall call =
                getMethodCall( this, Operation.GET, itemType, context, specifier,
                               null, false, complain );
        if ( call == null ) return false;
        Pair< Boolean, Object > p = call.invoke( !complain );
        if ( p.first != true ) return false;
        return Utils.valuesEqual( p.second, value );
    }

    /* (non-Javadoc)
     * @see SystemModel#op(SystemModel.Operation, java.util.Collection, java.util.Collection, java.lang.Object, java.lang.Object, java.lang.Object, boolean)
     */
    @Override
    public Collection< Object >
            op( SystemModel.Operation operation,
                Collection< ModelItem > itemTypes,
                Collection< Item > context, I identifier, N name, V version,
                U newValue,
                boolean failForMultipleItemMatches ) {
        Collection< Item > specifier = new ArrayList< Item >();
        Item item = null;
        Collection<Object> result = null;
        if ( identifier != null ) {
            item = new Item( identifier, ModelItem.IDENTIFIER );
            specifier.clear();
            specifier.add( item );
            result = op( operation, itemTypes, context, specifier, newValue, failForMultipleItemMatches );
            if ( Utils.isNullOrEmpty( result ) ) {
                return result;
            }
        }
        if ( version != null ) {
            item = new Item( version, ModelItem.VERSION );
            specifier.clear();
            specifier.add( item );
            Collection< Object > resultV = op( operation, itemTypes, context, specifier, newValue, failForMultipleItemMatches );
            Utils.intersect( result, resultV );
        }
        if ( name != null ) {
            item = new Item( name, ModelItem.NAME );
            specifier.clear();
            specifier.add( item );
            Collection< Object > resultV = op( operation, itemTypes, context, specifier, newValue, failForMultipleItemMatches );
            Utils.intersect( result, resultV );
        }
        return result == null ? Collections.emptyList() : result;
    }

    /**
     * @param operation
     * @param itemType
     * @param contextType
     * @param specifierType
     * @param complain
     * @return the specific Method in this class for performing the operation with the given arguments.
     */
    public static String getMethodName( SystemModel.Operation operation,
                                        SystemModel.ModelItem itemType,
                                        SystemModel.Item contextType,
                                        SystemModel.Item specifierType,
                                        Object newValue,
                                        boolean complain ) {
        MethodCall mc = getMethodCall( null, operation, itemType, contextType, specifierType, newValue, true, complain );
        Pair< Boolean, Object > result = mc.invoke( complain );
        return "" + result.second;
    }
    /**
     * @param operation
     * @param itemType
     * @param contextType
     * @param specifierType
     * @param complain
     * @return the specific Method in this class for performing the operation with the given arguments.
     */
    public static String getMethodName( SystemModel.Operation operation,
                                        SystemModel.ModelItem itemType,
                                        SystemModel.ModelItem contextType,
                                        SystemModel.ModelItem specifierType,
                                        boolean complain ) {
        if ( operation == null ) {
            Debug.error( complain, complain, "Trying to pass in null operation!" );
            return null;
        }
        String opName = getOperationName( operation );
        String contextTypeName;

        String itemTypeName;
        if ( itemType == null ) {
            itemTypeName = "";
        } else {
            itemTypeName = Utils.toCamelCase( itemType.toString() );
        }

        String specifierTypeName;
        if ( specifierType == null ) {
            if ( itemType == null ) {
                Debug.error( complain, complain, "Either itemType or specifierType may be null, but not both!" );
                return null;
            }
            specifierTypeName = "";
        } else {
            specifierTypeName = Utils.toCamelCase( specifierType.toString() );
            if ( itemType == null ) {
                itemTypeName = specifierTypeName;
            }
        }

        if ( contextType == null ) {
            contextTypeName = "";
        } else {
            contextTypeName = Utils.toCamelCase( contextType.toString() );
        }

        // construct method call name
        String by = "";
        if ( itemType != null && specifierType != null
             && specifierType != ModelItem.NAME && specifierType != itemType ) {
            by = getMethodNamePrepositionForSpecifier( operation,
                                                       specifierType,
                                                       contextType )
                 + specifierTypeName;
        }
        String of = "";
        if ( contextType != null ) {
            of = getMethodNamePrepositionForContext( operation, itemType,
                                                     contextType ) +
                 contextTypeName;
        }
        String callName = opName + itemTypeName + of + by;
        return callName;
    }

    public static String getMethodNamePrepositionForContext( Operation operation,
                                                             SystemModel.ModelItem targetType,
                                                             SystemModel.ModelItem contextType ) {
        switch ( operation ) {
            case CREATE:
            case DELETE:
            case GET:
            case READ:
            case SET:
            case UPDATE:
                break;
            default:
                Debug.error("Unrecognized Operation: " + operation );
        }
        //return "In";
        return "Of";
    }

    public static String getMethodNamePrepositionForSpecifier( Operation operation,
                                                               SystemModel.ModelItem specifierType,
                                                               SystemModel.ModelItem contextType ) {
        switch ( operation ) {
            case CREATE:
            case DELETE:
            case GET:
            case READ:
            case SET:
            case UPDATE:
                break;
            default:
                Debug.error("Unrecognized Operation: " + operation );
        }
        return "With";
    }

    public static <VAL> String getMethodNamePrepositionForNewValue( Operation operation,
                                                                    SystemModel.ModelItem specifierType,
                                                                    SystemModel.ModelItem contextType,
                                                                    VAL newValue ) {
       switch ( operation ) {
           case SET:
           case UPDATE:
               return "To";
           case CREATE:
               return "As";
           case DELETE:
           case GET:
           case READ:
               Debug.error( "No new value parameter for " + operation + " operation!" );
               break;
           default:
               Debug.error("Unrecognized Operation: " + operation );
       }
       return "";
   }

    /**
     * @param systemModel
     * @param operation
     * @param itemType
     * @param context
     * @param specifier
     * @param newValue
     * @param complain
     * @return the MethodCall for the method in systemModel that performs the
     *         given operation for a target with the given itemType of the given
     *         context item as specified by the given specifier.
     */
    public static <VAL> MethodCall getMethodCall( AbstractSystemModel< ?, ?, ?, ?, ?, ?, VAL, ?, ?, ?, ? > systemModel,
                                                  SystemModel.Operation operation,
                                                  SystemModel.ModelItem itemType,
                                                  SystemModel.Item context,
                                                  SystemModel.Item specifier,
                                                  VAL newValue,
                                                  boolean complain ) {
        return getMethodCall( systemModel, operation, itemType, context, specifier, newValue, false, complain );

    }
    /**
     * @param systemModel
     * @param operation
     * @param itemType
     * @param context
     * @param specifier
     * @param newValue
     * @param justStringRepresentation if true, return a MethodCall that just returns a string representation of the call whether or not it exists.
     * @param complain
     * @return the MethodCall for the method in systemModel that performs the
     *         given operation for a target with the given itemType of the given
     *         context item as specified by the given specifier.
     */
    public static <VAL> MethodCall getMethodCall( AbstractSystemModel< ?, ?, ?, ?, ?, ?, VAL, ?, ?, ?, ? > systemModel,
                                                  SystemModel.Operation operation,
                                                  SystemModel.ModelItem itemType,
                                                  SystemModel.Item context,
                                                  SystemModel.Item specifier,
                                                  VAL newValue,
                                                  boolean justStringRepresentation,
                                                  boolean complain ) {

        if ( operation == null ) {
            Debug.error( complain, complain, "Trying to pass in null operation!" );
            return null;
        }

        ArrayList< Class< ? > > argTypeList = new ArrayList< Class< ? > >();
        ArrayList< Object > argList = new ArrayList< Object >();
        ArrayList< String > argTypeStrings = new ArrayList< String >();

        String opName = getOperationName( operation );

        // get name and args for itemType
        String returnTypeString = "Object";
        String itemTypeName;
        if ( itemType == null ) {
            itemTypeName = "";
        } else {
            itemTypeName = Utils.toCamelCase( itemType.toString() );
            //argTypeList.add( getClass( itemType ) );
            returnTypeString = getGenericSymbol( itemType );
        }

        // get name and args for context
        SystemModel.ModelItem contextType = context == null ? null : context.kind;
        String contextTypeName = "";
        String contextPrep = "";
        String of = "";
        if ( contextType != null ) {
            // No need to include context type in call name unless there is no
            // specifier and no new value
            if ( specifier == null && ( newValue == null || !usesNewValue( operation ) ) ) {
                contextTypeName = Utils.toCamelCase( contextType.toString() );
//            }
//            if ( !Utils.isNullOrEmpty( contextTypeName ) ) {
                contextPrep = getMethodNamePrepositionForContext( operation, itemType, contextType );
                of = contextPrep + contextTypeName;
            }
            String paramType = Utils.isNullOrEmpty( of ) ? "Object" : getGenericSymbol( contextType );
            if ( systemModel != null ) argTypeList.add( paramType.equals( "Object" ) ? Object.class : systemModel.getClass( contextType ) );
            argList.add( context.obj );
            argTypeStrings.add( paramType + " context" );
        }

        // get name and args for specifier (and for the item as the specifier if itemType == null)
        SystemModel.ModelItem specifierType = specifier == null ? null : specifier.kind;
        String specifierTypeName = "";
        String specifierPrep = "";
        String by = "";
        if ( itemType != null && specifierType != null
             && specifierType != ModelItem.NAME && specifierType != itemType ) {
        }
        if ( specifierType == null ) {
            if ( itemType == null ) {
                Debug.error( complain, complain, "Either itemType or specifierType may be null, but not both!" );
            }
        } else {
            // No need for specifier in call name if there are parameters also
            // for the context and new value.
            if ( context == null || newValue == null || !usesNewValue( operation ) ) {
                specifierTypeName = Utils.toCamelCase( specifierType.toString() );
            }
            if ( itemType == null ) {
                itemTypeName = specifierTypeName;
            }
            if ( !Utils.isNullOrEmpty( specifierTypeName ) ) {
                specifierPrep = getMethodNamePrepositionForSpecifier( operation, specifierType, contextType );
            }
            if ( itemType != null && specifierType != null
                    && specifierType != ModelItem.NAME && specifierType != itemType ) {
                by = specifierPrep + specifierTypeName;
            }
            String paramType = Utils.isNullOrEmpty( by ) ? "Object" : getGenericSymbol( specifierType );
            if ( systemModel != null ) argTypeList.add( paramType.equals( "Object" ) ? Object.class : systemModel.getClass( specifierType ) );
            argList.add( specifier.obj );
            argTypeStrings.add( paramType + " specifier" );
        }

        // get args for new value
        if ( usesNewValue( operation ) ) {
            argList.add( newValue );
            argTypeStrings.add( getGenericSymbol( ModelItem.VALUE ) + " newValue" );
            if ( newValue != null ) {
                @SuppressWarnings( "unchecked" ) // the newValue parameter's type is U, so this is safe
                Class< ? extends VAL > nvClass = (Class< ? extends VAL >)newValue.getClass();
                if ( systemModel == null) {
                    argTypeList.add( nvClass );
                } else {
                    Class< ? > itemClass = systemModel.getClass( itemType );
                    Collection< Class<?> > classes =  Utils.newList( nvClass, itemClass );
                    Class<?> cls = ClassUtils.leastUpperBoundSuperclass( classes );
                    argTypeList.add( cls );
                }
            }
        }

        // construct method call name
        String newValuePrep = "";
        if ( usesNewValue( operation )
             && ( contextType == null || specifierType == null ) ) {
            newValuePrep =
                    getMethodNamePrepositionForNewValue( operation,
                                                         specifierType,
                                                         contextType, newValue );
        }

        String callName = opName + itemTypeName + of + by + newValuePrep;

        // if just printing a hypothetical method signature to a string, create a MethodCall that does it
        Method method = null;
        if ( justStringRepresentation ) {
            method = ClassUtils.getMethodForArgTypes( String.class, "concat", String.class );
            return new MethodCall( returnTypeString + " " + callName, method, Utils.toString( argTypeStrings, false ) );
        }

        // put the argument types into an array
        Class< ? >[] argTypes = new Class<?>[ argTypeList.size() ];
        argTypeList.toArray( argTypes );

        // try to lookup the Method from the callName and argTypes using reflection
        Class<?> sysClass = systemModel == null ? AbstractSystemModel.class : systemModel.getClass();
        method = ClassUtils.getMethodForArgTypes( sysClass, callName , argTypes , complain );
        if ( method == null ) {
            method = ClassUtils.getMethodForArgs( sysClass, callName, argList );
        }
        if ( method == null && by.length() > 0 ) {
            //callName = opName + itemTypeName + of;
            method = ClassUtils.getMethodForArgTypes( sysClass, callName, argTypes , complain );
            if ( method == null ) {
                method = ClassUtils.getMethodForArgs( sysClass, callName, argList );
            }
        }
        // fail and return null if the Method could not be found
        if ( method == null ) return null;

        // put the arguments into an array
        Object[] arguments = new Object[ argList.size() ];
        argList.toArray( arguments );

        // construct the method call
        return new MethodCall( systemModel, method, arguments );
    }


    public static boolean usesNewValue( SystemModel.Operation operation ) {
        return Utils.newList( //Operation.CREATE,
                             Operation.SET, Operation.UPDATE ).contains( operation );
    }

    public boolean maySet( Collection< SystemModel.ModelItem > itemTypes,
                           Collection< SystemModel.Item > context,
                           Collection< SystemModel.Item > specifier,
                           SystemModel.Item newValue,
                           Boolean failForMultipleItemMatches ) {
        return maySet( this, itemTypes, context, specifier, newValue,
                       failForMultipleItemMatches );
    }
    public static boolean maySet( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                                  Collection< SystemModel.ModelItem > itemTypes,
                                  Collection< SystemModel.Item > context,
                                  Collection< SystemModel.Item > specifier,
                                  SystemModel.Item newValue,
                                  Boolean failForMultipleItemMatches ) {
        for ( SystemModel.ModelItem itemType : itemTypes ) {
            for ( SystemModel.Item subcontext : context ) {
                for ( SystemModel.Item subspecifier : specifier ) {
                    if ( model.maySet( itemType, subcontext, subspecifier,
                                          newValue, failForMultipleItemMatches ) ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public boolean maySet( SystemModel.ModelItem itemType,
                           SystemModel.Item context,
                           SystemModel.Item specifier,
                           SystemModel.Item newValue,
                           Boolean failForMultipleItemMatches ) {
        return maySet( this, itemType, context, specifier, newValue,
                       failForMultipleItemMatches );
    }
    public static boolean maySet( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                                  SystemModel.ModelItem itemType,
                           SystemModel.Item context,
                           SystemModel.Item specifier,
                           SystemModel.Item newValue,
                           Boolean failForMultipleItemMatches ) {
        SystemModel.ModelItem contextType = context == null ? null : context.kind;
        SystemModel.ModelItem specifierType = specifier == null ? null : specifier.kind;
        SystemModel.ModelItem newValueType = newValue == null ? null : newValue.kind;
        return maySet( model, itemType, contextType, specifierType,
                       newValueType, failForMultipleItemMatches );
    }

    public boolean maySet( SystemModel.ModelItem itemType,
                           SystemModel.ModelItem contextType,
                           SystemModel.ModelItem specifierType,
                           SystemModel.ModelItem newValueType,
                           Boolean failForMultipleItemMatches ) {
        return maySet( this, itemType, contextType, specifierType,
                       newValueType, failForMultipleItemMatches );
    }
    public static boolean maySet( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                                  SystemModel.ModelItem itemType,
                                  SystemModel.ModelItem contextType,
                                  SystemModel.ModelItem specifierType,
                                  SystemModel.ModelItem newValueType,
                                  Boolean failForMultipleItemMatches ) {
        //if ( contextType == null ) return false;
        //if ( contextType == null && specifierType == null ) return false;
        if ( model != null ) {
            if ( itemType == ModelItem.NAME && !model.namesAreWritable() ) {
                return false;
            }
            if ( itemType == ModelItem.IDENTIFIER && !model.idsAreWritable() ) {
                return false;
            }
        }
        return true;
    }

    public boolean mayGet( Collection< SystemModel.ModelItem > itemTypes,
                           Collection< SystemModel.Item > context,
                           Collection< SystemModel.Item > specifier,
                           Boolean failForMultipleItemMatches ) {
        return mayGet( this, itemTypes, context, specifier,
                       failForMultipleItemMatches );

    }
    public static boolean mayGet( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                                  Collection< SystemModel.ModelItem > itemTypes,
                                  Collection< SystemModel.Item > context,
                                  Collection< SystemModel.Item > specifier,
                                  Boolean failForMultipleItemMatches ) {
        for ( SystemModel.ModelItem itemType : itemTypes ) {
            for ( SystemModel.Item subcontext : context ) {
                for ( SystemModel.Item subspecifier : specifier ) {
                    if ( model.mayGet( itemType, subcontext, subspecifier,
                                       failForMultipleItemMatches ) ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public boolean mayGet( SystemModel.ModelItem itemType,
                           SystemModel.Item context,
                           SystemModel.Item specifier,
                           Boolean failForMultipleItemMatches ) {
        return mayGet( this, itemType, context, specifier,
                       failForMultipleItemMatches );
    }
    public static boolean mayGet( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                                  SystemModel.ModelItem itemType,
                                  SystemModel.Item context,
                                  SystemModel.Item specifier,
                                  Boolean failForMultipleItemMatches ) {
        SystemModel.ModelItem contextType = context == null ? null : context.kind;
        SystemModel.ModelItem specifierType = specifier == null ? null : specifier.kind;
        return mayGet( model, itemType, contextType, specifierType,
                       failForMultipleItemMatches );
    }
    public boolean mayGet( SystemModel.ModelItem itemType,
                           SystemModel.ModelItem contextType,
                           SystemModel.ModelItem specifierType,
                           Boolean failForMultipleItemMatches ) {
        return mayGet( this, itemType, contextType, specifierType,
                       failForMultipleItemMatches );
    }
    public static boolean mayGet( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                                  SystemModel.ModelItem itemType,
                                  SystemModel.ModelItem contextType,
                                  SystemModel.ModelItem specifierType,
                                  Boolean failForMultipleItemMatches ) {
        // TODO Auto-generated method stub
        if ( contextType == null && specifierType == null ) return false;
        return true;
    }

    public boolean mayDelete( Collection< SystemModel.ModelItem > itemTypes,
                              Collection< SystemModel.Item > contexts,
                              Collection< SystemModel.Item > specifiers,
                              Boolean failForMultipleItemMatches ) {
        return mayDelete( this, itemTypes, contexts, specifiers,
                          failForMultipleItemMatches );
    }
    public static boolean mayDelete( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                                     Collection< SystemModel.ModelItem > itemTypes,
                                     Collection< SystemModel.Item > context,
                                     Collection< SystemModel.Item > specifier,
                                     Boolean failForMultipleItemMatches ) {
        for ( SystemModel.ModelItem itemType : itemTypes ) {
            for ( SystemModel.Item subcontext : context ) {
                for ( SystemModel.Item subspecifier : specifier ) {
                    if ( model.mayDelete( itemType, subcontext, subspecifier,
                                          failForMultipleItemMatches ) ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public boolean mayDelete( SystemModel.ModelItem itemType,
                              SystemModel.Item context,
                              SystemModel.Item specifier,
                              Boolean failForMultipleItemMatches ) {
        return mayDelete( this, itemType, context, specifier, failForMultipleItemMatches );
    }

    public static boolean mayDelete( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                                     SystemModel.ModelItem itemType,
                                     SystemModel.Item context, SystemModel.Item specifier,
                                     Boolean failForMultipleItemMatches ) {
        SystemModel.ModelItem contextType =
                context == null ? null : context.kind;
        SystemModel.ModelItem specifierType =
                specifier == null ? null : specifier.kind;
        return mayDelete( model, itemType, contextType, specifierType,
                          failForMultipleItemMatches );
    }

    public boolean mayDelete( SystemModel.ModelItem itemType,
                              SystemModel.ModelItem contextType,
                              SystemModel.ModelItem specifierType,
                              Boolean failForMultipleItemMatches ) {
        return mayDelete( this, itemType, contextType, specifierType,
                          failForMultipleItemMatches );
    }

    public static boolean mayDelete( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                                     SystemModel.ModelItem itemType,
                                     SystemModel.ModelItem contextType,
                                     SystemModel.ModelItem specifierType,
                                     Boolean failForMultipleItemMatches ) {
        //if ( contextType == null ) return false;
        //if ( contextType == null && specifierType == null ) return false;
        if ( model != null ) {
            if ( itemType == ModelItem.NAME && !model.namesAreWritable() ) {
                return false;
            }
            if ( itemType == ModelItem.IDENTIFIER && !model.idsAreWritable() ) {
                return false;
            }
        }
        return true;
    }

    public boolean mayCreate( Collection< SystemModel.ModelItem > itemTypes,
                              Collection< SystemModel.Item > context,
                              Collection< SystemModel.Item > specifier,
                              SystemModel.Item newValue,
                              Boolean failForMultipleItemMatches ) {
        return mayCreate( this, itemTypes, context, specifier, newValue,
                          failForMultipleItemMatches );
    }

    public static boolean mayCreate( AbstractSystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > model,
                                     Collection< SystemModel.ModelItem > itemTypes,
                                     Collection< SystemModel.Item > context,
                                     Collection< SystemModel.Item > specifier,
                                     SystemModel.Item newValue,
                                     Boolean failForMultipleItemMatches ) {
            for ( SystemModel.ModelItem itemType : itemTypes ) {
                for ( SystemModel.Item subcontext : context ) {
                    for ( SystemModel.Item subspecifier : specifier ) {
                        if ( model.mayCreate( itemType, subcontext, subspecifier,
                                              newValue, failForMultipleItemMatches ) ) {
                            return true;
                        }
                    }
                }
            }
        return false;
    }

    public boolean mayCreate( SystemModel.ModelItem itemType,
                              SystemModel.Item context,
                              SystemModel.Item specifier,
                              SystemModel.Item newValue,
                              Boolean failForMultipleItemMatches ) {
        return mayCreate( this, itemType, context, specifier, newValue,
                          failForMultipleItemMatches );
    }
    public static boolean mayCreate( AbstractSystemModel<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> model,
                                     SystemModel.ModelItem itemType,
                              SystemModel.Item context,
                              SystemModel.Item specifier,
                              SystemModel.Item newValue,
                              Boolean failForMultipleItemMatches ) {
        SystemModel.ModelItem contextType = context == null ? null : context.kind;
        SystemModel.ModelItem specifierType = specifier == null ? null : specifier.kind;
        SystemModel.ModelItem newValueType = newValue == null ? null : newValue.kind;
        return mayCreate( model, itemType, contextType, specifierType,
                          newValueType, failForMultipleItemMatches );
    }

    public boolean mayCreate( SystemModel.ModelItem itemType,
                              SystemModel.ModelItem contextType,
                              SystemModel.ModelItem specifierType,
                              SystemModel.ModelItem newValueType,
                              Boolean failForMultipleItemMatches ) {
        return mayCreate( this, itemType, contextType, specifierType,
                          newValueType, failForMultipleItemMatches );
    }

    public static boolean mayCreate( AbstractSystemModel<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> model,
                                     SystemModel.ModelItem itemType,
                                     SystemModel.ModelItem contextType,
                                     SystemModel.ModelItem specifierType,
                                     SystemModel.ModelItem newValueType,
                                     boolean failForMultipleItemMatches ) {
        if ( contextType != null && itemType != null
             && !canContain( contextType, itemType ) ) {
            return false;
        }
        if ( model != null ) {
            if ( itemType == ModelItem.NAME && !model.namesAreWritable() ) {
                return false;
            }
            if ( itemType == ModelItem.IDENTIFIER && !model.idsAreWritable() ) {
                return false;
            }
            if ( itemType == ModelItem.VERSION && !model.versionsAreWritable() ) {
                return false;
            }
        }
        // specifier is used to specify which item, but if the item is created, it doesn't need to be specified; a new value can be used
        //if ( itemType != null && specifierType != null ) return false;
        return true;
    }

    /* (non-Javadoc)
     * @see SystemModel#get(java.util.Collection, java.util.Collection, java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public Collection< Object >
            get( Collection< SystemModel.ModelItem > itemTypes,
                 Collection< Item > context, I identifier, N name, V version ) {
        return op( Operation.GET, itemTypes, context, identifier, name, version, null, false );
    }

    /* (non-Javadoc)
     * @see SystemModel#create(SystemModel.ModelItem, java.util.Collection, java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public Collection< Object > create( SystemModel.ModelItem itemType,
                                        Collection< Item > context, I identifier,
                                        N name, V version ) {
        Collection< SystemModel.ModelItem > itemTypes = new ArrayList< SystemModel.ModelItem >();
        itemTypes.add(itemType);
        return op( Operation.CREATE, itemTypes, context, identifier, name, version, null, false );
    }

    /* (non-Javadoc)
     * @see SystemModel#delete(SystemModel.ModelItem, java.util.Collection, java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public Collection< Object > delete( SystemModel.ModelItem itemType,
                                        Collection< Item > context, I identifier,
                                        N name, V version ) {
        Collection< SystemModel.ModelItem > itemTypes = new ArrayList< SystemModel.ModelItem >();
        itemTypes.add(itemType);
        return op( Operation.CREATE, itemTypes, context, identifier, name, version, null, false );
    }

    /* (non-Javadoc)
     * @see SystemModel#set(SystemModel.ModelItem, java.util.Collection, java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public Collection< Object > set( SystemModel.ModelItem itemType,
                                     Collection< Item > context, I identifier,
                                     N name, V version, U newValue ) {
        Collection< SystemModel.ModelItem > itemTypes = new ArrayList< SystemModel.ModelItem >();
        itemTypes.add(itemType);
        return op( Operation.CREATE, itemTypes, context, identifier, name, version, newValue, false );
    }

    public String getNameString( C context ) {
        return "" + getName( context );
    }

    @Override
    public Class<?> getClass(ModelItem item) {
        switch ( item ) {
            case CONSTRAINT:
                return getConstraintClass();
//            case CONTEXT:
//                return getContextClass();
            case IDENTIFIER:
                return getIdentifierClass();
            case NAME:
                return getNameClass();
            case ELEMENT:
                return getElementClass();
            case PROPERTY:
                return getPropertyClass();
            case RELATIONSHIP:
                return getRelationshipClass();
            case TYPE:
                return getTypeClass();
            case VALUE:
                return getValueClass();
            case VERSION:
                return getVersionClass();
            case VIEW:
                return getViewClass();
            case VIEWPOINT:
                return getViewpointClass();
            case WORKSPACE:
                return getWorkspaceClass();
            default:
                Debug.error( "Unexpected SystemModel.ModelItem: " + item );
        }
        return null;
    }

    /**
     * @param o
     * @param cls the type to which the Object is converted
     * @return a conversion of the Object to the specified type, null if o is null or if the conversion is unsuccessful
     */
    protected static <T> T as( Object o, Class<T> cls ) {
        T t = null;
        Pair< Boolean, T > res = ClassUtils.coerce( o, cls, true );
        if ( res != null && res.first != null && res.first == true ) t = res.second;
        return t;
    }

    /* (non-Javadoc)
     * @see sysml.SystemModel#asName(java.lang.Object)
     */
    @Override
    public N asName( Object o ) {
        return as( o, getNameClass() );
    }

    /* (non-Javadoc)
     * @see sysml.SystemModel#asIdentifier(java.lang.Object)
     */
    @Override
    public I asIdentifier( Object o ) {
        return as( o, getIdentifierClass() );
    }

    /* (non-Javadoc)
     * @see SystemModel#asObject(java.lang.Object)
     */
    @Override
    public E asElement( Object o ) {
        return as( o, getElementClass() );
    }

    /* (non-Javadoc)
     * @see sysml.SystemModel#asContextCollection(java.lang.Object)
     */
    @Override
    public Collection<C> asContextCollection( Object object ) {
        if ( object == null ) return null;
        Pair< Boolean, List< C > > result =
                ClassUtils.coerceList( object, getContextClass(), true );
        return result.second;
    }

    /* (non-Javadoc)
     * @see SystemModel#asContext(java.lang.Object)
     */
    @Override
    public C asContext( Object object ) {
        return as( object, getContextClass() );
    }

    /* (non-Javadoc)
     * @see SystemModel#asType(java.lang.Object)
     */
    @Override
    public T asType( Object o ) {
        return as( o, getTypeClass() );
    }

    /* (non-Javadoc)
     * @see SystemModel#asProperty(java.lang.Object)
     */
    @Override
    public P asProperty( Object o ) {
        return as( o, getPropertyClass() );
    }

    /* (non-Javadoc)
     * @see SystemModel#asValue(java.lang.Object)
     */
    @Override
    public U asValue( Object o ) {
        return as( o, getValueClass() );
    }

    /* (non-Javadoc)
     * @see SystemModel#asRelationship(java.lang.Object)
     */
    @Override
    public R asRelationship( Object o ) {
        return as( o, getRelationshipClass() );
    }

    /* (non-Javadoc)
     * @see SystemModel#asVersion(java.lang.Object)
     */
    @Override
    public V asVersion( Object o ) {
        return as( o, getVersionClass() );
    }

    /* (non-Javadoc)
     * @see SystemModel#asWorkspace(java.lang.Object)
     */
    @Override
    public W asWorkspace( Object o ) {
        return as( o, getWorkspaceClass() );
    }

    /* (non-Javadoc)
     * @see SystemModel#asConstraint(java.lang.Object)
     */
    @Override
    public CT asConstraint( Object o ) {
        return as( o, getConstraintClass() );
    }

    /* (non-Javadoc)
     * @see SystemModel#idsAreWritable()
     */
    @Override
    public abstract boolean idsAreWritable();

    /* (non-Javadoc)
     * @see SystemModel#namesAreWritable()
     */
    @Override
    public abstract boolean namesAreWritable();

    /* (non-Javadoc)
     * @see SystemModel#versionsAreWritable()
     */
    @Override
    public abstract boolean versionsAreWritable();

    @Override
    public Collection< E > getElement( C context,
                                       Object specifier ) {
        // REVIEW -- should check permissions before trying
        Collection< E > elements =
                new TreeSet< E >( CompareUtils.GenericComparator.instance() );

        // If there's no specifier, then this implementation won't work.
        if ( specifier == null ) {
            return elements;
        }

        I id = asIdentifier( specifier );
        if ( id != null || specifier == null ) {
            elements.addAll( getElementWithIdentifier( context, id ) );
            if ( !elements.isEmpty() ) return elements;
        }
        //
        N name = asName( specifier );
        if ( name != null ) {
            elements.addAll( getElementWithName( context, name ) );
            if ( !elements.isEmpty() ) return elements;
        }
        V version = asVersion( specifier );
        if ( version != null ) {
            elements.addAll( getElementWithVersion( context, version ) );
            if ( !elements.isEmpty() ) return elements;
        }
        W ws = asWorkspace( specifier );
        if ( ws != null ) {
            elements.addAll( getElementWithWorkspace( context, ws ) );
            if ( !elements.isEmpty() ) return elements;
        }
        T type = asType( specifier );
        if ( type != null ) {
            elements.addAll( getElementWithType( context, type ) );
            if ( !elements.isEmpty() ) return elements;
        }
        R rel = asRelationship( specifier );
        if ( rel != null ) {
            elements.addAll( getElementWithRelationship( context, rel ) );
            if ( !elements.isEmpty() ) return elements;
        }
        P property = asProperty( specifier );
        if ( property != null ) {
            elements.addAll( getElementWithProperty( context, property ) );
            if ( !elements.isEmpty() ) return elements;
        }
        U val = asValue( specifier );
        if ( val != null ) {
            elements.addAll( getElementWithValue( context, val ) );
            if ( !elements.isEmpty() ) return elements;
        }
        CT constraint = asConstraint( specifier );
        if ( constraint != null ) {
            elements.addAll( getElementWithConstraint( context, constraint ) );
            if ( !elements.isEmpty() ) return elements;
        }
        // TODO -- repeat for the remaining item types!
        return elements;
    }


//    /* (non-Javadoc)
//     * @see SystemModel#elementsMayBeChangedForVersion(java.lang.Object)
//     */
//    @Override
//    public abstract boolean elementsMayBeChangedForVersion( V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#typesMayBeChangedForVersion(java.lang.Object)
//     */
//    @Override
//    public abstract boolean typesMayBeChangedForVersion( V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#propertiesMayBeChangedForVersion(java.lang.Object)
//     */
//    @Override
//    public abstract boolean propertiesMayBeChangedForVersion( V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#elementsMayBeCreatedForVersion(java.lang.Object)
//     */
//    @Override
//    public abstract boolean elementsMayBeCreatedForVersion( V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#typesMayBeCreatedForVersion(java.lang.Object)
//     */
//    @Override
//    public abstract boolean typesMayBeCreatedForVersion( V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#propertiesMayBeCreatedForVersion(java.lang.Object)
//     */
//    @Override
//    public abstract boolean propertiesMayBeCreatedForVersion( V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#elementsMayBeDeletedForVersion(java.lang.Object)
//     */
//    @Override
//    public abstract boolean elementsMayBeDeletedForVersion( V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#typesMayBeDeletedForVersion(java.lang.Object)
//     */
//    @Override
//    public abstract boolean typesMayBeDeletedForVersion( V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#propertiesMayBeDeletedForVersion(java.lang.Object)
//     */
//    @Override
//    public abstract boolean propertiesMayBeDeletedForVersion( V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#createElement(java.lang.Object, java.lang.Object)
//     */
//    @Override
//    public abstract E createElement( I identifier, V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#setIdentifier(java.lang.Object, java.lang.Object)
//     */
//    @Override
//    public abstract boolean setIdentifier( E element, V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#setName(java.lang.Object, java.lang.Object)
//     */
//    @Override
//    public abstract boolean setName( E element, V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#setType(java.lang.Object, java.lang.Object)
//     */
//    @Override
//    public abstract boolean setType( E element, V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#deleteObject(java.lang.Object, java.lang.Object)
//     */
//    @Override
//    public abstract E deleteElement( I identifier, V version );
//
//    /* (non-Javadoc)
//     * @see SystemModel#deleteType(java.lang.Object, java.lang.Object)
//     */
//    @Override
//    public abstract T deleteType( E element, V version );

    /* (non-Javadoc)
     * @see SystemModel#map(java.util.Collection, SystemModel.MethodCall, int)
     */
    @Override
    public Collection< Object >
            map( Collection< E > elements,
                 MethodCall methodCall,
                 int indexOfElementArgument ) throws InvocationTargetException {
        return methodCall.map( elements, indexOfElementArgument );
    }

    /* (non-Javadoc)
     * @see SystemModel#filter(java.util.Collection, SystemModel.MethodCall, int)
     */
    @Override
    public Collection< E >
            filter( Collection< E > elements,
                    MethodCall methodCall,
                    int indexOfElementArgument ) throws InvocationTargetException {
        return methodCall.filter( elements, indexOfElementArgument );
    }

    /* (non-Javadoc)
     * @see SystemModel#forAll(java.util.Collection, SystemModel.MethodCall, int)
     */
    @Override
    public boolean
            forAll( Collection< E > elements,
                    MethodCall methodCall,
                    int indexOfElementArgument ) throws InvocationTargetException {
        //return methodCall.
        return false;
    }

    /* (non-Javadoc)
     * @see SystemModel#thereExists(java.util.Collection, SystemModel.MethodCall, int)
     */
    @Override
    public boolean thereExists( Collection< E > elements,
                                MethodCall methodCall,
                                int indexOfElementArgument ) throws InvocationTargetException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see SystemModel#fold(java.util.Collection, java.lang.Object, SystemModel.MethodCall, int, int)
     */
    @Override
    public Object fold( Collection< E > elements, Object initialValue,
                        MethodCall methodCall, int indexOfElementArgument,
                        int indexOfPriorResultArgument ) throws InvocationTargetException {
        return methodCall.fold( elements, initialValue, indexOfElementArgument, indexOfPriorResultArgument );
    }

    /* (non-Javadoc)
     * @see SystemModel#sort(java.util.Collection, java.util.Comparator, SystemModel.MethodCall, int)
     */
    @Override
    public Collection< E >
            sort( Collection< E > elements, Comparator< ? > comparator,
                  MethodCall methodCall,
                  int indexOfElementArgument ) throws InvocationTargetException {
        return methodCall.sort( elements, comparator, indexOfElementArgument );
    }

    /* (non-Javadoc)
     * @see SystemModel#getDomainConstraint(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public abstract CT getDomainConstraint( E element, V version, W workspace );

    /* (non-Javadoc)
     * @see SystemModel#addConstraint(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public abstract void addConstraint( CT constraint, V version, W workspace );

    /* (non-Javadoc)
     * @see SystemModel#addDomainConstraint(java.lang.Object, java.lang.Object, java.util.Set, java.lang.Object)
     */
    @Override
    public abstract void addDomainConstraint( CT constraint, V version,
                                     Set< U > valueDomainSet, W workspace );

    /* (non-Javadoc)
     * @see SystemModel#addDomainConstraint(java.lang.Object, java.lang.Object, gov.nasa.jpl.mbee.util.Pair, java.lang.Object)
     */
    @Override
    public abstract void
            addDomainConstraint( CT constraint, V version,
                                 Pair< U, U > valueDomainRange, W workspace );

    /* (non-Javadoc)
     * @see SystemModel#relaxDomain(java.lang.Object, java.lang.Object, java.util.Set, java.lang.Object)
     */
    @Override
    public abstract void relaxDomain( CT constraint, V version, Set< U > valueDomainSet,
                             W workspace );

    /* (non-Javadoc)
     * @see SystemModel#relaxDomain(java.lang.Object, java.lang.Object, gov.nasa.jpl.mbee.util.Pair, java.lang.Object)
     */
    @Override
    public abstract void relaxDomain( CT constraint, V version,
                             Pair< U, U > valueDomainRange, W workspace );

    /* (non-Javadoc)
     * @see SystemModel#getConstraintsOfElement(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public abstract Collection< CT > getConstraintsOfElement( E element, V version,
                                                     W workspace );

//    /* (non-Javadoc)
//     * @see SystemModel#getConstraintsOfContext(java.lang.Object)
//     */
//    @Override
//    public abstract Collection< CT > getConstraintsOfContext( C context );

    /* (non-Javadoc)
     * @see SystemModel#getViolatedConstraintsOfElement(java.lang.Object, java.lang.Object)
     */
    @Override
    public abstract Collection< CT > getViolatedConstraintsOfElement( E element,
                                                             V version );

//    /* (non-Javadoc)
//     * @see SystemModel#getViolatedConstraintsOfContext(java.lang.Object)
//     */
//    @Override
//    public abstract Collection< CT > getViolatedConstraintsOfContext( C context );

    /* (non-Javadoc)
     * @see SystemModel#setOptimizationFunction(java.lang.reflect.Method, java.lang.Object[])
     */
    @Override
    public abstract void setOptimizationFunction( Method method, Object... arguments );

    /* (non-Javadoc)
     * @see SystemModel#getScore()
     */
    @Override
    public abstract Number getScore();

}
