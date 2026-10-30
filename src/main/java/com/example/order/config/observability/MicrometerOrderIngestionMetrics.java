package com.example.order.config.observability;

import com.example.order.application.port.OrderIngestionMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Exposes {@code orders.ingestion} with an {@code outcome} tag on the Actuator metrics endpoints. */
@Component
public class MicrometerOrderIngestionMetrics implements OrderIngestionMetrics {

    static final String METRIC_NAME = "orders.ingestion";
    static final String OUTCOME_TAG = "outcome";

    private final Counter created;
    private final Counter replayed;
    private final Counter conflicted;

    public MicrometerOrderIngestionMetrics(MeterRegistry registry) {
        this.created = counter(registry, "created");
        this.replayed = counter(registry, "replayed");
        this.conflicted = counter(registry, "conflicted");
    }

    @Override
    public void created() {
        created.increment();
    }

    @Override
    public void replayed() {
        replayed.increment();
    }

    @Override
    public void conflicted() {
        conflicted.increment();
    }

    private static Counter counter(MeterRegistry registry, String outcome) {
        return Counter.builder(METRIC_NAME)
                .description("Order ingestion outcomes")
                .tag(OUTCOME_TAG, outcome)
                .register(registry);
    }
}
