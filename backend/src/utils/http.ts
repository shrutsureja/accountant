import type { Context } from 'hono';
import type { Bindings, Variables } from '../types';
import type { ZodType } from 'zod';

export async function parseJson<T>(c: Context<{ Bindings: Bindings; Variables: Variables }>, schema: ZodType<T>): Promise<T | Response> {
  try {
    const body = await c.req.json();
    const result = schema.safeParse(body);
    if (!result.success) return c.json({ error: 'Invalid request', details: result.error.issues }, 400);
    return result.data;
  } catch {
    return c.json({ error: 'Invalid JSON' }, 400);
  }
}

export const isResponse = (value: unknown): value is Response => value instanceof Response;

export function nowIso(): string {
  return new Date().toISOString();
}

