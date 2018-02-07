package gov.nasa.jpl.sysml.json_impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonSlot extends JsonBaseElement
{
   private final static Logger LOGGER = Logger.getLogger(JsonSlot.class.getName());

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public JsonSlot(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   public String getTypeId()
   {
      return systemModel.getPropertyTypeID(jsonObj);
   }

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
