package com.bookverse.service.ai;

import com.bookverse.common.exception.BookInactiveException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.request.ai.AiRecommendRequest;
import com.bookverse.dto.response.ai.AiChatResponse;
import com.bookverse.dto.response.ai.AiRecommendResponse;
import com.bookverse.entity.Book;
import com.bookverse.entity.Category;
import com.bookverse.enums.AiRequestType;
import com.bookverse.integration.rag.RagClient;
import com.bookverse.integration.rag.dto.RagCatalogBookHit;
import com.bookverse.integration.rag.dto.RagCatalogSearchResponse;
import com.bookverse.integration.rag.dto.RagQueryResponse;
import com.bookverse.integration.rag.dto.RagSource;
import com.bookverse.mapper.BookMapper;
import com.bookverse.repository.BookRepository;
import com.bookverse.service.ai.impl.AiChatServiceImpl;
import com.bookverse.service.book.BookOwnershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookverse.repository.ChatMessageRepository;
import com.bookverse.repository.ChatSessionRepository;
import com.bookverse.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiChatServiceImplTest {

    private BookRepository bookRepository;
    private RagClient ragClient;
    private BookOwnershipService bookOwnershipService;
    private AiUsageService aiUsageService;
    private BookMapper bookMapper;
    private ChatSessionRepository chatSessionRepository;
    private ChatMessageRepository chatMessageRepository;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;
    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        bookRepository = Mockito.mock(BookRepository.class);
        ragClient = Mockito.mock(RagClient.class);
        bookOwnershipService = Mockito.mock(BookOwnershipService.class);
        aiUsageService = Mockito.mock(AiUsageService.class);
        bookMapper = Mockito.mock(BookMapper.class);
        chatSessionRepository = Mockito.mock(ChatSessionRepository.class);
        chatMessageRepository = Mockito.mock(ChatMessageRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        objectMapper = new ObjectMapper();
        
        aiChatService = new AiChatServiceImpl(
                bookRepository, 
                ragClient, 
                bookOwnershipService, 
                aiUsageService, 
                bookMapper,
                chatSessionRepository,
                chatMessageRepository,
                userRepository,
                objectMapper
        );
    }

    @Test
    void chat_ShouldThrowResourceNotFoundException_WhenBookNotFound() {
        when(bookRepository.findAllById(any())).thenReturn(Collections.emptyList());
        AiChatRequest request = new AiChatRequest("query", List.of(1L), 5);

        assertThrows(ResourceNotFoundException.class, () -> aiChatService.chat(request, 100L));
    }

    @Test
    void chat_ShouldThrowBookInactiveException_WhenBookIsInactive() {
        Book inactiveBook = Book.builder()
                .id(1L)
                .title("Clean Code")
                .active(false)
                .build();
        when(bookRepository.findAllById(any())).thenReturn(List.of(inactiveBook));
        AiChatRequest request = new AiChatRequest("query", List.of(1L), 5);

        assertThrows(BookInactiveException.class, () -> aiChatService.chat(request, 100L));
    }

    @Test
    void chat_ShouldThrowAccessDeniedException_WhenUserHasNotPurchasedBooks() {
        Book book = Book.builder()
                .id(1L)
                .title("Clean Code")
                .active(true)
                .build();
        when(bookRepository.findAllById(any())).thenReturn(List.of(book));
        when(bookOwnershipService.hasUserPurchasedBooks(eq(100L), any())).thenReturn(false);

        AiChatRequest request = new AiChatRequest("query", List.of(1L), 5);

        assertThrows(AccessDeniedException.class, () -> aiChatService.chat(request, 100L));
    }

    @Test
    void chat_ShouldReturnConsolidatedResponse_WhenBooksValidAndPurchased() {
        Book book = Book.builder()
                .id(1L)
                .title("Clean Code")
                .active(true)
                .build();
        when(bookRepository.findAllById(any())).thenReturn(List.of(book));
        when(bookOwnershipService.hasUserPurchasedBooks(eq(100L), any())).thenReturn(true);

        RagSource source = new RagSource(1L, "Clean Code", "clean_code.pdf", "pdf", 0, 10, 0.95, "Use meaningful names.");
        RagQueryResponse queryResp = new RagQueryResponse("LLM answer", List.of(source), new RagQueryResponse.Usage(10, 20, 30));
        when(ragClient.query(any())).thenReturn(queryResp);

        AiChatRequest request = new AiChatRequest("meaningful names", List.of(1L), 5);
        AiChatResponse chatResp = aiChatService.chat(request, 100L);

        assertNotNull(chatResp);
        assertEquals(1, chatResp.sources().size());
        assertEquals("Clean Code", chatResp.sources().get(0).bookTitle());
        assertTrue(chatResp.answer().contains("LLM answer"));

        verify(aiUsageService).logUsage(eq(100L), eq(AiRequestType.BOOK_CHAT), eq("meaningful names"), any(), eq(10), eq(20), any(Long.class));
    }

    @Test
    void recommend_ShouldReturnRecommendations_WhenQueryIsSent() {
        Category category = Category.builder().id(2L).name("Programming").active(true).build();
        Book book = Book.builder()
                .id(1L)
                .title("Effective Java")
                .author("Joshua Bloch")
                .description("Great book")
                .active(true)
                .category(category)
                .build();

        RagCatalogSearchResponse catalogResp = new RagCatalogSearchResponse(List.of(new RagCatalogBookHit(1L, 0.85)));
        when(ragClient.catalogSearch(any())).thenReturn(catalogResp);
        when(bookRepository.findAllById(any())).thenReturn(List.of(book));

        AiRecommendRequest request = new AiRecommendRequest("Java", 5, java.util.Collections.emptyList());
        AiRecommendResponse response = aiChatService.recommend(request, 100L);

        assertNotNull(response);
        assertTrue(response.answer().contains("Effective Java"));
        verify(aiUsageService).logUsage(eq(100L), eq(AiRequestType.BOOK_RECOMMEND), eq("Java"), any(), any(Integer.class), any(Integer.class), any(Long.class));
    }
}
