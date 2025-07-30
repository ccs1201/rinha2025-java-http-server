package br.com.ccs.rinha.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;


public class DataSourceFactory {

    private static final Logger log = LoggerFactory.getLogger(DataSourceFactory.class);
    private static final HikariDataSource  instance;
    private static int poolSize;

    static {
        instance = initDataSource();
    }

    private DataSourceFactory() {
    }

    public static int getPoolSize() {
        return poolSize;
    }

    public static HikariDataSource getInstance() {
        return instance;
    }

    private static HikariDataSource  initDataSource() {
        String minIdleEnv = System.getenv("DATASOURCE_MINIMUM_IDLE").trim();
        String maxPoolEnv = System.getenv("DATASOURCE_MAXIMUM_POOL_SIZE").trim();
        String timeoutEnv = System.getenv("DATASOURCE_TIMEOUT").trim();


        int minIdle = minIdleEnv.isBlank() ? 10 : Integer.parseInt(minIdleEnv);
        int maxPoolSize = maxPoolEnv.isBlank() ? 10 : Integer.parseInt(maxPoolEnv);
        int dataSourceTimeout = timeoutEnv.isBlank() ? 5000 : Integer.parseInt(timeoutEnv);

        poolSize = maxPoolSize;

        String dataSourceUrl = System.getenv("DATASOURCE_URL").trim();
        String datasourceUsername = System.getenv("DATASOURCE_USERNAME").trim();
        String dataSourcePassword = System.getenv("DATASOURCE_PASSWORD").trim();

        log.info("Data Source URL: {}", dataSourceUrl);
        log.info("Data Source Username: {}", datasourceUsername);
        log.info("Data Source Password: {}", dataSourcePassword);
        log.info("Data Source Timeout: {}", dataSourceTimeout);
        log.info("Data Source Minimum Idle: {}", minIdle);
        log.info("Data Source Maximum Pool Size: {}", maxPoolSize);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dataSourceUrl);
        config.setUsername(datasourceUsername);
        config.setPassword(dataSourcePassword);
        config.setMinimumIdle(minIdle);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(dataSourceTimeout);
        config.setAutoCommit(true);
        config.setValidationTimeout(1000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "10");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "20");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.setConnectionTestQuery("SELECT 1");

        log.info("Data Source Configured {}", config);

        return new HikariDataSource(config);
    }

    public static void close(){
        instance.close();
        log.info("Hikari DataSource closed.");
    }
}
