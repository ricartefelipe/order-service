package com.example.order.integration;

import com.example.order.adapters.web.model.CreateOrderItemRequest;
import com.example.order.adapters.web.model.CreateOrderRequest;
import com.example.order.adapters.web.model.OrderResponse;
import com.example.order.adapters.web.model.PagedResponse;
import com.example.order.config.error.ApiErrorResponse;
import com.example.order.support.ClientTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
class OrderControllerIntegrationTest {

    private static final String ORIGIN_A = "system-a";
    private static final String ORIGIN_B = "system-b";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("orders")
            .withUsername("orders")
            .withPassword("orders");

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM orders");
    }

    @Test
    void createShouldReturn201AndCalculateTotals() {
        ResponseEntity<OrderResponse> response = post(buildRequest("A-1"), ORIGIN_A, ClientTokens.SCOPE_CREATE);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        OrderResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(ORIGIN_A, body.origin());
        assertEquals("BRL", body.currency());
        assertEquals("CALCULATED", body.status());
        assertEquals(new BigDecimal("26.00"), body.totalAmount());
        assertEquals(new BigDecimal("21.00"), body.items().get(0).lineTotal());
        assertEquals("/orders/" + body.id(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    void sameExternalIdAndSamePayloadReplaysTheOriginalOrder() {
        ResponseEntity<OrderResponse> first = post(buildRequest("A-REPLAY"), ORIGIN_A, ClientTokens.SCOPE_CREATE);
        CreateOrderRequest reordered = new CreateOrderRequest("A-REPLAY", null, List.of(
                new CreateOrderItemRequest("SKU-2", 1, new BigDecimal("5")),
                new CreateOrderItemRequest("SKU-1", 2, new BigDecimal("10.50"))));

        ResponseEntity<OrderResponse> second = post(reordered, ORIGIN_A, ClientTokens.SCOPE_CREATE);

        assertEquals(HttpStatus.CREATED, first.getStatusCode());
        assertEquals(HttpStatus.OK, second.getStatusCode());
        assertNotNull(first.getBody());
        assertNotNull(second.getBody());
        assertEquals(first.getBody().id(), second.getBody().id());
        assertEquals(first.getBody().createdAt(), second.getBody().createdAt());
        assertEquals(1, countOrders("A-REPLAY"));
    }

    @Test
    void sameExternalIdWithDifferentPayloadConflictsWithoutChangingTheOrder() {
        ResponseEntity<OrderResponse> first = post(buildRequest("A-CONFLICT"), ORIGIN_A, ClientTokens.SCOPE_CREATE);
        CreateOrderRequest changed = new CreateOrderRequest("A-CONFLICT", null, List.of(
                new CreateOrderItemRequest("SKU-1", 3, new BigDecimal("10.50"))));

        ResponseEntity<ApiErrorResponse> conflict = exchange("/orders", HttpMethod.POST, changed,
                ClientTokens.token(ORIGIN_A, ClientTokens.SCOPE_CREATE), ApiErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertNotNull(conflict.getBody());
        assertEquals(409, conflict.getBody().status());
        assertNotNull(first.getBody());
        BigDecimal stored = jdbcTemplate.queryForObject(
                "SELECT total_amount FROM orders WHERE id = ?", BigDecimal.class, first.getBody().id());
        assertEquals(new BigDecimal("26.00"), stored);
        assertEquals(1, countOrders("A-CONFLICT"));
    }

    @Test
    void differentCurrencyIsADifferentPayload() {
        post(buildRequest("A-CUR"), ORIGIN_A, ClientTokens.SCOPE_CREATE);
        CreateOrderRequest usd = new CreateOrderRequest("A-CUR", "USD", buildRequest("A-CUR").items());

        ResponseEntity<ApiErrorResponse> conflict = exchange("/orders", HttpMethod.POST, usd,
                ClientTokens.token(ORIGIN_A, ClientTokens.SCOPE_CREATE), ApiErrorResponse.class);

        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
    }

    @Test
    void twoOriginsMayUseTheSameExternalIdWithoutCollidingOrLeaking() {
        ResponseEntity<OrderResponse> a = post(buildRequest("SHARED-1"), ORIGIN_A, ClientTokens.SCOPE_CREATE);
        ResponseEntity<OrderResponse> b = post(buildRequest("SHARED-1"), ORIGIN_B, ClientTokens.SCOPE_CREATE);

        assertEquals(HttpStatus.CREATED, a.getStatusCode());
        assertEquals(HttpStatus.CREATED, b.getStatusCode());
        assertNotNull(a.getBody());
        assertNotNull(b.getBody());
        assertFalse(a.getBody().id().equals(b.getBody().id()));
        assertEquals(2, countOrders("SHARED-1"));

        ResponseEntity<ApiErrorResponse> crossRead = exchange("/orders/" + a.getBody().id(), HttpMethod.GET, null,
                ClientTokens.token(ORIGIN_B, ClientTokens.SCOPE_READ), ApiErrorResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, crossRead.getStatusCode());

        ResponseEntity<PagedResponse<OrderResponse>> listB = list("?page=0&size=10", ORIGIN_B);
        assertNotNull(listB.getBody());
        assertEquals(1, listB.getBody().totalElements());
        assertEquals(b.getBody().id(), listB.getBody().content().get(0).id());
    }

    @Test
    void lookupByExternalOrderIdIsScopedToTheOrigin() {
        post(buildRequest("REF-1"), ORIGIN_A, ClientTokens.SCOPE_CREATE);
        post(buildRequest("REF-2"), ORIGIN_A, ClientTokens.SCOPE_CREATE);

        ResponseEntity<PagedResponse<OrderResponse>> found = list("?externalOrderId=REF-1", ORIGIN_A);
        ResponseEntity<PagedResponse<OrderResponse>> other = list("?externalOrderId=REF-1", ORIGIN_B);

        assertNotNull(found.getBody());
        assertEquals(1, found.getBody().totalElements());
        assertEquals("REF-1", found.getBody().content().get(0).externalOrderId());
        assertNotNull(other.getBody());
        assertEquals(0, other.getBody().totalElements());
    }

    @Test
    void requestsWithoutTokenAreRejectedWith401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<ApiErrorResponse> create = restTemplate.exchange(url("/orders"), HttpMethod.POST,
                new HttpEntity<>(buildRequest("NO-TOKEN"), headers), ApiErrorResponse.class);
        ResponseEntity<ApiErrorResponse> read = restTemplate.exchange(url("/orders?page=0&size=1"), HttpMethod.GET,
                new HttpEntity<>(headers), ApiErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, create.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, read.getStatusCode());
        assertNotNull(create.getBody());
        assertEquals(401, create.getBody().status());
        assertEquals(0, countOrders("NO-TOKEN"));
    }

    @Test
    void expiredOrWrongAudienceTokensAreRejectedWith401() {
        ResponseEntity<ApiErrorResponse> expired = exchange("/orders?page=0&size=1", HttpMethod.GET, null,
                ClientTokens.expiredToken(ORIGIN_A, ClientTokens.SCOPE_READ), ApiErrorResponse.class);
        ResponseEntity<ApiErrorResponse> wrongAudience = exchange("/orders?page=0&size=1", HttpMethod.GET, null,
                ClientTokens.tokenForAudience(ORIGIN_A, "another-service", ClientTokens.SCOPE_READ), ApiErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, expired.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, wrongAudience.getStatusCode());
    }

    @Test
    void tokenWithoutTheRequiredScopeIsRejectedWith403() {
        ResponseEntity<ApiErrorResponse> createWithReadOnly = exchange("/orders", HttpMethod.POST, buildRequest("SCOPE-1"),
                ClientTokens.token(ORIGIN_A, ClientTokens.SCOPE_READ), ApiErrorResponse.class);
        ResponseEntity<ApiErrorResponse> readWithCreateOnly = exchange("/orders?page=0&size=1", HttpMethod.GET, null,
                ClientTokens.token(ORIGIN_A, ClientTokens.SCOPE_CREATE), ApiErrorResponse.class);

        assertEquals(HttpStatus.FORBIDDEN, createWithReadOnly.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, readWithCreateOnly.getStatusCode());
        assertEquals(0, countOrders("SCOPE-1"));
    }

    @Test
    void validationRejectsZeroQuantityNegativePriceUnknownCurrencyAndOverflow() {
        assertEquals(HttpStatus.BAD_REQUEST, postError(new CreateOrderRequest("V-1", null, List.of(
                new CreateOrderItemRequest("SKU-1", 0, new BigDecimal("1.00"))))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, postError(new CreateOrderRequest("V-2", null, List.of(
                new CreateOrderItemRequest("SKU-1", 1, new BigDecimal("-1.00"))))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, postError(new CreateOrderRequest("V-3", "ZZZ", List.of(
                new CreateOrderItemRequest("SKU-1", 1, new BigDecimal("1.00"))))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, postError(new CreateOrderRequest("V-4", null, List.of(
                new CreateOrderItemRequest("SKU-1", 1_000_000, new BigDecimal("99999999999999999.99"))))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, postError(new CreateOrderRequest("V-5", null, List.of(
                new CreateOrderItemRequest("SKU-1", 1, new BigDecimal("1.005"))))).getStatusCode());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class));
    }

    @Test
    void storedFingerprintMatchesTheSqlFormOfChangeset002() {
        ResponseEntity<OrderResponse> created = post(buildRequest("FP-1"), ORIGIN_A, ClientTokens.SCOPE_CREATE);
        assertNotNull(created.getBody());
        UUID id = created.getBody().id();

        String stored = jdbcTemplate.queryForObject("SELECT fingerprint FROM orders WHERE id = ?", String.class, id);
        String sql = jdbcTemplate.queryForObject("""
                SELECT encode(sha256(convert_to(
                  'v1' || E'\\n' || o.currency || E'\\n' ||
                  (SELECT string_agg(line, E'\\n' ORDER BY line COLLATE "C") FROM (
                     SELECT i.product_id || E'\\t' || i.quantity || E'\\t' ||
                            to_char(i.unit_price, 'FM99999999999999990.00') AS line
                     FROM order_items i WHERE i.order_id = o.id) lines),
                  'UTF8')), 'hex')
                FROM orders o WHERE o.id = ?
                """, String.class, id);

        assertEquals(sql, stored);
    }

    @Test
    void getPagedByStatusShouldReturnMetadata() {
        post(buildRequest("A-LIST-1"), ORIGIN_A, ClientTokens.SCOPE_CREATE);
        post(buildRequest("A-LIST-2"), ORIGIN_A, ClientTokens.SCOPE_CREATE);

        ResponseEntity<PagedResponse<OrderResponse>> response =
                list("?status=CALCULATED&page=0&size=10&sort=createdAt,desc", ORIGIN_A);

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
        String token = ClientTokens.token(ORIGIN_A, ClientTokens.SCOPE_CREATE);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<ResponseEntity<OrderResponse>> f1 = executor.submit(concurrentPostCall(request, token, ready, start));
        Future<ResponseEntity<OrderResponse>> f2 = executor.submit(concurrentPostCall(request, token, ready, start));

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        ResponseEntity<OrderResponse> r1 = f1.get(10, TimeUnit.SECONDS);
        ResponseEntity<OrderResponse> r2 = f2.get(10, TimeUnit.SECONDS);

        assertNotNull(r1.getBody());
        assertNotNull(r2.getBody());
        assertEquals(r1.getBody().id(), r2.getBody().id());
        assertTrue(r1.getStatusCode() == HttpStatus.CREATED || r2.getStatusCode() == HttpStatus.CREATED);
        assertFalse(r1.getStatusCode() == HttpStatus.CREATED && r2.getStatusCode() == HttpStatus.CREATED);
        assertEquals(1, countOrders(externalOrderId));

        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    private Callable<ResponseEntity<OrderResponse>> concurrentPostCall(CreateOrderRequest request, String token,
                                                                      CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            assertTrue(start.await(5, TimeUnit.SECONDS));
            return exchange("/orders", HttpMethod.POST, request, token, OrderResponse.class);
        };
    }

    private ResponseEntity<OrderResponse> post(CreateOrderRequest request, String origin, String... scopes) {
        return exchange("/orders", HttpMethod.POST, request, ClientTokens.token(origin, scopes), OrderResponse.class);
    }

    private ResponseEntity<ApiErrorResponse> postError(CreateOrderRequest request) {
        return exchange("/orders", HttpMethod.POST, request,
                ClientTokens.token(ORIGIN_A, ClientTokens.SCOPE_CREATE), ApiErrorResponse.class);
    }

    private ResponseEntity<PagedResponse<OrderResponse>> list(String query, String origin) {
        return restTemplate.exchange(url("/orders" + query), HttpMethod.GET,
                new HttpEntity<>(headers(ClientTokens.token(origin, ClientTokens.SCOPE_READ))),
                new ParameterizedTypeReference<>() {
                });
    }

    private <T> ResponseEntity<T> exchange(String path, HttpMethod method, Object body, String token, Class<T> type) {
        return restTemplate.exchange(url(path), method, new HttpEntity<>(body, headers(token)), type);
    }

    private int countOrders(String externalOrderId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE external_order_id = ?", Integer.class, externalOrderId);
        return count == null ? 0 : count;
    }

    private static CreateOrderRequest buildRequest(String externalOrderId) {
        return new CreateOrderRequest(
                externalOrderId,
                null,
                List.of(
                        new CreateOrderItemRequest("SKU-1", 2, new BigDecimal("10.50")),
                        new CreateOrderItemRequest("SKU-2", 1, new BigDecimal("5.00"))
                )
        );
    }

    private static HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);
        headers.set("X-Correlation-Id", "corr-" + UUID.randomUUID());
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
