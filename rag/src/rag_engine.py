from __future__ import annotations

from typing import Any
from src.config import settings
from src.schemas import QueryResponse, SearchHit, Source
from src.services import OpenAIService, MongoBookStore, QdrantStore


class RagEngine:
    def __init__(
        self,
        openai_service: OpenAIService | None = None,
        manifest: MongoBookStore | None = None,
        store: QdrantStore | None = None,
    ) -> None:
        self.openai_service = openai_service or OpenAIService()
        self.manifest = manifest or MongoBookStore()
        self.store = store or QdrantStore()

    def query(
        self,
        query: str,
        book_ids: list[int] | None = None,
        history: list[Any] | None = None,
        top_k: int | None = None,
    ) -> QueryResponse:
        limit = min(top_k or settings.default_top_k, settings.max_top_k)
        vector = self.openai_service.embed_texts([query])[0]
        hits = self.store.search(vector=vector, limit=limit, book_ids=book_ids)
        filtered_hits = [hit for hit in hits if hit.score >= 0.24]
        sources = [_source_from_hit(hit) for hit in filtered_hits]
        answer, cited_sources, usage = self.openai_service.make_answer(query, sources, history, book_ids=book_ids)
        return QueryResponse(answer=answer, sources=cited_sources, usage=usage)

    def delete(self, book_id: int) -> int:
        chunk_ids = self.manifest.chunk_ids_for_book(book_id)
        self.store.delete_points(chunk_ids)
        self.manifest.delete_book(book_id)
        return len(chunk_ids)

    def get_index_status(self, book_id: int) -> dict[str, Any]:
        book = self.manifest.get_book(book_id)
        if not book:
            return {"book_id": book_id, "status": "not_found"}
        return {
            "book_id": book_id,
            "status": book.get("status"),
            "chunk_count": book.get("chunk_count"),
            "updated_at": book.get("updated_at"),
            "error": book.get("error"),
        }


def _source_from_hit(hit: SearchHit) -> Source:
    payload = hit.payload
    return Source(
        book_id=int(payload.get("book_id") or 0),
        document_name=str(payload.get("document_name") or ""),
        file_name=str(payload.get("file_name") or ""),
        file_type=str(payload.get("file_type") or ""),
        chunk_index=int(payload.get("chunk_index") or 0),
        page=_optional_int(payload.get("page")),
        score=hit.score,
        text=str(payload.get("text") or ""),
    )


def _optional_int(value: object) -> int | None:
    if value is None:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None
