package br.com.ccs.rinha.handler;

import br.com.ccs.rinha.workers.PaymentsQueue;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class PaymentsHandler implements HttpHandler {

    private static final String EMPTY_RESPONSE = "";

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.getRequestReceiver().receiveFullBytes((ex, data) -> {
            exchange.setStatusCode(202);
            exchange.getResponseSender().send(EMPTY_RESPONSE);
            PaymentsQueue.offer(data);
        });
    }
}