/**
 *
 */
package gov.nasa.jpl.sysml.json_impl;

import org.json.JSONObject;

/**
 *
 */
public class JsonValueType extends JsonElement
{
   public JsonValueType(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   public String getUnit()
   {
      return getTagValue(JsonSystemModel.UNIT);
   }

   public String getQuantityKind()
   {
      return getTagValue(JsonSystemModel.QUANTITY_KIND);
   }
}
