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
3. **Home.** Add simple and detailed layouts sharing the same Room data. Resolve a per-user `UiMode` centrally, defaulting Alpa and Hitesh to simple and Shrut to detailed, with a lightweight persistent preference. Build and test both modes.
4. **Transactions.** Group rows by date, refine search/filter presentation, and preserve edit/delete. Add only the small Room/query support needed for useful filters. Build and test.
5. **Needs Review.** Show compact detected-expense rows with Confirm, Ignore, and Edit. Edit needs a small ViewModel/repository addition because the current screen only exposes Confirm and Ignore. Build and test.
6. **Reports.** Draw a compact daily chart in Compose from existing `dailyTotals`; show ranked categories, people, and summary metrics from existing totals. Avoid a chart dependency unless necessary. Build and test.
7. **Profile and Categories.** Group existing settings and actions, simplify category management, and retain account/export/sync/permission flows. Build and test.

After each phase, review the changed files, verify the Android build and tests, and stop if verification fails. Do not change backend contracts or Room schemas solely for presentation. Add focused Compose UI tests where practical. Keep raw SMS and personal data out of UI debugging artifacts.

## Progress

- Phase 1: blue-neutral theme, typography, shape/spacing tokens, shared basic components, and five-item navigation implemented. Android unit tests, debug build, and both CI jobs passed. Verified on the Redmi Note 13 5G: all five navigation labels fit on one line, and a manual sync reached the local Worker with HTTP 200 after restoring USB port forwarding.
- Phase 2: prominent amount entry, compact selectors, category/member/account bottom sheets, payment and date controls, and a persistent Save action. The category picker puts up to five categories used in confirmed transactions during the past 90 days first; remaining categories sort by name. Manual entry requires a category and excludes the internal Not categorized fallback; Other remains available. Android unit tests and debug build passed. On the Redmi, a ₹1.23 test expense saved offline, appeared in Transactions, synced after USB forwarding was restored, and was deleted and synced again. Household display names are Alpa, Hitesh, and Shrut; login usernames and member IDs remain stable.
- Phase 3: Simple Home shows monthly spending, review queue, Add, recent expenses, and a concise person summary. Detailed Home shows monthly comparison, today/week/review metrics, household and category breakdowns, and recent expenses. A single `UiModeResolver` derives defaults from the signed-in username: Alpa (`mom`) and Hitesh (`dad`) use Simple; Shrut (`shrut`) uses Detailed. Member names come from the synced catalog. Android unit tests and debug build passed; Detailed Home was inspected on the Redmi. A parent-account device login remains to be checked.
- Phase 4: Transactions has grouped date headings, search, month/category/person filters, compact rows, and the existing edit/delete actions. Filtering runs through Room so offline behavior is retained. Android unit tests and debug build passed. On the Redmi, the date groups and renamed household members rendered correctly; the current-month filter showed an empty result for October and the category/person selector sheets opened with the expected options.
