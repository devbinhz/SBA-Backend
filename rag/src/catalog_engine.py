from __future__ import annotations

from qdrant_client import models
from src.config import settings
from src.schemas import (
    CatalogBookHit,
    CatalogSearchResponse,
    CatalogUpsertItem,
    CatalogUpsertResponse,
)
from src.services import OpenAIService, QdrantStore

class CatalogEngine:
    def __init__(
        self,
        openai_service: OpenAIService | None = None,
        store: QdrantStore | None = None,
    ) -> None:
        self.openai_service = openai_service or OpenAIService()
        self.store = store or QdrantStore(collection_name=settings.qdrant_catalog_collection)

    def _build_text(self, item: CatalogUpsertItem) -> str:
        parts = [item.title, item.author]
        if item.category:
            parts.append(item.category)
        if item.publisher:
            parts.append(item.publisher)
        if item.publication_year is not None:
            parts.append(str(item.publication_year))
        if item.language:
            parts.append(item.language)
        if item.pages is not None:
            parts.append(f"{item.pages} pages")
        if item.description:
            parts.append(item.description)
        return " | ".join(parts)

    def upsert(self, items: list[CatalogUpsertItem]) -> CatalogUpsertResponse:
        self.store.ensure_collection()
        texts = [self._build_text(item) for item in items]
        vectors = self.openai_service.embed_texts(texts)
        points = []
        for item, vector in zip(items, vectors, strict=True):
            points.append(
                models.PointStruct(
                    id=item.book_id,
                    vector=vector,
                    payload={
                        "book_id": item.book_id,
                        "title": item.title,
                        "author": item.author,
                        "category": item.category,
                        "publisher": item.publisher,
                        "publication_year": item.publication_year,
                        "language": item.language,
                        "pages": item.pages,
                        "description": item.description,
                    },
                )
            )
        self.store.client.upsert(
            collection_name=self.store.collection_name,
            points=points,
            wait=True,
        )
        return CatalogUpsertResponse(status="ok")

    def search(self, query: str, top_k: int | None = None) -> CatalogSearchResponse:
        limit = top_k or settings.default_top_k
        vector = self.openai_service.embed_text(query)
        results = self.store.client.query_points(
            collection_name=self.store.collection_name,
            query=vector,
            limit=limit,
            with_payload=True,
        )
        hits = []
        for point in results.points:
            book_id = point.payload.get("book_id")
            if book_id is not None:
                hits.append(CatalogBookHit(book_id=int(book_id), score=point.score))
        return CatalogSearchResponse(hits=hits)

    def delete(self, book_id: int) -> None:
        try:
            self.store.client.delete(
                collection_name=self.store.collection_name,
                points_selector=models.PointIdsList(points=[book_id]),
                wait=True,
            )
        except Exception:
            pass
