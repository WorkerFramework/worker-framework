package com.hp.caf.util.rabbitmq;


import com.hp.caf.api.Encrypted;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * General configuration for a Lyra-managed RabbitMQ server connection from a client.
 */
public class RabbitConfiguration
{
    /**
     * The time (in seconds) between re-attempts if communication with RabbitMQ fails for any reason.
     */
    @Min(1)
    @Max(1000)
    private int backoffInterval = 1;
    /**
     * The maximum time (in seconds) between re-attempts if communication with RabbitMQ fails for any reason.
     */
    @Min(1)
    @Max(1000)
    private int maxBackoffInterval = 30;
    /**
     * The maximum number of retries involving communication failures with RabbitMQ.
     */
    @Min(0)
    @Max(1000)
    private int maxAttempts = 20;
    /**
     * The host that runs RabbitMQ.
     */
    @NotNull
    @Size(min = 1)
    private String rabbitHost;
    /**
     * The port exposed on the host to access RabbitMQ by.
     */
    @Min(1024)
    @Max(65535)
    private int rabbitPort;
    /**
     * The username to access the RabbitMQ server with.
     */
    @NotNull
    @Size(min = 1)
    private String rabbitUser;
    /**
     * The password to access the RabbitMQ server with.
     */
    @Encrypted
    @NotNull
    @Size(min = 1)
    private String rabbitPassword;


    /**
     * @return the seconds between the initial backoff in case of connection failure, this will increase with each subsequent failure
     */
    public int getBackoffInterval()
    {
        return backoffInterval;
    }


    public void setBackoffInterval(final int backoffInterval)
    {
        this.backoffInterval = backoffInterval;
    }


    /**
     * @return the maximum number of seconds between retry attempts
     */
    public int getMaxBackoffInterval()
    {
        return maxBackoffInterval;
    }


    public void setMaxBackoffInterval(final int maxBackoffInterval)
    {
        this.maxBackoffInterval = maxBackoffInterval;
    }


    /**
     * @return the maximum number of retries to perform in case of failure
     */
    public int getMaxAttempts()
    {
        return maxAttempts;
    }


    public void setMaxAttempts(final int maxAttempts)
    {
        this.maxAttempts = maxAttempts;
    }


    /**
     * @return the hostname of the RabbitMQ server, as seen from the client
     */
    public String getRabbitHost()
    {
        return rabbitHost;
    }


    public void setRabbitHost(final String rabbitHost)
    {
        this.rabbitHost = rabbitHost;
    }


    public int getRabbitPort()
    {
        return rabbitPort;
    }


    public void setRabbitPort(final int rabbitPort)
    {
        this.rabbitPort = rabbitPort;
    }


    /**
     * @return the user name to authenticate with on the RabbitMQ server
     */
    public String getRabbitUser()
    {
        return rabbitUser;
    }


    public void setRabbitUser(final String rabbitUser)
    {
        this.rabbitUser = rabbitUser;
    }


    /**
     * @return the password to authenticate with on the RabbitMQ server
     */
    public String getRabbitPassword()
    {
        return rabbitPassword;
    }


    public void setRabbitPassword(final String rabbitPassword)
    {
        this.rabbitPassword = rabbitPassword;
    }
}
