package gov.nasa.jpl.view_repo.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Map;

public class SerialJSONObject extends JSONObject implements Serializable {
    private static final long serialVersionUID = 5568685394137027249L;

    public SerialJSONObject() {
        super();
    }

    public SerialJSONObject(String json) {
        super(json);
    }

    public SerialJSONObject(Map<String, String> json) {
        super(json);
    }

    public SerialJSONObject(Object json) {
        super(json);
    }

    @Override
    public SerialJSONArray optJSONArray(String key) {
        return new SerialJSONArray(super.optJSONArray(key));
    }

    @Override
    public SerialJSONObject optJSONObject(String key) {
        return new SerialJSONObject(super.optJSONObject(key));
    }

    @Override
    public SerialJSONArray getJSONArray(String key) throws JSONException {
        return new SerialJSONArray(super.getJSONArray(key));
    }

    @Override
    public SerialJSONObject getJSONObject(String key) throws JSONException {
        return new SerialJSONObject(super.getJSONObject(key));
    }

    @Override
    public SerialJSONObject put(String key, Object value) throws JSONException {
        return new SerialJSONObject(super.put(key, value));
    }

    @Override
    public SerialJSONObject put(String key, int value) throws JSONException {
        return new SerialJSONObject(super.put(key, value));
    }

}
