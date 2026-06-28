package com.bookverse.service.ai;

import com.bookverse.integration.rag.dto.RagIngestResponse;
import com.bookverse.integration.rag.dto.RagIndexStatusResponse;
import com.bookverse.integration.rag.dto.RagCatalogStatusResponse;
import java.util.List;

public interface AdminRagService {
    RagIngestResponse ingestBookContent(Long bookId);
    RagIngestResponse ingestBooksContent(List<Long> ids);
    void upsertBookCatalog(Long bookId);
    void upsertBooksCatalog(List<Long> bookIds);
    void deleteBookIndex(Long bookId);
    void deleteBooksIndices(List<Long> bookIds);
    RagIndexStatusResponse getIndexStatus(Long bookId);
    RagCatalogStatusResponse getCatalogStatus(Long bookId);
}
