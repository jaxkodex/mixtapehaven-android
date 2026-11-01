#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
tmp=$(mktemp -d)
trap 'rm -f "$tmp/git" "$tmp/output"; rmdir "$tmp"' EXIT

# Simulate the remote branch without needing network access or repository credentials.
cat > "$tmp/git" <<'EOF'
#!/usr/bin/env bash
if [[ "${MOCK_GIT_FAILURE:-}" == 'true' ]]; then exit 1; fi
printf '%s\trefs/heads/main\n' "$MOCK_MAIN_SHA"
EOF
chmod +x "$tmp/git"
export PATH="$tmp:$PATH"
export GITHUB_OUTPUT="$tmp/output" GITHUB_SHA='commit-a'

export MOCK_MAIN_SHA='commit-a'
bash "$script_dir/check-play-head.sh"
[[ $(cat "$GITHUB_OUTPUT") == 'current=true' ]]

: > "$GITHUB_OUTPUT"
export MOCK_MAIN_SHA='commit-b'
bash "$script_dir/check-play-head.sh"
[[ $(cat "$GITHUB_OUTPUT") == 'current=false' ]]

: > "$GITHUB_OUTPUT"
export MOCK_GIT_FAILURE='true'
if bash "$script_dir/check-play-head.sh"; then
  echo 'A failed remote lookup must fail the check' >&2
  exit 1
fi
[[ ! -s "$GITHUB_OUTPUT" ]]
echo 'Play main-branch checks passed'
