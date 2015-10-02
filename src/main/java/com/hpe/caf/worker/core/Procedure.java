package com.hpe.caf.worker.core;


/**
 * Functional interface that takes no arguments and produces no result but performs some arbitrary action.
 */
@FunctionalInterface
public interface Procedure
{
    void execute();
}
