package com.bookverse.mapper;

import com.bookverse.entity.Order;
import com.bookverse.enums.OrderStatus;
import com.bookverse.enums.PaymentProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper orderMapper = new OrderMapper(new ObjectMapper());

    @Test
    void mapsGuestOrderWithoutCustomerAccount() {
        Order guestOrder = Order.builder()
                .id(10L)
                .guestEmail("guest@example.com")
                .status(OrderStatus.PENDING_PAYMENT)
                .subtotal(100L)
                .shippingFee(30L)
                .giftWrapFee(0L)
                .discountAmount(0L)
                .total(130L)
                .addressSnapshot("{\"recipient\":\"Guest\"}")
                .paymentMethod(PaymentProvider.VNPAY)
                .idempotencyKey("guest-key")
                .build();

        assertThat(orderMapper.toSummary(guestOrder).getUserId()).isNull();
        assertThat(orderMapper.toDetail(guestOrder, List.of()).getUserId()).isNull();
    }
}
