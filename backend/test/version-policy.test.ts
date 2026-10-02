import { describe, expect, it } from 'vitest';
import { Hono } from 'hono';
import { appVersionPolicy, requireSupportedVersion } from '../src/version-policy';
import type { Bindings, Variables } from '../src/types';

describe('app version policy', () => {
  it('rejects contradictory and malformed server policy', () => {
    expect(() => appVersionPolicy({ APP_MIN_VERSION_CODE: '3', APP_LATEST_VERSION_CODE: '2' })).toThrow();
    expect(() => appVersionPolicy({ APP_MIN_VERSION_CODE: 'oops' })).toThrow();
  });
  it('blocks old clients but allows supported clients', async () => {
    const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();
    app.use('*', requireSupportedVersion);
    app.get('/', c => c.text('ok'));
    const env = { APP_MIN_VERSION_CODE: '2', APP_LATEST_VERSION_CODE: '3' } as Bindings;
    expect((await app.request('/', {}, env)).status).toBe(426);
    expect((await app.request('/', { headers: { 'X-App-Version-Code': '1' } }, env)).status).toBe(426);
    expect((await app.request('/', { headers: { 'X-App-Version-Code': '2' } }, env)).status).toBe(200);
    expect((await app.request('/', { headers: { 'X-App-Version-Code': 'x' } }, env)).status).toBe(400);
  });
});

import app from '../src/index';
it('version endpoint is available without authentication even for obsolete clients', async () => {
  const response = await app.request('/api/v1/app/version', {}, { APP_MIN_VERSION_CODE: '2', APP_LATEST_VERSION_CODE: '3' } as Bindings);
  expect(response.status).toBe(200);
  expect((await response.json() as { minimumSupportedVersionCode: number }).minimumSupportedVersionCode).toBe(2);
});
