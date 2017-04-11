/**
 *
 */
package gov.nasa.jpl.sysml.json_impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;

import org.json.JSONObject;


/**
 *
 */
public class JsonStereotype extends JsonBaseElement
{
   private final static Logger LOGGER = Logger.getLogger(JsonStereotype.class.getName());

   public JsonStereotype(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public JsonProperty getTag(String name)
   {
      Collection<JSONObject> jList = systemModel.getElementWithName(jsonObj, name);
      for (JSONObject jObj : jList)
      {
         if (systemModel.isProperty(jObj))
         {
            return (JsonProperty)systemModel.wrap(jObj);
         }
      }
      return null;
   }
}
