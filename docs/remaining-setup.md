# Device and cloud setup

The codebase builds and runs locally. These steps connect it to production services and validate hardware-dependent behavior.

See [next-steps.md](next-steps.md) for outstanding implementation work; cloud setup alone does not complete V1.

## Development computer

Install JDK 21 (`sudo apt install openjdk-21-jdk`) and select it as Android Studio's Gradle JDK. Install Android SDK 36 and Build Tools 36 from Android Studio's SDK Manager.

## Cloudflare

Create the `accountant-db` D1 database, replace the placeholder `database_id` in `backend/wrangler.toml`, and store a long random `JWT_SECRET` with `wrangler secret put JWT_SECRET`. Apply both migrations before deploying. Replace all development PINs immediately.

## Firebase push notifications

Create a private Android Firebase app for package `com.shrutsureja.accountant` and place `google-services.json` in `android/app/`. That file is ignored by Git. The Google Services plugin is applied when this file exists, and the Messaging SDK dependency is configured. Notification handling, device-token registration, and server sending still need implementation before push delivery can be tested.

## Family device validation

Use anonymized real bank and UPI messages to add bank-specific parser fixtures. On each Redmi phone, validate SMS permission, notification access, fingerprint unlock, battery optimization, auto-start, WorkManager sync, and notification-listener persistence.
