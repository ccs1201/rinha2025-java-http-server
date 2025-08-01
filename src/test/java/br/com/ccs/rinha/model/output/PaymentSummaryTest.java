package br.com.ccs.rinha.model.output;

class PaymentSummaryTest {

    public static void main(String[] args) {
        var json = "{\"default\": {\"totalRequests\": 14725,\"totalAmount\": 1987.53},\"fallback\": {\"totalRequests\": 928\"totalAmount\": 3758.89}\n";
        var sumary = PaymentSummary.fromJson(json);

        System.out.println(sumary);
    }

}