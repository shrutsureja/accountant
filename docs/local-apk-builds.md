# Local family APK builds

Builds stay on your PC. No GitHub Release, email delivery, or public APK hosting is configured.

## Build

From the repository root, with Java 21 and the Android SDK configured:

```sh
python3 scripts/build_apk.py --version 1.1.0 --code 2 --api-url https://api.accountant.shrutsureja.com/
```

The output is `dist/Accountant-1.1.0-build-2-release.apk`, plus a SHA-256 checksum file. Share the APK directly with family once the backend URL is deployed and tested. The current cloud URL is a deployment target, not confirmation of a running service.

For the next update use a higher Android build code, for example `--version 1.1.1 --code 3`. The script remembers successful builds and rejects decreasing codes. Rebuilding the same version/code is allowed. The display version and build code appear on Login and Profile.

On the first release build, the script creates a signing key and password under `~/.config/accountant-build/`, restricted to your user. Back up this whole folder privately and keep using it for every release. Do not commit it. `ACCOUNTANT_BUILD_HOME` can point to a restored signing folder. The script runs Android unit tests before packaging and never embeds the signing password in the APK.

Debug/local testing:

```sh
python3 scripts/build_apk.py --version 1.1.0 --code 2 --api-url http://127.0.0.1:8787/ --debug
```

The debug filename ends in `-debug.apk`. A phone needs USB forwarding (`adb reverse tcp:8787 tcp:8787`) for that PC-local address. A second phone must use its own USB forwarding or a reachable LAN backend. Family release builds require HTTPS. The script leaves biometric bypass disabled for both variants.

The current development install uses a debug signing key. A release APK cannot be installed over that APK with a different signing key. Preserve/sync the phone's expenses before any migration; do not uninstall the current app to work around the signature check without a verified backup. Fresh installations on parents' phones should use the release key from the beginning.

## Login names and shared UI

All three members have the same dashboard and options. Login usernames are `shrut`, `alpa`, and `hitesh`. Migration `0004_login_names.sql` changes the parent usernames while preserving user IDs, PINs, and transaction ownership. Apply migrations to each backend database being used; the migration has been applied locally, not to a cloud database.

## App update policy

The app checks `GET /api/v1/app/version` on startup/resume and after login. The Worker configuration exposes:

- `APP_LATEST_VERSION_CODE`: latest available build, currently `2`.
- `APP_LATEST_VERSION_NAME`: display name, currently `1.1.0`.
- `APP_MIN_VERSION_CODE`: oldest allowed build, currently `1`.

For a soft update to build 3, set latest to 3 and its display name, leaving minimum at 1. Existing supported builds get an optional notice with Later. For a mandatory update to build 3, also set minimum to 3. The app then blocks normal screens and asks the user to contact Shrut for the APK; the API returns HTTP 426 for requests reporting an older build. Version headers are compatibility controls, not authentication.

The policy endpoint remains available without login even when an update is mandatory. Successful policies are cached per backend address. A failed check or a four-second timeout uses the cached policy: absence of a policy does not invent a mandatory update, and a previously confirmed mandatory update remains in force offline. Pending expenses are never removed by an update prompt. Optional notices can be dismissed for that version during the current app session.

Build the new APK and make it available to family before raising the minimum version. The build script does not deploy the backend or automatically change its update policy.

## Scope

The signed-in app syncs while open and connected, then every minute until it goes into the background or loses connectivity. Home has a manual Sync button beside the greeting; Profile retains manual sync. The old periodic background schedule is cancelled.

Profile SMS scanning offers This month, Last month, and Two months ago using calendar-month boundaries and original transaction dates. Needs Review identifies the source member from the transaction creator, independently of the selected payer.

90-day retention, cloud deployment, Git history cleanup, and release CI are not included. Transaction history remains local.
