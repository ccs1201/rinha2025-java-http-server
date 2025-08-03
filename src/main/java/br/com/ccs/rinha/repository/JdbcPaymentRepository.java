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
import java.util.concurrent.LinkedTransferQueue;


public class JdbcPaymentRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcPaymentRepository.class.getName());
    private static JdbcPaymentRepository instance;
    private static HikariDataSource dataSource;
    private static final PaymentSummary defaultSummary =
            new PaymentSummary(new PaymentSummary.Summary(0, BigDecimal.ZERO),
                    new PaymentSummary.Summary(0, BigDecimal.ZERO));

    private static final String SQL_SUMMARY = """
            SELECT 
                SUM(CASE WHEN is_default = true THEN 1 ELSE 0 END) as default_count,
                SUM(CASE WHEN is_default = true THEN amount ELSE 0 END) as default_amount,
                SUM(CASE WHEN is_default = false THEN 1 ELSE 0 END) as fallback_count,
                SUM(CASE WHEN is_default = false THEN amount ELSE 0 END) as fallback_amount
            FROM payments 
            WHERE requested_at >= ? AND requested_at <= ?
            """;

    private static LinkedTransferQueue<PaymentRequest> queue;

    public static JdbcPaymentRepository getInstance() {
        if (instance == null) {
            instance = new JdbcPaymentRepository();
        }
        return instance;
    }

    public JdbcPaymentRepository() {
        queue = new LinkedTransferQueue<>();
        initialize();
    }

    private static void initialize() {
        dataSource = DataSourceFactory.getInstance();
        var poolSize = DataSourceFactory.getPoolSize() - 1;

        for (int i = 0; i < poolSize; i++) {
            startWorker(i, queue);
        }
        log.info("JdbcPaymentRepository workers started");
    }

    private static void startWorker(int workerIndex, LinkedTransferQueue<PaymentRequest> queue) {

        Thread.ofVirtual().name("repository-worker-" + workerIndex).start(() -> {
            final var sql = """
                    INSERT INTO payments (amount, requested_at, is_default)
                    VALUES (?, ?, ?)""";

            final int BATCH_SIZE = Integer.parseInt(System.getenv("BATCH_SIZE"));
            final int BATCH_LIMIT = Integer.parseInt(System.getenv("BATCH_LIMIT"));

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);

                while (true) {
                    if (queue.size() >= BATCH_LIMIT) {
//                        long now = System.currentTimeMillis();
                        var batch = new ArrayList<PaymentRequest>(BATCH_SIZE);
                        queue.drainTo(batch, BATCH_SIZE);
                        executeInBatch(batch, stmt, conn);
//                        log.info("BATCH Size {} Processed in {}ms Queue size {}",
//                                batch.size(), System.currentTimeMillis() - now, queue.size());
                    }

                    executeSingleInsert(queue.take(), stmt, conn);
                }
            } catch (Exception e) {
                log.error("Worker failure", e);
            }
        });
    }

    public void save(PaymentRequest pr) {
       queue.offer(pr);
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
            log.error("BATCH error: {}", e.getMessage(), e);
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