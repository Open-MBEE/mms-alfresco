/**
 *
 */
package gov.nasa.jpl.sysml;

import java.lang.Object;
import java.util.Collection;
import java.util.List;

// import org.eclipse.emf.ecore.EObject;

/**
 * A Reference specifies and retrieves items in a {@link SystemModel}. The
 * reference is made from a scope (for example, a model element or the entire
 * model). A specifier is used to select items from that scope. A Reference may
 * be disambiguated from the context by another specifier that may be applied to
 * the items referenced.
 *
 * For example, the reference to relation3 in elementA.relation3.partProperties
 * is in the scope of elementA. relation3 might be used to refer to
 * <ol>
 * <li>an object of type relation3 that relates elementA to another element,
 * <li>the type/class of relation3,
 * <li>an element related to elementA by relation3, or
 * <li>a collection of elements that each match one of the prior criteria.
 * </ol>
 *
 * We might think of a "dot" syntax for References as follows:
 * <p>
 * {@literal reference ::= specifier | reference '.' specifier | reference '(' specifier ')'}
 * <br>
 * {@literal
 * specifier ::= property | name | type
 * }
 * <p>
 * A Reference with no scope may be a reference in a global scope or a reference
 * template that may be given a scope to instantiate it. isTemplate() specifies
 * whether it should be interpreted as a template.
 * <p>
 * If a scope is a Collection, it is generally ambiguous whether the specifier
 * applies to each item in the Collection or to the Collection as a single
 * object.
 * <p>
 * A Reference may specify multiple items. In addition, a Reference may have
 * alternative interpretations. getAlternatives() provides a Collection of
 * References as alternative interpretations. If there is only one
 * interpretation of Reference r, r.getAlternatives() returns a Collection
 * containing only r. If r is a template or if there are no interpretations,
 * r.getAlternatives() returns an empty Collection. If and only if there are no
 * interpretations, and r is not a template, isConsistent() returns false.
 * Otherwise, each Reference in the returned Collection is a more specific
 * Reference.
 * <p>
 * getItems() returns a Collection of the referenced items for the first
 * alternative interpretation. getItems() is empty if and only if
 * getAlternatives() is empty. Thus, there can be no consistent reference to
 * nothing.
 *
 * A "next specifier" may be used to disambiguate alternatives. It is
 * interpreted as a specifier that may be applied to each of getItems().
 *
 */
public interface Reference< RT, SM extends SystemModel< ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? > > extends Cloneable {

    public abstract Reference< RT, SM > makeReference();

    public abstract //< O, C, T, P, N, I, U, R, V, W, CT >
            Reference< RT, SM > makeReference( SM model );//SystemModel< O, C, T, P, N, I, U, R, V, W, CT > model );

    public abstract //< O, C, T, P, N, I, U, R, V, W, CT >
            Reference< RT, SM > makeReference( SM model, //SystemModel< O, C, T, P, N, I, U, R, V, W, CT > model,
                                           Object scope,
                                           Class< RT > type,
                                           Object specifier,
                                           Object nextSpecifier,
                                           boolean isTemplate );

    public abstract //< O, C, T, P, N, I, U, R, V, W, CT >
            //SystemModel< O, C, T, P, N, I, U, R, V, W, CT > getModel();
        SM getModel();

    public abstract //< O, C, T, P, N, I, U, R, V, W, CT > void
            void setModel( SM model ); //SystemModel< O, C, T, P, N, I, U, R, V, W, CT > model );

    /**
     * The scopeReference field serves as the scope if it is not null and is not
     * the object of reference. If scopeReference is null, then the model serves
     * as the scope instead. If the model is also null, then the scope is the
     * object of the FunctionCall, this.expression. If this expression is not a
     * FunctionCall, then the scope is global and may depend on imports. If
     * specifier and model are null, then scopeReference is the object
     * referenced, alternatives should be empty or null, and the scopeReference
     * is the object of this.expression as a FunctionCall, or equal in value to
     * this.expression as a Parameter or a Value.
     *
     * @return the object from which the reference is specified.
     */
    public abstract Object getScope();

    public abstract void setScope( Object scope );

    // want evaluateAndGetAlternatives()
    // public abstract List< Reference< ? > > getAlternatives();
    //
    // public abstract void
    // setAlternatives( List< Reference< ? > > alternatives );

    /**
     * @return the specifier String
     */
    public abstract Object getSpecifier();

    /**
     * Sets the new specifier String to the input String after trimming empty
     * parenthesis arguments and whitespace.
     *
     * @param specifier
     *            the new specifier String
     */
    public abstract void setSpecifier( Object specifier );

    /**
     * @return the type of item(s) referenced
     */
    public abstract Class< RT > getType();

    /**
     * @param type
     *            the type of item(s) referenced
     */
    public abstract void setType( Class< RT > type );

    /**
     * @return the nextSpecifier
     */
    public abstract Object getNextSpecifier();

    /**
     * @param nextSpecifier
     *            the nextSpecifier to set
     */
    public abstract void setNextSpecifier( Object nextSpecifier );


    public abstract boolean isTemplate();

    public abstract void setIsTemplate( boolean isTemplate );

    public abstract boolean isConsistent();

    // /**
    // * @return the nextSpecifierString
    // */
    // public abstract Object getNextSpecifierString();

    // /**
    // * @param nextSpecifierString
    // * the nextSpecifierString to set
    // */
    // public abstract void setNextSpecifierString( Object nextSpecifierString
    // );

    /* ===================================================================== */

    /*
     * (non-Javadoc)
     *
     * @see gov.nasa.jpl.ae.event.Expression#clone()
     */
    public abstract Reference< RT, SM > clone() throws CloneNotSupportedException;

    public abstract List< Reference< ? extends RT, SM > > getAlternatives();

    public abstract Collection< RT > getItems();

    // /**
    // * Evaluate the reference and return a single object matching this
    // reference
    // * for one interpretation.
    // *
    // * @param propagate
    // * whether stale parameters are updated by evaluating other
    // * expressions.
    // * @return a single object that is referenced for some interpretation of
    // * this object.
    // */
    // public abstract Object evaluateAndGetOne( boolean propagate );
    //
    // /**
    // * Evaluate this reference and return a list of evaluation results, one
    // * {@code Collection\< T \>} for each interpretation.
    // *
    // * @param propagate
    // * whether stale parameters are updated by evaluating other
    // * expressions.
    // * @return the list of alternative evaluation results.
    // */
    // public abstract List< Collection< ? >>
    // evaluateAndGetAlternatives( boolean propagate );
    //
    // public abstract Collection< T >
    // evaluateAndGetCollection( boolean propagate );
    //
    // // A collection is NOT returned if the reference matches
    // // multiple elements/objects.
    // /*
    // * Resolve the reference and return the referenced object, which may
    // require
    // * picking an alternative and/or selecting from a set of matching objects.
    // * (non-Javadoc)
    // *
    // * @see gov.nasa.jpl.ae.event.Expression#evaluate(boolean)
    // */
    // public abstract Collection< T > evaluate( boolean propagate );
    //
    // /* =====================================================================
    // */
    // /* =====================================================================
    // */
    //
    // public abstract void
    // setResultTypes( Class< T > singleResultType,
    // Class< ? extends Collection< T > > resultType );
    //
    // /**
    // * Assign this Reference's members the values of another's members.
    // *
    // * @param Reference
    // */
    // public abstract < T > void copyMembers( Reference< T > Reference );
    //
    // /**
    // * Assign this Reference's members the values of another's members.
    // *
    // * @param Reference
    // */
    // public abstract void copyLocalMembers( Reference< T > Reference );
    //
    // public abstract void copyLocalMembers( Reference< T > Reference,
    // boolean deep );
    //
    // public abstract T getDirectReference();
    //
    // /**
    // * @return whether this reference contains the object being referenced.
    // */
    // public abstract boolean isDirectReference();
    //
    // /**
    // * Parses the expressionString in "dot" syntax to load Reference members.
    // * The specifier is after the last '.' in the string, and everything
    // before
    // * is the scopeReferenceString. If there is no '.', expressionString is
    // the
    // * specifier, and the scopeReference is the existing scopeReference or (if
    // * the scopeReference is null) the model.
    // *
    // * @param expressionString
    // * expression in dot syntax
    // * @param propagate
    // */
    // public abstract void
    // parseDotSyntax( String expressionString, String nextSpecifier,
    // boolean propagate );
    //
    // /**
    // * @param specifier
    // * @param nextSpecifier
    // * @param propagate
    // * whether stale parameters are updated by evaluating other
    // * expressions.
    // * @return a Reference for the specifier in the context of the existing
    // * Reference.
    // */
    // public abstract Reference< ? > findAlternatives( String specifier,
    // Object nextSpecifier,
    // boolean propagate );
    //
    // /**
    // * @return whether there is an object that matches this reference
    // */
    // public abstract boolean isEmpty();
    //
    // /**
    // * @param propagate
    // * whether stale parameters are updated by evaluating other
    // * expressions.
    // * @return whether there is an object that matches this reference
    // */
    // public abstract boolean isEmpty( boolean propagate );
    //
    // public abstract boolean isSingleExpressionAnAlternative();
    //
    // public abstract int numberOfAlternatives();
    //
    // /**
    // * @param specifier
    // * @param nextSpecifier
    // * @return a Reference for the specifier in the context of the existing
    // * scopeReference or (if the scopeReference is null), the model.
    // */
    // public abstract Reference< ? >
    // findReferenceFromScope( String specifier, Object nextSpecifier );
    //
    // /**
    // * @return whether this reference depends on its scope
    // */
    // public abstract boolean isStatic();
    //
    // /**
    // * Make this reference a template by removing its scope. The template is
    // * instantiated by giving it a scope.
    // */
    // public abstract void removeNonStaticScope();
    //
    // /**
    // * Add or replace the scope with the input Object.
    // */
    // public abstract void addScope( Object o );
    //
    // /*
    // * (non-Javadoc)
    // *
    // * @see gov.nasa.jpl.ae.event.Expression#toString()
    // */
    // public abstract String toString();
    //
    // /*
    // * (non-Javadoc)
    // *
    // * @see gov.nasa.jpl.ae.event.Expression#toString(boolean, boolean,
    // * java.util.Set)
    // */
    // public abstract String toString( boolean withHash, boolean deep,
    // Set< Object > seen );
    //
    // /*
    // * (non-Javadoc)
    // *
    // * @see gov.nasa.jpl.ae.event.Expression#toString(boolean, boolean,
    // * java.util.Set, java.util.Map)
    // */
    // public abstract String toString( boolean withHash, boolean deep,
    // Set< Object > seen,
    // Map< String, Object > otherOptions );
    //
    // public abstract void setScopeReference( Object scopeReference );
    //
    // public abstract Object getScopeReference();

    // public abstract Parameter< EObject > getModel();

    // public abstract void setModel( Parameter< EObject > model );

    // public abstract void setModel( EObject eObject );

    // /**
    // * @return the scopeReferenceString
    // */
    // public abstract String getScopeReferenceString();
    //
    // /**
    // * @param scopeReferenceString
    // * the scopeReferenceString to set
    // */
    // public abstract void setScopeReferenceString( String scopeReferenceString
    // );
    //
    // /**
    // * @param singleResultType
    // * the singleResultType to set
    // */
    // public abstract void setSingleResultType( Class< T > singleResultType );
    //
    // /**
    // * @return the singleExpression
    // */
    // public abstract Expression< T > getSingleExpression();
    //
    // /**
    // * @param singleExpression
    // * the singleExpression to set
    // */
    // public abstract void setSingleExpression( Expression< T >
    // singleExpression );

}
