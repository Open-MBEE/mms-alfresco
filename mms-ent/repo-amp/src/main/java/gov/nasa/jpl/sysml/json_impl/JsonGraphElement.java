package gov.nasa.jpl.sysml.json_impl;

import gov.nasa.jpl.mbee.util.CompareUtils;

import org.json.JSONObject;

public abstract class JsonGraphElement
{
   protected String id;
   protected JSONObject jsonObj;
   protected JsonSystemModel systemModel;
   protected JsonGraphElement parent;

   public JsonGraphElement(JsonSystemModel systemModel, String id, JsonGraphElement parent)
   {

      this.systemModel = systemModel;
      this.id = id;
      this.jsonObj = systemModel.getElement(id);
      this.parent = parent;
   }

   public JsonGraphElement(JsonSystemModel systemModel, JSONObject jObj, JsonGraphElement parent)
   {
      this.systemModel = systemModel;
      this.jsonObj = jObj;
      this.id = systemModel.getIdentifier(jObj);
      this.parent = parent;
   }

   public JsonBaseElement getModelElement()
   {
      return systemModel.wrap(jsonObj);
   }

   public JsonGraphElement getParent()
   {
      return parent;
   }

   public String getElementId()
   {
      return id;
   }

   @Override
   public boolean equals(Object o)
   {
      if (o == null)
         return false;

      if (o instanceof JsonGraphElement)
      {
         JsonGraphElement gOther = (JsonGraphElement) o;

         JsonBaseElement thisElem = getModelElement();
         JsonBaseElement otherElem = gOther.getModelElement();

         if (!thisElem.equals(otherElem))
         {
            return false;
         }

         JsonGraphElement gParent = getParent();
         JsonGraphElement gOtherParent = gOther.getParent();

         while (gParent != null)
         {
            if (gOtherParent == null)
            {
               return false;
            }
            JsonBaseElement parentElem = gParent.getModelElement();
            JsonBaseElement otherParentElem = gOtherParent.getModelElement();
            if (!parentElem.equals(otherParentElem))
            {
               return false;
            }

            gParent = gParent.getParent();
            gOtherParent = gOtherParent.getParent();
         }

         if (gOtherParent != null)
         {
            return false;
         }

         return true;
      }
      else
      {
         return false;
      }
   }

   @Override
   public int hashCode()
   {
      StringBuilder sb = new StringBuilder();

      sb.append(id);

      JsonGraphElement gParent = getParent();
      while (gParent != null)
      {
         sb.append(gParent.getElementId());
         gParent = gParent.getParent();
      }

      return sb.toString().hashCode();
   }

   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();

      sb.append(getModelElement().getName());

      JsonGraphElement parent = getParent();
      while (parent != null)
      {
         sb.append("=>");
         sb.append(parent.getModelElement().getName());
         parent = parent.getParent();
      }
      return sb.toString();
   }

}
