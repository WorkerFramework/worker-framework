package com.hpe.caf.worker.testing.validation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Map;

/**
 * Created by ploch on 05/12/2015.
 */
public class PropertyMapValidator extends PropertyValidator {
    private final ValidatorFactory validatorFactory;
    private final boolean throwOnNewActualProperty;

    public PropertyMapValidator(ValidatorFactory validatorFactory) {

        this.validatorFactory = validatorFactory;
        this.throwOnNewActualProperty = validatorFactory.shouldThrowOnNewActualProperty();
    }

    public boolean process(PropertyMap itemUnderTest, PropertyMap validationMap) {
        // For each expected property (validationPropertyName) in the expected properties (validationMap)
        // check if the actual properties (itemUnderTest) contains the expected property
        for (String validationPropertyName : validationMap.keySet()) {

            // Test case expected data structure (validationMap):
            // 1)   We iterate over all properties.
            // 2)   If we can't find a matching entry in the validated object it's a failure
            //      because we expected a particular property that wasn't there. Also, we are
            //      only interested in properties listed in test case expectation. Other
            //      properties are ignored.
            // 3)   If we find a matching property but it is another property map we will
            //      recursively apply the same process for this map.
            // 4)   If we fina a matching property and it is a simple type, we execute value
            //      validation logic:
            //      - we run regular expression (validation map entry value) match on the
            //        matched validated property value. This is a default behaviour.
            //      - [phase 2] if we recognize a marker for a different validator we run
            //        this validator (it can be custom validator for example).
            // TODO:    We need a special handling of ReferenceData types:
            //          At the moment this would be treated as a complex property but we need
            //          a different behaviour. ReferenceData is a pointer to actual content
            //          (possibly in datastore). We should use the same mechanism as we do now
            //          in Speech and OCR - compare rehydrated data with file content.
            //          Depending on a file type, we have to do a binary comparison (for binary files)
            //          or text similarity comparison (default similarity should be set to 100%).
            if (!itemUnderTest.containsKey(validationPropertyName)) {
                // TODO: Fail
                throw new AssertionError(String.format("Actual result item under test doesn't have %s property belonging to the expected filter data validation file.", validationPropertyName));
            }

            if (itemUnderTest.isComplexProperty(validationPropertyName) != validationMap.isComplexProperty(validationPropertyName)) {
                throw new AssertionError("Property type mismatch - complex vs non-complex. Property name: " + validationPropertyName);
            }

            Object sourcePropertyValue = itemUnderTest.get(validationPropertyName);
            Object validationPropertyValue = validationMap.get(validationPropertyName);

            System.out.println("*** Validating '" + validationPropertyName + "'" );
            PropertyValidator validator = validatorFactory.create(validationPropertyName, sourcePropertyValue, validationPropertyValue);

            System.out.println("*** Created validator: " + validator.getClass().getSimpleName());
            validator.validate(validationPropertyName, sourcePropertyValue, validationPropertyValue);
        }

        Multimap<String, String> mapOfActualOutputNewProperties = ArrayListMultimap.create();
        // For each actual key that is not within the list of expected keys build up a list of them and print them
        for (Object key : itemUnderTest.keySet().stream().filter(item -> !validationMap.containsKey(item)).toArray()){
            mapOfActualOutputNewProperties.put(key.toString(), itemUnderTest.get(key).toString());
            System.out.println("Expected filter data validation file used doesn't have property belonging to the actual result. " + key.toString() + " " + itemUnderTest.get(key));
        }

        if ( mapOfActualOutputNewProperties.size() > 0 ) {
            if ( throwOnNewActualProperty ) {
                throw new AssertionError(String.format("Expected filter data validation file used does not have the " +
                        "following properties belonging to the actual result's : %s", mapOfActualOutputNewProperties.toString()));
            }
            mapOfActualOutputNewProperties.entries().stream().forEach(newProp -> printStringValueProperty(newProp));
        }
        return true;
    }

    private void printStringValueProperty(Map.Entry<String, String> property){
        System.out.println("New property existing within the actual result: " + property.getKey()
                + " . New property value: " + property.getValue());
    }

    @Override
    public boolean isValid(Object testedPropertyValue, Object validatorPropertyValue) {

        if (!(testedPropertyValue instanceof Map)) return false;
        if (!(validatorPropertyValue instanceof Map)) return false;

        PropertyMap sourceProperties = new PropertyMap((Map<? extends String, ?>) testedPropertyValue);
        PropertyMap validationProperties = new PropertyMap((Map<? extends String, ?>) validatorPropertyValue);

        return process(sourceProperties, validationProperties);
    }
}
