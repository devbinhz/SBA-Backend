package com.bookverse.controller;

import com.bookverse.config.SecurityConfig;
import com.bookverse.dto.request.review.ReviewRequestDTO;
import com.bookverse.dto.response.review.ReviewModerationHistoryResponseDTO;
import com.bookverse.dto.response.review.ReviewSummaryResponseDTO;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void getReviewSummary_isPublicAndReturnsRatingCounts() throws Exception {
        when(reviewService.getReviewSummary(1L)).thenReturn(ReviewSummaryResponseDTO.builder()
                .averageRating(BigDecimal.valueOf(4.5))
                .totalReviews(3)
                .ratingCounts(Map.of(5, 2L, 4, 1L, 3, 0L, 2, 0L, 1, 0L))
                .build());

        mockMvc.perform(get("/api/v1/books/1/reviews/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(4.5))
                .andExpect(jsonPath("$.data.totalReviews").value(3))
                .andExpect(jsonPath("$.data.ratingCounts.5").value(2))
                .andExpect(jsonPath("$.data.ratingCounts.1").value(0));
    }

    @Test
    void getModerationHistory_requiresAdminAndReturnsAuditEntries() throws Exception {
        when(reviewService.getModerationHistory(any(), any())).thenReturn(new PageImpl<>(List.of(
                ReviewModerationHistoryResponseDTO.builder()
                        .id(7L)
                        .reviewId(1L)
                        .moderatorName("Admin User")
                        .createdAt(Instant.parse("2026-07-12T08:00:00Z"))
                        .build()
        )));

        mockMvc.perform(get("/api/v1/admin/reviews/1/moderation-history")
                        .with(user(securityUser(9L, UserRole.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(7))
                .andExpect(jsonPath("$.data.items[0].moderatorName").value("Admin User"));

        mockMvc.perform(get("/api/v1/admin/reviews/1/moderation-history")
                        .with(user(securityUser(1L, UserRole.CUSTOMER))))
                .andExpect(status().isForbidden());
    }
}
