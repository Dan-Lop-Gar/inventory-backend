package com.inventory.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class HikariPoolConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource(MeterRegistry meterRegistry) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // Pool settings
        config.setPoolName("InventoryHikariPool");
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(20);
        config.setIdleTimeout(300_000);          // 5 minutos
        config.setConnectionTimeout(20_000);     // 20 segundos
        config.setMaxLifetime(1_800_000);        // 30 minutos
        config.setLeakDetectionThreshold(60_000); // 60 segundos
        config.setConnectionTestQuery("SELECT 1");

        // Configuración de Hibernate para batch inserts
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        HikariDataSource dataSource = new HikariDataSource(config);

        // Registra métricas de Hikari en Prometheus automáticamente
        // hikaricp_connections_active, hikaricp_connections_idle, etc.
        dataSource.setMetricRegistry(null); // Micrometer lo registra via auto-config

        log.info("HikariCP pool configurado: min={} max={}",
                config.getMinimumIdle(),
                config.getMaximumPoolSize()
        );

        return dataSource;
    }
}
