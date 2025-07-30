package br.com.ccs.rinha.handler;

import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class PurgeHandler implements HttpHandler {

    JdbcPaymentRepository repository = JdbcPaymentRepository.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        repository.purge();
        HandlerUtil.sendEmptyResponse(exchange);
    }
}
