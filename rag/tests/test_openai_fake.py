from src.services import FakeOpenAIService
from src.schemas import Source


def test_fake_embeddings_response_matches_openai_shape():
    service = FakeOpenAIService(dimensions=8)

    response = service.embeddings_response("hello world")

    assert response["object"] == "list"
    assert response["data"][0]["object"] == "embedding"
    assert len(response["data"][0]["embedding"]) == 8
    assert response["usage"]["total_tokens"] >= 1


def test_fake_answer_uses_source_text_without_preview_field():
    service = FakeOpenAIService()
    source = Source(
        book_id=1,
        document_name="Sample",
        file_name="Sample.epub",
        file_type="epub",
        chunk_index=0,
        page=1,
        score=0.9,
        text="A useful source chunk.",
    )

    answer, usage = service.make_answer("What is inside?", [source])

    assert "A useful source chunk." in answer
    assert usage.total_tokens >= usage.prompt_tokens
