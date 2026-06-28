import base64
from pathlib import Path
from types import SimpleNamespace

import fitz
from ebooklib import epub

from src.ingestion import parse_epub, parse_pdf


PIXEL_PNG = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
)


def test_epub_parser_extracts_ordered_text(monkeypatch, tmp_path: Path):
    path = tmp_path / "sample.epub"
    book = epub.EpubBook()
    book.set_identifier("sample")
    book.set_title("Sample")
    book.set_language("en")

    chapter1 = epub.EpubHtml(title="Chapter 1", file_name="c1.xhtml", lang="en")
    chapter1.content = "<h1>First</h1><p>Hello EPUB.</p>"
    chapter2 = epub.EpubHtml(title="Chapter 2", file_name="c2.xhtml", lang="en")
    chapter2.content = "<h1>Second</h1><p>More text.</p>"
    image = epub.EpubItem(
        uid="pixel",
        file_name="images/pixel.png",
        media_type="image/png",
        content=PIXEL_PNG,
    )

    book.add_item(chapter1)
    book.add_item(chapter2)
    book.add_item(image)
    book.spine = ["nav", chapter1, chapter2]
    book.add_item(epub.EpubNcx())
    book.add_item(epub.EpubNav())
    epub.write_epub(str(path), book)

    import src.ingestion as ingestion_module

    monkeypatch.setattr(
        ingestion_module,
        "settings",
        SimpleNamespace(epub_page_size_chars=1800, extract_images=True),
    )
    parsed = parse_epub(path)

    text = " ".join(page.text for page in parsed.pages)
    assert "First" in text
    assert "Second" in text
    assert parsed.pages[0].page == 1
    assert parsed.images is not None
    assert len(parsed.images) == 1
    assert parsed.images[0].mime_type == "image/png"
    assert parsed.images[0].source_path == "images/pixel.png"


def test_epub_parser_assigns_virtual_pages(monkeypatch, tmp_path: Path):
    path = tmp_path / "sample.epub"
    book = epub.EpubBook()
    book.set_identifier("sample")
    book.set_title("Sample")
    book.set_language("en")

    chapter = epub.EpubHtml(title="Chapter", file_name="c1.xhtml", lang="en")
    chapter.content = "<h1>Chapter</h1><p>alpha beta gamma delta epsilon zeta eta theta</p>"

    book.add_item(chapter)
    book.spine = ["nav", chapter]
    book.add_item(epub.EpubNcx())
    book.add_item(epub.EpubNav())
    epub.write_epub(str(path), book)

    import src.ingestion as ingestion_module

    monkeypatch.setattr(
        ingestion_module,
        "settings",
        type("Settings", (), {"epub_page_size_chars": 20})(),
    )

    parsed = parse_epub(path)

    assert [page.page for page in parsed.pages] == list(range(1, len(parsed.pages) + 1))
    assert len(parsed.pages) > 1


def test_pdf_parser_extracts_page_text(monkeypatch, tmp_path: Path):
    path = tmp_path / "sample_ (z-library.sk, 1lib.sk, z-lib.sk).pdf"
    document = fitz.open()
    for _index in range(3):
        page = document.new_page()
        page.insert_textbox(
            fitz.Rect(72, 72, 500, 720),
            "This is enough searchable PDF text.\n" * 20,
        )
    document[0].insert_image(fitz.Rect(72, 72, 82, 82), stream=PIXEL_PNG)
    document.save(path)
    document.close()

    import src.ingestion as ingestion_module

    monkeypatch.setattr(
        ingestion_module,
        "settings",
        SimpleNamespace(extract_images=True),
    )
    parsed = parse_pdf(path)

    assert parsed.pages[0].page == 1
    assert parsed.document_name == "sample"
    assert "searchable PDF text" in parsed.pages[0].text
    assert parsed.images is not None
    assert len(parsed.images) == 1
    assert parsed.images[0].page == 1
    assert parsed.images[0].mime_type == "image/png"


def test_pdf_parser_keeps_low_text_pdf(tmp_path: Path):
    path = tmp_path / "scan.pdf"
    document = fitz.open()
    document.new_page()
    document.save(path)
    document.close()

    parsed = parse_pdf(path)

    assert len(parsed.pages) == 1
    assert parsed.pages[0].text == ""
