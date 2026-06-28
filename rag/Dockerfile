FROM python:3.13-slim

WORKDIR /app

RUN pip install uv

COPY pyproject.toml README.md .python-version ./

RUN uv sync

COPY main.py ./
COPY src/ ./src/

ENV PATH="/app/.venv/bin:$PATH"

EXPOSE 8000

CMD ["python", "main.py"]
