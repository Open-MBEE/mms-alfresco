/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory,
 *    nor the names of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.nasa.jpl.sysml;

import gov.nasa.jpl.mbee.util.MethodCall;
import gov.nasa.jpl.mbee.util.Pair;

import java.lang.Object;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

import gov.nasa.jpl.sysml.SystemModel.Item;

/**
 * A generic interface for accessing system models as simplified SysML (without UML).
 * REVIEW -- What else might this need to be compatible with other things, like CMIS, OSLC, EMF, etc.
 */
public interface SystemModel<E, C, T, P, N, I, U, R, V, W, CT> {
    /**
     * ModelItems are types of things in a model on which Operations can be
     * performed.
     * <p>
     * REVIEW -- Consider adding FUNCTION/PREDICATE, EXPRESSION, EVENT<br>
     * REVIEW -- Consider adding FUNCTION/PREDICATE, EXPRESSION, <br>
     * TODO -- add COMMENT and associated methods, and remove C from generic
     *         parameters <br>
     * TODO? -- add SITE, CATEGORY, PROJECT???, PRODUCT, SNAPSHOT, CHANGESET, CONFIGURATION, ARTIFACT <br>
     */
    enum ModelItem {
        ELEMENT, //CONTEXT, // COMMENT
        TYPE, PROPERTY, NAME, IDENTIFIER, VALUE,
        RELATIONSHIP, VERSION, WORKSPACE, CONSTRAINT, VIEW, VIEWPOINT
    }


    /**
     * Operation is a CRUD operation.  READ is the same as GET, and UPDATE is the same as SET.
     * <p>
     * REVIEW -- Consider adding CLONE/COPY, MOVE, REPAIR, EXECUTE, EVALUATE, ???<br>
     * REVIEW -- Consider adding MAP, FILTER, FOLD, FORALL, EXISTS, SORT, ???<br>
     * REVIEW -- Consider adding SUM, SUBTRACT, INTERSECT, UNITE, DIFF, ???<br>
     * REVIEW -- Consider adding SATISFY, OPTIMIZE
     */
    enum Operation { CREATE, READ, UPDATE, DELETE,
                                   GET, SET }


    /**
     * An Object with a label for the kind of model item it is.
     */
    class Item {
        public ModelItem kind;
        public Object obj;

        public Item( Object obj, ModelItem kind ) {
            this.kind = kind;
            this.obj = obj;
        }
    }



    // general functions

    /**
     * Perform an Operation on something as specified by the input arguments.
     * Null values are interpreted as "unknown," "don't care," or
     * "not applicable." Multiple specifiers of the same kind of ModelItem are
     * interpreted as "or." For example,
     * {@code specifier = (("Fred", NAME), ("Wilma",
     * NAME))} means that the name may be either "Fred" or "Wilma."
     * <p>
     * Examples:
     * <ol>
     * <li> {@code op(READ, (ELEMENT), null, ("123", IDENTIFIER), null)} returns
     * the element(s) with ID = "123."
     * <li>
     * {@code op(UPDATE, (PROPERTY), ((o1, ELEMENT),(o2, ELEMENT)), (("mass", NAME)), kg(1.0))}
     * returns a collection of the "mass" properties of o1 and o2 with values
     * updated to 1.0kg.
     * <li>
     * {@code op(CREATE, (VERSION), ((v1, VERSION)), (("v2", IDENTIFIER)), v1)}
     * creates and returns a new version "v2" that is a copy of v1 and
     * follows/branches v1.
     * </ol>
     *
     * @param operation
     *            whether to read, create, delete, or update the item
     * @param itemTypes
     *            the kind of item it may be
     * @param context
     *            the items within which the operation is performed
     * @param specifier
     *            possible characteristics of the item
     * @param newValue
     *            a new value for the item, applicable to CREATE and UPDATE
     * @param failForMultipleItemMatches
     *            if true and multiple items are identified by the specifier for
     *            a READ, UPDATE, or DELETE operation, then do not perform the
     *            operation, and return null.
     * @return the item(s) specified in a collection or null if the operation is
     *         prohibited or inconsistent. See {@link isAllowed}.
     */
    Collection< Object > op(Operation operation, Collection<ModelItem> itemTypes, Collection<Item> context,
        Collection<Item> specifier, U newValue, Boolean failForMultipleItemMatches);

    /**
     * Specifies whether it is feasible to call op() with the non-null
     * arguments. A null argument here is interpreted as "some." The newValue
     * argument is an Item instead of an Object (as in op()) in order to test
     * whether a certain kind of value can be assigned.
     * <p>
     * Examples:
     * <ol>
     * <li> {@code isAllowed(READ, (VERSION), null, null, null)} returns true iff
     * the ModelInterface supports getting versions.
     * <li> {@code isAllowed(UPDATE, (PROPERTY), ((null, TYPE)), null, null)}
     * returns true iff the ModelInterface supports updating type properties.
     * <li>
     * {@code isAllowed(CREATE, (VERSION), ((null, VERSION)), (("x y", IDENTIFIER)), (null, VERSION))}
     * returns true iff the ModelInterface supports creating and copying a
     * version from the context of a version and specifying its identifier as
     * "x y". This may return false for "x y" and true for "xy" if spaces are
     * not allowed in identifiers.
     * <li> {@code isAllowed(null, (IDENTIFIER), null, null, ("x y", VALUE))}
     * returns true iff "x y" is a legal identifier.
     * <li> {@code isAllowed(CREATE, (ELEMENT), null, null, (null, TYPE))}
     * returns true iff an element can be assigned a type.
     * </ol>
     *
     * @param operation
     *            whether to create, read/get, update/set, or delete the item
     * @param itemTypes
     *            the kind of item it may be
     * @param context
     *            the items within which the operation is performed
     * @param specifier
     *            possible characteristics of the item
     * @param newValue
     *            a new value for the item, as applicable for operation = CREATE
     *            or UPDATE
     * @param failForMultipleItemMatches
     *            if true and multiple items are identified by the specifier for
     *            a READ, UPDATE, or DELETE operation, then return false.
     * @return whether some operations of the kinds specified by the arguments
     *         are consistent, legal, and feasible.
     */
    boolean isAllowed(Operation operation, Collection<ModelItem> itemTypes, Collection<Item> context,
        Collection<Item> specifier, Item newValue, Boolean failForMultipleItemMatches);

    /**
     * Either create, read/get, update/set, or delete something as specified by
     * the input arguments. Null values are interpreted as "unknown,"
     * "don't care," or "not applicable."
     *
     * @param op
     *            whether to read/get, create, or delete the item
     * @param itemTypes
     *            the kind of item
     * @param context
     *            the objects within which the operation is performed
     * @param identifier
     *            the identifier of the item
     * @param name
     *            the name of the item
     * @param version
     *            the version of the item
     * @param failForMultipleItemMatches
     *            if true and multiple items are identified by the other
     *            arguments for a READ, UPDATE, or DELETE operation, then do not
     *            perform the operation, and return null.
     * @return the matching or resulting item(s);
     */
    Collection< Object > op(Operation operation, Collection<ModelItem> itemTypes, Collection<Item> context,
        I identifier, N name, V version, U newValue, boolean failForMultipleItemMatches);

    // More specific functions that overlap with or may help implement the general functions above.

    /**
	 * Get the model item(s) identified by or matching the input arguments. Null
	 * values are interpreted as "unknown," "don't care," or "not applicable."
	 *
	 * @param kindOfItem
	 *            the item(s) must be of one of these specified kinds
	 * @param context
	 *            the objects that collectively contain the sought item(s);
	 * @param identifier
	 * @param name
	 * @param version
	 * @return the matching items
	 */
    Collection<Object> get(Collection<ModelItem> itemTypes, Collection<Item> context, I identifier, N name, V version);
    Collection<Object> create(ModelItem item, Collection<Item> context, I identifier, N name, V version);
    Collection<Object> delete(ModelItem item, Collection<Item> context, I identifier, N name, V version);
    Collection<Object> set(ModelItem item, Collection<Item> context, I identifier, N name, V version, U newValue);
    // TODO -- update args and add update() and maybe copy()/clone();

//    /**
//     * Set a context that may be used by other SystemModel functions when
//     * otherwise unspecified.
//     *
//     * @param context
//     */
//    public void setContext( Collection< C > context );
//    /**
//     * @return the context used by this SystemModel when otherwise unspecified
//     */
//    public Collection< C > getContext();
//    /**
//     * Set a workspace context that may be used by other SystemModel functions
//     * when otherwise unspecified.
//     *
//     * @param workspace
//     */
//    public void setWorkspace( W workspace );
//    /**
//     * @return the workspace used by this SystemModel when otherwise unspecified
//     */
//    public W getWorkspace();
//    /**
//     * Set a version context that may be used by other SystemModel functions
//     * when otherwise unspecified.
//     *
//     * @param version
//     */
//    public void setVersion( V version );
//    /**
//     * @return the version used by this SystemModel when otherwise unspecified
//     */
//    public V getVersion();
//
//    // accessors for class/object/element
//    public E getElement( C context, I identifier, V version );
//    public Collection<E> getRootElements( V version );
//    public I getElementId( E element, V version );
//    public N getName( E element, V version );
//    public T getTypeOf( E element, V version );
//    public T getType( C context, N name, V version );
//    public Collection<P> getTypeProperties( T type, V version );
//    public Collection<P> getProperties( E element, V version );
//    public P getProperty( E element, N propertyName, V version );
//    public Collection<R> getRelationships( E element, V version );
//    /**
//     * @param element an element that participates in the relationships
//     * @param relationshipName
//     * @param version the version of the relationship or element; null is interpreted as most current.
//     * @return all of the element's relationships with the given name and version
//     */
//    public Collection<R> getRelationships( E element, N relationshipName, V version );
//    public Collection<E> getRelated( E element, N relationshipName, V version );

    // relationships
    boolean isDirected(R relationship);//, V version );
    Collection< E > getRelatedElements(R relationship);//, V version );
    /**
     * @param relationship
     * @param role a role in a relationship might be source, target, first, second, last, numerator, denominator, quotient, sender, receiver, . . .
     * @param version the version of the relationship
     * @return the element serving the named role in the relationship
     */
    Collection< E > getElementForRole(R relationship, N role);
    Collection< E >  getSource(R relationship);
    Collection< E >  getTarget(R relationship);

//    public V latestVersion( Collection<C> context );

    // ModelItem classes
    // ELEMENT, CONTEXT, TYPE, PROPERTY, NAME, IDENTIFIER, VALUE,
    // RELATIONSHIP, VERSION, WORKSPACE, CONSTRAINT, VIEW, VIEWPOINT
    Class< ? > getClass(ModelItem item);
    Class< E > getElementClass();
    Class< C > getContextClass();
    Class< T > getTypeClass();
    Class< P > getPropertyClass();
    Class< N > getNameClass();
    Class< I > getIdentifierClass();
    Class< U > getValueClass();
    Class< R > getRelationshipClass();
    Class< V > getVersionClass();
    Class< W > getWorkspaceClass();
    Class< CT > getConstraintClass();
    Class< ? extends E > getViewClass();
    Class< ? extends E > getViewpointClass();

    /**
     * @param o
     * @return a conversion of the java.lang.Object to a SystemModel element or null
     */
    E asElement(Object o);

    /**
     * @param object
     * @return a conversion of the java.lang.Object to a SystemModel context or null
     */
    C asContext(Object o);

    /**
     * @param object
     * @return a Collection of contexts including either the object as a context
     *         or the contexts in the object as a Collection.
     */
    Collection< C > asContextCollection(Object o);
    T asType(Object o);
    P asProperty(Object o);
    N asName(Object o);
    I asIdentifier(Object o);
    U asValue(Object o);
    R asRelationship(Object o);
    V asVersion(Object o);
    W asWorkspace(Object o);
    CT asConstraint(Object o);

    // general edit policies

    boolean idsAreWritable();
    boolean namesAreWritable();
    boolean versionsAreWritable();

//    public boolean elementsMayBeChangedForVersion( V version );
//    public boolean typesMayBeChangedForVersion( V version );
//    public boolean propertiesMayBeChangedForVersion( V version );
//    public boolean elementsMayBeCreatedForVersion( V version );
//    public boolean typesMayBeCreatedForVersion( V version );
//    public boolean propertiesMayBeCreatedForVersion( V version );
//    public boolean elementsMayBeDeletedForVersion( V version );
//    public boolean typesMayBeDeletedForVersion( V version );
//    public boolean propertiesMayBeDeletedForVersion( V version );

    // create fcns
//    // TODO
//    public E createElement( I identifier, V version );
//    public boolean setIdentifier( E element, V version );
//    public boolean setName( E element, V version );
//    public boolean setType( E element, V version );

//    // delete fcns
//    // TODO
//    E deleteElement( I identifier, V version );
//    T deleteType( E element, V version );

    CT createConstraint( C context );
    E createElement( C context );
    I createIdentifier( C context ); // depends on idsAreWritable()
    N createName( C context ); // depends on namesAreWritable()
    P createProperty( C context );
    R createRelationship( C context );
    T createType( C context );
    U createValue( C context );
    V createVersion( C context );  // depends on versionsAreWritable()
    E createView( C context );
    E createViewpoint( C context );
    W createWorkspace( C context );
    Object delete( Object object  );
    Collection< CT > getConstraint( C context, Object specifier  );
    Collection< CT > getConstraintWithElement( C context, E specifier );
    Collection< CT > getConstraintWithIdentifier( C context, I specifier );
    Collection< CT > getConstraintWithName( C context, N specifier );
    Collection< CT > getConstraintWithProperty( C context, P specifier );
    Collection< CT > getConstraintWithRelationship( C context, R specifier );
    Collection< CT > getConstraintWithType( C context, T specifier );
    Collection< CT > getConstraintWithValue( C context, U specifier );
    Collection< CT > getConstraintWithVersion( C context, V specifier );
    Collection< CT > getConstraintWithView( C context, E specifier );
    Collection< CT > getConstraintWithViewpoint( C context, E specifier );
    Collection< CT > getConstraintWithWorkspace( C context, W specifier );
    Collection< E > getElement( C context, Object specifier );
    Collection< E > getElementWithConstraint( C context, CT specifier );
    Collection< E > getElementWithIdentifier( C context, I specifier );
    Collection< E > getElementWithName( C context, N specifier );
    Collection< E > getElementWithQualifiedName( C context, N specifier );
    Collection< E > getElementWithProperty( C context, P specifier );
    Collection< E > getElementWithRelationship( C context, R specifier );
    Collection< E > getElementWithType( C context, T specifier );
    Collection< E > getElementWithValue( C context, U specifier );
    Collection< E > getElementWithVersion( C context, V specifier );
    Collection< E > getElementWithView( C context, E specifier );
    Collection< E > getElementWithViewpoint( C context, E specifier );
    Collection< E > getElementWithWorkspace( C context, W specifier );
    N getName( C context );
    String getNameString( C context );
    I getIdentifier( C context );
    Collection< P > getProperty( C context, Object specifier );
    Collection< P > getPropertyWithConstraint( C context, CT specifier );
    Collection< P > getPropertyWithElement( C context, E specifier );
    Collection< P > getPropertyWithIdentifier( C context, I specifier );
    Collection< P > getPropertyWithRelationship( C context, R specifier );
    Collection< P > getPropertyWithType( C context, T specifier );
    Collection< P > getPropertyWithValue( C context, U specifier );
    Collection< P > getPropertyWithVersion( C context, V specifier );
    Collection< P > getPropertyWithView( C context, E specifier );
    Collection< P > getPropertyWithViewpoint( C context, E specifier );
    Collection< P > getPropertyWithWorkspace( C context, W specifier );
    Collection< R > getRelationship( C context, Object specifier );
    Collection< R > getRelationshipWithConstraint( C context, CT specifier );
    Collection< R > getRelationshipWithElement( C context, E specifier );
    Collection< R > getRelationshipWithIdentifier( C context, I specifier );
    Collection< R > getRelationshipWithName( C context, N specifier );
    Collection< R > getRelationshipWithProperty( C context, P specifier );
    Collection< R > getRelationshipWithType( C context, T specifier );
    Collection< R > getRelationshipWithValue( C context, U specifier );
    Collection< R > getRelationshipWithVersion( C context, V specifier );
    Collection< R > getRelationshipWithView( C context, E specifier );
    Collection< R > getRelationshipWithViewpoint( C context, E specifier );
    Collection< R > getRelationshipWithWorkspace( C context, W specifier );
    /**
     * Get types
     * @param context
     * @param specifier
     * @return type objects in the context that match the specifier
     */
    Collection< T > getType( C context, Object specifier );
    // TODO remove this once we fix getType()
    String getTypeString( C context, Object specifier );
    Collection< T > getTypeWithConstraint( C context, CT specifier );
    Collection< T > getTypeWithElement( C context, E specifier );
    Collection< T > getTypeWithIdentifier( C context, I specifier );
    Collection< T > getTypeWithName( C context, N specifier );
    Collection< T > getTypeWithProperty( C context, P specifier );
    Collection< T > getTypeWithRelationship( C context, R specifier );
    Collection< T > getTypeWithValue( C context, U specifier );
    Collection< T > getTypeWithVersion( C context, V specifier );
    Collection< T > getTypeWithView( C context, E specifier );
    Collection< T > getTypeWithViewpoint( C context, E specifier );
    Collection< T > getTypeWithWorkspace( C context, W specifier );
    Collection< U > getValue( C context, Object specifier );
    Collection< U > getValueWithConstraint( C context, CT specifier );
    Collection< U > getValueWithElement( C context, E specifier );
    Collection< U > getValueWithIdentifier( C context, I specifier );
    Collection< U > getValueWithName( C context, N specifier );
    Collection< U > getValueWithProperty( C context, P specifier );
    Collection< U > getValueWithRelationship( C context, R specifier );
    Collection< U > getValueWithType( C context, T specifier );
    Collection< U > getValueWithVersion( C context, V specifier );
    Collection< U > getValueWithView( C context, E specifier );
    Collection< U > getValueWithViewpoint( C context, E specifier );
    Collection< U > getValueWithWorkspace( C context, W specifier );
    Collection< V > getVersion( C context );
    Collection< E > getView( C context, Object specifier );
    Collection< E > getViewpoint( C context, Object specifier );
    Collection< E > getViewpointWithConstraint( C context, CT specifier );
    Collection< E > getViewpointWithElement( C context, E specifier );
    Collection< E > getViewpointWithIdentifier( C context, I specifier );
    Collection< E > getViewpointWithName( C context, N specifier );
    Collection< E > getViewpointWithProperty( C context, P specifier );
    Collection< E > getViewpointWithRelationship( C context, R specifier );
    Collection< E > getViewpointWithType( C context, T specifier );
    Collection< E > getViewpointWithValue( C context, U specifier );
    Collection< E > getViewpointWithVersion( C context, V specifier );
    Collection< E > getViewpointWithView( C context, E specifier );
    Collection< E > getViewpointWithWorkspace( C context, W specifier );
    Collection< E > getViewWithConstraint( C context, CT specifier );
    Collection< E > getViewWithElement( C context, E specifier );
    Collection< E > getViewWithIdentifier( C context, I specifier );
    Collection< E > getViewWithName( C context, N specifier );
    Collection< E > getViewWithProperty( C context, P specifier );
    Collection< E > getViewWithRelationship( C context, R specifier );
    Collection< E > getViewWithType( C context, T specifier );
    Collection< E > getViewWithValue( C context, U specifier );
    Collection< E > getViewWithVersion( C context, V specifier );
    Collection< E > getViewWithViewpoint( C context, E specifier );
    Collection< E > getViewWithWorkspace( C context, W specifier );
    Collection< W > getWorkspace( C context );
    Object set( Object object, Object specifier, U value );


    // query functions
    /**
     * Apply the method to each of the elements and return results. Subclasses
     * implementing map() may employ utilities for functional Java provided in
     * FunctionalUtils (TODO).
     *
     * @param elements
     *            the elements, on each of which the method is applied
     * @param methodCall
     *            Java method call where the method is either of Class E or with
     *            a parameter that is or extends E (for the elements). This
     *            method could be a call to op() or a call to a custom function
     *            that includes calls to various ModelInterface methods.
     * @param indexOfElementArgument
     *            where in the list of arguments an element from the collection
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the elements are each substituted for
     *            methodCall.objectOfCall).
     * @return null if the method call returns void; otherwise, a return value
     *         for each element
     * @throws java.lang.reflect.InvocationTargetException
     */
    Collection< Object > map(Collection<E> elements, MethodCall methodCall, int indexOfElementArgument)
                                             throws java.lang.reflect.InvocationTargetException;

    /**
     * Filter out the elements for which the method does not return true.
     * Subclasses implementing filter() may employ utilities for functional Java
     * provided in FunctionalUtils (TODO).
     *
     * @param elements
     *            the elements being filtered
     * @param methodCall
     *            Java method call where the method is either of Class E or with
     *            a parameter that is or extends E (for the elements) and
     *            returning a value that can be interpreted as a Boolean by
     *            Utils.isTrue().
     * @param indexOfElementArgument
     *            where in the list of arguments an element from the collection
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the elements are each substituted for
     *            methodCall.objectOfCall).
     * @return null if the function returns void; otherwise, a return value for
     *         each element
     * @throws java.lang.reflect.InvocationTargetException
     */
    Collection< E > filter(Collection<E> elements, MethodCall methodCall, int indexOfElementArgument)
                                           throws java.lang.reflect.InvocationTargetException;

    /**
     * Check whether the method returns true for each element. Subclasses
     * implementing forAll() may employ utilities for functional Java provided
     * in FunctionalUtils (TODO).
     *
     * @param elements
     *            the elements being tested
     * @param methodCall
     *            Java method call where the method is either of Class E or with
     *            a parameter that is or extends E (for the elements) and
     *            returning a value that can be interpreted as a Boolean by
     *            Utils.isTrue().
     * @param indexOfElementArgument
     *            where in the list of arguments an element from the collection
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the elements are each substituted for
     *            methodCall.objectOfCall).
     * @return true iff all method calls can clearly be interpreted as true
     *         (consistent with Utils.isTrue());
     * @throws java.lang.reflect.InvocationTargetException
     */
    boolean forAll(Collection<E> elements, MethodCall methodCall, int indexOfElementArgument)
                                   throws java.lang.reflect.InvocationTargetException;

    /**
     * Check whether the method returns true for some element. Subclasses
     * implementing thereExists() may employ utilities for functional Java
     * provided in FunctionalUtils (TODO).
     *
     * @param elements
     *            the elements being tested
     * @param methodCall
     *            Java method call where the method is either of Class E or with
     *            a parameter that is or extends E (for the elements) and
     *            returning a value that can be interpreted as a Boolean by
     *            Utils.isTrue().
     * @param indexOfElementArgument
     *            where in the list of arguments an element from the collection
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the elements are each substituted for
     *            methodCall.objectOfCall).
     * @return true iff any method call return value can clearly be interpreted
     *         as true (consistent with Utils.isTrue());
     * @throws java.lang.reflect.InvocationTargetException
     */
    boolean thereExists(Collection<E> elements, MethodCall methodCall, int indexOfElementArgument)
                                        throws java.lang.reflect.InvocationTargetException;

    /**
     * Inductively combine the results of applying the method to each of the
     * elements and the return results for the prior element. Subclasses
     * implementing fold() may employ utilities for functional Java provided in
     * FunctionalUtils (TODO).
     * <p>
     * For example, fold() is used below to sum an array of numbers.<br>
     * {@code int plus(int a, int b) ( return a+b; )} <br>
     * {@code int[] array = new int[] ( 2, 5, 6, 5 );}<br>
     * {@code int result = fold(Arrays.asList(array), 0.0, ClassUtils.getMethodForName(this.getClass(), "plus"), 0, 1, null); // result = 18}
     *
     * @param elements
     * @param initialValue
     *            an initial value to act as the first argument to first
     *            invocation of the method.
     * @param methodCall
     *            Java method call where the method is either of Class E or with
     *            a parameter that is or extends E (for the elements). Including
     *            the objectOfCall with the arguments if not static, the method
     *            must have two or more parameters, one of which can be assigned
     *            the prior result, which should have the same type as the
     *            method's return type, and another that is an E or extends E
     *            (element).
     * @param indexOfElementArgument
     *            where in the list of arguments an element from the collection
     *            is substituted (1 to total number of args) or 0 to indicate
     *            that the elements are each substituted for
     *            methodCall.objectOfCall.
     * @param indexOfPriorResultArgument
     *            where in the list of arguments the prior result value is
     *            substituted (1 to total number of args or 0 to indicate that
     *            the elements are each substituted for methodCall.objectOfCall).
     * @return the result of calling the method on the last element after calling
     *         the method on each prior element (in order), passing the prior
     *         return value into the call on each element.
     * @throws java.lang.reflect.InvocationTargetException
     */
    Object fold(Collection<E> elements, Object initialValue, MethodCall methodCall, int indexOfElementArgument,
        int indexOfPriorResultArgument)
                                throws java.lang.reflect.InvocationTargetException;

    /**
     * Apply the method to each of the elements and return results. Subclasses
     * implementing sort() may employ utilities for functional Java provided in
     * FunctionalUtils (TODO).
     *
     * @param elements to be sorted
     * @param comparator specifies precedence relation on a pair of return values
     * @param methodCall
     *            Java method call where the method is either of Class E or with
     *            a parameter that is or extends E (for the elements). This
     *            method could be a call to op() or a call to a custom function
     *            that includes calls to various ModelInterface methods.
     * @param indexOfElementArgument
     *            where in the list of arguments an element from the collection
     *            is substituted (1 to total number of args or 0 to indicate
     *            that the elements are each substituted for
     *            methodCall.objectOfCall).
     * @return the input elements in a new Collection sorted according to the method and comparator
     * @throws java.lang.reflect.InvocationTargetException
     */
    Collection< E > sort(Collection<E> elements, Comparator<?> comparator, MethodCall methodCall,
        int indexOfElementArgument)
                                         throws java.lang.reflect.InvocationTargetException;

    // support for problem solving

    // example problem solving session
    //
    // f = getElementWithName( "MyFolder" ).iterator().next();
    // x = getElementWithName( f, "x" ).iterator().next();
    // y = getElementWithName( f, "y" ).iterator().next();
    // c = createConstraint( f );
    // fixConstraintViolations(c);
    // System.out.println(y.toString());


//    // problem solving session
//    // TODO -- replace sessions with model refs
//    // TODO -- otherwise, create a Session class that would include these session functions, which would return Sessions.
//	/**
//	 * Create a new problem solving session.
//	 * <p>
//	 * TODO -- REVIEW -- this is similar to a workspace where hypothetical model
//	 * changes can be made -- this should be implemented for the model as a
//	 * whole, and problem solving sessions will be unnecessary.
//	 *
//	 * @param suggestedSessionId
//	 * @return the id for a new problem solving session, the one suggested if
//	 *         possible
//	 */
//    public I createNewSolverSession(I suggestedSessionId);
//    public I copySolverSession( I idOfSessionToCopy );
//    public I switchSolverSession( I idOfSessionToWhichToSwitch );
//    public I deleteSolverSession( I idOfSessionToWhichToSwitch );

//    // Constraint CRUD
//    public Constraint addConstraint( CE constraintElement, V version, W workspace );
//    public Constraint addDomainConstraint( CE constraintElement, V version, Set<U> valueDomainSet, W workspace );
//    public Constraint addDomainConstraint( CE constraintElement, V version, Pair<U,U> valueDomainRange, W workspace );
//    public Constraint relaxDomain( CE constraintElement, V version, Set<U> valueDomainSet, W workspace );
//    public Constraint relaxDomain( CE constraintElement, V version, Pair<U,U> valueDomainRange, W workspace );
//    public Collection<CE> getConstraintElementsOfElement( E element, V version, W workspace );
//    public Collection<CE> getConstraintElementsOfContext( C context );
//    public Collection<Constraint> getConstraintsOfElement( E element, V version, W workspace );
//    public Collection<Constraint> getConstraintsOfContext( C context );
//    public void setOptimizationFunction( Method method, Object... arguments ); // REVIEW -- should these be elements?
//    public Collection<CE> getViolatedConstraintElementsOfElement( E element, V version );
//    public Collection<CE> getViolatedConstraintElementsOfContext( C context );
//    public Collection<Constraint> getViolatedConstraintsOfElement( E element, V version );
//    public Collection<Constraint> getViolatedConstraintsOfContext( C context );
//    public Number getScore();

    // Constraint CRUD
    CT getDomainConstraint(E element, V version, W workspace);
    // TODO -- easier i/f for adding constraint that
    void addConstraint(CT constraint, V version, W workspace);
    void addDomainConstraint(CT constraint, V version, Set<U> valueDomainSet, W workspace);
    void addDomainConstraint(CT constraint, V version, Pair<U, U> valueDomainRange, W workspace);
    void relaxDomain(CT constraint, V version, Set<U> valueDomainSet, W workspace);
    void relaxDomain(CT constraint, V version, Pair<U, U> valueDomainRange, W workspace);
    Collection<CT> getConstraintsOfElement(E element, V version, W workspace);
    //public Collection<CT> getConstraintsOfContext( C context );
    Collection<CT> getViolatedConstraintsOfElement(E element, V version);
    //public Collection<CT> getViolatedConstraintsOfContext( C context );
    void setOptimizationFunction(Method method, Object... arguments); // REVIEW -- should these be elements? should the function be an interface type (add F to ModelItem)?
    Number getScore();
    //public <B> Number getScore(B objective); // TODO -- add B to class parameters?
    // TODO -- add other functions? like for delete? update?

    // TODO -- invoke solver/fix
    boolean fixConstraintViolations(E element, V version);




}
