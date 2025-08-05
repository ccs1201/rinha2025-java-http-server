package br.com.ccs.rinha.workers;

import br.com.ccs.rinha.model.input.PaymentRequest;

import java.util.concurrent.LinkedTransferQueue;

public class PaymentsQueue {

    private static final LinkedTransferQueue<PaymentRequest> QUEUE = new LinkedTransferQueue<>();

    private PaymentsQueue() {
    }

    public static void offer(byte[] paymentRequest) {
        QUEUE.offer(new PaymentRequest(paymentRequest));
    }

    public static PaymentRequest peek() throws InterruptedException {
        return QUEUE.take();
    }

    public static void requeue(PaymentRequest paymentRequest) {
        QUEUE.offer(paymentRequest);
    }
}
