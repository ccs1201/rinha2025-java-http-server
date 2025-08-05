package br.com.ccs.rinha.service;

import br.com.ccs.rinha.model.input.PaymentRequest;
import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import br.com.ccs.rinha.workers.PaymentsQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class PaymentProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorClient.class);

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json";

    private final JdbcPaymentRepository repository;
    private final HttpClient httpClient;
    private static final PaymentProcessorClient instance;
    private final URI defaultUri;
    private final URI fallbackUri;
    private final Duration timeOut;

    static {
        instance = new PaymentProcessorClient();
    }

    public static PaymentProcessorClient getInstance() {
        return instance;
    }

    private PaymentProcessorClient() {
        var defaultUrl = System.getenv("PAYMENT_PROCESSOR_DEFAULT_URL").trim();
        defaultUrl = defaultUrl.concat("/payments");

        var fallbackUrl = System.getenv("PAYMENT_PROCESSOR_FALLBACK_URL").trim();
        fallbackUrl = fallbackUrl.concat("/payments");
        var tOut = Integer.parseInt(System.getenv("PAYMENT_PROCESSOR_REQUEST_TIMEOUT"));
        var connTout = Integer.parseInt(System.getenv("PAYMENT_PROCESSOR_CONNECTION_TIMEOUT"));

        this.timeOut = Duration.ofMillis(tOut);

        this.repository = JdbcPaymentRepository.getInstance();

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connTout))
                .build();

        defaultUri = URI.create(defaultUrl);
        fallbackUri = URI.create(fallbackUrl);


        log.info("Default service URL: {} ", defaultUrl);
        log.info("Fallback fallback URL: {}", fallbackUrl);

        log.info("Timeout: {}", timeOut);
    }

    public void processPayment(PaymentRequest paymentRequest) {
        try {
            postToDefault(paymentRequest);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void postToDefault(PaymentRequest paymentRequest) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(defaultUri)
                .header(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .timeout(timeOut)
                .POST(HttpRequest.BodyPublishers.ofByteArray(paymentRequest.getPostData()))
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 200) {
            save(paymentRequest.parseToDefault());
            return;
        }

        PaymentsQueue.requeue(paymentRequest);
    }

    private void postToFallback(PaymentRequest paymentRequest) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(fallbackUri)
                .header(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .timeout(timeOut)
                .POST(HttpRequest.BodyPublishers.ofByteArray(paymentRequest.getPostData()))
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            PaymentsQueue.requeue(paymentRequest);
            return;
        }
        save(paymentRequest.parseToFallback());
    }

    private void save(PaymentRequest paymentRequest) {
        repository.save(paymentRequest);
    }
}