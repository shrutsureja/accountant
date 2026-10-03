import type { SyncChange } from './types';

// Offline expenses can reference accounts/categories created in the same upload.
// Preserve operation order within each entity type while applying parents first.
export function orderSyncChanges(changes: SyncChange[]): SyncChange[] {
  const rank = { category: 0, account: 1, merchant_rule: 2, transaction: 3 };
  return [...changes].sort((a, b) => rank[a.entity] - rank[b.entity]);
}
