package org.apache.accumulo.metrics.influx;

import org.apache.accumulo.core.metrics.MeterRegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.MeterRegistry;

public class InfluxMetricsRegistryFactory implements MeterRegistryFactory {

  private final Logger log = LoggerFactory.getLogger(InfluxMetricsRegistryFactory.class);

  @Override
  public MeterRegistry create() {
    log.warn("INITIALIZING INFLUX METRICS {}", System.getProperty("accumulo.application"));
    return null;
  }
}
