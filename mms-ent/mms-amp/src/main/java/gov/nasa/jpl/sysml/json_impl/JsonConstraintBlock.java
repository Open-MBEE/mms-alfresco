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
public class JsonConstraintBlock extends JsonElement
{
   private final static Logger LOGGER = Logger.getLogger(JsonConstraintBlock.class.getName());

   public JsonConstraintBlock(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public JsonConstraintParameter getConstraintParameter(String name)
   {
      JSONObject jObj = systemModel.getConstraintParameter(jsonObj, name);
      if (jObj != null)
      {
         return (JsonConstraintParameter) systemModel.wrap(jObj);
      }
      return null;
   }

   public List<JsonConstraintParameter> getConstraintParameters()
   {
      ArrayList<JsonConstraintParameter> parameters = new ArrayList<JsonConstraintParameter>();
      List<JSONObject> jList = systemModel.getConstraintParameters(jsonObj);
      for (JSONObject jObj : jList)
      {
         JsonBaseElement bObj = systemModel.wrap(jObj);
         if (bObj instanceof JsonConstraintParameter)
         {
            parameters.add((JsonConstraintParameter)bObj);
         }
         else
         {
            LOGGER.log(Level.WARNING, "Unexpected element for constraint property: %s", jObj);
         }
      }
      return parameters;
   }
}
