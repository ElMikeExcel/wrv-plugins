#!/usr/bin/env bash
# Expanded history WIP: never emit an artifact mislabeled as the separate approved color candidate.
set -euo pipefail
exec "$(cd "$(dirname "$0")" && pwd)/build-offline.sh" "$@"
