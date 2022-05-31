# Monitoring Accumulo - metrics and tracing

Accumulo 2.1.x changes the implementation of metrics and tracing. These improvements
provide flexibility using the standard 

 - Metrics - [micrometer](https://micrometer.io/)
 - Tracing - [opentelemetry](https://opentelemetry.io/)

## Metrics
Build a jar that contains a class the extends `org.apache.accumulo.core.metrics.MeterRegistryFactory` and place it on the Accumulo classpath.

Add the following to accumulo.properties
```
general.micrometer.enabled=true
general.micrometer.jvm.metrics.enabled=true
general.micrometer.factory=org.apache.accumulo.metrics.influx.InfluxMetricsRegistryFactory
```
## Tracing

