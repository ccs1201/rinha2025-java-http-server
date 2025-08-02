package br.com.ccs.rinha.model.input;

import java.math.BigDecimal;
import java.time.Instant;

public final class PaymentRequest {
    public String correlationId;
    public BigDecimal amount;
    public long requestedAt;
    public boolean isDefault;

    private String json;

    public void setDefaultFalse() {
        this.isDefault = false;
    }

    public void setDefaultTrue() {
        this.isDefault = true;
    }

    public String getJson() {
        if (json == null) {
            toJson();
        }
        return json;
    }

    public void resetJson(){
        requestedAt = System.currentTimeMillis();
        toJson();
    }

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
//        req.requestedAt = System.currentTimeMillis();
        req.setDefaultFalse();

        return req;
    }

    private void toJson() {
        var sb = new StringBuilder(128);
        json = sb.append("{")
                .append("\"correlationId\":\"").append(correlationId).append("\",")
                .append("\"amount\":").append(amount).append(",")
                .append("\"requestedAt\":\"").append(Instant.ofEpochMilli(requestedAt)).append("\"")
                .append("}")
                .toString();
    }
}