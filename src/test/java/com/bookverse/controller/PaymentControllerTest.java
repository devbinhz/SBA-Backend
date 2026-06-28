package com.bookverse.controller;

import com.bookverse.config.SecurityConfig;
import com.bookverse.dto.response.payment.PaymentWebhookResponseDTO;
import com.bookverse.security.CustomUserDetailsService;
import com.bookverse.security.JwtAuthenticationFilter;
import com.bookverse.security.JwtService;
import com.bookverse.service.payment.PaymentService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0, ServletRequest.class), invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
        when(paymentService.handleVnpayWebhook(any())).thenReturn(PaymentWebhookResponseDTO.builder()
                .processed(true)
                .duplicate(false)
                .status("PAID")
                .build());
    }

    @Test
    void vnpayWebhookGet_ShouldBePublicAndProcessQueryParams() throws Exception {
        mockMvc.perform(get("/api/v1/payments/vnpay/webhook")
                        .param("vnp_TxnRef", "1001001")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionStatus", "00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.processed").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    void vnpayWebhookPost_ShouldBePublicAndProcessFormParams() throws Exception {
        mockMvc.perform(post("/api/v1/payments/vnpay/webhook")
                        .param("vnp_TxnRef", "1001001")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionStatus", "00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.processed").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }
}
