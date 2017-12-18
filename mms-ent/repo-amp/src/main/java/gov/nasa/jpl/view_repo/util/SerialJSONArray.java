package gov.nasa.jpl.view_repo.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class SerialJSONArray extends JSONArray implements Serializable {
    private static final long serialVersionUID = -401291481377207434L;

    private final ArrayList<Object> myArrayList;

    public SerialJSONArray() {
        this.myArrayList = new ArrayList();
    }

    public SerialJSONArray(String source) throws JSONException {
        this(new SerialJSONTokener(source));
    }

    public SerialJSONArray(Collection<?> collection) {
        this.myArrayList = new ArrayList();
        if (collection != null) {
            Iterator var2 = collection.iterator();

            while(var2.hasNext()) {
                Object o = var2.next();
                this.myArrayList.add(JSONObject.wrap(o));
            }
        }

    }

    public SerialJSONArray(Object array) throws JSONException {
        this();
        if (!array.getClass().isArray()) {
            throw new JSONException("JSONArray initial value should be a string or collection or array.");
        } else {
            int length = Array.getLength(array);

            for(int i = 0; i < length; ++i) {
                this.put(JSONObject.wrap(Array.get(array, i)));
            }

        }
    }

    public SerialJSONArray(SerialJSONTokener x) throws JSONException {
        this();
        if (x.nextClean() != '[') {
            throw x.syntaxError("A JSONArray text must start with '['");
        } else if (x.nextClean() != ']') {
            x.back();

            while(true) {
                if (x.nextClean() == ',') {
                    x.back();
                    this.myArrayList.add(JSONObject.NULL);
                } else {
                    x.back();
                    this.myArrayList.add(x.nextValue());
                }

                switch(x.nextClean()) {
                    case ',':
                        if (x.nextClean() == ']') {
                            return;
                        }

                        x.back();
                        break;
                    case ']':
                        return;
                    default:
                        throw x.syntaxError("Expected a ',' or ']'");
                }
            }
        }
    }

    @Override
    public SerialJSONArray optJSONArray(int index) {
        Object o = this.opt(index);
        return o instanceof SerialJSONArray ? (SerialJSONArray) o : null;
    }

    @Override
    public SerialJSONObject optJSONObject(int index) {
        Object o = this.opt(index);
        return o instanceof SerialJSONObject ? (SerialJSONObject) o : null;
    }

    @Override
    public SerialJSONArray getJSONArray(int index) {
        Object object = this.get(index);
        if (object instanceof SerialJSONArray) {
            return (SerialJSONArray) object;
        } else {
            throw new JSONException("SerialJSONArray[" + index + "] is not a SerialJSONArray.");
        }
    }

    @Override
    public SerialJSONObject getJSONObject(int index) {
        Object object = this.get(index);
        if (object instanceof SerialJSONObject) {
            return (SerialJSONObject) object;
        } else {
            throw new JSONException("SerialJSONArray[" + index + "] is not a SerialJSONObject.");
        }
    }

    @Override
    public SerialJSONArray put(Object value) {
        return new SerialJSONArray(super.put(value).toString());
    }

    public SerialJSONArray replace(int i, Object value) {
        return (SerialJSONArray) this.put(i, value);
    }

}
