package gov.nasa.jpl.mbee.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public interface HasPreference< T > {
    public boolean prefer( T t1, T t2 );
    public int rank( T t );
    
    
    public static class Helper< T > implements HasPreference< T > {

        final List< T > totalOrder;
        HashMap< T, Integer > rank = new HashMap< T, Integer >();
        
        public static boolean classHasPreference( Class<?> cls ) {
            boolean hasPreference = false;
            for ( Class<?> i : cls.getInterfaces() ) {
              if ( HasPreference.class.isAssignableFrom( i ) ) {
                hasPreference = true;
                break;
              }
            }
            return hasPreference;
        }
        
        public Helper( List< T > totalOrder ) {
            this.totalOrder = totalOrder;
            if ( totalOrder != null ) {
                int ct = 0;
                for ( T t : totalOrder ) {
                    rank.put( t, ct++ );
                }
            }
        }
        
        @Override
        public boolean prefer( T t1, T t2 ) {
            boolean preferred = rank.get( t1 ) < rank.get( t2 );
            return preferred;
        }
        
        public int rank( T t ) {
            Integer r = rank.get( t );
            if ( r == null ) {
                // Try to match another way.
                
                // check if classes
                Class<?> tClass = (t instanceof Class) ? (Class<?>)t : null;
                Class<?>[] tClasses = (Class< ? >[])( ((t instanceof Collection) && ClassUtils.areClasses( (Collection<?>)t )) ? ((Collection<?>)t).toArray() : null );
                if ( tClasses == null ) {
                    tClasses = (Class< ? >[])( (t.getClass().isArray() && ClassUtils.areClasses( (Object[])t )) ? (Object[])t : null );
                }
                for ( T tc : totalOrder ) {
                    boolean equal = false;
                    if ( tClass != null && ( tc instanceof Class ) ) {
                        equal = ( (Class< ? >)tc ).isAssignableFrom(tClass);
                    } else if ( tClasses != null ) {
                        Class<?>[] tcClasses = (Class< ? >[])( ((tc instanceof Collection) && ClassUtils.areClasses( (Collection<?>)tc )) ? ((Collection<?>)tc).toArray() : null );
                        if ( tcClasses == null ) {
                            tcClasses = (Class< ? >[])( (tc.getClass().isArray() && ClassUtils.areClasses( (Object[])tc )) ? (Object[])tc : null );
                        }
                        if ( tcClasses != null ) equal = ClassUtils.classesMatch( tcClasses, tClasses );
                        else equal = Utils.valuesLooselyEqual( t, tc, false );
                    } else equal = Utils.valuesLooselyEqual( t, tc, false ); 
                    if ( equal ) {
                        if ( rank.containsKey( tc ) ) {
                            int rr = rank( tc );
                            if ( r == null || rr < r ) {
                                r = rr;
                                // remember rank
                                rank.put( t, r );
                                break; // We can assume that the first match
                                       // will have the highest rank since it's
                                       // a total order.
                            }
                        }
                    }
                }
            }
            if ( r != null ) return r.intValue();
            return Integer.MAX_VALUE;
        }
        
    }
}
