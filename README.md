# Vyom ERP

Attendance system for the Team Office Z200BW terminal, split into
independent projects. Each is its own git repository (a submodule here), so a
server or PC gets only the code it runs.

| Folder | Runs on | What it is |
| ------ | ------- | ---------- |
| [`web-dashboard/`](web-dashboard/README.md) | VPS (Docker) / dev machine | Java API + React portal + PostgreSQL |
| [`sdk/`](sdk/README.md) | Windows PC on the device LAN | Sync agent: device → PostgreSQL |
| `unused/` | — | Parked code and vendor samples, not built |

```
Z200BW ──LAN──> sdk agent (Windows) ──JDBC──> PostgreSQL <── web-dashboard (VPS)
```

The two projects share no code. Their only contract is the database schema,
described in each README.

## Cloning

```bash
git clone --recurse-submodules <vyom-erp-url>      # everything
git clone <web-dashboard-url>                      # VPS: dashboard only
git clone <sdk-url>                                # Windows PC: agent only
```

Already cloned without submodules: `git submodule update --init`.
After committing inside a submodule, commit here too to record its new version.

## Contributing

Each repo has an `AGENTS.md` with its conventions and rules for people and AI
agents ([this repo](AGENTS.md), [web-dashboard](web-dashboard/AGENTS.md),
[sdk](sdk/AGENTS.md)). In short:

- Push submodule changes to their `main` first, then bump the pointer here
  (`chore: bump erp-dashboard (...)` / `chore: bump sdk (...)`).
- Update the README and AGENTS.md in the same commit as the change.
- Never commit `.env` files, database dumps (`web-dashboard/db/`) or vendor DLLs.
