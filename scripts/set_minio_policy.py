import os
import json
from pathlib import Path
from minio import Minio

# Load .env manually to avoid dependency on python-dotenv
env_path = Path(__file__).resolve().parent.parent / '.env'
env_vars = {}
if env_path.exists():
    with open(env_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#') or '=' not in line:
                continue
            k, v = line.split('=', 1)
            env_vars[k.strip()] = v.strip()

endpoint = env_vars.get('MINIO_ENDPOINT', 'http://localhost:9000')
access_key = env_vars.get('MINIO_ACCESS_KEY', 'minioadmin')
secret_key = env_vars.get('MINIO_SECRET_KEY', 'minioadmin')
bucket = env_vars.get('MINIO_THUMBNAILS_BUCKET', 'bookverse-thumbnails')

# Clean http:// or https:// from endpoint for minio client connection
if endpoint.startswith('http://'):
    endpoint = endpoint[7:]
elif endpoint.startswith('https://'):
    endpoint = endpoint[8:]

# Try connecting to the specified endpoint, or fallback to localhost if running from outside docker network
endpoints_to_try = [endpoint]
if 'minio:9000' in endpoint or 'minio' in endpoint:
    endpoints_to_try.append('localhost:9000')

client = None
for ep in endpoints_to_try:
    try:
        print(f"Attempting to connect to Minio at: {ep}...")
        client = Minio(ep, access_key=access_key, secret_key=secret_key, secure=False)
        client.list_buckets()
        print(f"Successfully connected to Minio at: {ep}")
        break
    except Exception as e:
        print(f"Could not connect to {ep}: {e}")
        client = None

if not client:
    print("Error: Could not connect to any Minio endpoint. Make sure Minio is running.")
    exit(1)

# Define public read-only policy
public_read_policy = {
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {"AWS": ["*"]},
            "Action": ["s3:GetObject"],
            "Resource": [f"arn:aws:s3:::{bucket}/*"]
        }
    ]
}

try:
    if not client.bucket_exists(bucket):
        client.make_bucket(bucket)
        print(f"Bucket '{bucket}' created successfully.")
    
    client.set_bucket_policy(bucket, json.dumps(public_read_policy))
    print(f"Successfully configured public read-only policy for bucket: {bucket}")
except Exception as e:
    print(f"Failed to configure bucket policy: {e}")
    exit(1)
