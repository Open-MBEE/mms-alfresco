package gov.nasa.jpl.view_repo.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class SerialJSONObject extends JSONObject implements Serializable {
    private static final long serialVersionUID = 5568685394137027249L;

    private final Map<String, Object> map;

    public SerialJSONObject() {
        this.map = new HashMap();
    }

    public SerialJSONObject(JSONObject jo, String[] names) {
        this();

        for(int i = 0; i < names.length; ++i) {
            try {
                this.putOnce(names[i], jo.opt(names[i]));
            } catch (Exception var5) {
                ;
            }
        }

    }

    public SerialJSONObject(SerialJSONTokener x) throws JSONException {
        this();
        if (x.nextClean() != '{') {
            throw x.syntaxError("A JSONObject text must begin with '{'");
        } else {
            while(true) {
                char c = x.nextClean();
                switch(c) {
                    case '\u0000':
                        throw x.syntaxError("A JSONObject text must end with '}'");
                    case '}':
                        return;
                    default:
                        x.back();
                        String key = x.nextValue().toString();
                        c = x.nextClean();
                        if (c != ':') {
                            throw x.syntaxError("Expected a ':' after a key");
                        }

                        this.putOnce(key, x.nextValue());
                        switch(x.nextClean()) {
                            case ',':
                            case ';':
                                if (x.nextClean() == '}') {
                                    return;
                                }

                                x.back();
                                break;
                            case '}':
                                return;
                            default:
                                throw x.syntaxError("Expected a ',' or '}'");
                        }
                }
            }
        }
    }

    public SerialJSONObject(Map<?, ?> map) {
        this.map = new HashMap();
        if (map != null) {
            Iterator var2 = map.entrySet().iterator();

            while(var2.hasNext()) {
                Map.Entry<?, ?> e = (Map.Entry)var2.next();
                Object value = e.getValue();
                if (value != null) {
                    this.map.put(String.valueOf(e.getKey()), wrap(value));
                }
            }
        }

    }

    public SerialJSONObject(Object bean) {
        this();
        this.populateMap(bean);
    }

    public SerialJSONObject(Object object, String[] names) {
        this();
        Class<?> c = object.getClass();

        for(int i = 0; i < names.length; ++i) {
            String name = names[i];

            try {
                this.putOpt(name, c.getField(name).get(object));
            } catch (Exception var7) {
                ;
            }
        }

    }

    public SerialJSONObject(String source) throws JSONException {
        this(new SerialJSONTokener(source));
    }

    @Override
    public SerialJSONArray optJSONArray(String key) {
        Object o = this.opt(key);
        return o instanceof SerialJSONArray ? (SerialJSONArray) o : null;
    }

    @Override
    public SerialJSONObject optJSONObject(String key) {
        Object object = this.opt(key);
        return object instanceof SerialJSONObject ? (SerialJSONObject) object : null;
    }

    @Override
    public SerialJSONArray getJSONArray(String key) {
        Object object = this.get(key);
        if (object instanceof SerialJSONArray) {
            return (SerialJSONArray) object;
        } else if (object instanceof JSONArray) {
            return new SerialJSONArray(object);
        } else {
            throw new JSONException("SerialJSONObject[" + quote(key) + "] is not a SerialJSONArray.");
        }
    }

    @Override
    public SerialJSONObject getJSONObject(String key) {
        Object object = this.get(key);
        if (object instanceof SerialJSONObject) {
            return (SerialJSONObject) object;
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

    private void populateMap(Object bean) {
        Class<?> klass = bean.getClass();
        boolean includeSuperClass = klass.getClassLoader() != null;
        Method[] methods = includeSuperClass ? klass.getMethods() : klass.getDeclaredMethods();

        for(int i = 0; i < methods.length; ++i) {
            try {
                Method method = methods[i];
                if (Modifier.isPublic(method.getModifiers())) {
                    String name = method.getName();
                    String key = "";
                    if (name.startsWith("get")) {
                        if (!"getClass".equals(name) && !"getDeclaringClass".equals(name)) {
                            key = name.substring(3);
                        } else {
                            key = "";
                        }
                    } else if (name.startsWith("is")) {
                        key = name.substring(2);
                    }

                    if (key.length() > 0 && Character.isUpperCase(key.charAt(0)) && method.getParameterTypes().length == 0) {
                        if (key.length() == 1) {
                            key = key.toLowerCase(Locale.ROOT);
                        } else if (!Character.isUpperCase(key.charAt(1))) {
                            key = key.substring(0, 1).toLowerCase(Locale.ROOT) + key.substring(1);
                        }

                        Object result = method.invoke(bean, (Object[])null);
                        if (result != null) {
                            this.map.put(key, wrap(result));
                        }
                    }
                }
            } catch (Exception var10) {
                ;
            }
        }

    }

}
