#!/bin/bash
set -o allexport; source .env; set +o allexport
# -------------------------------
#  GoLang function
# -------------------------------
WASM_FILE="./demo-plugin/demo.wasm" \
HTTP_PORT="8888" \
FUNCTION_NAME="hello" \
java -jar target/chicory-vertx-starter-kit-0.0.0-SNAPSHOT-fat.jar

