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

//    private void parse(boolean isDefault) {
//        var json = new String(postData, StandardCharsets.UTF_8);
//        int startAmount = json.lastIndexOf(':') + 1;
//        this.amount = new BigDecimal(json.substring(startAmount, json.length() - 1));
//        this.requestedAt = Instant.parse(json.substring(16, json.indexOf('Z') + 1)).toEpochMilli();
//        this.isDefault = isDefault;
//    }

    private void parse(boolean isDefault) {
        var json = new String(postData, StandardCharsets.UTF_8);
        var chars = json.toCharArray();

        int idx = json.indexOf("\"amount\":") + 9;
        while (chars[idx] == ' ' || chars[idx] == '\t') idx++;

        int startAmount = idx;
        while ((chars[idx] >= '0' && chars[idx] <= '9') || chars[idx] == '.' || chars[idx] == '-') idx++;

        this.amount = new BigDecimal(json.substring(startAmount, idx));
        this.requestedAt = Instant.parse(json.substring(16, json.lastIndexOf("Z") + 1)).toEpochMilli();
        this.isDefault = isDefault;
    }

    public PaymentRequest parseToDefault() {
        parse(true);
        return this;
    }

    public PaymentRequest parseToFallback() {
        parse(false);
        return this;
    }


    private static final byte[] REQUESTED_AT_PREFIX = "{\"requestedAt\":\"".getBytes(StandardCharsets.UTF_8);
    private static final byte[] REQUESTED_AT_SUFFIX = "\",".getBytes(StandardCharsets.UTF_8);

    private static final ThreadLocal<ByteBuffer> BUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocate(256));
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC);

    public byte[] getPostData() {
        if (postData == null) {
            ByteBuffer buffer = BUFFER.get().clear();
            buffer.put(REQUESTED_AT_PREFIX);

            Instant now = Instant.ofEpochMilli(System.currentTimeMillis());
            buffer.put(FORMATTER.format(now).getBytes(StandardCharsets.UTF_8));

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
