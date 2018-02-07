package gov.nasa.jpl.view_repo.util;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SerialJSONArray extends JSONArray implements Serializable {
    private static final long serialVersionUID = -401291481377207434L;

    public SerialJSONArray() {
        super();
    }

    public SerialJSONArray(String source) {
        super(source);
    }

    public SerialJSONArray(Collection<?> collection) {
        super(collection);
    }

    @Override
    public SerialJSONArray optJSONArray(int index) {
        Object o = this.opt(index);
        return o instanceof JSONArray ? new SerialJSONArray(o.toString()) : null;
    }

    @Override
    public SerialJSONObject optJSONObject(int index) {
        Object o = this.opt(index);
        return o instanceof JSONObject ? new SerialJSONObject(o.toString()) : null;
    }

    @Override
    public SerialJSONArray getJSONArray(int index) {
        Object object = this.get(index);
        if (object instanceof JSONArray) {
            return new SerialJSONArray(object.toString());
        } else {
            throw new JSONException("SerialJSONArray[" + index + "] is not a SerialJSONArray.");
        }
    }

    @Override
    public SerialJSONObject getJSONObject(int index) {
        Object object = this.get(index);
        if (object instanceof JSONObject) {
            return new SerialJSONObject(object.toString());
        } else if (object instanceof HashMap) {
            return new SerialJSONObject((Map) object);
        } else {
            throw new JSONException("SerialJSONArray[" + index + "] is not a SerialJSONObject.");
        }
    }

    @Override
    public SerialJSONArray put(Object value) {
        return new SerialJSONArray(super.put(value).toString());
    }

    public SerialJSONArray replace(int i, Object value) {
        return (SerialJSONArray) super.put(i, value);
    }

}
