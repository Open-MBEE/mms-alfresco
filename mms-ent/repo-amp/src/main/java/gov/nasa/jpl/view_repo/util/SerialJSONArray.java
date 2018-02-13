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

}
