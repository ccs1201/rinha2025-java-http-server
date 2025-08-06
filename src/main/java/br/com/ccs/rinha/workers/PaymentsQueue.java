package br.com.ccs.rinha.workers;

import br.com.ccs.rinha.model.input.PaymentRequest;
import br.com.ccs.rinha.service.PaymentProcessorClient;

import java.util.concurrent.LinkedTransferQueue;

public class PaymentsQueue {

    private static final LinkedTransferQueue<PaymentRequest> QUEUE = new LinkedTransferQueue<>();
    private static final PaymentProcessorClient paymentProcessorClient = PaymentProcessorClient.getInstance();

    private PaymentsQueue() {
    }

    public static void offer(byte[] paymentRequest) {
//        QUEUE.offer(new PaymentRequest(paymentRequest));
        paymentProcessorClient.processPayment(new PaymentRequest(paymentRequest));
    }

    public static PaymentRequest peek() throws InterruptedException {
        return QUEUE.take();
    }

    public static void requeue(PaymentRequest paymentRequest) {
        QUEUE.offer(paymentRequest);
    }
}
