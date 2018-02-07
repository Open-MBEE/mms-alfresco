package gov.nasa.jpl.sysml.json_impl;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import gov.nasa.jpl.sysml.Property;

public class JsonProperty extends JsonBaseElement implements
      Property<String, String, Date>
{
   private final static Logger LOGGER = Logger.getLogger(JsonProperty.class.getName());

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public JsonProperty(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   @Override
   public JsonElement getType()
   {
      String propID = systemModel.getPropertyTypeID(jsonObj);

      if (propID == null)
         return null;

      JSONObject jTypeObj = systemModel.getElement(propID);
      JsonBaseElement typeObj = systemModel.wrap(jTypeObj);

      if (typeObj instanceof JsonElement)
      {
         return (JsonElement) typeObj;
      }
      else
      {
         LOGGER.log(Level.WARNING, "Type of property is not an element: %s", typeObj);
      }
      return null;
   }

   public String getTypeId()
   {
      return systemModel.getPropertyTypeID(jsonObj);
   }

   public long getMultiplicityMin()
   {
      return systemModel.getMultiplicityMin(jsonObj);
   }

   /**
    *
    * @return max multiplicity value. -1 for any.
    */
   public long getMultiplicityMax()
   {
      return systemModel.getMultiplicityMax(jsonObj);
   }

   public List<JsonProperty> getRedefinedByThis()
   {
      ArrayList<JsonProperty> props = new ArrayList<JsonProperty>();

      List<JSONObject> jProps = systemModel.getRedefinedByThis(jsonObj);

      for (JSONObject jProp : jProps)
      {
         if (jProp != null && systemModel.isProperty(jProp))
         {
            props.add((JsonProperty)systemModel.wrap(jProp));
         }
      }
      return props;
   }

   public List<JsonProperty> getRedefiningThis()
   {
      ArrayList<JsonProperty> props = new ArrayList<JsonProperty>();

      List<JSONObject> jProps = systemModel.getRedefiningThis(jsonObj);

      for (JSONObject jProp : jProps)
      {
         if (jProp != null && systemModel.isProperty(jProp))
         {
            props.add((JsonProperty)systemModel.wrap(jProp));
         }
      }
      return props;
   }

   @Override
   public JsonPropertyValues getValue()
   {
      Object value = systemModel.getSpecializationProperty(jsonObj, JsonSystemModel.VALUE);
      if (value instanceof JSONArray)
      {
         JSONArray jArray = (JSONArray)value;
         return new JsonPropertyValues(systemModel, jArray);
      }
      else
      {
         LOGGER.log(Level.WARNING, "Property value is not in array form: {0}", id);
         return null;
      }
   }
}
