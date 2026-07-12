package com.bookverse.controller;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.voucher.VoucherCreateRequestDTO;
import com.bookverse.dto.response.voucher.AdminVoucherResponseDTO;
import com.bookverse.enums.DiscountType;
import com.bookverse.enums.UserRole;
import com.bookverse.security.CustomUserDetailsService;
import com.bookverse.security.JwtAuthenticationFilter;
import com.bookverse.security.JwtService;
import com.bookverse.security.SecurityUser;
import com.bookverse.entity.User;
import com.bookverse.service.voucher.VoucherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoucherController.class)
@ActiveProfiles("test")
class VoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VoucherService voucherService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private SecurityUser adminUser;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0, ServletRequest.class), invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        User user = User.builder()
                .id(1L)
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .passwordHash("encoded")
                .build();
        adminUser = new SecurityUser(user);
    }

    @Test
    void createVoucher_Success() throws Exception {
        VoucherCreateRequestDTO request = new VoucherCreateRequestDTO();
        request.setName("Discount 10%");
        request.setDiscountType(DiscountType.PERCENTAGE);
        request.setDiscountValue(10L);
        request.setTierMinAmount(100000L);

        AdminVoucherResponseDTO response = AdminVoucherResponseDTO.builder()
                .id(1L)
                .name("Discount 10%")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10L)
                .tierMinAmount(100000L)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(voucherService.createVoucherConfig(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/vouchers")
                .with(user(adminUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("Discount 10%"));
    }

    @Test
    void getVouchers_Success() throws Exception {
        AdminVoucherResponseDTO response = AdminVoucherResponseDTO.builder()
                .id(1L)
                .name("Discount 10%")
                .build();
        PageResponseDTO<AdminVoucherResponseDTO> pageResponse = new PageResponseDTO<>(
                List.of(response), 0, 20, 1, 1
        );

        when(voucherService.getAllVoucherConfigs(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/vouchers")
                .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(1L));
    }
}
