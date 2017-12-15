package gov.nasa.jpl.sysml.json_impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

import org.json.JSONObject;

public class JsonParametricDiagram extends JsonBaseElement
{
   private final static Logger LOGGER = Logger.getLogger(JsonParametricDiagram.class.getName());

   public JsonParametricDiagram(JsonSystemModel systemModel, JSONObject jObj)
   {
      super(systemModel, jObj);
   }

   public static void setLogLevel(Level level)
   {
      LOGGER.setLevel(level);
   }

   public List<JsonBindingConnector> getBindingConnectors()
   {
      List<JsonBindingConnector> connectors = new ArrayList<JsonBindingConnector>();
      List<JSONObject> jList = getBindingConnectorsInternal();
      for (JSONObject jObj : jList)
      {
         JsonBaseElement bObj = systemModel.wrap(jObj);
         if (bObj instanceof JsonBindingConnector)
         {
            connectors.add((JsonBindingConnector)bObj);
         }
         else
         {
            LOGGER.log(Level.WARNING, "Unexpected element for binding connector property: %s", jObj);
         }
      }

      return connectors;
   }

   public List<JsonConstraintProperty> getConstraintProperties()
   {
      List<JsonConstraintProperty> props = new ArrayList<JsonConstraintProperty>();
      List<JSONObject> jList = getConstraintPropertiesInternal();
      for (JSONObject jObj : jList)
      {
         JsonBaseElement bObj = systemModel.wrap(jObj);
         if (bObj instanceof JsonConstraintProperty)
         {
            props.add((JsonConstraintProperty)bObj);
         }
         else
         {
            LOGGER.log(Level.WARNING, "Unexpected element for constraint property: %s", jObj);
         }
      }

      return props;
   }

   public Collection<JsonBaseElement> getElementsInDiagram()
   {
      return systemModel.getDiagramElements(jsonObj);
   }

   public Collection<JsonGraphElement> getGraphicalElements()
   {
      return systemModel.getDiagramGraphElements(jsonObj);
   }

   public JSONObject getContextBlock()
   {
      JSONObject owner = systemModel.getOwner(jsonObj);
      if (systemModel.isBlock(owner))
      {
         return owner;
      }
      else
      {
         LOGGER.log(Level.WARNING, "Owner of parametric diagram is not a block: %s", jsonObj);
         return null;
      }
   }

   public boolean hasContextBlock()
   {
      return getContextBlock() != null;
   }

   protected List<JSONObject> getBindingConnectorsInternal()
   {
      ArrayList<JSONObject> bindingConnectors = new ArrayList<JSONObject>();

      List<JSONObject> diagramElems = systemModel.getParametricDiagramElements(jsonObj);

      for (JSONObject diagramElem : diagramElems)
      {
         if(systemModel.isBindingConnector(diagramElem))
         {
            bindingConnectors.add(diagramElem);
         }
      }
      return bindingConnectors;
   }

   protected List<JSONObject> getConstraintPropertiesInternal()
   {
      ArrayList<JSONObject> constraintProps = new ArrayList<JSONObject>();

      List<JSONObject> diagramElems = systemModel.getParametricDiagramElements(jsonObj);

      for (JSONObject diagramElem : diagramElems)
      {
         if(systemModel.isConstraintProperty(diagramElem))
         {
            constraintProps.add(diagramElem);
         }
      }
      return constraintProps;
   }
}
