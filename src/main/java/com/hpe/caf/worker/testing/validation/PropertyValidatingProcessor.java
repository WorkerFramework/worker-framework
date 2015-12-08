package com.hpe.caf.worker.testing.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.AbstractResultProcessor;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.WorkerServices;
import com.hpe.caf.worker.testing.configuration.ValidationSettings;

import java.util.Map;

/**
 * Created by ploch on 07/12/2015.
 */
public abstract class PropertyValidatingProcessor<TResult, TInput, TExpected> extends AbstractResultProcessor<TResult, TInput, TExpected> {


    private final ValidatorFactory validatorFactory;

    protected PropertyValidatingProcessor(TestConfiguration<?, TResult, TInput, TExpected> testConfiguration, WorkerServices workerServices, ValidationSettings validationSettings) {
        super(workerServices.getCodec(), testConfiguration.getWorkerResultClass());
        this.validatorFactory = new ValidatorFactory(validationSettings, workerServices.getDataStore(), workerServices.getCodec(), testConfiguration.getTestDataFolder());
    }

    @Override
    protected boolean processWorkerResult(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result) throws Exception {

        Map<String, Object> expectation = getExpectationMap(testItem, message, result);
        PropertyMap expectationPropertyMap = new PropertyMap(expectation);

        ObjectMapper mapper = new ObjectMapper();
        Object validatedObject = getValidatedObject(testItem, message, result);

        PropertyMap propertyMap = mapper.convertValue(validatedObject, PropertyMap.class);

        PropertyValidator validator = validatorFactory.create("Root", propertyMap, expectationPropertyMap);

        validator.validate("Root", propertyMap, expectationPropertyMap);

        testItem.setCompleted(isCompleted(testItem, message, result));
        return true;
    }

    protected abstract boolean isCompleted(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result);

    protected abstract Map<String, Object> getExpectationMap(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result);

    protected abstract Object getValidatedObject(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result);

}
