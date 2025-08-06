package br.com.ccs.rinha.service;

import br.com.ccs.rinha.model.input.PaymentRequest;
import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import br.com.ccs.rinha.workers.PaymentsQueue;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorClient.class);

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json";

    private final JdbcPaymentRepository repository;
    private final HttpClient httpClient;
    private static final PaymentProcessorClient instance;
    private final URI defaultUri;
    private final URI fallbackUri;
    private final int requestTimeOut;

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
        this.requestTimeOut = Integer.parseInt(System.getenv("PAYMENT_PROCESSOR_REQUEST_TIMEOUT"));
        var connTout = Integer.parseInt(System.getenv("PAYMENT_PROCESSOR_CONNECTION_TIMEOUT"));


        this.repository = JdbcPaymentRepository.getInstance();

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .executor(new ThreadPoolExecutor(10, 10, 0L,
                        TimeUnit.SECONDS, new ArrayBlockingQueue<>(5000), Thread.ofVirtual().factory()))
                .connectTimeout(Duration.ofMillis(connTout))
                .build();

        defaultUri = URI.create(defaultUrl);
        fallbackUri = URI.create(fallbackUrl);


        log.info("Default service URL: {} ", defaultUrl);
        log.info("Fallback fallback URL: {}", fallbackUrl);

        log.info("Timeout: {}", requestTimeOut);
    }

    public void processPayment(PaymentRequest paymentRequest) {
            postToDefault(paymentRequest);
    }

    private void postToDefault(PaymentRequest paymentRequest) {
        var request = HttpRequest.newBuilder()
                .uri(defaultUri)
                .header(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .POST(HttpRequest.BodyPublishers.ofByteArray(paymentRequest.getPostData()))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .orTimeout(requestTimeOut, TimeUnit.MILLISECONDS)
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        save(paymentRequest.parseToDefault());
                    } else {
                        PaymentsQueue.requeue(paymentRequest);
                    }
                })
                .exceptionally(throwable -> {
                    PaymentsQueue.requeue(paymentRequest);
                    return null;
                });


        PaymentsQueue.requeue(paymentRequest);
    }

    private void postToFallback(PaymentRequest paymentRequest) {

        var request = HttpRequest.newBuilder()
                .uri(fallbackUri)
                .header(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .POST(HttpRequest.BodyPublishers.ofByteArray(paymentRequest.getPostData()))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .orTimeout(requestTimeOut, TimeUnit.MILLISECONDS)
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        save(paymentRequest.parseToFallback());
                    } else {
                        PaymentsQueue.requeue(paymentRequest);
                    }
                })
                .exceptionally(throwable -> {
                    PaymentsQueue.requeue(paymentRequest);
                    return null;
                });
    }

    private void save(PaymentRequest paymentRequest) {
        repository.save(paymentRequest);
    }
}