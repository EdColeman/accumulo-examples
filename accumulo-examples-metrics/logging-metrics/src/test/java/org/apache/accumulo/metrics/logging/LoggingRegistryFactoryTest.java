/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.accumulo.metrics.logging;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

public class LoggingRegistryFactoryTest {

  private static final Logger LOG = LoggerFactory.getLogger(LoggingRegistryFactoryTest.class);

  @Test
  public void metricsGenTest() {
    LoggingRegistryFactory factory = new LoggingRegistryFactory();
    var loggingReg = factory.create();
    Metrics.globalRegistry.add(loggingReg);

    LOG.info("Starting metrics generation");

    var samples = new MetricsSampleGen();
    try {
      for (int i = 0; i < 20; i++) {
        samples.doWork();
        Thread.sleep(10_000);
      }
    } catch (Exception ex) {
      // empty
    }
  }

  private static class MetricsSampleGen {
    private final AtomicLong gauge1;
    private final Counter counter1;

    public MetricsSampleGen() {
      Metrics.globalRegistry.config().commonTags("env", "test", "tag1", "tag-1");
      gauge1 = Metrics.gauge("gauge-1", new AtomicLong());
      counter1 = Metrics.counter("counter-1");
    }

    public void doWork() {
      var val1 = gauge1.incrementAndGet();
      counter1.increment(val1 * 3);
    }
  }
}
