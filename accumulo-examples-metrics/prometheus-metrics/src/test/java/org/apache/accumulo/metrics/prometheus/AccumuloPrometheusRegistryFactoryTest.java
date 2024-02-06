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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

class AccumuloPrometheusRegistryFactoryTest {

  private static final Logger LOG =
      LoggerFactory.getLogger(AccumuloPrometheusRegistryFactoryTest.class);
  private static CompositeMeterRegistry composite;

  private static Counter counter1;

  @BeforeAll
  public static void setup() {
    Properties p = System.getProperties();
    p.setProperty(AccumuloPrometheusRegistryFactory.METRICS_PROMETHEUS_ENDPOINT_PORT, "10123");
    composite = new CompositeMeterRegistry();

    Metrics.addRegistry(composite);

  }

  @Test
  public void createTest() throws Exception {
    AccumuloPrometheusRegistryFactory factory = new AccumuloPrometheusRegistryFactory();
    composite.add(factory.create());

    counter1 = composite.counter("counter1", List.of());
    counter1.increment();

    Thread.sleep(5_000);

    counter1.increment();

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder().uri(URI.create("http://localhost:" + "10123/metrics")).build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    LOG.info("response status: {}", response.statusCode());
    LOG.info("response body: {}", response.body());

    Thread.sleep(5_000);

    response = client.send(request, HttpResponse.BodyHandlers.ofString());
    LOG.info("response status: {}", response.statusCode());
    LOG.info("response body: {}", response.body());

  }
}
