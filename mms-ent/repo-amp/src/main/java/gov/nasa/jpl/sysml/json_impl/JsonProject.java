/**
 *
 */
package gov.nasa.jpl.sysml.json_impl;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

/**
 *
 */
public class JsonProject extends JsonBaseElement
{
   private final static Logger LOGGER = Logger.getLogger(JsonProject.class.getName());

   public JsonProject(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public JsonStereotype getStereotype(String name)
   {
      Collection<JSONObject> jList = systemModel.getElementWithName(jsonObj, name);

      for (JSONObject jObj : jList)
      {
         if (systemModel.isStereotype(jObj))
         {
            return (JsonStereotype)systemModel.wrap(jObj);
         }
      }
      return null;
   }

   public JsonProperty getTag(String stereotypeName, String tagName)
   {
      JsonStereotype stereotype = getStereotype(stereotypeName);
      if (stereotype != null)
      {
         return stereotype.getTag(tagName);
      }
      return null;
   }
}
