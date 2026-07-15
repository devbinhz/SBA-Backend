package com.bookverse.service.checkout;

import com.bookverse.dto.request.checkout.CheckoutRequestDTO;
import com.bookverse.dto.request.checkout.GuestCheckoutRequestDTO;
import com.bookverse.dto.response.checkout.CheckoutPreviewResponseDTO;
import com.bookverse.dto.response.checkout.CheckoutResponseDTO;

public interface CheckoutService {

    CheckoutPreviewResponseDTO preview(Long userId, CheckoutRequestDTO request);

    CheckoutPreviewResponseDTO previewGuest(GuestCheckoutRequestDTO request);

    CheckoutResponseDTO checkout(Long userId, String idempotencyKey, CheckoutRequestDTO request);

    CheckoutResponseDTO checkoutGuest(String idempotencyKey, GuestCheckoutRequestDTO request);
}
