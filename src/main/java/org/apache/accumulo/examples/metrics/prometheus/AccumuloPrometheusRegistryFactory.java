package org.apache.accumulo.examples.metrics.prometheus;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.accumulo.core.metrics.MeterRegistryFactory;

public class AccumuloPrometheusRegistryFactory implements MeterRegistryFactory {

    @Override
    public MeterRegistry create() {
        return null;
    }
}
