// @ts-expect-error Runtime Node 22 supports SQLite; installed types predate the experimental module.
import { DatabaseSync } from 'node:sqlite';
import { describe, expect, it } from 'vitest';
import { Hono } from 'hono';
import { syncRoutes } from '../src/routes/sync';
import type { Bindings, Variables } from '../src/types';

it('preserves an expense when another device archives its category', async () => {
  const sql = new DatabaseSync(':memory:');
  sql.exec(`PRAGMA foreign_keys=ON;
    CREATE TABLE categories(id TEXT PRIMARY KEY, name TEXT, icon TEXT, active INTEGER, created_at TEXT, updated_at TEXT, updated_by TEXT, deleted_at TEXT, client_updated_at TEXT, version INTEGER DEFAULT 1);
    CREATE TABLE accounts(id TEXT PRIMARY KEY, name TEXT, bank_name TEXT, last4 TEXT, owner_user_id TEXT, payment_method TEXT, active INTEGER, created_at TEXT, updated_at TEXT, updated_by TEXT, deleted_at TEXT, client_updated_at TEXT, version INTEGER DEFAULT 1);
    CREATE TABLE transactions(id TEXT PRIMARY KEY, category_id TEXT REFERENCES categories(id), amount_paise INTEGER, currency TEXT, paid_by_user_id TEXT, payment_method TEXT, account_id TEXT REFERENCES accounts(id), merchant TEXT, note TEXT, occurred_at TEXT, source TEXT, status TEXT, source_reference TEXT, fingerprint TEXT, created_by_user_id TEXT, updated_by_user_id TEXT, created_at TEXT, updated_at TEXT, deleted_at TEXT, client_updated_at TEXT, version INTEGER);
    CREATE TABLE merchant_rules(id TEXT, updated_at TEXT, client_updated_at TEXT);
    INSERT INTO categories(id,name,active,updated_at,client_updated_at) VALUES ('cat-shared','Shared',1,'2026-10-01T00:00:00Z','2026-10-01T00:00:00Z');
    INSERT INTO accounts(id,name,payment_method,active,updated_at,client_updated_at) VALUES ('account-shared','Shared account','UPI',1,'2026-10-01T00:00:00Z','2026-10-01T00:00:00Z');`);
  const adapter = { async batch(statements: Array<{ run: () => Promise<unknown> }>) { for (const statement of statements) await statement.run(); return []; }, prepare(query: string) {
    let args: Array<string | number | null> = [];
    const bound = {
      bind(...values: Array<string | number | null>) { args = values; return bound; },
      async first() { return sql.prepare(query).get(...args) ?? null; },
      async all() { return { results: sql.prepare(query).all(...args) }; },
      async run() { sql.prepare(query).run(...args); return { success: true }; },
    };
    return bound;
  } };
  const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();
  app.use('*', async (c, next) => { c.set('auth', { userId: 'user-shrut', deviceId: 'test-device' }); await next(); });
  app.route('/sync', syncRoutes);
  const at = '2026-10-03T10:00:00+05:30';
  const send = (changes: unknown[]) => app.request('/sync', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ changes }) }, { DB: adapter } as unknown as Bindings);
  expect((await send([{ entity: 'category', operation: 'DELETE', data: { id: 'cat-shared', updatedAt: at } }])).status).toBe(200);
  expect((await send([{ entity: 'transaction', operation: 'UPSERT', data: { id: 'expense-later', amountPaise: 2000, categoryId: 'cat-shared', paidByUserId: 'user-shrut', paymentMethod: 'CASH', occurredAt: at, updatedAt: at } }])).status).toBe(200);
  expect(sql.prepare('SELECT category_id FROM transactions WHERE id=?').get('expense-later')).toMatchObject({ category_id: 'cat-shared' });
  expect(sql.prepare('SELECT deleted_at FROM categories WHERE id=?').get('cat-shared')).toMatchObject({ deleted_at: expect.any(String) });
  expect((await send([{ entity: 'account', operation: 'DELETE', data: { id: 'account-shared', updatedAt: at } }])).status).toBe(200);
  expect((await send([{ entity: 'transaction', operation: 'UPSERT', data: { id: 'expense-with-archived-account', amountPaise: 2100, categoryId: 'cat-shared', accountId: 'account-shared', paidByUserId: 'user-shrut', paymentMethod: 'UPI', occurredAt: at, updatedAt: at } }])).status).toBe(200);
  expect(sql.prepare('SELECT account_id FROM transactions WHERE id=?').get('expense-with-archived-account')).toMatchObject({ account_id: 'account-shared' });
  expect(sql.prepare('SELECT deleted_at FROM accounts WHERE id=?').get('account-shared')).toMatchObject({ deleted_at: expect.any(String) });
  sql.prepare('INSERT INTO accounts(id,name,payment_method,active,updated_at,client_updated_at) VALUES (?,?,?,?,?,?)')
    .run('account-newer', 'Latest', 'UPI', 1, '2026-10-03T05:00:00Z', '2026-10-03T05:00:00Z');
  expect((await send([{ entity: 'account', operation: 'UPSERT', data: {
    id: 'account-newer', name: 'Stale', paymentMethod: 'UPI', active: false,
    updatedAt: '2026-10-03T09:00:00+05:30',
  } }])).status).toBe(200);
  expect(sql.prepare('SELECT name,active FROM accounts WHERE id=?').get('account-newer'))
    .toMatchObject({ name: 'Latest', active: 1 });
  sql.exec(`INSERT INTO categories(id,name,active,updated_at,client_updated_at) VALUES ('cat-inactive','Older category',1,'2026-10-01T00:00:00Z','2026-10-01T00:00:00Z');
    INSERT INTO accounts(id,name,payment_method,active,updated_at,client_updated_at) VALUES ('account-inactive','Older account','UPI',1,'2026-10-01T00:00:00Z','2026-10-01T00:00:00Z');`);
  expect((await send([
    { entity: 'account', operation: 'UPSERT', data: { id: 'account-inactive', name: 'Older account', paymentMethod: 'UPI', active: false, updatedAt: at } },
    { entity: 'category', operation: 'UPSERT', data: { id: 'cat-inactive', name: 'Older category', active: false, updatedAt: at } },
    { entity: 'transaction', operation: 'UPSERT', data: { id: 'expense-after-archive', amountPaise: 2200, categoryId: 'cat-inactive', accountId: 'account-inactive', paidByUserId: 'user-shrut', paymentMethod: 'UPI', occurredAt: at, updatedAt: at } },
  ])).status).toBe(200);
  expect(sql.prepare('SELECT category_id,account_id FROM transactions WHERE id=?').get('expense-after-archive'))
    .toMatchObject({ category_id: 'cat-inactive', account_id: 'account-inactive' });
  expect(sql.prepare('SELECT active FROM categories WHERE id=?').get('cat-inactive')).toMatchObject({ active: 0 });
  expect(sql.prepare('SELECT active FROM accounts WHERE id=?').get('account-inactive')).toMatchObject({ active: 0 });
  const recentCursor = new Date(Date.now() - 30_000).toISOString();
  const lateUpload = await app.request('/sync', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({
    lastSyncAt: recentCursor,
    changes: [{ entity: 'transaction', operation: 'UPSERT', data: {
      id: 'old-offline-expense', amountPaise: 2300, categoryId: 'cat-shared', accountId: 'account-shared',
      paidByUserId: 'user-shrut', paymentMethod: 'UPI', occurredAt: '2026-09-01T12:00:00Z',
      updatedAt: '2026-09-01T12:00:00Z',
    } }],
  }) }, { DB: adapter } as unknown as Bindings);
  expect(lateUpload.status).toBe(200);
  const lateBody = await lateUpload.json() as { serverChanges: Array<{ data: { id: string } }> };
  expect(lateBody.serverChanges.some(change => change.data.id === 'old-offline-expense')).toBe(true);
  const otherPhonePull = await app.request('/sync', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({
    lastSyncAt: recentCursor, changes: [],
  }) }, { DB: adapter } as unknown as Bindings);
  expect(otherPhonePull.status).toBe(200);
  const otherPhoneBody = await otherPhonePull.json() as { serverChanges: Array<{ data: { id: string } }> };
  expect(otherPhoneBody.serverChanges.some(change => change.data.id === 'old-offline-expense')).toBe(true);
  sql.close();
});
