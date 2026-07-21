package com.bookverse.dto.request.checkout;

import com.bookverse.enums.DeliveryType;
import com.bookverse.enums.PaymentProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequestDTO {

    @NotNull(message = "Address id is required")
    private Long addressId;

    @NotEmpty(message = "At least one cart item must be selected")
    private List<@NotNull(message = "Cart item id is required") Long> cartItemIds;

    private Long userVoucherId;

    @NotNull(message = "Delivery type is required")
    private DeliveryType deliveryType = DeliveryType.SELF;

    private Long giftWrapId;

    @NotNull(message = "Payment method is required")
    private PaymentProvider paymentMethod = PaymentProvider.VNPAY;
}
