package com.bookverse.service.ai;

import com.bookverse.common.exception.BookInactiveException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.response.ai.AiChatResponse;
import com.bookverse.entity.Book;
import com.bookverse.integration.rag.RagClient;
import com.bookverse.integration.rag.dto.RagQueryResponse;
import com.bookverse.integration.rag.dto.RagSource;
import com.bookverse.repository.BookRepository;
import com.bookverse.service.ai.impl.AiChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AiChatServiceImplTest {

    private BookRepository bookRepository;
    private RagClient ragClient;
    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        bookRepository = Mockito.mock(BookRepository.class);
        ragClient = Mockito.mock(RagClient.class);
        aiChatService = new AiChatServiceImpl(bookRepository, ragClient);
    }

    @Test
    void chat_ShouldThrowResourceNotFoundException_WhenBookNotFound() {
        when(bookRepository.findAllById(any())).thenReturn(Collections.emptyList());
        AiChatRequest request = new AiChatRequest("query", List.of(1L), 5);

        assertThrows(ResourceNotFoundException.class, () -> aiChatService.chat(request));
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

        assertThrows(BookInactiveException.class, () -> aiChatService.chat(request));
    }

    @Test
    void chat_ShouldReturnConsolidatedResponse_WhenBooksValid() {
        Book book = Book.builder()
                .id(1L)
                .title("Clean Code")
                .active(true)
                .build();
        when(bookRepository.findAllById(any())).thenReturn(List.of(book));

        RagSource source = new RagSource(1L, "Clean Code", "clean_code.pdf", "pdf", 0, 10, 0.95, "Use meaningful names.");
        RagQueryResponse queryResp = new RagQueryResponse("LLM answer", List.of(source), new RagQueryResponse.Usage(0, 0, 0));
        when(ragClient.query(any())).thenReturn(queryResp);

        AiChatRequest request = new AiChatRequest("meaningful names", List.of(1L), 5);
        AiChatResponse chatResp = aiChatService.chat(request);

        assertNotNull(chatResp);
        assertEquals(1, chatResp.sources().size());
        assertEquals("Clean Code", chatResp.sources().get(0).bookTitle());
        assertTrue(chatResp.answer().contains("Use meaningful names."));
    }
}
