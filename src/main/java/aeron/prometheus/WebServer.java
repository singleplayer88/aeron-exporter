/*
 * Copyright (c) 2020.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package aeron.prometheus;

import aeron.prometheus.collector.AeronCollector;
import aeron.prometheus.collector.CncFileReader;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Exports aeron cnc.data counters as an HTTP endpoint for Prometheus to poll.
 * <p>
 * Expects port number for the web server in "port" property.
 */
public class WebServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

    private static final String PORT_PROPERTY = "port";

    public static void main(String[] args) throws IOException {
        LOGGER.info("Starting Aeron Exporter");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> LOGGER.info("Aeron Exporter is shutting down")));

        new AeronCollector(new CncFileReader()).register();

        try {
            int port = Integer.parseInt(System.getProperty(PORT_PROPERTY, "-1"));
            if (port == -1) {
                throw new IllegalStateException("Port number expected");
            } else {
                new HTTPServer(new InetSocketAddress(port), CollectorRegistry.defaultRegistry);
            }
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Wrong format for port number");
        }
    }
}
