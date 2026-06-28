import os
import tempfile
import shutil
from pathlib import Path
from minio import Minio
from src.config import settings

class MinioBookStorage:
    def __init__(self):
        endpoint = settings.minio_endpoint
        secure = False
        if endpoint.startswith("http://"):
            endpoint = endpoint[7:]
        elif endpoint.startswith("https://"):
            endpoint = endpoint[8:]
            secure = True
        self.client = Minio(
            endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=secure,
        )
        self.bucket = settings.minio_books_bucket

    def download_book(self, file_name: str) -> Path:
        try:
            if self.is_available():
                suffix = Path(file_name).suffix
                temp_file = tempfile.NamedTemporaryFile(suffix=suffix, delete=False)
                temp_path = Path(temp_file.name)
                temp_file.close()
                self.client.fget_object(self.bucket, file_name, str(temp_path))
                return temp_path
        except Exception:
            pass

        local_path = settings.books_dir / file_name
        if local_path.exists():
            suffix = local_path.suffix
            temp_file = tempfile.NamedTemporaryFile(suffix=suffix, delete=False)
            temp_path = Path(temp_file.name)
            temp_file.close()
            shutil.copy(local_path, temp_path)
            return temp_path

        raise FileNotFoundError(f"Book {file_name} not found")

    def is_available(self) -> bool:
        try:
            return self.client.bucket_exists(self.bucket)
        except Exception:
            return False
