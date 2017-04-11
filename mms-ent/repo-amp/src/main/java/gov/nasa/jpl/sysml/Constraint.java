package gov.nasa.jpl.sysml;

import java.util.Collection;

public interface Constraint< N, I, D > extends Element< N, I, D > {
    public boolean evaluate();
    public boolean isViolated();  // This should be the same as !evaluate()
    public boolean resolve();
    public Collection<? extends Element< N, I, D > > getElements();
}
