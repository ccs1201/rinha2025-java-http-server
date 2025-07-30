package br.com.ccs.rinha.handler;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HandlerUtil {

    private HandlerUtil() {
    }

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final int STATUS_CODE = 200;

    public static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().set(CONTENT_TYPE, APPLICATION_JSON);
        exchange.sendResponseHeaders(STATUS_CODE, response.getBytes().length);
        try (exchange; OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void sendEmptyResponse(HttpExchange exchange) throws IOException {
        try (exchange) {
            exchange.sendResponseHeaders(STATUS_CODE, 0);
        }
    }
}