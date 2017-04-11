/**
 *
 */
package gov.nasa.jpl.sysml;

import gov.nasa.jpl.mbee.util.ClassUtils;
import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.MethodCall;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Utils;

import java.lang.Object;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import gov.nasa.jpl.sysml.AbstractReference.Interpretation.Category;
import gov.nasa.jpl.sysml.SystemModel.Item;
import gov.nasa.jpl.sysml.SystemModel.ModelItem;

/**
 * AbstractReference simply provides fields for supporting accessors in
 * Reference. Subclasses of AbstractReference must minimally redefine
 * makeReference(), getAlternatives(), and getItems().
 */
public class AbstractReference< RT, SM extends SystemModel< E, C, T, P, N, I, U, R, V, W, CT >, E, C, T, P, N, I, U, R, V, W, CT > implements Reference< RT, SM > {

    // try to use ModelItem enum as interpretation
    public static class Interpretation {
        public static enum Category { ModelItem, Related, RelatedSource, RelatedTarget, MULTIPLE, UNKNOWN };
        Category category = Category.UNKNOWN;
        SystemModel.ModelItem modelItemInterpretation = null;
        public Interpretation() {}
        public Interpretation( Category category,
                               ModelItem modelItemInterpretation ) {
            super();
            this.category = category;
            this.modelItemInterpretation = modelItemInterpretation;
        }
    }

    SM model = null;
    Object scope = null;
    Object specifier = null;
    Object nextSpecifier = null;
    Class< RT > type = null;
    Boolean isTemplate = null;

    Interpretation interpretation = new Interpretation();

    List< Reference< ? extends RT, SM > > alternatives = null;
    protected Collection< RT > items = null;

    public AbstractReference() {
    }

    public AbstractReference( SM model ) {
        this.model = model;
    }

    public AbstractReference( SM model,
                              Object scope,
                              Object specifier,
                              Object nextSpecifier,
                              Class< RT > type,
                              Boolean isTemplate ) {
        super();
        this.model = model;
        this.scope = scope;
        this.type = type;
        this.specifier = specifier;
        this.nextSpecifier = nextSpecifier;
        this.isTemplate = isTemplate;
    }

    @Override
    public AbstractReference< RT, SM, E, C, T, P, N, I, U, R, V, W, CT > makeReference()
    {
        return new AbstractReference< RT, SM, E, C, T, P, N, I, U, R, V, W, CT >();
    }

    @Override
    public AbstractReference< RT, SM, E, C, T, P, N, I, U, R, V, W, CT > makeReference( SM model ) {
        //return new AbstractReference< RT >( model );
        AbstractReference< RT, SM, E, C, T, P, N, I, U, R, V, W, CT > r = makeReference();
        r.setModel( model );
        return r;
    }

    @Override
    public AbstractReference< RT, SM, E, C, T, P, N, I, U, R, V, W, CT > makeReference( SM model,
                                           Object scope, Class< RT > type, Object specifier,
                                           Object nextSpecifier, boolean isTemplate ) {
        //return new AbstractReference< RT >( model, scope, type, specifier, nextSpecifier, isTemplate );
        AbstractReference< RT, SM, E, C, T, P, N, I, U, R, V, W, CT > r = makeReference();
        r.setModel( model );
        r.setScope( scope );
        r.setType( type );
        r.setSpecifier( specifier );
        r.setNextSpecifier( nextSpecifier );
        r.setIsTemplate( isTemplate );
        return r;
    }

    public AbstractReference< RT, SM, E, C, T, P, N, I, U, R, V, W, CT > clone() throws CloneNotSupportedException {
        //return new AbstractReference< RT >( model, scope, type, specifier, nextSpecifier, type, isTemplate );
        return makeReference( model, scope, type, specifier, nextSpecifier, isTemplate );
    }


    /* (non-Javadoc)
     * @see sysml.Reference#getModel()
     */
    @Override
    public SM getModel() {
        return this.model;
    }

    @Override
    public void setModel( SM model ) {
        this.model = model;
    }

    @Override
    public Object getScope() {
        return scope;
    }

    @Override
    public void setScope( Object scope ) {
        this.scope = scope;
    }

    @Override
    public Object getSpecifier() {
        return specifier;
    }

    @Override
    public void setSpecifier( Object specifier ) {
        this.specifier = specifier;
    }

    @Override
    public Class< RT > getType() {
        return type;
    }

    @Override
    public void setType( Class< RT > type ) {
        this.type = type;
    }

    @Override
    public Object getNextSpecifier() {
        return nextSpecifier;
    }

    @Override
    public void setNextSpecifier( Object nextSpecifier ) {
        this.nextSpecifier = nextSpecifier;
    }

    @Override
    public boolean isTemplate() {
        return isTemplate;
    }

    @Override
    public void setIsTemplate( boolean isTemplate ) {
        this.isTemplate = isTemplate;
    }

    /* (non-Javadoc)
     * @see sysml.Reference#isConsistent()
     */
    @Override
    public boolean isConsistent() {
        Collection< RT > items = getItems();
        if ( !isTemplate() && ( items == null || items.isEmpty() ) ) return false;
        if ( items != null && getType() != null ) {
            for ( RT item : items ) {
                if ( item != null && !getType().isInstance( item ) ) return false;
            }
        }
        return true;
    }

    @Override
    public List< Reference< ? extends RT, SM > > getAlternatives() {
        if ( alternatives == null ) {
            alternatives = new ArrayList< Reference< ? extends RT, SM > >();
        } else alternatives.clear();
        for ( Interpretation.Category c : Interpretation.Category.values() ) {
            SystemModel.ModelItem mi = null;
            Collection< RT > items;
            Interpretation interp = null;
            if ( c == Category.ModelItem ) {
                for ( SystemModel.ModelItem i : SystemModel.ModelItem.values() ) {
                    interp = new Interpretation( c, i );
                    Reference<RT, SM> alternative = getReferenceForInterpretation( interp );
                    if ( alternative != null ) alternatives.add( alternative );
                }
            } else {
                interp = new Interpretation( c, null );
                Reference<RT, SM> alternative = getReferenceForInterpretation( interp );
                if ( alternative != null ) alternatives.add( alternative );
            }
        }
        return alternatives;
    }
//  throw new UnsupportedOperationException();

    public AbstractReference< RT, SM, E, C, T, P, N, I, U, R, V, W, CT >
            getReferenceForInterpretation( Interpretation interp ) {
        Collection< RT > items = getItemsForInterpretation( interp );
        if ( !Utils.isNullOrEmpty( items ) ) {
            AbstractReference< RT, SM, E, C, T, P, N, I, U, R, V, W, CT > r;
            try {
                r = this.clone();
                r.interpretation = interp;
                r.setItems( items );
                return r;
            } catch ( CloneNotSupportedException e ) {
                e.printStackTrace();
            }
        }
        return null;
    }

    protected void setItems( Collection< RT > items ) {
        this.items  = items;
    }

    /**
     * @return a Collection of contexts as can be derived from the scope.
     */
    public Collection<C> getScopeAsContext() {
        Object scope = getScope();
        return getModel().asContextCollection( scope );
    }

    protected Collection< RT > getElementForRole( N role ) {
        Collection< RT > theItems = null;
        E scopeElement = getModel().asElement( getScope() );
        Collection< R > rels =
                getModel().getRelationshipWithName( (C)scopeElement,
                                                    getModel().asName( getSpecifier() ) );
        //theItems = Utils.asList( rels, getType() );
        if ( Utils.isNullOrEmpty( rels ) ) {
            String s = getSpecifier().toString().toLowerCase();
            if ( s.contains( "relation" ) || s.contains("source") ) {
                rels = getModel().getRelationship( (C)scopeElement, null );
                Method method;
                try {
                    method = getModel().getClass().getMethod( "getSource()",
                                                              new Class< ? >[] {} );
                    MethodCall methodCall =
                            new MethodCall( getModel(), method,
                                                        new Object[]{} );

                    Collection< Object > related = MethodCall.map( rels, methodCall, 0 );
                    theItems = Utils.asList( related, getType() );
                } catch ( NoSuchMethodException e ) {
                    e.printStackTrace();
                }
            }
        }
        return theItems;

    }

    public Collection< RT > getItemsForInterpretation( Interpretation i ) {
        E scopeElement = null;
        Pair< Boolean, E > resO = null;
        Collection< RT > theItems = null;
        Collection< E > related = null;
        switch ( i.category ) {
            case UNKNOWN:
                return Collections.emptyList();
            case Related:
////                    getItemsForInterpretation( new Interpretation( Category.ModelItem, ModelItem.RELATIONSHIP ) );
////                if ( items == null ) return Collections.emptyList();
////                for ( RT o : items ) {
//                resO = ClassUtils.coerce( getScope(),
//                                          getModel().getElementClass(), true );
//                scopeElement = resO.first ? resO.second : (E)getScope(); // REVIEW -- potential ClassCastException!
                scopeElement = getModel().asElement( getScope() );
                Collection< R > relationships = getModel().getRelationship( (C)scopeElement, getSpecifier() );
                for ( R r : relationships ) {
                    Collection< E > result = getModel().getRelatedElements( r );
                    if ( related == null ) related = result;
                    else if ( result != null ) related.addAll( result );
                }
                Pair< Boolean, List< RT > > p = ClassUtils.coerceList( related, getType(), true ); // REVIEW -- need to use coerce instead?
                theItems = p.second;
                //                }
//                SystemModel.MethodCall methodCall =
//                        new SystemModel.MethodCall( getModel(),
//                                                    getType().getMethod( "isInstance",
//                                                                         new Class<?>[] { Object.class } ),
//                                                                         new Object[]{ null } );
//                Collection< E > elements;
//                Collection< Object > otherItems = getModel().map( elements, methodCall, 0000 );
//                methodCall =
//                        new SystemModel.MethodCall( getType(),
//                                                    getType().getMethod( "isInstance",
//                                                                         new Class<?>[] { Object.class } ),
//                                                    new Object[]{ null } );
//                Collection< RT > filterResults = filter( items, methodCall, 0 );
//                return filterResults;
                return theItems;
//                break;
            case RelatedSource:
//                resO = ClassUtils.coerce( getScope(),
//                                          getModel().getElementClass(), true );
//                scopeElement = resO.first ? resO.second : (E)getScope(); // REVIEW -- potential ClassCastException!
                scopeElement = getModel().asElement( getScope() );
                Collection< R > rels =
                        getModel().getRelationshipWithName( (C)scopeElement,
                                                            getModel().asName( getSpecifier() ) );
                //theItems = Utils.asList( rels, getType() );
                if ( Utils.isNullOrEmpty( rels ) ) {
                    String s = getSpecifier().toString().toLowerCase();
                    if ( s.contains( "relation" ) || s.contains("source") ) {
                        rels = getModel().getRelationship( (C)scopeElement, null );
                        Method method;
                        try {
                            method = getModel().getClass().getMethod( "getSource()",
                                                             new Class< ? >[] {} );
                            MethodCall methodCall =
                                    new MethodCall( getModel(), method,
                                                                new Object[]{} );

                            related = MethodCall.map( rels, methodCall, 0 );
                            theItems = Utils.asList( related, getType() );
                        } catch ( NoSuchMethodException e ) {
                            e.printStackTrace();
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                return theItems;
            case RelatedTarget:
                // TODO
                break;
            case ModelItem:
                if ( i.modelItemInterpretation == null ) {
                    return Collections.emptyList();
                }
                Collection< RT > items = Collections.emptyList();
                Collection< ModelItem > itemTypes =
                        Utils.newList( i.modelItemInterpretation );
//                Collection< C > context = getScopeAsContext();
//                if ( Utils.isNullOrEmpty( context ) ) {
//                    context = getModel().asContextCollection(getModel());
//                }
                Collection<Item> context = new ArrayList<Item>();
                context.add( new Item( getScope(), ModelItem.ELEMENT ) );
                I id = null;
                Pair< Boolean, I > resI =
                        ClassUtils.coerce( getSpecifier(),
                                           getModel().getIdentifierClass(),
                                           true );
                if ( resI.first ) id = resI.second;
                N name = null;
                Pair< Boolean, N > resN =
                        ClassUtils.coerce( getSpecifier(),
                                           getModel().getNameClass(), true );
                if ( resN.first ) name = resN.second;
                Collection< Object > result =
                        getModel().op( SystemModel.Operation.READ, itemTypes,
                                       context, id, name, null, null, false );
                Pair< Boolean, List< RT > > resL =
                        ClassUtils.coerceList( result, getType(), true );
                if ( resL.first ) {
                    items = resL.second;
                }
                return items;
//                switch( i.modelItemInterpretation ) {
//                    case CONSTRAINT:
//                    //{HERE!!!;} // TODO
//                        break;
//                    case CONTEXT:
//                    case IDENTIFIER:
//                    case NAME:
//                    case OBJECT:
//                    case PROPERTY:
//                    case RELATIONSHIP:
//                    case TYPE:
//                    case VALUE:
//                    case VERSION:
//                    case VIEW:
//                    case VIEWPOINT:
//                    case WORKSPACE:
//                    default:
//                    // TODO
//                };
            default:
                Debug.error( "Unknown interpretation category " + i.category );
                // TODO -- ERROR!
        };
        return Collections.emptyList();
    }

    @Override
    public Collection< RT > getItems() {
        // TOOD
        return items;
    }

}
