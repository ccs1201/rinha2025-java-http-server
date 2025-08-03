package br.com.ccs.rinha.handler;

import io.undertow.server.HttpHandler;

public class PathHandler {

    private final HttpHandler handlers;

    public PathHandler() {
        this.handlers = configureHandlers();
    }

    private HttpHandler configureHandlers() {
        return new io.undertow.server.handlers.PathHandler()
                .addExactPath("/payments", new PaymentsHandler())
                .addExactPath("/payments-summary", new PaymentsSummaryHandler())
                .addExactPath("/purge-payments", new PurgeHandler());
    }

    public HttpHandler getHandlers() {
        return handlers;
    }
}
