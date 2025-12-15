package com.example.order.integration;

import com.example.order.adapters.web.model.CreateOrderItemRequest;
import com.example.order.adapters.web.model.CreateOrderRequest;
import com.example.order.adapters.web.model.OrderResponse;
import com.example.order.adapters.web.model.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("orders")
            .withUsername("orders")
            .withPassword("orders");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDb() {
        // order_items depends on orders
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM orders");
    }

    @Test
    void postShouldCreateOrder_201() {
        String externalOrderId = "A-POST-1";
        CreateOrderRequest request = buildRequest(externalOrderId);

        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                url("/orders"),
                HttpMethod.POST,
                new HttpEntity<>(request, jsonHeaders("corr-1")),
                OrderResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(externalOrderId, response.getBody().externalOrderId());
        assertEquals("CALCULATED", response.getBody().status());
        assertEquals(new BigDecimal("26.00"), response.getBody().totalAmount());
        assertEquals(2, response.getBody().items().size());

        assertTrue(response.getHeaders().containsKey("X-Correlation-Id"));
    }

    @Test
    void postDuplicateShouldReturnExisting_200_andNotDuplicate() {
        String externalOrderId = "A-DUP-1";
        CreateOrderRequest request = buildRequest(externalOrderId);

        ResponseEntity<OrderResponse> first = restTemplate.exchange(
                url("/orders"),
                HttpMethod.POST,
                new HttpEntity<>(request, jsonHeaders("corr-1")),
                OrderResponse.class
        );

        ResponseEntity<OrderResponse> second = restTemplate.exchange(
                url("/orders"),
                HttpMethod.POST,
                new HttpEntity<>(request, jsonHeaders("corr-2")),
                OrderResponse.class
        );

        assertEquals(HttpStatus.CREATED, first.getStatusCode());
        assertEquals(HttpStatus.OK, second.getStatusCode());

        assertNotNull(first.getBody());
        assertNotNull(second.getBody());
        assertEquals(first.getBody().id(), second.getBody().id());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE external_order_id = ?",
                Integer.class,
                externalOrderId
        );
        assertEquals(1, count);
    }

    @Test
    void getByIdShouldReturnOrder_200() {
        UUID id = createOrder("A-GET-1");

        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                url("/orders/" + id),
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders("corr-get")),
                OrderResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(id, response.getBody().id());
        assertEquals("CALCULATED", response.getBody().status());
    }

    @Test
    void getPagedByStatusShouldReturnMetadata() {
        createOrder("A-LIST-1");
        createOrder("A-LIST-2");

        ResponseEntity<PagedResponse<OrderResponse>> response = restTemplate.exchange(
                url("/orders?status=CALCULATED&page=0&size=10&sort=createdAt,desc"),
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders("corr-list")),
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().page());
        assertEquals(10, response.getBody().size());
        assertEquals(2, response.getBody().totalElements());
        assertEquals(1, response.getBody().totalPages());
        assertEquals(2, response.getBody().content().size());
    }

    @Test
    @Timeout(20)
    void concurrentIdempotencyShouldCreateOnlyOneRow() throws Exception {
        String externalOrderId = "A-CONCURRENT-1";
        CreateOrderRequest request = buildRequest(externalOrderId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<ResponseEntity<OrderResponse>> f1 = executor.submit(concurrentPostCall(request, "corr-c1", ready, start));
        Future<ResponseEntity<OrderResponse>> f2 = executor.submit(concurrentPostCall(request, "corr-c2", ready, start));

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        ResponseEntity<OrderResponse> r1 = f1.get(10, TimeUnit.SECONDS);
        ResponseEntity<OrderResponse> r2 = f2.get(10, TimeUnit.SECONDS);

        assertNotNull(r1.getBody());
        assertNotNull(r2.getBody());
        assertEquals(r1.getBody().id(), r2.getBody().id());

        // One should be CREATED and the other OK (idempotent).
        assertTrue(r1.getStatusCode() == HttpStatus.CREATED || r2.getStatusCode() == HttpStatus.CREATED);
        assertFalse(r1.getStatusCode() == HttpStatus.CREATED && r2.getStatusCode() == HttpStatus.CREATED);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE external_order_id = ?",
                Integer.class,
                externalOrderId
        );
        assertEquals(1, count);

        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    private Callable<ResponseEntity<OrderResponse>> concurrentPostCall(CreateOrderRequest request, String correlationId,
                                                                      CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            assertTrue(start.await(5, TimeUnit.SECONDS));
            return restTemplate.exchange(
                    url("/orders"),
                    HttpMethod.POST,
                    new HttpEntity<>(request, jsonHeaders(correlationId)),
                    OrderResponse.class
            );
        };
    }

    private UUID createOrder(String externalOrderId) {
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                url("/orders"),
                HttpMethod.POST,
                new HttpEntity<>(buildRequest(externalOrderId), jsonHeaders("corr-create")),
                OrderResponse.class
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody().id();
    }

    private CreateOrderRequest buildRequest(String externalOrderId) {
        return new CreateOrderRequest(
                externalOrderId,
                List.of(
                        new CreateOrderItemRequest("SKU-1", 2, new BigDecimal("10.50")),
                        new CreateOrderItemRequest("SKU-2", 1, new BigDecimal("5.00"))
                )
        );
    }

    private HttpHeaders jsonHeaders(String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-Correlation-Id", correlationId);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
