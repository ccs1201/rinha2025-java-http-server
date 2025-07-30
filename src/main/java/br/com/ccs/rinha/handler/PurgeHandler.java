package br.com.ccs.rinha.handler;

import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class PurgeHandler extends BaseHandler {

    JdbcPaymentRepository repository = JdbcPaymentRepository.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        repository.purge();
        sendEmptyResponse(exchange);
    }
}
