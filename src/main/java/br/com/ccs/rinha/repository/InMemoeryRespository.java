package br.com.ccs.rinha.repository;

import br.com.ccs.rinha.model.input.PaymentRequest;
import br.com.ccs.rinha.model.output.PaymentSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static br.com.ccs.rinha.model.input.builder.PaymentRequestBuilder.toEpochNanos;

public class InMemoeryRespository {

    private static final InMemoeryRespository instance = new InMemoeryRespository();
    private static final Logger log = LoggerFactory.getLogger(InMemoeryRespository.class);
    private final ConcurrentSkipListMap<Long, PaymentRequest> defaults = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<Long, PaymentRequest> fallbacks = new ConcurrentSkipListMap<>();

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
        var defaultSummary = computeDefaultSummary(from, to);
        var fallbackSummary = computeFallbacktSummary(from, to);

        return new PaymentSummary(defaultSummary, fallbackSummary);
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

        //todo chamar outra api

        return null;
    }

    public void purge() {
        defaults.clear();
        fallbacks.clear();
    }

}
