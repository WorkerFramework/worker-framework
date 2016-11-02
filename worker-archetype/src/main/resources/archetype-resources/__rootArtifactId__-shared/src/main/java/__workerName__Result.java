#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.util.ref.ReferencedData;

import javax.validation.constraints.NotNull;

/**
 * The result class of the worker, containing a worker status and a ReferencedData object for textData.
 */
public final class ${workerName}Result {

    /**
     * Worker specific return code.
     */
    @NotNull
    public ${workerName}Status workerStatus;

    /**
     * Result file is stored in datastore and accessed using this ReferencedData object.
     */
    public ReferencedData textData;
}
