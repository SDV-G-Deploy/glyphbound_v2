#!/usr/bin/env sh
set -eu

CURRENT_TAG="${GITHUB_REF_NAME:-}"
if [ -z "$CURRENT_TAG" ]; then
  CURRENT_TAG="$(git describe --tags --abbrev=0 2>/dev/null || echo "HEAD")"
fi

PREV_TAG="$(git tag --sort=-v:refname | grep -v "^${CURRENT_TAG}$" | head -n 1 || true)"

if [ -n "$PREV_TAG" ]; then
  RANGE="${PREV_TAG}..${CURRENT_TAG}"
  HEADER="## ${CURRENT_TAG} (since ${PREV_TAG})"
else
  RANGE="${CURRENT_TAG}"
  HEADER="## ${CURRENT_TAG}"
fi

echo "$HEADER"
echo
if [ "$CURRENT_TAG" = "HEAD" ]; then
  git log --pretty=format:"- %s (%h)" -n 40
else
  git log "$RANGE" --pretty=format:"- %s (%h)"
fi
