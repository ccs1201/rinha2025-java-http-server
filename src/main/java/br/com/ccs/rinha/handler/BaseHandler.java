package br.com.ccs.rinha.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseHandler implements HttpHandler {

    private static final String CONTANT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final int STATUS_CODE = 200;

    protected void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().set(CONTANT_TYPE, APPLICATION_JSON);
        exchange.sendResponseHeaders(STATUS_CODE, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected void sendEmptyResponse(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(STATUS_CODE, -1);
    }

    // Método utilitário para extrair o ID da URL
    protected String extractId(URI uri, String basePath) {
        // Ex: /clientes/123/extrato -> id = 123
        // Ex: /clientes/123/transacoes -> id = 123
        Pattern pattern = Pattern.compile(Pattern.quote(basePath) + "(\\d+).*");
        Matcher matcher = pattern.matcher(uri.getPath());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
}