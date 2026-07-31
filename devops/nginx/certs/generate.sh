#!/usr/bin/env bash
# Generate a local-dev TLS cert for nginx (HTTPS on localhost).
# Safari rejects Keycloak's Secure cookies on plain HTTP — HTTPS is required.
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
openssl req -x509 -nodes -newkey rsa:2048 -days 825 \
  -keyout "$DIR/localhost.key" \
  -out "$DIR/localhost.crt" \
  -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
echo "Wrote $DIR/localhost.crt and localhost.key"
echo "Trust once on macOS (Safari):"
echo "  sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain $DIR/localhost.crt"
