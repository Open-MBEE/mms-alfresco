package gov.nasa.jpl.sysml.json_impl;

import java.util.List;
import java.util.ArrayList;

import org.json.JSONObject;

public class JsonGraphEdge extends JsonGraphElement
{
   JsonGraphNode sourceNode;
   JsonGraphNode targetNode;

   public JsonGraphEdge(JsonSystemModel systemModel, JSONObject jObj, JsonGraphElement parent,
         JsonGraphNode sourceNode, JsonGraphNode targetNode)
   {
      super(systemModel, jObj, parent);
      this.sourceNode = sourceNode;
      this.targetNode = targetNode;
   }

   public JsonGraphNode getSource()
   {
      return sourceNode;
   }

   public List<JsonBaseElement> getSourceNestedSequence()
   {
      return sourceNode.getNestedElementSequence();
   }

   public JsonGraphNode getTarget()
   {
      return targetNode;
   }

   public List<JsonBaseElement> getTargetNestedSequence()
   {
      return targetNode.getNestedElementSequence();
   }
}
