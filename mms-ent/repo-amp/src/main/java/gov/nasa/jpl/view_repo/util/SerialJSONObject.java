package gov.nasa.jpl.view_repo.util;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SerialJSONObject extends JSONObject implements Serializable {
    private static final long serialVersionUID = 5568685394137027249L;

    public SerialJSONObject() {
        super();
    }

    public SerialJSONObject(Map<?, ?> map) {
        super(map);
    }

    public SerialJSONObject(String source) {
        super(source);
    }

    @Override
    public SerialJSONArray optJSONArray(String key) {
        Object o = this.opt(key);
        return o instanceof JSONArray ? new SerialJSONArray(o.toString()) : null;
    }

    @Override
    public SerialJSONObject optJSONObject(String key) {
        Object object = this.opt(key);
        return object instanceof JSONObject ? new SerialJSONObject(object.toString()) : null;
    }

    @Override
    public SerialJSONArray getJSONArray(String key) {
        Object object = this.get(key);
        if (object instanceof SerialJSONArray) {
            return (SerialJSONArray) object;
        } else if (object instanceof JSONArray) {
            return new SerialJSONArray(object.toString());
        } else if (object instanceof ArrayList) {
            return new SerialJSONArray((List) object);
        } else {
            throw new JSONException("SerialJSONObject[" + quote(key) + "] is not a SerialJSONArray.");
        }
    }

    @Override
    public SerialJSONObject getJSONObject(String key) {
        Object object = this.get(key);
        if (object instanceof SerialJSONObject) {
            return (SerialJSONObject) object;
        } else if (object instanceof JSONObject) {
            return new SerialJSONObject(object.toString());
        } else {
            throw new JSONException("SerialJSONObject[" + quote(key) + "] is not a SerialJSONObject.");
        }
    }

    @Override
    public SerialJSONObject put(String key, Object value) {
        return (SerialJSONObject) super.put(key, value);
    }

    public SerialJSONObject put(String key, int value) {
        return (SerialJSONObject) super.put(key, value);
    }
}
