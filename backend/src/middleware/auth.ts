import type { MiddlewareHandler } from 'hono';
import type { Bindings, Variables } from '../types';
import { verifyAccessToken } from '../utils/crypto';

export const requireAuth: MiddlewareHandler<{ Bindings: Bindings; Variables: Variables }> = async (c, next) => {
  const header = c.req.header('Authorization');
  if (!header?.startsWith('Bearer ')) return c.json({ error: 'Authentication required' }, 401);
  const payload = await verifyAccessToken(header.slice(7), c.env.JWT_SECRET);
  if (!payload || typeof payload.sub !== 'string' || typeof payload.deviceId !== 'string') {
    return c.json({ error: 'Session expired' }, 401);
  }
  c.set('auth', { userId: payload.sub, deviceId: payload.deviceId });
  await next();
};

