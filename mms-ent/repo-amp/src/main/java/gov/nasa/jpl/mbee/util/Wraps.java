package gov.nasa.jpl.mbee.util;

/**
 * An instance of {@code Wraps<V>} wraps or contains another type, {@code V}, or
 * object of type, {@code V}, similar to how a variable wraps a value.
 * 
 * @param <V>
 *          the type of the wrapped object
 */
public interface Wraps< V > {
  /**
   * @return the type of the object that this object wraps
   */
  public Class< ? > getType();
  
  /**
   * @param className
   *          the name of {@code this} class (which should be the class
   *          redefining this method or a subclass) with generic parameters
   * @return the name of the type for the object that would be wrapped by an
   *         object with a class name of {@code className}; this should be the
   *         type of the return value for {@link Wraps<V>.getValue(boolean)}.
   */
  public String getTypeNameForClassName( String className );

  /**
   * @return the primitive class corresponding to the object wrapped by this
   *         object (possibly in several layers)
   */
  public Class< ? > getPrimitiveType();
  
  /**
   * @param propagate
   *          whether or not to propagate dependencies in order to determine
   *          what object is wrapped
   * @return the object that is wrapped by this object
   */
  public V getValue( boolean propagate );

  /**
   * Set the value of the object that is wrapped by this object.
   * 
   * @param value
   *          the new value to be wrapped
   */
  public void setValue( V value );
  
}
