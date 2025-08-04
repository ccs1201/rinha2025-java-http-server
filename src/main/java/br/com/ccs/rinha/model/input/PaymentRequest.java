package br.com.ccs.rinha.model.input;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public final class PaymentRequest {
    public String correlationId;
    public BigDecimal amount;
    public long requestedAt;
    public boolean isDefault;
//    private String json;

    public void setDefaultFalse() {
        this.isDefault = false;
    }

    public void setDefaultTrue() {
        this.isDefault = true;
    }

//    public String getJson() {
//        if (json == null) {
//            toJson();
//        }
//        return json;
//    }

//    public void resetJson(){
//        requestedAt = System.currentTimeMillis();
//        toJson();
//    }

    public static PaymentRequest parse(String json) {
        PaymentRequest req = new PaymentRequest();
        var chars = json.toCharArray();

        int idx = json.indexOf("\"correlationId\":\"") + 17;
        int end = json.indexOf('"', idx);
        req.correlationId = json.substring(idx, end);

        idx = json.indexOf("\"amount\":", end) + 9;
        while (chars[idx] == ' ' || chars[idx] == '\t') idx++;

        int startAmount = idx;
        while ((chars[idx] >= '0' && chars[idx] <= '9') || chars[idx] == '.' || chars[idx] == '-') idx++;

        req.amount = new BigDecimal(json.substring(startAmount, idx));

        req.requestedAt = Instant.parse(json.substring(16, json.lastIndexOf("Z") + 1)).toEpochMilli();

//        req.requestedAt = System.currentTimeMillis();

        return req;
    }

//    private void toJson() {
//        var sb = new StringBuilder(128);
//        json = sb.append("{")
//                .append("\"correlationId\":\"").append(correlationId).append("\",")
//                .append("\"amount\":").append(amount).append(",")
//                .append("\"requestedAt\":\"").append(Instant.ofEpochMilli(requestedAt)).append("\"")
//                .append("}")
//                .toString();
//    }

    public static PaymentRequest parseToDefault(byte[] paymentRequestBytes) {
        var paymentRequest = parse(new String(paymentRequestBytes, StandardCharsets.UTF_8));
        paymentRequest.setDefaultTrue();
        return paymentRequest;
    }

    public static PaymentRequest parseToFallback(byte[] paymentRequestBytes) {
        var paymentRequest = parse(new String(paymentRequestBytes, StandardCharsets.UTF_8));
        paymentRequest.setDefaultFalse();
        return paymentRequest;
    }

    private static final String REQUESTED_AT = "{\"requestedAt\":\"";

    public static byte[] addRequestedAtToJsonBytes(byte[] jsonBytes) {
        var requestedAt = (REQUESTED_AT +
                Instant.now().toString() +
                "\",")
                .getBytes(StandardCharsets.UTF_8);

        byte[] newJsonBytes = new byte[jsonBytes.length + 47];

        System.arraycopy(requestedAt, 0, newJsonBytes, 0, requestedAt.length);
        System.arraycopy(jsonBytes, 1, newJsonBytes, requestedAt.length, jsonBytes.length - 1);

        return newJsonBytes;
    }
}