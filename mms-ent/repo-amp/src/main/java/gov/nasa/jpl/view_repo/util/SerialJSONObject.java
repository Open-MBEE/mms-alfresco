package gov.nasa.jpl.view_repo.util;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

public class SerialJSONObject extends JSONObject implements Serializable {
    static Logger logger = Logger.getLogger(SerialJSONObject.class);

    private static final long serialVersionUID = 5568685394137027249L;

    private Map map;
    public static final Object NULL = new SerialJSONObject.Null();

    public SerialJSONObject() {
        this.map = new HashMap();
    }

    public SerialJSONObject(JSONObject var1, String[] var2) throws JSONException {
        this();

        for(int var3 = 0; var3 < var2.length; ++var3) {
            this.putOnce(var2[var3], var1.opt(var2[var3]));
        }

    }

    public SerialJSONObject(SerialJSONTokener var1) throws JSONException {
        this();
        if (var1.nextClean() != '{') {
            throw var1.syntaxError("A SerialJSONObject text must begin with '{'");
        } else {
            while(true) {
                char var2 = var1.nextClean();
                switch(var2) {
                    case '\u0000':
                        throw var1.syntaxError("A SerialJSONObject text must end with '}'");
                    case '}':
                        return;
                    default:
                        var1.back();
                        String var3 = var1.nextValue().toString();
                        var2 = var1.nextClean();
                        if (var2 == '=') {
                            if (var1.next() != '>') {
                                var1.back();
                            }
                        } else if (var2 != ':') {
                            throw var1.syntaxError("Expected a ':' after a key");
                        }

                        this.putOnce(var3, var1.nextValue());
                        switch(var1.nextClean()) {
                            case ',':
                            case ';':
                                if (var1.nextClean() == '}') {
                                    return;
                                }

                                var1.back();
                                break;
                            case '}':
                                return;
                            default:
                                throw var1.syntaxError("Expected a ',' or '}'");
                        }
                }
            }
        }
    }

    public SerialJSONObject(Map var1) {
        this.map = (Map)(var1 == null ? new HashMap() : var1);
    }

    public SerialJSONObject(Map var1, boolean var2) {
        this.map = new HashMap();
        if (var1 != null) {
            Iterator var3 = var1.entrySet().iterator();

            while(var3.hasNext()) {
                Map.Entry var4 = (Map.Entry)var3.next();
                this.map.put(var4.getKey(), new SerialJSONObject(var4.getValue(), var2));
            }
        }

    }

    public SerialJSONObject(Object var1) {
        this();
        this.populateInternalMap(var1, false);
    }

    public SerialJSONObject(Object var1, boolean var2) {
        this();
        this.populateInternalMap(var1, var2);
    }

    private void populateInternalMap(Object var1, boolean var2) {
        Class var3 = var1.getClass();
        if (var3.getClassLoader() == null) {
            var2 = false;
        }

        Method[] var4 = var2 ? var3.getMethods() : var3.getDeclaredMethods();

        for(int var5 = 0; var5 < var4.length; ++var5) {
            try {
                Method var6 = var4[var5];
                String var7 = var6.getName();
                String var8 = "";
                if (var7.startsWith("get")) {
                    var8 = var7.substring(3);
                } else if (var7.startsWith("is")) {
                    var8 = var7.substring(2);
                }

                if (var8.length() > 0 && Character.isUpperCase(var8.charAt(0)) && var6.getParameterTypes().length == 0) {
                    if (var8.length() == 1) {
                        var8 = var8.toLowerCase();
                    } else if (!Character.isUpperCase(var8.charAt(1))) {
                        var8 = var8.substring(0, 1).toLowerCase() + var8.substring(1);
                    }

                    Object var9 = var6.invoke(var1, (Object[])null);
                    if (var9 == null) {
                        this.map.put(var8, NULL);
                    } else if (var9.getClass().isArray()) {
                        this.map.put(var8, new SerialJSONArray(var9, var2));
                    } else if (var9 instanceof Collection) {
                        this.map.put(var8, new SerialJSONArray((Collection)var9, var2));
                    } else if (var9 instanceof Map) {
                        this.map.put(var8, new SerialJSONObject((Map)var9, var2));
                    } else if (this.isStandardProperty(var9.getClass())) {
                        this.map.put(var8, var9);
                    } else if (!var9.getClass().getPackage().getName().startsWith("java") && var9.getClass().getClassLoader() != null) {
                        this.map.put(var8, new SerialJSONObject(var9, var2));
                    } else {
                        this.map.put(var8, var9.toString());
                    }
                }
            } catch (Exception var10) {
                throw new RuntimeException(var10);
            }
        }

    }

    private boolean isStandardProperty(Class var1) {
        return var1.isPrimitive() || var1.isAssignableFrom(Byte.class) || var1.isAssignableFrom(Short.class) || var1.isAssignableFrom(Integer.class) || var1.isAssignableFrom(Long.class) || var1.isAssignableFrom(Float.class) || var1.isAssignableFrom(Double.class) || var1.isAssignableFrom(Character.class) || var1.isAssignableFrom(String.class) || var1.isAssignableFrom(Boolean.class);
    }

    public SerialJSONObject(Object var1, String[] var2) {
        this();
        Class var3 = var1.getClass();

        for(int var4 = 0; var4 < var2.length; ++var4) {
            String var5 = var2[var4];

            try {
                this.putOpt(var5, var3.getField(var5).get(var1));
            } catch (Exception var7) {
                ;
            }
        }

    }

    public SerialJSONObject(String var1) throws JSONException {
        this(new SerialJSONTokener(var1));
    }

    @Override
    public SerialJSONObject accumulate(String var1, Object var2) throws JSONException {
        testValidity(var2);
        Object var3 = this.opt(var1);
        if (var3 == null) {
            this.put(var1, var2 instanceof SerialJSONArray ? (new SerialJSONArray()).put(var2) : var2);
        } else if (var3 instanceof SerialJSONArray) {
            ((SerialJSONArray)var3).put(var2);
        } else {
            this.put(var1, (Object)(new SerialJSONArray()).put(var3).put(var2));
        }

        return this;
    }

    @Override
    public SerialJSONObject append(String var1, Object var2) throws JSONException {
        testValidity(var2);
        Object var3 = this.opt(var1);
        if (var3 == null) {
            this.put(var1, (Object)(new SerialJSONArray()).put(var2));
        } else {
            if (!(var3 instanceof SerialJSONArray)) {
                throw new JSONException("JSONObject[" + var1 + "] is not a SerialJSONArray.");
            }

            this.put(var1, (Object)((SerialJSONArray)var3).put(var2));
        }

        return this;
    }

    public static String doubleToString(double var0) {
        if (!Double.isInfinite(var0) && !Double.isNaN(var0)) {
            String var2 = Double.toString(var0);
            if (var2.indexOf(46) > 0 && var2.indexOf(101) < 0 && var2.indexOf(69) < 0) {
                while(var2.endsWith("0")) {
                    var2 = var2.substring(0, var2.length() - 1);
                }

                if (var2.endsWith(".")) {
                    var2 = var2.substring(0, var2.length() - 1);
                }
            }

            return var2;
        } else {
            return "null";
        }
    }

    @Override
    public Object get(String var1) {
        Object var2 = this.opt(var1);
        if (var2 == null) {
            try {
                throw new JSONException("JSONObject[" + quote(var1) + "] not found.");
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e);
                }
            }
        }
        return var2;
    }

    @Override
    public boolean getBoolean(String var1) {
        Object var2 = this.get(var1);
        if (!var2.equals(Boolean.FALSE) && (!(var2 instanceof String) || !((String)var2).equalsIgnoreCase("false"))) {
            if (!var2.equals(Boolean.TRUE) && (!(var2 instanceof String) || !((String)var2).equalsIgnoreCase("true"))) {
                try {
                    throw new JSONException("JSONObject[" + quote(var1) + "] is not a Boolean.");
                } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e);
                }
            }
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public double getDouble(String var1) throws JSONException {
        Object var2 = this.get(var1);

        try {
            return var2 instanceof Number ? ((Number)var2).doubleValue() : Double.valueOf((String)var2);
        } catch (Exception var4) {
            throw new JSONException("JSONObject[" + quote(var1) + "] is not a number.");
        }
    }

    @Override
    public int getInt(String var1) throws JSONException {
        Object var2 = this.get(var1);
        return var2 instanceof Number ? ((Number)var2).intValue() : (int)this.getDouble(var1);
    }

    @Override
    public SerialJSONArray getJSONArray(String var1) {
        Object var2 = this.get(var1);
        if (var2 instanceof SerialJSONArray) {
            return (SerialJSONArray)var2;
        } else {
            try {
                throw new JSONException("JSONObject[" + quote(var1) + "] is not a SerialJSONArray.");
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e);
                }
            }
        }
        return new SerialJSONArray();
    }

    @Override
    public SerialJSONObject getJSONObject(String var1) {
        Object var2 = this.get(var1);
        if (var2 instanceof SerialJSONObject) {
            return (SerialJSONObject)var2;
        } else {
            try {
                throw new JSONException("JSONObject[" + quote(var1) + "] is not a SerialJSONObject.");
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e);
                }
            }
        }
        return new SerialJSONObject();
    }

    @Override
    public long getLong(String var1) throws JSONException {
        Object var2 = this.get(var1);
        return var2 instanceof Number ? ((Number)var2).longValue() : (long)this.getDouble(var1);
    }

    public static String[] getNames(JSONObject var0) {
        int var1 = var0.length();
        if (var1 == 0) {
            return null;
        } else {
            Iterator var2 = var0.keys();
            String[] var3 = new String[var1];

            for(int var4 = 0; var2.hasNext(); ++var4) {
                var3[var4] = (String)var2.next();
            }

            return var3;
        }
    }

    public static String[] getNames(Object var0) {
        if (var0 == null) {
            return null;
        } else {
            Class var1 = var0.getClass();
            Field[] var2 = var1.getFields();
            int var3 = var2.length;
            if (var3 == 0) {
                return null;
            } else {
                String[] var4 = new String[var3];

                for(int var5 = 0; var5 < var3; ++var5) {
                    var4[var5] = var2[var5].getName();
                }

                return var4;
            }
        }
    }

    @Override
    public String getString(String var1) {
        return this.get(var1).toString();
    }

    @Override
    public boolean has(String var1) {
        return this.map.containsKey(var1);
    }

    @Override
    public boolean isNull(String var1) {
        return NULL.equals(this.opt(var1));
    }

    @Override
    public Iterator keys() {
        return this.map.keySet().iterator();
    }

    @Override
    public int length() {
        return this.map.size();
    }

    @Override
    public SerialJSONArray names() {
        SerialJSONArray var1 = new SerialJSONArray();
        Iterator var2 = this.keys();

        while(var2.hasNext()) {
            var1.put(var2.next());
        }

        return var1.length() == 0 ? null : var1;
    }

    public static String numberToString(Number var0) throws JSONException {
        if (var0 == null) {
            throw new JSONException("Null pointer");
        } else {
            testValidity(var0);
            String var1 = var0.toString();
            if (var1.indexOf(46) > 0 && var1.indexOf(101) < 0 && var1.indexOf(69) < 0) {
                while(var1.endsWith("0")) {
                    var1 = var1.substring(0, var1.length() - 1);
                }

                if (var1.endsWith(".")) {
                    var1 = var1.substring(0, var1.length() - 1);
                }
            }

            return var1;
        }
    }

    @Override
    public Object opt(String var1) {
        return var1 == null ? null : this.map.get(var1);
    }

    @Override
    public boolean optBoolean(String var1) {
        return this.optBoolean(var1, false);
    }

    @Override
    public boolean optBoolean(String var1, boolean var2) {
        try {
            return this.getBoolean(var1);
        } catch (Exception var4) {
            return var2;
        }
    }

    @Override
    public SerialJSONObject put(String var1, Collection var2) throws JSONException {
        this.put(var1, (Object)(new SerialJSONArray(var2)));
        return this;
    }

    @Override
    public double optDouble(String var1) {
        return this.optDouble(var1, 0.0D / 0.0);
    }

    @Override
    public double optDouble(String var1, double var2) {
        try {
            Object var4 = this.opt(var1);
            return var4 instanceof Number ? ((Number)var4).doubleValue() : new Double((String)var4);
        } catch (Exception var5) {
            return var2;
        }
    }

    @Override
    public int optInt(String var1) {
        return this.optInt(var1, 0);
    }

    @Override
    public int optInt(String var1, int var2) {
        try {
            return this.getInt(var1);
        } catch (Exception var4) {
            return var2;
        }
    }

    @Override
    public SerialJSONArray optJSONArray(String var1) {
        Object var2 = this.opt(var1);
        return var2 instanceof SerialJSONArray ? (SerialJSONArray)var2 : null;
    }

    @Override
    public SerialJSONObject optJSONObject(String var1) {
        Object var2 = this.opt(var1);
        return var2 instanceof SerialJSONObject ? (SerialJSONObject)var2 : null;
    }

    @Override
    public long optLong(String var1) {
        return this.optLong(var1, 0L);
    }

    @Override
    public long optLong(String var1, long var2) {
        try {
            return this.getLong(var1);
        } catch (Exception var5) {
            return var2;
        }
    }

    @Override
    public String optString(String var1) {
        return this.optString(var1, "");
    }

    @Override
    public String optString(String var1, String var2) {
        Object var3 = this.opt(var1);
        return var3 != null ? var3.toString() : var2;
    }

    @Override
    public SerialJSONObject put(String var1, boolean var2) throws JSONException {
        this.put(var1, (Object)(var2 ? Boolean.TRUE : Boolean.FALSE));
        return this;
    }

    @Override
    public SerialJSONObject put(String var1, double var2) throws JSONException {
        this.put(var1, (Object)(new Double(var2)));
        return this;
    }

    @Override
    public SerialJSONObject put(String var1, int var2) throws JSONException {
        this.put(var1, (Object)(new Integer(var2)));
        return this;
    }

    @Override
    public SerialJSONObject put(String var1, long var2) throws JSONException {
        this.put(var1, (Object)(new Long(var2)));
        return this;
    }

    @Override
    public SerialJSONObject put(String var1, Map var2) throws JSONException {
        this.put(var1, (Object)(new SerialJSONObject(var2)));
        return this;
    }

    @Override
    public SerialJSONObject put(String var1, Object var2) throws JSONException {
        if (var1 == null) {
            throw new JSONException("Null key.");
        } else {
            if (var2 != null) {
                testValidity(var2);
                this.map.put(var1, var2);
            } else {
                this.remove(var1);
            }

            return this;
        }
    }

    @Override
    public SerialJSONObject putOnce(String var1, Object var2) throws JSONException {
        if (var1 != null && var2 != null) {
            if (this.opt(var1) != null) {
                throw new JSONException("Duplicate key \"" + var1 + "\"");
            }

            this.put(var1, var2);
        }

        return this;
    }

    @Override
    public SerialJSONObject putOpt(String var1, Object var2) throws JSONException {
        if (var1 != null && var2 != null) {
            this.put(var1, var2);
        }

        return this;
    }

    public static String quote(String var0) {
        if (var0 != null && var0.length() != 0) {
            char var2 = 0;
            int var4 = var0.length();
            StringBuffer var5 = new StringBuffer(var4 + 4);
            var5.append('"');

            for(int var3 = 0; var3 < var4; ++var3) {
                char var1 = var2;
                var2 = var0.charAt(var3);
                switch(var2) {
                    case '\b':
                        var5.append("\\b");
                        continue;
                    case '\t':
                        var5.append("\\t");
                        continue;
                    case '\n':
                        var5.append("\\n");
                        continue;
                    case '\f':
                        var5.append("\\f");
                        continue;
                    case '\r':
                        var5.append("\\r");
                        continue;
                    case '"':
                    case '\\':
                        var5.append('\\');
                        var5.append(var2);
                        continue;
                    case '/':
                        if (var1 == '<') {
                            var5.append('\\');
                        }

                        var5.append(var2);
                        continue;
                }

                if (var2 >= ' ' && (var2 < 128 || var2 >= 160) && (var2 < 8192 || var2 >= 8448)) {
                    var5.append(var2);
                } else {
                    String var6 = "000" + Integer.toHexString(var2);
                    var5.append("\\u" + var6.substring(var6.length() - 4));
                }
            }

            var5.append('"');
            return var5.toString();
        } else {
            return "\"\"";
        }
    }

    @Override
    public Object remove(String var1) {
        return this.map.remove(var1);
    }

    @Override
    public Iterator sortedKeys() {
        return (new TreeSet(this.map.keySet())).iterator();
    }

    public static Object stringToValue(String var0) {
        if (var0.equals("")) {
            return var0;
        } else if (var0.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        } else if (var0.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        } else if (var0.equalsIgnoreCase("null")) {
            return NULL;
        } else {
            char var1 = var0.charAt(0);
            if (var1 >= '0' && var1 <= '9' || var1 == '.' || var1 == '-' || var1 == '+') {
                if (var1 == '0') {
                    if (var0.length() > 2 && (var0.charAt(1) == 'x' || var0.charAt(1) == 'X')) {
                        try {
                            return new Integer(Integer.parseInt(var0.substring(2), 16));
                        } catch (Exception var9) {
                            ;
                        }
                    } else {
                        try {
                            return new Integer(Integer.parseInt(var0, 8));
                        } catch (Exception var8) {
                            ;
                        }
                    }
                }

                try {
                    return new Integer(var0);
                } catch (Exception var7) {
                    try {
                        return new Long(var0);
                    } catch (Exception var6) {
                        try {
                            return new Double(var0);
                        } catch (Exception var5) {
                            ;
                        }
                    }
                }
            }

            return var0;
        }
    }

    static void testValidity(Object var0) throws JSONException {
        if (var0 != null) {
            if (var0 instanceof Double) {
                if (((Double)var0).isInfinite() || ((Double)var0).isNaN()) {
                    throw new JSONException("JSON does not allow non-finite numbers.");
                }
            } else if (var0 instanceof Float && (((Float)var0).isInfinite() || ((Float)var0).isNaN())) {
                throw new JSONException("JSON does not allow non-finite numbers.");
            }
        }

    }

    @Override
    public SerialJSONArray toJSONArray(JSONArray var1) throws JSONException {
        if (var1 != null && var1.length() != 0) {
            SerialJSONArray var2 = new SerialJSONArray();

            for(int var3 = 0; var3 < var1.length(); ++var3) {
                var2.put(this.opt(var1.getString(var3)));
            }

            return var2;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        try {
            Iterator var1 = this.keys();
            StringBuffer var2 = new StringBuffer("{");

            while(var1.hasNext()) {
                if (var2.length() > 1) {
                    var2.append(',');
                }

                Object var3 = var1.next();
                var2.append(quote(var3.toString()));
                var2.append(':');
                var2.append(valueToString(this.map.get(var3)));
            }

            var2.append('}');
            return var2.toString();
        } catch (Exception var4) {
            return null;
        }
    }

    @Override
    public String toString(int var1) throws JSONException {
        return this.toString(var1, 0);
    }

    String toString(int var1, int var2) throws JSONException {
        int var4 = this.length();
        if (var4 == 0) {
            return "{}";
        } else {
            Iterator var5 = this.sortedKeys();
            StringBuffer var6 = new StringBuffer("{");
            int var7 = var2 + var1;
            Object var8;
            if (var4 == 1) {
                var8 = var5.next();
                var6.append(quote(var8.toString()));
                var6.append(": ");
                var6.append(valueToString(this.map.get(var8), var1, var2));
            } else {
                while(true) {
                    int var3;
                    if (!var5.hasNext()) {
                        if (var6.length() > 1) {
                            var6.append('\n');

                            for(var3 = 0; var3 < var2; ++var3) {
                                var6.append(' ');
                            }
                        }
                        break;
                    }

                    var8 = var5.next();
                    if (var6.length() > 1) {
                        var6.append(",\n");
                    } else {
                        var6.append('\n');
                    }

                    for(var3 = 0; var3 < var7; ++var3) {
                        var6.append(' ');
                    }

                    var6.append(quote(var8.toString()));
                    var6.append(": ");
                    var6.append(valueToString(this.map.get(var8), var1, var7));
                }
            }

            var6.append('}');
            return var6.toString();
        }
    }

    static String valueToString(Object var0) throws JSONException {
        if (var0 != null && !var0.equals((Object)null)) {
            if (var0 instanceof JSONString) {
                String var1;
                try {
                    var1 = ((JSONString)var0).toJSONString();
                } catch (Exception var3) {
                    throw new JSONException(var3);
                }

                if (var1 instanceof String) {
                    return (String)var1;
                } else {
                    throw new JSONException("Bad value from toJSONString: " + var1);
                }
            } else if (var0 instanceof Number) {
                return numberToString((Number)var0);
            } else if (!(var0 instanceof Boolean) && !(var0 instanceof SerialJSONObject) && !(var0 instanceof SerialJSONArray)) {
                if (var0 instanceof Map) {
                    return (new SerialJSONObject((Map)var0)).toString();
                } else if (var0 instanceof Collection) {
                    return (new SerialJSONArray((Collection)var0)).toString();
                } else {
                    return var0.getClass().isArray() ? (new SerialJSONArray(var0)).toString() : quote(var0.toString());
                }
            } else {
                return var0.toString();
            }
        } else {
            return "null";
        }
    }

    static String valueToString(Object var0, int var1, int var2) throws JSONException {
        if (var0 != null && !var0.equals((Object)null)) {
            try {
                if (var0 instanceof JSONString) {
                    String var3 = ((JSONString)var0).toJSONString();
                    if (var3 instanceof String) {
                        return (String)var3;
                    }
                }
            } catch (Exception var4) {
                ;
            }

            if (var0 instanceof Number) {
                return numberToString((Number)var0);
            } else if (var0 instanceof Boolean) {
                return var0.toString();
            } else if (var0 instanceof SerialJSONObject) {
                return ((SerialJSONObject)var0).toString(var1, var2);
            } else if (var0 instanceof SerialJSONArray) {
                return ((SerialJSONArray)var0).toString(var1, var2);
            } else if (var0 instanceof Map) {
                return (new SerialJSONObject((Map)var0)).toString(var1, var2);
            } else if (var0 instanceof Collection) {
                return (new SerialJSONArray((Collection)var0)).toString(var1, var2);
            } else {
                return var0.getClass().isArray() ? (new SerialJSONArray(var0)).toString(var1, var2) : quote(var0.toString());
            }
        } else {
            return "null";
        }
    }

    @Override
    public Writer write(Writer var1) throws JSONException {
        try {
            boolean var2 = false;
            Iterator var3 = this.keys();
            var1.write(123);

            for(; var3.hasNext(); var2 = true) {
                if (var2) {
                    var1.write(44);
                }

                Object var4 = var3.next();
                var1.write(quote(var4.toString()));
                var1.write(58);
                Object var5 = this.map.get(var4);
                if (var5 instanceof SerialJSONObject) {
                    ((SerialJSONObject)var5).write(var1);
                } else if (var5 instanceof SerialJSONArray) {
                    ((SerialJSONArray)var5).write(var1);
                } else {
                    var1.write(valueToString(var5));
                }
            }

            var1.write(125);
            return var1;
        } catch (IOException var6) {
            throw new JSONException(var6);
        }
    }

    private static final class Null {
        private Null() {
        }

        protected final Object clone() {
            return this;
        }

        public boolean equals(Object var1) {
            return var1 == null || var1 == this;
        }

        public String toString() {
            return "null";
        }
    }
}
