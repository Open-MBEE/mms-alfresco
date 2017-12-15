package gov.nasa.jpl.view_repo.util;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class SerialJSONArray extends JSONArray implements Serializable {
    static Logger logger = Logger.getLogger(SerialJSONArray.class);

    private static final long serialVersionUID = -401291481377207434L;
    private ArrayList myArrayList;

    public SerialJSONArray() {
        this.myArrayList = new ArrayList();
    }

    public SerialJSONArray(SerialJSONTokener var1) throws JSONException {
        this();
        char var2 = var1.nextClean();
        char var3;
        if (var2 == '[') {
            var3 = ']';
        } else {
            if (var2 != '(') {
                throw var1.syntaxError("A SerialJSONArray text must start with '['");
            }

            var3 = ')';
        }

        if (var1.nextClean() != ']') {
            var1.back();

            while(true) {
                if (var1.nextClean() == ',') {
                    var1.back();
                    this.myArrayList.add((Object)null);
                } else {
                    var1.back();
                    this.myArrayList.add(var1.nextValue());
                }

                var2 = var1.nextClean();
                switch(var2) {
                    case ')':
                    case ']':
                        if (var3 != var2) {
                            throw var1.syntaxError("Expected a '" + new Character(var3) + "'");
                        }

                        return;
                    case ',':
                    case ';':
                        if (var1.nextClean() == ']') {
                            return;
                        }

                        var1.back();
                        break;
                    default:
                        throw var1.syntaxError("Expected a ',' or ']'");
                }
            }
        }
    }

    public SerialJSONArray(String var1) throws JSONException {
        this(new SerialJSONTokener(var1));
    }

    public SerialJSONArray(Collection var1) {
        this.myArrayList = var1 == null ? new ArrayList() : new ArrayList(var1);
    }

    public SerialJSONArray(Collection var1, boolean var2) {
        this.myArrayList = new ArrayList();
        if (var1 != null) {
            Iterator var3 = var1.iterator();

            while(var3.hasNext()) {
                this.myArrayList.add(new SerialJSONObject(var3.next(), var2));
            }
        }

    }

    public SerialJSONArray(Object var1) throws JSONException {
        this();
        if (!var1.getClass().isArray()) {
            throw new JSONException("JSONArray initial value should be a string or collection or array.");
        } else {
            int var2 = Array.getLength(var1);

            for(int var3 = 0; var3 < var2; ++var3) {
                this.put(Array.get(var1, var3));
            }

        }
    }

    public SerialJSONArray(Object var1, boolean var2) throws JSONException {
        this();
        if (!var1.getClass().isArray()) {
            throw new JSONException("JSONArray initial value should be a string or collection or array.");
        } else {
            int var3 = Array.getLength(var1);

            for(int var4 = 0; var4 < var3; ++var4) {
                this.put((Object)(new SerialJSONObject(Array.get(var1, var4), var2)));
            }

        }
    }

    public Object get(int var1) throws JSONException {
        Object var2 = this.opt(var1);
        if (var2 == null) {
            throw new JSONException("JSONArray[" + var1 + "] not found.");
        } else {
            return var2;
        }
    }

    public boolean getBoolean(int var1) throws JSONException {
        Object var2 = this.get(var1);
        if (!var2.equals(Boolean.FALSE) && (!(var2 instanceof String) || !((String)var2).equalsIgnoreCase("false"))) {
            if (!var2.equals(Boolean.TRUE) && (!(var2 instanceof String) || !((String)var2).equalsIgnoreCase("true"))) {
                throw new JSONException("JSONArray[" + var1 + "] is not a Boolean.");
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public double getDouble(int var1) throws JSONException {
        Object var2 = this.get(var1);

        try {
            return var2 instanceof Number ? ((Number)var2).doubleValue() : Double.valueOf((String)var2);
        } catch (Exception var4) {
            throw new JSONException("JSONArray[" + var1 + "] is not a number.");
        }
    }

    public int getInt(int var1) throws JSONException {
        Object var2 = this.get(var1);
        return var2 instanceof Number ? ((Number)var2).intValue() : (int)this.getDouble(var1);
    }

    public SerialJSONArray getJSONArray(int var1) throws JSONException {
        Object var2 = this.get(var1);
        if (var2 instanceof SerialJSONArray) {
            return (SerialJSONArray)var2;
        } else {
            throw new JSONException("JSONArray[" + var1 + "] is not a SerialJSONArray.");
        }
    }

    public SerialJSONObject getJSONObject(int var1) throws JSONException {
        Object var2 = this.get(var1);
        if (var2 instanceof SerialJSONObject) {
            return (SerialJSONObject)var2;
        } else {
            throw new JSONException("JSONArray[" + var1 + "] is not a SerialJSONObject.");
        }
    }

    public long getLong(int var1) throws JSONException {
        Object var2 = this.get(var1);
        return var2 instanceof Number ? ((Number)var2).longValue() : (long)this.getDouble(var1);
    }

    public String getString(int var1) throws JSONException {
        return this.get(var1).toString();
    }

    public boolean isNull(int var1) {
        return SerialJSONObject.NULL.equals(this.opt(var1));
    }

    public String join(String var1) throws JSONException {
        int var2 = this.length();
        StringBuffer var3 = new StringBuffer();

        for(int var4 = 0; var4 < var2; ++var4) {
            if (var4 > 0) {
                var3.append(var1);
            }

            var3.append(SerialJSONObject.valueToString(this.myArrayList.get(var4)));
        }

        return var3.toString();
    }

    public int length() {
        return this.myArrayList.size();
    }

    public Object opt(int var1) {
        return var1 >= 0 && var1 < this.length() ? this.myArrayList.get(var1) : null;
    }

    public boolean optBoolean(int var1) {
        return this.optBoolean(var1, false);
    }

    public boolean optBoolean(int var1, boolean var2) {
        try {
            return this.getBoolean(var1);
        } catch (Exception var4) {
            return var2;
        }
    }

    public double optDouble(int var1) {
        return this.optDouble(var1, 0.0D / 0.0);
    }

    public double optDouble(int var1, double var2) {
        try {
            return this.getDouble(var1);
        } catch (Exception var5) {
            return var2;
        }
    }

    public int optInt(int var1) {
        return this.optInt(var1, 0);
    }

    public int optInt(int var1, int var2) {
        try {
            return this.getInt(var1);
        } catch (Exception var4) {
            return var2;
        }
    }

    public SerialJSONArray optJSONArray(int var1) {
        Object var2 = this.opt(var1);
        return var2 instanceof SerialJSONArray ? (SerialJSONArray)var2 : null;
    }

    public SerialJSONObject optJSONObject(int var1) {
        Object var2 = this.opt(var1);
        return var2 instanceof SerialJSONObject ? (SerialJSONObject)var2 : null;
    }

    public long optLong(int var1) {
        return this.optLong(var1, 0L);
    }

    public long optLong(int var1, long var2) {
        try {
            return this.getLong(var1);
        } catch (Exception var5) {
            return var2;
        }
    }

    public String optString(int var1) {
        return this.optString(var1, "");
    }

    public String optString(int var1, String var2) {
        Object var3 = this.opt(var1);
        return var3 != null ? var3.toString() : var2;
    }

    public SerialJSONArray put(boolean var1) {
        this.put((Object)(var1 ? Boolean.TRUE : Boolean.FALSE));
        return this;
    }

    public SerialJSONArray put(Collection var1) {
        this.put((Object)(new SerialJSONArray(var1)));
        return this;
    }

    public SerialJSONArray put(double var1) throws JSONException {
        Double var3 = new Double(var1);
        SerialJSONObject.testValidity(var3);
        this.put((Object)var3);
        return this;
    }

    public SerialJSONArray put(int var1) {
        this.put((Object)(new Integer(var1)));
        return this;
    }

    public SerialJSONArray put(long var1) {
        this.put((Object)(new Long(var1)));
        return this;
    }

    public SerialJSONArray put(Map var1) {
        this.put((Object)(new SerialJSONObject(var1)));
        return this;
    }

    public SerialJSONArray put(Object var1) {
        this.myArrayList.add(var1);
        return this;
    }

    public SerialJSONArray put(int var1, boolean var2) throws JSONException {
        this.put(var1, (Object)(var2 ? Boolean.TRUE : Boolean.FALSE));
        return this;
    }

    public SerialJSONArray put(int var1, Collection var2) throws JSONException {
        this.put(var1, (Object)(new SerialJSONArray(var2)));
        return this;
    }

    public SerialJSONArray put(int var1, double var2) throws JSONException {
        this.put(var1, (Object)(new Double(var2)));
        return this;
    }

    public SerialJSONArray put(int var1, int var2) throws JSONException {
        this.put(var1, (Object)(new Integer(var2)));
        return this;
    }

    public SerialJSONArray put(int var1, long var2) throws JSONException {
        this.put(var1, (Object)(new Long(var2)));
        return this;
    }

    public SerialJSONArray put(int var1, Map var2) throws JSONException {
        this.put(var1, (Object)(new SerialJSONObject(var2)));
        return this;
    }

    public SerialJSONArray put(int var1, Object var2) throws JSONException {
        SerialJSONObject.testValidity(var2);
        if (var1 < 0) {
            throw new JSONException("JSONArray[" + var1 + "] not found.");
        } else {
            if (var1 < this.length()) {
                this.myArrayList.set(var1, var2);
            } else {
                while(var1 != this.length()) {
                    this.put(SerialJSONObject.NULL);
                }

                this.put(var2);
            }

            return this;
        }
    }

    public SerialJSONObject toJSONObject(JSONArray var1) throws JSONException {
        if (var1 != null && var1.length() != 0 && this.length() != 0) {
            SerialJSONObject var2 = new SerialJSONObject();

            for(int var3 = 0; var3 < var1.length(); ++var3) {
                var2.put(var1.getString(var3), this.opt(var3));
            }

            return var2;
        } else {
            return null;
        }
    }

    public String toString() {
        try {
            return '[' + this.join(",") + ']';
        } catch (Exception var2) {
            return null;
        }
    }

    public String toString(int var1) throws JSONException {
        return this.toString(var1, 0);
    }

    String toString(int var1, int var2) throws JSONException {
        int var3 = this.length();
        if (var3 == 0) {
            return "[]";
        } else {
            StringBuffer var5 = new StringBuffer("[");
            if (var3 == 1) {
                var5.append(SerialJSONObject.valueToString(this.myArrayList.get(0), var1, var2));
            } else {
                int var6 = var2 + var1;
                var5.append('\n');

                int var4;
                for(var4 = 0; var4 < var3; ++var4) {
                    if (var4 > 0) {
                        var5.append(",\n");
                    }

                    for(int var7 = 0; var7 < var6; ++var7) {
                        var5.append(' ');
                    }

                    var5.append(SerialJSONObject.valueToString(this.myArrayList.get(var4), var1, var6));
                }

                var5.append('\n');

                for(var4 = 0; var4 < var2; ++var4) {
                    var5.append(' ');
                }
            }

            var5.append(']');
            return var5.toString();
        }
    }

    public Writer write(Writer var1) throws JSONException {
        try {
            boolean var2 = false;
            int var3 = this.length();
            var1.write(91);

            for(int var4 = 0; var4 < var3; ++var4) {
                if (var2) {
                    var1.write(44);
                }

                Object var5 = this.myArrayList.get(var4);
                if (var5 instanceof SerialJSONObject) {
                    ((SerialJSONObject)var5).write(var1);
                } else if (var5 instanceof SerialJSONArray) {
                    ((SerialJSONArray)var5).write(var1);
                } else {
                    var1.write(SerialJSONObject.valueToString(var5));
                }

                var2 = true;
            }

            var1.write(93);
            return var1;
        } catch (IOException var6) {
            throw new JSONException(var6);
        }
    }
}
