# Dev container

Runs an Android SDK toolchain and Claude Code in an isolated container.

## What's included

- **Java 17** base image, with the Android cmdline-tools and the platform /
  build-tools this project targets installed by `post-create.sh` into
  `$ANDROID_HOME` (persisted in a named volume across rebuilds).
- **Gradle cache** persisted in a named volume across rebuilds.
- **Claude Code** (`claude` in the terminal), with `~/.claude` persisted across
  rebuilds in a named volume.

## First run

1. Open the repo in VS Code and run **Dev Containers: Reopen in Container**.
2. Build the project: `./gradlew build`.

## Controlling Claude from your phone

Use [Remote Control](https://code.claude.com/docs/en/remote-control). Claude
keeps running **inside this container** (your filesystem and Android SDK stay
available), while claude.ai/code and the Claude mobile app act as a window
into that local session. This is different from Claude Code on the web, which
runs in Anthropic's cloud sandbox and can't see your container.

Start (or re-attach to) the session:

```bash
.devcontainer/claude-remote.sh
```

This launches `claude --remote-control "mixtapehaven-android" --dangerously-skip-permissions`
in a tmux session named `claude` (tmux just keeps it alive). Then:

1. **Sign in once** — on first attach run `/login` and choose the claude.ai
   option. Remote Control needs claude.ai OAuth, **not** an API key or
   `setup-token`. Auth persists in the `~/.claude` volume across rebuilds.
2. **Open it on your phone** — the session shows up at
   [claude.ai/code](https://claude.ai/code) and in the Claude app (tap **Code**)
   with a computer icon + green dot. Or scan the QR code shown in the terminal to
   jump straight there.

Requirements: a Pro / Max / Team / Enterprise plan and Claude Code v2.1.51+
(the container ships a newer version). On Team/Enterprise an admin must enable
the Remote Control toggle in Claude Code admin settings.

> **Heads-up:** `--dangerously-skip-permissions` lets Claude run any tool
> without asking. With Remote Control you can instead approve each tool call
> from your phone — drop that flag in `claude-remote.sh` if you prefer to
> review actions. It is gated on the non-root `vscode` user. Use only on
> trusted repos.
