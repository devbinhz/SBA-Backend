package com.bookverse.dto.request.checkout;

import com.bookverse.enums.DeliveryType;
import com.bookverse.enums.PaymentProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GuestCheckoutRequestDTO {

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Recipient name is required")
    private String recipient;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Address line is required")
    private String line;

    private String ward;

    private String district;

    @NotBlank(message = "City is required")
    private String city;

    @NotEmpty(message = "Cart items cannot be empty")
    @Valid
    private List<GuestCartItemDTO> items;

    @NotNull(message = "Delivery type is required")
    private DeliveryType deliveryType = DeliveryType.SELF;

    @NotNull(message = "Payment method is required")
    private PaymentProvider paymentMethod = PaymentProvider.VNPAY;
}
