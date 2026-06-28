package com.bookverse.integration.payment;

import com.bookverse.common.exception.BadRequestException;
import com.bookverse.config.VnpayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VnpayPaymentGateway implements PaymentGateway {

    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(VNPAY_ZONE);

    private final VnpayProperties properties;

    @Override
    public PaymentLinkResult createCheckoutLink(PaymentLinkCommand command) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.tmnCode());
        params.put("vnp_Amount", String.valueOf(command.amount() * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", String.valueOf(command.providerOrderCode()));
        params.put("vnp_OrderInfo", command.orderInfo());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.returnUrl());
        params.put("vnp_IpAddr", command.clientIp() == null || command.clientIp().isBlank() ? "127.0.0.1" : command.clientIp());
        params.put("vnp_CreateDate", VNPAY_DATE_TIME.format(java.time.Instant.now()));
        if (command.expiresAt() != null) {
            params.put("vnp_ExpireDate", VNPAY_DATE_TIME.format(command.expiresAt()));
        }
        String secureHash = hmacSha512(signingPayload(params));
        String query = queryString(params) + "&vnp_SecureHash=" + urlEncode(secureHash);
        return new PaymentLinkResult(String.valueOf(command.providerOrderCode()), properties.paymentUrl() + "?" + query);
    }

    @Override
    public PaymentWebhookResult verifyWebhook(PaymentWebhookCommand command) {
        TreeMap<String, String> params = new TreeMap<>(command.params());
        String secureHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        if (secureHash == null || secureHash.isBlank()) {
            return invalidWebhook(params);
        }
        boolean valid = hmacSha512(signingPayload(params)).equalsIgnoreCase(secureHash);
        Long providerOrderCode = parseProviderOrderCode(params.get("vnp_TxnRef"));
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);
        String transactionId = params.get("vnp_TransactionNo");
        return new PaymentWebhookResult(
                valid,
                dedupeKey(params, secureHash),
                "vnpay.payment",
                providerOrderCode,
                transactionId,
                success,
                responseCode,
                transactionStatus
        );
    }

    private PaymentWebhookResult invalidWebhook(TreeMap<String, String> params) {
        return new PaymentWebhookResult(
                false,
                dedupeKey(params, ""),
                "vnpay.payment",
                parseProviderOrderCode(params.get("vnp_TxnRef")),
                params.get("vnp_TransactionNo"),
                false,
                params.get("vnp_ResponseCode"),
                params.get("vnp_TransactionStatus")
        );
    }

    private Long parseProviderOrderCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid VNPAY transaction reference");
        }
    }

    private String signingPayload(TreeMap<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String queryString(TreeMap<String, String> params) {
        return signingPayload(params);
    }

    private String dedupeKey(TreeMap<String, String> params, String secureHash) {
        String transactionNo = params.get("vnp_TransactionNo");
        if (transactionNo != null && !transactionNo.isBlank()) {
            return "vnpay:" + params.getOrDefault("vnp_TxnRef", "unknown") + ":" + transactionNo;
        }
        return "vnpay:" + hmacSha512(signingPayload(params) + "|" + secureHash);
    }

    private String hmacSha512(String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(properties.hashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign VNPAY payload", exception);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
