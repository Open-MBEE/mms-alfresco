/**
 *
 */
package gov.nasa.jpl.sysml.json_impl;

import gov.nasa.jpl.mbee.util.CompareUtils;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nasa.jpl.sysml.BaseElement;
import gov.nasa.jpl.sysml.Version;
import gov.nasa.jpl.sysml.Workspace;

import org.json.JSONObject;


/**
 *
 */
public class JsonBaseElement implements BaseElement<String, String, Date>, Comparable<JsonBaseElement>
{

   JsonSystemModel systemModel;
   String id;
   String name;
   JSONObject jsonObj;

   String qualifiedName = null;
   String qualifiedId = null;

   private final static Logger LOGGER = Logger.getLogger(JsonBaseElement.class.getName());

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public JsonBaseElement(JsonSystemModel systemModel, String id)
   {

      this.systemModel = systemModel;
      this.id = id;
      this.jsonObj = systemModel.getElement(id);
      this.name = systemModel.getName(this.jsonObj);
   }

   public JsonBaseElement(JsonBaseElement elem)
   {
      this(elem.systemModel, elem.id);
   }

   public JsonBaseElement(JsonSystemModel systemModel, JSONObject jObj)
   {
      this.systemModel = systemModel;
      this.jsonObj = jObj;
      this.id = systemModel.getIdentifier(jObj);
      this.name = systemModel.getName(jObj);
   }

   @Override
   public String getId()
   {
      return id;
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public String getQualifiedName()
   {
      Object qualifiedNameObj = systemModel.getJsonProperty(jsonObj, JsonSystemModel.QUALIFIED_NAME);
      if (qualifiedNameObj != null)
      {
         return qualifiedNameObj.toString();
      }
      return "";
   }

   @Override
   public JsonBaseElement getOwner()
   {
      JSONObject elemJ = systemModel.getElement(id);
      JSONObject ownerJ = systemModel.getOwner(elemJ);

      return systemModel.wrap(ownerJ);
   }

   @Override
   public List<JsonBaseElement> getOwnedElements()
   {
      List<JsonBaseElement> elems = new ArrayList<JsonBaseElement>();
      List<JSONObject> jList = systemModel.getOwnedElements(jsonObj);
      for (JSONObject jObj : jList)
      {
         elems.add(systemModel.wrap(jObj));
      }

      return elems;
   }

   @Override
   public JsonProject getProject()
   {
      JSONObject current = jsonObj;
      JSONObject parent = systemModel.getOwner(jsonObj);

      while (parent != null)
      {
         current = parent;
         parent = systemModel.getOwner(current);
      }

      if (systemModel.isProject(current))
      {
         return (JsonProject)systemModel.wrap(current);
      }
      else
      {
         LOGGER.log(Level.WARNING, "Project could not be determined for: %s", jsonObj);
         return null;
      }
   }

   @Override
   public boolean isStereotypedAs(String name)
   {
      String stereotypeId = JsonSystemModel.getStereotypeID(name);
      if (stereotypeId != null)
      {
         List<String> metaTypeIds = systemModel.getAppliedMetaTypes(jsonObj);
         for (String metaTypeId: metaTypeIds)
         {
            if (stereotypeId.equals(metaTypeId))
            {
               return true;
            }
         }
      }
      else
      {
         LOGGER.log(Level.WARNING, "Unknown stereotype: %s", name);
      }

      return false;
   }

   @Override
   public String getTagValue(String name)
   {
      // TODO: this is a shortcut approach with hardcoded tag names
      // Need to construct tag list programmatically.
      String tagId = JsonSystemModel.getTagID(name);
      if (tagId != null)
      {
         List<JSONObject> jList = systemModel.getOwnedElements(jsonObj);
         for (JSONObject jObj : jList)
         {
            String type = systemModel.getType(jObj);
            if (JsonSystemModel.INSTANCE_SPECIFICATION.equals(type))
            {
               // Object classifier = systemModel.getSpecializationProperty(jObj, JsonSystemModel.CLASSIFIER);
               // if (classifier instanceof JSONArray)
               // {
               //   JSONArray jaClassifier = (JSONArray)classifier;
               // }

               List<JSONObject> jTagProperties = systemModel.getOwnedElements(jObj);
               for (JSONObject jTagProperty : jTagProperties)
               {
                  String propTypeId = systemModel.getPropertyTypeID(jTagProperty);
                  if (tagId.equals(propTypeId))
                  {
                     if (systemModel.isSlot(jTagProperty))
                     {
                        JsonSlot slot = (JsonSlot)systemModel.wrap(jTagProperty);

                        JsonPropertyValues propValues = slot.getValue();
                        if (propValues.getLength() == 0)
                        {
                           LOGGER.log(Level.WARNING, "No value for tag: %s", name);
                        }
                        else
                        {
                           if (propValues.getLength() > 1)
                           {
                              LOGGER.log(Level.WARNING, "Multiple values for tag. Return the first one: %s", name);
                           }

                           Object propValue = propValues.getValue(0);
                           if (propValue instanceof String)
                           {
                              return (String)propValue;
                           }
                           else if (propValue instanceof Double)
                           {
                              return "" + propValue;
                           }
                           else if (propValue instanceof Integer)
                           {
                              return "" + propValue;
                           }
                           else if (propValue instanceof JSONObject)
                           {
                              // TODO: for now return element name.
                              return systemModel.getName((JSONObject)propValue);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
      else
      {
         LOGGER.log(Level.WARNING, "Unknown tag: %s", name);
      }

      return null;
   }

   @Override
   public Workspace<String, String, Date> getWorkspace()
   {
      return null;
   }

   @Override
   public List<Version<String, Date, BaseElement<String, String, Date>>> getVersions()
   {
      return null;
   }

   @Override
   public Map<Date, Version<String, Date, BaseElement<String, String, Date>>> getVersionMap()
   {
      return null;
   }

   @Override
   public Version<String, Date, BaseElement<String, String, Date>> getLatestVersion()
   {
      return null;
   }

   @Override
   public Version<String, Date, BaseElement<String, String, Date>> getVersion()
   {
      return null;
   }

   @Override
   public Version<String, Date, BaseElement<String, String, Date>> getVersion(Date dateTime)
   {
      return null;
   }

   @Override
   public Date getCreationTime()
   {
      return null;
   }

   @Override
   public Date getModifiedTime()
   {
      return null;
   }

   @Override
   public void setVersion(Version<String, Date, BaseElement<String, String, Date>> version)
   {
      // TODO: implement
   }

   @Override
   public int compareTo(JsonBaseElement o)
   {
      // NOTE: simply check the element id
      return CompareUtils.compare(getId(), o.getId());
   }

   @Override
   public boolean equals(Object o)
   {
      if (o == null)
         return false;

      if (o instanceof JsonBaseElement)
      {
         return compareTo((JsonBaseElement) o) == 0;
      } else
      {
         return false;
      }
   }

   @Override
   public JsonBaseElement clone() throws CloneNotSupportedException
   {
      return new JsonBaseElement(this);
   }

   @Override
   public int hashCode()
   {
      return getId().hashCode();
   }

   @Override
   public String toString()
   {
      return String.format("%s (%s)", getName(), getId());
   }

   public String getSite()
   {
      Object siteObj = systemModel.getJsonProperty(jsonObj, JsonSystemModel.SITE_CHARACTERIZATION_ID);
      if (siteObj != null)
      {
         return siteObj.toString();
      }
      return "";
   }
}
