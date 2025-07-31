package br.com.ccs.rinha.model.input.builder;

import br.com.ccs.rinha.model.input.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public final class PaymentRequestBuilder {

    private static final AtomicLong lastNanos = new AtomicLong();
    private static final Logger log = LoggerFactory.getLogger(PaymentRequestBuilder.class);

    private PaymentRequestBuilder() {
    }

    public static PaymentRequest fromBytes(byte[] jsonBytes) {
        final String json = new String(jsonBytes, StandardCharsets.UTF_8);
        final char[] chars = json.toCharArray();

        try {
            var req = new PaymentRequest();

            int idx = json.indexOf("\"correlationId\":\"") + 17;
            int end = json.indexOf('"', idx);
            req.correlationId = json.substring(idx, end);

            idx = json.indexOf("\"amount\":", end) + 9;
            while (chars[idx] == ' ' || chars[idx] == '\t') idx++;

            int startAmount = idx;
            while ((chars[idx] >= '0' && chars[idx] <= '9') || chars[idx] == '.' || chars[idx] == '-') idx++;

            var amountStr = json.substring(startAmount, idx);
            req.amount = new BigDecimal(amountStr)
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();

            req.requestedAt = uniqueEpochNanos();
            req.setDefaultFalse();

            // Gera cache JSON
            toJson(req);

            return req;

        } catch (Exception e) {
            log.error("Erro ao construir PaymentRequest {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao construir PaymentRequest", e);
        }
    }

    private static void toJson(PaymentRequest req) {
        var sb = new StringBuilder(128);
        req.json = sb.append("{")
                .append("\"correlationId\":\"").append(req.correlationId).append("\",")
                .append("\"amount\":").append(req.amount / 100f).append(",")
                .append("\"requestedAt\":\"")
                .append(fromEpochNanos(req.requestedAt)).append("\"")
                .append("}")
                .toString();
    }

    public static long toEpochNanos(Instant instant) {
        return instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
    }


    public static Instant fromEpochNanos(long nanos) {
        long seconds = nanos / 1_000_000_000L;
        int nanoAdjustment = (int) (nanos % 1_000_000_000L);
        return Instant.ofEpochSecond(seconds, nanoAdjustment);
    }

    public static long uniqueEpochNanos() {
        long now = toEpochNanos(Instant.now());
        while (true) {
            long prev = lastNanos.get();
            long next = Math.max(now, prev + 1);
            if (lastNanos.compareAndSet(prev, next)) return next;
        }
    }

}
