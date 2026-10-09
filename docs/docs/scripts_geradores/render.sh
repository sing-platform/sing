#!/bin/bash
IN="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
OUT="$(cd "$(dirname "$2")" && pwd)/$(basename "$2")"
"/c/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2 --window-size=$3,$4 --default-background-color=FFFFFFFF --virtual-time-budget=3000 --screenshot="$(cygpath -w "$OUT")" "file:///$(cygpath -m "$IN")" >/dev/null 2>&1
ls -la "$OUT" | awk '{print $5, $9}'
