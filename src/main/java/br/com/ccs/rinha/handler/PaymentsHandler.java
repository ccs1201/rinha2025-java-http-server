package br.com.ccs.rinha.handler;

import br.com.ccs.rinha.workers.PaymentProcessorWorker;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class PaymentsHandler implements HttpHandler {

    private final PaymentProcessorWorker paymentProcessorWorker = PaymentProcessorWorker.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var requestBody = exchange.getRequestBody().readAllBytes();
        HandlerUtil.sendEmptyResponse(exchange);
        paymentProcessorWorker.offer(requestBody);
    }
}