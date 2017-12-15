package gov.nasa.jpl.mbee.util;

import java.util.HashMap;

public class NegativeOne extends Negative< One > {
    private static final long serialVersionUID = 8821562579555839417L;
    public static final NegativeOne negOne = new NegativeOne( One.one );
    public static final HashMap< Class< ? >, Number > valueMap =
            ConstantNumber.constructValueMap( negOne );

    public NegativeOne( gov.nasa.jpl.mbee.util.One number ) {
        super( number );
    }
    public static boolean isEqual( Object o ) {
        return negOne.equals( o );
    }
    @Override
    public ConstantNumber getInstance() {
        return negOne;
    }
    public static < T > T forClass( Class< T > cls ) throws ClassCastException {
        return ConstantNumber.forClass( negOne, cls );
    }
    @Override
    protected HashMap< Class< ? >, Number > getValueMap() {
        return valueMap;
    }
}