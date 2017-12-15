package gov.nasa.jpl.view_repo.test;

import org.junit.Test;

public class TestLoadClass {

    public static Class<?> tryLoadClass( ClassLoader cl, String clsName ) {
        Class<?> cls = null;
        try {
            cls = cl.loadClass( clsName );
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
        }
        if ( cls == null ) {
            System.out.println( "ClassLoader " + cl + " did not find class: "
                                + clsName );
        } else {
            System.out.println( "ClassLoader " + cl + " found class: "
                                + cls.getName() );
        }
        return cls;
    }

    @Test
    public void test() {
        String clsName = TestLoadClass.class.getName();
        ClassLoader cl = TestLoadClass.class.getClassLoader();
        Class<?> cls = tryLoadClass( cl, clsName );
        cl = Thread.currentThread().getContextClassLoader();
        cls = tryLoadClass( cl, clsName );
        cl = ClassLoader.getSystemClassLoader();
        cls = tryLoadClass( cl, clsName );
    }

    public static void main( String[] args ) {
        TestLoadClass t = new TestLoadClass();
        t.test();
    }
}
