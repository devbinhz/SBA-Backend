from fastapi.testclient import TestClient

from src.api import create_app, get_ingestion_pipeline, get_manifest, get_store
from src.schemas import IndexedDocument, IngestResponse, SearchHit


class FakePipeline:
    def ingest(self, items=None):
        return IngestResponse(
            indexed=[
                IndexedDocument(
                    book_id=1,
                    file_name="Sample.epub",
                    chunks=1,
                )
            ],
            total_chunks=1,
        )


class FakeManifest:
    database_name = "books_rag"
    books_collection_name = "books"
    chunks_collection_name = "chunks"
    images_collection_name = "images"

    def is_available(self):
        return True

    def get_chunk(self, chunk_id):
        return {
            "id": chunk_id,
            "book_id": 1,
            "document_name": "Sample",
            "file_name": "Sample.epub",
            "file_type": "epub",
            "chunk_index": 0,
            "page": 1,
            "text": "A useful source chunk.",
        }


class FakeStore:
    collection_name = "books"

    def is_available(self):
        return True

    def search(self, vector, limit, book_ids=None):
        return [
            SearchHit(
                id="chunk-1",
                score=0.9,
                payload={
                    "book_id": 1,
                    "document_name": "Sample",
                    "file_name": "Sample.epub",
                    "file_type": "epub",
                    "chunk_index": 0,
                    "page": 1,
                    "text": "A useful source chunk.",
                },
            )
        ]


def test_api_smoke_endpoints():
    app = create_app()
    app.dependency_overrides[get_manifest] = lambda: FakeManifest()
    app.dependency_overrides[get_store] = lambda: FakeStore()
    app.dependency_overrides[get_ingestion_pipeline] = lambda: FakePipeline()
    client = TestClient(app)

    health = client.get("/health")
    assert health.status_code == 200
    assert health.json()["qdrant"] == "ok"
    assert health.json()["mongo"] == "ok"

    ingest = client.post("/ingest", json={"items": [{"book_id": 1, "file_path": "Sample.epub"}]})
    assert ingest.status_code == 200
    assert ingest.json()["total_chunks"] == 1

    query = client.post("/query", json={"query": "What is inside?", "book_ids": [1], "top_k": 1})
    assert query.status_code == 200
    assert query.json()["sources"][0]["file_name"] == "Sample.epub"
    assert query.json()["sources"][0]["book_id"] == 1

def test_v1_endpoints_are_not_public():
    app = create_app()
    app.dependency_overrides[get_manifest] = lambda: FakeManifest()
    app.dependency_overrides[get_store] = lambda: FakeStore()
    client = TestClient(app)

    assert client.post("/v1/embeddings", json={"input": "hello"}).status_code == 404
    assert client.post("/v1/chat/completions", json={"messages": []}).status_code == 404
