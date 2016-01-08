package com.hpe.caf.worker.testing.validation;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStore;

import java.util.Collection;

/**
 * Created by gibsodom on 07/01/2016.
 */
public class ArrayReferencedDataValidator extends PropertyValidator {
    private DataStore dataStore;
    private Codec codec;
    private String testDataLocation;

    public ArrayReferencedDataValidator(DataStore store, Codec codec, String testDataLocation) {
        this.dataStore = store;
        this.codec = codec;
        this.testDataLocation = testDataLocation;
    }


    @Override
    protected boolean isValid(Object testedPropertyValue, Object validatorPropertyValue) {
        if(testedPropertyValue == null && validatorPropertyValue == null){
            return true;
        }
        if (!(testedPropertyValue instanceof Collection)) return false;
        if (!(validatorPropertyValue instanceof Collection)) return false;
        Object[] testedArray = ((Collection) testedPropertyValue).toArray();

        Object[] validationArray = ((Collection) validatorPropertyValue).toArray();

        for (int i = 0; i < validationArray.length; i++) {
            Object testedValue = testedArray[i];
            Object validationValue = validationArray[i];
            PropertyValidator validator = new ReferenceDataValidator(dataStore,codec,testDataLocation); //validatorFactory.create(null,testedValue,validationValue);
            boolean valid = validator.isValid(testedValue, validationValue);
            if (!valid) return false;
        }

        return true;
    }
}
