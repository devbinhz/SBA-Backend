from types import SimpleNamespace

from src.ingestion import TextChunk
from src.services import QdrantStore


class FakeQdrantClient:
    def __init__(self):
        self.deleted = []
        self.upserted = []

    def get_collection(self, collection_name):
        return SimpleNamespace()

    def create_collection(self, **kwargs):
        pass

    def create_payload_index(self, **kwargs):
        pass

    def delete(self, **kwargs):
        self.deleted.append(kwargs)

    def upsert(self, **kwargs):
        self.upserted.append(kwargs)

    def query_points(self, **kwargs):
        assert kwargs["query"] == [0.1, 0.2]
        return SimpleNamespace(
            points=[
                SimpleNamespace(
                    id="chunk-1",
                    score=0.75,
                    payload={
                        "book_id": 1,
                        "document_name": "Sample",
                        "file_name": "Sample.epub",
                        "file_type": "epub",
                        "chunk_index": 0,
                        "text": "chunk text",
                    },
                )
            ]
        )


def test_qdrant_store_uses_payload_metadata():
    client = FakeQdrantClient()
    store = QdrantStore(client=client, collection_name="books")
    chunk = TextChunk(
        id="chunk-1",
        book_id=1,
        document_name="Sample",
        file_name="Sample.epub",
        file_type="epub",
        chunk_index=0,
        text="chunk text",
    )

    store.delete_points(["old-chunk"])
    store.upsert_chunks([chunk], [[0.1, 0.2]])
    results = store.search([0.1, 0.2], limit=1, book_ids=[1])

    assert client.deleted
    point = client.upserted[0]["points"][0]
    assert point.id == "chunk-1"
    assert point.payload == {
        "book_id": 1,
        "document_name": "Sample",
        "file_name": "Sample.epub",
        "file_type": "epub",
        "chunk_index": 0,
        "text": "chunk text",
    }
    assert results[0].id == "chunk-1"
    assert results[0].payload["book_id"] == 1
