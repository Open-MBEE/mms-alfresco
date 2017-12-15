package gov.nasa.jpl.sysml;

import java.util.Collection;

public interface Constraint< N, I, D > extends Element< N, I, D > {
    boolean evaluate();
    boolean isViolated();  // This should be the same as !evaluate()
    boolean resolve();
    Collection<? extends Element< N, I, D > > getElements();
}
