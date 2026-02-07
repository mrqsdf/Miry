#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${1:-"$ROOT_DIR/docs/api"}"

LWJGL_VERSION="${LWJGL_VERSION:-3.3.3}"
JOML_VERSION="${JOML_VERSION:-1.10.7}"

DEPS_DIR="$ROOT_DIR/build/javadoc-deps"
SRC_DIR="$ROOT_DIR/src/main/java"

mkdir -p "$DEPS_DIR" "$OUT_DIR"

download() {
  local url="$1"
  local dest="$2"
  if [[ -f "$dest" ]]; then
    return 0
  fi
  echo "Downloading: $url"
  curl -fsSL "$url" -o "$dest"
}

resolve_jar() {
  local pattern="$1"
  local from_cache
  from_cache="$(find "${HOME}/.gradle/caches/modules-2/files-2.1" -type f -name "$pattern" 2>/dev/null | head -n 1 || true)"
  if [[ -n "$from_cache" ]]; then
    echo "$from_cache"
    return 0
  fi
  echo ""
}

ensure_deps() {
  local jars=()

  local j
  j="$(resolve_jar "joml-${JOML_VERSION}.jar")"
  if [[ -n "$j" ]]; then jars+=("$j"); fi

  for a in lwjgl lwjgl-glfw lwjgl-opengl lwjgl-stb; do
    j="$(resolve_jar "${a}-${LWJGL_VERSION}.jar")"
    if [[ -n "$j" ]]; then jars+=("$j"); fi
  done

  if [[ ${#jars[@]} -ge 5 ]]; then
    printf "%s\n" "${jars[@]}"
    return 0
  fi

  echo "Gradle cache is missing doc deps; downloading to $DEPS_DIR"

  download "https://repo1.maven.org/maven2/org/joml/joml/${JOML_VERSION}/joml-${JOML_VERSION}.jar" \
    "$DEPS_DIR/joml-${JOML_VERSION}.jar"

  for a in lwjgl lwjgl-glfw lwjgl-opengl lwjgl-stb; do
    download "https://repo1.maven.org/maven2/org/lwjgl/${a}/${LWJGL_VERSION}/${a}-${LWJGL_VERSION}.jar" \
      "$DEPS_DIR/${a}-${LWJGL_VERSION}.jar"
  done

  printf "%s\n" \
    "$DEPS_DIR/joml-${JOML_VERSION}.jar" \
    "$DEPS_DIR/lwjgl-${LWJGL_VERSION}.jar" \
    "$DEPS_DIR/lwjgl-glfw-${LWJGL_VERSION}.jar" \
    "$DEPS_DIR/lwjgl-opengl-${LWJGL_VERSION}.jar" \
    "$DEPS_DIR/lwjgl-stb-${LWJGL_VERSION}.jar"
}

CLASSPATH="$(ensure_deps | paste -sd: -)"

echo "Generating Javadoc → $OUT_DIR"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"
touch "$OUT_DIR/.nojekyll"

javadoc \
  -d "$OUT_DIR" \
  -sourcepath "$SRC_DIR" \
  -subpackages com.miry \
  -classpath "$CLASSPATH" \
  -encoding UTF-8 \
  -charset UTF-8 \
  -quiet \
  -Xdoclint:none

echo "Done."
