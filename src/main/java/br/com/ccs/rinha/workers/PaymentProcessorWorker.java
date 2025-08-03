package br.com.ccs.rinha.workers;

import br.com.ccs.rinha.service.PaymentProcessorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentProcessorWorker {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorWorker.class);
    private static final PaymentProcessorClient client = PaymentProcessorClient.getInstance();

    public PaymentProcessorWorker() {
    }

    public static void start() {
        var workers = Integer.parseInt(System.getenv("PAYMENT_PROCESSOR_WORKERS"));

        for (int i = 0; i < workers; i++) {
            startWorker(i);
        }

        log.info("Payment processor workers: {}", workers);
    }

    private static void startWorker(int wokerIndex) {
        Thread.ofVirtual().name("payment-processor-" + wokerIndex).start(() -> {
            log.info("Payment processor worker started: {}", wokerIndex);
            while (true) {
                try {
                    client.processPayment(PaymentsQueue.peek());
                } catch (InterruptedException e) {
                    log.error("Payment Worker error: {}", e.getMessage(), e);
                }
            }
        });
    }
}
