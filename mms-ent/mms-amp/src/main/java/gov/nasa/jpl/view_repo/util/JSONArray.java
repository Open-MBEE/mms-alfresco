package gov.nasa.jpl.view_repo.util;

import java.util.Iterator;
import java.util.Collection;

/**
 *
 * @author gpgreen
 */
public class JSONArray implements Iterable<Object> {
    
    private org.json.JSONArray array;

    public JSONArray() {
        array = new org.json.JSONArray();
    }

    public JSONArray(org.json.JSONArray jarray) {
        array = jarray;
    }
    
    public Object get(int idx) {
        return array.get(idx);
    }
    
    public JSONObject getJSONObject(int idx) {
        return new JSONObject(array.getJSONObject(idx));
    }
    
    public String getString(int idx) {
        return array.getString(idx);
    }

    @Override
    public Iterator<Object> iterator() {
        return array.iterator();
    }

    public int length() {
        return array.length();
    }

    public JSONObject optJSONObject(int idx) {
        return new JSONObject(array.optJSONObject(idx));
    }
    
    public String optString(int idx) {
        return array.optString(idx);
    }
    
    public String optString(int idx, String val) {
        return array.optString(idx, val);
    }
    
    public JSONArray put(int idx) {
        array.put(idx);
        return this;
    }

    public JSONArray put(int idx, Collection<?> collection) {
        array.put(idx, collection);
        return this;
    }

    public JSONArray put(int idx, Object obj) {
        array.put(idx, obj);
        return this;
    }

    public JSONArray put(Object obj) {
        array.put(obj);
        return this;
    }

    public JSONArray put(Collection<?> collection) {
        array.put(collection);
        return this;
    }

}
