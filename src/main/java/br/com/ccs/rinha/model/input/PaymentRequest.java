package br.com.ccs.rinha.model.input;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

    public PaymentRequest parse(String json) {

//        var start = System.nanoTime();

        int startAmount = json.lastIndexOf(':') + 1;
        this.amount = new BigDecimal(json.substring(startAmount, json.length() - 1));
        this.requestedAt = Instant.parse(json.substring(16, json.indexOf('Z') + 1)).toEpochMilli();

        var end = System.nanoTime();
//        System.out.printf("Parsing %.3f ms%n", (end - start) / 1_000_000.0);
        return this;
    }

    public PaymentRequest parseToDefault() {
        var paymentRequest = parse(new String(postData, StandardCharsets.UTF_8));
        paymentRequest.isDefault = true;
        return paymentRequest;
    }

    public PaymentRequest parseToFallback() {
        var paymentRequest = parse(new String(postData, StandardCharsets.UTF_8));
        paymentRequest.isDefault = false;
        return paymentRequest;
    }


    private static final byte[] REQUESTED_AT_PREFIX = "{\"requestedAt\":\"".getBytes(StandardCharsets.UTF_8);
    private static final byte[] REQUESTED_AT_SUFFIX = "\",".getBytes(StandardCharsets.UTF_8);

    private static final ThreadLocal<ByteBuffer> BUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocate(256));
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public byte[] getPostData() {
        if (postData == null) {

            ByteBuffer buffer = BUFFER.get().clear();
            buffer.put(REQUESTED_AT_PREFIX);

            buffer.put(FORMATTER.format(Instant.ofEpochMilli(System.currentTimeMillis())).getBytes(StandardCharsets.UTF_8));

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
