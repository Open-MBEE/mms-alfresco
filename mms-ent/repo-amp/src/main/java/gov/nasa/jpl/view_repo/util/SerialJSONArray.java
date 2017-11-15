package gov.nasa.jpl.view_repo.util;

import org.json.JSONArray;

import java.io.Serializable;

public class SerialJSONArray extends JSONArray implements Serializable {
    private static final long serialVersionUID = -401291481377207434L;

    public SerialJSONArray() {
        super();
    }

    public SerialJSONArray(String json) {
        super(json);
    }

    public SerialJSONArray(Object json) {
        super(json);
    }

    @Override
    public SerialJSONArray optJSONArray(int index) {
        return new SerialJSONArray(super.optJSONArray(index));
    }

    @Override
    public SerialJSONObject optJSONObject(int index) {
        return new SerialJSONObject(super.optJSONObject(index));
    }

    @Override
    public SerialJSONArray getJSONArray(int index) {
        return new SerialJSONArray(super.getJSONArray(index));
    }

    @Override
    public SerialJSONObject getJSONObject(int index) {
        return new SerialJSONObject(super.getJSONObject(index));
    }

    @Override public SerialJSONArray put(Object value) {
        return new SerialJSONArray(super.put(value));
    }

}
