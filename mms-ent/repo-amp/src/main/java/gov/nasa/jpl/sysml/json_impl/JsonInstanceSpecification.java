package gov.nasa.jpl.sysml.json_impl;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

public class JsonInstanceSpecification extends JsonBaseElement
{
   private final static Logger LOGGER = Logger.getLogger(JsonInstanceSpecification.class.getName());

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public JsonInstanceSpecification(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   public JsonElement getClassifier()
   {
      List<String> classifierIds = systemModel.getClassifierIds(jsonObj);

      if (classifierIds.size() > 0)
      {
         if (classifierIds.size() > 1)
         {
            LOGGER.log(Level.WARNING, "More than one classifier is defined for : {0}", id);
         }
         String classifierId = classifierIds.get(0);
         JSONObject jTypeObj = systemModel.getElement(classifierId);
         JsonBaseElement typeObj = systemModel.wrap(jTypeObj);

         if (typeObj instanceof JsonElement)
         {
            return (JsonElement) typeObj;
         }
         else
         {
            LOGGER.log(Level.WARNING, "Classifier of an instance specification is not a JsonElement: {0}", typeObj);
         }
      }
      return null;
   }

   public List<JsonSlot> getSlots()
   {
      ArrayList<JsonSlot> slots = new ArrayList<JsonSlot>();
      List<JSONObject> jList = systemModel.getSlots(jsonObj);
      for (JSONObject jObj : jList)
      {
         JsonSlot slot = (JsonSlot)systemModel.wrap(jObj);
         slots.add(slot);
      }

      return slots;
   }

   public JsonSlot findContainingSlot()
   {
      JSONObject jSlot = systemModel.getContainingSlot(jsonObj);

      if (jSlot != null)
      {
         return (JsonSlot) systemModel.wrap(jSlot);
      }
      return null;
   }

   public JsonInstanceSpecification findOwningInstanceSpecification()
   {
      JsonSlot slot = findContainingSlot();
      if (slot != null)
      {
         JsonBaseElement owner = slot.getOwner();
         if (owner instanceof JsonInstanceSpecification)
         {
            return (JsonInstanceSpecification) owner;
         }
      }
      return null;
   }
}
