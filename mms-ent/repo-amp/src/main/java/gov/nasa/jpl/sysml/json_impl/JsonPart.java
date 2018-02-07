package gov.nasa.jpl.sysml.json_impl;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import gov.nasa.jpl.sysml.Element;

public class JsonPart extends JsonProperty
{
   private final static Logger LOGGER = Logger.getLogger(JsonPart.class.getName());

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public JsonPart(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   @Override
   public JsonBlock getType()
   {
      Element<String, String, Date> type = super.getType();

      if (type instanceof JsonBlock)
      {
         return (JsonBlock) type;
      }
      else
      {
         LOGGER.log(Level.WARNING, "Part is not a type of block: %s", type);
         return null;
      }
   }
}
