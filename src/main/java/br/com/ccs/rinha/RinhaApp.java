package br.com.ccs.rinha;

import br.com.ccs.rinha.handler.PathHandler;
import io.undertow.Undertow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class RinhaApp {

    private static final Logger log = LoggerFactory.getLogger(RinhaApp.class);

    public static void main(String[] args) {
        var envPort = System.getenv("SERVER_PORT");
        var serverIOThreads = Integer.parseInt(System.getenv("SERVER_IO_THREADS"));
        var serverWorkerThreads = Integer.parseInt(System.getenv("SERVER_WORKER_THREADS"));
        int serverPort = Objects.isNull(envPort) ? 8080 : Integer.parseInt(envPort);

        log.info("Starting server on port {}", serverPort);
        printPromo(1);

        Undertow server = Undertow.builder()
                .addHttpListener(serverPort, "0.0.0.0")
                .setHandler(new PathHandler().getHandlers())
                .setIoThreads(serverIOThreads)
                .setWorkerThreads(serverWorkerThreads)
                .setDirectBuffers(true)
                .setBufferSize(512)
                .build();
        server.start();
        printPromo(1);
        log.info("Server started! Let's play @RinhaDeBackend");
    }

    private static void printPromo(int sleep) {
        var msg = """
                 
                 ##########     (Si vis pacem, para bellum)     ##########
                >>> ccs1201 follow on linkedisney -> https://www.linkedin.com/in/ccs1201/
                >>> follow on  github -> https://github.com/ccs1201
                """;
        System.out.println(msg);
        try {
            Thread.sleep(sleep * 1000L);
        } catch (Exception e) {
            log.error("Isto realmente não deveria acontece :(", e);
        }
    }

}
