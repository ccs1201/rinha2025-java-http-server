package br.com.ccs.rinha.workers;

import br.com.ccs.rinha.model.input.PaymentRequest;
import br.com.ccs.rinha.model.input.builder.PaymentRequestBuilder;
import br.com.ccs.rinha.service.PaymentProcessorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;

public class PaymentProcessorWorker {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorWorker.class);

    private final ArrayBlockingQueue<PaymentRequest>[] queues;
    private static PaymentProcessorWorker instance;


    public static PaymentProcessorWorker getInstace() {
        if (instance == null) {
            instance = new PaymentProcessorWorker();
        }
        return instance;
    }


    public PaymentProcessorWorker() {
        var workers = Integer.parseInt(System.getenv("PAYMENT_PROCESSOR_WORKERS"));
        int queueCapacity = Integer.parseInt(System.getenv("PAYMENT_QUEUE"));
        queues = new ArrayBlockingQueue[workers];

        for (int i = 0; i < workers; i++) {
            var queue = queues[i] = new ArrayBlockingQueue<>(queueCapacity);
            startWorkers(i, queue);
        }

        log.info("Payment processor workers: {}", workers);
    }

    private void startWorkers(int wokerIndex, ArrayBlockingQueue<PaymentRequest> queue) {
        Thread.ofVirtual().name("payment-processor-" + wokerIndex).start(() -> {
            var client = PaymentProcessorClient.getInstance();
            while (!Thread.currentThread().isInterrupted()) {
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
        var paymentRequest = PaymentRequestBuilder.fromBytes(data);
        offerToQueue(paymentRequest);
    }

    public void offer(PaymentRequest paymentRequest) {
        offerToQueue(paymentRequest);
    }

    private void offerToQueue(PaymentRequest paymentRequest) {
        int index = Math.abs(paymentRequest.hashCode()) % queues.length;
        if (!queues[index].offer(paymentRequest)) {
            log.error("Payment rejected by queue {}", index);
        }
    }
}
