# Uploads the gift-wrap pattern images under assets/gift-wrap/ into the MinIO
# thumbnails bucket, and makes sure that bucket exists with a public-read policy
# (same policy as scripts/set_minio_policy.py sets up for book covers/banners).
#
# Requires: MinIO running (docker compose up -d minio) and Docker available
# locally. Uses the official minio/mc image joined to the bookverse-minio
# container's network namespace, so it works regardless of the Compose
# project/network name.
#
# Run from the SBA-Backend directory:
#   ./scripts/seed-gift-wrap-images.ps1

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$AssetsDir = Join-Path $RepoRoot "assets\gift-wrap"
$MinioContainer = "bookverse-minio"
$Bucket = "bookverse-thumbnails"

if (-not (Test-Path $AssetsDir)) {
    throw "Assets directory not found: $AssetsDir"
}

$running = docker ps --filter "name=$MinioContainer" --format "{{.Names}}"
if (-not $running) {
    throw "Container '$MinioContainer' is not running. Start it first: docker compose up -d minio"
}

docker run --rm `
    --entrypoint sh `
    --network "container:$MinioContainer" `
    -v "${AssetsDir}:/data" `
    minio/mc:latest `
    -c "mc alias set local http://localhost:9000 minioadmin minioadmin && mc mb -p local/$Bucket && mc anonymous set download local/$Bucket && mc cp /data/kraft-plain.jpg local/$Bucket/gift-wrap/kraft-plain.jpg && mc cp /data/red-floral.jpg local/$Bucket/gift-wrap/red-floral.jpg && mc cp /data/blue-checker.jpg local/$Bucket/gift-wrap/blue-checker.jpg && mc cp /data/luxury-metallic.jpg local/$Bucket/gift-wrap/luxury-metallic.jpg && mc ls local/$Bucket/gift-wrap/"
