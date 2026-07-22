from __future__ import annotations

import json
import os

DATABASE_TOOLS_DEFINITIONS = [
    {
        "type": "function",
        "function": {
            "name": "rag_catalog",
            "description": (
                "Perform a semantic vector search on the book catalog to find books relevant to a given topic, genre, or description. "
                "Use this tool when the user asks for book recommendations by topic, genre, author style, or general interest. "
                "Returns a list of matching books with their IDs, titles, authors, prices, and descriptions."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "query_text": {
                        "type": "string",
                        "description": "The search query describing the kind of books to look for (e.g. 'machine learning for beginners', 'Vietnamese history')."
                    },
                    "top_k": {
                        "type": "integer",
                        "description": "Maximum number of books to return. Default is 10.",
                        "default": 10
                    }
                },
                "required": ["query_text"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "query_postgres",
            "description": (
                "Query the PostgreSQL database to filter, sort, or find books by structured criteria such as price, category, author, stock, or publication year. "
                "Use this tool when the user asks to sort books (e.g. cheapest, most expensive), filter by price range, filter by category name, or find books in stock. "
                "Returns a list of books matching the criteria."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "category": {
                        "type": "string",
                        "description": "Filter by category name (partial match, case-insensitive). Example: 'Technology', 'Psychology'."
                    },
                    "author": {
                        "type": "string",
                        "description": "Filter by author name (partial match, case-insensitive)."
                    },
                    "min_price": {
                        "type": "number",
                        "description": "Minimum price in VND (inclusive)."
                    },
                    "max_price": {
                        "type": "number",
                        "description": "Maximum price in VND (inclusive)."
                    },
                    "sort_by": {
                        "type": "string",
                        "description": "Sort order for results. Use 'price_asc' for cheapest first, 'price_desc' for most expensive first, 'newest' for newest publications first.",
                        "enum": ["price_asc", "price_desc", "newest"]
                    },
                    "in_stock": {
                        "type": "boolean",
                        "description": "If true, only return books currently in stock (stock > 0)."
                    },
                    "limit": {
                        "type": "integer",
                        "description": "Maximum number of books to return. Default is 10.",
                        "default": 10
                    }
                },
                "required": []
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "query_mongo",
            "description": (
                "Search for books using MongoDB full-text search on book descriptions and content. "
                "Use this tool when the user's query requires deep semantic matching against book content, summaries, or detailed descriptions. "
                "Returns a list of books whose descriptions or content match the query."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "search_text": {
                        "type": "string",
                        "description": "The text to search for within book descriptions and content."
                    },
                    "limit": {
                        "type": "integer",
                        "description": "Maximum number of books to return. Default is 10.",
                        "default": 10
                    }
                },
                "required": ["search_text"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "query_postgres_sql",
            "description": (
                "Execute a read-only SQL SELECT query against the PostgreSQL database. "
                "Use this tool when the user asks questions requiring database counts, statistics, aggregations, "
                "joining multiple tables (e.g. books, categories, orders, order_items, reviews), or complex filtering not supported by other tools. "
                "Always write clean standard SQL. Only SELECT queries are permitted."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "sql_query": {
                        "type": "string",
                        "description": "The exact read-only SQL SELECT query to execute (e.g. 'SELECT count(*) FROM books')."
                    }
                },
                "required": ["sql_query"]
            }
        }
    }
]


def rag_catalog(query_text: str, top_k: int = 10) -> list[dict]:
    """Perform a semantic vector search on the book catalog."""
    try:
        from src.config import settings
        from src.services import QdrantStore
        from qdrant_client import QdrantClient
        import urllib.request

        client = QdrantClient(url=settings.qdrant_url, timeout=settings.qdrant_timeout_seconds)

        # Get embedding for the query from OpenAI
        embed_url = os.getenv("OPENAI_EMBEDDING_BASE_URL", "https://api.openai.com/v1") + "/embeddings"
        api_key = settings.openai_api_key
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}",
            "ngrok-skip-browser-warning": "69420"
        }
        embed_payload = {
            "model": settings.openai_embedding_model,
            "input": query_text
        }
        req = urllib.request.Request(
            embed_url,
            data=json.dumps(embed_payload).encode("utf-8"),
            headers=headers,
            method="POST"
        )
        with urllib.request.urlopen(req, timeout=30) as resp:
            embed_data = json.loads(resp.read().decode("utf-8"))
        vector = embed_data["data"][0]["embedding"]

        results = client.query_points(
            collection_name=settings.qdrant_catalog_collection,
            query=vector,
            limit=top_k,
            with_payload=True,
        )

        books = []
        for point in results.points:
            p = point.payload or {}
            books.append({
                "id": p.get("book_id") or point.id,
                "title": p.get("title", ""),
                "author": p.get("author", ""),
                "category": p.get("category", ""),
                "price": float(p.get("price", 0)) if p.get("price") is not None else None,
                "description": p.get("description", ""),
                "publisher": p.get("publisher", ""),
                "publication_year": p.get("publication_year"),
                "language": p.get("language", ""),
                "pages": p.get("pages"),
                "stock": p.get("stock"),
            })
        if books:
            return books
    except Exception as e:
        print(f"DEBUG rag_catalog embedding search failed, falling back to keyword search: {e}", flush=True)

    return _postgres_keyword_search(query_text, limit=top_k)


def _postgres_keyword_search(query_text: str, limit: int = 10) -> list[dict]:
    """Fallback keyword search in PostgreSQL when vector embedding is unavailable."""
    try:
        import psycopg2
        import psycopg2.extras
        import os

        dsn = os.getenv("POSTGRES_URL") or "postgresql://{}:{}@{}:{}/{}".format(
            os.getenv("POSTGRES_USER", "bookverse"),
            os.getenv("POSTGRES_PASSWORD", "bookverse"),
            os.getenv("POSTGRES_HOST", "postgres"),
            os.getenv("POSTGRES_PORT", "5432"),
            os.getenv("POSTGRES_DB", "bookverse"),
        )
        conn = psycopg2.connect(dsn)
        cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)

        words = [w.strip() for w in query_text.split() if len(w.strip()) >= 2 and w.lower() not in {"and", "the", "for", "with", "about", "books", "find"}]
        if not words:
            words = [query_text.strip()]

        conditions = []
        args: list = []
        for w in words[:4]:
            conditions.append("(LOWER(b.title) LIKE LOWER(%s) OR LOWER(COALESCE(b.description, '')) LIKE LOWER(%s) OR LOWER(COALESCE(c.name, '')) LIKE LOWER(%s))")
            args.extend([f"%{w}%", f"%{w}%", f"%{w}%"])

        where_clause = " OR ".join(conditions)
        sql = f"""
            SELECT
                b.id, b.title, b.author, c.name AS category, b.price, b.original_price, b.stock, b.description, b.publisher, b.publication_year, b.language, b.pages
            FROM books b
            LEFT JOIN categories c ON b.category_id = c.id
            WHERE b.active = true AND ({where_clause})
            ORDER BY b.id ASC
            LIMIT %s
        """
        args.append(limit)
        cur.execute(sql, tuple(args))
        rows = cur.fetchall()
        cur.close()
        conn.close()

        return [{
            "id": row["id"],
            "title": row["title"],
            "author": row["author"],
            "category": row["category"],
            "price": float(row["price"]) if row["price"] is not None else None,
            "original_price": float(row["original_price"]) if row["original_price"] is not None else None,
            "stock": row["stock"],
            "description": row["description"],
            "publisher": row["publisher"],
            "publication_year": row["publication_year"],
            "language": row["language"],
            "pages": row["pages"],
        } for row in rows]
    except Exception as e:
        print(f"DEBUG _postgres_keyword_search ERROR: {e}", flush=True)
        return []


def query_postgres(
    category: str | None = None,
    author: str | None = None,
    min_price: float | None = None,
    max_price: float | None = None,
    sort_by: str | None = None,
    in_stock: bool | None = None,
    limit: int = 10,
) -> list[dict]:
    """Query PostgreSQL for books by structured filters."""
    try:
        import psycopg2
        import psycopg2.extras
        import os

        dsn = os.getenv("POSTGRES_URL") or "postgresql://{}:{}@{}:{}/{}".format(
            os.getenv("POSTGRES_USER", "bookverse"),
            os.getenv("POSTGRES_PASSWORD", "bookverse"),
            os.getenv("POSTGRES_HOST", "postgres"),
            os.getenv("POSTGRES_PORT", "5432"),
            os.getenv("POSTGRES_DB", "bookverse"),
        )

        conn = psycopg2.connect(dsn)
        cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)

        conditions = ["b.active = true", "c.active = true"]
        args: list = []

        if category:
            conditions.append("LOWER(c.name) LIKE LOWER(%s)")
            args.append(f"%{category}%")
        if author:
            conditions.append("LOWER(b.author) LIKE LOWER(%s)")
            args.append(f"%{author}%")
        if min_price is not None:
            conditions.append("b.price >= %s")
            args.append(min_price)
        if max_price is not None:
            conditions.append("b.price <= %s")
            args.append(max_price)
        if in_stock:
            conditions.append("b.stock > 0")

        where_clause = " AND ".join(conditions)

        order_clause = "b.id ASC"
        if sort_by == "price_asc":
            order_clause = "b.price ASC"
        elif sort_by == "price_desc":
            order_clause = "b.price DESC"
        elif sort_by == "newest":
            order_clause = "b.publication_year DESC NULLS LAST"

        sql = f"""
            SELECT
                b.id,
                b.title,
                b.author,
                c.name AS category,
                b.price,
                b.original_price,
                b.stock,
                b.description,
                b.publisher,
                b.publication_year,
                b.language,
                b.pages
            FROM books b
            LEFT JOIN categories c ON b.category_id = c.id
            WHERE {where_clause}
            ORDER BY {order_clause}
            LIMIT %s
        """
        args.append(limit)
        cur.execute(sql, tuple(args))
        rows = cur.fetchall()
        cur.close()
        conn.close()

        books = []
        for row in rows:
            books.append({
                "id": row["id"],
                "title": row["title"],
                "author": row["author"],
                "category": row["category"],
                "price": float(row["price"]) if row["price"] is not None else None,
                "original_price": float(row["original_price"]) if row["original_price"] is not None else None,
                "stock": row["stock"],
                "description": row["description"],
                "publisher": row["publisher"],
                "publication_year": row["publication_year"],
                "language": row["language"],
                "pages": row["pages"],
            })
        return books
    except Exception as e:
        print(f"DEBUG query_postgres ERROR: {e}", flush=True)
        return []


def query_mongo(search_text: str, limit: int = 10) -> list[dict]:
    """Full-text search on MongoDB book descriptions."""
    try:
        from src.services import MongoBookStore
        store = MongoBookStore()
        db = store.client[store.database_name]
        col = db[store.books_collection_name]

        results = col.find(
            {"$text": {"$search": search_text}},
            {"score": {"$meta": "textScore"}}
        ).sort([("score", {"$meta": "textScore"})]).limit(limit)

        books = []
        for doc in results:
            books.append({
                "id": doc.get("book_id") or doc.get("id"),
                "title": doc.get("document_name", ""),
                "description": doc.get("description", ""),
            })
        return books
    except Exception as e:
        print(f"DEBUG query_mongo ERROR: {e}", flush=True)
        return []


def query_postgres_sql(sql_query: str) -> list[dict]:
    """Execute a read-only SQL SELECT query on the PostgreSQL database."""
    print(f"DEBUG: query_postgres_sql executing: {sql_query}", flush=True)
    cleaned = sql_query.strip().upper()
    if not cleaned.startswith("SELECT"):
        return [{"error": "Only SELECT statements are allowed."}]
    
    forbidden = ["INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "ALTER", "CREATE", "GRANT", "REVOKE", "RENAME"]
    import re
    for keyword in forbidden:
        if re.search(r'\b' + keyword + r'\b', cleaned):
            return [{"error": f"Security violation: SQL query contains forbidden keyword '{keyword}'."}]
            
    try:
        import psycopg2
        import psycopg2.extras
        import os

        dsn = os.getenv("POSTGRES_URL") or "postgresql://{}:{}@{}:{}/{}".format(
            os.getenv("POSTGRES_USER", "bookverse"),
            os.getenv("POSTGRES_PASSWORD", "bookverse"),
            os.getenv("POSTGRES_HOST", "postgres"),
            os.getenv("POSTGRES_PORT", "5432"),
            os.getenv("POSTGRES_DB", "bookverse"),
        )
        conn = psycopg2.connect(dsn)
        
        with conn.cursor() as setup_cur:
            setup_cur.execute("SET statement_timeout = 5000;")
            
        cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cur.execute(sql_query)
        rows = cur.fetchall()
        cur.close()
        conn.close()

        import decimal
        import datetime
        formatted_rows = []
        for row in rows:
            formatted_row = {}
            for k, v in row.items():
                if isinstance(v, decimal.Decimal):
                    formatted_row[k] = float(v)
                elif isinstance(v, (datetime.datetime, datetime.date)):
                    formatted_row[k] = v.isoformat()
                else:
                    formatted_row[k] = v
            formatted_rows.append(formatted_row)
        return formatted_rows
    except Exception as e:
        return [{"error": f"SQL execution error: {e}"}]


def execute_database_tool(tool_name: str, args: dict) -> list[dict]:
    """Dispatch a tool call to the appropriate function."""
    if tool_name == "rag_catalog":
        return rag_catalog(**args)
    elif tool_name == "query_postgres":
        return query_postgres(**args)
    elif tool_name == "query_mongo":
        return query_mongo(**args)
    elif tool_name == "query_postgres_sql":
        return query_postgres_sql(**args)
    else:
        print(f"DEBUG execute_database_tool: unknown tool '{tool_name}'", flush=True)
        return []
