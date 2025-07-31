package br.com.ccs.rinha.handler;

import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import br.com.ccs.rinha.workers.PaymentProcessorWorker;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class PaymentsHandler implements HttpHandler {

    private final JdbcPaymentRepository repository = JdbcPaymentRepository.getInstance();
    private final PaymentProcessorWorker paymentProcessorWorker = PaymentProcessorWorker.getInstace();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var requestBody = exchange.getRequestBody().readAllBytes();
        HandlerUtil.sendEmptyResponse(exchange);
        paymentProcessorWorker.offer(requestBody);
    }
}