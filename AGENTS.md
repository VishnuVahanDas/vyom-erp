# AGENTS.md — vyom-erp

Instructions for AI coding agents working in this repository.

## What this repo is

`vyom-erp` is a **parent repo that only pins submodule versions**. It holds no
application code of its own. The real code lives in two independent git repos:

| Folder           | Remote repo                       | What it is                                             |
| ---------------- | --------------------------------- | ------------------------------------------------------ |
| `web-dashboard/` | `VishnuVahanDas/erp-dashboard`    | Java 24 REST API (`backend/`) + React/Vite UI (`frontend/`) + PostgreSQL, run with Docker Compose |
| `sdk/`           | `VishnuVahanDas/team-sdk`         | Windows sync agent: Z200BW device → PostgreSQL (Java 24, JNA, Maven wrapper) |
| `unused/`        | (in this repo)                    | Parked code and vendor samples. Not built; don't edit unless asked. |

The two submodules share no code. Their only contract is the **PostgreSQL
schema**: a schema change in one must stay compatible with the other. Read each
submodule's `README.md` before working in it.

## Submodule pointer rule (always do this)

Every time a submodule's `main` branch gets new commits, update the pointer
in `vyom-erp` and commit it. That includes commits you made, merged pull
requests, and commits someone else pushed. `vyom-erp/main` must always point
at the latest `origin/main` of each submodule.

### At the start of a session, check for drift

```bash
git fetch origin && git status -sb                  # parent up to date?
git submodule update --init
git submodule foreach 'git fetch -q origin && git log --oneline HEAD..origin/main'
```

Any commits listed mean that submodule's pointer is stale. Update it as below
before starting other work.

### After a submodule's main changes

```bash
# 1. In the submodule: get the work onto origin/main FIRST
cd web-dashboard            # or sdk
git switch main
git pull --ff-only          # or commit + git push origin main for your own changes

# 2. In the parent: record the new version
cd ..
git add web-dashboard       # or sdk, or both
git commit -m "chore: bump erp-dashboard (<short summary of what changed>)"
git push origin main
```

Equivalent shortcut from the parent: `git submodule update --remote <path>`.

Commit message conventions (match the existing history):
- One submodule: `chore: bump erp-dashboard (<summary>)` or `chore: bump sdk (<summary>)`
- Both: `chore: bump submodules (<summary 1>, <summary 2>)`

Summarise what the submodule commits did, e.g. `(date filter for punch logs)`,
not just their hashes.

### Pointer guardrails

- **Only point at commits that are on the submodule's `origin/main`.** Never
  record a commit that is unpushed, only on your machine, or only on a feature
  branch. Everyone else would get a broken checkout.
  Check with: `git -C <path> merge-base --is-ancestor HEAD origin/main`.
- **Push the submodule before the parent.** Pushing the parent first publishes
  a pointer to a commit nobody can fetch.
- Feature branches in a submodule are **not** recorded in the parent. Bump the
  pointer only after the branch is merged into that submodule's `main`.
- If `git status` in the parent shows `M web-dashboard` / `M sdk` and you didn't
  intend a bump, run `git submodule update` to go back to the pinned version.
  Don't commit an accidental pointer change.

## Security rules (hard requirements)

- **Never commit database dumps or production data.** `web-dashboard/db/` is
  git-ignored because a production dump (accounts, password hashes, session
  tokens) was once committed and had to be purged from history. Keep any
  `*.sql` dumps local only.
- **Never commit `.env` files.** Only `.env.example` is tracked, in each
  submodule.
- **Never commit vendor SDK files:** `unused/vendor-sdk-samples/` and the
  `sdk/native/*.dll` files are proprietary and stay local.
- Before committing, check `git status` / `git diff --cached` for secrets,
  dumps, `target/`, `dist/` and `node_modules/`.

## Git workflow

- `main` is the shared branch in all three repos. For non-trivial changes,
  work on a feature branch in the submodule (e.g. `feat/<topic>`), merge it to
  `main`, then bump the pointer here.
- **No force-pushes, history rewrites or branch deletions** without explicit
  approval from the user. Use `--force-with-lease`, never plain `--force`.
- After a branch is merged, it may be deleted from the remote (with approval).
  Check first that it has no unmerged commits:
  `git log --oneline origin/main..origin/<branch>` should print nothing.

## Building and running (summary — see each README)

**web-dashboard** (needs Docker Desktop, JDK 24+, Node 22):
```bash
cd web-dashboard
cp .env.example .env                           # once
docker compose up -d db                        # PostgreSQL on localhost:5433
cd backend && ./mvnw -q compile exec:java      # API on :8080
cd frontend && npm install && npm run dev      # UI on :5173 (proxies /api)
# or the full stack as in production:  docker compose up -d --build   -> :8088
```

**sdk** (64-bit Windows, JDK 24+, DLLs in `native/`):
```bash
cd sdk
copy .env.example .env                         # fill PG_* and DEVICE_PASSWORD
mvnw.cmd -q -DskipTests package                # build target/vyom-sync-agent.jar
run-agent.bat                                  # build if needed + run
```

Java packages live under `org.ajeet` in both projects.
