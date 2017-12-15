package gov.nasa.jpl.sysml.json_impl;

import java.util.List;
import java.util.ArrayList;

import org.json.JSONObject;

public class JsonGraphNode extends JsonGraphElement
{
   public JsonGraphNode(JsonSystemModel systemModel, JSONObject jObj, JsonGraphElement parent)
   {
      super(systemModel, jObj, parent);
   }

   public List<JsonBaseElement> getNestedElementSequence()
   {
      ArrayList<JsonBaseElement> elems = new ArrayList<JsonBaseElement>();

      elems.add(0, systemModel.wrap(jsonObj));

      JsonGraphElement p = getParent();
      while (p != null)
      {
         elems.add(0, p.getModelElement());
      }

      return elems;
   }

}
