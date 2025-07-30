package br.com.ccs.rinha.handler;

import br.com.ccs.rinha.model.input.PaymentRequest;
import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import br.com.ccs.rinha.service.PaymentProcessorClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PaymentsHandler implements HttpHandler {

    private final JdbcPaymentRepository repository = JdbcPaymentRepository.getInstance();
    private final PaymentProcessorClient client = PaymentProcessorClient.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var paymentRequest = PaymentRequest.parse(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        HandlerUtil.sendEmptyResponse(exchange);
        client.processPayment(paymentRequest);
    }
}