#!/bin/bash
# -n 10000 -c 1000
#hey -n 10 -c 5 -m POST \
hey -n 3000 -c 500 -m POST \
-H "Content-Type: text/plain" \
-d "John Doe" \
"http://localhost:8888"
