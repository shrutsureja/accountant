const encoder = new TextEncoder();

function bytesToBase64(bytes: Uint8Array): string {
  let binary = '';
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary);
}

function base64ToBytes(value: string): Uint8Array<ArrayBuffer> {
  const decoded = atob(value);
  const result = new Uint8Array(decoded.length);
  for (let i = 0; i < decoded.length; i++) result[i] = decoded.charCodeAt(i);
  return result;
}

export async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', encoder.encode(value));
  return bytesToBase64(new Uint8Array(digest));
}

export async function hashPin(pin: string, salt: Uint8Array<ArrayBuffer> = crypto.getRandomValues(new Uint8Array(16)), iterations = 100_000): Promise<string> {
  const key = await crypto.subtle.importKey('raw', encoder.encode(pin), 'PBKDF2', false, ['deriveBits']);
  const bits = await crypto.subtle.deriveBits({ name: 'PBKDF2', hash: 'SHA-256', salt, iterations }, key, 256);
  return `pbkdf2:${iterations}:${bytesToBase64(salt)}:${bytesToBase64(new Uint8Array(bits))}`;
}

export async function verifyPin(pin: string, encoded: string): Promise<boolean> {
  const [algorithm, rawIterations, rawSalt, expected] = encoded.split(':');
  if (algorithm !== 'pbkdf2' || !rawIterations || !rawSalt || !expected) return false;
  const actual = await hashPin(pin, base64ToBytes(rawSalt), Number(rawIterations));
  return constantTimeEqual(actual, encoded);
}

function constantTimeEqual(left: string, right: string): boolean {
  if (left.length !== right.length) return false;
  let result = 0;
  for (let i = 0; i < left.length; i++) result |= left.charCodeAt(i) ^ right.charCodeAt(i);
  return result === 0;
}

function base64Url(bytes: Uint8Array): string {
  return bytesToBase64(bytes).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
}

export async function createAccessToken(payload: object, secret: string, ttlSeconds: number): Promise<string> {
  const header = base64Url(encoder.encode(JSON.stringify({ alg: 'HS256', typ: 'JWT' })));
  const body = base64Url(encoder.encode(JSON.stringify({ ...payload, exp: Math.floor(Date.now() / 1000) + ttlSeconds })));
  const unsigned = `${header}.${body}`;
  const key = await crypto.subtle.importKey('raw', encoder.encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  const signature = await crypto.subtle.sign('HMAC', key, encoder.encode(unsigned));
  return `${unsigned}.${base64Url(new Uint8Array(signature))}`;
}

export async function verifyAccessToken(token: string, secret: string): Promise<Record<string, unknown> | null> {
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  const [header, body, signature] = parts as [string, string, string];
  const key = await crypto.subtle.importKey('raw', encoder.encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['verify']);
  const normalized = signature.replace(/-/g, '+').replace(/_/g, '/');
  const signatureBytes = base64ToBytes(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '='));
  const valid = await crypto.subtle.verify('HMAC', key, signatureBytes, encoder.encode(`${header}.${body}`));
  if (!valid) return null;
  const normalizedBody = body.replace(/-/g, '+').replace(/_/g, '/');
  const payload = JSON.parse(new TextDecoder().decode(base64ToBytes(normalizedBody.padEnd(Math.ceil(normalizedBody.length / 4) * 4, '=')))) as Record<string, unknown>;
  return typeof payload.exp === 'number' && payload.exp > Date.now() / 1000 ? payload : null;
}

export function randomToken(): string {
  return base64Url(crypto.getRandomValues(new Uint8Array(32)));
}
