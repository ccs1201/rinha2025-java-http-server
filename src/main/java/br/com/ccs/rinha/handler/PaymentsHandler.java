package br.com.ccs.rinha.handler;

import br.com.ccs.rinha.workers.PaymentsQueue;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class PaymentsHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var requestBody = exchange.getRequestBody().readAllBytes();
        HandlerUtil.sendEmptyResponse(exchange);
        PaymentsQueue.offer(requestBody);
    }
}