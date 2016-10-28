package com.hpe.caf.worker.datastore.cs;

import com.hpe.caf.storage.sdk.StorageClient;
import com.hpe.caf.storage.sdk.exceptions.StorageClientException;
import com.hpe.caf.storage.sdk.exceptions.StorageServiceConnectException;
import com.hpe.caf.storage.sdk.exceptions.StorageServiceException;
import com.hpe.caf.storage.sdk.model.AssetContainer;
import com.hpe.caf.storage.sdk.model.StorageServiceInfo;
import com.hpe.caf.storage.sdk.model.requests.CreateAssetContainerRequest;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public final class Main
{
    public static void main(String[] args) throws Exception
    {
        DefaultParser defaultParser = new DefaultParser();
        CommandLine cli = defaultParser.parse(createOptions(), args);

        createContainerSdk(cli);
        System.out.println("Seemed to work!");
    }

    private static void createContainerSdk(CommandLine cli) throws Exception
    {
        KeycloakAuthenticationConfiguration authConfig = new KeycloakAuthenticationConfiguration();
        authConfig.setRealm(cli.getOptionValue("kcRealm"));
        authConfig.setClientName(cli.getOptionValue("kcClientName"));
        authConfig.setClientSecret(cli.getOptionValue("kcClientSecret"));
        authConfig.setServerName(cli.getOptionValue("kcServerName"));
        authConfig.setPort(Integer.valueOf(cli.getOptionValue("kcServerPort")));
        authConfig.setUserName(cli.getOptionValue("kcUser"));
        authConfig.setPassword(cli.getOptionValue("kcPassword"));

        KeycloakClient keycloakClient = new KeycloakClient(authConfig);
        String accessToken = keycloakClient.getAccessToken();

        StorageClient client = new StorageClient(cli.getOptionValue("stServerName"), cli.getOptionValue("stServerPort"));
        try {
            StorageServiceInfo storageServiceStatus = client.getStorageServiceStatus();
            AssetContainer container = client.createAssetContainer(new CreateAssetContainerRequest(accessToken, cli.getOptionValue("kcRealm")));

            String id = container.getContainerId();
            System.out.println(container.getContainerId());
        } catch (StorageServiceException | StorageServiceConnectException | StorageClientException e) {

            throw new Exception("Warning: Failed to create container - maybe it already existed.", e);
        }
    }

    private static Options createOptions()
    {
        Options options = new Options();

        options.addOption(Option.builder()
            .longOpt("help")
            .desc("Prints this help message")
            .build());

        options.addOption(Option.builder()
            .longOpt("kcServerName")
            .argName("keycloak server name")
            .desc("Keycloak Server Name, eg: ec2-52-5-46-197.compute-1.amazonaws.com")
            .required()
            .type(String.class)
            .numberOfArgs(1)
            .hasArg()
            .build());

        options.addOption(Option.builder()
            .longOpt("kcServerPort")
            .argName("keycloak port")
            .desc("Keycloak Host Port, eg: 9020")
            .required()
            .numberOfArgs(1)
            .hasArg()
            .type(String.class)
            .build());

        options.addOption(Option.builder()
            .longOpt("kcRealm")
            .argName("keycloak realm")
            .desc("Keycloak Realm Name, eg: CAF")
            .type(String.class)
            .numberOfArgs(1)
            .required()
            .hasArg()
            .build());

        options.addOption(Option.builder()
            .longOpt("kcClientName")
            .argName("keycloak clientname")
            .desc("Keycloak Client Name, eg: CAF_App")
            .required()
            .numberOfArgs(1)
            .hasArg()
            .type(String.class)
            .build());

        options.addOption(Option.builder()
            .longOpt("kcClientSecret")
            .argName("keycloak clientsecret")
            .desc("Keycloak Client Secret, eg: 2cde664f-b267-4b17-ac5b-f2f8dad420b0")
            .required()
            .numberOfArgs(1)
            .hasArg()
            .type(String.class)
            .build());

        options.addOption(Option.builder()
            .longOpt("kcUser")
            .argName("keycloak user")
            .desc("Keycloak User To Obtain Token As eg: caf_worker_user@aspen.local")
            .required()
            .numberOfArgs(1)
            .hasArg()
            .type(String.class)
            .build());

        options.addOption(Option.builder()
            .longOpt("kcPassword")
            .argName("keycloak password")
            .desc("Keycloak Password To Obtain Token As, eg: Password1@")
            .required()
            .numberOfArgs(1)
            .hasArg()
            .type(String.class)
            .build());

        // storage settings used once we get a token.
        options.addOption(Option.builder()
            .longOpt("stServerName")
            .argName("storage server name")
            .desc("Storage Server name to create container within, eg: internalproxy.aspen.local")
            .required()
            .numberOfArgs(1)
            .hasArg()
            .type(String.class)
            .build());

        options.addOption(Option.builder()
            .longOpt("stServerPort")
            .argName("storage server port")
            .desc("Storage Server name to create container within, eg: 9110")
            .required()
            .numberOfArgs(1)
            .hasArg()
            .type(String.class)
            .build());

        options.addOption(Option.builder()
            .longOpt("stContainerName")
            .argName("storage container asset name")
            .desc("Storage Container Asset name to be given to new container, eg: CAF_1_3_1_RC1_Testing")
            .required()
            .numberOfArgs(1)
            .hasArg()
            .type(String.class)
            .build());

        return options;
    }
}
