package gov.nasa.jpl.mbee.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

// Number subclasses: BigDecimal, BigInteger, Byte, Double, Float, Integer, Long, and Short
public class One extends ConstantNumber {

    private static final long serialVersionUID = -7834316126633558122L;

    public static final One one = new One();
    public static final HashMap< Class< ? >, Number > valueMap =
            ConstantNumber.constructValueMap( one );

    @Override
    public int intValue() {
        return 1;
    }

    @Override
    public long longValue() {
        return 1;
    }

    @Override
    public float floatValue() {
        return 1.0f;
    }

    @Override
    public double doubleValue() {
        return 1.0;
    }

    @Override
    public short shortValue() {
        return 1;
    }

    public static boolean isEqual( Object o ) {
        return one.equals( o );
    }

    protected static BigDecimal bigDecimalOne = new BigDecimal( 1 );

    @Override
    public BigDecimal bigDecimalValue() {
        return bigDecimalOne;
    }

    protected static BigInteger bigIntegerOne = new BigInteger( "1" );

    @Override
    public BigInteger bigIntegerValue() {
        return bigIntegerOne;
    }

    @Override
    protected HashMap< Class< ? >, Number > getValueMap() {
        return valueMap;
    }

    @Override
    public ConstantNumber getInstance() {
        return one;
    }

    public static < T > T forClass( Class< T > cls ) throws ClassCastException {
        return ConstantNumber.forClass( one, cls );
    }
}