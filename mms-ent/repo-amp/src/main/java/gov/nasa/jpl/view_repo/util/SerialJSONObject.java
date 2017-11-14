package gov.nasa.jpl.view_repo.util;

import org.json.JSONObject;

import java.io.Serializable;

public class SerialJSONObject extends JSONObject implements Serializable {
    private static final long serialVersionUID = 5568685394137027249L;

    public SerialJSONObject() {
        super();
    }

    public SerialJSONObject(JSONObject json) {
        super(json != null ? json.toString() : "");
    }

    public SerialJSONObject(String json) {
        super(json != null ? json : "");
    }
}
