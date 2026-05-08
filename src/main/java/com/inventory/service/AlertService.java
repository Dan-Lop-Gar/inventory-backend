package com.inventory.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@Service
public class AlertService {

    private final ProductService productService;
    private final ScheduledExecutorService scheduledExecutor;

    public AlertService(
            ProductService productService,
            @Qualifier("scheduledExecutor") ScheduledExecutorService scheduledExecutor) {
        this.productService = productService;
        this.scheduledExecutor = scheduledExecutor;
    }

    // Cada hora verifica productos por debajo del mínimo
    @Scheduled(fixedDelay = 3600000)
    public void checkStockLevels() {
        scheduledExecutor.submit(() -> {
            log.info("Iniciando revisión de niveles de stock");
            productService.checkAndSendStockAlerts();
        });
    }
}
