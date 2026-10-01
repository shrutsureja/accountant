import { Hono } from 'hono';
import type { Bindings, Variables } from '../types';
import { accountSchema, categorySchema, merchantRuleSchema } from '../validators';
import { isResponse, nowIso, parseJson } from '../utils/http';
import { mapAccount, mapCategory, mapMerchantRule } from '../utils/mappers';

export const catalogRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

catalogRoutes.get('/members', async (c) => {
  const rows = await c.env.DB.prepare('SELECT id,username,display_name,avatar_initials,created_at FROM users ORDER BY display_name').all();
  return c.json({ members: rows.results.map((r) => ({ id: r.id, username: r.username, displayName: r.display_name, avatarInitials: r.avatar_initials, createdAt: r.created_at })) });
});

catalogRoutes.get('/categories', async (c) => {
  const rows = await c.env.DB.prepare('SELECT * FROM categories WHERE deleted_at IS NULL ORDER BY active DESC,name').all();
  return c.json({ categories: rows.results.map((r) => mapCategory(r as Record<string, unknown>)) });
});

catalogRoutes.post('/categories', async (c) => {
  const input = await parseJson(c, categorySchema); if (isResponse(input)) return input;
  const timestamp = input.updatedAt ?? nowIso();
  await c.env.DB.prepare('INSERT INTO categories (id,name,icon,active,created_at,updated_at,updated_by) VALUES (?,?,?,?,?,?,?)').bind(input.id,input.name,input.icon,input.active ? 1 : 0,timestamp,timestamp,c.get('auth').userId).run();
  const row = await c.env.DB.prepare('SELECT * FROM categories WHERE id=?').bind(input.id).first<Record<string, unknown>>();
  return c.json({ category: mapCategory(row!) }, 201);
});

catalogRoutes.patch('/categories/:id', async (c) => {
  const input = await parseJson(c, categorySchema.omit({ id: true }).partial()); if (isResponse(input)) return input;
  const old = await c.env.DB.prepare('SELECT * FROM categories WHERE id=? AND deleted_at IS NULL').bind(c.req.param('id')).first<Record<string, unknown>>();
  if (!old) return c.json({ error: 'Category not found' }, 404);
  await c.env.DB.prepare('UPDATE categories SET name=?,icon=?,active=?,updated_at=?,updated_by=?,version=version+1 WHERE id=?').bind(input.name ?? old.name,input.icon ?? old.icon,(input.active ?? Boolean(old.active)) ? 1 : 0,nowIso(),c.get('auth').userId,c.req.param('id')).run();
  const row = await c.env.DB.prepare('SELECT * FROM categories WHERE id=?').bind(c.req.param('id')).first<Record<string, unknown>>();
  return c.json({ category: mapCategory(row!) });
});

catalogRoutes.get('/accounts', async (c) => {
  const rows = await c.env.DB.prepare('SELECT * FROM accounts WHERE deleted_at IS NULL ORDER BY active DESC,name').all();
  return c.json({ accounts: rows.results.map((r) => mapAccount(r as Record<string, unknown>)) });
});

catalogRoutes.post('/accounts', async (c) => {
  const input = await parseJson(c, accountSchema); if (isResponse(input)) return input;
  const timestamp = input.updatedAt ?? nowIso();
  await c.env.DB.prepare('INSERT INTO accounts (id,name,bank_name,last4,owner_user_id,payment_method,active,created_at,updated_at,updated_by) VALUES (?,?,?,?,?,?,?,?,?,?)').bind(input.id,input.name,input.bankName ?? null,input.last4 ?? null,input.ownerUserId ?? null,input.paymentMethod,input.active ? 1 : 0,timestamp,timestamp,c.get('auth').userId).run();
  const row = await c.env.DB.prepare('SELECT * FROM accounts WHERE id=?').bind(input.id).first<Record<string, unknown>>();
  return c.json({ account: mapAccount(row!) }, 201);
});

catalogRoutes.patch('/accounts/:id', async (c) => {
  const input = await parseJson(c, accountSchema.omit({ id: true }).partial()); if (isResponse(input)) return input;
  const old = await c.env.DB.prepare('SELECT * FROM accounts WHERE id=? AND deleted_at IS NULL').bind(c.req.param('id')).first<Record<string, unknown>>();
  if (!old) return c.json({ error: 'Account not found' }, 404);
  await c.env.DB.prepare('UPDATE accounts SET name=?,bank_name=?,last4=?,owner_user_id=?,payment_method=?,active=?,updated_at=?,updated_by=?,version=version+1 WHERE id=?').bind(input.name ?? old.name,input.bankName ?? old.bank_name,input.last4 ?? old.last4,input.ownerUserId ?? old.owner_user_id,input.paymentMethod ?? old.payment_method,(input.active ?? Boolean(old.active)) ? 1 : 0,nowIso(),c.get('auth').userId,c.req.param('id')).run();
  const row = await c.env.DB.prepare('SELECT * FROM accounts WHERE id=?').bind(c.req.param('id')).first<Record<string, unknown>>();
  return c.json({ account: mapAccount(row!) });
});

catalogRoutes.get('/merchant-rules', async (c) => {
  const rows = await c.env.DB.prepare('SELECT * FROM merchant_rules WHERE deleted_at IS NULL ORDER BY merchant_pattern').all();
  return c.json({ merchantRules: rows.results.map((r) => mapMerchantRule(r as Record<string, unknown>)) });
});

catalogRoutes.post('/merchant-rules', async (c) => {
  const input = await parseJson(c, merchantRuleSchema); if (isResponse(input)) return input;
  const timestamp = input.updatedAt ?? nowIso();
  await c.env.DB.prepare(`INSERT INTO merchant_rules (id,merchant_pattern,category_id,created_by_user_id,created_at,updated_at,updated_by) VALUES (?,?,?,?,?,?,?)
    ON CONFLICT(merchant_pattern) DO UPDATE SET category_id=excluded.category_id,updated_at=excluded.updated_at,updated_by=excluded.updated_by,version=merchant_rules.version+1`).bind(input.id,input.merchantPattern.toUpperCase(),input.categoryId,c.get('auth').userId,timestamp,timestamp,c.get('auth').userId).run();
  const row = await c.env.DB.prepare('SELECT * FROM merchant_rules WHERE merchant_pattern=? COLLATE NOCASE').bind(input.merchantPattern).first<Record<string, unknown>>();
  return c.json({ merchantRule: mapMerchantRule(row!) }, 201);
});

catalogRoutes.patch('/merchant-rules/:id', async (c) => {
  const input = await parseJson(c, merchantRuleSchema.omit({ id: true }).partial()); if (isResponse(input)) return input;
  const old = await c.env.DB.prepare('SELECT * FROM merchant_rules WHERE id=? AND deleted_at IS NULL').bind(c.req.param('id')).first<Record<string, unknown>>();
  if (!old) return c.json({ error: 'Merchant rule not found' }, 404);
  await c.env.DB.prepare('UPDATE merchant_rules SET merchant_pattern=?,category_id=?,updated_at=?,updated_by=?,version=version+1 WHERE id=?').bind((input.merchantPattern ?? old.merchant_pattern as string).toUpperCase(),input.categoryId ?? old.category_id,nowIso(),c.get('auth').userId,c.req.param('id')).run();
  const row = await c.env.DB.prepare('SELECT * FROM merchant_rules WHERE id=?').bind(c.req.param('id')).first<Record<string, unknown>>();
  return c.json({ merchantRule: mapMerchantRule(row!) });
});

catalogRoutes.delete('/merchant-rules/:id', async (c) => {
  const timestamp = nowIso();
  const result = await c.env.DB.prepare('UPDATE merchant_rules SET deleted_at=?,updated_at=?,updated_by=?,version=version+1 WHERE id=? AND deleted_at IS NULL').bind(timestamp,timestamp,c.get('auth').userId,c.req.param('id')).run();
  return result.meta.changes ? c.body(null, 204) : c.json({ error: 'Merchant rule not found' }, 404);
});

