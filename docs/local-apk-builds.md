# Local family APK builds

Local builds stay on your PC. No GitHub Release, email delivery, or public APK hosting is configured. A separate, manually triggered GitHub Actions workflow can publish a Play internal-testing bundle once its secrets are configured.

## Build

For the next Google Play internal-testing release (version code 9 is already live), build a signed App Bundle with a new version code:

```sh
python3 scripts/build_apk.py --version 1.1.9 --code 11 --api-url https://api.accountant.shrutsureja.com/ --bundle
```

The output is `dist/Accountant-1.1.9.aab`. The `11` is Android's internal, increasing version code; users see only `1.1.9`. Nothing is uploaded by this script. Play signs delivered installs with Google's app-signing key (`0B:A8:…:59:20`) and accepts bundles signed by the existing upload key (`31:AD:…:84:11`). Both the local APK and Play bundle use `~/.config/accountant-build/accountant-release.p12`; the previously created `accountant-play-upload.p12` is unused. Back up the active keystore and `signing-password` privately. The Play build cannot automatically recreate a missing upload key.

Because Google uses a different app-signing key, a Play install cannot replace an old locally signed APK in place. Verify every phone has synced, then uninstall the local APK and install the Play version one phone at a time. Uninstalling removes local-only data and permission settings.

## Automated internal Play releases

`.github/workflows/play-internal.yml` runs only when started from **GitHub → Actions → Publish internal Play release → Run workflow**. Enter a display version and a version code larger than every version already uploaded to Play. It runs unit tests, builds the AAB against the production HTTPS API, checks that its upload certificate is `31:AD:…:84:11`, and submits it to the `internal` track. It does not publish a production release or deploy the backend. Play may still review a release.

Enable the Google Play Developer API in a Google Cloud project. To publish using the existing Play Console admin account, configure an OAuth desktop client, authorize `shrutsureja.code@gmail.com` for the `https://www.googleapis.com/auth/androidpublisher` scope, and store its Application Default Credentials JSON privately. An OAuth consent app in external **Testing** status issues a refresh token that expires after seven days; unattended releases beyond that need an OAuth app in production status or a service account. Google also supports a separate service account, but it is not required for the workflow. Add these secrets to a GitHub environment named `google-play-internal`:

- `ACCOUNTANT_UPLOAD_KEYSTORE_BASE64`: base64 encoding of the existing `accountant-release.p12` file (single line).
- `ACCOUNTANT_UPLOAD_KEYSTORE_PASSWORD`: contents of the existing `signing-password` file.
- `GOOGLE_PLAY_CREDENTIALS_JSON`: the OAuth authorized-user JSON credential (or a service-account JSON credential if that route is chosen later).

Restrict who can run the environment, and never commit these values. An authorized-user JSON includes a refresh token with the Play Console user's privileges. The values are restored into temporary files on the GitHub runner. The workflow publishes only after you manually start it; ordinary pushes run CI but do not create a Play release.

From the repository root, with Java 21 and the Android SDK configured:

```sh
python3 scripts/build_apk.py --version 1.1.5 --code 7 --api-url https://api.accountant.shrutsureja.com/
```

The output is `dist/Accountant-1.1.5-build-7-release.apk`, plus a SHA-256 checksum file. Share the APK directly with family once the backend URL is deployed and tested. See `cloudflare-deployment.md` for production deployment status.

For the next update use a higher Android build code, for example `--version 1.1.4 --code 6`. The script remembers successful builds and rejects decreasing codes. Rebuilding the same version/code is allowed. The display version and build code appear on Login and Profile.

The original signing key and password are stored under `~/.config/accountant-build/`, restricted to your user. Back up this folder privately and keep using the same upload key for every Play release. Do not commit it. `ACCOUNTANT_BUILD_HOME` can point to a restored signing folder. The script refuses to create a replacement key if these files are missing, runs Android unit tests before packaging, and never embeds the signing password in the APK.

Debug/local testing:

```sh
python3 scripts/build_apk.py --version 1.1.5 --code 7 --api-url http://127.0.0.1:8787/ --debug
```

The debug filename ends in `-debug.apk`. A phone needs USB forwarding (`adb reverse tcp:8787 tcp:8787`) for that PC-local address. A second phone must use its own USB forwarding or a reachable LAN backend. Family release builds require HTTPS. The script leaves biometric bypass disabled for both variants.

The current development install uses a debug signing key. A release APK cannot be installed over that APK with a different signing key. Preserve/sync the phone's expenses before any migration; do not uninstall the current app to work around the signature check without a verified backup. Fresh installations on parents' phones should use the release key from the beginning.

## Login names and shared UI

All three members have the same dashboard and options. Login usernames are `shrut`, `alpa`, and `hitesh`. Migration `0004_login_names.sql` changes the parent usernames while preserving user IDs, PINs, and transaction ownership. Apply migrations to each backend database being used; the migration has been applied locally and to production.

## App update policy

The app checks `GET /api/v1/app/version` on startup/resume and after login. The Worker configuration exposes:

- `APP_LATEST_VERSION_CODE`: latest available build, currently `5`.
- `APP_LATEST_VERSION_NAME`: display name, currently `1.1.5`.
- `APP_MIN_VERSION_CODE`: oldest allowed build, currently `1`.

For a soft update to build 7, set latest to 7 and its display name, leaving minimum at 1. Existing supported builds get an optional notice with Later. To require build 7, set minimum to 7 only after distributing the APK. The app then blocks normal screens on older builds and asks the user to contact Shrut for the APK; the API returns HTTP 426 for requests reporting an older build. Version headers are compatibility controls, not authentication.

The policy endpoint remains available without login even when an update is mandatory. Successful policies are cached per backend address. A failed check or a four-second timeout uses the cached policy: absence of a policy does not invent a mandatory update, and a previously confirmed mandatory update remains in force offline. Pending expenses are never removed by an update prompt. Optional notices can be dismissed for that version during the current app session.

Build the new APK and make it available to family before raising the minimum version. The build script does not deploy the backend or automatically change its update policy.

## Scope

The signed-in app syncs while open and connected, then every minute until it goes into the background or loses connectivity. Home has a manual Sync button beside the greeting; Profile retains manual sync. The old periodic background schedule is cancelled.

Profile SMS scanning offers This month, Last month, and Two months ago using calendar-month boundaries and original transaction dates. Needs Review identifies the source member from the transaction creator, independently of the selected payer.

90-day retention and Git history cleanup are not included. Transaction history remains local.
