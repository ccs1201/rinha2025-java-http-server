package br.com.ccs.rinha.workers;

import br.com.ccs.rinha.model.input.PaymentRequest;
import br.com.ccs.rinha.service.PaymentProcessorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedTransferQueue;

public class PaymentProcessorWorker {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorWorker.class);
    private final PaymentProcessorClient client = PaymentProcessorClient.getInstance();

    private final LinkedTransferQueue<PaymentRequest> queue = new LinkedTransferQueue<>();
    private static PaymentProcessorWorker instance;


    public static PaymentProcessorWorker getInstance() {
        if (instance == null) {
            instance = new PaymentProcessorWorker();
        }
        return instance;
    }

    public PaymentProcessorWorker() {
        var workers = Integer.parseInt(System.getenv("PAYMENT_PROCESSOR_WORKERS"));
        for (int i = 0; i < workers; i++) {
            startWorkers(i);
        }

        log.info("Payment processor workers: {}", workers);
    }

    private void startWorkers(int wokerIndex) {
        Thread.ofVirtual().name("payment-processor-" + wokerIndex).start(() -> {
            while (true) {
                try {
                    client.processPayment(queue.take());
                } catch (InterruptedException e) {
                    log.error("worker: {} has error: {}", Thread.currentThread().getName(), e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void offer(byte[] data) {
        queue.offer(new PaymentRequest(data));
    }
}
