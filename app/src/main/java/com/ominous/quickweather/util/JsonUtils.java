package com.ominous.quickweather.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

public class JsonUtils {

    //TODO deserializeGeneric

    public static <T> T deserialize(Class<T> objClass, JSONObject json)
            throws IllegalAccessException, InstantiationException, JSONException {

        T obj = objClass.newInstance();

        for (Field field : objClass.getFields()) {
            if (json.has(field.getName())) {
                if (field.getType().isPrimitive() || field.getType().equals(String.class)) {
                    field.set(obj, json.get(field.getName()));
                } else if (field.getType().isArray()) {
                    field.set(obj, deserializeArray(field.getType().getComponentType(), json.getJSONArray(field.getName())));
                } else {
                    field.set(obj, deserialize(field.getType(), json.getJSONObject(field.getName())));
                }
            }
        }

        return obj;
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] deserializeArray(Class<T> objClass, JSONArray json)
            throws IllegalAccessException, InstantiationException, JSONException {
        T[] objs = (T[]) Array.newInstance(objClass,json.length());

        for (int i=0,l=json.length();i<l;i++) {
            Array.set(objs, i, deserialize(objClass, json.getJSONObject(i)));
        }

        return objs;
    }
}
