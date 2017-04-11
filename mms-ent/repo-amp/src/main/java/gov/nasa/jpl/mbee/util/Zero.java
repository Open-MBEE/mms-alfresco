package gov.nasa.jpl.mbee.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

// Number subclasses: BigDecimal, BigInteger, Byte, Double, Float, Integer, Long, and Short
public class Zero extends ConstantNumber {
    private static final long serialVersionUID = -2927443825901952093L;

    public static final Zero zero = new Zero();
    public static final HashMap< Class< ? >, Number > valueMap =
            ConstantNumber.constructValueMap( zero );

    @Override
    public int intValue() {
        return 0;
    }

    @Override
    public long longValue() {
        return 0;
    }

    @Override
    public float floatValue() {
        return 0.0f;
    }

    @Override
    public double doubleValue() {
        return 0.0;
    }

    @Override
    public short shortValue() {
        return 0;
    }

    public static boolean isEqual( Object o ) {
        return zero.equals( o );
    }

    protected static BigDecimal bigDecimalZero = new BigDecimal( 0 );

    @Override
    public BigDecimal bigDecimalValue() {
        return bigDecimalZero;
    }

    protected static BigInteger bigIntegerZero = new BigInteger( "0" );

    @Override
    public BigInteger bigIntegerValue() {
        return bigIntegerZero;
    }

    @Override
    protected HashMap< Class< ? >, Number > getValueMap() {
        return valueMap;
    }

    @Override
    public ConstantNumber getInstance() {
        return zero;
    }

    public static < T > T forClass( Class< T > cls ) throws ClassCastException {
        return ConstantNumber.forClass( zero, cls );
    }
}