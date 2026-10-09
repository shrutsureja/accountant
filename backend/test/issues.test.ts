import { describe, expect, it } from 'vitest';
import { createIssueSchema } from '../src/routes/issues';
import { issueRoutes } from '../src/routes/issues';
import { Hono } from 'hono';
import type { Bindings, Variables } from '../src/types';
// @ts-expect-error Runtime Node 22 supports SQLite; installed types predate the experimental module.
import { DatabaseSync } from 'node:sqlite';

describe('issue report validation', () => {
  const valid = { category: 'BUG', title: 'Review screen hides an item', description: 'A confirmed bank alert is missing from review.' };
  it('accepts all issue categories and empty optional attachments', () => {
    for (const category of ['BUG', 'FEATURE', 'OTHER']) {
      const result = createIssueSchema.parse({ ...valid, category });
      expect(result.screenshots).toEqual([]);
    }
  });
  it('rejects oversized text, unknown categories, and too many screenshots', () => {
    expect(createIssueSchema.safeParse({ ...valid, title: 'x'.repeat(121) }).success).toBe(false);
    expect(createIssueSchema.safeParse({ ...valid, category: 'SECURITY' }).success).toBe(false);
    expect(createIssueSchema.safeParse({ ...valid, screenshots: Array(4).fill({ mimeType: 'image/png', base64: 'AAAA' }) }).success).toBe(false);
  });
});

it('stores reports and returns only metadata until an authenticated screenshot request', async () => {
  const sql = new DatabaseSync(':memory:');
  sql.exec(`CREATE TABLE users(id TEXT PRIMARY KEY,display_name TEXT);
    INSERT INTO users VALUES ('reporter','Shrut');
    CREATE TABLE issue_reports(id TEXT PRIMARY KEY,reporter_user_id TEXT,category TEXT,title TEXT,description TEXT,status TEXT,created_at TEXT,updated_at TEXT);
    CREATE TABLE issue_screenshots(id TEXT PRIMARY KEY,issue_id TEXT,mime_type TEXT,data_base64 TEXT,created_at TEXT);`);
  const db = {
    async batch(statements: Array<{ run: () => Promise<unknown> }>) { for (const statement of statements) await statement.run(); return []; },
    prepare(query: string) {
      let args: Array<string | number | null> = [];
      const bound = {
        bind(...values: Array<string | number | null>) { args = values; return bound; },
        async first() { return sql.prepare(query).get(...args) ?? null; },
        async all() { return { results: sql.prepare(query).all(...args) }; },
        async run() { sql.prepare(query).run(...args); return { success: true }; },
      };
      return bound;
    },
  };
  const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();
  app.use('*', async (c, next) => { c.set('auth', { userId: 'reporter', deviceId: 'phone' }); await next(); });
  app.route('/issues', issueRoutes);
  const image = 'iVBORw0KGgo='; // PNG signature; enough for the route's format check.
  const created = await app.request('/issues', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({
    category: 'BUG', title: 'Missing review item', description: 'A bank alert was not shown for review.',
    screenshots: [{ mimeType: 'image/png', base64: image }],
  }) }, { DB: db } as unknown as Bindings);
  expect(created.status).toBe(201);
  const id = ((await created.json()) as { id: string }).id;
  const list = await app.request('/issues', {}, { DB: db } as unknown as Bindings);
  expect(list.status).toBe(200);
  const listText = await list.text();
  expect(listText).toContain('Missing review item');
  expect(listText).not.toContain(image);
  const detail = await app.request(`/issues/${id}`, {}, { DB: db } as unknown as Bindings);
  const body = await detail.json() as { screenshots: Array<{ id: string }> };
  expect(body.screenshots).toHaveLength(1);
  const screenshot = await app.request(`/issues/${id}/screenshots/${body.screenshots[0]!.id}`, {}, { DB: db } as unknown as Bindings);
  expect(screenshot.status).toBe(200);
  expect(screenshot.headers.get('Content-Type')).toBe('image/png');
  expect(new Uint8Array(await screenshot.arrayBuffer())).toHaveLength(8);
  sql.close();
});
