from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[1]


@dataclass(frozen=True)
class Settings:
    books_dir: Path = ROOT_DIR / "assets" / "books"
    minio_endpoint: str = os.getenv("MINIO_ENDPOINT", "localhost:9000")
    minio_access_key: str = os.getenv("MINIO_ACCESS_KEY", "minioadmin")
    minio_secret_key: str = os.getenv("MINIO_SECRET_KEY", "minioadmin")
    minio_books_bucket: str = os.getenv("MINIO_BOOKS_BUCKET", "bookverse-books")
    mongo_url: str = os.getenv("MONGO_URL", "mongodb://localhost:27017")
    mongo_database: str = os.getenv("MONGO_DATABASE", "books_rag")
    mongo_books_collection: str = os.getenv("MONGO_BOOKS_COLLECTION", "books")
    mongo_chunks_collection: str = os.getenv("MONGO_CHUNKS_COLLECTION", "chunks")
    mongo_images_collection: str = os.getenv("MONGO_IMAGES_COLLECTION", "images")
    mongo_timeout_ms: int = int(os.getenv("MONGO_TIMEOUT_MS", "1000"))
    qdrant_url: str = os.getenv("QDRANT_URL", "http://localhost:6333")
    qdrant_catalog_collection: str = os.getenv("QDRANT_CATALOG_COLLECTION", "book_catalog")
    qdrant_collection: str = os.getenv("QDRANT_COLLECTION", "books")
    qdrant_timeout_seconds: int = int(os.getenv("QDRANT_TIMEOUT_SECONDS", "120"))
    qdrant_upsert_batch_size: int = int(os.getenv("QDRANT_UPSERT_BATCH_SIZE", "64"))
    qdrant_delete_batch_size: int = int(os.getenv("QDRANT_DELETE_BATCH_SIZE", "256"))
    fake_embedding_model: str = os.getenv(
        "FAKE_EMBEDDING_MODEL", "text-embedding-3-small"
    )
    fake_chat_model: str = os.getenv("FAKE_CHAT_MODEL", "gpt-4o-mini")
    embedding_dimension: int = int(os.getenv("EMBEDDING_DIMENSION", "1536"))
    chunk_target_tokens: int = int(os.getenv("CHUNK_TARGET_TOKENS", "300"))
    chunk_overlap_tokens: int = int(os.getenv("CHUNK_OVERLAP_TOKENS", "100"))
    epub_page_size_chars: int = int(os.getenv("EPUB_PAGE_SIZE_CHARS", "1800"))
    extract_images: bool = os.getenv("EXTRACT_IMAGES", "false").lower() in {
        "1",
        "true",
        "yes",
        "on",
    }
    default_top_k: int = int(os.getenv("DEFAULT_TOP_K", "5"))
    max_top_k: int = int(os.getenv("MAX_TOP_K", "20"))
    openai_api_key: str = os.getenv("OPENAI_API_KEY", "")


settings = Settings()
