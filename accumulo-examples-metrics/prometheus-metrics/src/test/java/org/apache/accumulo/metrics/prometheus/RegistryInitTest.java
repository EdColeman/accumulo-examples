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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class RegistryInitTest {
  private static final Logger LOG = LoggerFactory.getLogger(RegistryInitTest.class);

  @Test
  public void useGlobalRegistry() {
    // clean registries if the exist from other test runs.
    Metrics.globalRegistry.getRegistries().forEach(Metrics::removeRegistry);

    CompositeMeterRegistry composite1 = new CompositeMeterRegistry();
    Metrics.addRegistry(composite1);
    SimpleMeterRegistry simple = new SimpleMeterRegistry();
    composite1.add(simple);

    CompositeMeterRegistry composite2 = new CompositeMeterRegistry();
    Metrics.addRegistry(composite2);

    LOG.info("Registered: {}", Metrics.globalRegistry.getRegistries());
    assertEquals(2, Metrics.globalRegistry.getRegistries().size());

    Counter c1c1 = composite1.counter("c1c1", List.of());
    c1c1.increment();

    Counter c2c1 = composite1.counter("c2c1", List.of());
    c2c1.increment();

    LOG.info("c1c1: {}", composite1.get("c1c1").counter().count());
    assertNull(Metrics.globalRegistry.find("c1c1").counter());
    assertThrows(MeterNotFoundException.class, () -> composite2.get("c1c1").counters());
  }
}
