from __future__ import annotations

import hashlib
import json
import re
import unicodedata
import uuid
from dataclasses import dataclass, replace
from pathlib import Path

import fitz
from bs4 import BeautifulSoup
from ebooklib import ITEM_DOCUMENT, ITEM_IMAGE
from ebooklib import epub

from src.config import settings
from src.schemas import IndexedDocument, IngestError, IngestResponse, IngestItem


SUPPORTED_SUFFIXES = {".pdf", ".epub"}
TOKEN_RE = re.compile(r"\S+")
SOURCE_TAG_RE = re.compile(
    r"\s*[\(\[][^)\]]*(?:z-library|z-lib|1lib|libgen)[^)\]]*[\)\]]\s*",
    re.IGNORECASE,
)
WHITESPACE_RE = re.compile(r"\s+")
UNSAFE_KEY_RE = re.compile(r"[^A-Za-z0-9._-]+")
UNDERSCORE_RE = re.compile(r"_+")
TRAILING_COPY_RE = re.compile(r"[-_]\d+$")
KEY_TOKEN_RE = re.compile(r"[a-z0-9]+")


@dataclass(frozen=True)
class PageText:
    text: str
    page: int | None = None


@dataclass(frozen=True)
class ExtractedImage:
    id: str
    document_name: str
    file_name: str
    file_type: str
    image_index: int
    data: bytes
    mime_type: str | None = None
    extension: str | None = None
    page: int | None = None
    source_path: str | None = None
    width: int | None = None
    height: int | None = None
    checksum: str | None = None

    def payload(self) -> dict[str, object]:
        return _without_none(
            {
                "document_name": self.document_name,
                "file_name": self.file_name,
                "file_type": self.file_type,
                "image_index": self.image_index,
                "mime_type": self.mime_type,
                "extension": self.extension,
                "page": self.page,
                "source_path": self.source_path,
                "width": self.width,
                "height": self.height,
                "size_bytes": len(self.data),
                "checksum": self.checksum,
                "data": self.data,
            }
        )


@dataclass(frozen=True)
class ParsedDocument:
    document_name: str
    file_name: str
    file_type: str
    pages: list[PageText]
    images: list[ExtractedImage] | None = None


@dataclass(frozen=True)
class TextChunk:
    id: str
    book_id: int
    document_name: str
    file_name: str
    file_type: str
    chunk_index: int
    text: str
    page: int | None = None

    def payload(self) -> dict[str, object]:
        return _without_none(
            {
                "book_id": self.book_id,
                "document_name": self.document_name,
                "file_name": self.file_name,
                "file_type": self.file_type,
                "chunk_index": self.chunk_index,
                "page": self.page,
                "text": self.text,
            }
        )


class IngestionPipeline:
    def __init__(
        self,
        openai_service: object | None = None,
        manifest: object | None = None,
        store: object | None = None,
        storage: object | None = None,
    ) -> None:
        from src.services import FakeOpenAIService, MongoBookStore, QdrantStore
        from src.storage import MinioBookStorage

        self.openai_service = openai_service or FakeOpenAIService()
        self.manifest = manifest or MongoBookStore()
        self.store = store or QdrantStore()
        self.storage = storage or MinioBookStorage()

    def ingest(self, items: list[IngestItem]) -> IngestResponse:
        response = IngestResponse()
        if not items:
            return response

        self.store.ensure_collection()
        self.manifest.ensure_indexes()

        for item in items:
            book_id = item.book_id
            path = None
            try:
                path = self.storage.download_book(item.file_path)
            except Exception:
                response.errors.append(IngestError(book_id=book_id, error="document_not_found"))
                continue

            try:
                old_chunk_ids = self.manifest.chunk_ids_for_book(book_id)
                parsed = parse_book(path)
                if item.title:
                    document_name = sanitize_document_name(item.title)
                else:
                    document_name = parsed.document_name

                images = [
                    replace(image, document_name=document_name, file_name=item.file_path)
                    for image in parsed.images or []
                ]
                parsed = replace(
                    parsed,
                    document_name=document_name,
                    file_name=item.file_path,
                    images=images,
                )

                self.store.delete_points(old_chunk_ids)

                chunks = chunk_document(parsed, book_id)
                vectors = self.openai_service.embed_texts([chunk.text for chunk in chunks])
                self.store.upsert_chunks(chunks, vectors)
                self.manifest.mark_indexed(book_id=book_id, path=path, parsed=parsed, chunks=chunks)

                response.indexed.append(
                    IndexedDocument(
                        book_id=book_id,
                        file_name=parsed.file_name,
                        chunks=len(chunks),
                    )
                )
                response.total_chunks += len(chunks)
            except Exception as exc:
                error = str(exc)
                self.manifest.mark_error(book_id=book_id, error=error)
                response.errors.append(IngestError(book_id=book_id, error=error))
            finally:
                if path and path.exists():
                    try:
                        path.unlink()
                    except Exception:
                        pass

        return response


def parse_book(path: Path) -> ParsedDocument:
    suffix = path.suffix.lower()
    if suffix == ".pdf":
        return parse_pdf(path)
    if suffix == ".epub":
        return parse_epub(path)
    raise ValueError(f"Unsupported book type: {path.suffix}")


def parse_pdf(path: Path) -> ParsedDocument:
    pages: list[PageText] = []

    with fitz.open(path) as document:
        for index, page in enumerate(document, start=1):
            text = page.get_text("text").strip()
            pages.append(PageText(text=text, page=index))

    return ParsedDocument(
        document_name=sanitize_document_name(path.stem),
        file_name=path.name,
        file_type="pdf",
        pages=pages,
        images=extract_pdf_images(path) if _should_extract_images() else [],
    )


def parse_epub(path: Path) -> ParsedDocument:
    book = epub.read_epub(str(path))
    pages: list[PageText] = []

    virtual_page = 1
    for item in _ordered_document_items(book):
        html = item.get_content()
        soup = BeautifulSoup(html, "html.parser")

        for tag in soup(["script", "style", "nav"]):
            tag.decompose()

        text = soup.get_text(separator=" ", strip=True)
        if text:
            for page_text in _split_virtual_pages(text):
                pages.append(PageText(text=page_text, page=virtual_page))
                virtual_page += 1

    return ParsedDocument(
        document_name=sanitize_document_name(path.stem),
        file_name=path.name,
        file_type="epub",
        pages=pages,
        images=extract_epub_images(path) if _should_extract_images() else [],
    )


def extract_pdf_images(path: Path) -> list[ExtractedImage]:
    document_name = sanitize_document_name(path.stem)
    images: list[ExtractedImage] = []

    with fitz.open(path) as document:
        for page_number, page in enumerate(document, start=1):
            for page_image_index, image_ref in enumerate(page.get_images(full=True), start=1):
                xref = int(image_ref[0])
                image_info = document.extract_image(xref)
                data = image_info.get("image", b"")
                if not data:
                    continue
                extension = _normalized_extension(str(image_info.get("ext") or ""))
                checksum = _checksum(data)
                image_index = len(images)
                images.append(
                    ExtractedImage(
                        id=_image_id(path.name, image_index, checksum),
                        document_name=document_name,
                        file_name=path.name,
                        file_type="pdf",
                        image_index=image_index,
                        data=data,
                        mime_type=_mime_type(extension),
                        extension=extension,
                        page=page_number,
                        source_path=f"page:{page_number}:xref:{xref}:image:{page_image_index}",
                        width=_optional_int(image_info.get("width")),
                        height=_optional_int(image_info.get("height")),
                        checksum=checksum,
                    )
                )

    return images


def _should_extract_images() -> bool:
    return bool(getattr(settings, "extract_images", False))


def extract_epub_images(path: Path) -> list[ExtractedImage]:
    document_name = sanitize_document_name(path.stem)
    book = epub.read_epub(str(path))
    images: list[ExtractedImage] = []

    for item in book.get_items_of_type(ITEM_IMAGE):
        data = item.get_content()
        if not data:
            continue
        name = _item_name(item)
        extension = _extension_from_name(name) or _extension_from_bytes(data)
        checksum = _checksum(data)
        image_index = len(images)
        images.append(
            ExtractedImage(
                id=_image_id(path.name, image_index, checksum),
                document_name=document_name,
                file_name=path.name,
                file_type="epub",
                image_index=image_index,
                data=data,
                mime_type=_item_media_type(item) or _mime_type(extension),
                extension=extension,
                source_path=name,
                checksum=checksum,
            )
        )

    return images


def chunk_document(document: ParsedDocument, book_id: int) -> list[TextChunk]:
    token_stream: list[tuple[str, int | None]] = []
    for page in document.pages:
        token_stream.extend(
            (match.group(0), page.page) for match in TOKEN_RE.finditer(page.text)
        )

    if not token_stream:
        return []

    chunks: list[TextChunk] = []
    step = max(1, settings.chunk_target_tokens - settings.chunk_overlap_tokens)
    start = 0

    while start < len(token_stream):
        end = min(start + settings.chunk_target_tokens, len(token_stream))
        window = token_stream[start:end]
        text = " ".join(token for token, _page in window).strip()
        page = _dominant_page(window)
        chunk_index = len(chunks)
        chunks.append(
            TextChunk(
                id=_chunk_id(document.file_name, chunk_index, text),
                book_id=book_id,
                document_name=document.document_name,
                file_name=document.file_name,
                file_type=document.file_type,
                chunk_index=chunk_index,
                page=page,
                text=text,
            )
        )

        if end == len(token_stream):
            break
        start += step

    return chunks


def sanitize_document_name(name: str) -> str:
    normalized = unicodedata.normalize("NFKC", name)
    normalized = SOURCE_TAG_RE.sub(" ", normalized)
    normalized = normalized.replace("_", " ")
    normalized = WHITESPACE_RE.sub(" ", normalized)
    normalized = normalized.strip(" .-_")
    return normalized or "Untitled"





def _ordered_document_items(book: object) -> list[object]:
    ordered = []
    seen_ids: set[str] = set()

    for item_ref in book.spine:
        item_id = item_ref[0] if isinstance(item_ref, tuple) else item_ref
        item = book.get_item_with_id(item_id)
        if item and item.get_type() == ITEM_DOCUMENT:
            ordered.append(item)
            seen_ids.add(item.get_id())

    for item in book.get_items_of_type(ITEM_DOCUMENT):
        if item.get_id() not in seen_ids:
            ordered.append(item)

    return ordered


def _split_virtual_pages(text: str) -> list[str]:
    normalized = " ".join(text.split())
    if not normalized:
        return []

    page_size = max(1, settings.epub_page_size_chars)
    pages: list[str] = []
    start = 0
    while start < len(normalized):
        end = min(start + page_size, len(normalized))
        if end < len(normalized):
            boundary = normalized.rfind(" ", start, end)
            if boundary > start:
                end = boundary
        pages.append(normalized[start:end].strip())
        start = end
        while start < len(normalized) and normalized[start].isspace():
            start += 1
    return pages


def _dominant_page(window: list[tuple[str, int | None]]) -> int | None:
    counts: dict[int, int] = {}
    for _token, page in window:
        if page is not None:
            counts[page] = counts.get(page, 0) + 1
    if not counts:
        return None
    return max(counts.items(), key=lambda item: item[1])[0]


def _chunk_id(file_name: str, chunk_index: int, text: str) -> str:
    digest = hashlib.sha1(f"{file_name}:{chunk_index}:{text}".encode("utf-8")).hexdigest()
    return str(uuid.uuid5(uuid.NAMESPACE_URL, digest))


def _image_id(file_name: str, image_index: int, checksum: str) -> str:
    digest = hashlib.sha1(
        f"{file_name}:{image_index}:{checksum}".encode("utf-8")
    ).hexdigest()
    return str(uuid.uuid5(uuid.NAMESPACE_URL, digest))


def _checksum(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _item_name(item: object) -> str:
    get_name = getattr(item, "get_name", None)
    if callable(get_name):
        name = get_name()
        if name:
            return str(name)
    get_id = getattr(item, "get_id", None)
    if callable(get_id):
        item_id = get_id()
        if item_id:
            return str(item_id)
    return "image"


def _item_media_type(item: object) -> str | None:
    media_type = getattr(item, "media_type", None)
    return str(media_type) if media_type else None


def _extension_from_name(name: str) -> str | None:
    suffix = Path(name).suffix.lower().lstrip(".")
    return _normalized_extension(suffix) if suffix else None


def _extension_from_bytes(data: bytes) -> str | None:
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return "png"
    if data.startswith(b"\xff\xd8\xff"):
        return "jpg"
    if data.startswith(b"GIF87a") or data.startswith(b"GIF89a"):
        return "gif"
    if data.startswith(b"RIFF") and data[8:12] == b"WEBP":
        return "webp"
    if data.lstrip().startswith(b"<svg"):
        return "svg"
    return None


def _normalized_extension(extension: str | None) -> str | None:
    if not extension:
        return None
    extension = extension.lower().lstrip(".")
    if extension == "jpeg":
        return "jpg"
    return extension or None


def _mime_type(extension: str | None) -> str | None:
    if not extension:
        return None
    if extension == "jpg":
        return "image/jpeg"
    if extension == "svg":
        return "image/svg+xml"
    return f"image/{extension}"


def _optional_int(value: object) -> int | None:
    if value is None:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


def _without_none(payload: dict[str, object | None]) -> dict[str, object]:
    return {key: value for key, value in payload.items() if value is not None}
