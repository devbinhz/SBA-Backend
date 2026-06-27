# Books RAG

Local FastAPI RAG service for books stored in `assets/books`.

## Run

Start Qdrant, MongoDB, and Mongo Express:

```powershell
docker compose up -d qdrant mongo mongo-express
```

Mongo Express is available at `http://localhost:8081`.

Install dependencies and start the API:

```powershell
uv sync
uv run main.py
```

The API exposes:

- `GET /health`
- `POST /ingest`
- `POST /query`

The embedding and chat services are fake internal adapters for local
development. They preserve enough behavior for ingestion and querying, but
should be replaced with real providers before evaluating retrieval quality.

## Runtime Storage

- MongoDB `books_rag.books`: generated book records with document status, file
  info, page count, chunk count, image count, full text, and last update time.
- MongoDB `books_rag.chunks`: chunk records keyed by chunk id. It stores full
  chunk text, preview, page, and book metadata for lookup after Qdrant search.
- MongoDB `books_rag.images`: extracted PDF/EPUB images keyed by image id. It
  stores binary image data plus metadata such as page, source path, mime type,
  extension, size, and checksum. Querying does not use these images yet.
- `data/qdrant`: Qdrant persisted vector storage.
- `data/mongo`: MongoDB persisted document storage.

Qdrant points store only the chunk id plus a lightweight `content` payload. The
API maps Qdrant point ids back to MongoDB chunks to build query sources.

EPUB files do not have fixed physical pages. The app creates virtual EPUB pages
using `EPUB_PAGE_SIZE_CHARS`, defaulting to `1800` characters per page. Override
it before ingesting if you want a different page size:

```powershell
$env:EPUB_PAGE_SIZE_CHARS = "2200"
uv run main.py
```
