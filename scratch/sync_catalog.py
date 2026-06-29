import urllib.request
import json
import sys

def sync():
    sys.stdout.reconfigure(encoding='utf-8')
    
    # 1. Login as Admin
    admin_login_data = json.dumps({
        "email": "admin@bookverse.local",
        "password": "Admin123@"
    }).encode('utf-8')
    
    req = urllib.request.Request(
        "http://localhost:8080/api/v1/auth/login",
        data=admin_login_data,
        headers={"Content-Type": "application/json"}
    )
    try:
        with urllib.request.urlopen(req) as res:
            login_res = json.loads(res.read().decode('utf-8'))
            token = login_res["data"]["accessToken"]
            print("Successfully authenticated as Admin.")
    except Exception as e:
        print(f"Failed to login as admin: {e}")
        return

    # 2. Get list of all book IDs from database (via an public or admin endpoint, or simply looping 1 to 50)
    # Let's call the public books catalog endpoint to find all valid book IDs, or simply try IDs 1 to 50
    # Since ID is incremental, we can try IDs 1 to 50. If the book doesn't exist, Spring Boot will return 404 which we can skip.
    success_count = 0
    for book_id in range(1, 51):
        upsert_req = urllib.request.Request(
            f"http://localhost:8080/api/v1/admin/rag/catalog/upsert/{book_id}",
            data=b"", # POST request empty body
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json"
            }
        )
        try:
            with urllib.request.urlopen(upsert_req) as res:
                # Successfully upserted
                print(f"Indexed Book ID {book_id} to Qdrant catalog successfully.")
                success_count += 1
        except urllib.error.HTTPError as e:
            # If 404, the book ID doesn't exist, we skip
            if e.code == 404:
                continue
            else:
                print(f"Error indexing Book ID {book_id}: {e.code} - {e.reason}")
        except Exception as e:
            print(f"Error indexing Book ID {book_id}: {e}")

    print(f"Indexing completed. Total books indexed: {success_count}")

if __name__ == "__main__":
    sync()
