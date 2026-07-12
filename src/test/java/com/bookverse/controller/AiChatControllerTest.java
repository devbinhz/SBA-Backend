package com.bookverse.controller;

import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.request.ai.AiRecommendRequest;
import com.bookverse.dto.request.ai.CreateChatSessionRequestDTO;
import com.bookverse.dto.request.ai.SendMessageRequestDTO;
import com.bookverse.dto.response.ai.AiChatResponse;
import com.bookverse.dto.response.ai.AiRecommendResponse;
import com.bookverse.dto.response.ai.ChatSessionResponseDTO;
import com.bookverse.dto.response.ai.ChatMessageResponseDTO;
import com.bookverse.dto.response.ai.ChatSessionDetailsResponseDTO;
import com.bookverse.enums.AiRequestType;
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
        when(aiChatService.chat(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/chat")
                        .with(user(securityUser(1L, UserRole.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.answer").value("Clean code is..."));
    }

    @Test
    void recommend_ShouldReturnResponse_WhenCustomerAuthenticated() throws Exception {
        AiRecommendRequest request = new AiRecommendRequest("Recommend Java books", 5, java.util.Collections.emptyList());
        AiRecommendResponse response = new AiRecommendResponse("Here is a list", Collections.emptyList());
        when(aiChatService.recommend(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/recommend")
                        .with(user(securityUser(1L, UserRole.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.answer").value("Here is a list"));
    }

    @Test
    void createSession_ShouldReturnResponse_WhenCustomerAuthenticated() throws Exception {
        CreateChatSessionRequestDTO request = new CreateChatSessionRequestDTO("My Chat", AiRequestType.BOOK_CHAT, List.of(1L));
        ChatSessionResponseDTO response = new ChatSessionResponseDTO(1L, "My Chat", AiRequestType.BOOK_CHAT, List.of(1L), null, null);
        when(aiChatService.createSession(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/chat/sessions")
                        .with(user(securityUser(1L, UserRole.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("My Chat"));
    }

    @Test
    void listSessions_ShouldReturnResponse_WhenCustomerAuthenticated() throws Exception {
        ChatSessionResponseDTO session = new ChatSessionResponseDTO(1L, "My Chat", AiRequestType.BOOK_CHAT, List.of(1L), null, null);
        when(aiChatService.listSessions(any(), any())).thenReturn(List.of(session));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/ai/chat/sessions")
                        .with(user(securityUser(1L, UserRole.CUSTOMER)))
                        .param("type", "BOOK_CHAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("My Chat"));
    }

    @Test
    void getSessionDetails_ShouldReturnResponse_WhenCustomerAuthenticated() throws Exception {
        ChatSessionDetailsResponseDTO response = new ChatSessionDetailsResponseDTO(1L, "My Chat", AiRequestType.BOOK_CHAT, List.of(1L), Collections.emptyList(), null, null);
        when(aiChatService.getSessionDetails(any(), any())).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/ai/chat/sessions/1")
                        .with(user(securityUser(1L, UserRole.CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("My Chat"));
    }

    @Test
    void deleteSession_ShouldReturnResponse_WhenCustomerAuthenticated() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/ai/chat/sessions/1")
                        .with(user(securityUser(1L, UserRole.CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void sendMessage_ShouldReturnResponse_WhenCustomerAuthenticated() throws Exception {
        SendMessageRequestDTO request = new SendMessageRequestDTO("Hello");
        ChatMessageResponseDTO response = new ChatMessageResponseDTO(1L, "assistant", "Hi", Collections.emptyList(), null);
        when(aiChatService.sendMessage(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/chat/sessions/1/messages")
                        .with(user(securityUser(1L, UserRole.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("Hi"));
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

