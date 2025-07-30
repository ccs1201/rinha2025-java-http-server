package br.com.ccs.rinha.model.output;

import java.math.BigDecimal;

public record PaymentSummary(Summary _default, Summary fallback) {

    public record Summary(long totalRequests, BigDecimal totalAmount) {
        public String toJson() {
            return new StringBuilder(64)
                    .append("{\"totalRequests\":").append(totalRequests)
                    .append(",\"totalAmount\":").append(totalAmount).append("}")
                    .toString();
        }
    }

    public String toJson() {
        return new StringBuilder(128)
                .append("{\"default\":").append(_default.toJson())
                .append(",\"fallback\":").append(fallback.toJson())
                .append("}")
                .toString();
    }
}
