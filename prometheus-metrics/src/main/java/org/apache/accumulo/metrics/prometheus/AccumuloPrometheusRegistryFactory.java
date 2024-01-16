/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.metrics.prometheus;

import org.apache.accumulo.core.metrics.MeterRegistryFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class AccumuloPrometheusRegistryFactory implements MeterRegistryFactory {
  private static final Logger LOG =
      LoggerFactory.getLogger(AccumuloPrometheusRegistryFactory.class);

  public static final String METRICS_PROMETHEUS_ENDPOINT_PORT = "metrics.prometheus.endpoint.port";

  @Override
  public MeterRegistry create() {
    String port = System.getProperty(METRICS_PROMETHEUS_ENDPOINT_PORT, null);

    if (port == null) {
      throw new IllegalArgumentException("port cannot be null");
    }

    LOG.info("Starting prometheus metrics endpoint at port:{}", port);

    PrometheusMeterRegistry prometheusRegistry =
        new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    LOG.info("scrape: {}", prometheusRegistry.scrape());

    try {
      var endpoint = new AccumuloPrometheusRegistryFactory.MetricsHttpServer(Integer.valueOf(port));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return prometheusRegistry;
  }

  private static class MetricsHttpServer implements AutoCloseable {

    private final Server jetty;
    private final int connectedPort;

    public MetricsHttpServer(final int port) throws Exception {

      jetty = new Server();

      ServerConnector connector = new ServerConnector(jetty);
      connector.setPort(port);
      jetty.setConnectors(new Connector[] {connector});

      ServletHandler servletHandler = new ServletHandler();
      jetty.setHandler(servletHandler);

      servletHandler.addServletWithMapping(PrometheusExporterServlet.class, "/metrics");

      servletHandler.addServletWithMapping(CustomErrorServlet.class, "/*");

      jetty.setStopAtShutdown(true);
      jetty.setStopTimeout(5_000);

      jetty.start();

      connectedPort = ((ServerConnector) jetty.getConnectors()[0]).getLocalPort();

      LOG.info("ZZZ: Metrics HTTP server port: {}", connectedPort);
    }

    public int getConnectedPort() {
      return connectedPort;
    }

    @Override
    public void close() throws Exception {
      jetty.stop();
    }
  }

}
