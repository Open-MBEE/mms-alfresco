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
//import japa.parser.ast.body.Parameter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import junit.framework.Assert;

public class ClassUtils {

  /**
     * Compare argument types to determine how well a function call matches.
     */
    public static class ArgTypeCompare< T > {

      //public Class<?>[] candidateArgTypes = null;
      public Class<?>[] referenceArgTypes = null;
  //    public boolean isVarArgs = false;
      public Object referenceObject = null;
      public Class<?> referenceClass = null;
      public Boolean hasPreference = null;

      public int numMatching = 0;
      public int numNull = 0;
      public int numDeps = 0;
      public boolean okNumArgs = false;
      public int preferenceRank = Integer.MAX_VALUE;

      public T best = null;
      public boolean gotOkNumArgs = false;
      public int mostMatchingArgs = 0;
      public int mostDeps = 0;
      public boolean allArgsMatched = false;
      public boolean allNonNullArgsMatched = false;
      public Class<?>[] bestCandidateArgTypes = null;
      public int bestPreferenceRank = Integer.MAX_VALUE;
      public double bestScore = Double.MAX_VALUE;

      /**
       * @param argTypes1
       * @param argTypes2
       * @param isVarArgs
       */
      public ArgTypeCompare( Class< ? >[] referenceArgTypes ) {
        super();
        this.referenceArgTypes = referenceArgTypes;
      }

      public ArgTypeCompare( Object obj, Class<?> cls, Class< ? >[] referenceArgTypes ) {
          super();
          this.referenceObject = obj;
          this.referenceClass = cls != null ? cls : (obj == null ? null : obj.getClass());
          this.referenceArgTypes = referenceArgTypes;
      }
      
      public boolean hasPreference() {
          if ( referenceObject != null ) return referenceObject instanceof HasPreference;
          if ( hasPreference == null && referenceClass != null ) {
              hasPreference = HasPreference.Helper.classHasPreference( referenceClass );
          }
          return hasPreference;
      }

      public void compare( T o, Class<?>[] candidateArgTypes,
                           boolean isVarArgs ) {
        numMatching = 0;
        numNull = 0;
        numDeps = 0;
        preferenceRank = Integer.MAX_VALUE;
        boolean debugWasOn = Debug.isOn();
        if ( debugWasOn ) Debug.turnOff();
  //      double score = numArgsCost + argMismatchCost * argTypes.length;
        int candidateArgsLength =
            candidateArgTypes == null ? 0 : candidateArgTypes.length;
        int referenceArgsLength =
            referenceArgTypes == null ? 0 : referenceArgTypes.length;
        okNumArgs =
            ( candidateArgsLength == referenceArgsLength )
              || ( isVarArgs
                   && ( candidateArgsLength < referenceArgsLength
                        || candidateArgsLength == 1 ) );
        if ( Debug.isOn() ) Debug.outln( "okNumArgs = " + okNumArgs );
  //      if ( okNumArgs ) score -= numArgsCost;
        for ( int i = 0; i < Math.min( candidateArgsLength,
                                       referenceArgsLength ); ++i ) {
          if ( referenceArgTypes[ i ] == null ) {
            if ( Debug.isOn() ) Debug.outln( "null arg[ " + i + " ]" );
            ++numNull;
            ++numDeps;
            //++numMatching;
            continue;
          }
          if ( candidateArgTypes[ i ] == null ) {
            if ( Debug.isOn() ) Debug.outln( "null arg for args[ " + i
                + " ].getClass()=" + referenceArgTypes[ i ] );
            Debug.error(false, true, "null arg for args[ " + i
                + " ].getClass()=" + referenceArgTypes[ i ] );
            //++numNull;
            //++numDeps;
            continue;
          } else if ( candidateArgTypes[ i ].isAssignableFrom( referenceArgTypes[ i ] ) ) {
              if ( Debug.isOn() ) Debug.outln( "argTypes1[ " + i + " ]="
                           + candidateArgTypes[ i ] + " matches args[ " + i
                           + " ].getClass()=" + referenceArgTypes[ i ] );
            ++numMatching;
          } else if ( candidateArgTypes[ i ].isPrimitive() &&
                      classForPrimitive(candidateArgTypes[ i ]).isAssignableFrom( referenceArgTypes[ i ] ) ) {
            if ( Debug.isOn() ) Debug.outln( "argTypes1[ " + i + " ]="
                         + candidateArgTypes[ i ] + " matches args[ " + i
                         + " ].getClass()=" + referenceArgTypes[ i ] );
            ++numMatching;
//          } else if ( Parameter.class.isAssignableFrom( candidateArgTypes[ i ] ) &&
//                      Expression.class.isAssignableFrom( referenceArgTypes[ i ] ) ) {
//              if ( Debug.isOn() ) Debug.outln( "argTypes1[ " + i + " ]="
//                           + candidateArgTypes[ i ]
//                           + " could be made dependent on args[ " + i
//                           + " ].getClass()=" + referenceArgTypes[ i ] );
//            ++numDeps;
          } else {
              if ( Debug.isOn() ) Debug.outln( "argTypes1[ " + i + " ]="
                           + candidateArgTypes[ i ]
                           + " does not match args[ " + i + " ].getClass()="
                           + referenceArgTypes[ i ] );
          }
        }
        
        boolean isPreferred = false;
        if ( referenceObject != null && bestCandidateArgTypes != null && hasPreference() ) {
            isPreferred = ((HasPreference)referenceObject).prefer( candidateArgTypes, bestCandidateArgTypes );
            if ( isPreferred ) System.out.println( "=====================  YEAH!  ======================" );
        }
        
        if ( ( best == null )
            || ( !gotOkNumArgs && okNumArgs )
            || ( ( gotOkNumArgs == okNumArgs )
                 && ( ( numMatching > mostMatchingArgs )
                      || ( ( numMatching == mostMatchingArgs )
                           && ( ( numDeps > mostDeps )
                                   || ( ( numDeps == mostDeps )
                                        && isPreferred ) ) ) ) ) ) {
         best = o;
         gotOkNumArgs = okNumArgs;
         mostMatchingArgs = numMatching;
         mostDeps = numDeps;
         bestCandidateArgTypes = candidateArgTypes;
         allArgsMatched = ( numMatching >= candidateArgsLength );
         allNonNullArgsMatched = ( numMatching + numNull >= candidateArgsLength );
           if ( Debug.isOn() ) Debug.outln( "new match " + o + ", mostMatchingArgs="
                        + mostMatchingArgs + ",  allArgsMatched = "
                        + allArgsMatched + " = numMatching(" + numMatching
                        + ") >= candidateArgTypes.length("
                        + candidateArgsLength + "), numDeps=" + numDeps );
        }
        if ( debugWasOn ) Debug.turnOn();
      }
    }

    // TODO -- this would be useful for TimeVaryingMap.valueFromString();
//  public static <V> V valueFromString() {
//    V value = null;
//
//    return value;
//  }

//    // boolean, byte, char, short, int, long, float, and double
//    public static Class< ? > primitiveForClass( Class< ? > nonPrimClass ) {
//      if ( nonPrimClass == Integer.class ) {
//        return int.class;
//      }
//      if ( nonPrimClass == Double.class ) {
//        return double.class;
//      }
//      if ( nonPrimClass == Boolean.class ) {
//        return boolean.class;
//      }
//      if ( nonPrimClass == Long.class ) {
//        return long.class;
//      }
//      if ( nonPrimClass == Float.class ) {
//        return float.class;
//      }
//      if ( nonPrimClass == Character.class ) {
//        return char.class;
//      }
//      if ( nonPrimClass == Short.class ) {
//        return short.class;
//      }
//      if ( nonPrimClass == Byte.class ) {
//        return byte.class;
//      }
//      return null;
//    }

  // boolean, byte, char, short, int, long, float, and double
  public static Class< ? > classForPrimitive( Class< ? > primClass ) {
    return getPrimToNonPrim().get( primClass );
  }
  // boolean, byte, char, short, int, long, float, and double
  public static Class< ? > primitiveForClass( Class< ? > nonPrimClass ) {
    return getNonPrimToPrim().get( nonPrimClass );
  }


  private static Map<String,Class<?>> nonPrimitives = initializeNonPrimitives();
  private static Map<String,Class<?>> primitives = initializePrimitives();
  private static Map<Class<?>,Class<?>> primToNonPrim = initializePrimToNonPrim();
  private static Map<Class<?>,Class<?>> nonPrimToPrim = initializeNonPrimToPrim();

  public static Map<String,Class<?>> getPrimitives() {
    if ( primitives == null ) initializePrimitives();
    return primitives;
  }
  public static Map<String,Class<?>> getNonPrimitives() {
    if ( nonPrimitives == null ) initializeNonPrimitives();
    return nonPrimitives;
  }
  public static Map< Class< ? >,Class< ? > > getPrimToNonPrim() {
    if ( primToNonPrim == null ) initializePrimToNonPrim();
    return primToNonPrim;
  }
  public static Map< Class< ? >,Class< ? > > getNonPrimToPrim() {
    if ( nonPrimToPrim == null ) initializeNonPrimToPrim();
    return nonPrimToPrim;
  }

  // boolean, byte, char, short, int, long, float, and double
  public static Class< ? > classForPrimitive( String primClass ) {
    return getPrimitives().get( primClass );
  }

  // boolean, byte, char, short, int, long, float, and double
  private static Map< String, Class< ? > > initializeNonPrimitives() {
    nonPrimitives = new TreeMap< String, Class<?> >();
    for ( Class< ? > c : getNonPrimToPrim().keySet() ) {
      nonPrimitives.put( c.getSimpleName(), c );
    }
    return nonPrimitives;
  }

  // boolean, byte, char, short, int, long, float, and double
  private static Map< String, Class< ? > > initializePrimitives() {
    primitives = new TreeMap< String, Class<?> >();
    for ( Class< ? > c : getPrimToNonPrim().keySet() ) {
      primitives.put( c.getSimpleName(), c );
    }
//    primitives.put( "boolean", boolean.class );
//    primitives.put( "byte", byte.class );
//    primitives.put( "char", char.class );
//    primitives.put( "short", short.class );
//    primitives.put( "int", int.class );
//    primitives.put( "long", long.class );
//    primitives.put( "float", float.class );
//    primitives.put( "double", double.class );
//    primitives.put( "void", void.class );
    return primitives;
  }

  // boolean, byte, char, short, int, long, float, double, and void
  private static Map< Class< ? >, Class< ? > > initializePrimToNonPrim() {
    primToNonPrim = new HashMap< Class< ? >, Class<?> >();
    primToNonPrim.put( boolean.class, Boolean.class );
    primToNonPrim.put( byte.class, Byte.class );
    primToNonPrim.put( char.class, Character.class );
    primToNonPrim.put( short.class, Short.class );
    primToNonPrim.put( int.class, Integer.class );
    primToNonPrim.put( long.class, Long.class );
    primToNonPrim.put( float.class, Float.class );
    primToNonPrim.put( double.class, Double.class );
    primToNonPrim.put( void.class, Void.class );
    return primToNonPrim;
  }

  // boolean, byte, char, short, int, long, float, and double
  private static Map< Class< ? >, Class< ? > > initializeNonPrimToPrim() {
    nonPrimToPrim = new HashMap< Class< ? >, Class<?> >();
    for ( Entry< Class< ? >, Class< ? > > e : getPrimToNonPrim().entrySet() ) {
      nonPrimToPrim.put( e.getValue(), e.getKey() );
    }
    return nonPrimToPrim;
  }


  /**
   * @param type
   * @param preferredPackage
   * @return the Class corresponding to the string
   */
  public static Class< ? > getNonPrimitiveClass( String type, String preferredPackage ) {
    if ( type == null ) return null;
    Class< ? > cls = null;
    cls = getClassForName( type, null, preferredPackage, false );
    return getNonPrimitiveClass( cls );
  }

  /**
   * @param cls the possibly primitive Class object
   * @return the non-primitive class (e.g. Integer.class) corresponding to the
   *         input cls if cls is a primitive class (e.g. int.class); otherwise,
   *         cls (e.g. String.class)
   */
  public static Class< ? > getNonPrimitiveClass( Class< ? > cls ) {
    if ( cls == null ) return null;
    Class< ? > result = classForPrimitive( cls );
    if ( result == null ) return cls;
    return result;
  }

  /**
   * @param o
   * @return whether the object is a primitive class (like int or Integer) or an instance of one
   */
  public static boolean isPrimitive( Class<?> c ) {
    return ( getNonPrimitives().containsKey( c.getSimpleName() ) ||
             getPrimitives().containsKey( c.getSimpleName() ) );
  }


  /**
   * @param o
   * @return whether the object is a primitive class (like int or Integer) or an instance of one
   */
  public static boolean isPrimitive( Object o ) {
    if ( o instanceof Class ) {
      return isPrimitive( (Class<?>)o );
    }
    return isPrimitive( o.getClass() );
  }

  public static String addBackParametersToQualifiedName( String className,
                                                  String qualifiedName ) {
    String parameters = parameterPartOfName( className, true );
    String strippedName = noParameterName( className );
    if ( !Utils.isNullOrEmpty( parameters ) ) {
      String parameters2 = parameterPartOfName( qualifiedName, true );
      if ( Utils.isNullOrEmpty( parameters2 )
           && qualifiedName.endsWith( strippedName ) ) {
        return qualifiedName + parameters;
      }
    }
    return qualifiedName;
  }

  public static String getNonPrimitiveClassName( String type ) {
    if ( type == null ) return null;
    String simpleName = simpleName( type );
    Class<?> prim = getPrimitives().get( simpleName );
    String newName = type;
    if ( prim != null ) {
      Class<?> nonPrim = getPrimToNonPrim().get( prim );
      Assert.assertNotNull( nonPrim );
      newName = nonPrim.getSimpleName();
    }
//    Class< ? > cls = getNonPrimitiveClass( type );
//    String newName = type;
//    if ( cls != null ) {
//      if ( cls.isArray() ) {
//        newName = cls.getSimpleName();
//      } else {
//        newName = cls.getName();
//      }
//      newName = addBackParametersToQualifiedName( type, newName );
//    }
    return newName;
  }

  /**
   * @param c1
   * @param c2
   * @return whether a class, c1, is a subclass of another class, c2
   */
  public static boolean isSubclassOf( Class<?> c1, Class<?> c2 ) {
    try {
      c1.asSubclass( c2 );
    } catch( ClassCastException e ) {
      return false;
    }
    return true;
  }

  /**
   * @param cls
   * @return the class of the single generic Type parameter for the input Class
   *         or null if there is not exactly one.
   */
  public static Class< ? > getSingleGenericParameterType( Class< ? > cls ) {
    if ( cls == null ) return null;
    TypeVariable< ? >[] params = cls.getTypeParameters();
    if ( params != null && params.length == 1 ) {
      Class< ? > cls2;
      try {
        cls2 = Class.forName( params[ 0 ].getName() );
        return cls2;
      } catch ( ClassNotFoundException e ) {
      }
    }
    return null;
  }

  /**
   * @see gov.nasa.jpl.ae.util.ClassUtil#mostSpecificCommonSuperclass(Collection)
   * @param coll
   *          the collection
   * @return the lowest upper bound superclass assignable from all elements in
   *         the collection.
   *         <p>
   *         For example, If coll contains Integer.class and Double.class,
   *         leastUpperBoundSuperclass( coll ) returns Number.class because they
   *         both inherit from Number and Object, and Number is lower (more
   *         specific) than Object.
   */
  public static Class<?> leastUpperBoundSuperclass( Collection< ? > coll ) {
    return mostSpecificCommonSuperclass( coll );
  }

  /**
   * @param coll
   *          the collection
   * @return the most specific (lowest) common superclass assignable from all
   *         elements in the collection.
   *         <p>
   *         For example, If coll contains Integer.class and Double.class,
   *         leastUpperBoundSuperclass( coll ) returns Number.class because they
   *         both inherit from Number and Object, and Number is lower (more
   *         specific) than Object.
   *         <p>
   *         Note that if the the classes implement several common interfaces
   *         but directly extend Object, Object.class will be returned.
   */
  public static Class<?> mostSpecificCommonSuperclass( Collection< ? > coll ) {
    if ( Utils.isNullOrEmpty( coll ) ) return null;
    if ( areClasses( coll ) ) {
        return mostSpecificCommonSuperclass( Utils.toArrayOfType( coll.toArray(), Class.class ) );
    }
    ArrayList< Class<?> > classes = new ArrayList<Class<?> >();
    for ( Object o : coll ) {
        if ( o != null ) classes.add( o.getClass() );
    }
    return mostSpecificCommonSuperclass( Utils.toArrayOfType( classes, Class.class ) );
  }

    /**
     * @param classes
     *            an array of classes
     * @return the most specific (lowest) common superclass of all classes in
     *         the array.
     *         <p>
     *         For example, If the classes contained Integer.class and Double.class,
     *         mostSpecificCommonSuperclass( classes ) returns Number.class because
     *         they both inherit from Number and Object, and Number is lower
     *         (more specific) than Object.
     *         <p>
     *         Note that if the classes implement several common interfaces
     *         but directly extend Object, Object.class will be returned.
     */
  public static Class<?> mostSpecificCommonSuperclass( Class< ? >[] classes ) {
    if ( Utils.isNullOrEmpty( classes ) ) return null;
    Class<?> most = null;
    for ( Class<?> c : classes ) {
      if ( c == null ) continue;
      Class<?> cls = getNonPrimitiveClass( c );
      if ( most == null ) {
        most = cls;
        continue;
      }
      while ( cls != null && !cls.isAssignableFrom( most ) ) {
        cls = cls.getSuperclass();
      }
      if ( cls != null && cls.isAssignableFrom( most ) ) {
        most = cls;
      }
    }
    return most;
  }

  public static Class< ? > tryClassForName( String className,
                                            boolean initialize ) {
    return tryClassForName( className, initialize, null );
  }

  public static Class< ? > classForName( String className ) throws ClassNotFoundException {
    if (Utils.isNullOrEmpty( className )) return null;
    Thread t = Thread.currentThread();
    Class< ? > cls = null;
    ClassLoader[] loaders = new ClassLoader[] { t.getContextClassLoader(),
                                                ClassLoader.getSystemClassLoader(),
                                                ClassUtils.class.getClassLoader() };
    for ( ClassLoader cl : loaders ) {
        if ( cl != null ) {
            try {
                cls = cl.loadClass(className);
                if ( cls != null ) {
                    Debug.outln( "classForName(" + className + ") = " + cls.getSimpleName() );
                    break;
                }
            } catch ( Throwable e ) {
                Debug.errln( "classForName(" + className
                             + ") failed for loader: " + cl + "\n"
                             + e.getLocalizedMessage() );
            }
        }
    }
    return cls;
  }

  public static Class< ? > tryClassForName( String className,
                                            boolean initialize,
                                            ClassLoader myLoader ) {
    if ( Debug.isOn() ) Debug.outln( "trying tryClassForName( " + className + " )");
    Class< ? > classForName = null;
    if ( myLoader == null ) myLoader = Utils.loader;
    //if ( myLoader == null ) myLoader = gov.nasa.jpl.ae.event.Expression.class.getClassLoader();
    try {
      classForName = classForName( className );
    } catch ( ClassNotFoundException e1 ) {
      // ignore
    }
    if ( classForName == null ) {
      try {
        if ( myLoader == null ) {
          classForName = Class.forName( className );
        } else {
        classForName = Class.forName( className, initialize, myLoader );//, initialize, Utils.class.getClassLoader() );
        }
      } catch ( NoClassDefFoundError e ) {
        // ignore
      } catch ( ClassNotFoundException e ) {
        // ignore
      }
    }
    if ( classForName == null ) {
      classForName = getClassOfClass( className, "", initialize );
    }
    if ( Debug.isOn() ) Debug.outln( "tryClassForName( " + className + " ) = " + classForName );
    return classForName;
  }

  public static String replaceAllGenericParameters( String className, char replacement ) {
    if ( Utils.isNullOrEmpty( className ) ) return className;
    StringBuffer newName = new StringBuffer( className );
    int parameterDepth = 0;
    for ( int i=0; i<className.length(); ++i ) {
      char c = className.charAt( i );
      boolean replace = false;
      switch( c ) {
        case '>':
          --parameterDepth;
          replace = true;
          break;
        case '<':
          ++parameterDepth;
          replace = true;
          break;
        default:
          if ( parameterDepth > 0 ) replace = true;
      }
      if ( replace ) {
        newName.setCharAt( i, replacement );
      }
      if ( parameterDepth < 0 ) {
        Debug.error( false,
                     "Error! Generic parameter nesting is invalid for class name:\n  \""
                         + className + "\" at char #" + Integer.toString( i )
                         + "\n" + Utils.spaces( i + 3 ) + "^" );
      }
    }
    return newName.toString();
  }

  public static Class<?> getClassOfClass( String longClassName,
                                          String preferredPackageName,
                                          boolean initialize ) {
    Class< ? > classOfClass = null;
    String clsOfClsName = replaceAllGenericParameters( longClassName, '~' );
    int pos = clsOfClsName.lastIndexOf( '.' );
    if ( pos >= 0 ) {
      String clsOfClsName1 = longClassName.substring( 0, pos );
      String clsOfClsName2 = longClassName.substring( pos+1 );
      classOfClass =
          getClassOfClass( clsOfClsName1, clsOfClsName2, preferredPackageName,
                           initialize );
    }
    return classOfClass;
  }

  public static Class<?> getClassOfClass( String className,
                                          String clsOfClsName,
                                          String preferredPackageName,
                                          boolean initialize ) {
    Class< ? > classOfClass = null;
    Class< ? > classForName =
        getClassForName( className, null, preferredPackageName, initialize );
    if ( classForName == null ) {
      classForName =
          getClassOfClass( className, preferredPackageName, initialize );
    }
    if ( classForName != null ) {
      classOfClass = getClassOfClass( classForName, clsOfClsName );
    }
    return classOfClass;
  }

  public static Class<?> getClassOfClass( Class<?> cls,
                                            String clsOfClsName ) {
      if ( cls != null ) {
        Class< ? >[] classes = cls.getClasses();
        if ( classes != null ) {
          String clsName = cls.getName();
          String clsSimpleName = cls.getSimpleName();
          String longName = clsName + "." + clsOfClsName;
          String shorterName = clsSimpleName + "." + clsOfClsName;
          for ( int i = 0; i < classes.length; ++i ) {
            String n = classes[ i ].getName();
            String sn = classes[ i ].getSimpleName();
            if ( n.equals( longName )
                 || n.equals( shorterName )
                 || sn.equals( clsOfClsName )
  //               || n.endsWith( "." + longName )
  //               || n.endsWith( "." + shorterName )
                 || n.endsWith( "." + clsOfClsName ) ) {
              return classes[ i ];
            }
          }
        }
      }
      return null;
    }

  // TODO -- expand to include member names, too: className -> memberName -> Class
  public static Map< String, Class< ? > > classCache =
      Collections.synchronizedMap( new HashMap< String, Class< ? > >() );
  

  public static Class<?> getClassForName(String className, String memberName,
		  								 String[] packages,  boolean initialize) {

      if ( Utils.isNullOrEmpty( className ) ) return null;

      Class< ?  > cls = null;

      // Check the packages for the fname:
      for (String pkg : packages) {

        Class< ? > foo = ClassUtils.getClassForName( className, memberName,
                                                                   pkg, initialize );
        if (foo != null) {
          cls = foo;
          break;
        }
      }

      return cls;
  }

  public static Class<?> getClassForName( String className, String memberName,
                                          String preferredPackage, boolean initialize ) {
    if ( Utils.isNullOrEmpty( className ) ) return null;
    Class<?> cls = null;
    char firstChar = className.charAt( 0 );
    if ( firstChar >= 'a' && firstChar <= 'z' ) {
      cls = classForPrimitive( className );
      if ( cls != null
           && ( Utils.isNullOrEmpty( memberName ) || hasMember( cls, memberName ) ) ) {
        return cls;
      }
    }
    Class< ? > cls2 = classCache.get( className );
    if ( cls2 != null ) {
      if ( Utils.isNullOrEmpty( memberName ) || hasMember( cls2, memberName ) ) {
        return cls2;
      }
      if ( cls == null ) cls = cls2;
    }
    List< Class<?>> classList = getClassesForName( className, initialize );
    if ( !Utils.isNullOrEmpty( classList ) && initialize && !isPackageName( className ) ) {  // REVIEW
      classList = getClassesForName( className, !initialize );
    }
    if ( !Utils.isNullOrEmpty( classList ) ) {
//        Class< ? > best = null;
//        boolean bestHasSpecifier = false;
//        boolean bestInPreferredPackage = false;
//        for ( Class< ? > c : classList ) {
//          boolean inPreferredPackage = false;
//          boolean hasSpecifier = hasMember( c, specifier );
//          if ( inPackage( c, preferredPackage ) ) {
//            classCache.put( className, c );
//            if ( hasSpecifier ) return c;
//            inPreferredPackage = true;
//          }
//        }
      cls2 = getClassFromClasses( classList, memberName, preferredPackage );
      if ( cls2 != null ) cls = cls2;
    }
    if ( cls != null ) classCache.put( className, cls );
    return cls;
  }
  //  public static Class<?> getClassForName( String className,
  //                                          boolean initialize ) {
  //    return getClassFromClasses( getClassesForName( className, initialize ) );
  //  }

  public static HashMap< String, List< Class<?> > > classesCache =
      new HashMap< String, List<Class<?>> >();

  public static boolean optimistic = false;  // try to find again even if failed in the past
  
  public static List< Class< ? > > getClassesForName( String className,
                                                        boolean initialize ) {
  //                                                    ClassLoader loader,
  //                                                    Package[] packages) {
    if ( Debug.isOn() ) Debug.outln( "getClassesForName( " + className + " )" );
    List< Class< ? > > classList = classesCache.get( className );
    if ( Debug.isOn() ) Debug.outln("classList " + classList + " from classesCache " + classesCache );
    if ( classList != null && ( !optimistic || !classList.isEmpty() ) ) {
      if ( Debug.isOn() ) Debug.outln( "getClassesForName( " + className + " ) returning " + classList );
      return classList;
    }
    classList = new ArrayList< Class< ? > >();
    if ( Utils.isNullOrEmpty( className ) ) {
      if ( Debug.isOn() ) Debug.outln( "getClassesForName( " + className + " ) rempty className - returning null" );
      return null;
    }
  //    ClassLoader loader = Utils.class.getClassLoader();
  //    if ( loader != null ) {
  //      for ( String pkgName : packagesToForceLoad ) {
  //        try {
  //          loader.getResources( pkgName);
  //          loader.getResources( pkgName + File.separator + "*" );
  //          loader.loadClass("");
  //        } catch ( IOException e ) {
  //          // ignore
  //        } catch ( ClassNotFoundException e ) {
  //          // ignore
  //        }
  //      }
  //    }
      Class< ? > classForName = tryClassForName( className, initialize );//, loader );
      if ( classForName != null ) classList.add( classForName );
      String strippedClassName = noParameterName( className );
      if ( Debug.isOn() ) Debug.outln( "getClassesForName( " + className + " ): strippedClassName = "
                   + strippedClassName );
      boolean strippedWorthTrying = false;
      if ( !Utils.isNullOrEmpty( strippedClassName ) ) {
        strippedWorthTrying = !strippedClassName.equals( className );
        if ( strippedWorthTrying ) {
          classForName = tryClassForName( strippedClassName, initialize );//, loader );
          if ( classForName != null ) classList.add( classForName );
        }
      }
      List<String> FQNs = getFullyQualifiedNames( className );//, packages );
      if ( Debug.isOn() ) Debug.outln( "getClassesForName( " + className + " ): fully qualified names = "
          + FQNs );
      if ( FQNs.isEmpty() && strippedWorthTrying ) {
        FQNs = getFullyQualifiedNames( strippedClassName );//, packages );
      }
      if ( !FQNs.isEmpty() ) {
        for ( String fqn : FQNs ) {
          classForName = tryClassForName( fqn, initialize );//, loader );
          if ( classForName != null ) classList.add( classForName );
        }
      }
      if ( classList != null ) {
        classesCache.put( className, classList );
      }
      if ( Debug.isOn() ) Debug.outln( "getClassesForName( " + className + " ) returning " + classList );
      return classList;
    }

  public static Collection<String> getPackageStrings(Package[] packages) {
    Set<String> packageStrings = new TreeSet<String>();
    if ( Utils.isNullOrEmpty( packages ) ) {
      packages = Package.getPackages();
    }
    for (Package aPackage : packages ) {
      packageStrings.add(aPackage.getName());
    }
    return packageStrings;
  }

  public static boolean isPackageName( String packageName ) {
    Package[] packages = Package.getPackages();
    for (Package aPackage : packages ) {
      String packageString = aPackage.getName();
      if ( packageString.startsWith( packageName ) ) {
        if ( packageString.charAt( packageName.length() ) == '.' ) return true;
      }
    }
//    Collection<String> packageStrings = getPackageStrings( null );
//    if ( packageStrings.contains( packageName ) ) return true;
//    for ( String packageString : packageStrings ) {
//      if ( packageString.startsWith( packageName ) ) {
//        if ( packageString.charAt( packageName.length() ) == '.' ) return true;
//      }
//    }
    return false;
  }

  /**
   * @param c
   * @param packageName
   * @return whether the Class is in the package with the given name
   */
  public static boolean inPackage( Class<?> c, String packageName ) {
    return isInPackage( c, packageName );
  }
  /**
   * @param c
   * @param packageName
   * @return whether the Class is in the package with the given name
   */
  public static boolean isInPackage( Class<?> c, String packageName ) {
    if ( c.getPackage().getName().equals( packageName ) ) {
      if ( Debug.isOn() ) Debug.errln("Found package! " + packageName );
      return true;
    }
    return false;
  }

  public static String simpleName( String longName ) {
    if ( longName == null ) return null;
    int pos = longName.lastIndexOf( "." );
    return longName.substring( pos+1 ); // pos is -1 if no '.'
  }

  public static String getFullyQualifiedName( String classOrInterfaceName,
                                              boolean doTypeParameters ) {
    String typeParameters = "";
    if ( classOrInterfaceName.contains( "<" )
         && classOrInterfaceName.contains( ">" ) ) {
      typeParameters = parameterPartOfName( classOrInterfaceName, false );
      // TODO -- how does this work for multiple parameters? e.g., Map<String,Float>
      String check = replaceAllGenericParameters( typeParameters, '~' );
      Assert.assertFalse( check.contains( "," ) );
      if ( typeParameters != null && typeParameters.contains( "Customer" ) ) {
        Debug.breakpoint();
      }
      typeParameters = "<" + ( doTypeParameters
                               ? getFullyQualifiedName( typeParameters, true )
                               : getNonPrimitiveClassName( typeParameters ) ) + ">";
      classOrInterfaceName =
          classOrInterfaceName.substring( 0, classOrInterfaceName.indexOf( '<' ) );
    }
    List< String > names = getFullyQualifiedNames( classOrInterfaceName );
    if ( Utils.isNullOrEmpty( names ) ) {
      names = getFullyQualifiedNames( simpleName( classOrInterfaceName ) );
    }
    if ( !Utils.isNullOrEmpty( names ) ) {
      for ( String n : names ) {
        if ( n.endsWith( classOrInterfaceName ) ) {
          classOrInterfaceName = n;
          break;
        }
      }
    }
    return classOrInterfaceName + typeParameters;
  }

  public static List<String> getFullyQualifiedNames(String simpleClassOrInterfaceName ) {
    return getFullyQualifiedNames( simpleClassOrInterfaceName, null );
  }

  public static List<String> getFullyQualifiedNames(String simpleClassOrInterfaceName, Package[] packages) {
    Collection<String> packageStrings = getPackageStrings( packages );

    List<String> fqns = new ArrayList<String>();
    //if ( Debug.isOn() ) Debug.outln( "getFullyQualifiedNames( " + simpleClassOrInterfaceName
    //             + " ): packages = " + packageStrings );
    for (String aPackage : packageStrings) {
        try {
            String fqn = aPackage + "." + simpleClassOrInterfaceName;
            Class.forName(fqn);
            fqns.add(fqn);
        } catch (NoClassDefFoundError e) {
          // Ignore
        } catch (Exception e) {
            // Ignore
        }
    }
    if ( Debug.isOn() ) Debug.outln( "getFullyQualifiedNames( " + simpleClassOrInterfaceName
                 + " ): returning " + fqns );
    return fqns;
  }

  public static Constructor< ? > getConstructorForArgs( String className,
                                                        Object[] args,
                                                        String preferredPackage ) {
    Class< ? > classForName = getClassForName( className, null, preferredPackage, false );
    if ( classForName == null ) {
      System.err.println( "Couldn't find the class " + className
                          + " to get constructor with args=" + Utils.toString( args, false ) );
      return null;
    }
    return getConstructorForArgs( classForName, args );
  }

  public static Constructor< ? > getConstructorForArgTypes( String className,
                                                            Class<?>[] argTypes,
                                                            String preferredPackage ) {
    Class< ? > classForName = getClassForName( className, null, preferredPackage, false );
    if ( classForName == null ) {
      System.err.println( "Couldn't find the class " + className
                          + " to get constructor with args=" + Utils.toString( argTypes, false ) );
      return null;
    }
    return getConstructorForArgTypes( classForName, argTypes );
  }

  
  public static Vector<Object> wrappedObjects( Object object, boolean propagate  ) {
      Vector< Object > list = new Vector<Object>();
      if ( object == null ) return list;
      Seen<Object> seen = null;
      Object o = object;
      Pair< Boolean, Seen< Object > > p;
      while ( !(p = Utils.seen( o, true, seen )).first ) {
          seen = p.second;
          list.add( o );
          if ( o instanceof Wraps ) {
              o = ((Wraps)o).getValue( propagate );
          } else break;
      }
      return list;
  }
  
  public static boolean classMatches( Class<?> parentCls, Object object, boolean propagate ) {
      if ( object == null ) return false;
      Seen<Object> seen = null;
      Object o = object;
      Pair< Boolean, Seen< Object > > p;
      while ( !(p = Utils.seen( o, true, seen )).first ) {
          seen = p.second;
          Class<?> c = o.getClass();
          if ( parentCls.isInstance( o ) ) return true;
          if ( o instanceof Class ) {
              Class<?> oc = (Class<?>)o;
              if ( parentCls.isAssignableFrom( c ) ) return true;
              // REVIEW -- try c.isAssignable( cls )?
          }
          if ( o instanceof Wraps ) {
              o = ((Wraps)o).getValue( propagate );
              if ( o == null ) break;
          } else break;
      }
      return false;
  }
  
  
    public static boolean classesMatch( Class< ? >[] parentClasses,
                                        Object[] instances, boolean propagate ) {
        if ( parentClasses == instances ) return true;
        if ( parentClasses == null || instances == null ) return false;
        if ( parentClasses.length != instances.length ) return false;
        for ( int i = 0; i < parentClasses.length; ++i ) {
            if ( !classMatches( parentClasses[ i ], instances[ i ], propagate ) ) {
                return false;
            }
        }
        return true;
    }

    public static boolean classesMatch( Class< ? >[] parentClasses,
                                        Class< ? >[] subclasses ) {
        if ( parentClasses == subclasses ) return true;
        if ( parentClasses == null || subclasses == null ) return false;
        if ( parentClasses.length != subclasses.length ) return false;
        for ( int i = 0; i < parentClasses.length; ++i ) {
            if ( !parentClasses[ i ].isAssignableFrom( subclasses[ i ] ) ) {
                return false;
            }
        }
        return true;
    }
  
  public static Class<?>[] getClasses( Object[] objects ) {
    //return toClass( objects );
    Class< ? > argTypes[] = null;
    if ( objects != null ) {
      argTypes = new Class< ? >[ objects.length ];
      for ( int i = 0; i < objects.length; ++i ) {
        if ( objects[ i ] == null ) {
          argTypes[ i ] = null;
        } else {
          argTypes[ i ] = objects[ i ].getClass();
        }
      }
    }
    return argTypes;
  }

  /**
   * Determines whether the array contains only Class<?> objects possibly with
   * some (but not all nulls).
   *
   * @param objects
   * @return
   */
  public static boolean areClasses( Object[] objects ) {
    boolean someClass = false;
    boolean someNotClass = false;
    for ( Object o : objects ) {
      if ( o == null ) continue;
      if ( o instanceof Class ) {
        someClass = true;
      } else {
        someNotClass = true;
      }
    }
    return !someNotClass && someClass;
  }

  /**
   * Determines whether the list contains only Class<?> objects possibly with
   * some (but not all nulls).
   *
   * @param objects
   * @return
   */
  public static boolean areClasses( Collection<?> objects ) {
      return areClasses( objects.toArray() );
  }

  public static Constructor< ? > getConstructorForArgs( Class< ? > cls,
                                                          Object[] args ) {
      if ( Debug.isOn() ) Debug.outln( "getConstructorForArgs( " + cls.getName() + ", "
                   + Utils.toString( args ) );
      Class< ? > argTypes[] = getClasses( args );
      return getConstructorForArgTypes( cls, argTypes );
  /*    //Method matchingMethod = null;
      boolean gotOkNumArgs = false;
      int mostMatchingArgs = 0;
      boolean allArgsMatched = false;
      Constructor< ? > ctor = null;

      for ( Constructor< ? > aCtor : cls.getConstructors() ) {
          int numMatching = 0;
          boolean okNumArgs =
              ( aCtor.getParameterTypes().length == args.length )
                  || ( aCtor.isVarArgs()
                       && ( aCtor.getParameterTypes().length < args.length
                            || aCtor.getParameterTypes().length == 1 ) );
          //if ( !okNumArgs ) continue;
          for ( int i = 0; i < Math.min( aCtor.getParameterTypes().length,
                                         args.length ); ++i ) {
            if ( args[ i ] == null ) continue;
            if ( ((Class<?>)aCtor.getParameterTypes()[ i ]).isAssignableFrom( args[ i ].getClass() ) ) {
              ++numMatching;
            }
          }
          if ( ( ctor == null ) || ( okNumArgs && !gotOkNumArgs )
               || ( ( okNumArgs && !gotOkNumArgs )
                    && ( numMatching > mostMatchingArgs ) ) ) {
            ctor = aCtor;
            gotOkNumArgs = okNumArgs;
            mostMatchingArgs = numMatching;
            allArgsMatched = ( numMatching >= aCtor.getParameterTypes().length );
          }
        }
      if ( ctor != null && !allArgsMatched ) {
        System.err.println( "constructor returned (" + ctor
                            + ") does not match all args: " + toString( args ) );
      } else if ( ctor == null ) {
        System.err.println( "constructor " + cls.getSimpleName() + toString( args )
                            + " not found" );
      }
      return ctor;
  */  }

  public static Pair< Constructor< ? >, Object[] >
      getConstructorForArgs( Class< ? > eventClass, Object[] arguments,
                             Object enclosingInstance ) {
    boolean nonStaticInnerClass = isInnerClass( eventClass );
    if ( Debug.isOn() ) Debug.outln( eventClass.getName() + ": nonStaticInnerClass = " + nonStaticInnerClass );
    Object newArgs[] = arguments;
    if ( nonStaticInnerClass ) {
      int argumentsLength = arguments == null ? 0 : arguments.length;
      newArgs = new Object[ argumentsLength + 1 ];
      assert enclosingInstance != null;
      newArgs[ 0 ] = enclosingInstance;
      for ( int i = 0; i < argumentsLength; ++i ) {
        newArgs[ i + 1 ] = arguments[ i ];
      }
    }
    Constructor< ? > constructor =
        getConstructorForArgs( eventClass, newArgs );
    return new Pair< Constructor< ? >, Object[] >(constructor, newArgs );
  }

  public static < T > T getBestArgTypes( Map< T, Pair< Class< ? >[], Boolean > > candidates,
                                         // ConstructorDeclaration[] ctors,
                                         Class< ? >... argTypes ) {
    ArgTypeCompare< T > atc =
        new ArgTypeCompare< T >( argTypes );
    for ( Entry< T, Pair< Class< ? >[], Boolean > > e : candidates.entrySet() ) {
      atc.compare( e.getKey(), e.getValue().first, e.getValue().second );
    }
    if ( atc.best != null && !atc.allNonNullArgsMatched ) {
      System.err.println( "constructor returned (" + atc.best
                          + ") only matches " + atc.mostMatchingArgs
                          + " args: " + Utils.toString( argTypes, false ) );
    } else if ( atc.best == null ) {
      System.err.println( "best args not found in " + candidates );
    }
    return atc.best;
  }

  public static Constructor< ? > getConstructorForArgTypes( Constructor<?>[] ctors,
                                                              Class< ? >... argTypes ) {
      //Constructor< ? >[] ctors = null;
      ArgTypeCompare< Constructor< ? > > atc =
          new ArgTypeCompare< Constructor< ? > >( argTypes );
      for ( Constructor< ? > aCtor : ctors) {
        atc.compare( aCtor, aCtor.getParameterTypes(), aCtor.isVarArgs() );
      }
      if ( atc.best != null && !atc.allNonNullArgsMatched ) {
        System.err.println( "constructor returned (" + atc.best
                            + ") only matches " + atc.mostMatchingArgs
                            + " args: " + Utils.toString( argTypes, false ) );
      } else if ( atc.best == null ) {
        System.err.println( "constructor not found in " + ctors );
  //                          cls.getSimpleName()
  //                          + toString( argTypes, false ) + " not found for "
  //                          + cls.getSimpleName() );
      }
      return atc.best;
    }

  public static Constructor< ? > getConstructorForArgTypes( Class< ? > cls,
                                                              Class< ? >... argTypes ) {
      if ( argTypes == null ) argTypes = new Class< ? >[] {};
      if ( Debug.isOn() ) Debug.outln( "getConstructorForArgTypes( cls=" + cls.getName()
                   + ", argTypes=" + Utils.toString( argTypes ) + " )" );
      return getConstructorForArgTypes( cls.getConstructors(), argTypes );
  /*    ArgTypeCompare atc = new ArgTypeCompare( argTypes );
      for ( Constructor< ? > aCtor : cls.getConstructors() ) {
        atc.compare( aCtor, aCtor.getParameterTypes(), aCtor.isVarArgs() );
      }
      if ( atc.best != null && !atc.allArgsMatched ) {
        System.err.println( "constructor returned (" + atc.best
                            + ") only matches " + atc.mostMatchingArgs
                            + " args: " + toString( argTypes, false ) );
      } else if ( atc.best == null ) {
        System.err.println( "constructor " + cls.getSimpleName()
                            + toString( argTypes, false ) + " not found for "
                            + cls.getSimpleName() );
      }
      return (Constructor< ? >)atc.best;
  */  }

  public static Constructor< ? >
    getConstructorForArgTypes( Class< ? > cls, String packageName ) {
    Pair< Constructor< ? >, Object[] > p =
        getConstructorForArgs(cls, new Object[]{}, packageName );
    if ( p == null ) return null;
    return p.first;
  }

  public static boolean isInnerClass( Class< ? > eventClass ) {
    return ( !Modifier.isStatic( eventClass.getModifiers() )
             && eventClass.getEnclosingClass() != null );
  }

  public static Class< ? > getClassFromClasses( List< Class< ? > > classList,
                                                String specifier,
                                                String preferredPackage ) {
    if ( Utils.isNullOrEmpty( classList ) ) {
      return null;
    }
    if ( classList.size() > 1 ) {
      if ( Debug.isOn() ) {
        Debug.outln( "getClassFromClasses(" + classList + ", specifier="
                     + specifier + ", preferredPackage=" + preferredPackage
                     + ")" );
        Debug.outln( "Got multiple class candidates for constructor! "
                     + classList );
      }
      Class<?> bestCls = null;
      int bestLength = Integer.MAX_VALUE;
      int bestLengthOfCommonPreferredPkgPrefix = 0;
      int bestLengthOfCommonPkgPrefix = 0;
      //boolean bestHasMember = false;
      boolean bestHasSpecifier = false;
      boolean bestInPreferredPackage = false;

      for ( Class<?> c : classList ) {
        boolean best = false;
        int lengthOfCommonPkgPrefix = Integer.MAX_VALUE;
        int lengthOfCommonPreferredPkgPrefix = Integer.MAX_VALUE;
        int length = 0;
        boolean hasSpecifier = hasMember( c, specifier );
        boolean inPreferredPackage = inPackage( c, preferredPackage );
        if ( !inPreferredPackage ) {
          lengthOfCommonPreferredPkgPrefix =
              lengthOfCommonPrefix( c.getPackage().getName(), preferredPackage );
          lengthOfCommonPkgPrefix =
              lengthOfCommonPrefix( c.getPackage().getName(),
                                    ClassUtils.class.getPackage().getName() );
          length = c.getPackage().getName().length();
        }
        best = ( bestCls == null ||
                 ( hasSpecifier && !bestHasSpecifier ) ||
                 ( inPreferredPackage && !bestInPreferredPackage ) ||
                 ( lengthOfCommonPreferredPkgPrefix > bestLengthOfCommonPreferredPkgPrefix ) ||
                 ( ( lengthOfCommonPreferredPkgPrefix == bestLengthOfCommonPreferredPkgPrefix ) &&
                     ( ( lengthOfCommonPkgPrefix > bestLengthOfCommonPkgPrefix ) ||
                       ( ( lengthOfCommonPkgPrefix == bestLengthOfCommonPkgPrefix ) &&
                         ( length < bestLength ) ) ) ) );
        if ( best ) {
          bestCls = c;
          bestLengthOfCommonPreferredPkgPrefix = lengthOfCommonPreferredPkgPrefix;
          bestLengthOfCommonPkgPrefix = lengthOfCommonPkgPrefix;
          bestLength = length;
        }
      }
      Debug.outln( "Best class " + bestCls.getCanonicalName()
                          + " has length, " + bestLength
                          + ", and common prefix length of packages, "
                          + bestLengthOfCommonPkgPrefix + ", pkg="
                          + bestCls.getPackage().getName()
                          + ", ClassUtils pkg="
                          + ClassUtils.class.getPackage().getName()
                          + ", and common prefix length of packages, "
                          + bestLengthOfCommonPreferredPkgPrefix
                          + ", preferredPackage pkg=" + preferredPackage );
      if ( Debug.isOn() && !bestHasSpecifier && !bestInPreferredPackage ) {
        Debug.errln( "Warning! Picked non-matching candidate from multiple class candidates! " + classList );
      }

      return bestCls;
    }
    assert( classList.size() == 1 );
    return classList.get( 0 );
  }

  public static Method getJavaMethodForCommonFunction( String functionName,
                                                       List<Object> args ) {
    return getJavaMethodForCommonFunction( functionName, args.toArray() );
  }

  // TODO -- feed this through ArgTypeCompare to avoid mismatches
    public static Method getJavaMethodForCommonFunction( String functionName,
                                                         Object[] args ) {
  //    boolean alreadyClass = areClasses( args );
      Class<?>[] argTypes = null;
  //    if ( alreadyClass ) {
  //      argTypes = new Class<?>[args.length];
  //      boolean ok = toArrayOfType( args, argTypes, Class.class );
  //      assert ok;
  //    } else {
        argTypes = getClasses(args);
  //    }
      return getJavaMethodForCommonFunction( functionName, argTypes );
    }

  public static Method getJavaMethodForCommonFunction( String functionName,
                                                       Class[] argTypes ) {
    // REVIEW -- Could use external Reflections library to get all classes in a
    // package:
    //   Reflections reflections = new Reflections("my.project.prefix");
    //   Set<Class<? extends Object>> allClasses =
    //      reflections.getSubTypesOf(Object.class);
    //   use on:
    //     java.lang
    //     org.apache.commons.lang.
    //     java.util?
    Class< ? >[] classes =
        new Class< ? >[] { Math.class, //StringUtils.class,
                           Integer.class,
                           Double.class, Character.class, Boolean.class,
                           String.class,
                           //org.apache.commons.lang.ArrayUtils.class,
                           Arrays.class,
                           Collections.class,
                           Utils.class,
                           ClassUtils.class,
                           TimeUtils.class,
                           CompareUtils.class,
                           FileUtils.class};
    for ( Class<?> c : classes ) {
      Method m = getMethodForArgTypes( c, functionName, argTypes );
      if ( m != null ) return m;
    }
    return null;
  }

  public static Method getMethodForArgs( String className,
                                         String preferredPackage,
                                         String callName,
                                         Object... args ) {
    Class< ? > classForName = getClassForName( className, callName, preferredPackage, false );
    if ( Debug.errorOnNull( "Couldn't find the class " + className + " for method "
                      + callName + ( args == null ? "" : Utils.toString( args, false ) ),
                      classForName ) ) {
      return null;
    }
    return getMethodForArgs( classForName, callName, args );
  }

  public static Method getMethodForArgs( Class< ? > cls, String callName,
                                         Object... args ) {
      return getMethodForArgs(cls, callName, true, args );
  }
  public static Method getMethodForArgs( Class< ? > cls, String callName,
                                         boolean complain,
                                         Object... args ) {
        if ( Debug.isOn() ) Debug.outln( "getMethodForArgs( cls="
                                         + ( cls == null ? "null"
                                                         : cls.getName() )
                                         + ", callName=" + callName + ", args="
                                         + Utils.toString( args ) + " )" );
      Class< ? > argTypes[] = null;
  //    boolean allClasses = areClasses( args );
  //    if ( allClasses ) {
  //      //argTypes = (Class< ? >[])args;
  //      boolean ok = toArrayOfType( args, argTypes, Class.class );
  //      assert ok;
  //    } else {
        argTypes = getClasses( args );
  //    }
      return getMethodForArgTypes( cls, callName, argTypes, complain );
    }

  public static Method getMethodForArgTypes( String className,
                                             String preferredPackage,
                                             String callName,
                                             Class<?>... argTypes ) {
      return getMethodForArgTypes( className, preferredPackage, callName, true,
                                   argTypes );
  }
  public static Method getMethodForArgTypes( String className,
                                             String preferredPackage,
                                             String callName,
                                             boolean complain,
                                             Class<?>... argTypes ) {
    return getMethodForArgTypes( className, preferredPackage, callName,
                                 argTypes, complain );
  }

  private static int lengthOfCommonPrefix( String s1, String s2 ) {
    if ( s1 == null || s2 == null ) return 0;
    int i=0;
    for ( ; i < Math.min( s1.length(), s2.length() ); ++i ) {
      if ( s1.charAt(i) != s2.charAt(i) ) {
        break;
      }
    }
    return i;
  }

  public static Method getMethodForArgTypes( String className,
                                             String preferredPackage,
                                             String callName,
                                             Class<?>[] argTypes,
                                             boolean complainIfNotFound ) {
    //Debug.turnOff();  // DELETE ME -- FIXME
    Debug.outln("=========================start===============================");
    //Debug.errln("=========================start===============================");
    //Class< ? > classForName = getClassForName( className, preferredPackage, false );
    String classNameNoParams = noParameterName( className );
    List< Class< ? > > classesForName = getClassesForName( classNameNoParams, false );
    //Debug.err("classForName = " + classForName );
    Debug.outln("classesForName = " + classesForName );
    if ( Utils.isNullOrEmpty( classesForName ) ) {
      if ( complainIfNotFound ) {
        System.err.println( "Couldn't find the class " + className + " for method "
                     + callName
                     + ( argTypes == null ? "" : Utils.toString( argTypes, false ) ) );
      }
      Debug.outln("===========================end==============================");
      //Debug.errln("===========================end==============================");
      //Debug.turnOff();  // DELETE ME -- FIXME
      return null;
    }
    Method best = null;
    Method nextBest = null;
    int charsInCommonWithPreferredPackage = 0;
    boolean tie = false;
    for ( Class<?> cls : classesForName ) {
      //Method m = getMethodForArgTypes( classForName, callName, argTypes );
      Method m = getMethodForArgTypes( cls, callName, argTypes, false );
      if ( m != null ) {
        int numCharsInCommon = lengthOfCommonPrefix(preferredPackage,cls.getCanonicalName());
        if ( best == null ) {
          best = m;
          charsInCommonWithPreferredPackage = numCharsInCommon;
        } else {
          if ( numCharsInCommon > charsInCommonWithPreferredPackage ) {
            best = m;
            charsInCommonWithPreferredPackage = numCharsInCommon;
          } else if ( charsInCommonWithPreferredPackage == numCharsInCommon ) {
            tie = true;
            nextBest = m;
          }
        }
      }
    }
    if ( tie ) {
      System.err.println( "Warning! Multiple candidate methods found by getMethodForArgTypes("
          + className + "." + callName
          + Utils.toString( argTypes, false )
          + "): (1) " + best + " (2) " + nextBest );
    }
    if ( Debug.isOn() ) Debug.errorOnNull( "getMethodForArgTypes(" + className + "." + callName
                 + Utils.toString( argTypes, false ) + "): Could not find method!", best );
    Debug.outln("===========================end==============================");
    //Debug.errln("===========================end==============================");
    //Debug.turnOff();  // DELETE ME -- FIXME
    return best;
  }

  public static Method oldGetMethodForArgTypes( String className,
                                                String preferredPackage,
                                                String callName,
                                                Class<?>[] argTypes,
                                                boolean complainIfNotFound ) {
    Class< ? > classForName = getClassForName( className, callName, preferredPackage, false );
    if ( classForName == null ) {
      if ( complainIfNotFound ) {
        System.err.println( "Couldn't find the class " + className + " for method "
                     + callName
                     + ( argTypes == null ? "" : Utils.toString( argTypes, false ) ) );
      }
      return null;
    }
    Method m = getMethodForArgTypes( classForName, callName, argTypes );
    if ( Debug.isOn() ) Debug.errorOnNull( "getMethodForArgTypes(" + className + "." + callName
                 + Utils.toString( argTypes, false ) + "): Could not find method!", m );
    return m;
  }

  public static Method getMethodForArgTypes( Class< ? > cls, String callName,
                                             Class<?>... argTypes ) {
    return getMethodForArgTypes( cls, callName, argTypes, true );
  }
  public static Method getMethodForArgTypes( Class< ? > cls, String callName,
                                             Class<?>[] argTypes, boolean complain ) {
      return getMethodForArgTypes( null, cls, callName, argTypes, complain );
      
  }
  public static Method getMethodForArgTypes( Object object, Class< ? > cls, String callName,
                                             Class<?>[] argTypes, boolean complain ) {
  //    return getMethodForArgTypes( cls, callName, argTypes, 10.0, 2.0, null );
  //  }
  //  public static Method getMethodForArgTypes( Class< ? > cls, String callName,
  //                                             Class<?>[] argTypes,
  //                                             double numArgsCost,
  //                                             double argMismatchCost,
  //                                             Map< Class< ? >, Map< Class< ? >, Double > > transformCost ) {
      if ( argTypes == null ) argTypes = new Class<?>[] {};
      String clsName = ( cls == null ? "null" : cls.getName() );
      if ( Debug.isOn() ) Debug.outln( "getMethodForArgTypes( cls=" + clsName
                                       + ", callName=" + callName
                                       + ", argTypes="
                                       + Utils.toString( argTypes ) + " )" );
  //    Method matchingMethod = null;
  //    boolean gotOkNumArgs = false;
  //    int mostMatchingArgs = 0;
  //    int mostDeps = 0;
  //    boolean allArgsMatched = false;
  //    double bestScore = Double.MAX_VALUE;
      boolean debugWasOn = Debug.isOn();
      //Debug.turnOff();
      if ( Debug.isOn() ) Debug.outln( "calling " + clsName + ".class.getMethod(" + callName + ")"  );
      if ( cls != null ) {
        try {
            Method method = cls.getMethod( callName, argTypes );
            if ( method != null ) return method;
        } catch ( NoSuchMethodException e1 ) {
        } catch ( SecurityException e1 ) {
        }
        try {
            Method method = cls.getDeclaredMethod( callName, argTypes );
            if ( method != null ) return method;
        } catch ( NoSuchMethodException e1 ) {
        } catch ( SecurityException e1 ) {
        }
      }
      Method[] methods = null;
      if ( Debug.isOn() ) Debug.outln( "calling getMethods() on class "
                                       + clsName );
      try {
        methods = cls == null ? null : cls.getMethods();
      } catch ( Exception e ) {
          if ( complain ) {
        Debug.error(true, false, "Got exception calling " + clsName
                            + ".getMethod(): " + e.getMessage() );
          }
      }
      if ( Debug.isOn() ) Debug.outln( "--> got methods: " + Utils.toString( methods ) );
      ArgTypeCompare atc = new ArgTypeCompare( object, cls, argTypes );
      if ( methods != null ) {
        for ( Method m : methods ) {
          if ( m.getName().equals( callName ) ) {
            atc.compare( m, m.getParameterTypes(), m.isVarArgs() );
          }
        }
      }
      if ( debugWasOn ) {
        Debug.turnOn();
      }
      if ( atc.best != null && !atc.allArgsMatched ) {
      if ( Debug.isOn() ) Debug.errln( "getMethodForArgTypes( cls="
                                       + clsName + ", callName="
                                       + callName + ", argTypes="
                                       + Utils.toString( argTypes )
                                       + " ): method returned (" + atc.best
                                       + ") only matches "
                                       + atc.mostMatchingArgs + " args: "
                                       + Utils.toString( argTypes ) );
      } else if ( atc.best == null && complain ) {
        Debug.error(true, false, "method " + callName + "(" + Utils.toString( argTypes ) + ")"
                            + " not found for " + clsName );
      }
      return (Method)atc.best;
    }


    /**
     * @param cls
     * @return all private, protected, public, static, and non-static methods
     *         declared in this class or any superclass.
     */
    public Set< Method > getAllMethods( Class< ? > cls ) {
        Set< Method > methods =
                new TreeSet< Method >( CompareUtils.GenericComparator.instance() );
        if ( cls == null ) return methods;
        methods.addAll( Arrays.asList( cls.getDeclaredMethods() ) );
        Class< ? > superCls = cls.getSuperclass();
        methods.addAll( getAllMethods( superCls ) );
        return methods;
    }

  public static String dominantType( String argType1, String argType2 ) {
	  if ( argType1 == null ) return argType2;
	  if ( argType2 == null ) return argType1;
	  if ( argType1.equals( "String" ) ) return argType1;
	  if ( argType2.equals( "String" ) ) return argType2;
	  if ( argType1.toLowerCase().equals( "double" ) ) return argType1;
	  if ( argType2.toLowerCase().equals( "double" ) ) return argType2;
      if ( argType1.toLowerCase().equals( "float" ) ) return argType1;
      if ( argType2.toLowerCase().equals( "float" ) ) return argType2;
	  if ( argType1.toLowerCase().startsWith( "long" ) ) return argType1;
	  if ( argType2.toLowerCase().startsWith( "long" ) ) return argType2;
	  if ( argType1.toLowerCase().startsWith( "int" ) ) return argType1;
	  if ( argType2.toLowerCase().startsWith( "int" ) ) return argType2;
	  return argType1;
	}

  public static Class<?> dominantTypeClass(Class<?> cls1, Class<?> cls2) {
	  if ( cls1 == null ) return cls2;
      if ( cls2 == null ) return cls1;
	  String name1 = cls1.getSimpleName();
	  String name2 = cls2.getSimpleName();

	  if (name1.equals(dominantType(name1, name2))) {
		  return cls1;
	  }
	  else {
		  return cls2;
	  }
  }

/**
   * Find and invoke the named method from the given object with the given
   * arguments.
   *
   * @param o
   * @param methodName
   * @param args
   * @param suppressErrors
   * @return in a Pair whether the invocation was successful and the return
   *         value (or null)
   */
  public static Pair< Boolean, Object > runMethod( boolean suppressErrors,
                                                   Object o, String methodName,
                                                   Object... args ) {
    Method m = getMethodForArgs( o.getClass(), methodName, args );
    return runMethod( suppressErrors, o, m, args );
  }

  /**
   * Invoke the method from the given object with the given arguments.
   *
   * @param o
   * @param methodName
   * @param args
   * @param suppressErrors
   * @return in a Pair whether the invocation was successful and the return
   *         value (or null)
   */
  public static Pair< Boolean, Object > runMethod( boolean suppressErrors,
                                                   Object o, Method method,
                                                   Object... args ) {
    Pair< Boolean, Object > p = new Pair< Boolean, Object >( false, null );
    List<Throwable> errors = new ArrayList< Throwable >();
    try {
      p = runMethod( o, method, args );
    } catch ( IllegalArgumentException e ) {
      if ( !suppressErrors ) {
        errors.add( e );
      }
    } catch ( IllegalAccessException e ) {
      if ( !suppressErrors ) {
        errors.add( e );
      }
    } catch ( InvocationTargetException e ) {
      if ( !suppressErrors ) {
        errors.add( e );
      }
    }
    if ( !p.first && isStatic( method ) && o != null ) {
      List< Object > l = Utils.newList( o );
      l.addAll( Arrays.asList( args ) );
      p = runMethod( true, null, method, l.toArray() );
      if ( !p.first && l.size() > 1 ) {
        p = runMethod( true, null, method, new Object[] { o } );
      }
    }
    if ( !suppressErrors && !p.first ) {
      Debug.error( false,
                   "runMethod( " + o + ", " + method + ", " +
                       Utils.toString( args, true ) + " ) failed!" );
    }
    for ( Throwable e : errors ) {
      e.printStackTrace();
    }
    return p;
  }

  public static boolean isStatic( Class<?> cls ) {
    if ( cls == null ) return false;
    return ( Modifier.isStatic( cls.getModifiers() ) );
  }

  public static boolean isStatic( Member member ) {
    if ( member == null ) return false;
    return ( Modifier.isStatic( member.getModifiers() ) );
  }

  public static boolean isFinal( Member member ) {
      if ( member == null ) return false;
      return ( Modifier.isFinal( member.getModifiers() ) );
  }

  /**
   * Find and invoke the named method from the given object with the given
   * arguments.
   *
   * @param o
   * @param methodName
   * @param args
   * @return in a Pair whether the invocation was successful and the return
   *         value (or null)
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  public static Pair< Boolean, Object >
      runMethod( Object o, String methodName,
                 Object... args ) throws IllegalArgumentException,
                                         IllegalAccessException,
                                         InvocationTargetException {
    Method m = getMethodForArgs( o.getClass(), methodName, args );
    return runMethod( o, m, args );
  }

  /**
   * Invoke the method from the given object with the given arguments.
   *
   * @param o
   * @param m
   * @param args
   * @return in a Pair whether the invocation was successful and the return
   *         value (or null)
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  public static Pair< Boolean, Object >
    runMethod( Object o, Method m,
               Object... args ) throws IllegalArgumentException,
                                       IllegalAccessException,
                                       InvocationTargetException {
    Pair< Boolean, Object > p = new Pair< Boolean, Object >( false, null );
    if ( m == null ) {
      return p;
    }
    p.second = m.invoke( o, args );
    p.first = true;
    return p;
  }

  /**
   * @param o
   * @param fieldName
   * @return
   */
  public static Object getFieldValue( Object o, String fieldName ) {
    return getFieldValue( o, fieldName, false, false );
  }

  protected static final String[] candidateMethodNames =
          new String[]{ "getMember", "getValueNoPropagate", "getValue",
                        "getField", "get" };
  
  /**
   * Get the value of the object's field with with the given fieldName using
   * reflection.
   *
   * @param o
   *          the object whose field value is sought
   * @param fieldName
   * @param suppressExceptions
   * @return the value of the field
   */
  public static Object getFieldValue( Object o, String fieldName,
                                      boolean suppressExceptions ) {
      return getFieldValue( o, fieldName, true, suppressExceptions );
  }
  
  public static Object getFieldValue( Object o, String fieldName,
                                      boolean tryMethods,
                                      boolean suppressExceptions ) {
    if ( o == null || Utils.isNullOrEmpty( fieldName ) ) {
      return null;
    }
    Exception ex = null;
    Field f = null;
    try {
      f = o.getClass().getField( fieldName );
      if ( !f.isAccessible() ) f.setAccessible( true );
      return f.get( o );
    } catch ( NoSuchFieldException e ) {
      ex = e;
    } catch ( SecurityException e ) {
      ex = e;
    } catch ( IllegalArgumentException e ) {
      ex = e;
    } catch ( IllegalAccessException e ) {
      ex = e;
    }
//  if ( f == null && o instanceof gov.nasa.jpl.ae.event.Parameter ) {
//  return getFieldValue( ( (gov.nasa.jpl.ae.event.Parameter)o ).getValueNoPropagate(),
//                        fieldName );
//}
    Object result = null;
    if ( tryMethods ) {
    for ( String mName : candidateMethodNames ) {
      Method m = getMethodForArgs( o.getClass(), mName, fieldName );
      if ( m == null ) m = getMethodForArgs( o.getClass(), mName );
      if ( m != null ) {
        try {
          boolean gotArgs = Utils.isNullOrEmpty( m.getParameterTypes() );
          Object[] args = gotArgs ? new Object[]{} : new Object[]{ fieldName };
          result = m.invoke( o, args );
          if ( !gotArgs && result != null ) return getFieldValue( result, fieldName, false, true );
          return result;
        } catch ( IllegalArgumentException e ) {
            // ex is already non-null, so no need to assign it here.
        } catch ( IllegalAccessException e ) {
        } catch ( InvocationTargetException e ) {
        }
      }
    }
    }
    if ( !suppressExceptions && result == null && ex != null ) {
      //System.out.println("$$$$$$$$$   WTF  $$$$$$$$$$");
      ex.printStackTrace();
    }
    return null;
  }

  /**
   * Return true if and only if the object has a field with the given name.
   * @param o
   * @param fieldName
   * @return
   */
  public static boolean hasField( Object o, String fieldName ) {
    return getField( o, fieldName, true ) != null;
  }

  /**
   * Get the Field of the Class of the object with the given fieldName using
   * reflection.
   *
   * @param o
   *          the object whose field is sought
   * @param fieldName
   * @param suppressExceptions
   *          if true, do not throw or write out any exceptions
   * @return the Field of the Class
   */
  public static Field getField( Object o, String fieldName, boolean suppressExceptions ) {
    if ( o == null || Utils.isNullOrEmpty( fieldName ) ) {
      if ( !suppressExceptions ) {
        Debug.error("Null input not allowed! getField(" + o + ", " + fieldName + ")");
      }
      return null;
    }
    Field f = null;
    Class< ? > cls = null;
    if ( o instanceof Class< ? > ) {
      cls = (Class< ? >)o;
      f = getField(cls, fieldName, true );
    }
    if ( f == null ) {
      cls = o.getClass();
      f = getField(cls, fieldName, true );
    }
    if ( f != null ) return f;
//  if ( f == null && o instanceof gov.nasa.jpl.ae.event.Parameter ) {
//  f = getField( ( (gov.nasa.jpl.ae.event.Parameter)o ).getValueNoPropagate(),
//                fieldName, suppressExceptions );
//}
    Object result = null;
    String[] candidateMethodNames = new String[]{ "getMember", "getValueNoPropagate", "getValue", "getField", "get" };
    for ( String mName : candidateMethodNames ) {
      Method m = getMethodForArgs( o.getClass(), mName, fieldName );
      if ( m == null ) m = getMethodForArgs( o.getClass(), mName );
      if ( m != null ) {
        try {
          boolean gotArgs = Utils.isNullOrEmpty( m.getParameterTypes() );
          Object[] args = gotArgs ? new Object[]{} : new Object[]{ fieldName };
          result = m.invoke( o, args );
          if ( result instanceof Field ) return (Field)result;
          if ( !gotArgs ) return getField( result, fieldName, suppressExceptions );
        } catch ( IllegalArgumentException e ) {
            // ex is already non-null, so no need to assign it here.
        } catch ( IllegalAccessException e ) {
        } catch ( InvocationTargetException e ) {
        }
      }
    }
    Debug.errorOnNull( !suppressExceptions, !suppressExceptions,
                       "Could not get field " + fieldName + " for " + o, f );
    return f;
  }

  /**
   * Get the Field of the Class with the given fieldName using reflection.
   *
   * @param cls
   *          the Class whose field is sought
   * @param fieldName
   * @param suppressExceptions
   *          if true, do not throw or write out any exceptions
   * @return the Field of the Class
   */
  public static Field getField( Class< ? > cls, String fieldName, boolean suppressExceptions ) {

    if ( cls == null || Utils.isNullOrEmpty( fieldName ) ) {
      if ( !suppressExceptions ) {
        Debug.error("Null input not allowed! getField(" + cls + ", " + fieldName + ")");
      }
      return null;
    }
    Exception ex = null;
    Field f = null;
    try {
      f = cls.getField( fieldName );
      return f;
    } catch ( NoSuchFieldException e ) {
      ex = e;
    } catch ( SecurityException e ) {
      ex = e;
    } catch ( IllegalArgumentException e ) {
      ex = e;
    }
    if ( !suppressExceptions && f == null && ex != null ) {
      ex.printStackTrace();
    }
    return null;
  }

  public static boolean hasMember( Class< ? > c, String memberName ) {
    boolean has = hasField( c, memberName );
    if ( !has ) has = hasMethod( c, memberName );
    return has;
  }
  
//  public static <T> Class<?> getEnumClass( Class<T> cls ) {
//      Class<T> enumClass = cls;
//      if ( enumClass.isEnum() ) return cls;
//      for ( TypeVariable< Class<T> > t : 
//            enumClass.getTypeParameters() ) {
//          t.
//      }
//
//  }
//  public static Object getEnumConstant( Class<?> enumClass, String constantName ) {
//      LinkedList< Class<?> > classQ = new LinkedList< Class<?> >();
//      classQ.add( enumClass );
//  }
  public static Object getEnumConstant( Class<?> enumClass, String constantName ) {
      if ( enumClass.isEnum() ) {
          Object[] constants = enumClass.getEnumConstants();
          if ( constants != null ) {
              for ( Object constant : constants ) {
    
                  if ( constant != null
                       && ( "" + constant ).equals( constantName ) ) {
                      return constant;
                  }
              }
          }
      }
      return null;
//      else if ( enumClass.getClasses().length > 0 ) {
//          for ( Class<?> c : enumClass.getDeclaredClasses() ) {
//              
//          }
//      }
  }
  
  protected static final String[] idStrings = new String[] { "id", "ID", "Id" };
  protected static final String[] idMethodStrings =
          new String[] { "getId", "getID", "id", "ID" };

  // REVIEW -- consider walking through the fields and methods and matching
  // lowercase on "get" and "id"; Do the same for getName()
  // REVIEW -- consider genericizing as getMemberValue()
  public static Object getId( Object o ) {
    if ( o instanceof HasId ) {
      return ((HasId<?>)o).getId();
    }
    try {
    	if (o instanceof HasId)
    	{
    		return ((HasId) o).getId();
    	}
        for ( String fieldName : new String[] { "id", "ID", "Id" } ) {
            Object oId = ClassUtils.getFieldValue( o, fieldName, false, true );
            if ( oId != null ) return oId;
        }
        for ( String methodName : idMethodStrings ) {
            Method m = ClassUtils.getMethodForArgs( o.getClass(),
                                                    methodName, false, (Object[])null );
            if ( m != null ) {
                Object oId = m.invoke( o, (Object[])null );
                if ( oId != null ) return oId;
            }
        }
    } catch ( Throwable t ) {
        // ignore
    }
    if ( o instanceof Wraps ) {
        return getId( ( (Wraps)o ).getValue( false ) );
    }
    return null;
  }

  protected static final String[] nameStrings =
          new String[] { "name", "NAME", "Name" };
  protected static final String[] nameMethodStrings = 
          new String[] { "getName", "get_name", "name", "NAME", "Name" };

  // REVIEW -- consider walking through the fields and methods and matching
  // lowercase on "get" and "name"; Do the same for getId().
  // REVIEW -- consider genericizing as getMemberValue()
  public static Object getName( Object o ) {
      if ( o instanceof HasName ) {
          return ( (HasName< ? >)o ).getName();
      }
      try {
          for ( String fieldName : nameStrings ) {
              Object oId = ClassUtils.getFieldValue( o, fieldName, false, true );
              if ( oId != null ) return oId;
          }
          for ( String methodName : nameMethodStrings ) {
              Method m = ClassUtils.getMethodForArgs( o.getClass(),
                                                      methodName, false, (Object[])null );
              if ( m != null ) {
                  Object oId = m.invoke( o, (Object[])null );
                  if ( oId != null ) return oId;
              }
          }
      } catch ( Throwable t ) {
          // ignore
      }
      if ( o instanceof Wraps ) {
          return getName( ((Wraps)o).getValue( false ) );
      }
      return null;
    }

    protected static final String[] typeStrings =
            new String[] { "type", "TYPE", "Type", "class", "CLASS", "Class" };
    protected static final String[] typeMethodStrings =
            new String[] { "getType", "get_type", "type", "TYPE", "Type",
                          "getClass", "get_class", "class", "CLASS", "Class" };
  
    // REVIEW -- consider walking through the fields and methods and matching
    // lowercase on "get" and "type"; Do the same for getId() and getName().
    // REVIEW -- consider genericizing as getMemberValue()
    public static Object getType( Object o ) {
        if ( o instanceof Wraps ) {
            Object type = ( (Wraps)o ).getType();
            if ( type != null ) return type;
            return getType( ( (Wraps)o ).getValue( false ) );
        }
        try {
            for ( String fieldName : typeStrings ) {
                Object oId = ClassUtils.getFieldValue( o, fieldName, false, true );
                if ( oId != null ) return oId;
            }
            for ( String methodName : typeMethodStrings ) {
                Method m =
                        ClassUtils.getMethodForArgs( o.getClass(), methodName,
                                                     false, (Object[])null );
                if ( m != null ) {
                    Object oId = m.invoke( o, (Object[])null );
                    if ( oId != null ) return oId;
                }
            }
        } catch ( Throwable t ) {
            // ignore
        }
        return null;
    }

    protected static final String[] valueStrings =
            new String[] { "value", "VALUE", "Value" };
    protected static final String[] valueMethodStrings =
            new String[] { "getValue", "get_value", "value", "VALUE", "Value",
                           "evaluate", "EVALUATE", "Evaluate" };

    // REVIEW -- consider walking through the fields and methods and matching
    // lowercase on "get" and "value"; Do the same for getId() and getName().
    // REVIEW -- consider genericizing as getMemberValue()
    public static Object getValue( Object o ) {
        if ( o instanceof Wraps ) {
           return ((Wraps)o).getValue( false );
        }
        try {
            for ( String fieldName : valueStrings ) {
                Object oId = ClassUtils.getFieldValue( o, fieldName, false, true );
                if ( oId != null ) return oId;
            }
            for ( String methodName : valueMethodStrings ) {
                Method m =
                        ClassUtils.getMethodForArgs( o.getClass(), methodName,
                                                     false, (Object[])null );
                if ( m != null ) {
                    Object oId = m.invoke( o, (Object[])null );
                    if ( oId != null ) return oId;
                }
            }
        } catch ( Throwable t ) {
            // ignore
        }
        return null;
    }

  public static String parameterPartOfName( String longName ) {
    return parameterPartOfName( longName, true );
  }
  public static String parameterPartOfName( String longName,
                                            boolean includeBrackets ) {
    int pos1 = longName.indexOf( '<' );
    if ( pos1 == -1 ) {
      return null;
    }
    int pos2 = longName.lastIndexOf( '>' );
    assert( pos2 >= 0 );
    if ( pos2 == -1 ) return null;
    if ( !includeBrackets ) {
      pos1 += 1;
      pos2 -=1;
    }
    String paramPart = longName.substring( pos1, pos2+1 ).trim();
    return paramPart;
  }

  public static String noParameterName( String longName ) {
    if ( longName == null ) return null;
    int pos = longName.indexOf( '<' );
    if ( pos == -1 ) {
      return longName;
    }
    String noParamName = longName.substring( 0, pos );
    return noParamName;
  }

  public static String toString( Class<?> type ) {
      if (type == null) return "";
      if ( type.isArray() ) {
        Class<?> compType = type.getComponentType();
        String compString = toString( compType );
        if ( Utils.isNullOrEmpty( compString ) ) {
            return "";
        }
        compString = Utils.replaceSuffix(compString, ".class", "" );
        return compString + "[]" + ".class";
      }
      StringBuffer sb = new StringBuffer();
      String typeName = type.getName();
      if ( typeName != null ) typeName = typeName.replace( '$', '.' );
      sb.append( ClassUtils.noParameterName( typeName )
                           + ".class" );
      return sb.toString();
  }

  /**
   * @param type
   * @return whether the input type is a number class or a number primitive.
   */
  public static boolean isNumber( Class< ? > type ) {
    if ( type == null ) return false;
    Class<?> forPrim = classForPrimitive( type );
    if ( forPrim != null ) type = forPrim;
    return ( Number.class.isAssignableFrom( type ) );
  }

  /**
   * @param type
   * @return whether the input type has an integer domain (int, short, long, etc).
   */
  public static boolean isInteger( Class< ? > type ) {
    if ( type == null ) return false;
    return ( Integer.class.isAssignableFrom( type ) );
//    Class<?> forPrim = classForPrimitive( type );
//    if ( forPrim != null ) type = forPrim;
  }

  /**
   * Convert a number of one type to a number of another type.
   *
   * @param n
   *            the number to convert
   * @param cls
   *            the type of number to which to convert
   * @return the converted number or null if the cast fails or an exception is
   *         caught
   */
  public static <N> N castNumber( Number n, Class<N> cls ) {
    try {
      Class<?> c = ClassUtils.classForPrimitive( cls );
      if ( c == null ) c = cls;
      if ( c == Long.class ) return (N)(Long)n.longValue();
      if ( c == Short.class ) return (N)(Short)n.shortValue();
      if ( c == Double.class ) return (N)(Double)n.doubleValue();
      if ( c == Integer.class ) return (N)(Integer)n.intValue();
      if ( c == Float.class ) return (N)(Float)n.floatValue();
//          if ( c == Character.class ) return (TT)(Character)n.shortValue();
//        if ( c == Long.class ) return cls.cast( n.longValue() );
    } catch ( Exception e ) {
      // ignore
    }
    return null;
  }

  /**
   * @param cls
   * @param methodName
   * @return all public methods of {@code cls} (or inherited by {@code cls})
   *         that have the simple name, {@code methodName}.
   */
  public static Method[] getMethodsForName( Class< ? > cls, String methodName ) {
    List< Method > methods = new ArrayList< Method >();
    for ( Method m : cls.getMethods() ) {
      if ( m.getName().equals(methodName) ) {
        methods.add( m );
      }
    }
    Method[] mArr = new Method[methods.size()];
    boolean succ = Utils.toArrayOfType( methods, mArr, Method.class );
    if ( !succ ) {
      Debug.error( "Error! Cast to Method[] failed for getMethodsForName(" +
                   cls + ", " + methodName + ")" );
    }
    return mArr;
  }

  /**
   * @param o
   * @param methodName
   * @return whether the named method can be called from the Object, o
   */
  public static boolean hasMethod( Object o, String methodName ) {
    if ( o instanceof Class && hasMethod( (Class< ? >)o, methodName ) ) return true;
    return hasMethod( o.getClass(), methodName );
  }

  /**
   * @param cls
   * @param methodName
   * @return whether the Class has a method with the given name
   */
  public static boolean hasMethod( Class< ? > cls, String methodName ) {
    return !Utils.isNullOrEmpty( getMethodsForName( cls, methodName ) );
  }

  /**
   * @param o
   * @param methodName
   * @param args
   * @return whether the named method can be called from the given Object with
   *         the given arguments
   */
  public static boolean hasMethod( Object o, String methodName, Object[] args ) {
    if ( o instanceof Class && hasMethod( (Class< ? >)o, methodName, args ) ) return true;
    return hasMethod( o.getClass(), methodName, args );
  }

  /**
   * @param cls
   * @param methodName
   * @param args
   * @return whether the Class has a method with the given name that can be called with the
   *         the given arguments
   */
  public static boolean hasMethod( Class< ? > cls, String methodName, Object[] args ) {
    return getMethodForArgs( cls, methodName, args ) != null;
  }

  /**
   * This function helps avoid compile warnings by just having the warning here.
   * @param tt
   * @return Collection.class cast as having a type parameter
   */
  public static <TT> Class< Collection<TT> > getCollectionClass( TT tt ) {
    return (Class< Collection<TT> >)Collection.class.asSubclass( Collection.class  );
//    Collection<TT> coll = new ArrayList< TT >();
//    Class< Collection<TT> > ccls = (Class< Collection<TT> >)coll.getClass();
//    return ccls;
  }

  /**
   * This function helps avoid compile warnings by just having the warning here.
   * @param ttcls
   * @return Collection.class cast as having a type parameter
   */
  public static <TT> Class< Collection<TT> > getCollectionClass( Class<TT> ttcls ) {
    return (Class< Collection<TT> >)Collection.class.asSubclass( Collection.class  );
//    Collection<TT> coll = new ArrayList< TT >();
//    //Class< Collection<TT> > ccls = (Class< Collection<TT> >)coll.getClass();
//    Class< Collection<TT> > ccls = (Class< Collection<TT> >)coll.getClass().asSubclass( Collection.class );
//    return ccls;
  }

  //  public static <TT> Collection<TT> makeCollection( TT tt, Class<TT> cls ) {
  //    ArrayList< TT > ttList = new ArrayList< TT >();
  //    ttList.add( tt );
  //    return ttList;
  //  }

  public static < TT > Collection< TT >
      makeCollection( TT tt, Class< ? extends TT > cls ) {
    // return Utils2.newList( tt );
    ArrayList< TT > ttList = new ArrayList< TT >();
    ttList.add( tt );
    return ttList;
  }

//  public static < TT > Collection< TT >
//      makeCollection( Parameter< TT > tt, Class< ? extends TT > cls ) {
//    // return Utils2.newList( tt.getValue() );
//    ArrayList< TT > ttList = new ArrayList< TT >();
//    ttList.add( tt.getValue() );
//    return ttList;
//  }

  public static class A {
    int a;
    public A( int a ) { this.a = a; }
    public int get() { return a; }
  }
  public static class B extends A {
    int b;
    public B( int a, int b ) { super(a); this.b = b; }
    @Override
    public int get() { return b; }
  }
  public static void main( String[] args ) {
    A a = new A(1);
    System.out.println("A.class = " + A.class );
    B b = new B(2,3);
    Collection<B> collB = new ArrayList<B>();
    ArrayList<B> arrListB = new ArrayList<B>();

    Class<?> cls1 = collB.getClass();
    System.out.println("collB.getClass() as Class<?> = " + cls1 );

    Class< ? extends Collection > clsb = collB.getClass();
    System.out.println("collB.getClass() as Class< ? extends Collection > = " + clsb );

    Class< ? extends ArrayList > clsa = arrListB.getClass();
    System.out.println("arrListB.getClass() as Class< ? extends ArrayList > = " + clsa );

    Class< ? extends Collection > clssb = arrListB.getClass().asSubclass( Collection.class );
    System.out.println("arrListB.getClass().asSubclass( Collection.class ) as Class< ? extends Collection > = " + clssb );

  }
  public static Field[] getAllFields( Class< ? extends Object > cls ) {
    List<Field> fieldList =  getListOfAllFields( cls );
    Field[] fieldArr = new Field[fieldList.size()];
    fieldList.toArray( fieldArr );
    return fieldArr;
  }
  public static List<Field> getListOfAllFields( Class< ? extends Object > cls ) {
    if ( cls == null ) return null;
    ArrayList<Field> fieldList = new ArrayList< Field >();
    for ( Field f : cls.getDeclaredFields() ) {
      f.setAccessible( true );
      fieldList.add( f );
    }
    List< Field > superFields = getListOfAllFields( cls.getSuperclass() );
    if ( superFields != null ) fieldList.addAll( superFields );
    return fieldList;
  }

  /**
   * Try to convert an object into one of the specified class.
   *
   * @param o
   *          the object to convert into type cls
   * @param cls
   *          the {@link Class} of the object to return
   * @param propagate
   * @return an object of the type specified or null if the conversion was
   *         unsuccessful.
   */
  public static <T> Pair<Boolean, T> coerce( Object o, Class<T> cls, boolean propagate ) {
    // REVIEW -- How is Wraps involved in wrapping in Expression? Can evaluate()
    // be moved out of Expression into a Wraps factory helper, or part into
    // ClassUtils and the other to Wraps?
    // TODO -- REVIEW -- Can null be a valid return value? Should
    // Expression.evaluate() also return a pair?
    if ( o == null ) return new Pair< Boolean, T >( false, null );
    Object v = //Expression.
        evaluate( o, cls, propagate );
    Boolean succ = null;
    T t = null;
    if ( v != null ) {
      succ = true;
      try {
        if ( cls == null ) {
          t = (T)v;
        } else {
          t = cls.cast( v );
        }
      } catch ( ClassCastException e ) {
        succ = false;
      }
      if ( t == null ) succ = false;
    }
    return new Pair< Boolean, T >( succ, t );
  }

  public static < T > Pair< Boolean, List< T > > coerceList( Object object, Class<T> cls, boolean propagate ) {
      Pair< Boolean, List< T > > resultPair = new Pair< Boolean, List< T > >( false, null );
      if ( object == null ) {
          resultPair.first = true;
          return resultPair;
      }
      Pair< Boolean, T > p = coerce( object, cls, propagate );
      List< T > result = null;
      if ( p.first ) {
          resultPair.first = true;
          result = Utils.newList( p.second );
      } else if ( object instanceof Collection ) {
          Collection<?> coll = (Collection<?>)object;
          for ( Object oo : coll ) {
              Pair<Boolean, List<T> > oop = coerceList( oo, cls, propagate );
              if ( oop.first ) {
                  List<T> ooResult = oop.second;
                  if ( !Utils.isNullOrEmpty( ooResult ) ) {
                      if ( Utils.isNullOrEmpty( result ) ) {
                          result = ooResult;
                      } else {
                          result.addAll( ooResult );
                      }
                  }
                  resultPair.first = true;
              }

          }
      }
      resultPair.second = result;
      return resultPair;
  }

  /**
   * Evaluate/dig or wrap the object of the given type cls from the object o,
   * which may be a Parameter or an Expression.
   *
   * @param object
   *          the object to evaluate
   * @param cls
   *          the type of the object to find
   * @return o if o is of type cls, an object of type cls that is an evaluation
   *         of o, or null otherwise.
   */
  public static <TT> TT evaluate( Object object, Class< TT > cls,
                                  boolean propagate ) throws ClassCastException {
    if ( object == null ) return null;
    // Check if object is already what we want.
    if ( cls != null && cls.isInstance( object ) || cls.equals( object.getClass() ) ) {
      return (TT)object;
    }

    // Try to evaluate object or dig inside to get the object of the right type.
    Object value = null;

    if ( object instanceof Wraps ) {
        Object wrappedObj = ( (Wraps)object ).getValue( propagate );
        try {
            value = evaluate( wrappedObj, cls, propagate );
            if ( value != null ) return (TT)value;
        } catch ( Throwable e ) {
            // ignore
        }
    }
    
    if ( object instanceof Collection ) {
        Collection<?> coll = (Collection<?>)object;
        if ( coll.size() == 1 ) {
            value = coll.iterator().next();
            if ( value != null ) {
                value = evaluate( value, cls, propagate );
                if ( value != null ) return (TT)value;
            }
        }
    }
    //    if ( object instanceof Parameter ) {
//      value = ( (Parameter)object ).getValue( propagate );
//      return evaluate( value, cls, propagate, allowWrapping );
//    }
//    else if ( object instanceof Expression ) {
//      Expression< ? > expr = (Expression<?>)object;
//      if ( cls != null && cls.isInstance( expr.expression ) &&
//           expr.form != Form.Function) {
//        return (TT)expr.expression;
//      }
//      value = expr.evaluate( propagate );
//      return evaluate( value, cls, propagate, allowWrapping );
//    }
//    else if ( object instanceof Call) {
//      value = ( (Call)object ).evaluate( propagate );
//      return evaluate( value, cls, propagate, allowWrapping );
//    } else
    if ( cls != null && ClassUtils.isNumber( cls ) ) {
        if ( ClassUtils.isNumber( object.getClass() ) ) {
            try {
                Number n = (Number)object;
                TT tt = castNumber( n, cls );
                if ( tt != null || object == null ) {
                    return tt;
                }
            } catch ( Exception e ) {
                // warning?  we shouldn't get here, right?
            }
        } else {
            // try to make the string a number
            try {
                String s = evaluate( object, String.class, propagate );
                Double d = new Double( s );
                if ( d != null ) {
                    return evaluate( d, cls, propagate );
                }
            } catch (Throwable t) {}
        }
    }

    // if
    if ( cls.equals( String.class ) ) {
        @SuppressWarnings( "unchecked" )
        TT r = (TT)object.toString();
        return r;
    }
//    else if ( allowWrapping && cls != null ){
//      // If evaluating doesn't work, maybe we need to wrap the value in a parameter.
//      if ( cls.isAssignableFrom( Parameter.class ) ) {
//        if ( Debug.isOn() ) Debug.error( false, "Warning: wrapping value with a parameter with null owner!" );
//        return (TT)( new Parameter( null, null, object, null ) );
//      } else if ( cls.isAssignableFrom( Expression.class ) ) {
//        return (TT)( new Expression( object ) );
//      }
//    }
    TT r = null;
    try {
      r = (TT)object;
    } catch ( ClassCastException cce ) {
      Debug.errln( "Warning! No evaluation of " + object + " with type " + cls.getName() + "!" );
      throw cce;
    }
    if ( cls != null && cls.isInstance( r ) || ( r != null && cls == r.getClass() ) ) {
      return r;
    }
    return null;
  }

  /**
   * Determine whether the values of two objects are equal by evaluating them.
   * @param o1
   * @param o2
   * @return whether the evaluations of o1 and o2 are equal.
   * @throws ClassCastException
   */
  public static boolean valuesEqual( Object o1, Object o2 ) throws ClassCastException {
    return valuesEqual( o1, o2, null, false, false );
  }
  public static boolean valuesEqual( Object o1, Object o2, Class<?> cls ) throws ClassCastException {
    return valuesEqual( o1, o2, cls, false, false );
  }
  public static boolean valuesEqual( Object o1, Object o2, Class<?> cls,
                                     boolean propagate,
                                     boolean allowWrapping ) throws ClassCastException {
    if ( o1 == o2 ) return true;
    if ( o1 == null || o2 == null ) return false;
    if ( (o1 instanceof Float && o2 instanceof Double ) || (o2 instanceof Float && o1 instanceof Double ) ) {
      Debug.out( "" );
    }
    Object v1 = evaluate( o1, cls, propagate );//, false );
    Object v2 = evaluate( o2, cls, propagate );//, false );
    if ( Utils.valuesEqual( v1, v2 ) ) return true;
    Class< ? > cls1 = null;
    if ( v1 != null ) {
      cls1 = v1.getClass();
    }
    if ( v1 != o1 || v2 != o2 ) {
      if ( cls1 != null && cls1 != cls && valuesEqual( v2, v1, cls1 ) ) return true;
    }
    if ( v2 != null ) {
      if ( v1 != o2 || v2 != o1 ) {
        Class< ? > cls2 = v2.getClass();
        if ( cls2 != cls && cls2 != cls1 && valuesEqual( v1, v2, cls2 ) ) return true;
      }
    }
    return false;
    /*
    Class< ? > cls1 =
        ( cls != null ) ? cls : ( ( v1 == null ) ? null : v1.getClass() );
    Class< ? > cls2 =
        ( cls != null ) ? cls : ( ( v2 == null ) ? null : v2.getClass() );
    v1 = evaluate( v1, cls1, propagate, allowWrapping );
    v2 = evaluate( v2, cls1, propagate, allowWrapping );
    if ( Utils.valuesEqual( v1, v2 ) ) return true;
    Class< ? > cls1 =
        ( cls != null ) ? cls : ( ( v1 == null ) ? null : v1.getClass() );
    Class< ? > cls2 =
        ( cls != null ) ? cls : ( ( v2 == null ) ? null : v2.getClass() );
    Object v1 = evaluate( o1, cls1, propagate, allowWrapping );
    Object v2 = evaluate( o2, cls1, propagate, allowWrapping );
    if ( Utils.valuesEqual( v1, v2 ) ) return true;
    if ( cls1 != cls2 ) {
      v1 = evaluate( o1, cls2, propagate, allowWrapping );
      v2 = evaluate( o2, cls2, propagate, allowWrapping );
    }
    return Utils.valuesEqual( v1, v2 );
    */
  }

  /**
   * @param o
   * @return a collection of o's Class, superclasses, and interfaces
   */
  public static List< Class< ? > > getAllClasses( Object o ) {
    Class< ? > cls = (Class< ? >)(o instanceof Class ? o : o.getClass() );
    HashSet< Class< ? > > set = new HashSet< Class< ? > >();
    List< Class< ? > > classes = new ArrayList< Class< ? > >();
    List<Class<?>> queue = new ArrayList< Class<?> >();
    queue.add( cls );
    while ( !queue.isEmpty() ) { // REVIEW -- could probably use iterator and use classes as the queue
      Class< ? > c = queue.get( 0 );
      queue.remove( 0 );
      if ( set.contains( c ) ) continue;
      Class< ? > parent = cls.getSuperclass();
      if ( parent != null && !set.contains( parent ) ) queue.add( parent );
      queue.addAll( Arrays.asList( cls.getInterfaces() ) );
      classes.add( 0, c );
      set.add( c );
    }
    return classes;
  }
}
