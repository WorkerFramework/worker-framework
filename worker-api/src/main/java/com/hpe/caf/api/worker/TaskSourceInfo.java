package com.hpe.caf.api.worker;

/**
 * Records information about the agent that initiated a task message.
 */
public class TaskSourceInfo {
    /**
     * The name of the agent.
     */
    private String name;

    /**
     * The version of the initiating agent.
     */
    private String version;

    public TaskSourceInfo() {
    }

    public TaskSourceInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
