package com.bookverse.controller;

import com.bookverse.config.SecurityConfig;
import com.bookverse.dto.request.review.ReviewRequestDTO;
import com.bookverse.entity.User;
import com.bookverse.enums.UserRole;
import com.bookverse.security.CustomUserDetailsService;
import com.bookverse.security.JwtAuthenticationFilter;
import com.bookverse.security.JwtService;
import com.bookverse.security.SecurityUser;
import com.bookverse.service.review.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookverse.common.exception.GlobalExceptionHandler;

@WebMvcTest(ReviewController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

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

    @Test
    void createReview_invalidRating_returnsBadRequest() throws Exception {
        ReviewRequestDTO request = ReviewRequestDTO.builder()
                .bookId(1L)
                .rating(6) // Invalid rating
                .comment("Good")
                .build();

        mockMvc.perform(post("/api/v1/reviews")
                .with(user(securityUser(1L, UserRole.CUSTOMER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.rating").exists());

        ReviewRequestDTO request2 = ReviewRequestDTO.builder()
                .bookId(1L)
                .rating(0) // Invalid rating
                .comment("Bad")
                .build();

        mockMvc.perform(post("/api/v1/reviews")
                .with(user(securityUser(1L, UserRole.CUSTOMER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.rating").exists());
    }
}
