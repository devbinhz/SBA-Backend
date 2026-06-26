package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.dto.response.payment.PaymentWebhookResponseDTO;
import com.bookverse.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment provider callbacks")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Receive VNPAY payment webhook")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook accepted")
    @GetMapping("/vnpay/webhook")
    public ApiResponse<PaymentWebhookResponseDTO> vnpayWebhookGet(@RequestParam Map<String, String> params) {
        return ApiResponse.success(paymentService.handleVnpayWebhook(params));
    }

    @Operation(summary = "Receive VNPAY payment webhook")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook accepted")
    @PostMapping("/vnpay/webhook")
    public ApiResponse<PaymentWebhookResponseDTO> vnpayWebhookPost(@RequestParam Map<String, String> params) {
        return ApiResponse.success(paymentService.handleVnpayWebhook(params));
    }
}
