#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.util.ref.ReferencedData;

import javax.validation.constraints.NotNull;

/**
 * The task supplied to the worker. This is the main means of communication to the worker, providing the ReferencedData and
 * the action to take.
 */
public final class ${workerName}Task {

    /**
     * The ReferencedData file in the DataStore.
     */
    @NotNull
    public ReferencedData sourceData;

    /**
     * Identifies a target (relative) location for any output data that worker will save in data store.
     * If datastore-cs is used, this value will identify a target data store container to use.
     * If a worker needs to store output data, this should be used when calling {@link com.hpe.caf.api.worker.DataStore${symbol_pound}store(Path, String)} method.
     * Example usage is located in ${workerName} class, 'wrapAsReferencedData' method.
     */
    public String datastorePartialReference;

    /**
     * Enumeration to represent which action the worker will perform.
     */
    public ${workerName}Action action;
}
