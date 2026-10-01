# Accountant API

Base path: `/api/v1`. Every endpoint except `/health`, `/auth/login`, and `/auth/refresh` requires `Authorization: Bearer <access token>`.

Authentication uses `POST /auth/login`, `POST /auth/refresh`, and `POST /auth/logout`. Household members are returned by `GET /members`.

Transactions support list, create, detail, update, soft delete, and review actions under `/transactions`. List filters are `status`, `search`, `member`, `from`, and `to`.

Catalog resources are `/categories`, `/accounts`, and `/merchant-rules`. Analytics endpoints are `/analytics/monthly`, `/daily`, `/weekly`, `/categories`, `/members`, and `/month-comparison`; each accepts a `month=YYYY-MM` query.

`POST /sync` accepts a last synchronization timestamp plus client changes. Changes are applied with last-write-wins semantics, and every server row updated after that timestamp is returned. Entity names are `transaction`, `category`, `account`, and `merchant_rule`.

All timestamps are ISO 8601 with offsets. All money values are integer paise. Only structured SMS/notification fields are valid transaction data; raw message bodies are intentionally absent from the contract.

