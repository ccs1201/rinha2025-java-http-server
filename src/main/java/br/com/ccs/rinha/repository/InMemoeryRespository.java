package br.com.ccs.rinha.repository;

import br.com.ccs.rinha.model.input.PaymentRequest;
import br.com.ccs.rinha.model.output.PaymentSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;

import static br.com.ccs.rinha.model.input.builder.PaymentRequestBuilder.toEpochNanos;

public class InMemoeryRespository {

    private static final InMemoeryRespository instance = new InMemoeryRespository();
    private static final Logger log = LoggerFactory.getLogger(InMemoeryRespository.class);
    private final ConcurrentSkipListMap<Long, PaymentRequest> defaults = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<Long, PaymentRequest> fallbacks = new ConcurrentSkipListMap<>();
    private static String SUMMARY_URL;
    private static final HttpClient HTTP_CLIENT;

    static {
        var SUMMARY_URL_PATTERN = "http://%s:9999/internal-summary";
        if (System.getenv("INSTANCE_ID").equals("1")) {
            SUMMARY_URL = SUMMARY_URL_PATTERN.formatted("localhost").concat("?from=%s&to=%s");
        } else {
            SUMMARY_URL = SUMMARY_URL_PATTERN.formatted("backend-api1").concat("?from=%s&to=%s");
        }

        HTTP_CLIENT = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofMillis(200))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();

        log.info("Instace ID: {}", System.getenv("INSTANCE_ID"));
        log.info("Summary URL: {}", SUMMARY_URL);
    }

    public void save(PaymentRequest paymentRequest) {
        if (!paymentRequest.isDefault) {
            if (fallbacks.put(paymentRequest.requestedAt, paymentRequest) != null) {
                log.error("Colisão ao inserir na lista de fallback: ", paymentRequest.requestedAt);
            }
            return;
        }

        if (defaults.put(paymentRequest.requestedAt, paymentRequest) != null) {
            log.error("Colisao ao inserir na lista default: {}", paymentRequest.requestedAt);
        }
    }

    public static InMemoeryRespository getInstance() {
        return instance;
    }

    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {

        var call = CompletableFuture.supplyAsync(() -> callOther(from, to));

        var defaultSummary = computeDefaultSummary(from, to);
        var fallbackSummary = computeFallbacktSummary(from, to);
        var localSummary = new PaymentSummary(defaultSummary, fallbackSummary);

        var otherInstanceSummary = call.join();

        return PaymentSummary.agregate(localSummary, otherInstanceSummary);
    }

    private PaymentSummary.Summary computeDefaultSummary(OffsetDateTime from, OffsetDateTime to) {
        var range = defaults
                .subMap(toEpochNanos(from.toInstant()), true,
                        toEpochNanos(to.toInstant()), true);
        return computeSummary(range);
    }

    private PaymentSummary.Summary computeFallbacktSummary(OffsetDateTime from, OffsetDateTime to) {
        var range = fallbacks
                .subMap(toEpochNanos(from.toInstant()), true,
                        toEpochNanos(to.toInstant()), true);
        return computeSummary(range);
    }

    private static PaymentSummary.Summary computeSummary(ConcurrentNavigableMap<Long, PaymentRequest> range) {
        var amount = 0;
        var requests = 0;

        for (PaymentRequest p : range.values()) {
            amount += p.amount;
            requests++;
        }

        return new PaymentSummary.Summary(requests, new BigDecimal(amount).movePointLeft(2));
    }

    private PaymentSummary callOther(OffsetDateTime from, OffsetDateTime to) {

        var request = HttpRequest.newBuilder()
                .uri(URI.create(SUMMARY_URL.formatted(from, to)))
                .GET()
                .build();

        return HTTP_CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(PaymentSummary::fromJson)
                .join();
    }

    public void purge() {
        defaults.clear();
        fallbacks.clear();
    }

    public PaymentSummary getSummaryInternal(OffsetDateTime from, OffsetDateTime to) {
        var defaultSummary = computeDefaultSummary(from, to);
        var fallbackSummary = computeFallbacktSummary(from, to);
        return new PaymentSummary(defaultSummary, fallbackSummary);
    }
}
