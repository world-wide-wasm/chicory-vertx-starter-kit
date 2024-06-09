#!/bin/bash
tinygo build -scheduler=none --no-debug \
  -o demo.wasm \
  -target=wasi main.go

ls -lh *.wasm
