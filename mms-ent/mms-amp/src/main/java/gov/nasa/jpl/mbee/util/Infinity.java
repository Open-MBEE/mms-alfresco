package gov.nasa.jpl.mbee.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

// Number subclasses: BigDecimal, BigInteger, Byte, Double, Float, Integer, Long, and Short
public class Infinity extends ConstantNumber {
    private static final long serialVersionUID = 337798366684971505L;

    public static final Infinity inf = new Infinity();
    public static final HashMap< Class< ? >, Number > valueMap =
            ConstantNumber.constructValueMap( inf );

    @Override
    public int intValue() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long longValue() {
        return Long.MAX_VALUE;
    }

    @Override
    public float floatValue() {
        return Float.MAX_VALUE;
    }

    @Override
    public double doubleValue() {
        return Double.MAX_VALUE;
    }

    @Override
    public short shortValue() {
        return Short.MAX_VALUE;
    }

    public static boolean isEqual( Object o ) {
        return inf.equals( o );
    }

    @Override
    public int compareTo( Number n ) {
        if ( n == null ) return 1;
        if ( equals( n ) ) return 0;
        return 1;
    }

    protected static BigDecimal bigDecimalInf =
            new BigDecimal( Double.MAX_VALUE );

    @Override
    public BigDecimal bigDecimalValue() {
        return bigDecimalInf;
    }

    protected static BigInteger bigIntegerInf =
            new BigInteger( "" + Long.MAX_VALUE );

    @Override
    public BigInteger bigIntegerValue() {
        return bigIntegerInf;
    }

    @Override
    protected HashMap< Class< ? >, Number > getValueMap() {
        return valueMap;
    }

    @Override
    public ConstantNumber getInstance() {
        return inf;
    }
    
    public static <T> T forClass(Class<T> cls) throws ClassCastException {
        return ConstantNumber.forClass( inf, cls );
    }

}