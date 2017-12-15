package gov.nasa.jpl.mbee.util;

import java.math.BigDecimal;
import java.math.BigInteger;

// Number subclasses: BigDecimal, BigInteger, Byte, Double, Float, Integer, Long, and Short
public abstract class Negative<T extends ConstantNumber> extends ConstantNumber {

    private static final long serialVersionUID = -7834316126633558122L;

    //public static final Negative< T > negative = null;//new Negative<T>();
    //protected static T number;
//    private static Negative< T > getNegative() {
//        if ( negative == null ) {
//            if ( number == null ) return null;
//            negative = new Negative< ConstantNumber >( number );
//        }
//        return negative;
//    }

//    public HashMap< Class< ? >, Number > valueMap = null;
//            //ConstantNumber.constructValueMap( one );

    protected T number;
    
    public Negative( T number ) {
        super();
        this.number = number;
//        valueMap = constructValueMap( this );
    }
    
    @Override
    public int intValue() {
        return -number.intValue();
    }

    @Override
    public long longValue() {
        return -number.longValue();
    }

    @Override
    public float floatValue() {
        return -number.floatValue();
    }

    @Override
    public double doubleValue() {
        return -number.doubleValue();
    }

    @Override
    public short shortValue() {
        return (short)-number.shortValue();
    }

//    public static boolean isEqual( Object o ) {
//        return this.equals( o );
//    }

//    protected static BigDecimal bigDecimalOne = new BigDecimal( 1 );

    @Override
    public BigDecimal bigDecimalValue() {
        return number.bigDecimalValue().negate();
    }

    @Override
    public BigInteger bigIntegerValue() {
        return number.bigIntegerValue().negate();
    }

//    @Override
//    protected HashMap< Class< ? >, Number > getValueMap() {
//        if ( valueMap == null ) {
//            valueMap = ConstantNumber.constructValueMap( this );
//        }
//        return valueMap;
//    }
//
//    @Override
//    public ConstantNumber getInstance() {
//        return this;
//    }
}