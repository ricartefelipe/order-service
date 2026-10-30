package com.example.order.application.port;

/** Counters of the ingestion outcomes, implemented by the observability adapter. */
public interface OrderIngestionMetrics {

    void created();

    void replayed();

    void conflicted();
}
