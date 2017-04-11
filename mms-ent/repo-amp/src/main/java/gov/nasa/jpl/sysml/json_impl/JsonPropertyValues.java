package gov.nasa.jpl.sysml.json_impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonPropertyValues
{
   // TODO: add integer
   public final static String LITERAL_REAL = "LiteralReal";
   public final static String LITERAL_STRING = "LiteralString";
   public final static String LITERAL_BOOLEAN = "LiteralBoolean";
   public final static String ELEMENT_VALUE = "ElementValue";
   public final static String INSTANCE_VALUE = "InstanceValue";

   public final static String DOUBLE = "double";
   public final static String STRING = "string";
   public final static String BOOLEAN = "boolean";
   public final static String ELEMENT = "element";
   public final static String INSTANCE = "instance";

   private final static Logger LOGGER = Logger.getLogger(JsonPropertyValues.class.getName());

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   protected JsonSystemModel systemModel;
   protected JSONArray jsonArray;

   public JsonPropertyValues( JsonSystemModel systemModel, JSONArray jArray )
   {
      this.systemModel = systemModel;
      this.jsonArray = jArray;
   }

   public int getLength()
   {
      return jsonArray.length();
   }

   public String getValueType()
   {
      if (getLength() == 0)
      {
         return null;
      }

      JSONObject jValue = jsonArray.getJSONObject(0);
      return jValue.getString(JsonSystemModel.TYPE);
   }

   @SuppressWarnings("rawtypes")
   public java.lang.Class getValueClass()
   {
      String type = getValueType();
      if (type == null)
      {
         return null;
      }

      if (type.equals(LITERAL_REAL))
      {
         return Double.class;
      }
      else if (type.equals(LITERAL_STRING))
      {
         return String.class;
      }
      else if (type.equals(LITERAL_BOOLEAN))
      {
         return Boolean.class;
      }
      else if (type.equals(ELEMENT_VALUE))
      {
         return JSONObject.class;
      }
      else if (type.equals(INSTANCE_VALUE))
      {
         return JSONObject.class;
      }
      return null;
   }

   public Object getValue(int i)
   {
      String type = getValueType();
      if (type == null)
      {
         return null;
      }

      JSONObject jValue = jsonArray.getJSONObject(i);
      if (type.equals(LITERAL_REAL))
      {
         return Double.valueOf(jValue.getDouble(DOUBLE));
      }
      else if (type.equals(LITERAL_STRING))
      {
         return jValue.getString(STRING);
      }
      else if (type.equals(LITERAL_BOOLEAN))
      {
         return Boolean.valueOf(jValue.getBoolean(BOOLEAN));
      }
      else if (type.equals(ELEMENT_VALUE))
      {
         if (jValue.isNull(ELEMENT))
         {
            LOGGER.log(Level.WARNING, "Element property value is null: %s", jsonArray);
            return null;
         }
         else
         {
            return systemModel.getElement(jValue.getString(ELEMENT));
         }
      }
      else if (type.equals(INSTANCE_VALUE))
      {
         if (jValue.isNull(INSTANCE))
         {
            LOGGER.log(Level.WARNING, "Element property value is null: %s", jsonArray);
            return null;
         }
         else
         {
            return systemModel.getElement(jValue.getString(INSTANCE));
         }
      }

      LOGGER.log(Level.WARNING, "unknown property value type: %s", jsonArray);

      return null;
   }

   public String getValueAsString(int i)
   {
      String type = getValueType();
      if (type == null)
      {
         return "";
      }

      JSONObject jValue = jsonArray.getJSONObject(i);
      if (type.equals(LITERAL_REAL))
      {
         return "" + jValue.getDouble(DOUBLE);
      }
      else if (type.equals(LITERAL_STRING))
      {
         return jValue.getString(STRING);
      }
      else if (type.equals(LITERAL_BOOLEAN))
      {
         return "" + jValue.getBoolean(BOOLEAN);
      }
      else if (type.equals(ELEMENT_VALUE))
      {
         // return element id
         return jValue.getString(ELEMENT);
      }
      else if (type.equals(INSTANCE_VALUE))
      {
         // return element id
         return jValue.getString(INSTANCE);
      }

      LOGGER.log(Level.WARNING, "unknown property value type: %s", jsonArray);

      return "";
   }
}
