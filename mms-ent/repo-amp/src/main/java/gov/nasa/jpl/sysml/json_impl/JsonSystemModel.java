/*******************************************************************************
 * Copyright (c) <2014>, California Institute of Technology ("Caltech").
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

package gov.nasa.jpl.sysml.json_impl;

import gov.nasa.jpl.mbee.util.FileUtils;
import gov.nasa.jpl.mbee.util.Pair;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.collections4.map.MultiValueMap;

import gov.nasa.jpl.sysml.AbstractSystemModel;

public class JsonSystemModel
      extends AbstractSystemModel< JSONObject, JSONObject, JSONObject, JSONObject, String, String, JSONObject, JSONObject, Object, Object, JSONObject >
{
   public static final String NAME = "name";
   public static final String QUALIFIED_NAME = "qualifiedName";
   public static final String TYPE = "type";
   public static final String OWNER = "owner";
   public static final String SOURCE = "source";
   public static final String TARGET = "target";
   public static final String SITE_CHARACTERIZATION_ID = "siteCharacterizationId";
   public static final String SYSMLID = "sysmlid";
   public static final String QUALIFIED_ID = "qualifiedId";
   public static final String ELEMENTS = "elements";
   public static final String SPECIALIZATION = "specialization";
   public static final String VALUE = "value";
   public static final String PROPERTY = "Property";
   public static final String PROPERTY_TYPE = "propertyType";
   public static final String APPLIED_METATYPES = "appliedMetatypes";
   public static final String CONTENTS = "contents";
   public static final String OPERAND = "operand";
   public static final String INSTANCE = "instance";
   public static final String ELEMENT = "element";
   public static final String _SLOT_ = "-slot-";
   public static final String STRING = "string";
   public static final String LIST = "list";
   public static final String INSTANCE_SPECIFICATION = "InstanceSpecification";
   public static final String INSTANCE_SPECIFICATION_SPECIFICATION = "instanceSpecificationSpecification";
   public static final String SOURCE_PATH = "sourcePath";
   public static final String TARGET_PATH = "targetPath";
   public static final String CLASSIFIER = "classifier";
   public static final String MULTIPLICITY_MAX = "multiplicityMax";
   public static final String MULTIPLICITY_MIN = "multiplicityMin";
   public static final String REDEFINES = "redefines";

   public static final String ST_BLOCK = "_11_5EAPbeta_be00301_1147424179914_458922_958";
   public static final String ST_PART = "_15_0_be00301_1199377756297_348405_2678";
   public static final String ST_VALUE_PROPERTY = "_12_0_be00301_1164123483951_695645_2041";
   public static final String ST_VALUE_TYPE = "_11_5EAPbeta_be00301_1147430239267_179145_1189";
   public static final String ST_UNIT = "_11_5EAPbeta_be00301_1147430329106_6539_1367";
   public static final String ST_QUANTITY_KIND = "_11_5EAPbeta_be00301_1147430310770_313444_1322";
   public static final String ST_CONSTRAINT_BLOCK = "_11_5EAPbeta_be00301_1147767804973_159489_404";
   public static final String ST_CONSTRAINT_PROPERTY = "_11_5EAPbeta_be00301_1147767840464_372327_467";
   public static final String ST_CONSTRAINT_PARAMETER = "_17_0_1_42401aa_1327611824171_784118_12184";

   public static final String META_GENERALIZATION = "_9_0_62a020a_1105704885195_432731_7879";
   public static final String META_INSTANCE_SPECIFICATION = "_9_0_62a020a_1105704885251_933969_7897";
   public static final String META_SLOT = "_9_0_62a020a_1105704885275_885607_7905";
   public static final String META_STEREOTYPE = "_9_0_62a020a_1105704941426_574917_9666";
   public static final String META_DIAGRAM = "_9_0_62a020a_1106296071977_61607_0";

   public static final String ST_BINDING_CONNECTOR = "_15_0_be00301_1204738115945_253883_5044";
   public static final String ST_DEPENDENCY = "_16_5_4_409a058d_1259862803278_226185_1083";
   public static final String ST_VIEW = "_17_0_1_232f03dc_1325612611695_581988_21583";
   public static final String ST_VIEWPOINT = "_11_5EAPbeta_be00301_1147420812402_281263_364";
   public static final String ST_EXPOSE = "_16_5_4_409a058d_1259862803278_226185_1083";
   public static final String ST_CONFORMS = "_17_0_2_3_407019f_1389807639137_860750_29082";
   public static final String ST_DIAGRAM_INFO = "_9_0_be00301_1108044380615_150487_0";


   public static final String TAG_GENERATED_FROM_VIEW = "_17_0_5_1_407019f_1430628276506_565_12080";
   public static final String TAG_UNIT = "_11_5EAPbeta_be00301_1147430364958_360156_1425";
   public static final String TAG_QUANTITY_KIND = "_11_5EAPbeta_be00301_1147430349926_544971_1421";

   public static final String UNIT="unit";
   public static final String QUANTITY_KIND="quantityKind";

   // MBSE Analyzer stereotypes and tags
   public static final String ST_EXTERNAL_ANALYSIS = "_17_0_1_42401aa_1327611546796_59249_12173";
   public static final String TAG_URL = "_17_0_1_42401aa_1327611913312_170324_12190";
   public static final String TAG_TYPE = "_17_0_2_60c020e_1382374932939_673434_11829";
   public static final String EXTERNAL_ANALYSIS = "ExternalAnalysis";
   public static final String URL = "url";

   public static final String ST_ANALYSIS_VARIABLE = "_17_0_1_42401aa_1327611824171_784118_12184";
   public static final String TAG_ANALYSYS_VAR_NAME = "_17_0_1_42401aa_1327611953156_52897_12191";
   public static final String TAG_DEFAULT_VALUE = "_17_0_1_2_42401aa_1340748740267_950617_11079";
   public static final String TAG_LOWER_BOUND = "_17_0_1_2_42401aa_1340762556414_622074_11080";
   public static final String TAG_UPPER_BOUND = "_17_0_1_2_42401aa_1340762569113_63864_11081";
   public static final String TAG_DIRECTION = "_17_0_2_42401aa_1360724516348_444121_11837";
   public static final String ANALYSIS_VARIABLE = "AnalysisVariable";
   public static final String ANALYSYS_VAR_NAME = "analysisVarName";
   public static final String DEFAULT_VALUE = "defaultValue";
   public static final String LOWER_BOUND = "lowerBound";
   public static final String UPPER_BOUND = "upperBound";
   public static final String DIRECTION = "direction";

   public static final String ST_SCRIPT = "_17_0_2_60c020e_1372450719621_997016_11834";
   public static final String TAG_LANGUAGE = "_17_0_2_60c020e_1372450756111_40969_11857";
   public static final String TAG_BODY = "_17_0_2_42401aa_1383670300996_566645_11829";
   public static final String SCRIPT = "Script";
   public static final String LANGUAGE = "language";
   public static final String BODY = "body";

   // Special view and viewpoint to collect elements on parametric diagrams
   public static final String ID_COLLECT_PARAM_DIAGRAM_ELEMENTS_VIEWPOINT = "_18_0_2_f060354_1448523624628_704247_15032";

   // Map of stereotypes
   static protected Map<String, String> stereotypeMap = new HashMap<String, String>();

   // Map of tags
   static protected Map<String, String> tagMap = new HashMap<String, String>();

   // Map of Generalization relationships (from child to parent)
   protected MultiValueMap<String, String> generalizationMap = new MultiValueMap<String, String>();

   // JSONObject that contains the JSON model:
   protected JSONObject root = null;

   // Map of project names
   protected Map<String, JSONObject> projectMap = new LinkedHashMap<String, JSONObject>();

   // Map of element sysmlid to JSON element objects:
   protected Map<String, JSONObject> elementMap = new LinkedHashMap<String, JSONObject>();

   // Map of element sysmlid to List of sysmlids that that element owns
   protected Map<String, List<String>> ownershipMap = new LinkedHashMap<String, List<String>>();

   // Map of instance specification sysmlid to slot sysml id
   protected Map<String, String> slotMap = new LinkedHashMap<String, String>();

   // property redefinition maps
   protected MultiValueMap<String, String> toRedefinedMap = new MultiValueMap<String, String>();
   protected MultiValueMap<String, String> fromRedefinedMap = new MultiValueMap<String, String>();

   // Map of element sysmlid to List of views of the element
   protected MultiValueMap<String, String> viewMap = new MultiValueMap<String, String>();

   // Map of view to viewpoint
   protected Map<String, String> viewpointMap = new LinkedHashMap<String, String>();


   private final static Logger LOGGER = Logger.getLogger(JsonSystemModel.class.getName());

   static
   {
      stereotypeMap.put(EXTERNAL_ANALYSIS, ST_EXTERNAL_ANALYSIS);
      stereotypeMap.put(ANALYSIS_VARIABLE, ST_ANALYSIS_VARIABLE);
      stereotypeMap.put(SCRIPT, ST_SCRIPT);

      tagMap.put(UNIT, TAG_UNIT);
      tagMap.put(QUANTITY_KIND, TAG_QUANTITY_KIND);

      tagMap.put(URL, TAG_URL);
      tagMap.put(TYPE, TAG_TYPE);
      tagMap.put(ANALYSYS_VAR_NAME, TAG_ANALYSYS_VAR_NAME);
      tagMap.put(DEFAULT_VALUE, TAG_DEFAULT_VALUE);
      tagMap.put(LOWER_BOUND, TAG_LOWER_BOUND);
      tagMap.put(UPPER_BOUND, TAG_UPPER_BOUND);
      tagMap.put(DIRECTION, TAG_DIRECTION);
      tagMap.put(LANGUAGE, TAG_LANGUAGE);
      tagMap.put(BODY, TAG_BODY);
   }

   public static String getStereotypeID(String name)
   {
      return stereotypeMap.get(name);
   }

   public static String getTagID(String name)
   {
      return tagMap.get(name);
   }

   public JsonSystemModel(Collection<String> libraryFilePaths) throws JsonSystemModelException
   {
      for (String libraryFilePath : libraryFilePaths)
      {
         readJson(libraryFilePath);
      }
   }

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public void readJson(String filePath) throws JsonSystemModelException
   {
      try
      {
         JSONObject project = null;

         String jsonString = FileUtils.fileToString(filePath);

         JSONObject json = new JSONObject(jsonString);
         // Make sure JSON format contains what we expect:
         if (json.has(ELEMENTS))
         {
            JSONArray elements = json.getJSONArray(ELEMENTS);

            for (int i = 0; i < elements.length(); i++)
            {
               JSONObject jsonObj = elements.getJSONObject(i);

               // Make sure JSON format contains what we expect:
               if (jsonObj.has(SYSMLID))
               {
                  // check for owner id rather than owner object since the owner
                  // may not be available in the model.
                  if (getOwnerId(jsonObj) == null)
                  {
                     if (project == null)
                     {
                        project = jsonObj;
                        String projectName = getName(project);
                        if (projectMap.containsKey(projectName))
                        {
                           LOGGER.log(Level.WARNING, "Project with the same name already exists: {0}", projectName);
                        }
                        else
                        {
                           projectMap.put(getName(project), project);
                        }
                     }
                     else
                     {
                        LOGGER.log(Level.WARNING, "There is more than one element with no owner: {0}", jsonObj);
                     }
                  }

                  // Update element map
                  String id = jsonObj.getString(SYSMLID);
                  elementMap.put(id, jsonObj);

                  // Update ownership map
                  if (jsonObj.has(OWNER) && !jsonObj.isNull(OWNER))
                  {
                     String owner = jsonObj.getString(OWNER);
                     List<String> owned = ownershipMap.get(owner);
                     if (owned == null)
                     {
                        owned = new ArrayList<String>();
                        ownershipMap.put(owner, owned);
                     }
                     owned.add(jsonObj.getString(SYSMLID));

                     // update slot map
                     if (isSlot(jsonObj))
                     {
                        JSONObject jOwner = getElement(owner);
                        // NOTE: isInstanceSpecification cannot be used until all elements are loaded.
                        // Simply create map of all instance specification including the ones used for tags.
                        // It does not hurt when we need to search relationships of actual instance specifications.
                        // if(isInstanceSpecification(jOwner))
                        // {
                           JsonSlot slot = (JsonSlot)wrap(jsonObj);
                           JsonPropertyValues values = slot.getValue();
                           if (JsonPropertyValues.INSTANCE_VALUE.equals(values.getValueType()))
                           {
                              for (int idxv=0; idxv < values.getLength(); idxv++)
                              {
                                 String idInstanceSpecification = values.getValueAsString(idxv);
                                 slotMap.put(idInstanceSpecification, id);
                              }
                           }
                        // }
                     }
                  }

                  // construct property redefinition maps
                  if (isProperty(jsonObj))
                  {
                     Object redefinedList = getSpecializationProperty(jsonObj, JsonSystemModel.REDEFINES);
                     if (redefinedList == null)
                     {
                        // ignore
                     }
                     else if (redefinedList instanceof JSONArray)
                     {
                        JSONArray jArray = (JSONArray)redefinedList;
                        for (int idx=0; idx < jArray.length(); idx++)
                        {
                           String propId = jArray.getString(idx);
                           toRedefinedMap.put(id, propId);
                           fromRedefinedMap.put(propId, id);
                        }
                     }
                     else
                     {
                        LOGGER.log(Level.WARNING, "redefines list is not in array form: {0}", id);
                     }
                  }

                  if (isGeneralization(jsonObj))
                  {
                     Object source = getSpecializationProperty(jsonObj, SOURCE);
                     Object target = getSpecializationProperty(jsonObj, TARGET);

                     if (!(source instanceof String))
                     {
                        LOGGER.log(Level.WARNING, "Source id is not a string for element: {0}", jsonObj);
                     }
                     else if (!(target instanceof String))
                     {
                        LOGGER.log(Level.WARNING, "Target id is not a string for element: {0}", jsonObj);
                     }
                     else
                     {
                        generalizationMap.put((String)source, target);
                     }
                  }

                  if (isExpose(jsonObj))
                  {
                     Object source = getSpecializationProperty(jsonObj, SOURCE);
                     Object target = getSpecializationProperty(jsonObj, TARGET);

                     if (!(source instanceof String))
                     {
                        LOGGER.log(Level.WARNING, "Source id is not a string for element: {0}", jsonObj);
                     }
                     else if (!(target instanceof String))
                     {
                        LOGGER.log(Level.WARNING, "Target id is not a string for element: {0}", jsonObj);
                     }
                     else
                     {
                        viewMap.put((String)target, source);
                     }
                  }

                  if (isConforms(jsonObj))
                  {
                     Object source = getSpecializationProperty(jsonObj, SOURCE);
                     Object target = getSpecializationProperty(jsonObj, TARGET);

                     if (!(source instanceof String))
                     {
                        LOGGER.log(Level.WARNING, "Source id is not a string for element: {0}", jsonObj);
                     }
                     else if (!(target instanceof String))
                     {
                        LOGGER.log(Level.WARNING, "Target id is not a string for element: {0}", jsonObj);
                     }
                     else
                     {
                        viewpointMap.put((String)source, (String)target);
                     }
                  }
               }
               else
               {
                  throw new JsonSystemModelException("Element has no sysmlid: "
                     + jsonObj.toString());
               }
            }
         }
         else
         {
            throw new JsonSystemModelException("Root element is not " + ELEMENTS);
         }
      }
      catch (Exception ex)
      {
         throw new JsonSystemModelException("Root element is not " + ELEMENTS, ex);
      }
   }


   public JsonBaseElement wrap(JSONObject jObj)
   {
      if (jObj == null)
      {
         return null;
      }
      else if (isProject(jObj))
      {
         return new JsonProject(this, jObj);
      }
      else if (isValueType(jObj))
      {
         return new JsonValueType(this, jObj);
      }
      else if (isBlock(jObj))
      {
         return new JsonBlock(this, jObj);
      }
      else if (isConstraintBlock(jObj))
      {
         return new JsonConstraintBlock(this, jObj);
      }
      else if (isPart(jObj))
      {
         return new JsonPart(this, jObj);
      }
      else if (isValueProperty(jObj))
      {
         return new JsonValueProperty(this, jObj);
      }
      else if (isConstraintProperty(jObj))
      {
         return new JsonConstraintProperty(this, jObj);
      }
      else if (isInstanceSpecification(jObj))
      {
         return new JsonInstanceSpecification(this, jObj);
      }
      else if (isSlot(jObj))
      {
         return new JsonSlot(this, jObj);
      }
      else if (isConstraintParameter(jObj))
      {
         return new JsonConstraintParameter(this, jObj);
      }
      else if (isBindingConnector(jObj))
      {
         return new JsonBindingConnector(this, jObj);
      }
      else if (isParametricDiagram(jObj))
      {
         return new JsonParametricDiagram(this, jObj);
      }
      else if (isStereotype(jObj))
      {
         return new JsonStereotype(this, jObj);
      }
      // Note: this should come after checking more specific properties such as value property and slot
      else if (isProperty(jObj))
      {
         return new JsonProperty(this, jObj);
      }
      else
      {
         return new JsonBaseElement(this, jObj);
      }
   }

   public JSONObject getProject(String name)
   {
      return projectMap.get(name);
   }


   @Override
   public String getName(JSONObject element)
   {
      Object name = getJsonProperty(element, NAME);
      if (name == null)
      {
         return "";
      }
      else if (name instanceof String)
      {
         return (String) name;
      }
      else
      {
         throw new IllegalStateException("Element name is not string: " + name);
      }
   }

   public JSONObject getElement(String id)
   {
      Collection<JSONObject> jColl = getElementWithIdentifier(null, id);
      if (jColl.size() > 0)
      {
         return jColl.iterator().next();
      }

      return null;
   }

   public List<JSONObject> getOwnedElements(String id)
   {
      List<JSONObject> elements = new ArrayList<JSONObject>();
      if (id == null)
      {
         LOGGER.log(Level.WARNING, "elementid is null");
         return elements;
      }

      if (!ownershipMap.containsKey(id))
      {
         // the element does not own any elements.
         return elements;
      }

      List<String> ownedIds = ownershipMap.get(id);

      for (String ownedId : ownedIds)
      {
         JSONObject child = getElement(ownedId);
         if (child != null)
         {
            elements.add(child);
         }
      }
      return elements;
   }

   public Collection<JSONObject> getSuperClasses(JSONObject element)
   {
      LinkedHashMap<String, JSONObject> classes = new LinkedHashMap<String, JSONObject>();
      collectSuperClasses(element, classes, true);

      return classes.values();
   }

   private void collectSuperClasses(JSONObject element, HashMap<String, JSONObject> classes,
         boolean recursive)
   {
      String id = getIdentifier(element);
      Collection<String> parentIds = generalizationMap.getCollection(id);
      if (parentIds != null)
      {
         for (String parentId : parentIds)
         {
            JSONObject parentElem = getElement(parentId);
            classes.put(parentId, parentElem);
            if (recursive)
            {
               collectSuperClasses(parentElem, classes, recursive);
            }
         }
      }
   }

   public boolean isSuperClass(JSONObject ancestor, JSONObject child)
   {
      Collection<JSONObject> superClasses = getSuperClasses(child);

      String ancestorId = getIdentifier(ancestor);
      for (JSONObject superClass : superClasses)
      {
         String superId = getIdentifier(superClass);
         if (ancestorId.equals(superId))
         {
            return true;
         }
      }
      return false;
   }

   public List<JSONObject> getRedefinedByThis(JSONObject element)
   {
      ArrayList<JSONObject> props = new ArrayList<JSONObject>();
      String id = getIdentifier(element);
      Collection<String> redefinedIds = toRedefinedMap.getCollection(id);

      if (redefinedIds != null)
      {
         for (String redefinedId : redefinedIds)
         {
            JSONObject elem = getElement(redefinedId);
            if (elem != null)
            {
               props.add(elem);
            }
            else
            {
               LOGGER.log(Level.WARNING, "Redefined element not found: {0}", redefinedId);
            }
         }
      }

      return props;
   }

   public List<JSONObject> getRedefiningThis(JSONObject element)
   {
      ArrayList<JSONObject> props = new ArrayList<JSONObject>();
      String id = getIdentifier(element);
      Collection<String> redefiningIds = fromRedefinedMap.getCollection(id);

      if (redefiningIds != null)
      {
         for (String redefiningId : redefiningIds)
         {
            JSONObject elem = getElement(redefiningId);
            if (elem != null)
            {
               props.add(elem);
            }
            else
            {
               LOGGER.log(Level.WARNING, "Redefining element not found: {0}", redefiningId);
            }
         }
      }

      return props;
   }

   public boolean isBlock(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_BLOCK);
   }

   public boolean isPart(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_PART);
   }

   public boolean isValueProperty(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_VALUE_PROPERTY);
   }

   /**
    * Return true if the element is an instance specification for a block or a part.
    * Will return false for Instance specifications that used to associate tag values.
    *
    * @param element
    * @return
    */
   public boolean isInstanceSpecification(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      if(metaTypes.contains(META_INSTANCE_SPECIFICATION))
      {
         if (getClassifierIds(element).size() > 0)
         {
            return true;
         }
      }

      return false;
   }

   public boolean isValueType(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_VALUE_TYPE);
   }

   public boolean isUnit(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_UNIT);
   }

   public boolean isQuantityKind(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_QUANTITY_KIND);
   }

   public boolean isConstraintBlock(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_CONSTRAINT_BLOCK);
   }

   public boolean isProperty(JSONObject element)
   {
      if (element == null) return false;

      String type = getType(element);
      return PROPERTY.equals(type);
   }

   public boolean isConstraintProperty(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_CONSTRAINT_PROPERTY);
   }

   public boolean isConstraintParameter(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_CONSTRAINT_PARAMETER);
   }

   public boolean isBindingConnector(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_BINDING_CONNECTOR);
   }

   public boolean isSlot(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(META_SLOT);
   }

   public boolean isDiagram(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(META_DIAGRAM);
   }

   public boolean isStereotype(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(META_STEREOTYPE);
   }

   public boolean isProject(JSONObject element)
   {
      if (element == null) return false;

      return getOwnerId(element) == null;
   }

   public boolean isParametricDiagram(JSONObject element)
   {
      if (element == null) return false;

      if (!isDiagram(element))
      {
         return false;
      }

      List<JSONObject> jList = getParametricDiagramElements(element);

      return jList.size() > 0;
   }

   public boolean isGeneralization(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(META_GENERALIZATION);
   }

   public boolean isView(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_VIEW);
   }

   public boolean isViewpoint(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_VIEWPOINT);
   }

   public boolean isExpose(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_EXPOSE);
   }

   public boolean isConforms(JSONObject element)
   {
      if (element == null) return false;

      List<String> metaTypes = getAppliedMetaTypes(element);
      return metaTypes.contains(ST_CONFORMS);
   }

   public List<JSONObject> getSourcePath(JSONObject bindingConnector)
   {
      ArrayList<JSONObject> pathElements = new ArrayList<JSONObject>();

      Object sourcePath = getSpecializationProperty(bindingConnector, SOURCE_PATH);
      if (sourcePath instanceof JSONArray)
      {
         JSONArray sourcePathJA = (JSONArray) sourcePath;
         for (int i=0; i < sourcePathJA.length(); i++)
         {
            String id = sourcePathJA.getString(i);
            JSONObject elem = getElement(id);
            if (elem == null)
            {
               LOGGER.log(Level.WARNING, "Element was not found: %s", id);
            }
            else
            {
               pathElements.add(elem);
            }
         }
      }
      else
      {
         LOGGER.log(Level.WARNING, "sourcePath is not a JSON array: %s", bindingConnector);
      }
      return pathElements;
   }

   public List<JSONObject> getTargetPath(JSONObject bindingConnector)
   {
      ArrayList<JSONObject> pathElements = new ArrayList<JSONObject>();

      Object targetPath = getSpecializationProperty(bindingConnector, TARGET_PATH);
      if (targetPath instanceof JSONArray)
      {
         JSONArray targetPathJA = (JSONArray) targetPath;
         for (int i=0; i < targetPathJA.length(); i++)
         {
            String id = targetPathJA.getString(i);
            JSONObject elem = getElement(id);
            if (elem == null)
            {
               LOGGER.log(Level.WARNING, "Element was not found: %s", id);
            }
            else
            {
               pathElements.add(elem);
            }
         }
      }
      else
      {
         LOGGER.log(Level.WARNING, "targetPath is not a JSON array: %s", bindingConnector);
      }
      return pathElements;
   }


   private boolean isCollectParamElementsView(JSONObject element)
   {
      String id = getIdentifier(element);

      String viewpointID = viewpointMap.get(id);

       return ID_COLLECT_PARAM_DIAGRAM_ELEMENTS_VIEWPOINT.equals(viewpointID);

   }

   public List<JSONObject> getParametricDiagramView(String diagramID)
   {
      Collection<String> views = viewMap.getCollection(diagramID);

      ArrayList<JSONObject> parametricDiagramViews = new ArrayList<JSONObject>();

      if (views != null)
      {
         for (String viewID : views)
         {
            Collection<JSONObject> jViews = getElementWithIdentifier(null, viewID);

            for (JSONObject jView : jViews)
            {
               if (isCollectParamElementsView(jView))
               {
                  parametricDiagramViews.add(jView);
               }
            }
         }
      }
      return parametricDiagramViews;
   }

   public Collection<JsonGraphElement> getDiagramGraphElements(JSONObject parametricDiagram)
   {
      LinkedHashSet<JsonGraphElement> graphElements = new LinkedHashSet<JsonGraphElement>();

      List<JSONObject> diagramElements = getParametricDiagramElements(parametricDiagram);

      JsonParametricDiagram diagram = (JsonParametricDiagram) wrap(parametricDiagram);

      graphElements.add(new JsonGraphNode(this, parametricDiagram, null));

      JSONObject jContext = diagram.getContextBlock();

      JsonGraphNode gContext = new JsonGraphNode(this, jContext, null);

      // see if this helps MBSE Pak parse parametric diagrams
      graphElements.add(gContext);

      for (JSONObject diagramElement : diagramElements)
      {
         if (isConstraintProperty(diagramElement))
         {
            JsonGraphElement gElem = new JsonGraphNode(this, diagramElement, gContext);
            graphElements.add(gElem);
         }
         else if (isBindingConnector(diagramElement))
         {
            JsonGraphElement parent = null;
            JsonBindingConnector bindingConnector = (JsonBindingConnector) wrap(diagramElement);

            List<JsonBaseElement> sourcePath = bindingConnector.getSourcePath();
            parent = gContext;
            List<JsonGraphNode> sourceNodes = new ArrayList<JsonGraphNode>();
            List<JsonGraphNode> targetNodes = new ArrayList<JsonGraphNode>();

            for (JsonBaseElement elem: sourcePath)
            {
               JsonGraphNode gElemInPath = new JsonGraphNode(this, elem.jsonObj , parent);
               sourceNodes.add(gElemInPath);
               graphElements.add(gElemInPath);
               parent = gElemInPath;
            }

            List<JsonBaseElement> targetPath = bindingConnector.getTargetPath();
            parent = gContext;
            for (JsonBaseElement elem: targetPath)
            {
               JsonGraphNode gElemInPath = new JsonGraphNode(this, elem.jsonObj , parent);
               targetNodes.add(gElemInPath);
               graphElements.add(gElemInPath);
               parent = gElemInPath;
            }

            JsonGraphElement gElem = new JsonGraphEdge(this, diagramElement, gContext,
                  sourceNodes.get(sourceNodes.size()-1), targetNodes.get(targetNodes.size()-1));
            graphElements.add(gElem);
         }
         else
         {
            LOGGER.log(Level.WARNING, "Unrecognized element type for parametric diagram: {0}", diagramElement);
         }
      }

      return graphElements;
   }

   public Collection<JsonBaseElement> getDiagramElements(JSONObject parametricDiagram)
   {
      LinkedHashSet<JsonBaseElement> elements = new LinkedHashSet<JsonBaseElement>();

      List<JSONObject> diagramElements = getParametricDiagramElements(parametricDiagram);

      JsonParametricDiagram diagram = (JsonParametricDiagram) wrap(parametricDiagram);

      for (JSONObject diagramElement : diagramElements)
      {
         if (isConstraintProperty(diagramElement))
         {
            elements.add(wrap(diagramElement));
         }
         else if (isBindingConnector(diagramElement))
         {
            JsonBindingConnector bindingConnector = (JsonBindingConnector) wrap(diagramElement);
            elements.add(bindingConnector);
            List<JsonBaseElement> sourcePath = bindingConnector.getSourcePath();

            for (JsonBaseElement elem: sourcePath)
            {
               elements.add(elem);
            }

            List<JsonBaseElement> targetPath = bindingConnector.getTargetPath();
            for (JsonBaseElement elem: targetPath)
            {
               elements.add(elem);
            }
         }
         else
         {
            LOGGER.log(Level.WARNING, "Unrecognized element type for parametric diagram: {0}", diagramElement);
         }
      }

      return elements;
   }

   public List<JSONObject> getParts(JSONObject element)
   {
      List<JSONObject> jProps = getElementProperties(element);
      List<JSONObject> parts = new ArrayList<JSONObject>();

      for (JSONObject jProp : jProps)
      {
         if (isPart(jProp))
         {
            parts.add(jProp);
         }
      }

      return parts;
   }

   public JSONObject getPart(JSONObject element, String name)
   {
      List<JSONObject> jProps = getElementProperties(element);

      for (JSONObject jProp : jProps)
      {
         if (isPart(jProp) && name.equals(getName(jProp)))
         {
            return jProp;
         }
      }

      return null;
   }

   public List<JSONObject> getValueProperties(JSONObject element)
   {
      List<JSONObject> jProps = getElementProperties(element);
      List<JSONObject> valueProperties = new ArrayList<JSONObject>();

      for (JSONObject jProp : jProps)
      {
         if (isValueProperty(jProp))
         {
            valueProperties.add(jProp);
         }
      }

      return valueProperties;
   }

   public List<JSONObject> getConstraintParameters(JSONObject element)
   {
      List<JSONObject> jProps = getElementProperties(element);
      List<JSONObject> parameters = new ArrayList<JSONObject>();

      for (JSONObject jProp : jProps)
      {
         if (isConstraintParameter(jProp))
         {
            parameters.add(jProp);
         }
      }

      return parameters;
   }

   public JSONObject getConstraintParameter(JSONObject element, String name)
   {
      List<JSONObject> jProps = getElementProperties(element);

      for (JSONObject jProp : jProps)
      {
         if (isConstraintParameter(jProp) && name.equals(getName(jProp)))
         {
            return jProp;
         }
      }

      return null;
   }

   public List<JSONObject> getSlots(JSONObject element)
   {
      List<JSONObject> jProps = getElementProperties(element);
      List<JSONObject> slots = new ArrayList<JSONObject>();

      for (JSONObject jProp : jProps)
      {
         if (isSlot(jProp))
         {
            slots.add(jProp);
         }
      }

      return slots;
   }

   public JSONObject getContainingSlot(JSONObject instanceSpecification)
   {
      String id = getIdentifier(instanceSpecification);
      String slotId = slotMap.get(id);
      if (slotId != null)
      {
         JSONObject jSlot = getElement(slotId);

         if(isSlot(jSlot))
         {
            return jSlot;
         }
         else
         {
            LOGGER.log(Level.WARNING, "Instance specification ({0}) is mapped from something other than a slot: {1}",
                  new Object[] {id, slotId});
         }
      }
      return null;
   }

   protected String getType(JSONObject element)
   {
      Object type = getSpecializationProperty(element, TYPE);

      if (type == null)
      {
         return null;
      }
      if (!(type instanceof String))
      {
         LOGGER.log(Level.WARNING, "Type is not in understandable format: {0}", type.toString());
         return null;
      }

      return (String) type;
   }

   protected List<String> getAppliedMetaTypes(JSONObject element)
   {
      List<String> metaTypes = new ArrayList<String>();

      Object obj = getJsonProperty(element, APPLIED_METATYPES);
      if (obj instanceof JSONArray)
      {
         JSONArray objJA = (JSONArray)obj;

         for (int i = 0; i < objJA.length(); i++)
         {
            metaTypes.add(objJA.getString(i));

         }
      }
      return metaTypes;
   }

   protected String getPropertyTypeID(JSONObject element)
   {
      Object typeId = getSpecializationProperty(element, PROPERTY_TYPE);
      if (typeId instanceof String)
      {
         return (String)typeId;
      }
      else
      {
         LOGGER.log(Level.WARNING, "propertyType is not in an understandable format: {0}", getIdentifier(element));
         return null;
      }
   }

   protected List<String> getClassifierIds(JSONObject element)
   {
      ArrayList<String> classifierIds = new ArrayList<String>();

      Object classifiers = getSpecializationProperty(element, CLASSIFIER);
      if (classifiers == null)
      {
         return classifierIds;
      }
      else if (classifiers instanceof JSONArray)
      {
         JSONArray jClassifiers = (JSONArray)classifiers;
         for (int i=0; i < jClassifiers.length(); i++)
         {
            classifierIds.add(jClassifiers.getString(i));
         }
      }
      else
      {
         LOGGER.log(Level.WARNING, "classifier is not an array of id: {0}", getIdentifier(element));
      }
      return classifierIds;
   }

   public long getMultiplicityMin(JSONObject element)
   {
      if (hasSpecializationProperty(element, MULTIPLICITY_MIN))
      {
         Object multiplicityMin = getSpecializationProperty(element, MULTIPLICITY_MIN);

         if (multiplicityMin == null)
         {
            return 1L;
         }
         else if (multiplicityMin instanceof Long)
         {
            return ((Long)multiplicityMin).longValue();
         }
         else if (multiplicityMin instanceof Integer)
         {
            return ((Integer)multiplicityMin).longValue();
         }
         else
         {
            LOGGER.log(Level.WARNING, "Multiplicity min ({0}) was not parsed correctly for {1}",
                  new Object[] {multiplicityMin.toString(), getIdentifier(element)});
         }
      }
      return 1L;
   }

   public long getMultiplicityMax(JSONObject element)
   {
      if (hasSpecializationProperty(element, MULTIPLICITY_MAX))
      {
         Object multiplicityMax = getSpecializationProperty(element, MULTIPLICITY_MAX);

         if (multiplicityMax == null)
         {
            return 1L;
         }
         else if (multiplicityMax instanceof Long)
         {
            return ((Long)multiplicityMax).longValue();
         }
         else if (multiplicityMax instanceof Integer)
         {
            return ((Integer)multiplicityMax).longValue();
         }
         else
         {
            LOGGER.log(Level.WARNING, "Multiplicity max ({0}) was not parsed correctly for {1}",
                  new Object[] {multiplicityMax.toString(), getIdentifier(element)});
         }
      }
      return 1L;
   }

   protected boolean hasJsonProperty(JSONObject element, String name)
   {
      if (element != null)
      {
         // See if element has the jsonName field:
         if (element.has(name) && !element.isNull(name))
         {
            return true;
         }
      }
      return false;
   }

   protected boolean hasSpecializationProperty(JSONObject element, String propName)
         throws JSONException
   {
      if (element != null)
      {
         if (getSpecializationProperty(element, propName) != null)
         {
            return true;
         }
      }
      return false;
   }

   protected Object getJsonProperty(JSONObject element, String propName)
   {
      if (element != null)
      {
         if (element.has(propName) && !element.isNull(propName))
         {
            return element.get(propName);
         }
      }
      return null;
   }

   protected Object getSpecializationProperty(JSONObject element, String propName)
   {
      if (element.has(SPECIALIZATION))
      {
         Object specialization = element.get(SPECIALIZATION);
         if (specialization instanceof JSONObject)
         {
            JSONObject specializationJson = (JSONObject) specialization;
            if (specializationJson.has(propName))
            {
               return specializationJson.get(propName);
            }
         }
      }
      return null;
   }

   public List<JSONObject> getElementProperties(JSONObject element)
         throws JSONException
   {
      ArrayList<JSONObject> jProps = new ArrayList<JSONObject>();

      if (element != null)
      {
         List<JSONObject> children = getOwnedElements(element);

         for (JSONObject child : children)
         {
            // Make sure the children are of type "Property":
            if (PROPERTY.equals(getType(child)))
            {
               jProps.add(child);
            }
         }
      }
      return jProps;
   }

   public JSONObject getElementProperty(JSONObject element, String name)
         throws JSONException
   {
      if (element != null)
      {
         List<JSONObject> children = getOwnedElements(element);

         for (JSONObject child : children)
         {
            // Make sure the children are of type "Property":
            if (PROPERTY.equals(getType(child)))
            {
               if (name.equals(getName(child)))
               {
                  return child;
               }
            }
         }
      }
      return null;
   }

   public List<JSONObject> getOwnedSlots(JSONObject element)
         throws JSONException
   {
      ArrayList<JSONObject> slots = new ArrayList<JSONObject>();

      if (element != null)
      {
         List<JSONObject> children = getOwnedElements(element);

         for (JSONObject child : children)
         {
            if (isSlot(child))
            {
               slots.add(child);
            }
         }
      }
      return slots;
   }

   protected List<JSONObject> searchWithinContextByProperty(JSONObject owner,
         String propName, Object propValue)
   {
      List<JSONObject> elementList = new ArrayList<JSONObject>();

      if (owner == null)
         return elementList;

      Collection<JSONObject> children = getOwnedElements(owner);
      for (JSONObject child : children)
      {
         if (propValue == null
               || propValue.equals(getJsonProperty(child, propName)))
         {
            elementList.add(child);
         }

         List<JSONObject> childElementList = searchWithinContextByProperty(child,
               propName, propValue);

         elementList.addAll(childElementList);
      }

      return elementList;
   }

   protected List<JSONObject> searchWithinContextBySpecialization(JSONObject owner,
         String propName, Object propValue)
   {
      List<JSONObject> elementList = new ArrayList<JSONObject>();

      if (owner == null)
         return elementList;

      Collection<JSONObject> children = getOwnedElements(owner);
      for (JSONObject child : children)
      {
         if (propValue == null
               || propValue.equals(getSpecializationProperty(child, propName)))
         {
            elementList.add(child);
         }

         List<JSONObject> childElementList = searchWithinContextBySpecialization(child,
               propName, propValue);

         elementList.addAll(childElementList);
      }

      return elementList;
   }

   protected List<JSONObject> searchForElementsByProperty(JSONObject context,
         String propName, Object propValue)
   {
      List<JSONObject> elementList = new ArrayList<JSONObject>();
      if (propName == null)
      {
         return elementList;
      }

      if (context == null)
      {
         // Search for element by going linearly through all the elements:
         for (JSONObject element : elementMap.values())
         {
            if (hasJsonProperty(element, propName))
            {
               if (propValue == null
                     || getJsonProperty(element, propName).equals(propValue))
               {
                  elementList.add(element);
               }
            }
         }
      }
      else
      {
         // Do depth first search within the context of the owner for a match:

         JSONObject owner = context;
         elementList = searchWithinContextByProperty(owner, propName, propValue);
      }
      return elementList;
   }

   protected List<JSONObject> searchForElementsBySpecialization(JSONObject context,
         String propName, Object propValue)
   {
      List<JSONObject> elementList = new ArrayList<JSONObject>();
      if (propName == null)
      {
         return elementList;
      }

      if (context == null)
      {
         // Search for element by going linearly through all the elements:
         for (JSONObject element : elementMap.values())
         {
            if (hasSpecializationProperty(element, propName))
            {
               if (propValue == null
                     || getSpecializationProperty(element, propName).equals(propValue))
               {
                  elementList.add(element);
               }
            }
         }
      }
      else
      {
         // Do depth first search within the context of the owner for a match:
         JSONObject owner = context;
         elementList = searchWithinContextBySpecialization(owner, propName, propValue);
      }
      return elementList;
   }

   public List<JSONObject> getOwnedElements(JSONObject owner)
   {
      ArrayList<JSONObject> elements = new ArrayList<JSONObject>();
      if (owner == null)
      {
         throw new IllegalArgumentException("owner cannot be null");
      }

      if (!owner.has(SYSMLID) || owner.isNull(SYSMLID))
      {
         LOGGER.log(Level.WARNING, "sysmlid is not in a valid format: {0}", owner.toString());
         return elements;
      }

      String id = (String) owner.get(SYSMLID);
      return getOwnedElements(id);
   }

   public JSONObject getOwner(JSONObject element)
   {
      Object prop = getJsonProperty(element, OWNER);
      if (prop instanceof String)
      {
         String id = (String) prop;
         Collection<JSONObject> jObjs = getElementWithIdentifier(null, id);
         if (jObjs.size() > 1)
         {
            LOGGER.log(Level.WARNING, "Element has more than one owner: {0}", element);
         }

         if (jObjs.size() > 0)
         {
            return jObjs.iterator().next();
         }
      }
      return null;
   }

   public String getOwnerId(JSONObject element)
   {
      Object prop = getJsonProperty(element, OWNER);
      if (prop instanceof String)
      {
         return (String) prop;
      }
      return null;
   }

   @Override
   public Collection<JSONObject> getSource(JSONObject relationship)
   {
      ArrayList<JSONObject> sources = new ArrayList<JSONObject>();

      Object prop = getSpecializationProperty(relationship, SOURCE);
      if (prop instanceof String)
      {
         String id = (String) prop;

         Collection<JSONObject> jObjs = getElementWithIdentifier(null, id);
         return jObjs;
      }

      // if source is not defined, try to get it by parsing source path
      List<JSONObject> sourceSequence = getSourcePath(relationship);
      if (sourceSequence.size() > 0)
      {
         sources.add(sourceSequence.get(sourceSequence.size()-1));
      }

      return sources;
   }

   @Override
   public Collection<JSONObject> getTarget(JSONObject relationship)
   {
      ArrayList<JSONObject> targets = new ArrayList<JSONObject>();

      Object prop = getSpecializationProperty(relationship, TARGET);
      if (prop instanceof String)
      {
         String id = (String) prop;
         Collection<JSONObject> jObjs = getElementWithIdentifier(null, id);
         return jObjs;
      }

      // if target is not defined, try to get it from target path
      List<JSONObject> targetSequence = getTargetPath(relationship);
      if (targetSequence.size() > 0)
      {
         targets.add(targetSequence.get(targetSequence.size()-1));
      }
      return targets;
   }

   @Override
   public Collection<JSONObject> getElementWithIdentifier(JSONObject context, String id)
   {
      ArrayList<JSONObject> jArray = new ArrayList<JSONObject>();

      if (id == null)
      {
         return jArray;
      }

      if (context == null)
      {
         JSONObject jObj = elementMap.get(id);
         if (jObj != null)
         {
            jArray.add(jObj);
            return jArray;
         }
      }

      // if context is not null, search under the context
      return searchForElementsByProperty(context, SYSMLID, id);
   }

   @Override
   public Collection<JSONObject> getElementWithName(JSONObject context,
         String name)
   {
      if (name == null)
      {
         return null;
      }
      return searchForElementsByProperty(context, NAME, name);
   }

   @Override
   public Collection<JSONObject> getElementWithQualifiedName(JSONObject context,
         String qualifiedName)
   {
      if (qualifiedName == null)
      {
         return null;
      }
      return searchForElementsByProperty(context, QUALIFIED_NAME, qualifiedName);
   }

   /**
    * Get child elements of the given type. The search is recursive following containment
    * relationships.
    *
    * @param context owner
    * @param specifier type of child elements to be searched
    */
   public Collection<JSONObject> getElementWithType(JSONObject context,
         String specifier)
   {
      if (specifier == null)
      {
         return null;
      }
      return searchForElementsBySpecialization(context, TYPE, specifier);
   }

   @Override
   public String getIdentifier(JSONObject context)
   {
      Object id = null;
      try
      {
         id = getJsonProperty(context, SYSMLID);
      } catch (JSONException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      if (id instanceof String)
      {
         return (String)id;
      }

      return null;
   }

   @Override
   public Collection<JSONObject> getPropertyWithIdentifier(JSONObject context,
         String specifier)
   {
      List<JSONObject> jProps = null;
      try
      {
         jProps = getElementProperties(context);
      }
      catch (JSONException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      if (jProps != null)
      {
         Collection<JSONObject> propertiesToReturn = new ArrayList<JSONObject>();
         for (JSONObject jProp : jProps)
         {
            // Make sure property type matches the specifier:
            String id = getIdentifier(jProp);
            if (id != null && id.equals(specifier))
            {
               propertiesToReturn.add(jProp);
            }
         }

         if (!propertiesToReturn.isEmpty())
            return propertiesToReturn;
      }

      return null;
   }

   // not used currently?
   public Collection<JSONObject> getPropertyWithType(JSONObject context,
         String typeName)
   {

      List<JSONObject> jProps = null;
      try
      {
         jProps = getElementProperties(context);
      }
      catch (JSONException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      Collection<JSONObject> propertiesToReturn = new ArrayList<JSONObject>();
      for (JSONObject jProp : jProps)
      {
         // Make sure property type matches the specifier:
         String propertyTypeID = getPropertyTypeID(jProp);
         JSONObject propertyType = getElement(propertyTypeID);
         if (propertyType != null)
         {
            Object propertyTypeName = getJsonProperty(propertyType, NAME);
            if (typeName.equals(propertyTypeName))
            {
               propertiesToReturn.add(jProp);
            }
         }
      }

      return propertiesToReturn;
   }

   protected List<JSONObject> getParametricDiagramElements(JSONObject parametricDiagram)
   {
      ArrayList<JSONObject> diagramElements = new ArrayList<JSONObject>();

      String id = getIdentifier(parametricDiagram);

      List<JSONObject> diagramViews = getParametricDiagramView(id);

      for (JSONObject diagramView : diagramViews)
      {
         String viewID = getIdentifier(diagramView);
         Object contents = getSpecializationProperty(diagramView, CONTENTS);
         if (contents instanceof JSONObject)
         {
            JSONObject contentsJ = (JSONObject) contents;
            if (contentsJ.has(OPERAND) && !contentsJ.isNull(OPERAND))
            {
               Object operand = contentsJ.get(OPERAND);
               if (operand instanceof JSONArray)
               {
                  JSONArray operandJA = (JSONArray)operand;
                  for (int i=0; i < operandJA.length(); i++)
                  {
                     Object oper = operandJA.get(i);
                     if (oper instanceof JSONObject)
                     {
                        JSONObject operJ = (JSONObject)oper;
                        String instanceID = operJ.getString(INSTANCE);

                        JSONObject generatedFromViewTag = getElement(instanceID + _SLOT_ +
                              TAG_GENERATED_FROM_VIEW);

                        if (generatedFromViewTag != null)
                        {
                           Object value = getSpecializationProperty(generatedFromViewTag, VALUE);
                           if (value instanceof JSONArray)
                           {
                              JSONArray valueJA = (JSONArray) value;
                              for (int i2=0; i2 < valueJA.length(); i2++)
                              {
                                 Object v = valueJA.get(i2);
                                 if (v instanceof JSONObject)
                                 {
                                    JSONObject vJ = (JSONObject) v;
                                    if (vJ.has(ELEMENT) && !vJ.isNull(ELEMENT))
                                    {
                                       String elementID = vJ.getString(ELEMENT);
                                       if (viewID.equals(elementID))
                                       {
                                          JSONObject instance = getElement(instanceID);
                                          if (instance != null)
                                          {
                                             diagramElements.addAll(getElementsFromViewInstance(instance));
                                          }
                                          else
                                          {
                                             LOGGER.log(Level.WARNING, "View instance is not found: {0}", instanceID);
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                           else
                           {
                              LOGGER.log(Level.WARNING, "specialization value is not a json array: %s", instanceID);
                           }
                        }
                        else
                        {
                           LOGGER.log(Level.WARNING, "generatedFromView tag is null: %s", instanceID);
                        }

                     }
                     else
                     {
                        LOGGER.log(Level.WARNING, "element of operand is not a JSON array: %s", oper);
                     }
                  }
               }
               else
               {
                  LOGGER.log(Level.WARNING, "operand is not a JSON array: %s", operand);
               }
            }
         }
         else
         {
            LOGGER.log(Level.WARNING, "contents is not a JSON object");
         }
      }

      return diagramElements;
   }

   private List<JSONObject> getElementsFromViewInstance(JSONObject viewInstance)
   {
      ArrayList<JSONObject> elements = new ArrayList<JSONObject>();

      Object specSpecification = getSpecializationProperty(viewInstance,
            INSTANCE_SPECIFICATION_SPECIFICATION);
      if (specSpecification instanceof JSONObject)
      {
         JSONObject specSpecificationJ = (JSONObject) specSpecification;
         if (specSpecificationJ.has(STRING) && !specSpecificationJ.isNull(STRING))
         {
            String valueStr = specSpecificationJ.getString(STRING);
            JSONObject jValue = new JSONObject(valueStr);

            if (jValue.has(LIST))
            {
               JSONArray listElems = jValue.getJSONArray(LIST);

               for (int i = 0; i < listElems.length(); i++)
               {
                  Object listElem = listElems.get(i);
                  if (listElem instanceof JSONArray)
                  {
                     JSONArray listElemJA = (JSONArray)listElem;
                     if (listElemJA.length() > 0)
                     {
                        Object listElem0 = listElemJA.get(0);
                        if (listElem0 instanceof JSONObject)
                        {
                           JSONObject listElem0J = (JSONObject)listElem0;
                           if (listElem0J.has(SOURCE) && !listElem0J.isNull(SOURCE))
                           {
                              String sourceID = listElem0J.getString(SOURCE);
                              JSONObject elem = getElement(sourceID);
                              if (elem != null)
                              {
                                 elements.add(elem);
                              }
                              else
                              {
                                 LOGGER.log(Level.WARNING, "element was not found: %s", sourceID);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
      return elements;
   }

   @Override
   public boolean isDirected(JSONObject relationship)
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public Collection<JSONObject> getRelatedElements(JSONObject relationship)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getElementForRole(JSONObject relationship, String role)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Class<JSONObject> getElementClass()
   {
      return JSONObject.class;
   }

   @Override
   public Class<JSONObject> getContextClass()
   {
      return JSONObject.class;
   }

   @Override
   public Class<JSONObject> getTypeClass()
   {
      return JSONObject.class;
   }

   @Override
   public Class<JSONObject> getPropertyClass()
   {
      return JSONObject.class;
   }

   @Override
   public Class<String> getNameClass()
   {
      return String.class;
   }

   @Override
   public Class<String> getIdentifierClass()
   {
      return String.class;
   }

   @Override
   public Class<JSONObject> getValueClass()
   {
      return JSONObject.class;
   }

   @Override
   public Class<JSONObject> getRelationshipClass()
   {
      return JSONObject.class;
   }

   @Override
   public Class<Object> getVersionClass()
   {
      return Object.class;
   }

   @Override
   public Class<Object> getWorkspaceClass()
   {
      return Object.class;
   }

   @Override
   public Class<JSONObject> getConstraintClass()
   {
      return JSONObject.class;
   }

   @Override
   public Class<? extends JSONObject> getViewClass()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Class<? extends JSONObject> getViewpointClass()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public JSONObject createConstraint(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public JSONObject createElement(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String createIdentifier(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String createName(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public JSONObject createProperty(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public JSONObject createRelationship(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public JSONObject createType(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public JSONObject createValue(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Object createVersion(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public JSONObject createView(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public JSONObject createViewpoint(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Object createWorkspace(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Object delete(Object object)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getConstraint(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getConstraintWithElement(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getConstraintWithIdentifier(JSONObject context, String specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getConstraintWithName(JSONObject context, String specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getConstraintWithProperty(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getConstraintWithRelationship(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getConstraintWithType(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getConstraintWithValue(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getConstraintWithVersion(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getConstraintWithView(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getConstraintWithViewpoint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getConstraintWithWorkspace(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getElementWithConstraint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getElementWithProperty(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getElementWithRelationship(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getElementWithType(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getElementWithValue(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getElementWithVersion(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getElementWithView(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getElementWithViewpoint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getElementWithWorkspace(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getProperty(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getPropertyWithConstraint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getPropertyWithElement(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getPropertyWithRelationship(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getPropertyWithType(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getPropertyWithValue(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getPropertyWithVersion(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getPropertyWithView(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getPropertyWithViewpoint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getPropertyWithWorkspace(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getRelationship(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getRelationshipWithConstraint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getRelationshipWithElement(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getRelationshipWithIdentifier(JSONObject context, String specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getRelationshipWithName(JSONObject context, String specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getRelationshipWithProperty(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getRelationshipWithType(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getRelationshipWithValue(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getRelationshipWithVersion(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getRelationshipWithView(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getRelationshipWithViewpoint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getRelationshipWithWorkspace(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getType(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getTypeString(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getTypeWithConstraint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getTypeWithElement(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getTypeWithIdentifier(JSONObject context, String specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getTypeWithName(JSONObject context, String specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getTypeWithProperty(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getTypeWithRelationship(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getTypeWithValue(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getTypeWithVersion(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getTypeWithView(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getTypeWithViewpoint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getTypeWithWorkspace(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getValue(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getValueWithConstraint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getValueWithElement(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getValueWithIdentifier(JSONObject context, String specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getValueWithName(JSONObject context, String specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getValueWithProperty(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getValueWithRelationship(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getValueWithType(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getValueWithVersion(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getValueWithView(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getValueWithViewpoint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getValueWithWorkspace(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<Object> getVersion(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getView(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewpoint(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewpointWithConstraint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewpointWithElement(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewpointWithIdentifier(JSONObject context, String specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewpointWithName(JSONObject context, String specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewpointWithProperty(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewpointWithRelationship(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewpointWithType(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewpointWithValue(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewpointWithVersion(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewpointWithView(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewpointWithWorkspace(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewWithConstraint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewWithElement(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewWithIdentifier(JSONObject context, String specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewWithName(JSONObject context, String specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewWithProperty(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewWithRelationship(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewWithType(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewWithValue(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewWithVersion(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewWithViewpoint(JSONObject context, JSONObject specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViewWithWorkspace(JSONObject context, Object specifier)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<Object> getWorkspace(JSONObject context)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Object set(Object object, Object specifier, JSONObject value)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean fixConstraintViolations(JSONObject element, Object version)
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean idsAreWritable()
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean namesAreWritable()
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean versionsAreWritable()
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public JSONObject getDomainConstraint(JSONObject element, Object version, Object workspace)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void addConstraint(JSONObject constraint, Object version, Object workspace)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void addDomainConstraint(JSONObject constraint, Object version, Set<JSONObject> valueDomainSet,
         Object workspace)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void addDomainConstraint(JSONObject constraint, Object version,
         Pair<JSONObject, JSONObject> valueDomainRange, Object workspace)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void relaxDomain(JSONObject constraint, Object version, Set<JSONObject> valueDomainSet, Object workspace)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void relaxDomain(JSONObject constraint, Object version, Pair<JSONObject, JSONObject> valueDomainRange,
         Object workspace)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public Collection<JSONObject> getConstraintsOfElement(JSONObject element, Object version, Object workspace)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Collection<JSONObject> getViolatedConstraintsOfElement(JSONObject element, Object version)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void setOptimizationFunction(Method method, Object... arguments)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public Number getScore()
   {
      // TODO Auto-generated method stub
      return null;
   }
}
