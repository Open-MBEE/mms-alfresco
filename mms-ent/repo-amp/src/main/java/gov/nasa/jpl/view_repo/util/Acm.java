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

package gov.nasa.jpl.view_repo.util;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;


/**
 * Simple static class for keeping track of Alfresco Content Model types and JSON mappings
 * @author cinyoung
 *
 */
public class Acm {
	static Logger logger = Logger.getLogger(Acm.class);

    // Additions with api.raml file:
    public static final String SYSMLID = Sjm.SYSMLID;
    public static final String SPECIFICATION = "specification";
    public static final String PARAMETERS = "parameters";
    public static final String EXPRESSION = "expression";
    public static final String ELEMENT = "element";
    public static final String VERSION = "version";
    public static final String DIRECTION = "direction";
    public static final String DEFAULT_VALUE = "defaultValue";
    public static final String JSON_SPECIALIZATION = "specialization";

    // JSON types
    public static final String JSON_COMMENT = "Comment";
    public static final String JSON_CONSTRAINT = "Constraint";
    public static final String JSON_CONSTRAINT_SPECIFICATION = SPECIFICATION; // Used to be "constraintSpecification";
    public static final String JSON_CONFORM = "Conform";
    public static final String JSON_DEPENDENCY = "Dependency";
    public static final String JSON_DIRECTED_RELATIONSHIP = "DirectedRelationship";
    public static final String JSON_ELEMENT = "Element";
    public static final String JSON_EXPOSE = "Expose";
    public static final String JSON_GENERALIZATION = "Generalization";
    public static final String JSON_PACKAGE = "Package";
    public static final String JSON_PROPERTY = "Property";
    public static final String JSON_VIEWPOINT = "Viewpoint";
    public static final String JSON_IS_DERIVED = "isDerived";
    public static final String JSON_IS_SLOT = "isSlot";
    public static final String JSON_DOCUMENTATION = "documentation";
    public static final String JSON_ID = SYSMLID; // Used to be "id"
    public static final String JSON_NAME = "name";
    public static final String JSON_SOURCE = "source";
    public static final String JSON_TARGET = "target";
    public static final String JSON_VALUE_TYPE = "valueType";
    public static final String JSON_VALUE = "value";
    public static final String JSON_BODY = "body";
    public static final String JSON_EXPRESSION_BODY = "expressionBody";
    public static final String JSON_ANNOTATED_ELEMENTS = "annotatedElements";
    public static String JSON_PROJECT_VERSION = "projectVersion";

    public static final String JSON_ALLOWED_ELEMENTS = "allowedElements";
    public static final String JSON_CHILDREN_VIEWS = "childrenViews";
    public static final String JSON_CONTAINS = "contains";
    public static final String JSON_DISPLAYED_ELEMENTS = "displayedElements";
    public static final String JSON_NO_SECTIONS = "noSections";
    public static final String JSON_VIEW_2_VIEW = "view2view";
    public static final String JSON_PRODUCT = "Product";
    public static final String JSON_VIEW = "View";

    public static final String JSON_BOOLEAN = "boolean";
    public static final String JSON_DOUBLE = "double";
    public static final String JSON_INTEGER = "integer";
    public static final String JSON_REAL = "real";
    public static final String JSON_NATURAL_VALUE = "naturalValue";
    public static final String JSON_STRING = "string";

    public static final String JSON_TYPE = "type";
    public static final String JSON_OWNER = Sjm.OWNERID;
    public static final String JSON_LAST_MODIFIED = "modified";
    public static final String JSON_AUTHOR = "author";
    public static final String JSON_PROPERTY_TYPE = "propertyType";

    // Value spec additions
    public static final String JSON_VALUE_SPECIFICATION = "ValueSpecification";
    public static final String JSON_VALUE_EXPRESSION = "valueExpression";
    public static final String JSON_DURATION = "Duration";
    public static final String JSON_DURATION_INTERVAL = "DurationInterval";
    public static final String JSON_DURATION_MAX = "durationMax";
    public static final String JSON_DURATION_MIN = "durationMin";
    public static final String JSON_ELEMENT_VALUE = "ElementValue";
    public static final String JSON_ELEMENT_VALUE_ELEMENT = ELEMENT; // Used to be "elementValueOfElement"
    public static final String JSON_EXPRESSION = "Expression";
    public static final String JSON_OPERAND = "operand";
    public static final String JSON_INSTANCE_VALUE = "InstanceValue";
    public static final String JSON_INSTANCE = "instance";
    public static final String JSON_INTERVAL = "interval";
    public static final String JSON_LITERAL_BOOLEAN = "LiteralBoolean";
    public static final String JSON_LITERAL_INTEGER = "LiteralInteger";
    public static final String JSON_LITERAL_NULL = "LiteralNull";
    public static final String JSON_LITERAL_REAL = "LiteralReal";
    public static final String JSON_LITERAL_UNLIMITED_NATURAL = "LiteralUnlimitedNatural";
    public static final String JSON_LITERAL_STRING = "LiteralString";
    public static final String JSON_OPAQUE_EXPRESSION = "OpaqueExpression";
    public static final String JSON_STRING_EXPRESSION = "StringExpression";
    public static final String JSON_TIME_EXPRESSION = "TimeExpression";
    public static final String JSON_TIME_INTERVAL = "TimeInterval";
    public static final String JSON_TIME_INTERVAL_MAX = "timeIntervalMax";
    public static final String JSON_TIME_INTERVAL_MIN = "timeIntervalMin";
    public static final String JSON_OPERATION = "Operation";
    public static final String JSON_OPERATION_PARAMETER = PARAMETERS; // Used to be "operationParameter"
    public static final String JSON_INSTANCE_SPECIFICATION = "InstanceSpecification";
    public static final String JSON_INSTANCE_SPECIFICATION_SPECIFICATION = "instanceSpecificationSpecification";
    public static final String JSON_PARAMETER = "Parameter";
    public static final String JSON_PARAMETER_DIRECTION = DIRECTION; // Used to be "parameterDirection"
    public static final String JSON_PARAMETER_DEFAULT_VALUE = DEFAULT_VALUE; // Used to be "parameterDefaultValue"
    public static final String JSON_PARAMETER_TYPE = "parameterType";
    public static final String JSON_OPERATION_EXPRESSION = EXPRESSION; // Used to be "operationExpression"
    public static final String JSON_METHOD = "method";
    public static final String JSON_CONNECTOR = "Connector";
    public static final String JSON_CONNECTOR_ROLE = "roles";

    public static final String JSON_READ = "read";

    // Sysml 2.0 additions:
    public static final String JSON_CONTENTS = "contents";
    public static final String JSON_SOURCE_PATH = "sourcePath";
    public static final String JSON_TARGET_PATH = "targetPath";
    public static final String JSON_CONNECTOR_KIND = "connectorKind";
    public static final String JSON_CONNECTOR_VALUE = "connectorValue";
    public static final String JSON_ASSOCIATION = "Association";
    public static final String JSON_OWNED_END = "ownedEnd";
    public static final String JSON_AGGREGATION = "aggregation";
    public static final String JSON_SOURCE_AGGREGATION = "sourceAggregation";
    public static final String JSON_TARGET_AGGREGATION = "targetAggregation";
    public static final String JSON_CHARACTERIZES = "Characterizes";
    public static final String JSON_SUCCESSION = "Succession";
    public static final String JSON_BINDING = "Binding";
    public static final String JSON_LITERAL_SET = "LiteralSet";
    public static final String JSON_SET = "set";
    public static final String JSON_SET_OPERAND = "setOperand";
    public static final String JSON_MAGICDRAW_DATA = "MagicDrawData";
    public static final String JSON_MD_DATA = "mdData";
    public static final String JSON_LOWER = "lower";
    public static final String JSON_UPPER = "upper";
    public static final String JSON_MULTIPLICITY_MIN = "multiplicityMin";
    public static final String JSON_MULTIPLICITY_MAX = "multiplicityMax";
    public static final String JSON_REDEFINES = "redefines";
    public static final String JSON_TARGET_LOWER = "targetLower";
    public static final String JSON_TARGET_UPPER = "targetUpper";
    public static final String JSON_SOURCE_LOWER = "sourceLower";
    public static final String JSON_SOURCE_UPPER = "sourceUpper";
    public static final String JSON_CONNECTOR_TYPE = "connectorType";

    // Site packages additions:
    public static final String JSON_IS_SITE = "isSite";

    public static final String JSON_UNTYPED = "Untyped";
    public static final String JSON_CLASSIFIER = "classifier";
    public static final String JSON_SLOTS = "slots";

    // metatype information
    public static final String JSON_IS_METATYPE = "isMetatype";
    public static final String JSON_METATYPES = "metatypes";
    public static final String JSON_APPLIED_METATYPES = "appliedMetatypes";

    public static final String JSON_OWNED_ATTRIBUTE = "ownedAttribute";

    // ACM types with the different name spaces
    public static final String SYSML = "sysml:";
    public static final String VIEW = "view2:";
    public static final String CM = "cm:";
    public static final String EMS = "ems:";

    public static final String ACM_COMMENT = SYSML + JSON_COMMENT;
    public static final String ACM_CONSTRAINT = SYSML + JSON_CONSTRAINT;
    public static final String ACM_CONSTRAINT_SPECIFICATION = SYSML + "constraintSpecification";
    public static final String ACM_CONFORM = SYSML + JSON_CONFORM;
    public static final String ACM_DEPENDENCY = SYSML + JSON_DEPENDENCY;
    public static final String ACM_DIRECTED_RELATIONSHIP = SYSML + JSON_DIRECTED_RELATIONSHIP;
    public static final String ACM_ELEMENT = SYSML + JSON_ELEMENT;
    public static final String ACM_EXPOSE = SYSML + JSON_EXPOSE;
    public static final String ACM_GENERALIZATION = SYSML + JSON_GENERALIZATION;
    public static final String ACM_PACKAGE = SYSML + JSON_PACKAGE;
    public static final String ACM_PROPERTY = SYSML + JSON_PROPERTY;
    public static final String ACM_VIEWPOINT = SYSML + JSON_VIEWPOINT;
    public static final String ACM_IS_DERIVED = SYSML + JSON_IS_DERIVED;
    public static final String ACM_IS_SLOT = SYSML + JSON_IS_SLOT;
    public static final String ACM_DOCUMENTATION = SYSML + JSON_DOCUMENTATION;
    public static final String ACM_IDENTIFIABLE = SYSML + "Identifiable";
    public static final String ACM_ID = SYSML + "id";
    public static final String ACM_NAME = SYSML + JSON_NAME;
    public static final String ACM_SOURCE = SYSML + JSON_SOURCE;
    public static final String ACM_TARGET = SYSML + JSON_TARGET;
    public static final String ACM_VALUE_TYPE = SYSML + JSON_VALUE_TYPE;
    public static final String ACM_VALUE = SYSML + JSON_VALUE;
    public static final String ACM_TYPE = SYSML + JSON_TYPE;
    public static final String ACM_REIFIED_CONTAINMENT = SYSML + "reifiedContainment";
    public static final String ACM_BODY = SYSML + JSON_BODY;
    public static final String ACM_EXPRESSION_BODY = SYSML + JSON_EXPRESSION_BODY;
    public static final String ACM_ANNOTATED_ELEMENTS = SYSML + JSON_ANNOTATED_ELEMENTS;
    public static String ACM_PROJECT_VERSION = SYSML + JSON_PROJECT_VERSION;

    public static final String ACM_ALLOWED_ELEMENTS = VIEW + JSON_ALLOWED_ELEMENTS;
    public static final String ACM_CHILDREN_VIEWS = VIEW + JSON_CHILDREN_VIEWS;
    public static final String ACM_CONTAINS = VIEW + JSON_CONTAINS;
    public static final String ACM_DISPLAYED_ELEMENTS = VIEW + JSON_DISPLAYED_ELEMENTS;
    public static final String ACM_NO_SECTIONS = VIEW + JSON_NO_SECTIONS;
    public static final String ACM_VIEW_2_VIEW = VIEW + JSON_VIEW_2_VIEW;
    public static final String ACM_PRODUCT = VIEW + JSON_PRODUCT;
    public static final String ACM_VIEW = SYSML + JSON_VIEW; // yes, this starts with sysml instead of view2

    public static final String ACM_BOOLEAN = SYSML + JSON_BOOLEAN;
    public static final String ACM_DOUBLE = SYSML + JSON_DOUBLE;
    public static final String ACM_INTEGER = SYSML + JSON_INTEGER;
    public static final String ACM_REAL = SYSML + JSON_REAL;
    public static final String ACM_NATURAL_VALUE = SYSML + JSON_NATURAL_VALUE;
    public static final String ACM_STRING = SYSML + JSON_STRING;

    public static final String ACM_PROPERTY_TYPE = SYSML + JSON_PROPERTY_TYPE;

    // Value spec additions
    public static final String ACM_VALUE_SPECIFICATION = SYSML + JSON_VALUE_SPECIFICATION;
    public static final String ACM_VALUE_EXPRESSION = SYSML + JSON_VALUE_EXPRESSION;
    public static final String ACM_DURATION = SYSML + JSON_DURATION;
    public static final String ACM_DURATION_INTERVAL = SYSML + JSON_DURATION_INTERVAL;
    public static final String ACM_DURATION_MAX = SYSML + "durationMax";
    public static final String ACM_DURATION_MIN = SYSML + "durationMin";
    public static final String ACM_ELEMENT_VALUE = SYSML + JSON_ELEMENT_VALUE;
    public static final String ACM_ELEMENT_VALUE_ELEMENT = SYSML + "elementValueOfElement";
    public static final String ACM_EXPRESSION = SYSML + JSON_EXPRESSION;
    public static final String ACM_OPERAND = SYSML + JSON_OPERAND;
    public static final String ACM_INSTANCE_VALUE = SYSML + JSON_INSTANCE_VALUE;
    public static final String ACM_INSTANCE = SYSML + JSON_INSTANCE;
    public static final String ACM_INTERVAL = SYSML + JSON_INTERVAL;
    public static final String ACM_LITERAL_BOOLEAN = SYSML + JSON_LITERAL_BOOLEAN;
    public static final String ACM_LITERAL_INTEGER = SYSML + JSON_LITERAL_INTEGER;
    public static final String ACM_LITERAL_NULL = SYSML + JSON_LITERAL_NULL;
    public static final String ACM_LITERAL_REAL = SYSML + JSON_LITERAL_REAL;
    public static final String ACM_LITERAL_UNLIMITED_NATURAL = SYSML + JSON_LITERAL_UNLIMITED_NATURAL;
    public static final String ACM_LITERAL_STRING = SYSML + JSON_LITERAL_STRING;
    public static final String ACM_OPAQUE_EXPRESSION = SYSML + JSON_OPAQUE_EXPRESSION;
    public static final String ACM_STRING_EXPRESSION = SYSML + JSON_STRING_EXPRESSION;
    public static final String ACM_TIME_EXPRESSION = SYSML + JSON_TIME_EXPRESSION;
    public static final String ACM_TIME_INTERVAL = SYSML + JSON_TIME_INTERVAL;
    public static final String ACM_TIME_INTERVAL_MAX = SYSML + "timeIntervalMax";
    public static final String ACM_TIME_INTERVAL_MIN = SYSML + "timeIntervalMin";
    public static final String ACM_OPERATION = SYSML + JSON_OPERATION;
    public static final String ACM_OPERATION_PARAMETER = SYSML + "operationParameter";
    public static final String ACM_INSTANCE_SPECIFICATION = SYSML + JSON_INSTANCE_SPECIFICATION;
    public static final String ACM_INSTANCE_SPECIFICATION_SPECIFICATION = SYSML + "instanceSpecificationSpecification";
    public static final String ACM_PARAMETER = SYSML + JSON_PARAMETER;
    public static final String ACM_PARAMETER_DIRECTION = SYSML + "parameterDirection";
    public static final String ACM_PARAMETER_DEFAULT_VALUE = SYSML + "parameterDefaultValue";
    public static final String ACM_PARAMETER_TYPE = SYSML + JSON_PARAMETER_TYPE;
    public static final String ACM_OPERATION_EXPRESSION = SYSML + "operationExpression";
    public static final String ACM_METHOD = SYSML + JSON_METHOD;
    public static final String ACM_CONNECTOR = SYSML + JSON_CONNECTOR;
    public static final String ACM_CONNECTOR_ROLE = SYSML + "roles";

    // relationship property aspect names
    public static final String ACM_RELATIONSHIPS_AS_SOURCE = "sysml:relationshipsAsSource";
    public static final String ACM_RELATIONSHIPS_AS_TARGET = "sysml:relationshipsAsTarget";
    public static final String ACM_UNDIRECTED_RELATIONSHIPS = "sysml:undirectedRelationships";
    // properties for above aspects
    public static final String ACM_REL_AS_SOURCE = "sysml:relAsSource";
    public static final String ACM_REL_AS_TARGET = "sysml:relAsTarget";
    public static final String ACM_UNDIRECTED_REL = "sysml:undirectedRel";

    // Additions with api.raml file:
    public static final String ACM_SPECIALIZATION = SYSML + JSON_SPECIALIZATION;

    public static String ACM_ELEMENT_FOLDER = SYSML + "ElementFolder";
    public static String ACM_PROJECT = SYSML + "Project";

    public static String ACM_LAST_MODIFIED = CM + "modified";
    public static String ACM_AUTHOR = CM + "modifier";

    public static String CM_NAME = CM + "name";
    public static String CM_TITLE = CM + "title";

    // Sysml 2.0 additions:
    public static final String ACM_CONTENTS = SYSML + JSON_CONTENTS;
    public static final String ACM_SOURCE_PATH = SYSML + JSON_SOURCE_PATH;
    public static final String ACM_TARGET_PATH = SYSML + JSON_TARGET_PATH;
    public static final String ACM_CONNECTOR_KIND = SYSML + JSON_CONNECTOR_KIND;
    public static final String ACM_CONNECTOR_VALUE = SYSML + JSON_CONNECTOR_VALUE;
    public static final String ACM_ASSOCIATION = SYSML + JSON_ASSOCIATION;
    public static final String ACM_OWNED_END = SYSML + JSON_OWNED_END;
    public static final String ACM_AGGREGATION = SYSML + JSON_AGGREGATION;
    public static final String ACM_SOURCE_AGGREGATION = SYSML + JSON_SOURCE_AGGREGATION;
    public static final String ACM_TARGET_AGGREGATION = SYSML + JSON_TARGET_AGGREGATION;
    public static final String ACM_CHARACTERIZES = SYSML + JSON_CHARACTERIZES;
    public static final String ACM_SUCCESSION = SYSML + JSON_SUCCESSION;
    public static final String ACM_BINDING = SYSML + JSON_BINDING;
    public static final String ACM_LITERAL_SET = SYSML + JSON_LITERAL_SET;
    public static final String ACM_SET = SYSML + JSON_SET;
    public static final String ACM_SET_OPERAND = SYSML + JSON_SET_OPERAND;
    public static final String ACM_MAGICDRAW_DATA = EMS + JSON_MAGICDRAW_DATA;
    public static final String ACM_MD_DATA = EMS + JSON_MD_DATA;
    public static final String ACM_LOWER = SYSML + JSON_LOWER;
    public static final String ACM_UPPER = SYSML + JSON_UPPER;
    public static final String ACM_MULTIPLICITY_MIN = SYSML + JSON_MULTIPLICITY_MIN;
    public static final String ACM_MULTIPLICITY_MAX = SYSML + JSON_MULTIPLICITY_MAX;
    public static final String ACM_REDEFINES = SYSML + JSON_REDEFINES;
    public static final String ACM_TARGET_LOWER = SYSML + JSON_TARGET_LOWER;
    public static final String ACM_TARGET_UPPER = SYSML + JSON_TARGET_UPPER;
    public static final String ACM_SOURCE_LOWER = SYSML + JSON_SOURCE_LOWER;
    public static final String ACM_SOURCE_UPPER = SYSML + JSON_SOURCE_UPPER;
    public static final String ACM_CONNECTOR_TYPE = SYSML + JSON_CONNECTOR_TYPE;

    // Site packages additions:
    public static final String ACM_SITE = EMS + "Site";
    public static final String ACM_SITE_PARENT = EMS + "siteParent";
    public static final String ACM_SITE_CHILDREN = EMS + "siteChildren";
    public static final String ACM_SITE_PACKAGE = EMS + "sitePackage";
    public static final String ACM_SITE_SITE = EMS + "siteSite";
    public static final String ACM_SITE_CHARACTERIZATION = EMS + "SiteCharacterization";
    public static final String ACM_IS_SITE = SYSML + JSON_IS_SITE;

    public static final String ACM_UNTYPED = SYSML + JSON_UNTYPED;
    public static final String ACM_UNTYPED_PROPERTY = SYSML + "untyped";
    public static final String ACM_METACLASS = SYSML + "Metaclass";
    public static final String ACM_METACLASS_PROPERTY = SYSML + "metaclass";
    public static final String ACM_STEREOTYPE = SYSML + "Stereotype";
    public static final String ACM_STEREOTYPE_PROPERTY = SYSML + "stereotype";
    public static final String ACM_DELETED = "ems:Deleted";
    public static final String ACM_DELETED_PROPERTY = "ems:deleted";
    //public static final String ACM_LITERAL_NULL = SYSML + "LiteralNull";
    public static final String ACM_LITERAL_NULL_PROPERTY = SYSML + "literalNull";
    //public static final String ACM_CHARACTERIZES = SYSML + "";;
    public static final String ACM_CHARACTERIZES_PROPERTY = SYSML + "characterizes";
    //public static final String ACM_SUCCESSION = SYSML + "";;
    public static final String ACM_SUCCESSION_PROPERTY = SYSML + "succession";
    //public static final String ACM_BINDING = SYSML + "";;
    public static final String ACM_BINDING_PROPERTY = SYSML + "binding";
    public static final String ACM_DEPENDENCY_PROPERTY = SYSML + "dependency";
    public static final String ACM_EXPOSE_PROPERTY = SYSML + "expose";
    public static final String ACM_CONFORM_PROPERTY = SYSML + "conform";
    public static final String ACM_GENERALIZATION_PROPERTY = SYSML + "generalization";

    public static final String ACM_CLASSIFIER = SYSML + JSON_CLASSIFIER;
    public static final String ACM_SLOTS = SYSML + JSON_SLOTS;

    public static final String ACM_IS_METATYPE = SYSML + JSON_IS_METATYPE;
    public static final String ACM_METATYPES = SYSML + JSON_METATYPES;
    public static final String ACM_APPLIED_METATYPES = SYSML + JSON_APPLIED_METATYPES;
    public static final String ACM_HAS_METATYPE = SYSML + "HasMetatype";


    public static final String ACM_OWNED_ATTRIBUTE = SYSML + JSON_OWNED_ATTRIBUTE;
    public static final String ACM_OWNS_ATTRIBUTE = SYSML + "OwnsAttribute";

    /**
     *  JSON to Alfresco Content Model mapping
     */
    protected static Map<String, String> JSON2ACM = null; //getJSON2ACM();
    /**
     *  Alfresco Content Model 2 JSON types
     */
    protected static Map<String, String> ACM2JSON = null; //getACM2JSON();

    {init();}

        //private static final long serialVersionUID = -5467934440503910163L;
    public static Map<String, String> getACM2JSON() {
        if ( ACM2JSON == null || ACM2JSON.size() <= 0 ) {
            init();
        }
        return ACM2JSON;
    }
    public static Map<String, String> getJSON2ACM() {
        if ( JSON2ACM == null || JSON2ACM.size() <= 0 ) {
            init();
        }
        return JSON2ACM;
    }
    public static void init() {
        try {
            ACM2JSON = new HashMap<String, String>();
            JSON2ACM = new HashMap<String, String>();
            for ( Field f : Acm.class.getDeclaredFields() ) {
                if ( f.getName().startsWith( "JSON_" ) ) {
                    String acmName = f.getName().replace( "JSON", "ACM" );
                    try {
                        Field f2 = Acm.class.getField( acmName );

                        if ( f2 != null ) {
                            String jsonVal = (String)f.get(null);
                            String acmVal = (String)f2.get(null);
//                            if ( !f.getName().equals("JSON_VALUE") ) {
                                JSON2ACM.put( jsonVal, acmVal);
                                if ( !f.getName().equals("JSON_VALUE_TYPE") ) {
                                    // this is parsed differently so don't include it
                                    ACM2JSON.put( acmVal, jsonVal);
                                }
//                            }
                        }
                    } catch (Throwable t) {
                        if ( t instanceof NoSuchFieldException ) {
                            if (logger.isDebugEnabled()) logger.debug( t.getLocalizedMessage() );
                        } else {
                            t.printStackTrace();
                        }
                    }
                }
            }
        } catch ( Throwable t ) {
            t.printStackTrace();
        }
    }

    /**
     * Properties that are JSONArrays rather than primitive types, so parsing is different
     */
    protected static final Set<String> JSON_ARRAYS = new HashSet<String>() {
         private static final long serialVersionUID = -2080928480362524333L;
        {
            add(JSON_DISPLAYED_ELEMENTS);
            add(JSON_ALLOWED_ELEMENTS);
            add(JSON_CHILDREN_VIEWS);
            add(JSON_CONTAINS);
            add(JSON_VIEW_2_VIEW);
            add(JSON_NO_SECTIONS);
            add(JSON_CONNECTOR_ROLE);
            add(JSON_OWNED_ATTRIBUTE);
            add(JSON_REDEFINES);
//            add(JSON_ANNOTATED_ELEMENTS);
        }
    };

    /**
     * Properties that are reference Elements rather than primitive types, so parsing is different
     */
    protected static final Set<String> JSON_NODEREFS = new HashSet<String>() {
        private static final long serialVersionUID = -6616374715310786125L;
        {
           add(JSON_VALUE);
           add(JSON_VALUE_EXPRESSION);
           add(JSON_LOWER);
           add(JSON_UPPER);
           add(JSON_DURATION_MAX);
           add(JSON_DURATION_MIN);
           add(JSON_ELEMENT_VALUE_ELEMENT);
           add(JSON_OPERAND);
           add(JSON_INSTANCE);
           add(JSON_TIME_INTERVAL_MAX);
           add(JSON_TIME_INTERVAL_MIN);
           add(JSON_OPERATION_PARAMETER);
           add(JSON_INSTANCE_SPECIFICATION_SPECIFICATION);
           add(JSON_CONSTRAINT_SPECIFICATION);
           add(JSON_PARAMETER_DEFAULT_VALUE);
           add(JSON_OPERATION_EXPRESSION);
           add(JSON_METHOD);
           add(JSON_CONNECTOR_ROLE);
           add(JSON_OWNED_ATTRIBUTE);
           add(JSON_REDEFINES);
//           add(JSON_ANNOTATED_ELEMENTS);
       }
   };


    /**
     * Properties that are always serialized in JSON
     */
    protected static final Set<String> COMMON_JSON = new HashSet<String>() {
        private static final long serialVersionUID = 8715041115029041344L;
        {
            add(JSON_ID);
            add(JSON_AUTHOR);
            add(JSON_LAST_MODIFIED);
        }
    };

    /**
     * Properties that are serialized when requesting Comments
     */
    public static final Set<String> COMMENT_JSON = new HashSet<String>() {
        private static final long serialVersionUID = -6102902765527909734L;
        {
            add(JSON_BODY);
            addAll(COMMON_JSON);
        }
    };

    /**
     * Properties that are serialized when requesting Products
     */
    protected static final Set<String> PRODUCT_JSON = new HashSet<String>() {
        private static final long serialVersionUID = 3335972461663141541L;
        {
            add(JSON_VIEW_2_VIEW);
            add(JSON_NO_SECTIONS);

            addAll(COMMON_JSON);
        }
    };

    /**
     * Properties that are serialized when requesting Views
     */
    protected static final Set<String> VIEW_JSON = new HashSet<String>() {
        private static final long serialVersionUID = -2080928480362524333L;
        {
            add(JSON_DISPLAYED_ELEMENTS);
            add(JSON_ALLOWED_ELEMENTS);
            add(JSON_CHILDREN_VIEWS);
            add(JSON_CONTAINS);
            addAll(COMMON_JSON);
        }
    };

    protected static final Set<String> MISC_PROPS_JSON = new HashSet<String>() {
        private static final long serialVersionUID = -2069995088621697991L;
        {
            add(JSON_CONSTRAINT_SPECIFICATION);
            add(JSON_EXPRESSION_BODY);
            add(JSON_PROJECT_VERSION);
            add(JSON_BOOLEAN);
            add(JSON_DOUBLE);
            add(JSON_INTEGER);
            add(JSON_REAL);
            add(JSON_NATURAL_VALUE);
            add(JSON_STRING);
            add(JSON_VALUE_EXPRESSION);
            add(JSON_DURATION_MAX);
            add(JSON_DURATION_MIN);
            add(JSON_ELEMENT_VALUE_ELEMENT);
            add(JSON_OPERAND);
            add(JSON_INSTANCE);
            add(JSON_INTERVAL);
            add(JSON_TIME_INTERVAL_MAX);
            add(JSON_TIME_INTERVAL_MIN);
            add(JSON_OPERATION_PARAMETER);
            add(JSON_INSTANCE_SPECIFICATION_SPECIFICATION);
            add(JSON_PARAMETER_DIRECTION);
            add(JSON_PARAMETER_DEFAULT_VALUE);
            add(JSON_OPERATION_EXPRESSION);
            add(JSON_METHOD);
            add(JSON_CONNECTOR_ROLE);
            // TODO -- Not everything is here (ex, aggregation), but MISC_PROPS_JSON is not used outside Acm.
        }
    };


    /**
     * Properties that are serialized when when requesting Elements
     */
    protected static final Set<String> ELEMENT_JSON = new HashSet<String>() {
        private static final long serialVersionUID = -6771999751087714932L;
        {
            //addAll( getACM2JSON().values() ); // Everything
            //addAll( getJSON2ACM().keySet() ); // Everything
            // now get rid of stuff that's handled special
            //removeAll()

            add(JSON_VALUE);

            add(JSON_BODY);
            add(JSON_TYPE);
            add(JSON_NAME);
            add(JSON_DOCUMENTATION);
            add(JSON_PROPERTY_TYPE);
            add(JSON_IS_DERIVED);
            // TODO: source/target should become noderefs at some point
            add(JSON_SOURCE);
            add(JSON_TARGET);

            add(JSON_OWNER);
            add(JSON_VALUE_TYPE);
            add(JSON_COMMENT);

            addAll(COMMON_JSON);

            addAll(VIEW_JSON);
            addAll(PRODUCT_JSON);

            addAll(MISC_PROPS_JSON);

            add(JSON_SPECIALIZATION);

            add(JSON_OWNED_ATTRIBUTE);
        }
    };

    /**
     * Properties of an Element, ie not found via specialization
     */
    public static final Set<String> ELEMENT_PROPS_JSON = new HashSet<String>() {
        private static final long serialVersionUID = 2925847909688618372L;

        // quickfix to add a compiler generated UID is failing
        {
            addAll(COMMON_JSON);    // id, author, last modified
            add(JSON_NAME);
            add(JSON_DOCUMENTATION);
            add(JSON_OWNER);
            add(JSON_OWNED_ATTRIBUTE);
        }
    };

    /**
     * Serialize all properties
     */
    public static final Set<String> ALL_JSON = new HashSet<String>() {
        private static final long serialVersionUID = 494169408514256049L;
        {
            addAll(COMMENT_JSON);
            addAll(ELEMENT_JSON);
            addAll(PRODUCT_JSON);
            addAll(VIEW_JSON);
        }
    };

    /**
     * Enumeration for specifying which JSON serialization property set to use
     *
     */
    public static enum JSON_TYPE_FILTER {
        VIEW, ELEMENT, PRODUCT, COMMENT, ALL;
    }

    /**
     * Map to filter the JSON keys for display purposes inside of EmsScriptNode
     */
    public static final Map<JSON_TYPE_FILTER, Set<String>> JSON_FILTER_MAP = new HashMap<JSON_TYPE_FILTER, Set<String>>() {
        private static final long serialVersionUID = -2080928480362524333L;
        {
            put(JSON_TYPE_FILTER.ALL, ALL_JSON);
            put(JSON_TYPE_FILTER.COMMENT, COMMENT_JSON);
            put(JSON_TYPE_FILTER.ELEMENT, ELEMENT_JSON);
            put(JSON_TYPE_FILTER.PRODUCT, ELEMENT_JSON);//PRODUCT_JSON);
            put(JSON_TYPE_FILTER.VIEW, ELEMENT_JSON); //VIEW_JSON);
        }
    };

    public static final String[] ACM_ASPECTS = {
        // relationship aspects such as ACM_RELATIONSHIPS_AS_SOURCE are left
        // out intentionally since they are not intended to be sysml types
        // like those above.

        // READ THIS: The type written to element json is the first of these found for
        // an element, so subclasses should precede parents, and more
        // likely matches should precede less likely ones.
        Acm.ACM_PRODUCT,
        Acm.ACM_VIEW,
        Acm.ACM_PROPERTY,
        Acm.ACM_PACKAGE,
        Acm.ACM_ELEMENT_VALUE,
        Acm.ACM_LITERAL_STRING,
        Acm.ACM_LITERAL_INTEGER,
        Acm.ACM_LITERAL_REAL,
        Acm.ACM_LITERAL_BOOLEAN,
        Acm.ACM_LITERAL_UNLIMITED_NATURAL,
        Acm.ACM_LITERAL_NULL,
        Acm.ACM_VIEWPOINT,
        Acm.ACM_COMMENT,
        Acm.ACM_CONSTRAINT,
        Acm.ACM_CONFORM,
        Acm.ACM_EXPOSE,
        Acm.ACM_GENERALIZATION,
        Acm.ACM_DURATION,
        Acm.ACM_DURATION_INTERVAL,
        Acm.ACM_EXPRESSION,
        Acm.ACM_INSTANCE_VALUE,
        Acm.ACM_INTERVAL,
        Acm.ACM_OPAQUE_EXPRESSION,
        Acm.ACM_STRING_EXPRESSION,
        Acm.ACM_TIME_EXPRESSION,
        Acm.ACM_TIME_INTERVAL,
        Acm.ACM_OPERATION,
        Acm.ACM_INSTANCE_SPECIFICATION,
        Acm.ACM_PARAMETER,
        Acm.ACM_SUCCESSION,
        Acm.ACM_BINDING,
        Acm.ACM_CONNECTOR,
        Acm.ACM_DEPENDENCY,
        Acm.ACM_CHARACTERIZES,
        Acm.ACM_LITERAL_SET,
        Acm.ACM_ASSOCIATION,
        Acm.ACM_DIRECTED_RELATIONSHIP,
        Acm.ACM_VALUE_SPECIFICATION,
        Acm.ACM_MAGICDRAW_DATA,
        Acm.ACM_UNTYPED
    };

    public static final String[] ACM_RELATIONSHIP_PROPERTY_ASPECTS = {
        ACM_RELATIONSHIPS_AS_SOURCE,
        ACM_RELATIONSHIPS_AS_TARGET,
        ACM_UNDIRECTED_RELATIONSHIPS
    };

    public static HashMap<String,String> PROPERTY_FOR_RELATIONSHIP_PROPERTY_ASPECTS = new HashMap<String,String>(){
        private static final long serialVersionUID = -5161523326512878741L;
        {
            put( ACM_RELATIONSHIPS_AS_SOURCE, ACM_REL_AS_SOURCE );
            put( ACM_RELATIONSHIPS_AS_TARGET, ACM_REL_AS_TARGET );
            put( ACM_UNDIRECTED_RELATIONSHIPS, ACM_UNDIRECTED_REL );
            };
    };

    public static final Set<String> VALUESPEC_ASPECTS = new HashSet<String>() {
        private static final long serialVersionUID = 494169408514256049L;
        {
            add("sysml:Duration");
            add("sysml:DurationInterval");
            add("sysml:ElementValue");
            add("sysml:Expression");
            add("sysml:InstanceValue");
            add("sysml:Interval");
            add("sysml:LiteralBoolean");
            add("sysml:LiteralInteger");
            add("sysml:LiteralNull");
            add("sysml:LiteralReal");
            add("sysml:LiteralString");
            add("sysml:LiteralUnlimitedNatural");
            add("sysml:OpaqueExpression");
            add("sysml:StringExpression");
            add("sysml:TimeExpression");
            add("sysml:TimeInterval");
        }
    };

    private static final Set<String> PROPERTY_VALUESPECS = new HashSet<String>() {
        private static final long serialVersionUID = -3103946764628743702L;
        {
            add(ACM_VALUE);
            add(ACM_LOWER);
            add(ACM_UPPER);
        }
    };

    private static final Set<String> EXPRESSION_VALUESPECS = new HashSet<String>() {
        private static final long serialVersionUID = -3646524138517088328L;
        {
            add(ACM_OPERAND);
        }
    };

    private static final Set<String> CONSTRAINT_VALUESPECS = new HashSet<String>() {
        private static final long serialVersionUID = -3646524138517088328L;
        {
            add(ACM_CONSTRAINT_SPECIFICATION);
        }
    };

    private static final Set<String> OPERATION_VALUESPECS = new HashSet<String>() {
        private static final long serialVersionUID = -3646524138517088328L;
        {
            add(ACM_OPERATION_EXPRESSION);
        }
    };

    private static final Set<String> ENUMERATION_VALUESPECS = new HashSet<String>() {
        private static final long serialVersionUID = -9213542282537528782L;
        {
            add(ACM_SET);
            add(ACM_SET_OPERAND);
        }
    };

    private static final Set<String> CONNECTOR_VALUESPECS = new HashSet<String>() {
        private static final long serialVersionUID = 1807385978686490394L;
        {
            add(ACM_CONNECTOR_VALUE);
            add(ACM_TARGET_LOWER);
            add(ACM_TARGET_UPPER);
            add(ACM_SOURCE_LOWER);
            add(ACM_SOURCE_UPPER);
        }
    };

    private static final Set<String> VIEW_VALUESPECS = new HashSet<String>() {
        private static final long serialVersionUID = -3590674933293910026L;
        {
            add(ACM_CONTENTS);
        }
    };

    private static final Set<String> PRODUCT_VALUESPECS = new HashSet<String>() {
        private static final long serialVersionUID = -5889978723965174381L;
        {
            add(ACM_CONTENTS);
        }
    };

    private static final Set<String> INSTANCE_SPEC_VALUESPECS = new HashSet<String>() {
        private static final long serialVersionUID = -5889978723965174381L;
        {
            add(ACM_INSTANCE_SPECIFICATION_SPECIFICATION);
        }
    };

    /**
     * Maps types that have properties that point to ValueSpecs and a list of those
     * properties.
     */
    public static final HashMap<String,Set<String>> TYPES_WITH_VALUESPEC = new HashMap<String,Set<String>>() {

        private static final long serialVersionUID = 2157925925273966466L;
        {
            put(ACM_PROPERTY, PROPERTY_VALUESPECS);
            put(ACM_EXPRESSION, EXPRESSION_VALUESPECS);
            put(ACM_LITERAL_SET, ENUMERATION_VALUESPECS);
            put(ACM_CONNECTOR, CONNECTOR_VALUESPECS);
            put(ACM_CONSTRAINT, CONSTRAINT_VALUESPECS);
            put(ACM_OPERATION, OPERATION_VALUESPECS);
            put(ACM_VIEW, VIEW_VALUESPECS);
            put(ACM_PRODUCT, PRODUCT_VALUESPECS);
            put(ACM_INSTANCE_SPECIFICATION, INSTANCE_SPEC_VALUESPECS);
        }
    };

    /**
     * The addition and removal of aspects is not available from a versioned
     * node in alfresco unless a property is changed. Thus, aspects that are
     * important see in older versions of nodes/elements must be accompanied
     * with a property change. Those aspects are mapped below to an integer
     * property that may be changed.
     */
    public static final HashMap< String, String > ASPECTS_WITH_BOGUS_PROPERTY =
            new HashMap< String, String >() {

        private static final long serialVersionUID = 245249712914397853L;
        {
            put(ACM_DELETED, ACM_DELETED_PROPERTY);
            put(ACM_UNTYPED, ACM_UNTYPED_PROPERTY);
            put(ACM_METACLASS, ACM_METACLASS_PROPERTY);
            put(ACM_STEREOTYPE, ACM_STEREOTYPE_PROPERTY);
            put(ACM_LITERAL_NULL, ACM_LITERAL_NULL_PROPERTY);
            put(ACM_CHARACTERIZES, ACM_CHARACTERIZES_PROPERTY);
            put(ACM_SUCCESSION, ACM_SUCCESSION_PROPERTY);
            put(ACM_BINDING, ACM_BINDING_PROPERTY);
            put(ACM_DEPENDENCY, ACM_DEPENDENCY_PROPERTY);
            put(ACM_EXPOSE, ACM_EXPOSE_PROPERTY);
            put(ACM_CONFORM, ACM_CONFORM_PROPERTY);
            put(ACM_GENERALIZATION, ACM_GENERALIZATION_PROPERTY);
        }
    };
}
