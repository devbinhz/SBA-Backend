from __future__ import annotations

import uvicorn
from fastapi import Body, Depends, FastAPI

from src.ingestion import IngestionPipeline
from src.rag_engine import RagEngine
from src.catalog_engine import CatalogEngine
from src.config import settings
from src.schemas import (
    HealthResponse,
    IngestRequest,
    IngestResponse,
    QueryRequest,
    QueryResponse,
    CatalogUpsertRequest,
    CatalogUpsertResponse,
    CatalogSearchRequest,
    CatalogSearchResponse,
    DeleteIndexResponse,
    IndexStatusResponse,
)
from src.services import FakeOpenAIService, MongoBookStore, QdrantStore
from src.storage import MinioBookStorage


def get_manifest() -> MongoBookStore:
    return MongoBookStore()


def get_store() -> QdrantStore:
    return QdrantStore()


def get_openai_service() -> FakeOpenAIService:
    return FakeOpenAIService()


def get_rag_engine(
    openai_service: FakeOpenAIService = Depends(get_openai_service),
    manifest: MongoBookStore = Depends(get_manifest),
    store: QdrantStore = Depends(get_store),
) -> RagEngine:
    return RagEngine(openai_service=openai_service, manifest=manifest, store=store)


def get_catalog_engine(
    openai_service: FakeOpenAIService = Depends(get_openai_service),
    store: QdrantStore = Depends(get_store),
) -> CatalogEngine:
    catalog_store = QdrantStore(client=store.client, collection_name=settings.qdrant_catalog_collection)
    return CatalogEngine(openai_service=openai_service, store=catalog_store)


def get_storage() -> MinioBookStorage:
    return MinioBookStorage()


def get_ingestion_pipeline(
    openai_service: FakeOpenAIService = Depends(get_openai_service),
    manifest: MongoBookStore = Depends(get_manifest),
    store: QdrantStore = Depends(get_store),
    storage: MinioBookStorage = Depends(get_storage),
) -> IngestionPipeline:
    return IngestionPipeline(
        openai_service=openai_service,
        manifest=manifest,
        store=store,
        storage=storage,
    )


def create_app() -> FastAPI:
    app = FastAPI(title="Books RAG", version="0.1.0")

    @app.get("/health", response_model=HealthResponse)
    def health(
        manifest: MongoBookStore = Depends(get_manifest),
        store: QdrantStore = Depends(get_store),
        storage: MinioBookStorage = Depends(get_storage),
    ) -> HealthResponse:
        return HealthResponse(
            status="ok",
            qdrant="ok" if store.is_available() else "unavailable",
            mongo="ok" if manifest.is_available() else "unavailable",
            minio="ok" if storage.is_available() else "unavailable",
            mongo_database=manifest.database_name,
            books_collection=manifest.books_collection_name,
            chunks_collection=manifest.chunks_collection_name,
            images_collection=manifest.images_collection_name,
            collection=store.collection_name,
        )

    @app.post("/ingest", response_model=IngestResponse)
    def ingest(
        request: IngestRequest,
        pipeline: IngestionPipeline = Depends(get_ingestion_pipeline),
    ) -> IngestResponse:
        return pipeline.ingest(items=request.items)

    @app.post("/query", response_model=QueryResponse)
    def query(
        request: QueryRequest,
        engine: RagEngine = Depends(get_rag_engine),
    ) -> QueryResponse:
        return engine.query(
            query=request.query,
            book_ids=request.book_ids,
            top_k=request.top_k,
        )

    @app.post("/catalog/upsert", response_model=CatalogUpsertResponse)
    def catalog_upsert(
        request: CatalogUpsertRequest,
        engine: CatalogEngine = Depends(get_catalog_engine),
    ) -> CatalogUpsertResponse:
        return engine.upsert(items=request.items)

    @app.post("/catalog/search", response_model=CatalogSearchResponse)
    def catalog_search(
        request: CatalogSearchRequest,
        engine: CatalogEngine = Depends(get_catalog_engine),
    ) -> CatalogSearchResponse:
        return engine.search(query=request.query, top_k=request.top_k)

    @app.delete("/catalog/{book_id}")
    def delete_catalog(
        book_id: int,
        engine: CatalogEngine = Depends(get_catalog_engine),
    ) -> dict[str, str]:
        engine.delete(book_id=book_id)
        return {"status": "ok"}

    @app.delete("/index/{book_id}", response_model=DeleteIndexResponse)
    def delete_index(
        book_id: int,
        engine: RagEngine = Depends(get_rag_engine),
    ) -> DeleteIndexResponse:
        deleted_chunks = engine.delete(book_id=book_id)
        return DeleteIndexResponse(book_id=book_id, deleted_chunks=deleted_chunks)

    @app.get("/index/{book_id}/status", response_model=IndexStatusResponse)
    def index_status(
        book_id: int,
        engine: RagEngine = Depends(get_rag_engine),
    ) -> IndexStatusResponse:
        status_data = engine.get_index_status(book_id=book_id)
        return IndexStatusResponse(**status_data)

    return app


app = create_app()


def run() -> None:
    uvicorn.run("src.api:app", host="0.0.0.0", port=8000, reload=True)
