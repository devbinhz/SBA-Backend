package com.bookverse.controller;

import com.bookverse.config.SecurityConfig;
import com.bookverse.dto.response.checkout.CheckoutPreviewResponseDTO;
import com.bookverse.dto.response.checkout.CheckoutResponseDTO;
import com.bookverse.security.CustomUserDetailsService;
import com.bookverse.security.JwtAuthenticationFilter;
import com.bookverse.security.JwtService;
import com.bookverse.service.checkout.CheckoutService;
import com.bookverse.service.order.OrderService;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class OrderControllerSecurityTest {

    private static final String GUEST_REQUEST = """
            {
              "email": "guest@example.com",
              "recipient": "Guest Customer",
              "phone": "0900000000",
              "line": "123 Main Street",
              "ward": "Ward 1",
              "district": "District 1",
              "city": "Ho Chi Minh City",
              "items": [{"bookId": 1, "quantity": 1}],
              "deliveryType": "SELF"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;
    @MockBean
    private CheckoutService checkoutService;
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
            chain.doFilter(invocation.getArgument(0, ServletRequest.class),
                    invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    void guestPreviewIsPublic() throws Exception {
        when(checkoutService.previewGuest(any())).thenReturn(new CheckoutPreviewResponseDTO());

        mockMvc.perform(post("/api/v1/orders/guest/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUEST_REQUEST))
                .andExpect(status().isOk());
    }

    @Test
    void guestCheckoutIsPublic() throws Exception {
        when(paymentService.checkoutGuest(any(), any(), any())).thenReturn(new CheckoutResponseDTO());

        mockMvc.perform(post("/api/v1/orders/guest")
                        .header("Idempotency-Key", "guest-test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUEST_REQUEST))
                .andExpect(status().isCreated());
    }
}
