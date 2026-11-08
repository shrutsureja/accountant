// @ts-expect-error Runtime Node 22 supports SQLite; installed types predate the experimental module.
import { DatabaseSync } from 'node:sqlite';
import { describe, expect, it } from 'vitest';
import { Hono } from 'hono';
import { authRoutes } from '../src/routes/auth';
import { hashPin } from '../src/utils/crypto';
import type { Bindings, Variables } from '../src/types';

describe('login rate limiting', () => {
  it('locks after ten wrong PINs within fifteen minutes until the PIN is reset', async () => {
    const sql = new DatabaseSync(':memory:');
    sql.exec(`CREATE TABLE users(id TEXT PRIMARY KEY, username TEXT, display_name TEXT, avatar_initials TEXT, pin_hash TEXT);
      CREATE TABLE devices(id TEXT PRIMARY KEY, user_id TEXT, device_name TEXT, refresh_token_hash TEXT, last_seen_at TEXT, created_at TEXT, revoked_at TEXT);
      CREATE TABLE auth_ip_attempts(key TEXT PRIMARY KEY, attempts INTEGER NOT NULL, window_started_at INTEGER NOT NULL);
      CREATE TABLE auth_login_failures(user_id TEXT PRIMARY KEY, attempts INTEGER NOT NULL, window_started_at INTEGER NOT NULL, locked_at TEXT);`);
    sql.prepare('INSERT INTO users VALUES (?,?,?,?,?)').run('user-1', 'shrut', 'Shrut', 'S', await hashPin('123456', new Uint8Array(16), 10));
    const db = {
      prepare(query: string) {
        let args: Array<string | number | null> = [];
        const bound = {
          bind(...values: Array<string | number | null>) { args = values; return bound; },
          async first() { return sql.prepare(query).get(...args) ?? null; },
          async run() { const result = sql.prepare(query).run(...args); return { success: true, meta: { changes: result.changes } }; },
        };
        return bound;
      },
    };
    const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();
    app.route('/auth', authRoutes);
    const login = (pin: string) => app.request('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'CF-Connecting-IP': '192.0.2.1' },
      body: JSON.stringify({ username: 'shrut', pin, deviceId: 'test-device', deviceName: 'Test phone' }),
    }, { DB: db, JWT_SECRET: 'test-secret' } as unknown as Bindings);

    for (let attempt = 0; attempt < 9; attempt++) expect((await login('000000')).status).toBe(401);
    const locked = await login('000000');
    expect(locked.status).toBe(423);
    expect((await locked.json() as { error: string }).error).toContain('Contact Shrut');
    sql.prepare('UPDATE auth_login_failures SET window_started_at=?').run(Date.now() - 16 * 60 * 1000);
    expect((await login('123456')).status).toBe(423);

    // The private reset procedure replaces the PIN and clears this lock.
    sql.prepare('UPDATE users SET pin_hash=? WHERE id=?').run(await hashPin('654321', new Uint8Array(16), 10), 'user-1');
    sql.prepare('DELETE FROM auth_login_failures WHERE user_id=?').run('user-1');
    const accepted = await login('654321');
    expect(accepted.status).toBe(200);
    expect(sql.prepare("SELECT attempts FROM auth_login_failures WHERE user_id='user-1'").get()).toBeUndefined();
    sql.close();
  });

  it('forgets fewer than ten wrong PINs after fifteen minutes or a successful login', async () => {
    const sql = new DatabaseSync(':memory:');
    sql.exec(`CREATE TABLE users(id TEXT PRIMARY KEY, username TEXT, display_name TEXT, avatar_initials TEXT, pin_hash TEXT);
      CREATE TABLE devices(id TEXT PRIMARY KEY, user_id TEXT, device_name TEXT, refresh_token_hash TEXT, last_seen_at TEXT, created_at TEXT, revoked_at TEXT);
      CREATE TABLE auth_ip_attempts(key TEXT PRIMARY KEY, attempts INTEGER NOT NULL, window_started_at INTEGER NOT NULL);
      CREATE TABLE auth_login_failures(user_id TEXT PRIMARY KEY, attempts INTEGER NOT NULL, window_started_at INTEGER NOT NULL, locked_at TEXT);`);
    sql.prepare('INSERT INTO users VALUES (?,?,?,?,?)').run('user-1', 'shrut', 'Shrut', 'S', await hashPin('123456', new Uint8Array(16), 10));
    const db = { prepare(query: string) { let args: Array<string | number | null> = []; const bound = {
      bind(...values: Array<string | number | null>) { args = values; return bound; },
      async first() { return sql.prepare(query).get(...args) ?? null; },
      async run() { sql.prepare(query).run(...args); return { success: true }; },
    }; return bound; } };
    const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();
    app.route('/auth', authRoutes);
    const login = (pin: string) => app.request('/auth/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json', 'CF-Connecting-IP': '192.0.2.2' },
      body: JSON.stringify({ username: 'shrut', pin, deviceId: 'test-device', deviceName: 'Test phone' }),
    }, { DB: db, JWT_SECRET: 'test-secret' } as unknown as Bindings);
    expect((await login('000000')).status).toBe(401);
    sql.prepare('UPDATE auth_login_failures SET window_started_at=?').run(Date.now() - 16 * 60 * 1000);
    expect((await login('000000')).status).toBe(401);
    expect(sql.prepare("SELECT attempts FROM auth_login_failures WHERE user_id='user-1'").get()).toMatchObject({ attempts: 1 });
    expect((await login('123456')).status).toBe(200);
    expect(sql.prepare("SELECT attempts FROM auth_login_failures WHERE user_id='user-1'").get()).toBeUndefined();
    sql.close();
  });
});
