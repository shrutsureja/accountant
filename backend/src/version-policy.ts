import type { MiddlewareHandler } from 'hono';
import type { Bindings, Variables } from './types';

function versionCode(raw: string | undefined, fallback: number): number {
  if (raw === undefined) return fallback;
  if (!/^[1-9]\d*$/.test(raw)) throw new Error('Invalid app version configuration');
  const value = Number(raw);
  if (!Number.isSafeInteger(value) || value > 2100000000) throw new Error('Invalid app version configuration');
  return value;
}

export function appVersionPolicy(env: Partial<Bindings>) {
  const minimumSupportedVersionCode = versionCode(env.APP_MIN_VERSION_CODE, 1);
  const latestVersionCode = versionCode(env.APP_LATEST_VERSION_CODE, 2);
  if (minimumSupportedVersionCode > latestVersionCode) throw new Error('Minimum app version exceeds latest version');
  return {
    minimumSupportedVersionCode,
    latestVersionCode,
    latestVersionName: env.APP_LATEST_VERSION_NAME ?? '1.1.0',
    message: 'Contact Shrut for the updated Accountant APK.',
  };
}

export const requireSupportedVersion: MiddlewareHandler<{ Bindings: Bindings; Variables: Variables }> = async (c, next) => {
  const policy = appVersionPolicy(c.env);
  // Existing builds did not send a version header and are version code 1.
  const header = c.req.header('X-App-Version-Code') ?? '1';
  if (!/^[1-9]\d*$/.test(header) || !Number.isSafeInteger(Number(header))) return c.json({ error: 'Invalid app version' }, 400);
  if (Number(header) < policy.minimumSupportedVersionCode) return c.json({ error: 'App update required', ...policy }, 426);
  await next();
};
