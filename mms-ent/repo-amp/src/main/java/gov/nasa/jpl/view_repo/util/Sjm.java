/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech"). U.S. Government sponsorship
 * acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. - Neither the name of Caltech nor its operating
 * division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple static class for keeping track of Sysml JSON mappings
 *
 * @author han
 */

public class Sjm {
    public static final String AGGREGATION = "aggregation";
    public static final String APPLIEDSTEREOTYPEINSTANCEID = "appliedStereotypeInstanceId";
    public static final String ASSOCIATIONENDID = "associationEndId";
    public static final String ASSOCIATIONID = "associationId";
    public static final String CLASSIFIERIDS = "classifierIds";
    public static final String CLIENTDEPENDENCYIDS = "clientDependencyIds";
    public static final String COLLABORATIONUSEIDS = "collaborationUseIds";
    public static final String DATATYPEID = "datatypeId";
    public static final String DEFAULTVALUE = "defaultValue";
    public static final String DEFININGFEATUREID = "definingFeatureId";
    public static final String DEPLOYMENTIDS = "deploymentIds";
    public static final String DESCRIPTION = "description";
    public static final String DOCUMENTATION = "documentation";
    public static final String ELEMENTIMPORTIDS = "elementImportIds";
    public static final String ELEMENTS = "elements";
    public static final String ENDAPATHIDS = "endAPathIds";
    public static final String ENDBPATHIDS = "endBPathIds";
    public static final String ENDIDS = "endIds";
    public static final String GENERALIZATIONIDS = "generalizationIds";
    public static final String INTERFACEID = "interfaceId";
    public static final String ISABSTRACT = "isAbstract";
    public static final String ISDERIVED = "isDerived";
    public static final String ISDERIVEDUNION = "isDerivedUnion";
    public static final String ISFINALSPECIALIZATION = "isFinalSpecialization";
    public static final String ISID = "isID";
    public static final String ISLEAF = "isLeaf";
    public static final String ISORDERED = "isOrdered";
    public static final String ISREADONLY = "isReadOnly";
    public static final String ISSTATIC = "isStatic";
    public static final String ISUNIQUE = "isUnique";
    public static final String LOWERVALUE = "lowerValue";
    public static final String MDEXTENSIONSIDS = "mdExtensionsIds";
    public static final String MEMBERENDIDS = "memberEndIds";
    public static final String NAME = "name";
    public static final String NAMEEXPRESSION = "nameExpression";
    public static final String NAVIGABLEOWNEDENDIDS = "navigableOwnedEndIds";
    public static final String OWNEDATTRIBUTEIDS = "ownedAttributeIds";
    public static final String OWNEDENDIDS = "ownedEndIds";
    public static final String OWNERID = "ownerId";
    public static final String PACKAGEIMPORTIDS = "packageImportIds";
    public static final String POWERTYPEEXTENTIDS = "powertypeExtentIds";
    public static final String PROPERTYTYPE = "propertyType";
    public static final String QUALIFIERIDS = "qualifierIds";
    public static final String REDEFINEDCLASSIFIERIDS = "redefinedClassifierIds";
    public static final String REDEFINEDPROPERTYIDS = "redefinedPropertyIds";
    public static final String REPRESENTATIONID = "representationId";
    public static final String SLOTIDS = "slotIds";
    public static final String SOURCEID = "sourceId";
    public static final String SPECIFICATION = "specification";
    public static final String STEREOTYPEDELEMENTID = "stereotypedElementId";
    public static final String SUBSETTEDPROPERTYIDS = "subsettedPropertyIds";
    public static final String SUBSTITUTIONIDS = "substitutionIds";
    public static final String SUPPLIERDEPENDENCYIDS = "supplierDependencyIds";
    public static final String SYNCELEMENTID = "syncElementId";
    public static final String SYSMLID = "id";
    public static final String TARGET = "targetId";
    public static final String TEMPLATEBINDINGIDS = "templateBindingIds";
    public static final String TEMPLATEPARAMETERID = "templateParameterId";
    public static final String TYPE = "type";
    public static final String TYPEID = "typeId";
    public static final String UPPERVALUE = "upperValue";
    public static final String USECASEIDS = "useCaseIds";
    public static final String VALUEID = "valueId";
    public static final String VISIBILITY = "visibility";

    public static final String APPLIEDSTEREOTYPEIDS = "_appliedStereotypeIds";
    public static final String CHILDVIEWS = "_childViews";
    public static final String COMMITID = "_commitId";
    public static final String CONTENTS = "_contents";
    public static final String CREATED = "_created";
    public static final String CREATOR = "_creator";
    public static final String EDITABLE = "_editable";
    public static final String ELASTICID = "_elasticId";
    public static final String ISSITE = "_isSite";
    public static final String MODIFIED = "_modified";
    public static final String MODIFIER = "_modifier";
    public static final String MOUNTS = "_mounts";
    public static final String PARENTVIEWS = "_parentViews";
    public static final String PROJECTID = "_projectId";
    public static final String PROPERTIES = "_properties";
    public static final String QUALIFIEDID = "_qualifiedId";
    public static final String QUALIFIEDNAME = "_qualifiedName";
    public static final String REFID = "_refId";
    public static final String RELATEDDOCUMENTS = "_relatedDocuments";
    public static final String SITECHARACTERIZATIONID = "_groupId";
    public static final String SITES = "_sites";
    public static final String SLOTS = "_slots";
    public static final String TIMESTAMP = "_timestamp";

    public static final Map<String, String> STEREOTYPEIDS;

    static {
        STEREOTYPEIDS = new HashMap<>();
        STEREOTYPEIDS.put("_17_0_2_3_87b0275_1371477871400_792964_43374", "document");
        STEREOTYPEIDS.put("_17_0_1_232f03dc_1325612611695_581988_21583", "view");
        STEREOTYPEIDS.put("_17_0_2_3_407019f_1389807639137_860750_29082", "conforms");
        STEREOTYPEIDS.put("_16_5_4_409a058d_1259862803278_226185_1083", "exposes");
        STEREOTYPEIDS.put("_17_0_5_1_8660276_1407362513794_939259_26181", "characterizes");
    }


    public enum Aggregation {
        COMPOSITE("composite"), SIMPLE("simple"), NONE("none");

        private final String value;

        Aggregation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
