package com.bookverse.controller;

import com.bookverse.dto.request.cart.CartItemRequestDTO;
import com.bookverse.dto.request.cart.CartMergeRequestDTO;
import com.bookverse.dto.response.cart.CartResponseDTO;
import com.bookverse.entity.User;
import com.bookverse.enums.UserRole;
import com.bookverse.security.JwtService;
import com.bookverse.service.cart.CartService;
import com.bookverse.config.SecurityConfig;
import com.bookverse.security.CustomUserDetailsService;
import com.bookverse.security.JwtAuthenticationFilter;
import com.bookverse.security.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;
    
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
    }

    @Test
    void getCart_ShouldReturnCart_WhenCustomerAuthenticated() throws Exception {
        CartResponseDTO mockResponse = new CartResponseDTO();
        when(cartService.getCartResponse(any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/cart").with(user(securityUser(1L, UserRole.CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getCart_ShouldReturnForbidden_WhenAdminAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/cart").with(user(securityUser(2L, UserRole.ADMIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCart_ShouldReturnForbidden_WhenNotAuthenticatedInControllerSlice() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addCartItem_ShouldReturnCart_WhenValidRequest() throws Exception {
        CartItemRequestDTO request = new CartItemRequestDTO();
        request.setBookId(1L);
        request.setQuantity(2);

        CartResponseDTO mockResponse = new CartResponseDTO();
        when(cartService.addCartItem(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/cart/items")
                .with(user(securityUser(1L, UserRole.CUSTOMER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void mergeCart_ShouldReturnCart_WhenCustomerAuthenticated() throws Exception {
        CartMergeRequestDTO request = new CartMergeRequestDTO(List.of(new CartItemRequestDTO(1L, 2)));
        when(cartService.mergeCart(any(), any())).thenReturn(new CartResponseDTO());

        mockMvc.perform(post("/api/v1/cart/merge")
                        .with(user(securityUser(1L, UserRole.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void mergeCart_ShouldReturnForbidden_WhenAdminAuthenticated() throws Exception {
        CartMergeRequestDTO request = new CartMergeRequestDTO(List.of(new CartItemRequestDTO(1L, 2)));

        mockMvc.perform(post("/api/v1/cart/merge")
                        .with(user(securityUser(2L, UserRole.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private SecurityUser securityUser(Long id, UserRole role) {
        return new SecurityUser(User.builder()
                .id(id)
                .email(role.name().toLowerCase() + "@example.com")
                .passwordHash("password")
                .fullName(role.name())
                .role(role)
                .enabled(true)
                .emailVerified(true)
                .build());
    }
}
