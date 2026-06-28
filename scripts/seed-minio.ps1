$MinioEndpoint = $env:MINIO_ENDPOINT
if (-not $MinioEndpoint) {
    $MinioEndpoint = "localhost:9000"
}
$MinioAccessKey = $env:MINIO_ACCESS_KEY
if (-not $MinioAccessKey) {
    $MinioAccessKey = "minioadmin"
}
$MinioSecretKey = $env:MINIO_SECRET_KEY
if (-not $MinioSecretKey) {
    $MinioSecretKey = "minioadmin"
}
$MinioBucket = $env:MINIO_BOOKS_BUCKET
if (-not $MinioBucket) {
    $MinioBucket = "bookverse-books"
}

uv run --project rag python -c "
import os
from pathlib import Path
from minio import Minio

endpoint = '$MinioEndpoint'
if endpoint.startswith('http://'):
    endpoint = endpoint[7:]
elif endpoint.startswith('https://'):
    endpoint = endpoint[8:]

client = Minio(
    endpoint,
    access_key='$MinioAccessKey',
    secret_key='$MinioSecretKey',
    secure=False
)

bucket = '$MinioBucket'
if not client.bucket_exists(bucket):
    client.make_bucket(bucket)

books_dir = Path('rag/assets/books')
for f in books_dir.iterdir():
    if f.is_file() and f.suffix.lower() in {'.pdf', '.epub'}:
        client.fput_object(bucket, f.name, str(f))
"
