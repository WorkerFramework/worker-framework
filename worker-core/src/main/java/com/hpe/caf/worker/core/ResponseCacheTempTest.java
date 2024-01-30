/*
 * Copyright 2015-2024 Open Text.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ResponseCache;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hpe.caf.api.worker.JobNotFoundException;
import com.hpe.caf.api.worker.JobStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

final class ResponseCacheTempTest
{

    private static final Logger LOG = LoggerFactory.getLogger(ResponseCacheTempTest.class);
    public static void main(String [] args) throws Exception
    {
        ResponseCache.setDefault(new JobStatusResponseCache());
//        final Server server = new Server();
//        server.start();
//        LOG.debug("Request 1");
//        getJobStatus("rory16", "http://localhost:1080/test");
//        server.stop();
//        LOG.debug("Request 2");
//        getJobStatus("rory16", "http://localhost:1080/test");

        // HTTP - cache working
        LOG.debug("HTTP");
        LOG.debug("Request 1");
        getJobStatus("rory16", "http://larry-int01.swinfra.net:9410/job-service/v1/partitions/tenant-rorywin1/jobs/rory16/status");
        LOG.debug("Request 2 (Fetched from cache successfully");
        getJobStatus("rory16", "http://larry-int01.swinfra.net:9410/job-service/v1/partitions/tenant-rorywin1/jobs/rory16/status");

        // HTTPS - cache not working
        LOG.debug("HTTPS");
        LOG.debug("Request 1");
        getJobStatus("rory16", "https://larry-int01.swinfra.net:10056/job-service/v1/partitions/tenant-rorywin1/jobs/rory16/status");
        LOG.debug("Request 2");
        getJobStatus("rory16", "https://larry-int01.swinfra.net:10056/job-service/v1/partitions/tenant-rorywin1/jobs/rory16/status");
        LOG.debug("Request 3");
        Thread.sleep(6000);
        getJobStatus("rory16", "https://larry-int01.swinfra.net:10056/job-service/v1/partitions/tenant-rorywin1/jobs/rory16/status");
    }

    private static JobStatusResponse getJobStatus(String jobId, String statusCheckUrl) throws JobNotFoundException
    {
        JobStatusResponse jobStatusResponse = new ResponseCacheTempTest.JobStatusResponse();
        try {
            URL url = new URL(statusCheckUrl);
            final URLConnection connection = url.openConnection();
            if (connection instanceof HttpURLConnection) {
                // Get the request headers
                Map<String,List<String>> requestHeaders = connection.getRequestProperties();

                // Print or process the headers
                for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    LOG.info("Request header: " + key + ": " + String.join(", ", values));
                    //   System.out.println(key + ": " + String.join(", ", values));
                }



                if (((HttpURLConnection) connection).getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw new JobNotFoundException(
                            "Unable to check job status as job " + jobId + " was not found using status check URL " + statusCheckUrl);
                }


            }
            long statusCheckIntervalMillis = JobStatusResponseCache.getStatusCheckIntervalMillis(connection);
            try (BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String responseValue;
                if ((responseValue = response.readLine()) != null) {
                    final String responseValueWithoutQuotes = responseValue.replaceAll("\"", "");
                    LOG.debug("Job {} : retrieved job status '{}' from status check URL {}.",
                            jobId, responseValueWithoutQuotes, statusCheckUrl);
                    jobStatusResponse.setJobStatus(JobStatus.valueOf(responseValueWithoutQuotes));
                } else {
                    LOG.warn("Job {} : assuming that job is active - no suitable response from status check URL {}.", jobId, statusCheckUrl);
                    jobStatusResponse.setJobStatus(JobStatus.Active);
                }
            } catch (Exception ex) {
                LOG.warn("Job {} : assuming that job is active - failed to perform status check using URL {}. ", jobId, statusCheckUrl, ex);
                jobStatusResponse.setJobStatus(JobStatus.Active);
            }
            jobStatusResponse.setStatusCheckIntervalMillis(statusCheckIntervalMillis);
        } catch (final JobNotFoundException e) {
            throw e;
        } catch (final Exception e) {
            LOG.warn("Job {} : assuming that job is active - failed to perform status check using URL {}. ", jobId, statusCheckUrl, e);
            jobStatusResponse.setJobStatus(JobStatus.Active);
        }
        return jobStatusResponse;
    }

    private static class JobStatusResponse
    {
        private JobStatus jobStatus;
        private long statusCheckIntervalMillis;

        public JobStatusResponse()
        {
            this(JobStatus.Active, JobStatusResponseCache.getDefaultJobStatusCheckIntervalMillis());
        }

        public JobStatusResponse(JobStatus jobStatus, long statusCheckInterval)
        {
            this.jobStatus = jobStatus;
            this.statusCheckIntervalMillis = statusCheckInterval;
        }

        public JobStatus getJobStatus()
        {
            return jobStatus;
        }

        public void setJobStatus(JobStatus jobStatus)
        {
            this.jobStatus = jobStatus;
        }

        public long getStatusCheckIntervalMillis()
        {
            return statusCheckIntervalMillis;
        }

        public void setStatusCheckIntervalMillis(long statusCheckIntervalMillis)
        {
            this.statusCheckIntervalMillis = statusCheckIntervalMillis;
        }
    }

    public static class Server
    {
        HttpServer server;

        public void start() throws Exception
        {
            server = HttpServer.create(new InetSocketAddress(1080), 0);
            server.createContext("/test", new MyHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
        }

        public void stop() throws Exception
        {
            server.stop(0);
        }

        static class MyHandler implements HttpHandler
        {
            @Override
            public void handle(HttpExchange t) throws IOException
            {
                String response = "Completed";
                t.getResponseHeaders().add("CacheableJobStatus", "true");
                t.getResponseHeaders().add("Cache-Control", "no-transform, max-age=5");
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
}
