import type { Context } from 'hono';
import type { Bindings, Variables } from '../types';
import { sha256 } from './crypto';

const WINDOW_MS = 15 * 60 * 1000;
const WRONG_PIN_LIMIT = 10;
const IP_REQUEST_LIMIT = 30;
const LOCK_MESSAGE = 'Account locked after 10 incorrect PIN attempts. Contact Shrut to change your PIN.';
type AuthContext = Context<{ Bindings: Bindings; Variables: Variables }>;

export async function checkIpLoginRateLimit(c: AuthContext): Promise<Response | null> {
  const now = Date.now();
  const cutoff = now - WINDOW_MS;
  await c.env.DB.prepare('DELETE FROM auth_ip_attempts WHERE window_started_at <= ?').bind(cutoff).run();
  const ip = c.req.header('CF-Connecting-IP') ?? 'local';
  const key = await sha256(ip);
  const row = await c.env.DB.prepare(`INSERT INTO auth_ip_attempts (key, attempts, window_started_at)
    VALUES (?, 1, ?) ON CONFLICT(key) DO UPDATE SET
    attempts = CASE WHEN window_started_at <= ? THEN 1 ELSE attempts + 1 END,
    window_started_at = CASE WHEN window_started_at <= ? THEN excluded.window_started_at ELSE window_started_at END
    RETURNING attempts, window_started_at`)
    .bind(key, now, cutoff, cutoff).first<{ attempts: number; window_started_at: number }>();
  if (row && row.attempts > IP_REQUEST_LIMIT) {
    const retryAfter = Math.max(1, Math.ceil((row.window_started_at + WINDOW_MS - now) / 1000));
    c.header('Retry-After', String(retryAfter));
    return c.json({ error: 'Too many login requests. Please try again later.' }, 429);
  }
  return null;
}

export async function isAccountLocked(c: AuthContext, userId: string): Promise<boolean> {
  const row = await c.env.DB.prepare('SELECT locked_at FROM auth_login_failures WHERE user_id=?')
    .bind(userId).first<{ locked_at: string | null }>();
  return Boolean(row?.locked_at);
}

export async function recordWrongPin(c: AuthContext, userId: string): Promise<boolean> {
  const now = Date.now();
  const cutoff = now - WINDOW_MS;
  const row = await c.env.DB.prepare(`INSERT INTO auth_login_failures (user_id, attempts, window_started_at, locked_at)
    VALUES (?, 1, ?, NULL) ON CONFLICT(user_id) DO UPDATE SET
    attempts = CASE WHEN locked_at IS NOT NULL THEN attempts WHEN window_started_at <= ? THEN 1 ELSE attempts + 1 END,
    window_started_at = CASE WHEN locked_at IS NULL AND window_started_at <= ? THEN excluded.window_started_at ELSE window_started_at END,
    locked_at = CASE WHEN locked_at IS NOT NULL THEN locked_at
      WHEN window_started_at > ? AND attempts + 1 >= ? THEN ? ELSE NULL END
    RETURNING locked_at`)
    .bind(userId, now, cutoff, cutoff, cutoff, WRONG_PIN_LIMIT, new Date(now).toISOString())
    .first<{ locked_at: string | null }>();
  return Boolean(row?.locked_at);
}

export async function clearWrongPins(c: AuthContext, userId: string): Promise<void> {
  await c.env.DB.prepare('DELETE FROM auth_login_failures WHERE user_id=? AND locked_at IS NULL')
    .bind(userId).run();
}

export function accountLockedResponse(c: AuthContext): Response {
  return c.json({ error: LOCK_MESSAGE }, 423);
}
