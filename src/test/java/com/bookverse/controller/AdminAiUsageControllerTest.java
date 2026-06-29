package com.bookverse.controller;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.response.ai.AiUsageLogResponseDTO;
import com.bookverse.entity.User;
import com.bookverse.enums.AiRequestType;
import com.bookverse.enums.UserRole;
import com.bookverse.security.JwtService;
import com.bookverse.service.ai.AiUsageService;
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

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAiUsageController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AdminAiUsageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiUsageService aiUsageService;

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
    void getLogs_ShouldReturnLogs_WhenAdminAuthenticated() throws Exception {
        AiUsageLogResponseDTO log = new AiUsageLogResponseDTO(1L, 2L, "customer@example.com", "Customer Name", AiRequestType.BOOK_RECOMMEND, "query", "response", 10, 20, 30, 100L, Instant.now());
        PageResponseDTO<AiUsageLogResponseDTO> pageResponse = new PageResponseDTO<>(List.of(log), 0, 10, 1L, 1);
        when(aiUsageService.getLogs(any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/ai/usage/logs")
                        .with(user(securityUser(1L, UserRole.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].query").value("query"));
    }

    @Test
    void getUserSummary_ShouldReturnSummary_WhenAdminAuthenticated() throws Exception {
        when(aiUsageService.getUserSummary()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/admin/ai/usage/summary/users")
                        .with(user(securityUser(1L, UserRole.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getDailySummary_ShouldReturnSummary_WhenAdminAuthenticated() throws Exception {
        when(aiUsageService.getDailySummary()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/admin/ai/usage/summary/daily")
                        .with(user(securityUser(1L, UserRole.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getLogs_ShouldReturn403Forbidden_WhenCustomerTriesToAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/usage/logs")
                        .with(user(securityUser(2L, UserRole.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON))
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
