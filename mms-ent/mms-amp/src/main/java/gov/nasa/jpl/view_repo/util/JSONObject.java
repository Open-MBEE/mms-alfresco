package gov.nasa.jpl.view_repo.util;

import java.util.Iterator;
import java.util.Collection;

/**
 *
 * @author gpgreen
 */
public class JSONObject {
    private org.json.JSONObject o;

    public JSONObject() {
        o = new org.json.JSONObject();
    }

    public JSONObject(org.json.JSONObject jobj) {
        o = jobj;
    }
    
    public JSONObject(String s) {
    	o = new org.json.JSONObject(s);
    }
    
    public JSONObject(Object obj) {
        o = new org.json.JSONObject(obj);
    }

    public Object get(String key) {
        return o.get(key);
    }
    
    public boolean getBoolean(String key) {
        return o.getBoolean(key);
    }

    public JSONArray getJSONArray(String key) {
        return new JSONArray(o.getJSONArray(key));
    }
    
    public JSONObject getJSONObject(String key) {
        return new JSONObject(o.getJSONObject(key));
    }

    /**
     * Get an array of field names from an Object.
     *
     * @return An array of field names, or null if there are no names.
     */
    public static String[] getNames(Object object) {
        return org.json.JSONObject.getNames(object);
    }
    
    public String getString(String key) {
        return o.getString(key);
    }
    
    public boolean has(String key) {
        return o.has(key);
    }

    public boolean isNull(String key) {
        return o.isNull(key);
    }
    
    public Iterator<String> keys() {
        return o.keys();
    }

    public int length() {
        return o.length();
    }
    
    public boolean optBoolean(String key) {
        return o.optBoolean(key);
    }

    public String optString(String key) {
        return o.optString(key);
    }

    public String optString(String key, String val) {
        return o.optString(key, val);
    }

    public JSONArray optJSONArray(String key) {
        return new JSONArray(o.optJSONArray(key));
    }

    public JSONObject optJSONObject(String key) {
        return new JSONObject(o.optJSONObject(key));
    }

    public JSONObject put(String key, Collection<?> array) {
        org.json.JSONArray jlist = new org.json.JSONArray();
        for (Object obj : array) {
            jlist.put(obj);
        }
        o.put(key, jlist);
        return this;
    }
    
    public JSONObject put(String key, JSONObject jobj) {
        o.put(key, jobj.o);
        return this;
    }

    public JSONObject put(String key, Object obj) {
        o.put(key, obj);
        return this;
    }
    
    public JSONObject put(String key, String val) {
        o.put(key, val);
        return this;
    }

    public Object remove(String key) {
        return o.remove(key);
    }
    
    public String toString(int tab) {
        return o.toString(tab);
    }
    
    public static JSONObject NULL = new JSONObject((Object)null);
}
