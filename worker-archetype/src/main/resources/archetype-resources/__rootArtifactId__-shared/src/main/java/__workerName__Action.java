#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

/**
 * Enumeration specifying which action to be taken in the worker.
 */
public enum ${workerName}Action {
    REVERSE,
    CAPITALISE,
    VERBATIM
}
