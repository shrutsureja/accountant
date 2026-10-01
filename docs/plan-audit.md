# Plan audit (1 October 2026)

This compares the current repository with `accountant_product_engineering_plan.md`. "Implemented" means code exists; it does not imply the plan's real-device acceptance criteria have passed.

| Plan phases | Current state | Remaining acceptance work |
| --- | --- | --- |
| 0: repository | Android and Worker projects, migrations, README, API notes exist. | Add basic CI; verify Worker deployment and D1 in the chosen cloud environment. |
| 1: login and shell | Login, encrypted local tokens, biometric prompt, navigation, and automatic token refresh exist. Login works on one Redmi. | Test biometric/PIN fallback, revoked-device checks, and PIN management. |
| 2: manual expenses | Room-backed create, list, edit, delete; offline create/persistence and synced edit/delete tested on one Redmi. | Fix current-user/last-payment defaults; finish date/time selection and broader device tests. |
| 3: sync | WorkManager and `/sync` run on the Redmi. First sync, token refresh, edit, and deletion passed against local D1. | Fix catalog/transaction ordering, pagination, concurrent edits, atomic acknowledgement, and conflict tests on two devices. |
| 4: dashboard | Current and previous-month spending visible on Home; Home/Reports allow month navigation; confirmed totals and breakdowns verified for September. Uncategorized expenses appear in category breakdown. | Add explicit month-over-month comparison and broader analytics tests. |
| 5: SMS | Receiver assembles multipart messages; parser handles synthetic BOB/Kotak/SBI/UPI examples and excludes failed/pending/mandate notices; live permission action exists. No raw SMS body is in the transaction DTO. | Test live delivery and 30-day scanning on device, extend bank-specific adapters/anonymized fixtures, improve time/account extraction and deduplication. |
| 6: notifications | Listener and support for Google Pay, PhonePe, Paytm exist. | Add supported banking apps and tailored parsers; test listener persistence and access prompt on Redmi. |
| 7: deduplication | A SHA-256 fingerprint and exact-match Room query exist. | Replace five-minute bucket equality with a true ±5-minute match; reconcile SMS and notification details, reference IDs, and cross-device duplicates; make insert atomic. |
| 8: review | Confirm/ignore and category suggestion exist. | Add review editing before confirmation and device acceptance tests. |
| 9: merchant learning | Rule storage and suggestion lookup exist. | Add remember-category prompt and a tested rule update flow. |
| 10–12: analytics/search/CSV | Basic reporting, month selection, text search, and CSV sharing exist. | Add prior-month comparison, category/member/date/payment filters, CSV range selection, and calculation/export tests. |
| 13: push | Firebase config, Gradle plugin, and Messaging dependency are configured. | Implement service, notification permission/channel, token lifecycle, backend FCM sending, and delivery tests. |
| 14: 30-day import | Inbox scanner creates detected transactions and now counts only newly inserted candidates. | Show first-run Scan/Skip choice, guard permissions/errors, and verify duplicate behavior on device. |
| 15: family testing | One Redmi connected; login, offline persistence, sync create/edit/delete, and historical dashboard were tested. | Complete acceptance tests on all three phones, including Xiaomi battery/background behavior, accessibility, and family usability. |

## SMS-specific findings

- The bank-specific parsers still delegate to a generic regex; there is no Axis adapter or distinct UPI adapter. BOB and Kotak wrappers and synthetic tests have been added.
- The receiver now joins multipart segments before parsing, but live delivery is not yet verified on-device.
- The UI has a separate `RECEIVE_SMS` permission action for live capture; history scanning requests `READ_SMS`.
- `ParsedTransaction` has no bank, direction, or extracted transaction time. The receiver uses the SMS delivery timestamp.
- The generic parser now excludes failed, pending, reversed, canceled, request, and mandate messages. More anonymized regression cases are needed.
- Reference-only fingerprints can merge unrelated transactions with reused references, while details that differ across SMS and notifications can prevent matching the same payment.
- The 30-day scanner now increments the displayed count only for inserted candidates; device verification remains.
- Detection before a valid user session attributes a candidate to the first seeded member; this needs explicit ownership behavior.

The user explicitly authorized inspecting this phone's SMS. Inspection found BOB transfer/Dr/Cr and Kotak “Sent Rs” formats that the earlier generic parser missed. SBI samples were mostly incoming credits or card notices, which should stay out of expense totals. UPI mandate/request messages should also stay out. Only redacted patterns and aggregate findings were used; raw messages were not committed.
