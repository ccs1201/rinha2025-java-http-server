package br.com.ccs.rinha.model.output;

import java.math.BigDecimal;

public record PaymentSummary(Summary _default, Summary fallback) {

    public static PaymentSummary fromJson(String json) {
        var summaryDefault = json.substring(json.indexOf("{\"default\":") + 11, json.indexOf("}"));
        var summaryFallback = json.substring(json.indexOf(", \"fallback\":") + 14, json.length() - 2);

        return new PaymentSummary(Summary.fromJson(summaryDefault), Summary.fromJson(summaryFallback));
    }

    public static PaymentSummary agregate(PaymentSummary localSummary, PaymentSummary otherInstanceSummary) {
        return new PaymentSummary(createDefaultSummary(localSummary, otherInstanceSummary),
                createFallbackSummary(localSummary, otherInstanceSummary));
    }

    private static Summary createDefaultSummary(PaymentSummary localSummary, PaymentSummary otherInstanceSummary) {
        return new Summary(sumRequests(localSummary._default.totalRequests, otherInstanceSummary._default.totalRequests),
                sumAmounts(localSummary._default.totalAmount, otherInstanceSummary._default.totalAmount));
    }

    private static Summary createFallbackSummary(PaymentSummary localSummary, PaymentSummary otherInstanceSummary) {
        return new Summary(sumRequests(localSummary.fallback.totalRequests, otherInstanceSummary.fallback.totalRequests),
                sumAmounts(localSummary.fallback.totalAmount, otherInstanceSummary.fallback.totalAmount));
    }

    private static BigDecimal sumAmounts(BigDecimal totalAmount, BigDecimal totalAmount1) {
        return totalAmount1.add(totalAmount);
    }

    private static long sumRequests(long totalRequests, long totalRequests1) {
        return totalRequests + totalRequests1;
    }

    public record Summary(long totalRequests, BigDecimal totalAmount) {
        public static Summary fromJson(String json) {
            return new Summary(parseTotalRequests(json), parseTotalAmount(json));
        }

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

    private static int parseTotalRequests(String json) {
        var amount = json.substring(json.indexOf(":") + 1, json.indexOf(","));
        return Integer.parseInt(amount);
    }

    private static BigDecimal parseTotalAmount(String json) {
        return new BigDecimal(json.substring(json.lastIndexOf(":") + 1));
    }
}
