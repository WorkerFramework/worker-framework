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
 * The {@code PropertyValidatingProcessor} class.
 * Processor validating property maps - objects converted to maps of
 * property names and values.
 * Servers as an entry point for result validation and creates actual
 * validators ({@link PropertyValidator} implementations).
 * depending on property type.
 *
 * @param <TResult>   the worker result type parameter
 * @param <TInput>    the worker test input type parameter
 * @param <TExpected> the worker test expectation type parameter
 */
public abstract class PropertyValidatingProcessor<TResult, TInput, TExpected> extends AbstractResultProcessor<TResult, TInput, TExpected> {


    private final ValidatorFactory validatorFactory;

    /**
     * Instantiates a new Property validating processor.
     *
     * @param testConfiguration  the test configuration
     * @param workerServices     the worker services
     * @param validationSettings the validation settings
     */
    public PropertyValidatingProcessor(TestConfiguration<?, TResult, TInput, TExpected> testConfiguration, WorkerServices workerServices, ValidationSettings validationSettings) {
        super(workerServices.getCodec(), testConfiguration.getWorkerResultClass());
        this.validatorFactory = new ValidatorFactory(validationSettings, workerServices.getDataStore(), workerServices.getCodec(), testConfiguration.getTestDataFolder());
    }

    /**
     * Validates worker result.
     * This method acquires a map used for validation from test item expectation
     * and converts validated result into another map that will be validated.
     * {@link ValidatorFactory} creates a concrete {@link PropertyValidator} which then
     * validates the result using expectation.
     * @param testItem the test item which contains test input and expectation
     * @param message the task message retrieved from a queue
     * @param result the worker result deserialized from message body
     * @return the boolean indicating whether validation succeeded.
     * @throws Exception
     */
    @Override
    protected boolean processWorkerResult(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result) throws Exception {

        Map<String, Object> expectation = getExpectationMap(testItem, message, result);
        if (expectation == null) {
            System.err.println("Could not locate result in pre-defined testcase, item tag '" + testItem.getTag() + "'. Message id: '" + message.getTaskId() + "'. ");
            return false;
        }
        PropertyMap expectationPropertyMap = new PropertyMap(expectation);

        ObjectMapper mapper = new ObjectMapper();
        Object validatedObject = getValidatedObject(testItem, message, result);

        PropertyMap propertyMap = mapper.convertValue(validatedObject, PropertyMap.class);

        PropertyValidator
                validator = validatorFactory.createRootValidator();

        validator.validate("Root", propertyMap, expectationPropertyMap);

        testItem.setCompleted(isCompleted(testItem, message, result));
        return true;
    }

    /**
     * Is completed boolean.
     *
     * @param testItem the test item
     * @param message  the message
     * @param result   the result
     * @return the boolean
     */
    protected abstract boolean isCompleted(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result);

    /**
     * Gets expectation map.
     *
     * @param testItem the test item
     * @param message  the message
     * @param result   the result
     * @return the expectation map
     */
    protected abstract Map<String, Object> getExpectationMap(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result);

    /**
     * Gets validated object.
     *
     * @param testItem the test item
     * @param message  the message
     * @param result   the result
     * @return the validated object
     */
    protected abstract Object getValidatedObject(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result);

}
