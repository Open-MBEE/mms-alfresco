/**
 *
 */
package gov.nasa.jpl.sysml.json_impl;


import gov.nasa.jpl.mbee.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import gov.nasa.jpl.sysml.Element;
import gov.nasa.jpl.sysml.Property;

/**
 *
 */
public class JsonElement extends JsonBaseElement implements
      Element<String, String, Date>
{
   private final static Logger LOGGER = Logger.getLogger(JsonBaseElement.class.getName());

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public JsonElement(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   @Override
   public Collection<JsonElement> getSuperClasses()
   {
      List<JsonElement> classes = new ArrayList<JsonElement>();
      Collection<JSONObject> jList = systemModel.getSuperClasses(systemModel
            .getElement(id));
      for (JSONObject jObj : jList)
      {
         JsonBaseElement bElem = systemModel.wrap(jObj);
         if (bElem instanceof JsonElement)
         {
            classes.add((JsonElement) bElem);
         }
         else
         {
            LOGGER.log(Level.WARNING, "Unexpected type of super class: %s", jsonObj);
         }
      }

      return classes;
   }

   public boolean isSuperClassOf(JsonElement cls)
   {
      String id = cls.getId();
      JSONObject jElem = systemModel.getElement(id);
      return systemModel.isSuperClass(jsonObj, jElem);
   }

   public boolean isSubClassOf(JsonElement cls)
   {
      String id = cls.getId();
      JSONObject jElem = systemModel.getElement(id);
      return systemModel.isSuperClass(jElem, jsonObj);
   }

   @Override
   public Collection<JsonProperty> getProperties()
   {
      List<JsonProperty> props = new ArrayList<JsonProperty>();

      List<JSONObject> jProps = systemModel.getElementProperties(jsonObj);
      for (JSONObject jProp : jProps)
      {
         JsonBaseElement elem = systemModel.wrap(jProp);
         if (elem instanceof JsonProperty)
         {
            props.add((JsonProperty)elem);
         }
      }
      return props;
   }

   @Override
   public Collection<JsonProperty> getProperty(
         Object specifier)
   {
      if (specifier == null)
         return null;

      Collection<JsonProperty> props = null;
      JsonProperty prop = getPropertyWithIdentifier(specifier
            .toString());
      if (prop != null)
      {
         props = new ArrayList<JsonProperty>();
         props.add(prop);
      }
      if (props == null || props.isEmpty())
      {
         props = getPropertyWithName(name);
      }
      if (props == null || props.isEmpty())
      {
         props = getPropertyWithValue(name);
      }
      return props;
   }

   public JsonProperty getSingleProperty(Object specifier)
   {
      Collection<JsonProperty> props = getProperty(specifier);
      if (Utils.isNullOrEmpty(props))
      {
         return null;
      }
      return props.iterator().next();
   }

   @Override
   public JsonProperty getPropertyWithIdentifier(String id)
   {
      if (id == null)
         return null;

      Collection<JsonProperty> props = getProperties();
      for (JsonProperty prop : props)
      {
         if (id.equals(prop.getId()))
         {
            return prop;
         }
      }

      return null;
   }

   @Override
   public Collection<JsonProperty> getPropertyWithName(
         String name)
   {
      ArrayList<JsonProperty> list = new ArrayList<JsonProperty>();
      for (JsonProperty prop : getProperties())
      {
         if (prop.getName() != null && prop.getName().equals(name))
         {
            list.add(prop);
         }
      }
      return list;
   }

   @Override
   public Collection<Property< String, String, Date >> getPropertyWithType(
         Element< String, String, Date > type)
   {
      ArrayList<Property<String, String, Date>> list = new ArrayList<Property<String, String, Date>>();
      for (Property<String, String, Date> prop : getProperties())
      {
         if (prop.getType() != null && prop.getType().equals(type))
         {
            list.add(prop);
         }
      }
      return list;
   }

   @Override
   public Collection<JsonProperty> getPropertyWithValue(
         Object value)
   {
      ArrayList<JsonProperty> list = new ArrayList<JsonProperty>();
      for (JsonProperty prop : getProperties())
      {
         if (prop.getValue() != null && prop.getValue().equals(value))
         {
            list.add(prop);
         }
      }
      return list;
   }
}
