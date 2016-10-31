package com.hpe.caf.worker.testing.validation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by ploch on 05/12/2015.
 */
public class PropertyMap extends LinkedHashMap<String, Object> {

    public PropertyMap(Map<? extends String, ?> m) {
        super(m);
    }

    public PropertyMap() {}


    public boolean isComplexProperty(String name) {
        if (!containsKey(name)) throw propertyNotFound(name);
        Object propertyValue = get(name);

        return propertyValue instanceof Map;
    }

    public PropertyMap getComplex(String key) {
        if (!containsKey(key)) throw propertyNotFound(key);
        Object propertyValue = super.get(key);

        if (!(propertyValue instanceof Map))  throw complexPropertyExpected(key, propertyValue);

        return new PropertyMap((Map)propertyValue);
    }


    private RuntimeException propertyNotFound(String propertyName) {
        return new RuntimeException(String.format("Property %s was not found.", propertyName));
    }

    private RuntimeException complexPropertyExpected(String propertyName, Object propertyValue) {
        return new RuntimeException(String.format("Expected that %s property to be a complex type but value type is %s type.", propertyName, propertyValue.getClass().getSimpleName()));

    };
}
