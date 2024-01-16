/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.accumulo.metrics.prometheus;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class PrometheusExporterServlet extends HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger(PrometheusExporterServlet.class);
  private static final long serialVersionUID = 1L;

  private transient PrometheusMeterRegistry metrics = null;

  @Override
  public void init() {
    LOG.info("called servlet init;");

    LOG.info("Registries: {}", Metrics.globalRegistry.getRegistries());

    var x = Metrics.globalRegistry.getRegistries().stream()
        .filter(r -> r instanceof PrometheusMeterRegistry).findFirst();

    x.ifPresent(meterRegistry -> metrics = (PrometheusMeterRegistry) meterRegistry);

    LOG.info("Metrics: {}", metrics);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String scrape = metrics.scrape();

    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");

    if (acceptsGZipEncoding(request)) {
      // noop - TODO stub if compression is desired
    }

    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println(scrape);
  }

  private boolean acceptsGZipEncoding(HttpServletRequest httpRequest) {
    String acceptEncoding = httpRequest.getHeader("Accept-Encoding");

    return acceptEncoding != null && acceptEncoding.contains("gzip");
  }
}
