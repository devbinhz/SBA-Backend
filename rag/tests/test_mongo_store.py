from pathlib import Path

from src.ingestion import ExtractedImage, PageText, ParsedDocument, TextChunk
from src.services import MongoBookStore


class FakeCollection:
    def __init__(self):
        self.documents = {}
        self.indexes = []

    def create_index(self, index):
        self.indexes.append(index)

    def find(self, filter=None, projection=None):
        filter = filter or {}
        matches = [
            document
            for document in self.documents.values()
            if all(document.get(key) == value for key, value in filter.items())
        ]
        if projection == {"_id": 1}:
            return [{"_id": document["_id"]} for document in matches]
        return [dict(document) for document in matches]

    def find_one(self, filter):
        for document in self.find(filter):
            return document
        return None

    def replace_one(self, filter, document, upsert=False):
        key = filter["_id"]
        if key in self.documents or upsert:
            self.documents[key] = dict(document)

    def update_one(self, filter, update, upsert=False):
        key = filter["_id"]
        if key not in self.documents and upsert:
            self.documents[key] = {"_id": key}
        if key in self.documents:
            doc = self.documents[key]
            if "$set" in update:
                for k, v in update["$set"].items():
                    doc[k] = v

    def delete_many(self, filter):
        for key, document in list(self.documents.items()):
            if all(document.get(field) == value for field, value in filter.items()):
                self.documents.pop(key)

    def insert_many(self, documents):
        for document in documents:
            self.documents[document["_id"]] = dict(document)


class FakeDatabase:
    def __init__(self):
        self.collections = {}
        self.pinged = False

    def __getitem__(self, name):
        return self.collections.setdefault(name, FakeCollection())

    def command(self, command):
        if command == "ping":
            self.pinged = True
            return {"ok": 1}
        raise ValueError(command)


def test_mongo_store_writes_indexed_document_with_full_text(tmp_path: Path):
    book_path = tmp_path / "Sample.epub"
    book_path.write_text("content", encoding="utf-8")
    database = FakeDatabase()
    store = MongoBookStore(database=database)
    parsed = ParsedDocument(
        document_name="Sample",
        file_name="Sample.epub",
        file_type="epub",
        pages=[
            PageText(text="alpha", page=1),
            PageText(text="beta", page=2),
        ],
        images=[
            ExtractedImage(
                id="image-1",
                document_name="Sample",
                file_name="Sample.epub",
                file_type="epub",
                image_index=0,
                data=b"image-bytes",
                mime_type="image/png",
                extension="png",
                source_path="images/pixel.png",
                checksum="checksum",
            )
        ],
    )
    chunk = TextChunk(
        id="chunk-1",
        book_id=1,
        document_name="Sample",
        file_name="Sample.epub",
        file_type="epub",
        chunk_index=0,
        text="alpha beta",
        page=1,
    )

    book_id = store.mark_indexed(book_id=1, path=book_path, parsed=parsed, chunks=[chunk])

    saved_book = store.get_book(book_id)
    saved_chunk = store.get_chunk("chunk-1")
    assert saved_book is not None
    assert saved_book["document_name"] == "Sample"
    assert saved_book["full_text"] == "alpha\n\nbeta"
    assert saved_book["chunk_count"] == 1
    assert saved_book["image_count"] == 1
    assert "error" not in saved_book
    assert saved_chunk is not None
    assert saved_chunk["text"] == "alpha beta"
    assert saved_chunk["book_id"] == book_id
    saved_image = store.get_image("image-1")
    assert saved_image is not None
    assert saved_image["data"] == b"image-bytes"
    assert saved_image["book_id"] == book_id
    assert saved_image["source_path"] == "images/pixel.png"


def test_mongo_store_replaces_chunks_for_existing_book(tmp_path: Path):
    book_path = tmp_path / "Sample.epub"
    book_path.write_text("content", encoding="utf-8")
    store = MongoBookStore(database=FakeDatabase())
    parsed = ParsedDocument(
        document_name="Sample",
        file_name="Sample.epub",
        file_type="epub",
        pages=[PageText(text="alpha beta", page=1)],
    )
    first = TextChunk(
        id="chunk-1",
        book_id=1,
        document_name="Sample",
        file_name="Sample.epub",
        file_type="epub",
        chunk_index=0,
        text="alpha",
        page=1,
    )
    second = TextChunk(
        id="chunk-2",
        book_id=1,
        document_name="Sample",
        file_name="Sample.epub",
        file_type="epub",
        chunk_index=0,
        text="beta",
        page=1,
    )
    image = ExtractedImage(
        id="image-1",
        document_name="Sample",
        file_name="Sample.epub",
        file_type="epub",
        image_index=0,
        data=b"old-image",
    )
    parsed_with_image = ParsedDocument(
        document_name="Sample",
        file_name="Sample.epub",
        file_type="epub",
        pages=[PageText(text="alpha beta", page=1)],
        images=[image],
    )

    book_id = store.mark_indexed(book_id=1, path=book_path, parsed=parsed_with_image, chunks=[first])
    store.mark_indexed(book_id=1, path=book_path, parsed=parsed, chunks=[second])

    assert store.chunk_ids_for_book(book_id) == ["chunk-2"]
    assert store.get_chunk("chunk-1") is None
    assert store.get_chunk("chunk-2")["text"] == "beta"
    assert store.get_image("image-1") is None
