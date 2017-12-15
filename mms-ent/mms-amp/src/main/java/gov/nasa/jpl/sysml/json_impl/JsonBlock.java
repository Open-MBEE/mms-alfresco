/**
 *
 */
package gov.nasa.jpl.sysml.json_impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 */
public class JsonBlock extends JsonElement
{
   private final static Logger LOGGER = Logger.getLogger(JsonBlock.class.getName());

   public JsonBlock(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public List<JsonPart> getParts()
   {
      ArrayList<JsonPart> parts = new ArrayList<JsonPart>();
      List<JSONObject> jList = systemModel.getParts(jsonObj);
      for (JSONObject jObj : jList)
      {
         JsonPart part = (JsonPart)systemModel.wrap(jObj);
         parts.add(part);
      }
      return parts;
   }

   public JsonPart getPart(String name)
   {
      JSONObject jObj = systemModel.getPart(jsonObj, name);
      if (jObj != null)
      {
         return (JsonPart)systemModel.wrap(jObj);
      }
      return null;
   }

   public List<JsonValueProperty> getValueProperties()
   {
      ArrayList<JsonValueProperty> valueProps = new ArrayList<JsonValueProperty>();
      List<JSONObject> jList = systemModel.getValueProperties(jsonObj);
      for (JSONObject jObj : jList)
      {
         if (systemModel.isValueProperty(jObj))
         {
            JsonValueProperty valueProp = (JsonValueProperty)systemModel.wrap(jObj);
            valueProps.add(valueProp);
         }
      }
      return valueProps;
   }

   public JsonValueProperty getValueProperty(String name)
   {
      List<JSONObject> jList = systemModel.getValueProperties(jsonObj);
      for (JSONObject jObj : jList)
      {
         if (systemModel.isValueProperty(jObj))
         {
            if (name.equals(systemModel.getName(jObj)))
            {
               return (JsonValueProperty)systemModel.wrap(jObj);
            }
         }
      }
      return null;
   }

   public List<JsonParametricDiagram> getParametricDiagrams()
   {
      ArrayList<JsonParametricDiagram> parametricDiagrams = new ArrayList<JsonParametricDiagram>();
      List<JSONObject> jList = systemModel.getOwnedElements(jsonObj);

      for (JSONObject jObj : jList)
      {
         if (systemModel.isParametricDiagram(jObj))
         {
            JsonParametricDiagram parametricDiagram = (JsonParametricDiagram)systemModel.wrap(jObj);
            parametricDiagrams.add(parametricDiagram);
         }
      }
      return parametricDiagrams;
   }
}
