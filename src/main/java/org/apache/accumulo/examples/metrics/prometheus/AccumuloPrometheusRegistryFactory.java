package org.apache.accumulo.examples.metrics.prometheus;

import org.apache.accumulo.core.metrics.MeterRegistryFactory;

import io.micrometer.core.instrument.MeterRegistry;

public class AccumuloPrometheusRegistryFactory implements MeterRegistryFactory {

  @Override
  public MeterRegistry create() {
    return null;
  }
}
