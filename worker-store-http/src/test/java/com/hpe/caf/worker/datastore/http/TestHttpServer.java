/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.datastore.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;

final class TestHttpServer implements AutoCloseable
{
    private final int port;
    private final HttpServer server;

    public TestHttpServer() throws IOException
    {
        port = findFreePort();
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new DefaultHandler());
        server.setExecutor(null);
        server.start();
    }

    @Override
    public void close() throws Exception
    {
        server.stop(0);
    }

    public int getPort()
    {
        return port;
    }

    static class DefaultHandler implements HttpHandler
    {
        private final Map<String, byte[]> storedData = new HashMap<>();

        @Override
        public void handle(HttpExchange httpExchange) throws IOException
        {
            switch (httpExchange.getRequestMethod()) {
                case "GET":
                    handleGet(httpExchange);
                    break;
                case "PUT":
                    handlePut(httpExchange);
                    break;
                case "DELETE":
                    handleDelete(httpExchange);
                    break;
                default:
                    throw new RuntimeException("Only GET and PUT are supported by this HTTP server");
            }
        }

        private void handleGet(final HttpExchange httpExchange) throws IOException
        {
            final String storedDataReference = httpExchange.getRequestURI().toString().replaceFirst("/", "");
            if (storedDataReference.isEmpty()) {
                httpExchange.sendResponseHeaders(200, -1); // Healthcheck
            } else {
                final byte[] storedDataByteArray = storedData.get(storedDataReference);
                if (storedDataByteArray != null) {
                    httpExchange.sendResponseHeaders(200, storedDataByteArray.length);
                    try (final OutputStream outputStream = httpExchange.getResponseBody()) {
                        outputStream.write(storedDataByteArray);
                    }
                } else {
                    httpExchange.sendResponseHeaders(404, -1);
                }
            }
        }

        private void handlePut(final HttpExchange httpExchange) throws IOException
        {
            final String storedDataReference = httpExchange.getRequestURI().toString().replaceFirst("/", "");
            storedData.put(storedDataReference, IOUtils.toByteArray(httpExchange.getRequestBody()));
            httpExchange.sendResponseHeaders(200, 0);
        }

        private void handleDelete(final HttpExchange httpExchange) throws IOException
        {
            final String storedDataReference = httpExchange.getRequestURI().toString().replaceFirst("/", "");
            storedData.remove(storedDataReference);
            httpExchange.sendResponseHeaders(204, 0);
        }
    }

    private static int findFreePort()
    {
        try (final ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (final IOException e) {
            throw new RuntimeException("Unable to find free port for HTTP server", e);
        }
    }
}
