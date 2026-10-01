type Row = Record<string, unknown>;

export function mapTransaction(row: Row): Record<string, unknown> {
  return {
    id: row.id,
    amountPaise: row.amount_paise,
    currency: row.currency,
    categoryId: row.category_id,
    paidByUserId: row.paid_by_user_id,
    paymentMethod: row.payment_method,
    accountId: row.account_id,
    merchant: row.merchant,
    note: row.note,
    occurredAt: row.occurred_at,
    source: row.source,
    status: row.status,
    sourceReference: row.source_reference,
    fingerprint: row.fingerprint,
    createdByUserId: row.created_by_user_id,
    updatedByUserId: row.updated_by_user_id,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    deletedAt: row.deleted_at,
    version: row.version,
  };
}

export function mapCategory(row: Row): Record<string, unknown> {
  return { id: row.id, name: row.name, icon: row.icon, active: Boolean(row.active), createdAt: row.created_at, updatedAt: row.updated_at, deletedAt: row.deleted_at, version: row.version, updatedBy: row.updated_by };
}

export function mapAccount(row: Row): Record<string, unknown> {
  return { id: row.id, name: row.name, bankName: row.bank_name, last4: row.last4, ownerUserId: row.owner_user_id, paymentMethod: row.payment_method, active: Boolean(row.active), createdAt: row.created_at, updatedAt: row.updated_at, deletedAt: row.deleted_at, version: row.version, updatedBy: row.updated_by };
}

export function mapMerchantRule(row: Row): Record<string, unknown> {
  return { id: row.id, merchantPattern: row.merchant_pattern, categoryId: row.category_id, createdByUserId: row.created_by_user_id, createdAt: row.created_at, updatedAt: row.updated_at, deletedAt: row.deleted_at, version: row.version, updatedBy: row.updated_by };
}

