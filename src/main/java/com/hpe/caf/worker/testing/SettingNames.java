package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 23/11/2015.
 */
public final class SettingNames {

    private SettingNames(){}

    public static final String inputFolder = "input.folder";
    public static final String expectedFolder = "expected.folder";
    public static final String processSubFolders = "process.subfolders";

    // New names for folders above. inputFolder and expectedFolder are now replaced with:
    // testCaseFolder and documentFolder;
    public static final String testCaseFolder = expectedFolder;
    public static final String documentFolder = inputFolder;

    public static final String dataStoreContainerId = "datastore.container.id";
    public static final String useDataStore = "datastore.enabled";

    public static final String dockerHostAddress = "docker.host.address";
    public static final String rabbitmqNodePort = "rabbitmq.node.port";
    public static final String rabbitmqCtrlPort = "rabbitmq.ctrl.port";

    public static final String taskTemplate = "task.template";


}
