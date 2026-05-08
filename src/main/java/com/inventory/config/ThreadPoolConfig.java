package com.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Configuration
public class ThreadPoolConfig {

    // Para procesamiento de reportes pesados — CPU bound
    @Bean(name = "reportExecutor")
    public ExecutorService reportExecutor() {
        return new ThreadPoolExecutor(
2,                          // core threads
            4,                          // max threads
            60L,                        // keepAlive
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            Thread.ofPlatform()
                .name("report-worker-", 1)
                .factory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    // Para llamadas gRPC internas — IO bound, virtual threads
    @Bean(name = "grpcInternalExecutor")
    public ExecutorService grpcInternalExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // Para notificaciones asíncronas — no bloquean el request principal
    @Bean(name = "notificationExecutor")
    public ExecutorService notificationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // Para tareas periódicas — revisión de stock, cleanup
    @Bean(name = "scheduledExecutor")
    public ScheduledExecutorService scheduledExecutor() {
        return new ScheduledThreadPoolExecutor(
            2,
            Thread.ofPlatform()
                .name("scheduled-worker-", 1)
                .factory()
        );
    }
}
