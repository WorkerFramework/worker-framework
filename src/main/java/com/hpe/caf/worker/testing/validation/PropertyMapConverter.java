package com.hpe.caf.worker.testing.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Created by ploch on 05/12/2015.
 */
public class PropertyMapConverter {

    private ObjectMapper mapper = new YAMLMapper();

    public PropertyMapConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public PropertyMapConverter() {
    }

    public PropertyMap convertObject(Object obj) {
        return mapper.convertValue(obj, PropertyMap.class);
    }

}
