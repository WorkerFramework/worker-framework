package com.hpe.caf.api.worker;

import com.hpe.caf.api.Configuration;

@Configuration
public class WorkerConfiguration {
    private String workerVersion;


    public String getWorkerVersion() {
        return workerVersion;
    }


    public void setWorkerVersion(String workerVersion) {
        this.workerVersion = workerVersion;
    }
}
