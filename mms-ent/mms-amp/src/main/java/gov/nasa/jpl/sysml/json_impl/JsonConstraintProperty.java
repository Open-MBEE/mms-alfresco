package gov.nasa.jpl.sysml.json_impl;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import gov.nasa.jpl.sysml.Element;

public class JsonConstraintProperty extends JsonProperty
{
   private final static Logger LOGGER = Logger.getLogger(JsonConstraintProperty.class.getName());

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public JsonConstraintProperty(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   @Override
   public JsonConstraintBlock getType()
   {
      Element<String, String, Date> type = super.getType();

      if (type instanceof JsonConstraintBlock)
      {
         return (JsonConstraintBlock) type;
      }
      else
      {
         LOGGER.log(Level.WARNING, "Constraint property is not a type of constraint block: %s", type);
         return null;
      }
   }
}
