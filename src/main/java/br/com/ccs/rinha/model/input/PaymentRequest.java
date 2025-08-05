package br.com.ccs.rinha.model.input;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class PaymentRequest {
    public BigDecimal amount;
    public long requestedAt;
    public boolean isDefault;
    public byte[] requestData;
    private byte[] postData;


    public PaymentRequest(byte[] requestData) {
        this.requestData = requestData;
    }

    public void setDefaultFalse() {
        this.isDefault = false;
    }

    public void setDefaultTrue() {
        this.isDefault = true;
    }

    public PaymentRequest parse(String json) {
        var chars = json.toCharArray();

        int idx = json.indexOf("\"correlationId\":\"") + 17;
        int end = json.indexOf('"', idx);
//        req.correlationId = json.substring(idx, end);

        idx = json.indexOf("\"amount\":", end) + 9;
        while (chars[idx] == ' ' || chars[idx] == '\t') idx++;

        int startAmount = idx;
        while ((chars[idx] >= '0' && chars[idx] <= '9') || chars[idx] == '.' || chars[idx] == '-') idx++;

        this.amount = new BigDecimal(json.substring(startAmount, idx));
        this.requestedAt = Instant.parse(json.substring(16, json.lastIndexOf("Z") + 1)).toEpochMilli();

        return this;
    }

    public PaymentRequest parseToDefault() {
        var paymentRequest = parse(new String(postData, StandardCharsets.UTF_8));
        paymentRequest.setDefaultTrue();
        return paymentRequest;
    }

    public PaymentRequest parseToFallback() {
        var paymentRequest = parse(new String(postData, StandardCharsets.UTF_8));
        paymentRequest.setDefaultFalse();
        return paymentRequest;
    }


    private static final byte[] REQUESTED_AT_PREFIX = "{\"requestedAt\":\"".getBytes(StandardCharsets.UTF_8);
    private static final byte[] REQUESTED_AT_SUFFIX = "\",".getBytes(StandardCharsets.UTF_8);

    private static final ThreadLocal<ByteBuffer> BUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocate(256));
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    public byte[] getPostData() {
        if (postData == null) {
            Instant now = Instant.ofEpochMilli(System.currentTimeMillis());
            var timestamp = FORMATTER.format(now).getBytes(StandardCharsets.UTF_8);

            ByteBuffer buffer = BUFFER.get();
            buffer.clear();

            buffer.put(REQUESTED_AT_PREFIX);
            buffer.put(timestamp);
            buffer.put(REQUESTED_AT_SUFFIX);

            buffer.put(requestData, 1, requestData.length - 1);

            byte[] result = new byte[buffer.position()];
            buffer.flip();
            buffer.get(result);
            postData = result;
        }

        return postData;
    }
}
