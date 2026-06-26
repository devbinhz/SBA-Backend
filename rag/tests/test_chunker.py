from src.ingestion import PageText, ParsedDocument


def test_chunker_preserves_stable_metadata(monkeypatch):
    import src.ingestion as ingestion_module

    monkeypatch.setattr(
        ingestion_module,
        "settings",
        type("Settings", (), {"chunk_target_tokens": 4, "chunk_overlap_tokens": 1})(),
    )
    document = ParsedDocument(
        document_name="sample",
        file_name="sample.pdf",
        file_type="pdf",
        pages=[
            PageText(text="one two three four five", page=1),
            PageText(text="six seven eight", page=2),
        ],
    )

    chunks = ingestion_module.chunk_document(document, 1)

    assert [chunk.chunk_index for chunk in chunks] == [0, 1, 2]
    assert chunks[0].document_name == "sample"
    assert chunks[0].file_name == "sample.pdf"
    assert chunks[0].book_id == 1
    assert chunks[0].page == 1
    assert chunks[0].id
