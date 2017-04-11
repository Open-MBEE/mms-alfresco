package gov.nasa.jpl.sysml.json_impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

public class JsonValueProperty extends JsonProperty
{
   private final static Logger LOGGER = Logger.getLogger(JsonValueProperty.class.getName());

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public JsonValueProperty(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   @Override
   public JsonValueType getType()
   {
      String propID = systemModel.getPropertyTypeID(jsonObj);

      if (propID == null)
         return null;

      JSONObject jTypeObj = systemModel.getElement(propID);
      JsonBaseElement typeObj = systemModel.wrap(jTypeObj);

      if (typeObj instanceof JsonValueType)
      {
         return (JsonValueType) typeObj;
      }
      else
      {
         LOGGER.log(Level.WARNING, "Type of value property is not a value type: %s", typeObj);
      }
      return null;
   }

   public Object getDefaultValue()
   {
      JsonPropertyValues values = getValue();
      if (values.getLength() == 0)
      {
         return null;
      }

      // TODO: return multiple values?
      return values.getValue(0);
   }
}
