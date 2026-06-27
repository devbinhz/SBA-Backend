from __future__ import annotations

import hashlib
import json
import math
import time
import urllib.error
import urllib.request
import uuid
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from qdrant_client import QdrantClient
from qdrant_client.http import models

from src.config import settings
from src.ingestion import ExtractedImage, ParsedDocument, TextChunk, sanitize_document_name
from src.schemas import SearchHit, Source, Usage


def _call_openai_api(url: str, api_key: str, payload: dict) -> dict:
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}"
    }
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers=headers,
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        error_body = e.read().decode("utf-8")
        raise RuntimeError(f"OpenAI call failed: {e.code} {e.reason} - {error_body}")
    except Exception as e:
        raise RuntimeError(f"OpenAI call failed: {str(e)}")


class FakeOpenAIService:
    def __init__(
        self,
        embedding_model: str | None = None,
        chat_model: str | None = None,
        dimensions: int | None = None,
    ) -> None:
        self.embedding_model = embedding_model or settings.fake_embedding_model
        self.chat_model = chat_model or settings.fake_chat_model
        self.dimensions = dimensions or settings.embedding_dimension
        self.api_key = settings.openai_api_key

    def embed_texts(self, texts: list[str], dimensions: int | None = None) -> list[list[float]]:
        if self.api_key:
            payload = {
                "input": texts,
                "model": "text-embedding-3-small"
            }
            if dimensions:
                payload["dimensions"] = dimensions
            try:
                response = _call_openai_api("https://api.openai.com/v1/embeddings", self.api_key, payload)
                return [data["embedding"] for data in response["data"]]
            except Exception:
                pass
        size = dimensions or self.dimensions
        return [_fake_embedding(text, size) for text in texts]

    def embed_text(self, text: str, dimensions: int | None = None) -> list[float]:
        return self.embed_texts([text], dimensions)[0]

    def embeddings_response(
        self,
        input_value: str | list[str],
        model: str | None = None,
        dimensions: int | None = None,
    ) -> dict[str, Any]:
        texts = [input_value] if isinstance(input_value, str) else input_value
        size = dimensions or self.dimensions
        vectors = self.embed_texts(texts, size)
        prompt_tokens = sum(estimate_tokens(text) for text in texts)

        return {
            "object": "list",
            "data": [
                {
                    "object": "embedding",
                    "embedding": vector,
                    "index": index,
                }
                for index, vector in enumerate(vectors)
            ],
            "model": model or self.embedding_model,
            "usage": {
                "prompt_tokens": prompt_tokens,
                "total_tokens": prompt_tokens,
            },
        }

    def make_answer(self, query: str, sources: list[Source]) -> tuple[str, Usage]:
        if not sources:
            return "I could not find relevant indexed book context for that question.", Usage(prompt_tokens=0, completion_tokens=0, total_tokens=0)

        if self.api_key:
            context_parts = []
            for index, source in enumerate(sources, start=1):
                location = f"page {source.page}" if source.page is not None else "no page"
                context_parts.append(
                    f"Source [{index}]: File: {source.file_name} ({location})\nContent: {source.text}"
                )
            context_str = "\n\n".join(context_parts)

            system_prompt = (
                "You are a helpful book assistant. Answer the user's question based strictly on the provided book source chunks.\n"
                "Provide the sources used (e.g. [1], [2]) in your response where appropriate.\n\n"
                f"Provided book context:\n{context_str}"
            )
            payload = {
                "model": "gpt-4o-mini",
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": query}
                ],
                "temperature": 0.3
            }
            try:
                response = _call_openai_api("https://api.openai.com/v1/chat/completions", self.api_key, payload)
                answer = response["choices"][0]["message"]["content"]
                usage_data = response.get("usage", {})
                prompt_tokens = usage_data.get("prompt_tokens", 0)
                completion_tokens = usage_data.get("completion_tokens", 0)
                total_tokens = usage_data.get("total_tokens", 0)
                return answer, Usage(prompt_tokens=prompt_tokens, completion_tokens=completion_tokens, total_tokens=total_tokens)
            except Exception:
                pass

        prompt_tokens = estimate_tokens(query) + sum(
            estimate_tokens(source.text) for source in sources
        )
        source_lines = []
        for index, source in enumerate(sources, start=1):
            location = f"page {source.page}" if source.page is not None else "no page"
            source_lines.append(
                f"[{index}] {source.document_name} ({location}): {_preview(source.text)}"
            )
        answer = (
            "Fake OpenAI chat response based on retrieved book chunks.\n\n"
            f"Question: {query}\n\n"
            "Relevant sources:\n"
            + "\n".join(source_lines)
        )
        completion_tokens = estimate_tokens(answer)
        return (
            answer,
            Usage(
                prompt_tokens=prompt_tokens,
                completion_tokens=completion_tokens,
                total_tokens=prompt_tokens + completion_tokens,
            ),
        )

    def chat_completion_response(
        self,
        messages: list[dict[str, Any]],
        answer: str,
        usage: Usage,
        model: str | None = None,
    ) -> dict[str, Any]:
        return {
            "id": f"chatcmpl_fake_{int(time.time() * 1000)}",
            "object": "chat.completion",
            "created": int(time.time()),
            "model": model or self.chat_model,
            "choices": [
                {
                    "index": 0,
                    "message": {"role": "assistant", "content": answer},
                    "finish_reason": "stop",
                }
            ],
            "usage": usage.model_dump(),
        }


class MongoBookStore:
    def __init__(
        self,
        *,
        client: Any | None = None,
        database: Any | None = None,
        mongo_url: str | None = None,
        database_name: str | None = None,
        books_collection: str | None = None,
        chunks_collection: str | None = None,
        images_collection: str | None = None,
    ) -> None:
        self.database_name = database_name or settings.mongo_database
        self.books_collection_name = books_collection or settings.mongo_books_collection
        self.chunks_collection_name = chunks_collection or settings.mongo_chunks_collection
        self.images_collection_name = images_collection or settings.mongo_images_collection
        self._client = client

        if database is not None:
            self.database = database
        else:
            if self._client is None:
                try:
                    from pymongo import MongoClient
                except ImportError as exc:  # pragma: no cover - environment guard
                    raise RuntimeError(
                        "pymongo is required for MongoBookStore. Run `uv sync` first."
                    ) from exc
                self._client = MongoClient(
                    mongo_url or settings.mongo_url,
                    serverSelectionTimeoutMS=settings.mongo_timeout_ms,
                )
            self.database = self._client[self.database_name]

        self.books = self.database[self.books_collection_name]
        self.chunks = self.database[self.chunks_collection_name]
        self.images = self.database[self.images_collection_name]

    def is_available(self) -> bool:
        try:
            self.database.command("ping")
            return True
        except Exception:
            return False

    def ensure_indexes(self) -> None:
        self.books.create_index("document_name")
        self.chunks.create_index("book_id")
        self.chunks.create_index([("document_name", 1), ("chunk_index", 1)])
        self.images.create_index("book_id")
        self.images.create_index([("document_name", 1), ("image_index", 1)])

    def chunk_ids_for_book(self, book_id: int) -> list[str]:
        return [
            str(chunk["_id"])
            for chunk in self.chunks.find({"book_id": book_id}, {"_id": 1})
            if "_id" in chunk
        ]

    def mark_indexed(
        self,
        *,
        book_id: int,
        path: Path,
        parsed: ParsedDocument,
        chunks: list[TextChunk],
    ) -> int:
        self._replace_book(
            path=path,
            parsed=parsed,
            book_id=book_id,
            status="indexed",
            chunks=chunks,
            error=None,
        )
        return book_id

    def mark_error(self, *, book_id: int, error: str) -> int:
        self.books.update_one(
            {"_id": book_id},
            {
                "$set": {
                    "status": "error",
                    "error": error,
                    "updated_at": _now(),
                }
            },
            upsert=True,
        )
        return book_id

    def get_chunk(self, chunk_id: str) -> dict[str, Any] | None:
        chunk = self.chunks.find_one({"_id": chunk_id})
        return _with_id(chunk)

    def get_book(self, book_id: int) -> dict[str, Any] | None:
        book = self.books.find_one({"_id": book_id})
        return _with_id(book)

    def get_image(self, image_id: str) -> dict[str, Any] | None:
        image = self.images.find_one({"_id": image_id})
        return _with_id(image)

    def delete_book(self, book_id: int) -> None:
        self.books.delete_many({"_id": book_id})
        self.chunks.delete_many({"book_id": book_id})
        self.images.delete_many({"book_id": book_id})

    def _replace_book(
        self,
        *,
        path: Path,
        parsed: ParsedDocument,
        book_id: int,
        status: str,
        chunks: list[TextChunk],
        error: str | None,
    ) -> None:
        self.chunks.delete_many({"book_id": book_id})
        self.images.delete_many({"book_id": book_id})

        page_numbers = [page.page for page in parsed.pages if page.page is not None]
        images = parsed.images or []
        book_record = {
            "_id": book_id,
            "document_name": parsed.document_name,
            "file_name": parsed.file_name,
            "file_type": parsed.file_type,
            "status": status,
            "size_bytes": path.stat().st_size if path.exists() else None,
            "page_count": len(set(page_numbers)),
            "chunk_count": len(chunks),
            "image_count": len(images),
            "full_text": _full_text(parsed),
            "updated_at": _now(),
        }
        if error is not None:
            book_record["error"] = error
        self.books.replace_one({"_id": book_id}, book_record, upsert=True)

        chunk_records = [
            {
                **chunk.payload(),
                "_id": chunk.id,
                "book_id": book_id,
                "preview": _preview(chunk.text),
                "updated_at": _now(),
            }
            for chunk in chunks
        ]
        if chunk_records:
            self.chunks.insert_many(chunk_records)

        image_records = [_image_record(image, book_id) for image in images]
        if image_records:
            self.images.insert_many(image_records)


class QdrantStore:
    def __init__(
        self,
        client: QdrantClient | None = None,
        collection_name: str | None = None,
    ) -> None:
        self.client = client or QdrantClient(
            url=settings.qdrant_url,
            timeout=settings.qdrant_timeout_seconds,
        )
        self.collection_name = collection_name or settings.qdrant_collection

    def is_available(self) -> bool:
        try:
            self.client.get_collections()
            return True
        except Exception:
            return False

    def ensure_collection(self) -> None:
        if self._collection_exists():
            return
        self.client.create_collection(
            collection_name=self.collection_name,
            vectors_config=models.VectorParams(
                size=settings.embedding_dimension,
                distance=models.Distance.COSINE,
            ),
        )
        self.client.create_payload_index(
            collection_name=self.collection_name,
            field_name="book_id",
            field_schema=models.PayloadSchemaType.INTEGER,
        )

    def delete_points(self, point_ids: list[str]) -> None:
        if not point_ids or not self._collection_exists():
            return
        for batch in _batched(point_ids, settings.qdrant_delete_batch_size):
            self.client.delete(
                collection_name=self.collection_name,
                points_selector=models.PointIdsList(points=batch),
                wait=True,
            )

    def upsert_chunks(self, chunks: list[TextChunk], vectors: list[list[float]]) -> None:
        if len(chunks) != len(vectors):
            raise ValueError("chunks and vectors must have the same length")
        if not chunks:
            return

        for batch in _batched(
            list(zip(chunks, vectors, strict=True)),
            settings.qdrant_upsert_batch_size,
        ):
            points = [
                models.PointStruct(
                    id=chunk.id,
                    vector=vector,
                    payload=chunk.payload(),
                )
                for chunk, vector in batch
            ]
            self.client.upsert(
                collection_name=self.collection_name,
                points=points,
                wait=True,
            )

    def search(
        self,
        vector: list[float],
        limit: int,
        book_ids: list[int] | None = None,
    ) -> list[SearchHit]:
        query_filter = None
        if book_ids:
            query_filter = models.Filter(
                must=[
                    models.FieldCondition(
                        key="book_id",
                        match=models.MatchAny(any=book_ids),
                    )
                ]
            )
        response = self.client.query_points(
            collection_name=self.collection_name,
            query=vector,
            query_filter=query_filter,
            limit=limit,
            with_payload=True,
        )
        return [
            SearchHit(
                id=str(result.id),
                score=float(result.score),
                payload=_payload(result.payload),
            )
            for result in response.points
        ]

    def _collection_exists(self) -> bool:
        try:
            self.client.get_collection(self.collection_name)
            return True
        except Exception:
            return False


def estimate_tokens(text: str) -> int:
    return max(1, math.ceil(len(text.split()) * 1.35)) if text else 0


def _fake_embedding(text: str, dimensions: int) -> list[float]:
    if dimensions <= 0:
        raise ValueError("dimensions must be positive")

    vector: list[float] = []
    counter = 0
    while len(vector) < dimensions:
        digest = hashlib.sha256(f"{counter}:{text}".encode("utf-8")).digest()
        for byte in digest:
            vector.append((byte / 127.5) - 1.0)
            if len(vector) == dimensions:
                break
        counter += 1

    norm = math.sqrt(sum(value * value for value in vector))
    if norm == 0:
        return [0.0 for _ in vector]
    return [value / norm for value in vector]


def _image_record(image: ExtractedImage, book_id: int) -> dict[str, object]:
    return {
        **image.payload(),
        "_id": image.id,
        "book_id": book_id,
        "updated_at": _now(),
    }


def _full_text(parsed: ParsedDocument) -> str:
    return "\n\n".join(page.text.strip() for page in parsed.pages if page.text.strip())


def _with_id(document: dict[str, Any] | None) -> dict[str, Any] | None:
    if document is None:
        return None
    normalized = dict(document)
    if "_id" in normalized:
        normalized["id"] = str(normalized["_id"])
    return normalized


def _now() -> str:
    return datetime.now(UTC).isoformat()


def _preview(text: str, limit: int = 280) -> str:
    normalized = " ".join(text.split())
    if len(normalized) <= limit:
        return normalized
    return normalized[: limit - 3].rstrip() + "..."


def _payload(payload: Any) -> dict[str, object]:
    return payload if isinstance(payload, dict) else {}


def _batched[T](items: list[T], size: int) -> list[list[T]]:
    batch_size = max(1, size)
    return [items[index : index + batch_size] for index in range(0, len(items), batch_size)]
