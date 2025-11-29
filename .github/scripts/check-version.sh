#!/usr/bin/env bash
set -euo pipefail

# Usage: check-version.sh <current-tag> [<previous-tag>]
# If previous-tag is omitted, script finds the most recent tag prior to current-tag.

current_tag=${1:-}
prev_tag=${2:-}

if [ -z "$current_tag" ]; then
  echo "Usage: $0 <current-tag> [<previous-tag>]"
  exit 2
fi

# fetch tags and history (safe for shallow clones)
git fetch --tags --prune || true

# normalize tag names (strip leading refs/tags/ if present)
current_tag=${current_tag#refs/tags/}

if [ -z "$prev_tag" ]; then
  prev_tag=$(git for-each-ref --sort=-creatordate --format '%(refname:short)' refs/tags | awk -v cur="$current_tag" '$0!=cur{print; exit}')
fi

if [ -z "$prev_tag" ]; then
  echo "No previous tag found — treating as first release (publish=true)"
  exit 0
fi

echo "current tag: $current_tag"
echo "previous tag: $prev_tag"

get_version_from() {
  local ref=$1
  git show "$ref":build.gradle 2>/dev/null | sed -n "s/^[[:space:]]*version[[:space:]]*=[[:space:]]*['\"]\([^'\"]*\)['\"].*/\1/p" | head -n1 || true
}

current_version=$(get_version_from "$current_tag")
prev_version=$(get_version_from "$prev_tag")

current_version=${current_version#v}
prev_version=${prev_version#v}

echo "version at $current_tag: ${current_version:-<not found>}"
echo "version at $prev_tag: ${prev_version:-<not found>}"

if [ -z "$current_version" ]; then
  echo "No version found in current build.gradle. Skipping publish." >&2
  exit 3
fi

if [ "$current_version" != "$prev_version" ]; then
  echo "Version changed ($prev_version -> $current_version): publish=true"
  exit 0
else
  echo "Version unchanged ($current_version): publish=false"
  exit 4
fi
