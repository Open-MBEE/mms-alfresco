/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory,
 *    nor the names of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.nasa.jpl.mbee.util;

//import gov.nasa.jpl.ae.event.Expression;
//import gov.nasa.jpl.ae.event.Timepoint;
//import gov.nasa.jpl.ae.solver.Random;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.apache.commons.lang.StringUtils;

/**
 * A set of miscellaneous utility functions.
 */
public class Utils {

  // static members

  public static ClassLoader loader = null;

  // empty collection constants
  public static final List<?> emptyList = Collections.EMPTY_LIST;//new ArrayList( 0 );
  public static final ArrayList<?> emptyArrayList = new ArrayList( 0 );
  @SuppressWarnings( "unchecked" )
  public static <T> List<T> getEmptyList() {
    return (List< T >)emptyList;
  }
  @SuppressWarnings( "unchecked" )
  public static < T > ArrayList< T > getEmptyArrayList() {
    return (ArrayList< T >)emptyArrayList;
  }
  @SuppressWarnings( "unchecked" )
  public static <T> List<T> getEmptyList(Class<T> cls) {
    return (List< T >)emptyList;
  }
  @SuppressWarnings( "unchecked" )
  public static <T> ArrayList<T> getEmptyArrayList(Class<T> cls) {
    return (ArrayList< T >)emptyArrayList;
  }

  public static final Set<?> emptySet = Collections.EMPTY_SET;//new TreeSet();
  @SuppressWarnings( "unchecked" )
  public static <T> Set<T> getEmptySet() {
    return (Set< T >)emptySet;
  }
  public static <T> Set<T> getEmptySetOfType(Class<T> cls) {
      return (Set< T >)emptySet;
    }
  public static final Map<?,?> emptyMap = Collections.EMPTY_MAP;//new TreeMap();
  @SuppressWarnings( "unchecked" )
  public static <T1,T2> Map<T1,T2> getEmptyMap() {
    return (Map< T1, T2 >)emptyMap;
  }

  public static String toString( List< ? > arr ) {
    return toString( arr, true );
  }
  public static String toString( List< ? > arr, boolean square ) {
    return toString( arr.toArray(), square );
  }

  public static String toString( Object[] arr ) {
    return toString( arr, true );
  }
  public static String toString( Object[] arr, boolean square ) {
    if (arr == null) return "null";
    StringBuffer sb = new StringBuffer();
    if ( square ) {
      sb.append( "[" );
    } else {
      sb.append( "(" );
    }
    for ( int i = 0; i < arr.length; ++i ) {//Object o : arr ) {
      if ( i > 0 ) sb.append( "," );
      if ( arr[i] == null ) {
        sb.append("null");
      } else {
        sb.append( arr[i].toString() );
      }
    }
    if ( square ) {
      sb.append( "]" );
    } else {
      sb.append( ")" );
    }

    return sb.toString();
  }

  /**
   * Translate a string s to an Integer.
   *
   * @param s
   *          is the string to parse as an Integer
   * @return the integer translation of string s, or return null if s is not an
   *         integer.
   */
  public static Integer toInteger( String s ) {
    Integer i = null;
    try {
      i = Integer.parseInt( s );
    } catch ( NumberFormatException e ) {
      // leave i = null
    }
    return i;
  }

  /**
   * Translate a string s to an Long.
   *
   * @param s
   *          is the string to parse as an Long
   * @return the long translation of string s, or return null if s is not a
   *         long.
   */
  public static Long toLong( String s ) {
    Long l = null;
    try {
      l = Long.parseLong( s );
    } catch ( NumberFormatException e ) {
      // leave i = null
    }
    return l;
  }

  /**
   * Translate a string s to a Double.
   *
   * @param s
   *          is the string to parse as a Double
   * @return the double translation of string s, or return null if s is not a
   *         double/integer.
   */
  public static Double toDouble( String s ) {
    Double i = null;
    try {
      i = Double.parseDouble( s );
    } catch ( NumberFormatException e ) {
      // leave i = null
    }
    return i;
  }

  /**
   * Generate a string that repeats/replicates a string a specified number of
   * times.
   *
   * @param s
   *          is the string to repeat.
   * @param times
   *          is the number of times to repeat the string.
   * @return a concatenation of times instances of s.
   */
  public static String repeat( String s, int times ) {
    StringBuilder sb = new StringBuilder();
    for ( int i=0; i<times; ++i ) {
      sb.append(s);
    }
    return sb.toString();
  }

  public static String spaces( int n ) {
    return repeat( " ", n );
  }

  public static String numberWithLeadingZeroes( int n, int totalChars ) {
    Formatter formatter = new Formatter(Locale.US);
    String suffix = "" + formatter.format( "%0" + totalChars + "d", n );
    formatter.close();
    return suffix;
  }


  // Check if string has really got something.
  public static boolean isNullOrEmpty( String s ) {
    return ( s == null || s.isEmpty() ||
             s.trim().toLowerCase().equals( "null" ) );
  }

  // Check if array has really got something.
  public static boolean isNullOrEmpty( Object[] s ) {
    return ( s == null || s.length == 0 );
  }

  // Check if Collection has really got something.
  public static boolean isNullOrEmpty( Collection< ? > s ) {
    return ( s == null || s.isEmpty() );
  }

  // Check if Map has really got something.
  public static boolean isNullOrEmpty( Map< ?, ? > s ) {
    return ( s == null || s.isEmpty() );
  }

  // generic map<X, map<Y, Z> >.put(x, y, z)
  /**
   * A generic method for putting a triple into a map of a map to emulate
   * map&ltX, map&ltY, Z> >.put(x, y, z). For both keys (x and y), if no entry
   * exists, an entry is created.
   *
   * @param map
   *          the target map to receive the new entry
   * @param t1
   *          the outer map key
   * @param t2
   *          the inner map key
   * @param t3
   *          the inner map value
   * @return the pre-existing value mapped to t2 in the inner map or null if
   *         there is no such entry/
   */
  public static < T1, T2, T3 >
      T3 put( Map< T1, Map< T2, T3 > > map, T1 t1, T2 t2, T3 t3 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.put() with null argument!",
                            map, t1, t2, t3 ) ) {
      return null;
    }
    Map< T2, T3 > innerMap = map.get( t1 );
    if ( innerMap == null ) {
      innerMap = new LinkedHashMap< T2, T3 >();
      map.put( t1, innerMap );
    }
    return innerMap.put( t2, t3 );
  }

  // generic map<X, map<Y, Z> >.get(x, y) --> z
  /**
   * A generic method for getting a value from a map in a map to emulate
   * map&ltX, map&ltY, Z> >.get(x, y) --> z.
   *
   * @param map
   *          the map of map from which to retrieve the value
   * @param t1
   *          the outer map key
   * @param t2
   *          the inner map key
   * @return the value mapped to t2 in the inner map that is mapped to t1 or
   *         null if either entry does not exist.
   */
  public static < T1, T2, T3 >
      T3 get( Map< T1, Map< T2, T3 > > map, T1 t1, T2 t2 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.get() with null argument!",
                            map, t1, t2 ) ) {
      return null;
    }
    Map< T2, T3 > innerMap = map.get( t1 );
    if ( innerMap != null ) {
      return innerMap.get( t2 );
    }
    return null;
  }

  // generic map< W, map<X, map<Y, Z> >.put(w, x, y, z)
  public static <T1, T2, T3, T4 > T4 put( Map< T1, Map< T2, Map< T3, T4 > > > map, T1 t1, T2 t2, T3 t3, T4 t4 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.put() with null argument!",
                            map, t1, t2, t3, t4 ) ) {
      return null;
    }
    Map< T2, Map< T3, T4 > > innerMap = map.get( t1 );
    if ( innerMap == null ) {
      innerMap = new LinkedHashMap< T2, Map< T3, T4 > >();
      map.put( t1, innerMap );
    }
    return put( innerMap, t2, t3, t4 );
  }

  // generic map< W, map<X, map<Y, Z> >.get(w, x, y) --> z
  public static <T1, T2, T3, T4 > T4 get( Map< T1, Map< T2, Map< T3, T4 > > > map, T1 t1, T2 t2, T3 t3 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.get() with null argument!",
                            map, t1, t2, t3 ) ) {
      return null;
    }
    Map< T2, Map< T3, T4 > > innerMap = map.get( t1 );
    if ( innerMap != null ) {
      return get( innerMap, t2, t3 );
    }
    return null;
  }
  
  // generic map< W, map<X, map<Y, Z> >.put(w, x, y, z)
  public static <T1, T2, T3, T4, T5 >
      T5 put( Map< T1, Map< T2, Map< T3, Map< T4, T5 > > > > map,
              T1 t1, T2 t2, T3 t3, T4 t4, T5 t5 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.put() with null argument!",
                            map, t1, t2, t3, t4 ) ) {
      return null;
    }
    Map< T4, T5 > innerMap = 
            (Map< T4, T5 >)get( map, t1, t2, t3 );
    if ( innerMap == null ) {
      innerMap = new LinkedHashMap< T4, T5 >();
      put( map, t1, t2, t3, innerMap );
    }
    return innerMap.put( t4, t5 );
  }

  // generic map< W, map<X, map<Y, Z> >.get(w, x, y) --> z
  public static <T1, T2, T3, T4, T5 >
      T5 get( Map< T1, Map< T2, Map< T3, Map< T4, T5 > > > > map,
              T1 t1, T2 t2, T3 t3, T4 t4 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.get() with null argument!",
                            map, t1, t2, t3, t4 ) ) {
      return null;
    }
    Map< T4, T5 > innerMap = get( map, t1, t2, t3 );
    if ( innerMap != null ) {
      return innerMap.get( t4 );
    }
    return null;
  }

  // generic map< W, map<X, map<Y, Z> >.put(w, x, y, z)
  public static <T1, T2, T3, T4, T5, T6 >
      T6 put( Map< T1, Map< T2, Map< T3, Map< T4, Map< T5, T6 > > > > > map,
              T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.put() with null argument!",
                            map, t1, t2, t3, t4, t5 ) ) {
      return null;
    }
    Map< T4, Map< T5, T6 > > innerMap = 
            (Map< T4, Map< T5, T6> >)get( map, t1, t2, t3 );
    if ( innerMap == null ) {
      innerMap = new LinkedHashMap< T4, Map< T5, T6 > >();
      put( map, t1, t2, t3, innerMap );
    }
    return put( innerMap, t4, t5, t6 );
  }

  // generic map< W, map<X, map<Y, Z> >.get(w, x, y) --> z
  public static <T1, T2, T3, T4, T5, T6 >
      T6 get( Map< T1, Map< T2, Map< T3, Map< T4, Map< T5, T6 > > > > > map,
              T1 t1, T2 t2, T3 t3, T4 t4, T5 t5 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.get() with null argument!",
                            map, t1, t2, t3, t4, t5 ) ) {
      return null;
    }
    Map< T4, Map< T5, T6 > > innerMap = get( map, t1, t2, t3 );
    if ( innerMap != null ) {
      return get( innerMap, t4, t5 );
    }
    return null;
  }

  // generic map< W, map<X, map<Y, Z> >.put(w, x, y, z)
  public static <T1, T2, T3, T4, T5, T6, T7 >
      T7 put( Map< T1, Map< T2, Map< T3, Map< T4, Map< T5, Map<T6, T7> > > > > > map,
              T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.put() with null argument!",
                            map, t1, t2, t3, t4 ) ) {
      return null;
    }
    //Map< T1, Map< T2, Map< T3, ? > > > castMap = (Map< T1, Map< T2, Map< T3, ? > > >)toMap(map, Map< T1, Map< T2, Map< T3, ? > > >.class, Object.class);
    Map< T4, Map< T5, Map< T6, T7 > > > innerMap = 
            (Map< T4, Map< T5, Map< T6, T7 > > >)get( map, t1, t2, t3 );
    if ( innerMap == null ) {
      innerMap = new LinkedHashMap< T4, Map< T5, Map< T6, T7 > > >();
      put( map, t1, t2, t3, innerMap );
    }
    return put( innerMap, t4, t5, t6, t7 );
  }

  // generic map< W, map<X, map<Y, Z> >.get(w, x, y) --> z
  public static <T1, T2, T3, T4, T5, T6, T7 >
      T7 get( Map< T1, Map< T2, Map< T3, Map< T4, Map< T5, Map<T6, T7> > > > > > map,
              T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.get() with null argument!",
                            map, t1, t2, t3 ) ) {
      return null;
    }
    Map< T4, Map< T5, Map< T6, T7 > > > innerMap = get( map, t1, t2, t3 );
    if ( innerMap != null ) {
      return get( innerMap, t4, t5, t6 );
    }
    return null;
  }

  // generic map< W, map<X, map<Y, Z> >.put(w, x, y, z)
  public static <T1, T2, T3, T4, T5, T6, T7, T8 >
      T8 put( Map< T1, Map< T2, Map< T3, Map< T4, Map< T5, Map<T6, Map< T7, T8 > > > > > > > map,
              T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.put() with null argument!",
                            map, t1, t2, t3, t4, t5, t6, t7 ) ) {
      return null;
    }
    //Map< T1, Map< T2, Map< T3, ? > > > castMap = (Map< T1, Map< T2, Map< T3, ? > > >)toMap(map, Map< T1, Map< T2, Map< T3, ? > > >.class, Object.class);
    Map< T4, Map< T5, Map< T6, Map< T7, T8 > > > > innerMap = 
            (Map< T4, Map< T5, Map< T6, Map< T7, T8 > > > >)get( map, t1, t2, t3 );
    if ( innerMap == null ) {
      innerMap = new LinkedHashMap< T4, Map< T5, Map< T6, Map< T7, T8 > > > >();
      put( map, t1, t2, t3, innerMap );
    }
    return put( innerMap, t4, t5, t6, t7, t8 );
  }

  // generic map< W, map<X, map<Y, Z> >.get(w, x, y) --> z
  public static <T1, T2, T3, T4, T5, T6, T7, T8 >
      T8 get( Map< T1, Map< T2, Map< T3, Map< T4, Map< T5, Map<T6, Map< T7, T8 > > > > > > > map,
              T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.get() with null argument!",
                            map, t1, t2, t3, t4, t5, t6, t7 ) ) {
      return null;
    }
    Map< T4, Map< T5, Map< T6, Map< T7, T8 > > > > innerMap = get( map, t1, t2, t3 );
    if ( innerMap != null ) {
      return get( innerMap, t4, t5, t6, t7 );
    }
    return null;
  }

  // generic map< W, map<X, map<Y, Z> >.put(w, x, y, z)
  public static <T1, T2, T3, T4, T5, T6, T7, T8, T9 >
      T9 put( Map< T1, Map< T2, Map< T3, Map< T4, Map< T5, Map<T6, Map< T7, Map< T8, T9 > > > > > > > > map,
              T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.put() with null argument!",
                            map, t1, t2, t3, t4, t5, t6, t7, t8 ) ) {
      return null;
    }
    //Map< T1, Map< T2, Map< T3, ? > > > castMap = (Map< T1, Map< T2, Map< T3, ? > > >)toMap(map, Map< T1, Map< T2, Map< T3, ? > > >.class, Object.class);
    Map< T4, Map< T5, Map< T6, Map< T7, Map< T8, T9 > > > > > innerMap = 
            (Map< T4, Map< T5, Map< T6, Map< T7, Map< T8, T9 > > > > >)get( map, t1, t2, t3 );
    if ( innerMap == null ) {
      innerMap = new LinkedHashMap< T4, Map< T5, Map< T6, Map< T7, Map< T8, T9 > > > > >();
      put( map, t1, t2, t3, innerMap );
    }
    return put( innerMap, t4, t5, t6, t7, t8, t9 );
  }

  // generic map< W, map<X, map<Y, Z> >.get(w, x, y) --> z
  public static <T1, T2, T3, T4, T5, T6, T7, T8, T9 >
      T9 get( Map< T1, Map< T2, Map< T3, Map< T4, Map< T5, Map<T6, Map< T7, Map< T8, T9 > > > > > > > > map,
              T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.get() with null argument!",
                            map, t1, t2, t3, t4, t5, t6, t7, t8 ) ) {
      return null;
    }
    Map< T4, Map< T5, Map< T6, Map< T7, Map< T8, T9 > > > > > innerMap = get( map, t1, t2, t3 );
    if ( innerMap != null ) {
      return get( innerMap, t4, t5, t6, t7, t8 );
    }
    return null;
  }

  // generic map< W, map<X, map<Y, Z> >.put(w, x, y, z)
  public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10 >
      T10 put( Map< T1, Map< T2, Map< T3, Map< T4, Map< T5, Map<T6, Map< T7, Map< T8, Map< T9, T10 > > > > > > > > > map,
              T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.put() with null argument!",
                            map, t1, t2, t3, t4, t5, t6, t7, t8, t9 ) ) {
      return null;
    }
    //Map< T1, Map< T2, Map< T3, ? > > > castMap = (Map< T1, Map< T2, Map< T3, ? > > >)toMap(map, Map< T1, Map< T2, Map< T3, ? > > >.class, Object.class);
    Map< T4, Map< T5, Map< T6, Map< T7, Map< T8, Map< T9, T10 > > > > > > innerMap = 
            (Map< T4, Map< T5, Map< T6, Map< T7, Map< T8, Map< T9, T10 > > > > > >)get( map, t1, t2, t3 );
    if ( innerMap == null ) {
      innerMap = new LinkedHashMap< T4, Map< T5, Map< T6, Map< T7, Map< T8, Map< T9, T10 > > > > > >();
      put( map, t1, t2, t3, innerMap );
    }
    return put( innerMap, t4, t5, t6, t7, t8, t9, t10 );
  }

  // generic map< W, map<X, map<Y, Z> >.get(w, x, y) --> z
  public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10 >
      T10 get( Map< T1, Map< T2, Map< T3, Map< T4, Map< T5, Map<T6, Map< T7, Map< T8, Map< T9, T10 > > > > > > > > > map,
              T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9 ) {
    if ( Debug.errorOnNull( "Error! Called Utils.get() with null argument!",
                            map, t1, t2, t3, t4, t5, t6, t7, t8, t9 ) ) {
      return null;
    }
    Map< T4, Map< T5, Map< T6, Map< T7, Map< T8, Map< T9, T10 > > > > > > innerMap = get( map, t1, t2, t3 );
    if ( innerMap != null ) {
      return get( innerMap, t4, t5, t6, t7, t8, t9 );
    }
    return null;
  }

  /**
   * Manages a "seen" set for avoiding infinite recursion.
   * @param o is the object visited
   * @param recursive is whether infinite recursion is possible
   * @param seen is the set of objects already visited
   * @return whether the object has already been visited
   */
  public static < T > Pair< Boolean, Seen< T > > seen( T o, boolean recursive,
                                                      Seen< T > seen ) {
//    boolean hadSeen = false;
//    if ( seen == null && recursive ) {
//      seen = new SeenHashSet< T >();
//      seen.add( o );
//    }
//    seen.see( o, recursive );
    if ( seen != null && seen.contains( o ) ) {
//    ++seenCt;
      return new Pair< Boolean, Seen<T> >( seen.see( o, recursive ), seen );
    }
//  ++notSeenCt;
    if ( seen == null && recursive == true ) {
      seen = new SeenHashSet< T >(); // ok to use hash here since we never iterate
                                 // over the contents
    }
    if ( seen != null ) seen.add( o );
    return new Pair< Boolean, Seen< T > >( false, seen );
  }

//  private static long notSeenCt = 0;
//  private static long seenCt = 0;

  /**
   * Manages a "seen" set for avoiding infinite recursion.
   * @param o is the object visited
   * @param recursive is whether infinite recursion is possible
   * @param seen is the set of objects already visited
   * @return whether the object has already been visited
   */
  public static < T > Pair< Boolean, Set< T > > seen( T o, boolean recursive,
                                                      Set< T > seen ) {
    if ( seen instanceof Seen ) {
      Pair< Boolean, Seen< T > > p = seen( o, recursive, (Seen< T >)seen );
      return new Pair< Boolean, Set<T> >( p );
    }
    if ( seen != null && seen.contains( o ) ) {
//      ++seenCt;
      return new Pair< Boolean, Set< T > >( true, seen );
    }
//    ++notSeenCt;
    if ( seen == null && recursive == true ) {
      seen = new HashSet< T >(); // ok to use hash here since we never iterate
                                 // over the contents
    }
    if ( seen != null ) seen.add( o );
    return new Pair< Boolean, Set< T > >( false, seen );
  }

  /**
   * Replace the last occurrence of the substring in s with the replacement.
   * @param s
   * @param replacement
   * @return the result of the replacement
   */
  public static String replaceLast( String s, String substring,
                                    String replacement ) {
    int pos = s.lastIndexOf(substring);
    if ( pos == -1 ) return s;
    return s.substring( 0, pos ) + replacement
           + s.substring( pos + substring.length() );
  }

  public static String spewObjectPrefix = "* * * * *";
  public static String spewObjectSuffix = spewObjectPrefix;

  public static String spewObject( Object o, String indent ) {
    return spewObject( o, indent, spewObjectPrefix, spewObjectSuffix );
  }

  // TODO -- bring over spewContents from EMFUtils, but apply it to Objects
  // instead of EObjects.
  // TODO -- write out fields and their values. don't forget about setting
  // things to be accessible, if desired.
  public static String spewObject(Object o, String indent,
                                  String prefix, String suffix ) {
    StringBuffer sb = new StringBuffer();
    sb.append(indent + prefix + "\n");
    Class<?> c = o.getClass();
    Method[] methods = c.getMethods();
    for (Method m : methods) {
      if (m.getReturnType() == void.class || m.getReturnType() == null
          || m.getName().startsWith("set")
          || m.getName().startsWith("clone")
          || m.getName().startsWith("wait")
          || m.getName().startsWith("notify")
          || m.getName().startsWith("remove")
          || m.getName().startsWith("delete")) {
        continue;
      }
      if (m.getParameterTypes().length == 0) {
          sb.append(indent + m.getDeclaringClass() + ", "
              + m.toGenericString() + " --> "
              + ClassUtils.runMethod(true, o, m).second + "\n");
      }
    }
    sb.append(indent + suffix + "\n");
    return sb.toString();
  }


  // REVIEW -- consider moving these along with put & get to a CollectionUtils class

  /**
   * @param c
   * @return c if c is a {@link List} or, otherwise, an ArrayList containing
   *         the elements of c
   */
  public static <V, T extends V> List<V> toList( Collection<T> c ) {
    return asList( c );
  }
  /**
   * @param c
   * @return c if c is a {@link List} or, otherwise, a new List containing
   *         the elements of c
   */
  public static <V, T extends V> List<V> asList( Collection<T> c ) {
    if ( c instanceof List ) return (List<V>)c;
    List<V> list = new ArrayList< V >( c );
    return list;
  }

  public static <V, T> List<V> asList( Object o, Class<V> cls ) {
      if ( o == null ) return null;
      if ( o instanceof Collection ) {
          return asList( (Collection<?>)o, cls );
      }
      return asList( newList( o ), cls );
  }
  
  public static <V, T extends V> List<V> arrayAsList( T[] c ) {
      List<V> list = new ArrayList< V >();
      for ( T co : c ) {
          list.add(co);
      }
      return list;
  }

  public static <V, T extends V> List<V> arrayAsList( T[] c, Class<V> cls ) {
      List<V> list = new ArrayList< V >();
      for ( T co : c ) {
          try {
              V v  = cls.cast( co );
              list.add(v);
          } catch ( ClassCastException e ) {
              Debug.error( true, false, "Error! Could not cast " + co + " to " + cls.getSimpleName() + "!" );
          }
      }
      return list;
  }


  /**
   * @param c
   * @param cls
   * @return a new {@link List} containing
   *         the elements of c cast to type V
   */
  public static <V, T> ArrayList<V> asList( Collection<T> c, Class<V> cls ) {
      return asList(c, cls, true);
  }

  /**
   * @param c
   * @param cls
   * @return a new {@link List} containing
   *         the elements of c cast to type V
   */
  public static <V, T> ArrayList<V> asList( Collection<T> c, Class<V> cls, boolean allowNull ) {
      ArrayList<V> list = new ArrayList< V >();
      for ( T t : c ) {
          if (t == null || cls == null || cls.isAssignableFrom( t.getClass() ) ) {
              if (allowNull || t != null) {
                  try {
                      V v;
                      if (cls != null) {
                          v = cls.cast( t );
                      } else {
                          v = (V) t;
                      }
                      list.add( v );
                  } catch ( ClassCastException e ) {}
              }
          }
      }
      return list;
  }
  
  
    /**
     * @param source
     * @param newKeyType
     * @param newValueType
     * @return a new {@link LinkedHashMap} containing the entries in source map
     *         that successfully cast to the new key and value types
     */
    public static < K1, V1, K2, V2 > Map< K2, V2 >
            toMap( Map< K1, V1 > source,
                   Class< K2 > newKeyType,
                   Class< V2 > newValueType ) {
        Map< K2, V2 > target = new LinkedHashMap< K2, V2 >();
        for ( Map.Entry< K1, V1 > e : source.entrySet() ) {
            Pair< Boolean, K2 > p =
                    ClassUtils.coerce( e.getKey(), newKeyType, true );
            if ( p.first ) {
                K2 k = p.second;
                Pair< Boolean, V2 > pv =
                        ClassUtils.coerce( e.getValue(), newValueType, true );
                if ( pv.first ) {
                    V2 v = pv.second;
                    target.put( k, v );
                }
            }
        }
        return target;
    }
    
    public static <ID, T> Map<ID, T> toMap( Collection<T> set ) {
        LinkedHashMap< ID, T > map = new LinkedHashMap< ID, T >();
        for ( T t : set ) {
        	ID id = null;
        	try
        	{ id = (ID) ClassUtils.getId( t ); }
        	catch
        	( Throwable e ) { Debug.error("Could not get ID for toMap"); return null; }
        	if (id == null)
        	{
        		Debug.error("ID returned with value of null");
        	}
        	else
        	{
        		map.put( id, t );
        	}
        }
        return map;
    }

  /**
   * @param c
   * @return a c if c is a {@link Set} or, otherwise, a {@link LinkedHashSet} containing
   *         the elements of c
   */
  public static <T> Set<T> toSet( Collection<T> c ) {
      // TODO -- make this and other toX methods use <V, T extends V> like in toList()
    return asSet( c );
  }
  
  public static <T> Set<T> asSet( T[] tArray ) {
      return toSet(tArray);
  }
  public static <T> Set<T> toSet( T[] tArray ) {
      LinkedHashSet<T> set = new LinkedHashSet< T >( newList( tArray ) );
      return set;
  }

  /**
   * @param c
   * @return a c if c is a {@link Set} or, otherwise, a {@link LinkedHashSet} containing
   *         the elements of c
   */
  public static <T> Set<T> asSet( Collection<T> c ) {
    if ( c instanceof Set ) return (Set<T>)c;
    LinkedHashSet<T> set = new LinkedHashSet< T >( c );
    return set;
  }

//  public static Constructor< ? >
//      getConstructorForArgTypes( Class< ? > cls, String packageName,
//                                 Class< ? >... argTypes ) {
//    // Pair< Constructor< ? >, Object[] > p =
//    return getConstructorForArgTypes( cls, argTypes );
//    // if ( p == null ) return null;
//    // return p.first;
//  }

  public static <T1, T2> T2[] toArrayOfType( T1[] source, Class< T2 > newType ) {
      return toArrayOfType( source, newType, false );
  }
  public static <T1, T2> T2[] toArrayOfType( T1[] source, Class< T2 > newType, boolean complain ) {
      T2[] target = (T2[])Array.newInstance( newType, source.length );
      boolean succ = toArrayOfType( source, target, newType );
      if ( complain && !succ ) {
          Debug.error( "Could not convert array " + toString( source ) + " to " + newType.getSimpleName() + "[]!");
      }
      return target;
  }
  public static <T1, T2> boolean toArrayOfType( T1[] source, T2[] target,
                                                Class< T2 > newType ) {
    boolean succ = true;
    for ( int i=0; i < source.length; ++i ) {
      try {
        target[i] = newType.cast( source[i] );
      } catch ( ClassCastException e ) {
        succ = false;
        target[i] = null;
      }
    }
    return succ;
  }

  public static <T1, T2> T2[] toArrayOfType( Collection<T1> source, Class< T2 > newType ) {
      return toArrayOfType( source, newType, false );
  }
  public static <T1, T2> T2[] toArrayOfType( Collection<T1> source, Class< T2 > newType, boolean complain ) {
      return toArrayOfType( source.toArray(), newType, complain );
  }
  public static <T1, T2> boolean toArrayOfType( Collection<T1> source,
                                                T2[] target,
                                                Class< T2 > newType ) {
    return toArrayOfType( source.toArray(), target, newType );
  }

  public static <T> String join( Collection<T> things, String delim ) {
    return join( things.toArray(), delim );
  }

  public static <T> String join( T[] things, String delim ) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for ( T t : things ) {
      if ( first ) first = false;
      else sb.append( delim );
      sb.append( t );
    }
    return sb.toString();
  }
  public static <T> T[] scramble( T[] array ) {
    for ( int i=0; i < array.length; ++i ) {
      int j = Random.global.nextInt( array.length );
      if ( j != i ) {
        T tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
      }
    }
    return array;
  }
  public static <T> T[] scramble( Collection< T > collection ) {
    if ( isNullOrEmpty( collection ) ) return (T[])new Object[]{};
    T[] a = (T[])new Object[collection.size()];
    collection.toArray( a );
    return scramble( a );
  }

  /**
   * @param o
   * @param T
   * @return the number of occurrences of o in the Collection c
   */
  public static < T > int occurrences( T value, Collection< T > c ) {
    if ( c == null ) return 0;
    int ct = 0;
    // TODO -- shouldn't be calling event.Expression here--make
    // Expression.evaluate() work for Wraps and include Wraps in src/util
    // TODO -- maybe try creating an Evaluatable interface so that evaluate can be overridden to call that of Expression or ClassUtil.
//    Object v = Expression.evaluate( value, null, false );
    Object v = ClassUtils.evaluate( value, null, false );
    for ( T o : c ) {
      //Object ov = ClassUtils.evaluate( o, null, false );
      Object ov = ClassUtils.evaluate( o, null, false );
      if ( valuesEqual( ov, v ) ) ct++;
      else if ( valuesEqual( o, value ) ) ct++;
    }
    return ct;
  }

//  /**
//   * A potentially more efficient addAll() for unordered Collections.
//   * @param coll1
//   * @param coll2
//   * @return the longer of the two collections after adding the shorter to the longer.
//   */
//  public static < T, C extends Collection<T> > C addAll( Collection<T> coll1, Collection<T> coll2 ) {
//    if ( coll1.size() < coll2.size() ) {
//      coll2.addAll( coll1 );
//      return (C)coll2;
//    }
//    coll1.addAll( coll2 );
//    return (C)coll1;
//  }

  /**
   * A potentially more efficient addAll() for unordered Collections.
   * @param coll1
   * @param coll2
   * @return the longer of the two collections after adding the shorter to the longer.
   */
  public static < T, C extends Collection<T> > C addAll( Collection<T> coll1,
                                                         Collection<T> coll2 ) {
    if ( coll1 == null ) return (C)coll2;
    if ( coll2 == null ) return (C)coll1;

    Collection<T> cSmaller, cBigger;
    if ( coll1.size() < coll2.size() ) {
      cSmaller = coll1;
      cBigger = coll2;
    } else {
      cSmaller = coll2;
      cBigger = coll1;
    }
    try {
      cBigger.addAll( cSmaller );
      return (C)cBigger;
    } catch (UnsupportedOperationException e) {}
    try {
      cSmaller.addAll( cBigger );
      return (C)cSmaller;
    } catch (UnsupportedOperationException e) {}
    ArrayList<T> newList = new ArrayList< T >( cBigger );
    newList.addAll( cSmaller );
    return (C)newList;
  }

  public static final String timestampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  public static final String fileTimestampFormat = "yyyy-MM-dd'T'HH.mm.ss.SSSZ";

  // Converts time offset to a date-time String in Timepoint.timestamp format.
  // Assumes t is an offset from Timepoint.epoch in Timepoint.units.
  public static String timestampForFile() {
    String timeString =
        new SimpleDateFormat( fileTimestampFormat ).format( System.currentTimeMillis() );
    return timeString;
  }

  public static String addTimestampToFilename( String fileName ) {
    int pos = fileName.lastIndexOf( '.' );
    String prefix = fileName;
    String suffix = "";
    if ( pos != -1 ) {
      prefix = fileName.substring( 0, pos );
      suffix = fileName.substring( pos );
    }
    String newFileName = prefix + timestampForFile() + suffix;
    return newFileName;
  }

  public static <T1, T2> boolean valuesEqual( T1 v1, T2 v2 ) {
    return v1 == v2 || ( v1 != null && v1.equals( v2 ) );
  }
  
  public static <T1, T2> boolean valuesLooselyEqual( T1 v1, T2 v2,
                                                     boolean propagate  ) {
      return valuesLooselyEqual( v1, v2, true, propagate );
  }
  public static <T1, T2> boolean valuesLooselyEqual( T1 v1, T2 v2, boolean checkWrap,
                                                     boolean propagate  ) {
      Seen<Object> seen1 = null;
      Object vv1 = v1;
      while ( true ) {
          Pair< Boolean, Seen< Object > > p1 = Utils.seen( vv1, true, seen1 );
          if ( p1.first ) break;
          seen1 = p1.second;
          Object vv2 = v2;
          Seen<Object> seen2 = null;
          while ( true ) {
              Pair< Boolean, Seen< Object > > p2 = Utils.seen( vv2, true, seen2 );
              if ( p2.first ) break;
              seen2 = p2.second;
              
              if ( valuesLooselyEqualNoWrap( vv1, vv2, propagate ) ) return true;
              
              if ( checkWrap && vv2 instanceof Wraps )
                  vv2 = ((Wraps<?>)vv2).getValue( propagate );
              else break;
          }
          if ( checkWrap && vv1 instanceof Wraps )
              vv1 = ((Wraps<?>)vv1).getValue( propagate );
          else break;
      }
      return false;
  }

  public static <T1, T2> boolean valuesLooselyEqualNoWrap( T1 v1, T2 v2, boolean propagate  ) {
      // Plain values
      if ( valuesEqual( v1, v2 ) ) return true;
      
      // Check null
      if ( v1 == null || v2 == null ) return false;
      
      // Classes
      if ( v1 instanceof Class ) {
          if ( ClassUtils.classMatches( (Class<?>)v1, v2, propagate ) ) {
              return true;  
          }
          if ( !( v2 instanceof Class ) ) return false;
      }
      if ( v2 instanceof Class ) {
          return ClassUtils.classMatches( (Class<?>)v2, v1, propagate );
      }

      // Collections and arrays
      Collection<?> v1c = ( v1 instanceof Collection ) ? (Collection<?>)v1 : null;
      Object[] v1a = (v1c == null && v1.getClass().isArray()) ? (Object[])v1 : null;
      if ( v1c != null || v1a != null ) {
          Collection<?> v2c = ( v2 instanceof Collection ) ? (Collection<?>)v2 : null;
          Object[] v2a = (v2c == null && v2.getClass().isArray()) ? (Object[])v2 : null;
          if ( v2c == null && v2a == null ) return false;
          int s1 = v1c == null ? v1a.length : v1c.size();
          int s2 = v2c == null ? v2a.length : v2c.size();
          if ( s1 != s2 ) return false;
          Iterator<?> i1 = v1c == null ? null : v1c.iterator();
          Iterator<?> i2 = v2c == null ? null : v2c.iterator();
          for ( int i = 0; i < s1; ++i ) {
              Object vv1 = v1c == null ? v1a[i] : i1.next(); 
              Object vv2 = v2c == null ? v2a[i] : i2.next();
              if ( !valuesLooselyEqual( vv1, vv2, propagate ) ) return false;
          }
          return true;
      }
      
      // Pairs
      if ( ( v1 instanceof Pair ) && ( v2 instanceof Pair ) ) {
          Pair<?,?> p1 = (Pair<?,?>)v1;
          Pair<?,?> p2 = (Pair<?,?>)v2;
          return valuesLooselyEqual( p1.first, p2.first, propagate ) &&
               valuesLooselyEqual( p1.second, p2.second, propagate );
      }
      
      // Map entries
      if ( ( v1 instanceof Entry ) && ( v2 instanceof Entry ) ) {
          return valuesLooselyEqual( ((Entry< ?, ? >)v1).getKey(),
                                     ((Entry< ?, ? >)v2).getKey(), propagate ) &&
                 valuesLooselyEqual( ((Entry< ?, ? >)v1).getValue(),
                                     ((Entry< ?, ? >)v2).getValue(), propagate );
      }
      
      // Maps
      if ( ( v1 instanceof Map ) && ( v2 instanceof Map ) ) {
          Map<?,?> m1 = (Map<?,?>)v1;
          Map<?,?> m2 = (Map<?,?>)v2;
          if ( m1.size() != m2.size() ) return false;
          for ( Object k : m1.keySet() ) {
              if ( !m2.containsKey( k ) ) return false;  // REVIEW -- try do loosely equal here?
              if ( !valuesLooselyEqual( m1.get( k ), m2.get( k ), propagate ) ) return false;
          }
          return true;
      }
      
      return false;
  }

  public static String toStringNoHash( Object o ) {
//    if ( o == null ) return "null";
//    try {
//      if ( o.getClass().getMethod( "toString", (Class<?>[])null ).getDeclaringClass() == o.getClass() ) {
//        return o.toString();
//      }
//    } catch ( SecurityException e ) {
//      e.printStackTrace();
//    } catch ( NoSuchMethodException e ) {
//      e.printStackTrace();
//    }
    return o.toString().replace( Integer.toHexString(o.hashCode()), "" );
  }


  /**
   * @param s1
   * @param s2
   * @return the length of the longest common substring which is also a prefix of one of the strings.
   */
  public static int longestPrefixSubstring( String subcontext, String subc ) {
    int numMatch = 0;
    if ( subcontext.contains( subc ) ) {
      if ( numMatch < subc.length() ) {
        //subcontextKey = subc;
        numMatch = subc.length();
        //numDontMatch = subcontext.length() - subc.length();
//      } else if ( numMatch == subc.length() ) {
//        if ( subcontext.length() - subc.length() < numDontMatch ) {
//          subcontextKey = subc;
//          numDontMatch = subcontext.length() - subc.length();
//        }
      }
    } else if ( subc.contains( subcontext ) ) {
      if ( numMatch < subcontext.length() ) {
        //subcontextKey = subcontext;
        numMatch = subcontext.length();
        //numDontMatch = subc.length() - subcontext.length();
//      } else if ( numMatch == subcontext.length() ) {
//        if ( subc.length() - subcontext.length() < numDontMatch ) {
//          subcontextKey = subcontext;
//          numDontMatch = subc.length() - subcontext.length();
//        }
      }
    }
    return numMatch;
  }


  /**
   * This implementation appears {@code O(n^2)}. This is slower than a suffix
   * trie implementation, which is {@code O(n+m)}. The code below is copied from
   * wikipedia.
   *
   * @param s1
   * @param s2
   * @return the length of the longest common substring
   *
   *
   */
  public static int longestCommonSubstr( String s1, String s2 ) {
    if ( s1.isEmpty() || s2.isEmpty() ) {
      return 0;
    }

    int m = s1.length();
    int n = s2.length();
    int cost = 0;
    int maxLen = 0;
    int[] p = new int[ n ];
    int[] d = new int[ n ];

    for ( int i = 0; i < m; ++i ) {
      for ( int j = 0; j < n; ++j ) {
        if ( s1.charAt( i ) != s2.charAt( j ) ) {
          cost = 0;
        } else {
          if ( ( i == 0 ) || ( j == 0 ) ) {
            cost = 1;
          } else {
            cost = p[ j - 1 ] + 1;
          }
        }
        d[ j ] = cost;

        if ( cost > maxLen ) {
          maxLen = cost;
        }
      } // for {}

      int[] swap = p;
      p = d;
      d = swap;
    }

    return maxLen;
  }

  /**
   * @param word
   * @return the word with the first character capitalized, if applicable
   */
  public static String capitalize( String word ) {
    String capitalizedWord = word;
    if ( Character.isLowerCase( word.charAt( 0 ) ) ) {
      capitalizedWord =
          "" + Character.toUpperCase( word.charAt( 0 ) )
              + word.substring( 1 );
    }
    return capitalizedWord;
  }

  /**
   * Creates a new {@link TreeSet} and inserts the arguments, {@code ts}.
   * @param ts
   * @return the new {@link Set}
   */
  public static < T > Set< T > newSet( T... ts ) {
    Set< T > newSet = new TreeSet< T >(CompareUtils.GenericComparator.instance());
    newSet.addAll( Arrays.asList( ts ) );
    return newSet;
  }

  /**
   * Creates a new {@link ArrayList} and inserts the arguments, {@code ts}.
   * @param ts
   * @return the new {@link ArrayList}
   */
  public static < T > ArrayList< T > newList( T... ts ) {
    ArrayList< T > newList = new ArrayList< T >();
    if ( ts != null && ts.length > 0 ) {
        newList.addAll( Arrays.asList( ts ) );
    }
    return newList;
  }

  /**
   * Creates a new {@link HashMap} and inserts the arguments, {@code pairs}.
   * @param pairs
   * @return the new {@link Map}
   */
  public static < K, V > HashMap< K, V > newHashMap( Pair< K, V >... pairs ) {
    HashMap< K, V > map = new HashMap< K, V >();
    if ( pairs != null && pairs.length > 0 ) {
        for ( Pair< K, V > pair : pairs ) {
            map.put(pair.first, pair.second);
        }
    }
    return map;
  }

  /**
   * Creates a new {@link HashMap} and inserts the arguments, {@code pairs}.
   * @param pairs
   * @return the new {@link Map}
   */
  public static < K, V > TreeMap< K, V > newMap( Pair< K, V >... pairs ) {
    TreeMap< K, V > map = new TreeMap< K, V >(CompareUtils.GenericComparator.instance());
    if ( pairs != null && pairs.length > 0 ) {
        for ( Pair< K, V > pair : pairs ) {
            map.put(pair.first, pair.second);
        }
    }
    return map;
  }

  public static <T> T[] newArrayWithOneNull(Class<T> cls) {
      @SuppressWarnings( "unchecked" )
      T[] oneNullArg = (T[])Array.newInstance( cls, 1 );
      oneNullArg[0] = null;
      return oneNullArg;
  }

  public static < T > ArrayList< T > newListWithOneNull() {
      ArrayList< T > newList = new ArrayList< T >();
      newList.add(null);
      return newList;
  }

  protected static final String[] trueStrings = new String[] {"t", "true", "1", "1.0", "yes", "y"};

  public static Boolean isTrue(Object o) {
      return isTrue(o, true);
  }

  public static Boolean isTrue(Object o, boolean strict) {
      if (o == null)
          return strict ? null : false;
      if (Boolean.class.isAssignableFrom(o.getClass())) {
          return (Boolean)o;
      }
      String lower = o.toString().toLowerCase();
      if (lower.equals("true"))
          return true;
      if (lower.equals("false"))
          return false;
      if (strict)
          return null;
      for (String t: trueStrings) {
          if (lower.equals(t))
              return true;
      }
      return false;
  }

  public static Integer parseInt(String intStr) {
    try {
      int i = Integer.parseInt(intStr);
      return i;
    } catch (Exception e) {
      return null;
    }
  }
  public static boolean isInt(String intStr) {
    try {
      int i = Integer.parseInt(intStr);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
  public static boolean isNumber(String s) {
    if (isNullOrEmpty(s))
      return false;
    try {
      Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return false;
    } catch (NullPointerException e) {
      return false;
    }
    return true;
  }
  /**
   * Count the number of occurrences of the regular expression in the string.
   * @param regex
   * @param string
   * @return the number of occurrences of regex in string
   */
  public static int count( String regex, String string ) {
    int count = 0;
    int pos = 0;
    Pattern pattern = Pattern.compile( regex );
    Matcher matcher = pattern.matcher(string);
//    boolean found = false;
    while ( matcher.find() ) {
//      System.out.format( "I found the text" + " \"%s\" starting at "
//                             + "index %d and ending at index %d.%n",
//                         matcher.group(), matcher.start(), matcher.end() );
//      found = true;
      count++;
    }
    return count;
  }
  public static String replaceSuffix( String source, String str, String replacement ) {
    if ( source == null ) return null;
    if (replacement == null) replacement = "";
    if ( isNullOrEmpty( str ) ) return source + replacement;
    String compString = source;
    int pos = compString.lastIndexOf( str );
    if ( pos != -1 && pos == compString.length() - str.length() ) {
        compString = compString.substring( 0, pos ) + replacement;
    }
    return compString;
  }

    public static < T, C extends Collection< T > > C add( C coll, T... items ) {
        return plus( coll, items );
    }

    public static < T, C extends Collection< T >, D extends Collection< T > > C add( C coll, D items ) {
        return plus( coll, items );
    }

    public static < T, C extends Collection< T > > C addN( C coll, int n, T... items ) {
        if ( n < items.length ) {
            return plus( coll, Arrays.copyOf( items, n ) );
        }
        return plus( coll, items );
    }

    public static < T, C extends Collection< T >, D extends Collection< T > > C addN( C coll, int n, D items ) {
        return addN( coll, n, (T[])items.toArray() );
    }

    public static < T, C extends Collection< T > > C plus( C coll, T... items ) {
        coll.addAll( newList( items ) );
        return coll;
    }

    public static < T, C extends Collection< T >, D extends Collection< T > > C plus( C coll, D items ) {
        coll.addAll( items );
        return coll;
    }

    public static < T, C extends Collection< T > > C
            remove( C coll, T... items ) {
        return minus( coll, items );
    }

    public static < T, C extends Collection< T > > C minus( C coll, T... items ) {
        coll.removeAll( newList( items ) );
        return coll;
    }

    public static < T, C extends Collection< T >, D extends Collection< T > > C remove( C coll, D items ) {
        return minus( coll, items );
    }
    public static < T, C extends Collection< T >, D extends Collection< T > > C minus( C coll, D items ) {
        return minus( coll, (T[])items.toArray() );
    }


    public static <T> boolean removeAll( Map< T, ? > map, Collection<T> items ) {
        boolean changed = false;
        for ( T t : items ) {
            Object removed = map.remove( t );
            if ( !changed && removed != null ) changed = true;
        }
        return changed;
    }

    public static < T, M extends Map< T, ? > > M remove( M coll, T... items ) {
        return minus( coll, items );
    }

    public static < T, M extends Map< T, ? > > M minus( M coll, T... items ) {
        removeAll( coll, newList( items ) );
        return coll;
    }

    public static < T, M extends Map< T, ? >, D extends Map<T, ?> > M remove( M coll, D items ) {
        return minus( coll, items );
    }
    public static < T, M extends Map< T, ? >, D extends Map< T, ? > > M minus( M coll, D items ) {
        return minus( coll, (T[])items.keySet().toArray() );
    }



    public static <T> Collection<T> truncate( Collection<T> coll, int maximumSize ) {
        if ( coll.size() > maximumSize ) try {
            Iterator<T> iter = coll.iterator();
            for ( int i = 0; i < maximumSize; ++i ) {
                iter.next();
            }
            while (iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return coll;
    }

    public static boolean intersect( Collection< ? > coll1,
                                     Collection< ? > coll2 ) {
        if ( isNullOrEmpty( coll1 ) ) return false;
        if ( isNullOrEmpty( coll2 ) ) {
            coll1.clear();
            return false;
        }
        Collection<Object> deleteList = new ArrayList<Object>();
        boolean anySame = false;
        for ( Object o : coll1 ) {
            if ( !coll2.contains( o ) ) {
                deleteList.add( o );
            } else {
                anySame = true;
            }
        }
        coll1.removeAll( deleteList );
        return anySame;
    }
    public static String toCamelCase( String s ) {
        if ( s == null ) return null;
        if ( s.isEmpty() || !Character.isLetter( s.codePointAt( 0 ) ) )
            return s;
        char prefix = s.charAt( 0 );
        prefix = Character.toUpperCase( prefix );

        String suffix = s.substring( 1 );
        // check to see if suffix has lower case letters, in which case it is
        // assumed to already be camel case.
        for ( int i = 0; i < suffix.length(); ++i ) {
            char c = suffix.charAt( i );
            if ( Character.isLowerCase( c ) ) {
                suffix = suffix.toLowerCase();
                break;
            }
        }

        return prefix + suffix;
    }
    /**
     * Get the differences between two sets as additions and removals of items
     * of the first to generate the second.
     *
     * @param s1
     *            first set
     * @param s2
     *            second set
     * @return the difference as a set of additions and a set of removals in a
     *         Pair.
     */
    public static < T > Pair< Set< T >, Set< T > > diff( Set< T > s1, Set< T > s2 ) {
        Pair< Set< T >, Set< T > > p =
                new Pair< Set< T >, Set< T > >( new LinkedHashSet< T >(),
                                                new LinkedHashSet< T >() );
        LinkedHashSet< T > s3 = new LinkedHashSet< T >(s1);
        s3.addAll( s2 );
        for ( T t : s3 ) {
            boolean c1 = s1.contains( t );
            boolean c2 = s2.contains( t );
            if ( c1 && c2 ) continue;
            if ( c2 ) p.first.add( t );
            else p.second.add( t );
        }
        return p;
    }

    /**
     * Get the differences between two maps as additions, removals, and updates
     * of items of the first to generate the second.
     *
     * @param map1
     *            first map
     * @param map2
     *            second map
     * @return the difference as ID sets of additions, removals, and deletions
     *         in a List.
     */
    public static < ID, T > List< Set< ID > > diff( Map< ID, T > map1, Map< ID, T > map2 ) {
        List< Set< ID > > diffs = new ArrayList< Set<ID> >();
        Set< ID > added = new LinkedHashSet<ID>();
        Set< ID > removed = new LinkedHashSet<ID>();
        Set< ID > updated = new LinkedHashSet<ID>();

        Set<ID> allIDs = new LinkedHashSet<ID>( map1.keySet() );
        allIDs.addAll( map2.keySet() );
        //System.out.println("allIDs " + allIDs);

        for ( ID id : allIDs ) {
            boolean c1 = map1.keySet().contains( id );
            boolean c2 = map2.keySet().contains( id );
            T t1 = c1 ? map1.get( id ) : null;
            T t2 = c2 ? map2.get( id ) : null;
            if ( c1 && c2 ) {
                if ( t1 != t2 && ( t1 == null || !t1.equals( t2 ) ) ) {
                    updated.add( id );
                }
            } else if ( c2 ) added.add( id );
            else removed.add( id );
        }

        diffs.add( added );
        diffs.add( removed );
        diffs.add( updated );
        return diffs;
    }

    public static boolean isNegative( Number n ) {
        return n.doubleValue() < 0.0;
    }

}
