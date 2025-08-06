package br.com.ccs.rinha.handler;

import br.com.ccs.rinha.model.input.PaymentRequest;
import br.com.ccs.rinha.service.PaymentProcessorClient;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.nio.ByteBuffer;

public class PaymentsHandler implements HttpHandler {

    private final ByteBuffer buffer = ByteBuffer.allocateDirect(0);
    private static final PaymentProcessorClient CLIENT = PaymentProcessorClient.getInstance();

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.getRequestReceiver().receiveFullBytes((ex, data) -> {
            exchange.setStatusCode(202);
            exchange.getResponseSender().send(buffer);
            CLIENT.processPayment(new PaymentRequest(data));
        });
    }
}