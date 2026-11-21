# Google Sheets storage (unused)

Moved out of the build: storage is PostgreSQL now. These classes implemented
`org.ajeet.sheets.SheetsClient` (still in `sdk/`) and were selected when
`SPREADSHEET_ID` was set. To bring them back: move both files to
`web-dashboard/backend/src/main/java/org/ajeet/sheets/`, re-add the Google
dependencies below to `web-dashboard/backend/pom.xml`, and restore the
`SPREADSHEET_ID` branch in `Main`.

```xml
<dependency><groupId>com.google.api-client</groupId><artifactId>google-api-client</artifactId><version>2.7.2</version></dependency>
<dependency><groupId>com.google.apis</groupId><artifactId>google-api-services-sheets</artifactId><version>v4-rev20251110-2.0.0</version></dependency>
<dependency><groupId>com.google.auth</groupId><artifactId>google-auth-library-oauth2-http</artifactId><version>1.55.0</version></dependency>
<dependency><groupId>com.google.oauth-client</groupId><artifactId>google-oauth-client-jetty</artifactId><version>1.39.0</version></dependency>
```

- `GoogleSheetsClient` — service account, A1 ranges, `USER_ENTERED` writes,
  `INSERT_ROWS` append, `batchUpdate` for row deletes.
- `SheetsAuth` — credential resolution (see below).

## Connect a real Google Sheet

Auth is resolved by `SheetsAuth` in this order: service-account key → OAuth
desktop client (`credentials.json`) → Application Default Credentials.
Pick the option your org's policy allows.

### Option A — OAuth desktop app (no service-account key needed)

Use this when `iam.disableServiceAccountKeyCreation` blocks JSON keys.

1. Cloud Console → **APIs & Services → Library** → enable *Google Sheets API*.
2. **OAuth consent screen** → Internal (Workspace org) or External → set app
   name + emails → on *Scopes* add `.../auth/spreadsheets` → on *Test users*
   add your Google account → leave status **Testing**.
3. **Credentials → Create Credentials → OAuth client ID → Desktop app** →
   download the JSON → save as `credentials.json` in the project root
   (git-ignored). Desktop-app type needs no redirect URI config.
4. Sign-in account must have **edit access to the spreadsheet**.
5. Run:

```powershell
$env:SPREADSHEET_ID = "1AbC..."   # from docs.google.com/spreadsheets/d/<ID>/edit
& "...mvn.cmd" -q exec:java
```

A browser opens on first run ("unverified app" → **Advanced → continue**);
the token is cached in `tokens/` so restarts won't re-ask. Testing-mode
refresh tokens can expire after ~7 days — just re-run.

### Option B — Service account (if keys are allowed)

1. Create a service account + JSON key (save as `service-account.json`,
   git-ignored) and **share the spreadsheet** with its `client_email` (Editor).
2. Run:

```powershell
$env:SERVICE_ACCOUNT_KEY_PATH = "D:\demos\attendance_extraction\service-account.json"
$env:SPREADSHEET_ID = "1AbC..."
& "...mvn.cmd" -q exec:java
```

### Option C — Application Default Credentials

If `gcloud` is installed and allowed: `gcloud auth application-default login`,
set `SPREADSHEET_ID`, and run — no key or client-secret file needed.

The header row of the **first tab** defines the table columns. Put the expected
columns (e.g. `Employee ID, Name, Date, Status, Hours`) in row 1.

## Notes & limits

- `USER_ENTERED` input option: writes parse like typing (numbers, dates,
  formulas). Switch to `RAW` for literal storage.
- Sheets API quota: ~300 read + 300 write req/min/project — batch where possible.
- No transactions; concurrent appends can interleave. Fine for append-heavy
  operational data, not for strict consistency needs.

