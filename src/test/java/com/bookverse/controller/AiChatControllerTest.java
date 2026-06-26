package com.bookverse.controller;

import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.response.ai.AiChatResponse;
import com.bookverse.integration.rag.RagClient;
import com.bookverse.integration.rag.dto.RagHealthResponse;
import com.bookverse.integration.rag.dto.RagIngestRequest;
import com.bookverse.integration.rag.dto.RagIngestResponse;
import com.bookverse.entity.User;
import com.bookverse.enums.UserRole;
import com.bookverse.security.JwtService;
import com.bookverse.service.ai.AiChatService;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiChatController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiChatService aiChatService;

    @MockBean
    private RagClient ragClient;

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
    void chat_ShouldReturnResponse_WhenCustomerAuthenticated() throws Exception {
        AiChatRequest request = new AiChatRequest("What is clean code?", List.of(1L), 5);
        AiChatResponse response = new AiChatResponse("Clean code is...", Collections.emptyList());
        when(aiChatService.chat(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/chat")
                        .with(user(securityUser(1L, UserRole.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.answer").value("Clean code is..."));
    }

    @Test
    void checkHealth_ShouldReturnHealth_WhenAdminAuthenticated() throws Exception {
        RagHealthResponse health = new RagHealthResponse("ok", "ok", "ok", "ok");
        when(ragClient.checkHealth()).thenReturn(health);

        mockMvc.perform(get("/api/v1/ai/health")
                        .with(user(securityUser(1L, UserRole.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.qdrant").value("ok"));
    }

    @Test
    void checkHealth_ShouldReturnForbidden_WhenCustomerAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/ai/health")
                        .with(user(securityUser(2L, UserRole.CUSTOMER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void ingest_ShouldReturnIngestResponse_WhenAdminAuthenticated() throws Exception {
        RagIngestRequest request = new RagIngestRequest(Collections.emptyList());
        RagIngestResponse response = new RagIngestResponse(Collections.emptyList(), Collections.emptyList(), 0);
        when(aiChatService.ingest(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/ingest")
                        .with(user(securityUser(1L, UserRole.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total_chunks").value(0));
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
