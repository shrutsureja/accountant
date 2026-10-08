# Cloudflare deployment

Production uses Worker `accountant-api`, D1 `accountant-production`, and the custom hostname `api.accountant.shrutsureja.com`. Use `backend/wrangler.production.toml` explicitly; the default configuration remains for local development. Public workers.dev and preview URLs are disabled.

## Deploy an update

```sh
cd backend
npx wrangler whoami
npm test
npm run typecheck
npx wrangler d1 migrations apply accountant-production --remote --config wrangler.production.toml
npx wrangler deploy --config wrangler.production.toml
curl --fail https://api.accountant.shrutsureja.com/health
```

`JWT_SECRET` is installed as a Worker secret, not a source-code variable. Production PINs differ from development seeds. Never rerun the initial data import against a populated production database: it would overwrite later edits.

## Private APK

```sh
python3 scripts/build_apk.py --version 1.1.3 --code 5 --api-url https://api.accountant.shrutsureja.com/
```

Output: `dist/Accountant-1.1.3-build-5-release.apk`. This file is local and ignored by Git; share it directly with family. Production policy advertises the latest available build, with minimum build 1. Build and distribute future APKs before increasing the minimum.

Existing development installations use a different signing key. Do not uninstall them until all pending transactions are synced and data is backed up. A debug-signed build targeting the cloud can update these installations without deleting their data; the signed release is for clean installations or a deliberate backed-up migration.

## Initial migration — 3 October 2026

A private local SQLite snapshot remains under `~/.config/accountant-build/backups/`. At the user's request, production starts with **zero transactions** and zero merchant rules. Only the three family accounts, default categories and Cash account are retained. Local development refresh sessions were not migrated.

Do not install a cloud-targeted debug build over a development installation containing old data: pending local expenses could upload, and already-synced local expenses would remain visible. Use a fresh release installation for the clean production start, after preserving any local data the user wants to keep.

Private family login PINs are in `~/.config/accountant-build/family-logins.json`, readable only by the local user. Keep that file and signing material outside Git. The JWT backup is in the same restricted configuration folder. The one-time import file is obsolete and must not be reused.

Production PIN hashes use PBKDF2-SHA256 with 100,000 iterations, matching the deployed Workers limit; the earlier 310,000-iteration local hashes caused production login failures.

Cloudflare automatically provisions the custom-domain certificate. A successful deploy command alone does not confirm that HTTPS, login, or phone sync works; verify those before sharing the APK.

## Verified deployment

HTTPS health and version-policy checks pass. Unauthenticated member access returns 401. Shrut, Alpa, and Hitesh have each logged in and synced. On 3 October, the production database contained 53 transactions, 16 categories, and 2 accounts after Shrut's pending phone changes uploaded; no duplicate transaction fingerprint groups were found. Build 3 synced successfully after the server-side batching fix, then build 4 was installed over it on Shrut's Redmi with local data preserved. Build 4 synced successfully and showed notification capture and expense alerts enabled. Backend tests, typecheck, Android unit tests, and the signed release build pass. The release APK remains local and has not been publicly uploaded.

Sync applies category and account changes before dependent transactions in a database batch. Archiving either catalog item keeps its row and ID, so another phone can still sync an expense referencing it; the archived item is excluded from new selections. The sync timestamp migration keeps client edit time for conflict resolution and records server arrival time for downloads, so an old offline expense uploaded later reaches other phones. Different timezone offsets are compared as instants. A private production database export was taken before this migration. A genuine incoming payment notification has not yet been observed end to end with build 4.

On 8 October, a fresh private D1 export was taken before changing categories. `Investment` was renamed to `Zerodha`, and `SIP` and `LIC` were added. Five confirmed expenses identified by merchant or note as SIP were moved to SIP, and the confirmed LIC expense was moved to LIC. Four confirmed toll-plaza expenses remain in Other pending the user's preferred Transport classification. Ignored transactions were not recategorized.

Build 5 adds an Ignored tab in Needs Review with Restore, a one-step Save & confirm action for incomplete detections, full member-name chips in Transactions, and automatic account creation only when a trusted bank source identifies the funding account suffix. The signed build passes Android tests and release lint; no USB phone was connected for an end-to-end device check on 8 October. Build 5 uses the same signing certificate as build 4, so it can update an existing release installation without deleting local data.
