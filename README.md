# Accountant

See [next steps and device/Firebase testing](docs/next-steps.md) for the implementation checkpoint and remaining work before household rollout.

Private, offline-first household expense tracking for Android. The repository contains:

- `android/` — Kotlin, Jetpack Compose, Room, WorkManager, Hilt application.
- `backend/` — Cloudflare Worker, Hono API, and D1 migrations.
- `docs/` — API and local setup notes.

## Backend

```bash
cd backend
npm install
npm test
npm run dev
```

Apply the local D1 schema before first use:

```bash
npx wrangler d1 migrations apply accountant-db --local
```

Seeded development logins are `shrut`, `mom`, and `dad`, displayed as Shrut, Alpa, and Hitesh respectively, each with PIN `123456`. Change the PINs before a real deployment.

## Android

Install a JDK before opening the project. On Ubuntu/Debian:

```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

Then open `android/` in Android Studio and choose that JDK under **Settings → Build Tools → Gradle → Gradle JDK**. The checked-in Gradle wrapper handles the Gradle installation.

The default debug API URL is `http://10.0.2.2:8787/`, which reaches a Worker running on the development computer from the Android emulator. Override it for a deployed Worker with:

```bash
cd android
./gradlew assembleDebug -PAPI_BASE_URL=https://your-worker.example.workers.dev/
```

Release builds reject cleartext HTTP. Debug builds permit it only for local Worker development.

The UI reads exclusively from Room. Manual expenses are committed locally first and synchronization runs independently through WorkManager.

The debug APK is produced at `android/app/build/outputs/apk/debug/app-debug.apk`.

### Local backend on a USB-connected phone

The phone debug build uses `http://127.0.0.1:8787/` only when built with that `API_BASE_URL`. On a phone, this address reaches the phone itself unless ADB forwards it to the computer. Keep the Worker running in one terminal:

```bash
cd backend
npm run dev -- --ip 127.0.0.1 --port 8787
```

With the unlocked phone connected and authorized for USB debugging, run in another terminal:

```bash
adb devices
adb reverse tcp:8787 tcp:8787
cd android
./gradlew assembleDebug -PAPI_BASE_URL=http://127.0.0.1:8787/
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Check `http://127.0.0.1:8787/health` on the computer first. ADB reverse forwarding disappears when the phone disconnects; reconnect it and run `adb reverse tcp:8787 tcp:8787` again. If the Worker stops, restart it. The emulator uses the default `10.0.2.2` address instead and does not need USB forwarding.

For local UI testing on your own debug build, add `-PLOCAL_AUTH_BYPASS=true` to the Gradle command to skip the biometric prompt. Login with the account PIN is still required. The flag defaults to `false`, and release builds always disable the bypass. Do not distribute a debug APK built with the bypass enabled.

## UI redesign

The [phased UI redesign plan](docs/ui-redesign-plan.md) and [visual reference](docs/design/accountant-ui-reference.png) are checked in for review. The redesign keeps the existing Compose navigation, ViewModels, Room data flow, and backend contracts. It starts with a blue-neutral theme and shared components, then proceeds through Add Expense, simple/detailed Home, Transactions, Needs Review, Reports, and Profile/Categories. Each phase is built and tested before the next begins.

The five bottom destinations remain Home, Transactions, Add, Reports, and Profile. The visual reference illustrates layout and hierarchy; its example amounts are not application fixtures.

## Security and privacy

Amounts are integer paise throughout. PINs use salted PBKDF2 hashes supported by the Cloudflare Web Crypto runtime. Refresh tokens are only stored hashed in D1. SMS and notification bodies are parsed on-device and are never sent to the API.

## Private family APK builds

Use `python3 scripts/build_apk.py --version 1.1.0 --code 2 --api-url https://api.accountant.shrutsureja.com/` to produce a signed `dist/Accountant-1.1.0-build-2-release.apk` locally. Nothing is uploaded. The cloud backend must be deployed before this URL can be used by family phones.

See [local build and update instructions](docs/local-apk-builds.md) for signing-key backup, debug builds, parent login names, and optional/mandatory app updates.
