#!/bin/sh
set -eu

: "${MINIO_ENDPOINT:?MINIO_ENDPOINT is required}"
: "${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}"
: "${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}"

mc alias set local \
  "$MINIO_ENDPOINT" \
  "$MINIO_ROOT_USER" \
  "$MINIO_ROOT_PASSWORD"

mc mb --ignore-existing local/video-originals
mc mb --ignore-existing local/video-hls
mc mb --ignore-existing local/video-thumbnails

# Local development only. Keep these buckets private in production.
mc anonymous set download local/video-hls
mc anonymous set download local/video-thumbnails