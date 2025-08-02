package br.com.ccs.rinha.repository;


import br.com.ccs.rinha.config.DataSourceFactory;
import br.com.ccs.rinha.model.input.PaymentRequest;
import br.com.ccs.rinha.model.output.PaymentSummary;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;


public class JdbcPaymentRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcPaymentRepository.class.getName());
    private static JdbcPaymentRepository instance;
    private static HikariDataSource dataSource;
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

    private ArrayBlockingQueue<PaymentRequest> queue;


    public static JdbcPaymentRepository getInstance() {
        if (instance == null) {
            instance = new JdbcPaymentRepository();
        }
        return instance;
    }

    public JdbcPaymentRepository() {
        queue = new ArrayBlockingQueue<>(5_000);
        initialize();
    }

    private void initialize() {
        dataSource = DataSourceFactory.getInstance();
        var poolSize = DataSourceFactory.getPoolSize() - 1;

        for (int i = 0; i < poolSize; i++) {
//            var queue = new ArrayBlockingQueue<PaymentRequest>(1_000);
//            queues[i] = queue;
            startWorker(i, queue);
        }
        log.info("JdbcPaymentRepository workers started");
    }

    private void startWorker(int workerIndex, ArrayBlockingQueue<PaymentRequest> queue) {

        Thread.ofVirtual().name("repository-worker-" + workerIndex).start(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
                conn.setAutoCommit(false);

                while (!Thread.currentThread().isInterrupted()) {

                    if (queue.size() >= 15) {
                        long now = System.currentTimeMillis();
                        var batch = new ArrayList<PaymentRequest>(150);
                        queue.drainTo(batch, 150);
                        executeInBatch(batch, stmt, conn);
                        log.info("BATCH Size {} Processed in {}ms Queue size {}",
                                batch.size(), System.currentTimeMillis() - now, queue.size());
                    }

                    executeSingleInsert(queue.take(), stmt, conn);
                }
            } catch (Exception e) {
                log.error("Worker failure", e);
            }
        });
    }

    public void save(PaymentRequest pr) {
        if (!queue.offer(pr)) {
            log.info("Payment rejected by queues");
        }
    }

    private static void executeInBatch(List<PaymentRequest> batch, PreparedStatement stmt, Connection conn) {
        try {
            for (int i = 0; i < batch.size(); i++) {
                stmt.setBigDecimal(1, batch.get(i).amount);
                stmt.setLong(2, batch.get(i).requestedAt);
                stmt.setBoolean(3, batch.get(i).isDefault);
                stmt.addBatch();
            }

            stmt.executeBatch();
            conn.commit();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void executeSingleInsert(PaymentRequest pr, PreparedStatement stmt, Connection conn) {
        try {
            stmt.setBigDecimal(1, pr.amount);
            stmt.setLong(2, pr.requestedAt);
            stmt.setBoolean(3, pr.isDefault);
            stmt.execute();
            conn.commit();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    public PaymentSummary getSummary(OffsetDateTime from, OffsetDateTime to) {
//        sleep();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SUMMARY)) {

            stmt.setObject(1, from.toInstant().toEpochMilli());
            stmt.setObject(2, to.toInstant().toEpochMilli());

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

    private static void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void purge() {
        String sql = "DELETE FROM payments";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error on purge {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}