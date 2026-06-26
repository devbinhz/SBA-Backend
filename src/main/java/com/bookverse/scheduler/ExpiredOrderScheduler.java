package com.bookverse.scheduler;

import com.bookverse.config.SchedulerProperties;
import com.bookverse.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredOrderScheduler {

    private final OrderService orderService;
    private final SchedulerProperties schedulerProperties;

    @Scheduled(fixedDelayString = "${bookverse.scheduler.expired-orders.fixed-delay-ms}")
    public void expirePendingOrders() {
        int expired = orderService.expirePendingOrders(schedulerProperties.batchSize());
        if (expired > 0) {
            log.info("Expired pending orders: count={}", expired);
        }
    }
}
