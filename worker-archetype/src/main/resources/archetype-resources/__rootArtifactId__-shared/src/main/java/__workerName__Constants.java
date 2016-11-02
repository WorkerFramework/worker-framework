#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

/**
 * ${workerName} constants including API version and the name of the worker.
 */
public final class ${workerName}Constants {

    public static final String WORKER_NAME = "${workerName}";

    public static final int WORKER_API_VER = 1;

    private ${workerName}Constants() {
    }

}
