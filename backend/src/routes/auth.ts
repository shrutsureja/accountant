import { Hono } from 'hono';
import { z } from 'zod';
import type { Bindings, Variables } from '../types';
import { createAccessToken, randomToken, sha256, verifyPin } from '../utils/crypto';
import { isResponse, nowIso, parseJson } from '../utils/http';
import { loginSchema } from '../validators';
import { requireAuth } from '../middleware/auth';

export const authRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

function tokenTtl(c: { env: Bindings }): number {
  return Number(c.env.ACCESS_TOKEN_TTL_SECONDS ?? 900);
}

authRoutes.post('/login', async (c) => {
  const input = await parseJson(c, loginSchema);
  if (isResponse(input)) return input;
  const user = await c.env.DB.prepare('SELECT * FROM users WHERE username = ? COLLATE NOCASE').bind(input.username).first<Record<string, string>>();
  if (!user?.pin_hash || !(await verifyPin(input.pin, user.pin_hash))) return c.json({ error: 'Username or PIN is incorrect' }, 401);

  const refreshToken = randomToken();
  const timestamp = nowIso();
  await c.env.DB.prepare(`INSERT INTO devices (id,user_id,device_name,refresh_token_hash,last_seen_at,created_at,revoked_at)
    VALUES (?,?,?,?,?,?,NULL) ON CONFLICT(id) DO UPDATE SET user_id=excluded.user_id,device_name=excluded.device_name,refresh_token_hash=excluded.refresh_token_hash,last_seen_at=excluded.last_seen_at,revoked_at=NULL`)
    .bind(input.deviceId, user.id, input.deviceName, await sha256(refreshToken), timestamp, timestamp).run();
  const accessToken = await createAccessToken({ sub: user.id, deviceId: input.deviceId }, c.env.JWT_SECRET, tokenTtl(c));
  return c.json({ accessToken, refreshToken, expiresIn: tokenTtl(c), user: { id: user.id, username: user.username, displayName: user.display_name, avatarInitials: user.avatar_initials } });
});

authRoutes.post('/refresh', async (c) => {
  const input = await parseJson(c, z.object({ deviceId: z.string(), refreshToken: z.string().min(20) }));
  if (isResponse(input)) return input;
  const device = await c.env.DB.prepare('SELECT id,user_id,refresh_token_hash FROM devices WHERE id=? AND revoked_at IS NULL').bind(input.deviceId).first<Record<string, string>>();
  if (!device || device.refresh_token_hash !== await sha256(input.refreshToken)) return c.json({ error: 'Session expired' }, 401);
  const replacement = randomToken();
  await c.env.DB.prepare('UPDATE devices SET refresh_token_hash=?,last_seen_at=? WHERE id=?').bind(await sha256(replacement), nowIso(), input.deviceId).run();
  return c.json({ accessToken: await createAccessToken({ sub: device.user_id, deviceId: device.id }, c.env.JWT_SECRET, tokenTtl(c)), refreshToken: replacement, expiresIn: tokenTtl(c) });
});

authRoutes.post('/logout', requireAuth, async (c) => {
  await c.env.DB.prepare('UPDATE devices SET revoked_at=?,refresh_token_hash=NULL WHERE id=?').bind(nowIso(), c.get('auth').deviceId).run();
  return c.body(null, 204);
});
