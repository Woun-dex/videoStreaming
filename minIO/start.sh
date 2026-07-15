#!/bin/sh
set -eu

exec minio server /data --console-address ":9001"