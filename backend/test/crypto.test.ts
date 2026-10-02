import { describe, expect, it } from 'vitest';
import { createAccessToken, hashPin, sha256, verifyAccessToken, verifyPin } from '../src/utils/crypto';

describe('authentication crypto', () => {
  it('hashes and verifies PINs without storing plaintext', async () => {
    const hash = await hashPin('123456', new TextEncoder().encode('test-salt-16byte'), 10);
    expect(hash).not.toContain('123456');
    expect(await verifyPin('123456', hash)).toBe(true);
    expect(await verifyPin('654321', hash)).toBe(false);
  });

  it('uses a work factor supported by deployed Cloudflare Workers', async () => {
    const hash = await hashPin('987654');
    expect(hash.split(':')[1]).toBe('100000');
    expect(await verifyPin('987654', hash)).toBe(true);
  });

  it('signs and verifies access tokens', async () => {
    const token = await createAccessToken({ sub: 'user-1', deviceId: 'device-1' }, 'test-secret', 60);
    const payload = await verifyAccessToken(token, 'test-secret');
    expect(payload?.sub).toBe('user-1');
    expect(await verifyAccessToken(token, 'wrong-secret')).toBeNull();
  });

  it('generates stable hashes for refresh token lookup', async () => {
    expect(await sha256('token')).toBe(await sha256('token'));
    expect(await sha256('token')).not.toBe(await sha256('other'));
  });
});

