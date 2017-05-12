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

import java.util.Collection;

import gov.nasa.jpl.sysml.SystemModel.Item;
import gov.nasa.jpl.sysml.SystemModel.ModelItem;
import gov.nasa.jpl.sysml.SystemModel.Operation;

/**
  * Translate from one system model of type F (the From type) to another of type T (the To type).
 */
public interface Translator< F extends SystemModel< FO, FC, FT, FP, FN, FI, FU, FR, FV, FW, FCT >,
                             FO, FC, FT, FP, FN, FI, FU, FR, FV, FW, FCT,
                             T extends SystemModel< TO, TC, TT, TP, TN, TI, TU, TR, TV, TW, TCT >,
                             TO, TC, TT, TP, TN, TI, TU, TR, TV, TW, TCT > {
    F getFromModel();
    T getToModel();
    void setFromModel(F fromModel);
    void setToModel(T toModel);

    /**
     * Import from the from-model to the to-model.
     */
    void importModel();

    /**
     * Perform an Operation on something in the target system (to-model) as
     * specified by the input arguments interpreted, if possible, for the source
     * system (F). This is similar to the op() function of {@link SystemModel}
     * but querying is performed on the fromModel, and writing is performed on
     * the toModel.
     * <p>
     * READ will CREATE will add a translation of the item specified in
     * fromModel to toModel. UPDATE will update an item in toModel based on an
     * item specified in fromModel.
     * <p>
     * Null values are interpreted as "unknown," "don't care," or
     * "not applicable." Multiple specifiers of the same kind of ModelItem are
     * interpreted as "or." For example,
     * {@code specifier = (("Fred", NAME), ("Wilma",
     * NAME))} means that the name may be either "Fred" or "Wilma."
     * <p>
     * Examples:
     * <ol>
     * <li> {@code op(READ, (OBJECT), null, ("123", IDENTIFIER), null)} returns
     * an object(s) in toModel with an ID that is the translation of the
     * fromModel ID = "123." In other words, it returns the object in toModel
     * that corresponds to the fromModel object with ID, "123."
     * <li>
     * {@code op(UPDATE, (PROPERTY), ((o1, OBJECT),(o2, OBJECT)), (("mass", NAME), ("kg", TYPE)), 1.0)}
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
     *         prohibited or inconsistent.
     */
    Collection< Object > op(Operation operation, Collection<ModelItem> itemTypes, Collection<Item> context,
        Collection<Item> specifier, FU newValue, Boolean failForMultipleItemMatches);


    /**
     * Translate the from-model object to a to-model object.
     * @param fObject
     * @return the corresponding to-model object or a new to-model object (TODO -- is it added to the model?)
     */
    TO translateObject(FO fObject);

    /**
     * Translate the from-model context to a to-model context.
     * @param fContext
     * @return the corresponding to-model context or a new to-model context (TODO -- is it added to the model?)
     */
    TC translateContext(FC fContext);

    /**
     * Translate the from-model type to a to-model type.
     * @param fType
     * @return the corresponding to-model type or a new to-model type (TODO -- is it added to the model?)
     */
    TT translateType(FT fType);

    /**
     * Translate the from-model property to a to-model property.
     * @param fProperty
     * @return the corresponding to-model property or a new to-model property (TODO -- is it added to the model?)
     */
    TP translateProperty(FP fProperty);

    /**
     * Translate the from-model name to a to-model name.
     * @param fName
     * @return the corresponding to-model name or a new to-model name (TODO -- is it added to the model?)
     */
    TN translateName(FN fName);

    /**
     * Translate the from-model identifier to a to-model identifier.
     * @param fInterface
     * @return the corresponding to-model interface or a new to-model interface (TODO -- is it added to the model?)
     */
    TI translateIdentifier(FI fIdentifier);

    /**
     * Translate the from-model value to a to-model value.
     * @param fValue
     * @return the corresponding to-model value or a new to-model value (TODO -- is it added to the model?)
     */
    TU translateValue(FU fValue);

    /**
     * Translate the from-model relationship to a to-model relationship.
     * @param fRelationship
     * @return the corresponding to-model relationship or a new to-model relationship (TODO -- is it added to the model?)
     */
    TR translateRelationship(FR fRelationship);

    /**
     * Translate the from-model version to a to-model version.
     * @param fVersion
     * @return the corresponding to-model version or a new to-model version (TODO -- is it added to the model?)
     */
    TV translateVersion(FV fVersion);

    /**
     * Translate the from-model workspace to a to-model workspace.
     * @param fWorkspace
     * @return the corresponding to-model workspace or a new to-model workspace (TODO -- is it added to the model?)
     */
    TW translateWorkspace(FW fWorkspace);

    /**
     * Translate the from-model constraint to a to-model constraint.
     * @param fConstraint
     * @return the corresponding to-model constraint or a new to-model constraint (TODO -- is it added to the model?)
     */
    TCT translateConstraint(FCT fConstraint);

}
