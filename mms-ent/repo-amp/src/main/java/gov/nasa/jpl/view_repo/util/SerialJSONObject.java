package gov.nasa.jpl.view_repo.util;

import com.google.gson.JsonObject;

import java.io.Serializable;

public class SerialJSONObject implements Serializable {
    private static final long serialVersionUID = 5568685394137027249L;
    private String src;

    public SerialJSONObject(String source) {
        src = source;
    }

    public JsonObject getJSONObject() {
        if (src == null)
            return new JsonObject();
        return JsonUtil.buildFromString(src);
    }
}
