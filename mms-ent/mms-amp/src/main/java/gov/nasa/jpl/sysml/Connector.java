/**
 *
 */
package gov.nasa.jpl.sysml;

import java.util.List;

import gov.nasa.jpl.sysml.json_impl.JsonBaseElement;

/**
 *
 */
public interface Connector<N, I, D> extends BaseElement<N, I, D> {

   JsonBaseElement getTarget();
   JsonBaseElement getSource();
   List<? extends JsonBaseElement> getTargetPath();
   List<? extends JsonBaseElement> getSourcePath();

}
