package br.com.ccs.rinha;

import br.com.ccs.rinha.config.DataSourceFactory;
import br.com.ccs.rinha.config.ExecutorConfig;
import br.com.ccs.rinha.handler.PaymentsHandler;
import br.com.ccs.rinha.handler.PaymentsSummaryHandler;
import br.com.ccs.rinha.handler.PurgeHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        var executor = ExecutorConfig.getExecutor();
        server.setExecutor(executor);

        server.createContext("/payments", new PaymentsHandler());
        server.createContext("/payments-summary", new PaymentsSummaryHandler());
        server.createContext("/purge-payments", new PurgeHandler());

        server.start();
        log.info("Servidor iniciado na porta: {} ", port);

        // Adiciona um hook de shutdown para fechar o pool de conexões e o executor
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Desligando servidor...");
            server.stop(0); // Para o servidor imediatamente
            if (executor != null) {
                executor.shutdownNow(); // Encerra o executor das threads
            }

            DataSourceFactory.close();

            log.info("Servidor desligado.");
        }));
    }
}