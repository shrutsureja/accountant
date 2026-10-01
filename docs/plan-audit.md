# Plan audit (1 October 2026)

This compares the current repository with `accountant_product_engineering_plan.md`. "Implemented" means code exists; it does not imply the plan's real-device acceptance criteria have passed.

| Plan phases | Current state | Remaining acceptance work |
| --- | --- | --- |
| 0: repository | Android and Worker projects, migrations, README, API notes exist. | Add basic CI; verify Worker deployment and D1 in the chosen cloud environment. |
| 1: login and shell | Login, encrypted local tokens, biometric prompt, navigation exist. | Test on Redmi; fix PIN fallback without logging out, token refresh, revoked-device checks, and PIN management. |
| 2: manual expenses | Room-backed create, list, edit, delete are present. | Test offline persistence and date/member defaults on device; finish expense date/time selection. |
| 3: sync | WorkManager and `/sync` exist. | Fix catalog/transaction ordering, pagination, concurrent edits, atomic acknowledgement, and conflict tests on two devices. |
| 4: dashboard | Basic totals and breakdowns exist. | Add previous-month comparison and validate all totals against confirmed Room transactions. |
| 5: SMS | Receiver, local generic parser, thin HDFC/SBI/ICICI wrappers, and review candidate creation exist. No raw SMS body is in the transaction DTO. | Ask for RECEIVE_SMS at runtime; assemble multipart SMS; add real bank/UPI adapters and anonymized fixtures; handle failed/reversed payments, exact paise, timestamps, and account/bank fields; run opt-in device tests. |
| 6: notifications | Listener and support for Google Pay, PhonePe, Paytm exist. | Add supported banking apps and tailored parsers; test listener persistence and access prompt on Redmi. |
| 7: deduplication | A SHA-256 fingerprint and exact-match Room query exist. | Replace five-minute bucket equality with a true ±5-minute match; reconcile SMS and notification details, reference IDs, and cross-device duplicates; make insert atomic. |
| 8: review | Confirm/ignore and category suggestion exist. | Add review editing before confirmation and device acceptance tests. |
| 9: merchant learning | Rule storage and suggestion lookup exist. | Add remember-category prompt and a tested rule update flow. |
| 10–12: analytics/search/CSV | Basic reporting, text search, and CSV sharing exist. | Add previous-month comparison, category/member/date/payment filters, report month selection, CSV range selection, and calculation/export tests. |
| 13: push | Firebase config, Gradle plugin, and Messaging dependency are configured. | Implement service, notification permission/channel, token lifecycle, backend FCM sending, and delivery tests. |
| 14: 30-day import | Inbox scanner exists and creates detected transactions. | Show first-run Scan/Skip choice, count only newly inserted candidates, guard permissions/errors, and verify duplicates on device. |
| 15: family testing | First Redmi connected for development. | Complete acceptance tests on all three phones, including Xiaomi battery/background behavior, offline use, accessibility, and family usability. |

## SMS-specific findings

- The current bank-specific parsers only check a bank keyword and delegate to the same generic regex; there is no Axis adapter or distinct UPI adapter.
- The receiver parses each SMS segment separately, so multipart payments may be missed.
- The UI requests `READ_SMS` only for history scanning; live capture also requires `RECEIVE_SMS` permission.
- `ParsedTransaction` has no bank, direction, or extracted transaction time. The receiver uses the SMS delivery timestamp.
- The generic parser can treat a pending or failed payment message as a debit when it contains “paid” or “debited.” It also lacks anonymized examples from the household banks.
- Reference-only fingerprints can merge unrelated transactions with reused references, while details that differ across SMS and notifications can prevent matching the same payment.
- The 30-day scanner increments its displayed count even when repository deduplication skips the message.
- Detection before a valid user session attributes a candidate to the first seeded member; this needs explicit ownership behavior.

No raw SMS inbox or live banking notifications were inspected for this audit. The current device session should use synthetic or user-anonymized samples until the user explicitly opts in to scanning their private messages.
