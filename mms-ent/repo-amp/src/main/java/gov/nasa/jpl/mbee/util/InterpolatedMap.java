/**
 *
 */
package gov.nasa.jpl.mbee.util;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import junit.framework.Assert;


/**
 * @author bclement
 *
 */
public class InterpolatedMap< K, V > extends TreeMap< K, V > {

    private static final long serialVersionUID = 7388614842922181155L;

    public static class Interpolation {
        protected static final byte STEP = 0; // value for key = get(floorKey(
                                              // key ))
        protected static final byte LINEAR = 1; // floorVal+(ceilVal-floorVal)*(key-floorKey)/(ceilKey-floorKey)
        protected static final byte RAMP = 2; // linear
        protected static final byte NONE = Byte.MAX_VALUE; // value for key =
                                                           // get(key)
        public byte type = STEP;

        public Interpolation() {}

        public Interpolation( byte type ) {
            this.type = type;
        }

        @Override
        public String toString() {
            switch ( type ) {
                case NONE:
                    return "NONE";
                case STEP:
                    return "STEP";
                case LINEAR:
                    return "LINEAR";
                case RAMP:
                    return "RAMP";
                default:
                    return null;
            }
        }

        public void fromString( String s ) {
            try {
                type = Byte.parseByte( s );
            } catch ( NumberFormatException e ) {
                // ignore
            }
            if ( s.toLowerCase().equals( "none" ) ) {
                type = NONE;
            } else if ( s.toLowerCase().equals( "step" ) ) {
                type = STEP;
            } else if ( s.toLowerCase().equals( "linear" ) ) {
                type = LINEAR;
            } else if ( s.toLowerCase().equals( "ramp" ) ) {
                type = RAMP;
            } else {
                Debug.error( true, "Can't parse interpolation string! " + s );
            }
        }
    }

    // Fields
    public Interpolation interpolation = new Interpolation();

    /**
     * @see {@link TreeMap}
     */
    public InterpolatedMap() {
        super();
    }

    /**
     * @see {@link TreeMap}
     * @param comparator
     */
    public InterpolatedMap( Comparator< ? super K > comparator ) {
        super( comparator );
    }

    /**
     * @see {@link TreeMap}
     * @param m
     */
    public InterpolatedMap( Map< ? extends K, ? extends V > m ) {
        super( m );
    }

    /**
     * @see {@link TreeMap}
     * @param m
     */
    public InterpolatedMap( SortedMap< K, ? extends V > m ) {
        super( m );
    }

    @Override
    public boolean containsKey( Object k ) {
        if ( super.containsKey( k ) ) return true;
        K key = null;
        try {
            key = (K)k;
        } catch (ClassCastException e) {
            e.printStackTrace();
            return false;
        }
        if ( size() < 2 ) return false;
        if ( interpolation.type == Interpolation.STEP ||
             interpolation.type == Interpolation.LINEAR ||
             interpolation.type == Interpolation.RAMP ) {
             //Set< java.util.Map.Entry< K, V > > entries = this.entrySet();
            K first = this.firstKey();
            if ( first instanceof Comparable ) {
                if ( ( (Comparable)first ).compareTo( key ) != -1 ) return false;
                K last = this.lastKey();
                if ( last instanceof Comparable ) {
                    if ( ( (Comparable)last ).compareTo( key ) != 1 ) return false;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsValue( Object val ) {
        if ( super.containsValue( val ) ) return true;
        V value = null;
        try {
            value = (V)val;
        } catch (ClassCastException e) {
            e.printStackTrace();
            return false;
        }
        if ( interpolation.type == Interpolation.LINEAR ||
                interpolation.type == Interpolation.RAMP ) {
            boolean before = true;
            boolean first = true;
            for ( V v : values() ) {
                boolean lastBefore = before;
                if ( v instanceof Comparable ) {
                    int comp = ((Comparable)v).compareTo( value );
                    before = comp == -1;
                    if ( first ) {
                        first = false;
                    } else {
                        if ( before != lastBefore || comp == 0 ) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public K getKeyBefore( K t ) {
        if ( t == null ) return null;
        K k = this.lowerKey( t );
        return k;
    }

    public V getValueBefore( K t ) {
        if ( t == null ) return null;
        K justBeforeKey = getKeyBefore( t );
        V valBefore = null;
        if ( justBeforeKey != null ) {
          valBefore = get( justBeforeKey );
        }
        return valBefore;
      }

    public K getKeyAfter( K t ) {
        if ( t == null ) return null;
        K k = this.higherKey( t );
        return k;
    }

    @Override
    public V get( Object k ) {
        K t = null;
        try {
            t = (K)k;
        } catch ( ClassCastException e ) {
            e.printStackTrace();
        }
        if ( t == null ) return null;

        NavigableMap< K, V > m = headMap( t, true );
        if ( !m.isEmpty() ) {
            Entry< K, V > e = m.lastEntry();
            if ( e.getKey().equals( t ) ) {
                return m.lastEntry().getValue();
            }
        }
        V v1 = null, v2 = null;
        if ( interpolation.type == Interpolation.STEP ) {
            if ( !m.isEmpty() ) {
                v1 = m.lastEntry().getValue();
            }
            if ( Debug.isOn() ) {
                v2 = getValueBefore( t );
                Assert.assertEquals( v1, v2 );
            }
            return v1; //
        } else if ( interpolation.type == Interpolation.NONE ) {
            return null;
        } else if ( interpolation.type == Interpolation.LINEAR ) {
            K t1 = null;
            if ( !m.isEmpty() ) {
                t1 = m.lastEntry().getKey();
                v1 = m.lastEntry().getValue();
            }
            if ( Debug.isOn() ) {
                Assert.assertEquals( t1, getKeyBefore( t ) );
                Assert.assertEquals( v1, get( t1 ) );
                Debug.outln( "getValue() change looks good." );
            }
            K t2 = getKeyAfter( t );
            // v1 = get( t1 );
            if ( t1.equals( t2 ) ) return v1;
            v2 = get( t2 );
            if ( v1 == null ) return null;
            if ( v2 == null ) return v1;
            // floorVal+(ceilVal-floorVal)*(key-floorKey)/(ceilKey-floorKey)
            // v1 = v1 + ( ( v2 - v1 ) * ( t - t1 ) ) / ( t2 - t1 );
            // TODO -- try to parse numbers from toString() if not Numbers
            if ( v1 instanceof Number && v2 instanceof Number
                 && t instanceof Number && t1 instanceof Number
                 && t2 instanceof Number ) {
                double nv1 = ( (Number)v1 ).doubleValue();
                double nv2 = ( (Number)v2 ).doubleValue();
                double nt = ( (Number)t ).doubleValue();
                double nt1 = ( (Number)t1 ).doubleValue();
                double nt2 = ( (Number)t2 ).doubleValue();
                nv1 = nv1 + ( ( nv2 - nv1 ) * ( nt - nt1 ) ) / ( nt2 - nt1 );
                Number result = nv1;
                // TODO -- BAE handles casting Numbers somewhere
                // (Expression.evaluate or Functions). Make this generic and put
                // in Util.
                // TODO -- Not handling BigDecimal and others.
                if ( v1 instanceof Double ) {
                    v1 = (V)(Double)result.doubleValue();
                } else if ( v1 instanceof Float ) {
                    v1 = (V)(Float)result.floatValue();
                } else if ( v1 instanceof Long ) {
                    v1 = (V)(Long)result.longValue();
                } else if ( v1 instanceof Integer ) {
                    v1 = (V)(Integer)result.intValue();
                } else if ( v1 instanceof Byte ) {
                    v1 = (V)(Byte)result.byteValue();
                } else if ( v1 instanceof Short ) {
                    v1 = (V)(Short)result.shortValue();
                } else {
                    Debug.error( true,
                                 "InterpolatedMap.get(): not supporting Number class "
                                         + v1.getClass().getSimpleName()
                                         + " for " + interpolation.type + "!" );
                    v1 = null;
                }
            }

            return v1;
        }
        Debug.error( true, "InterpolatedMap.get(): invalid key or value for "
                           + interpolation.type + " -- must be Numbers!" );
        return null;
        // // TODO Auto-generated method stub
        // return super.get( t );
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub

    }

}
