# Accountant UI redesign plan

The existing Kotlin/Jetpack Compose app remains the foundation. This plan changes presentation in stages while preserving offline-first Room storage, the repository, sync, API contracts, and the five destinations: Home, Transactions, Add, Reports, Profile. The supplied [reference image](design/accountant-ui-reference.png) guides hierarchy, density, and color; it is not a literal screen specification or source of live data.

## Current structure

- `MainActivity.kt` owns authentication gating, the Compose navigation graph, the bottom bar, and profile action wiring.
- `ui/Screens.kt` holds all current screens and their local form state; `ui/Components.kt` has shared money, section, empty/error, and transaction components.
- `ui/ViewModels.kt` exposes Room-backed `StateFlow` state and actions through `AccountantRepository`. The repository uses `AccountantDao` and `AccountantApi`; `AuthStore` persists session preferences.
- `ui/theme/Theme.kt` has a small green Material theme. No chart library or Compose instrumentation-test dependency is currently present.

## Phases

1. **Theme and shared foundation.** Add blue-neutral color, typography, shape, and spacing tokens; reusable basic controls; compact, readable five-item navigation. Build and run tests.
2. **Add Expense.** Make amount entry prominent, replace large category/member/account dropdowns with bottom sheets, and keep the existing `AddViewModel.save` path. Verify creation and offline persistence; build and test.
3. **Home.** Add simple and detailed layouts sharing the same Room data. Resolve a per-user `UiMode` centrally, defaulting parents to simple and Shrut to detailed, with a lightweight persistent preference. Build and test both modes.
4. **Transactions.** Group rows by date, refine search/filter presentation, and preserve edit/delete. Add only the small Room/query support needed for useful filters. Build and test.
5. **Needs Review.** Show compact detected-expense rows with Confirm, Ignore, and Edit. Edit needs a small ViewModel/repository addition because the current screen only exposes Confirm and Ignore. Build and test.
6. **Reports.** Draw a compact daily chart in Compose from existing `dailyTotals`; show ranked categories, people, and summary metrics from existing totals. Avoid a chart dependency unless necessary. Build and test.
7. **Profile and Categories.** Group existing settings and actions, simplify category management, and retain account/export/sync/permission flows. Build and test.

After each phase, review the changed files, verify the Android build and tests, and stop if verification fails. Do not change backend contracts or Room schemas solely for presentation. Add focused Compose UI tests where practical. Keep raw SMS and personal data out of UI debugging artifacts.

## Progress

- Phase 1: blue-neutral theme, typography, shape/spacing tokens, shared basic components, and five-item navigation implemented. Android unit tests and debug build passed locally; the backend and Android CI jobs also passed. On-device visual confirmation is pending because the Redmi disconnected from ADB during this phase.
