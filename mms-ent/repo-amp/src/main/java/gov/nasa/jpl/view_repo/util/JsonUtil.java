/*******************************************************************************
 * Copyright (c) <2018>, The Boeing Company
 *
 * All rights reserved.
 *
 ******************************************************************************/

package gov.nasa.jpl.view_repo.util;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/**
 * Simple static class for working with com.google.gson objects
 * Version 2.8.0 of this library, doesn't have a deepCopy method
 * 
 * @author ggreen
 *
 */
public class JsonUtil {

    /**
     * make a deep copy, 
     * Version 2.8.0 of this library, doesn't have a deepCopy method
     * version 2.8.2 has added it, then this can be removed
     * 
     * @author ggreen
     *
     */
    public static JsonElement deepCopy(JsonElement element) {
        if (element.isJsonNull())
            return JsonNull.INSTANCE;
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            JsonArray newarray = new JsonArray();
            for (int i=0; i<array.size(); ++i)
                newarray.add(deepCopy(array.get(i)));
            return newarray;
        } else if (element.isJsonObject()) {
            JsonObject copyFrom = element.getAsJsonObject();
            JsonObject o = new JsonObject();
            for (Map.Entry<String, JsonElement> elem : copyFrom.entrySet())  {
                o.add(elem.getKey(), deepCopy(elem.getValue()));
            }
            return o;
        }
        return element;
    }
	
    public static JsonObject deepCopy(JsonObject obj) {
        return (JsonObject)deepCopy((JsonElement)obj);
    }
	
    public static JsonObject buildFromString(String str) {
        if (str == null || str.isEmpty())
            return new JsonObject();
        JsonParser parser = new JsonParser();
        return parser.parse(str).getAsJsonObject();
    }
	
    public static JsonObject addStringList(JsonObject obj, String key, List<String> values) {
        JsonArray array = new JsonArray();
        if (values != null) {
            for (int i=0; i<values.size(); ++i)
                array.add(values.get(i));
        }
        obj.add(key, array);
        return obj;
    }
	
    public static JsonObject addStringSet(JsonObject obj, String key, Set<String> values) {
        JsonArray array = new JsonArray();
        if (values != null) {
            Iterator<String> i = values.iterator();
            while (i.hasNext())
                array.add(i.next());
        }
        obj.add(key, array);
        return obj;
    }
	
    public static JsonObject fromMap(Map<String, String> propertyMap) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, String> entry : propertyMap.entrySet())
            obj.addProperty(entry.getKey(), entry.getValue());
        return obj;
    }
	
    public static JsonArray getOptArray(JsonObject obj, String name) {
        if (obj == null || !obj.has(name) || (obj.has(name) && obj.get(name).isJsonNull()))
            return new JsonArray();
        return obj.get(name).getAsJsonArray();
    }

    public static JsonObject getOptObject(JsonObject obj, String name) {
        if (obj == null || !obj.has(name) || (obj.has(name) && obj.get(name).isJsonNull()))
            return new JsonObject();
        return obj.get(name).getAsJsonObject();
    }

    public static String getOptString(JsonObject obj, String name) {
        if (obj == null || !obj.has(name) || (obj.has(name) && obj.get(name).isJsonNull()))
            return "";
        return obj.get(name).getAsString();
    }
    
    public static String getOptString(JsonObject obj, String name, String option) {
        if (obj == null || !obj.has(name) || (obj.has(name) && obj.get(name).isJsonNull()))
            return option;
        return obj.get(name).getAsString();
    }
    
    public static JsonObject getOptObject(JsonArray arry, int index) {
        JsonElement elem = arry.get(index);
        if (elem.isJsonObject())
            return elem.getAsJsonObject();
        return new JsonObject();
    }

    public static String getOptString(JsonArray arry, int index) {
        JsonElement elem = arry.get(index);
        if (elem.isJsonPrimitive())
            return elem.getAsString();
        return "";
    }
}
