#!/usr/bin/env bash
set -euo pipefail

# Checkout persists the repository credentials, so this also works for private repos.
latest_sha=$(git ls-remote --exit-code origin refs/heads/main | cut -f1)
if [[ "$latest_sha" == "$GITHUB_SHA" ]]; then
  echo 'current=true' >> "$GITHUB_OUTPUT"
else
  echo 'current=false' >> "$GITHUB_OUTPUT"
  echo "::notice::Skipping $GITHUB_SHA: main has advanced to $latest_sha."
fi
