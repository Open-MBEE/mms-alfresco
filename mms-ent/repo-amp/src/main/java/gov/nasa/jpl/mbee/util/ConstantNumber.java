package gov.nasa.jpl.mbee.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

public abstract class ConstantNumber extends Number implements Comparable< Number > {
    private static final long serialVersionUID = -8761617466495267383L;

    abstract protected HashMap<Class<?>, Number> getValueMap();
    abstract public ConstantNumber getInstance();
    abstract public BigDecimal bigDecimalValue();
    abstract public BigInteger bigIntegerValue();

    
    public ConstantNumber() {
        super();
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null ) return false;
        Number infVal = getValueMap().get( o.getClass() );
        if ( infVal == null ) return false;
        if ( infVal.equals( o ) ) return true;
        return false;
      }
    
    @Override
    public int compareTo( Number n ) {
        if ( n == null ) return 1;
        if ( n instanceof BigDecimal ) {
            return ( (BigDecimal)n ).compareTo( bigDecimalValue() );
        }
        if ( n instanceof BigInteger ) {
            return ( (BigInteger)n ).compareTo( bigIntegerValue() );
        }
        return CompareUtils.compare( doubleValue(), n.doubleValue() );
    }

    protected static HashMap< Class< ? >, Number > constructValueMap( ConstantNumber n ) {
        HashMap< Class< ? >, Number > valueMap =
                new HashMap< Class< ? >, Number >();
        valueMap.put( Integer.class, n.intValue() );
        valueMap.put( Long.class, n.longValue() );
        valueMap.put( Float.class, n.floatValue() );
        valueMap.put( Double.class, n.doubleValue() );
        valueMap.put( Short.class, n.shortValue() );
        valueMap.put( BigDecimal.class, n.bigDecimalValue() );
        valueMap.put( BigInteger.class, n.bigIntegerValue() );

        valueMap.put( int.class, n.intValue() );
        valueMap.put( long.class, n.longValue() );
        valueMap.put( float.class, n.floatValue() );
        valueMap.put( double.class, n.doubleValue() );
        valueMap.put( short.class, n.shortValue() );
        return valueMap;
    }

    public static < T > T forClass( ConstantNumber constant, Class< T > cls )
                                                               throws ClassCastException {
        if ( cls == null ) {
            throw new ClassCastException( "Cannot pass null into "
                                          + constant.getClass()
                                          + ".forClass(Class<T>)." );
        }
        Number n = null;
        Class< ? > pc = cls;
        while ( pc != null && !pc.equals( Object.class )
                && !Number.class.isAssignableFrom( pc ) ) {
            pc = pc.getSuperclass();
        }
        if ( pc != null && !Number.class.isAssignableFrom( pc ) ) {
            Class< ? extends Number > c = pc.asSubclass( Number.class );
            n = constant.getValueMap().get( c );
        }
        return cls.cast( n );
    }
}