package br.com.ccs.rinha.handler;

import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

public class PaymentsSummaryHandler implements HttpHandler {

    private static final String CONTENT_TYPE = "application/json";
    private static final HttpString CONTENT_TYPE_HEADER = new HttpString("Content-Type");

    private final JdbcPaymentRepository repository = JdbcPaymentRepository.getInstance();


    @Override
    public void handleRequest(HttpServerExchange exchange) throws IOException {
        var from = OffsetDateTime.parse(exchange.getQueryParameters().get("from").getFirst());
        var to = OffsetDateTime.parse(exchange.getQueryParameters().get("to").getFirst());
        exchange.setStatusCode(200);
        exchange.getResponseHeaders().put(CONTENT_TYPE_HEADER, CONTENT_TYPE);
        exchange.getResponseSender()
                .send(ByteBuffer
                        .wrap(repository.getSummary(from, to).toJson().getBytes(StandardCharsets.UTF_8)));
    }
}