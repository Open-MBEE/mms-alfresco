package gov.nasa.jpl.mbee.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

// Number subclasses: BigDecimal, BigInteger, Byte, Double, Float, Integer, Long, and Short
public class NegativeInfinity extends ConstantNumber {
    private static final long serialVersionUID = -3805573505903953839L;
    
    public static final NegativeInfinity negInf = new NegativeInfinity();
    public static final HashMap< Class< ? >, Number > valueMap =
            ConstantNumber.constructValueMap( negInf );

    @Override
    public int intValue() {
        return Integer.MIN_VALUE;
    }

    @Override
    public long longValue() {
        return Long.MIN_VALUE;
    }

    @Override
    public float floatValue() {
        return -Float.MAX_VALUE;
    }

    @Override
    public double doubleValue() {
        return -Double.MAX_VALUE;
    }

    @Override
    public short shortValue() {
        return Short.MIN_VALUE;
    }

    public static boolean isEqual( Object o ) {
        return negInf.equals( o );
    }

    @Override
    public int compareTo( Number n ) {
        if ( n == null ) return 1;
        if ( equals( n ) ) return 0;
        return -1;
    }

    protected static BigDecimal bigDecimalNegInf =
            new BigDecimal( -Double.MAX_VALUE );

    @Override
    public BigDecimal bigDecimalValue() {
        return bigDecimalNegInf;
    }

    protected static BigInteger bigIntegerNegInf =
            new BigInteger( "" + Long.MIN_VALUE );

    @Override
    public BigInteger bigIntegerValue() {
        return bigIntegerNegInf;
    }

    @Override
    protected HashMap< Class< ? >, Number > getValueMap() {
        return valueMap;
    }

    @Override
    public ConstantNumber getInstance() {
        return negInf;
    }

    public static < T > T forClass( Class< T > cls ) throws ClassCastException {
        return ConstantNumber.forClass( negInf, cls );
    }

}