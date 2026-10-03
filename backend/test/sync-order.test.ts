import { describe, expect, it } from 'vitest';
import { Hono } from 'hono';
import { syncRoutes } from '../src/routes/sync';
import { orderSyncChanges } from '../src/sync-order';
import type { Bindings, Variables, SyncChange } from '../src/types';

const timestamp = '2026-10-03T02:00:00+05:30';
const changes: SyncChange[] = [
  { entity: 'transaction', operation: 'UPSERT', data: { id: 'expense-new', amountPaise: 2000, categoryId: 'category-new', accountId: 'account-new', paidByUserId: 'user-shrut', paymentMethod: 'UPI', occurredAt: timestamp, updatedAt: timestamp } },
  { entity: 'account', operation: 'UPSERT', data: { id: 'account-new', name: 'Test account', paymentMethod: 'UPI', updatedAt: timestamp } },
  { entity: 'category', operation: 'UPSERT', data: { id: 'category-new', name: 'Test category', updatedAt: timestamp } },
];

describe('offline sync dependencies', () => {
  it('uploads parent records before their expense without mutating the upload', () => {
    expect(orderSyncChanges(changes).map(c => c.entity)).toEqual(['category', 'account', 'transaction']);
    expect(changes[0]!.entity).toBe('transaction');
    const remove: SyncChange = { ...changes[1]!, operation: 'DELETE' };
    expect(orderSyncChanges([changes[1]!, remove])).toEqual([changes[1]!, remove]);
  });

  it('accepts an expense and its newly created account/category in one request', async () => {
    const parents = new Set<string>();
    const inserted: string[] = [];
    const db = { async batch(statements: Array<{ run: () => Promise<unknown> }>) { for (const statement of statements) await statement.run(); return []; }, prepare(sql: string) {
      let args: unknown[] = [];
      const statement = {
        bind(...values: unknown[]) { args = values; return statement; },
        async first() { return null; },
        async all() { return { results: [] }; },
        async run() {
          if (sql.startsWith('INSERT INTO transactions')) {
            if (!parents.has(String(args[3])) || !parents.has(String(args[6]))) throw new Error('FOREIGN KEY constraint failed');
            inserted.push(String(args[0]));
          } else if (sql.startsWith('INSERT INTO categories') || sql.startsWith('INSERT INTO accounts')) parents.add(String(args[0]));
          return { success: true };
        },
      };
      return statement;
    } };
    const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();
    app.use('*', async (c, next) => { c.set('auth', { userId: 'user-shrut', deviceId: 'test-device' }); await next(); });
    app.route('/sync', syncRoutes);
    const response = await app.request('/sync', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ changes }) }, { DB: db } as unknown as Bindings);
    expect(response.status).toBe(200);
    expect(inserted).toEqual(['expense-new']);
  });
});
