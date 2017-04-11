/**
 *
 */
package gov.nasa.jpl.sysml.json_impl;

import gov.nasa.jpl.sysml.Connector;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;


/**
 *
 */
public class JsonBindingConnector extends JsonBaseElement implements Connector<String, String, Date>
{
   private final static Logger LOGGER = Logger.getLogger(JsonBindingConnector.class.getName());

   public JsonBindingConnector(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public JsonBaseElement getTarget()
   {
      // TODO: target is null for binding connector in JSON.
      // For now use target path instead.
      List<JsonBaseElement> pathElems = getTargetPath();
      if (pathElems.size() > 0)
      {
         return pathElems.get(pathElems.size()-1);
      }
      return null;
   }

   public JsonBaseElement getSource()
   {
      // TODO: source is null for binding connector in JSON.
      // For now use source path instead.
      List<JsonBaseElement> pathElems = getSourcePath();
      if (pathElems.size() > 0)
      {
         return pathElems.get(pathElems.size()-1);
      }
      return null;
   }

   public List<JsonBaseElement> getTargetPath()
   {
      List<JsonBaseElement> pathElems = new ArrayList<JsonBaseElement>();
      List<JSONObject> jPathElems = systemModel.getTargetPath(jsonObj);
      for (JSONObject jPathElem : jPathElems)
      {
         pathElems.add(systemModel.wrap(jPathElem));
      }

      return pathElems;
   }

   public List<JsonBaseElement> getSourcePath()
   {
      List<JsonBaseElement> pathElems = new ArrayList<JsonBaseElement>();
      List<JSONObject> jPathElems = systemModel.getSourcePath(jsonObj);
      for (JSONObject jPathElem : jPathElems)
      {
         pathElems.add(systemModel.wrap(jPathElem));
      }

      return pathElems;
   }
}
