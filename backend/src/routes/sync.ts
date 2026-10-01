import { Hono } from 'hono';
import { z } from 'zod';
import type { Bindings, SyncChange, SyncEntity, Variables } from '../types';
import { isResponse, nowIso, parseJson } from '../utils/http';
import { accountSchema, categorySchema, merchantRuleSchema, transactionSchema } from '../validators';
import { mapAccount, mapCategory, mapMerchantRule, mapTransaction } from '../utils/mappers';

export const syncRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

const syncSchema = z.object({
  lastSyncAt: z.string().datetime({ offset: true }).nullable().optional(),
  changes: z.array(z.object({ entity: z.enum(['transaction','category','account','merchant_rule']), operation: z.enum(['UPSERT','DELETE']), data: z.record(z.string(), z.unknown()) })).max(1000),
});

async function applyChange(db: D1Database, change: SyncChange, userId: string, serverTime: string): Promise<void> {
  const id = change.data.id;
  if (typeof id !== 'string') throw new Error('Every sync change needs an id');
  const clientUpdatedAt = typeof change.data.updatedAt === 'string' ? change.data.updatedAt : serverTime;
  const table = ({ transaction: 'transactions', category: 'categories', account: 'accounts', merchant_rule: 'merchant_rules' } as const)[change.entity];
  const existing = await db.prepare(`SELECT updated_at FROM ${table} WHERE id=?`).bind(id).first<{ updated_at: string }>();
  if (existing && existing.updated_at > clientUpdatedAt) return;

  if (change.operation === 'DELETE') {
    if (change.entity === 'transaction') await db.prepare("UPDATE transactions SET status='DELETED',deleted_at=?,updated_at=?,updated_by_user_id=?,version=version+1 WHERE id=?").bind(clientUpdatedAt,clientUpdatedAt,userId,id).run();
    else await db.prepare(`UPDATE ${table} SET deleted_at=?,updated_at=?,updated_by=?,version=version+1 WHERE id=?`).bind(clientUpdatedAt,clientUpdatedAt,userId,id).run();
    return;
  }

  if (change.entity === 'transaction') {
    const v = transactionSchema.parse(change.data);
    await db.prepare(`INSERT INTO transactions (id,amount_paise,currency,category_id,paid_by_user_id,payment_method,account_id,merchant,note,occurred_at,source,status,source_reference,fingerprint,created_by_user_id,updated_by_user_id,created_at,updated_at,version)
      VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET amount_paise=excluded.amount_paise,currency=excluded.currency,category_id=excluded.category_id,paid_by_user_id=excluded.paid_by_user_id,payment_method=excluded.payment_method,account_id=excluded.account_id,merchant=excluded.merchant,note=excluded.note,occurred_at=excluded.occurred_at,source=excluded.source,status=excluded.status,source_reference=excluded.source_reference,fingerprint=excluded.fingerprint,updated_by_user_id=excluded.updated_by_user_id,updated_at=excluded.updated_at,version=excluded.version`)
      .bind(v.id,v.amountPaise,v.currency,v.categoryId ?? null,v.paidByUserId,v.paymentMethod,v.accountId ?? null,v.merchant ?? null,v.note ?? null,v.occurredAt,v.source,v.status,v.sourceReference ?? null,v.fingerprint ?? null,userId,userId,clientUpdatedAt,clientUpdatedAt,v.version ?? 1).run();
  } else if (change.entity === 'category') {
    const v = categorySchema.parse(change.data);
    await db.prepare('INSERT INTO categories (id,name,icon,active,created_at,updated_at,updated_by) VALUES (?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,icon=excluded.icon,active=excluded.active,updated_at=excluded.updated_at,updated_by=excluded.updated_by,version=categories.version+1').bind(v.id,v.name,v.icon,v.active?1:0,clientUpdatedAt,clientUpdatedAt,userId).run();
  } else if (change.entity === 'account') {
    const v = accountSchema.parse(change.data);
    await db.prepare('INSERT INTO accounts (id,name,bank_name,last4,owner_user_id,payment_method,active,created_at,updated_at,updated_by) VALUES (?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,bank_name=excluded.bank_name,last4=excluded.last4,owner_user_id=excluded.owner_user_id,payment_method=excluded.payment_method,active=excluded.active,updated_at=excluded.updated_at,updated_by=excluded.updated_by,version=accounts.version+1').bind(v.id,v.name,v.bankName ?? null,v.last4 ?? null,v.ownerUserId ?? null,v.paymentMethod,v.active?1:0,clientUpdatedAt,clientUpdatedAt,userId).run();
  } else {
    const v = merchantRuleSchema.parse(change.data);
    await db.prepare('INSERT INTO merchant_rules (id,merchant_pattern,category_id,created_by_user_id,created_at,updated_at,updated_by) VALUES (?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET merchant_pattern=excluded.merchant_pattern,category_id=excluded.category_id,updated_at=excluded.updated_at,updated_by=excluded.updated_by,version=merchant_rules.version+1').bind(v.id,v.merchantPattern.toUpperCase(),v.categoryId,userId,clientUpdatedAt,clientUpdatedAt,userId).run();
  }
}

async function serverChanges(db: D1Database, since: string): Promise<Array<{ entity: SyncEntity; operation: 'UPSERT' | 'DELETE'; data: Record<string, unknown> }>> {
  const result: Array<{ entity: SyncEntity; operation: 'UPSERT' | 'DELETE'; data: Record<string, unknown> }> = [];
  const definitions = [
    ['transaction','transactions',mapTransaction],['category','categories',mapCategory],['account','accounts',mapAccount],['merchant_rule','merchant_rules',mapMerchantRule],
  ] as const;
  for (const [entity,table,mapper] of definitions) {
    const rows = await db.prepare(`SELECT * FROM ${table} WHERE updated_at>? ORDER BY updated_at LIMIT 2000`).bind(since).all();
    for (const row of rows.results) {
      const mapped = mapper(row as Record<string, unknown>);
      result.push({ entity, operation: mapped.deletedAt ? 'DELETE' : 'UPSERT', data: mapped });
    }
  }
  return result;
}

syncRoutes.post('/', async (c) => {
  const input = await parseJson(c, syncSchema); if (isResponse(input)) return input;
  const serverTime = nowIso();
  try {
    for (const change of input.changes) await applyChange(c.env.DB, change, c.get('auth').userId, serverTime);
  } catch (error) {
    return c.json({ error: 'A local change could not be synchronized', details: error instanceof Error ? error.message : 'Unknown validation error' }, 400);
  }
  return c.json({ serverChanges: await serverChanges(c.env.DB, input.lastSyncAt ?? '1970-01-01T00:00:00.000Z'), serverTime });
});
