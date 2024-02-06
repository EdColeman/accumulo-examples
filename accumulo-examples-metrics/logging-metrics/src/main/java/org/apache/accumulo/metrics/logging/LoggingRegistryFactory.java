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

import java.util.function.Consumer;

import org.apache.accumulo.core.metrics.MeterRegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;

/**
 * Set the following properties to use this MeterRegistryFactory
 *
 * <p>
 * Property.GENERAL_MICROMETER_ENABLED = true
 * <p>
 * Property.GENERAL_MICROMETER_FACTORY = LoggingRegistryFactory.class.getName()
 */
public class LoggingRegistryFactory implements MeterRegistryFactory {
  private static final Logger metricsLogger =
      LoggerFactory.getLogger("org.apache.accumulo.METRICS");
  private static final Logger LOG = LoggerFactory.getLogger(LoggingRegistryFactory.class);

  private final Consumer<String> metricConsumer = metricsLogger::info;
  private final LoggingRegistryConfig lconf = c -> {
    if (c.equals("logging.step")) {
      return "10s";
    }
    return null;
  };

  @Override
  public MeterRegistry create() {
    LOG.info("starting metrics registration");
    return LoggingMeterRegistry.builder(lconf).loggingSink(metricConsumer).build();
  }

}
