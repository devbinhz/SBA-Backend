import os
import sys
import pathlib
import shutil
import fitz
from ebooklib import epub
from minio import Minio

sys.path.append(str(pathlib.Path(__file__).resolve().parents[1]))
from src.config import settings

def extract_pdf_cover(pdf_path):
    doc = fitz.open(pdf_path)
    page = doc[0]
    pix = page.get_pixmap(dpi=150)
    image_bytes = pix.tobytes("png")
    doc.close()
    return image_bytes

def extract_epub_cover(epub_path):
    book = epub.read_epub(str(epub_path))
    for item in book.get_items():
        if item.get_type() in (10, 1):
            name = item.get_name().lower()
            item_id = item.get_id().lower()
            if "cover" in name or "cover" in item_id:
                return item.get_content()
    cover_meta = book.get_metadata("OPF", "cover")
    if cover_meta:
        try:
            cover_id = cover_meta[0][1].get("content")
            if cover_id:
                item = book.get_item_with_id(cover_id)
                if item:
                    return item.get_content()
        except Exception:
            pass
    for item in book.get_items_of_type(10):
        return item.get_content()
    for item in book.get_items_of_type(1):
        return item.get_content()
    return None

def main():
    endpoint = os.getenv("MINIO_ENDPOINT", settings.minio_endpoint)
    if endpoint.startswith("http://"):
        endpoint = endpoint[7:]
    elif endpoint.startswith("https://"):
        endpoint = endpoint[8:]
        
    access_key = os.getenv("MINIO_ACCESS_KEY", settings.minio_access_key)
    secret_key = os.getenv("MINIO_SECRET_KEY", settings.minio_secret_key)
    books_bucket = os.getenv("MINIO_BOOKS_BUCKET", settings.minio_books_bucket)
    thumbnails_bucket = os.getenv("MINIO_THUMBNAILS_BUCKET", "bookverse-thumbnails")
    
    client = Minio(endpoint, access_key=access_key, secret_key=secret_key, secure=False)
    
    if not client.bucket_exists(thumbnails_bucket):
        client.make_bucket(thumbnails_bucket)
        
    objects = list(client.list_objects(books_bucket))
    
    local_dir = pathlib.Path("temp_migrate_thumbnails")
    local_dir.mkdir(exist_ok=True)
    
    success_count = 0
    fail_count = 0
    
    for obj in objects:
        name = obj.object_name
        lower_name = name.lower()
        if not (lower_name.endswith(".pdf") or lower_name.endswith(".epub")):
            continue
            
        local_path = local_dir / name
        try:
            client.fget_object(books_bucket, name, str(local_path))
            
            img_bytes = None
            if lower_name.endswith(".pdf"):
                img_bytes = extract_pdf_cover(local_path)
            elif lower_name.endswith(".epub"):
                img_bytes = extract_epub_cover(local_path)
                
            if img_bytes:
                thumbnail_name = name.rsplit(".", 1)[0] + ".png"
                out_path = local_dir / thumbnail_name
                with open(out_path, "wb") as f:
                    f.write(img_bytes)
                client.fput_object(thumbnails_bucket, thumbnail_name, str(out_path), content_type="image/png")
                success_count += 1
                print(f"SUCCESS: {name} -> {thumbnail_name}")
            else:
                fail_count += 1
                print(f"FAILED: {name} (No cover extracted)")
        except Exception as e:
            fail_count += 1
            print(f"ERROR: {name} -> {e}")
            
    if local_dir.exists():
        shutil.rmtree(local_dir)
        
    print(f"\nMigration completed. Success: {success_count}, Failed: {fail_count}")

if __name__ == "__main__":
    main()
