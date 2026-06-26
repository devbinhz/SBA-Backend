from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class Usage(BaseModel):
    prompt_tokens: int = 0
    completion_tokens: int = 0
    total_tokens: int = 0


class HealthResponse(BaseModel):
    status: Literal["ok"]
    qdrant: Literal["ok", "unavailable"]
    mongo: Literal["ok", "unavailable"]
    minio: Literal["ok", "unavailable"]
    mongo_database: str
    books_collection: str
    chunks_collection: str
    images_collection: str
    collection: str


class IngestItem(BaseModel):
    book_id: int
    file_path: str
    title: str | None = None


class IngestRequest(BaseModel):
    items: list[IngestItem]


class IndexedDocument(BaseModel):
    book_id: int
    file_name: str
    chunks: int


class IngestError(BaseModel):
    book_id: int
    error: str


class IngestResponse(BaseModel):
    indexed: list[IndexedDocument] = Field(default_factory=list)
    errors: list[IngestError] = Field(default_factory=list)
    total_chunks: int = 0


class QueryRequest(BaseModel):
    query: str = Field(min_length=1)
    book_ids: list[int] | None = None
    top_k: int | None = Field(default=None, ge=1)


class Source(BaseModel):
    book_id: int
    document_name: str
    file_name: str
    file_type: str
    chunk_index: int
    page: int | None = None
    score: float
    text: str


class QueryResponse(BaseModel):
    answer: str
    sources: list[Source]
    usage: Usage


class SearchHit(BaseModel):
    id: str
    score: float
    payload: dict[str, object]


class CatalogUpsertItem(BaseModel):
    book_id: int
    title: str
    author: str
    category: str | None = None
    publisher: str | None = None
    publication_year: int | None = None
    language: str | None = None
    pages: int | None = None
    description: str | None = None


class CatalogUpsertRequest(BaseModel):
    items: list[CatalogUpsertItem]


class CatalogUpsertResponse(BaseModel):
    status: Literal["ok"]


class CatalogSearchRequest(BaseModel):
    query: str = Field(min_length=1)
    top_k: int | None = Field(default=None, ge=1)


class CatalogBookHit(BaseModel):
    book_id: int
    score: float


class CatalogSearchResponse(BaseModel):
    hits: list[CatalogBookHit]


class DeleteIndexResponse(BaseModel):
    book_id: int
    deleted_chunks: int


class IndexStatusResponse(BaseModel):
    book_id: int
    status: Literal["indexed", "error", "not_found"]
    chunk_count: int | None = None
    updated_at: str | None = None
    error: str | None = None

