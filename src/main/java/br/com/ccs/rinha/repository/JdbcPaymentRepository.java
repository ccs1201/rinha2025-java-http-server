package br.com.ccs.rinha.repository;


import br.com.ccs.rinha.config.DataSourceFactory;
import br.com.ccs.rinha.model.input.PaymentRequest;
import br.com.ccs.rinha.model.output.PaymentSummary;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;


public class JdbcPaymentRepository {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JdbcPaymentRepository.class.getName());
    private static JdbcPaymentRepository instance;
    private static DataSource dataSource;
    private static final PaymentSummary defaultSummary =
            new PaymentSummary(new PaymentSummary.Summary(0, BigDecimal.ZERO),
                    new PaymentSummary.Summary(0, BigDecimal.ZERO));

    private static final String SQL_INSERT = """
            INSERT INTO payments (amount, requested_at, is_default) 
            VALUES (?, ?, ?)""";

    private static final String SQL_SUMMARY = """
            SELECT 
                SUM(CASE WHEN is_default = true THEN 1 ELSE 0 END) as default_count,
                SUM(CASE WHEN is_default = true THEN amount ELSE 0 END) as default_amount,
                SUM(CASE WHEN is_default = false THEN 1 ELSE 0 END) as fallback_count,
                SUM(CASE WHEN is_default = false THEN amount ELSE 0 END) as fallback_amount
            FROM payments 
            WHERE requested_at >= ? AND requested_at <= ?
            """;

    private ArrayBlockingQueue<PaymentRequest> queues[];


    public static JdbcPaymentRepository getInstance() {
        if (instance == null) {
            instance = new JdbcPaymentRepository();
        }
        return instance;
    }

    public JdbcPaymentRepository() {
        queues = new ArrayBlockingQueue[DataSourceFactory.getPoolSize() - 1];
        initialize();
    }

    private void initialize() {
        dataSource = DataSourceFactory.getInstance();
        var poolSize = DataSourceFactory.getPoolSize() - 1;

        for (int i = 0; i < poolSize; i++) {
            var queue = new ArrayBlockingQueue<PaymentRequest>(1_000);
            queues[i] = queue;
            startWorker(i, queue);
        }
        log.info("JdbcPaymentRepository workers started");
    }

    private void startWorker(int workerIndex, ArrayBlockingQueue<PaymentRequest> queue) {

        Thread.ofVirtual().name("repository-worker-" + workerIndex).start(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        if (queue.size() >= 10) {
                            executeInBatch(queue, stmt, conn);
                            continue;
                        }
                        executeSingleInsert(queue.take(), stmt, conn);

                    } catch (Exception e) {
                        conn.rollback();
                        log.error("Error inserting payment", e);
                    }
                }
            } catch (Exception e) {
                log.error("Worker failure", e);
            }
        });
    }

    public void save(PaymentRequest paymentRequest) {
        int index = Math.abs(paymentRequest.hashCode()) % queues.length;
        boolean accepted = queues[index].offer(paymentRequest);
        if (!accepted) {
            log.info(String.format("Payment rejected by queues"));
        }
    }

    private static void executeInBatch(ArrayBlockingQueue<PaymentRequest> queue, PreparedStatement stmt, Connection conn) throws SQLException {
        long now = System.currentTimeMillis();
        List<PaymentRequest> batch = new ArrayList<>(100);
        queue.drainTo(batch, 100);

        if (batch.isEmpty()) return;

        for (PaymentRequest pr : batch) {
            stmt.setBigDecimal(1, pr.amount);
            stmt.setObject(2, Timestamp.from(pr.requestedAt));
            stmt.setBoolean(3, pr.isDefault);
            stmt.addBatch();
        }

        stmt.executeBatch();
        conn.commit();

        long elapsed = System.currentTimeMillis() - now;
        log.info("BATCH Size {} Processed in {}ms Queue size {}",
                batch.size(), elapsed, queue.size());
    }

    private static void executeSingleInsert(PaymentRequest pr, PreparedStatement stmt, Connection conn) throws InterruptedException, SQLException {

        stmt.setBigDecimal(1, pr.amount);
        stmt.setObject(2, Timestamp.from(pr.requestedAt));
        stmt.setBoolean(3, pr.isDefault);
        stmt.execute();
        conn.commit();
    }


    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SUMMARY)) {

            stmt.setObject(1, from);
            stmt.setObject(2, to);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PaymentSummary(
                            new PaymentSummary.Summary(rs.getLong("default_count"),
                                    rs.getBigDecimal("default_amount") != null ? rs.getBigDecimal("default_amount") : BigDecimal.ZERO),
                            new PaymentSummary.Summary(rs.getLong("fallback_count"),
                                    rs.getBigDecimal("fallback_amount") != null ? rs.getBigDecimal("fallback_amount") : BigDecimal.ZERO)
                    );
                }
                return defaultSummary;
            }

        } catch (SQLException e) {
            log.error("Summary error {}", e.getMessage(), e);
            return defaultSummary;
        }
    }

    public void purge() {
        String sql = "DELETE FROM payments";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}