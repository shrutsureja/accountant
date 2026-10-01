import { Hono } from 'hono';
import type { Bindings, Variables } from '../types';
import { isResponse, nowIso, parseJson } from '../utils/http';
import { mapTransaction } from '../utils/mappers';
import { transactionPatchSchema, transactionSchema } from '../validators';

export const transactionRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

transactionRoutes.get('/', async (c) => {
  const status = c.req.query('status') ?? 'CONFIRMED';
  const search = c.req.query('search')?.trim();
  const member = c.req.query('member');
  const from = c.req.query('from');
  const to = c.req.query('to');
  const clauses = ['t.deleted_at IS NULL', 't.status = ?'];
  const values: unknown[] = [status];
  if (search) { clauses.push('(t.merchant LIKE ? OR t.note LIKE ? OR c.name LIKE ? OR u.display_name LIKE ?)'); values.push(...Array(4).fill(`%${search}%`)); }
  if (member) { clauses.push('t.paid_by_user_id = ?'); values.push(member); }
  if (from) { clauses.push('t.occurred_at >= ?'); values.push(from); }
  if (to) { clauses.push('t.occurred_at < ?'); values.push(to); }
  const result = await c.env.DB.prepare(`SELECT t.* FROM transactions t LEFT JOIN categories c ON c.id=t.category_id LEFT JOIN users u ON u.id=t.paid_by_user_id WHERE ${clauses.join(' AND ')} ORDER BY t.occurred_at DESC LIMIT 500`).bind(...values).all();
  return c.json({ transactions: result.results.map((row) => mapTransaction(row as Record<string, unknown>)) });
});

transactionRoutes.get('/review', async (c) => {
  const result = await c.env.DB.prepare("SELECT * FROM transactions WHERE status='DETECTED' AND deleted_at IS NULL ORDER BY occurred_at DESC").all();
  return c.json({ transactions: result.results.map((row) => mapTransaction(row as Record<string, unknown>)) });
});

transactionRoutes.post('/', async (c) => {
  const input = await parseJson(c, transactionSchema);
  if (isResponse(input)) return input;
  const auth = c.get('auth');
  const timestamp = nowIso();
  if (input.fingerprint) {
    const duplicate = await c.env.DB.prepare("SELECT id FROM transactions WHERE fingerprint=? AND status!='DELETED' AND deleted_at IS NULL").bind(input.fingerprint).first();
    if (duplicate) return c.json({ error: 'Possible duplicate', existingId: duplicate.id }, 409);
  }
  await c.env.DB.prepare(`INSERT INTO transactions (id,amount_paise,currency,category_id,paid_by_user_id,payment_method,account_id,merchant,note,occurred_at,source,status,source_reference,fingerprint,created_by_user_id,updated_by_user_id,created_at,updated_at,version)
    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)`)
    .bind(input.id,input.amountPaise,input.currency,input.categoryId ?? null,input.paidByUserId,input.paymentMethod,input.accountId ?? null,input.merchant ?? null,input.note ?? null,input.occurredAt,input.source,input.status,input.sourceReference ?? null,input.fingerprint ?? null,auth.userId,auth.userId,timestamp,input.updatedAt ?? timestamp).run();
  const row = await c.env.DB.prepare('SELECT * FROM transactions WHERE id=?').bind(input.id).first<Record<string, unknown>>();
  return c.json({ transaction: mapTransaction(row!) }, 201);
});

transactionRoutes.get('/:id', async (c) => {
  const row = await c.env.DB.prepare('SELECT * FROM transactions WHERE id=? AND deleted_at IS NULL').bind(c.req.param('id')).first<Record<string, unknown>>();
  return row ? c.json({ transaction: mapTransaction(row) }) : c.json({ error: 'Transaction not found' }, 404);
});

transactionRoutes.patch('/:id', async (c) => {
  const input = await parseJson(c, transactionPatchSchema);
  if (isResponse(input)) return input;
  const existing = await c.env.DB.prepare('SELECT * FROM transactions WHERE id=? AND deleted_at IS NULL').bind(c.req.param('id')).first<Record<string, unknown>>();
  if (!existing) return c.json({ error: 'Transaction not found' }, 404);
  const merged = { amountPaise: existing.amount_paise, currency: existing.currency, categoryId: existing.category_id, paidByUserId: existing.paid_by_user_id, paymentMethod: existing.payment_method, accountId: existing.account_id, merchant: existing.merchant, note: existing.note, occurredAt: existing.occurred_at, source: existing.source, status: existing.status, sourceReference: existing.source_reference, fingerprint: existing.fingerprint, ...input };
  const timestamp = nowIso();
  await c.env.DB.prepare(`UPDATE transactions SET amount_paise=?,currency=?,category_id=?,paid_by_user_id=?,payment_method=?,account_id=?,merchant=?,note=?,occurred_at=?,source=?,status=?,source_reference=?,fingerprint=?,updated_by_user_id=?,updated_at=?,version=version+1 WHERE id=?`)
    .bind(merged.amountPaise,merged.currency,merged.categoryId,merged.paidByUserId,merged.paymentMethod,merged.accountId,merged.merchant,merged.note,merged.occurredAt,merged.source,merged.status,merged.sourceReference,merged.fingerprint,c.get('auth').userId,timestamp,c.req.param('id')).run();
  const row = await c.env.DB.prepare('SELECT * FROM transactions WHERE id=?').bind(c.req.param('id')).first<Record<string, unknown>>();
  return c.json({ transaction: mapTransaction(row!) });
});

transactionRoutes.delete('/:id', async (c) => {
  const timestamp = nowIso();
  const result = await c.env.DB.prepare("UPDATE transactions SET status='DELETED',deleted_at=?,updated_at=?,updated_by_user_id=?,version=version+1 WHERE id=? AND deleted_at IS NULL").bind(timestamp,timestamp,c.get('auth').userId,c.req.param('id')).run();
  return result.meta.changes ? c.body(null, 204) : c.json({ error: 'Transaction not found' }, 404);
});

for (const action of ['confirm', 'ignore'] as const) {
  transactionRoutes.post(`/:id/${action}`, async (c) => {
    const status = action === 'confirm' ? 'CONFIRMED' : 'IGNORED';
    const timestamp = nowIso();
    const result = await c.env.DB.prepare("UPDATE transactions SET status=?,updated_at=?,updated_by_user_id=?,version=version+1 WHERE id=? AND status='DETECTED' AND deleted_at IS NULL").bind(status,timestamp,c.get('auth').userId,c.req.param('id')).run();
    if (!result.meta.changes) return c.json({ error: 'Review item not found' }, 404);
    const row = await c.env.DB.prepare('SELECT * FROM transactions WHERE id=?').bind(c.req.param('id')).first<Record<string, unknown>>();
    return c.json({ transaction: mapTransaction(row!) });
  });
}

