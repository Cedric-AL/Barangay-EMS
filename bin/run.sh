#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"

if [ -f lib/gson.jar ]; then
  rm -rf out
  mkdir -p out
  find src -name '*.java' > out/sources.txt
  javac -encoding UTF-8 -cp lib/gson.jar -d out @out/sources.txt
  exec java -cp "out:lib/gson.jar" barangayems.Main
fi

if command -v mvn >/dev/null 2>&1; then
  exec mvn -q compile exec:java
fi

echo "Gson dependency not found. Install Maven, or place Gson 2.10.1 at lib/gson.jar." >&2
exit 1
