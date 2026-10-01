# Next steps

The [plan audit](plan-audit.md) tracks every planned phase and records specific SMS and notification gaps.

## Current checkpoint

The Android SDK is installed locally. The Firebase-configured debug APK and Android unit tests passed on 1 October 2026. Backend typechecking, crypto tests, migrations, and a local login/create/analytics smoke test passed previously; the local Worker health endpoint also passed. This is an initial implementation, not a fully validated V1: HyperOS canceled the first APK install pending device approval, so physical-device behavior and multi-device consistency are still unverified.

## Implementation work before household rollout

1. Fix synchronization correctness: push catalogs before dependent transactions; normalize timestamps; handle pagination, retries, concurrent edits during sync, and atomic acknowledgment. Test offline phone A → server → phone B, deletions, and conflicts.
2. Implement automatic access-token refresh, revoked-device checks, login rate limiting, safe PIN changes, and offline PIN unlock. Ensure phones without enrolled biometrics can enter the app without a login loop.
3. Complete expense date/time selection, current-user and last-payment defaults, previous-month comparison, report month selection, date/category/payment filters, CSV date ranges, category rename, device management, sync status, and merchant-learning prompts.
4. Harden capture: multipart SMS, RECEIVE_SMS permission flow, reference/time-window deduplication across sources and devices, failed-payment exclusions, real anonymized fixtures, and review editing.
5. Verify the configured Firebase Messaging SDK/plugin integration, then implement FirebaseMessagingService, notification permission/channel, device-token registration/rotation, and server sending. The Gradle dependencies are configured; push delivery is not yet implemented or verified.
6. Add meaningful Room, repository, API, analytics, and sync integration tests. Current unit coverage is limited to parser/fingerprint cases and backend cryptography.

## Physical-device test session

Start with one Redmi, then repeat on all three. Enable Developer options by tapping the OS/MIUI version repeatedly in Settings, enable USB debugging, connect with a data-capable USB cable, and accept the computer authorization prompt. Menu labels vary by OS version.

Use Android Studio's device selector or run `adb devices`. For a local backend over USB, build with `-PAPI_BASE_URL=http://127.0.0.1:8787/`, run `adb reverse tcp:8787 tcp:8787`, and start Wrangler on the computer. The default emulator address 10.0.2.2 does not work on a physical phone.

Install the resulting debug APK with `adb install -r app/build/outputs/apk/debug/app-debug.apk` from android/. With multiple devices, use `adb -s DEVICE_SERIAL` for install and reverse commands.

Test and record:

- Sign in, close/reopen, fingerprint success/cancel, and PIN fallback.
- Disable connectivity after login; add ₹420 Groceries/Cash; verify immediate display and persistence after restart. Edit and delete offline.
- Restore connectivity and tap Sync now. Verify a second phone receives the same data and totals, including edits and deletions.
- Confirm that DETECTED items do not count toward reports until confirmed; ignored items do not count.
- Grant SMS/notification permissions deliberately. Scan anonymized fixtures and validate one review item per payment, amount in paise, and no raw message storage/upload.
- Export CSV and inspect amounts, quoting, category/member labels, and non-ASCII text.
- Test large font size, tap targets, screen locking, process restart, battery restrictions, and notification-listener persistence. Record phone model and OS version with each issue.

For hands-on debugging, leave one authorized phone connected by USB. Use targeted logcat/app diagnostics; do not collect or share complete SMS inboxes or unrelated device logs.

## Firebase configuration

1. Open https://console.firebase.google.com/ and create an Accountant project. Google Analytics is optional for this app.
2. Add an Android app with the exact application ID `com.shrutsureja.accountant`, nickname Accountant. FCM setup does not require a signing fingerprint.
3. Download `google-services.json` to `android/app/google-services.json`. It is ignored by Git. This file contains app/project identifiers, not a service-account private key.
4. The Messaging SDK dependency and Google Services plugin are configured. The plugin activates when the local config file exists; this build passed. Implement token registration, service, permission prompt, and notification channel.
5. After integration, retrieve the test device's FCM token through a deliberate diagnostic flow and send a test message from Firebase Messaging with the app in the background. Verify permission, delivery, tap behavior, and token rotation.
6. For automated Worker sending, configure FCM HTTP v1 authorization through server-side credentials. Store private credentials only as Cloudflare secrets; never put them in the Android app, repository, or chat.

References: https://firebase.google.com/docs/android/setup and https://firebase.google.com/docs/cloud-messaging/android/get-started

## Production backend

Create D1, configure its real ID, provision a JWT secret, and deploy only after authentication/sync checks above pass. Separate development seed users/PINs from production provisioning. Set the Android build's API URL to the deployed HTTPS Worker endpoint. Firebase is for push delivery; expenses remain in Room/D1 and login uses the existing household API.
