package br.com.ccs.rinha.repository;

import br.com.ccs.rinha.model.input.PaymentRequest;
import br.com.ccs.rinha.model.output.PaymentSummary;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentSkipListMap;

public class InMemoeryRespository {

    ConcurrentSkipListMap<Long, PaymentRequest> index = new ConcurrentSkipListMap<>();

    public void insert(PaymentRequest paymentRequest) {
        index.put(paymentRequest.requestedAt, paymentRequest);
    }


    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {
        var range = index.subMap(from.toInstant().toEpochMilli(), true, to.toInstant().toEpochMilli(), true);

        var defaultAmount = 0;
        var defaultRequests = 0;
        var fallbackAmount = 0;
        var fallbackrequests = 0;

        for (PaymentRequest p : range.values()) {

            if (!p.isDefault) {
                fallbackrequests++;
                fallbackAmount += p.amount.intValueExact();
            }

            defaultAmount += p.amount.intValueExact();
            defaultRequests++;

        }

        return new PaymentSummary(
                new PaymentSummary.Summary(defaultRequests, new BigDecimal(defaultAmount).movePointLeft(2)),
                new PaymentSummary.Summary(fallbackrequests, new BigDecimal(fallbackAmount).movePointLeft(2)));

    }

    private PaymentSummary callOther(OffsetDateTime from, OffsetDateTime to) {

        //todo chamar outra api

        return null;
    }

    public void purge() {
        index.clear();
    }

}
