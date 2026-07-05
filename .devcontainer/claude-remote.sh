#!/usr/bin/env bash
# Run Claude Code inside the dev container as a Remote Control session, kept
# alive in tmux. Remote Control makes this local (in-container) session show up
# in claude.ai/code and the Claude mobile app, so you can drive it from your
# phone or any browser while Claude keeps running here against your local
# filesystem and Android SDK.
#
#   claude-remote.sh          ensure the session exists, then attach to it
#   claude-remote.sh start    ensure the session exists (detached); don't attach
#
# The `start` form is used by postAttachCommand in devcontainer.json, which has
# no interactive terminal — so it only creates the background session.
#
# Connect from your phone:
#   1. Attach locally once (`claude-remote.sh`) and run `/login` to sign in with
#      your claude.ai account — Remote Control needs claude.ai OAuth, not an API
#      key or setup-token. (~/.claude is a persistent volume, so this is one-time.)
#   2. The session appears at claude.ai/code with a computer icon + green dot,
#      or scan the QR code shown in the terminal to open it in the Claude app.
#
# Requirements: Pro/Max/Team/Enterprise plan; Claude Code v2.1.51+.
#
# WARNING: --dangerously-skip-permissions lets Claude run any tool without
# asking. With Remote Control you can instead approve each tool call from your
# phone — drop that flag in this script if you prefer to review actions. It is
# gated here on the non-root `vscode` user. Use only on trusted repos.
set -euo pipefail

SESSION="claude"
WORKSPACE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Create the session detached if it isn't already running.
if ! tmux has-session -t "$SESSION" 2>/dev/null; then
	# `|| bash` keeps the tmux window open if Claude exits or fails to start,
	# so the error stays readable for troubleshooting.
	tmux new-session -d -s "$SESSION" \
		"cd '$WORKSPACE_DIR' && (claude --remote-control \"mixtapehaven-android\" --dangerously-skip-permissions || bash)"
fi

# `start` just guarantees the session exists; anything else means "attach".
if [ "${1:-}" = "start" ]; then
	echo "Claude Remote Control session '$SESSION' is running. Attach with: tmux attach -t $SESSION"
	exit 0
fi

exec tmux attach -t "$SESSION"
