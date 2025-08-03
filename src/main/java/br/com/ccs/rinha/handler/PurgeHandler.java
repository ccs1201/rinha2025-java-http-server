package br.com.ccs.rinha.handler;

import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class PurgeHandler implements HttpHandler {

    JdbcPaymentRepository repository = JdbcPaymentRepository.getInstance();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        repository.purge();
        exchange.setStatusCode(200);
        exchange.getResponseSender().send("");
    }
}
