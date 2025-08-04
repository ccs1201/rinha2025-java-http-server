package br.com.ccs.rinha.workers;

import br.com.ccs.rinha.model.input.PaymentRequest;

import java.util.concurrent.LinkedTransferQueue;

public class PaymentsQueue {

    private static final LinkedTransferQueue<byte[]> QUEUE = new LinkedTransferQueue<>();

    private PaymentsQueue() {
    }

    public static void offer(byte[] paymentRequest) {
        QUEUE.offer(PaymentRequest.addRequestedAtToJsonBytes(paymentRequest));
    }

    public static byte[] peek() throws InterruptedException {
        return QUEUE.take();
    }
}
