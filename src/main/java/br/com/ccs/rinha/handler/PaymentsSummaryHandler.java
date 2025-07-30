package br.com.ccs.rinha.handler;

import br.com.ccs.rinha.repository.JdbcPaymentRepository;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class PaymentsSummaryHandler extends BaseHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentsSummaryHandler.class);

    private final JdbcPaymentRepository repository = JdbcPaymentRepository.getInstance();


    @Override
    public void handle(HttpExchange exchange) throws IOException {

        var q = exchange.getRequestURI().getQuery();
        try {

            var from = DateParser.parseFrom(q);
            var to = DateParser.parseTo(q);
            var response = repository.getSummary(from, to);
            sendResponse(exchange, response.toJson());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private final class DateParser {

        public static OffsetDateTime parseFrom(CharSequence input) {
            int start = 5; // after "from="
            int end = input.length();
            for (int i = start; i < input.length(); i++) {
                if (input.charAt(i) == '&') {
                    end = i;
                    break;
                }
            }
            return OffsetDateTime.parse(input.subSequence(start, end));
        }

        public static OffsetDateTime parseTo(String input) {
            int start = input.indexOf("&to=") + 4;
            return OffsetDateTime.parse(input.subSequence(start, input.length()));
        }
    }

}