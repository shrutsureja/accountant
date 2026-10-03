import { Hono } from 'hono';
import { orderSyncChanges } from '../sync-order';
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

function prepareChange(db: D1Database, change: SyncChange, userId: string, serverTime: string): D1PreparedStatement {
  const id = change.data.id;
  if (typeof id !== 'string') throw new Error('Every sync change needs an id');
  const clientUpdatedAt = typeof change.data.updatedAt === 'string' ? change.data.updatedAt : serverTime;
  const table = ({ transaction: 'transactions', category: 'categories', account: 'accounts', merchant_rule: 'merchant_rules' } as const)[change.entity];
  if (change.operation === 'DELETE') {
    if (change.entity === 'transaction') return db.prepare("UPDATE transactions SET status='DELETED',deleted_at=?,updated_at=?,client_updated_at=?,updated_by_user_id=?,version=version+1 WHERE id=? AND julianday(client_updated_at)<=julianday(?)").bind(serverTime,serverTime,clientUpdatedAt,userId,id,clientUpdatedAt);
    return db.prepare(`UPDATE ${table} SET deleted_at=?,updated_at=?,client_updated_at=?,updated_by=?,version=version+1 WHERE id=? AND julianday(client_updated_at)<=julianday(?)`).bind(serverTime,serverTime,clientUpdatedAt,userId,id,clientUpdatedAt);
  }

  if (change.entity === 'transaction') {
    const v = transactionSchema.parse(change.data);
    return db.prepare(`INSERT INTO transactions (id,amount_paise,currency,category_id,paid_by_user_id,payment_method,account_id,merchant,note,occurred_at,source,status,source_reference,fingerprint,created_by_user_id,updated_by_user_id,created_at,updated_at,client_updated_at,version)
      VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET amount_paise=excluded.amount_paise,currency=excluded.currency,category_id=excluded.category_id,paid_by_user_id=excluded.paid_by_user_id,payment_method=excluded.payment_method,account_id=excluded.account_id,merchant=excluded.merchant,note=excluded.note,occurred_at=excluded.occurred_at,source=excluded.source,status=excluded.status,source_reference=excluded.source_reference,fingerprint=excluded.fingerprint,updated_by_user_id=excluded.updated_by_user_id,updated_at=excluded.updated_at,client_updated_at=excluded.client_updated_at,version=excluded.version WHERE julianday(transactions.client_updated_at)<=julianday(excluded.client_updated_at)`)
      .bind(v.id,v.amountPaise,v.currency,v.categoryId ?? null,v.paidByUserId,v.paymentMethod,v.accountId ?? null,v.merchant ?? null,v.note ?? null,v.occurredAt,v.source,v.status,v.sourceReference ?? null,v.fingerprint ?? null,userId,userId,serverTime,serverTime,clientUpdatedAt,v.version ?? 1);
  } else if (change.entity === 'category') {
    const v = categorySchema.parse(change.data);
    return db.prepare('INSERT INTO categories (id,name,icon,active,created_at,updated_at,client_updated_at,updated_by) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,icon=excluded.icon,active=excluded.active,updated_at=excluded.updated_at,client_updated_at=excluded.client_updated_at,updated_by=excluded.updated_by,version=categories.version+1 WHERE julianday(categories.client_updated_at)<=julianday(excluded.client_updated_at)').bind(v.id,v.name,v.icon,v.active?1:0,serverTime,serverTime,clientUpdatedAt,userId);
  } else if (change.entity === 'account') {
    const v = accountSchema.parse(change.data);
    return db.prepare('INSERT INTO accounts (id,name,bank_name,last4,owner_user_id,payment_method,active,created_at,updated_at,client_updated_at,updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,bank_name=excluded.bank_name,last4=excluded.last4,owner_user_id=excluded.owner_user_id,payment_method=excluded.payment_method,active=excluded.active,updated_at=excluded.updated_at,client_updated_at=excluded.client_updated_at,updated_by=excluded.updated_by,version=accounts.version+1 WHERE julianday(accounts.client_updated_at)<=julianday(excluded.client_updated_at)').bind(v.id,v.name,v.bankName ?? null,v.last4 ?? null,v.ownerUserId ?? null,v.paymentMethod,v.active?1:0,serverTime,serverTime,clientUpdatedAt,userId);
  } else {
    const v = merchantRuleSchema.parse(change.data);
    return db.prepare('INSERT INTO merchant_rules (id,merchant_pattern,category_id,created_by_user_id,created_at,updated_at,client_updated_at,updated_by) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET merchant_pattern=excluded.merchant_pattern,category_id=excluded.category_id,updated_at=excluded.updated_at,client_updated_at=excluded.client_updated_at,updated_by=excluded.updated_by,version=merchant_rules.version+1 WHERE julianday(merchant_rules.client_updated_at)<=julianday(excluded.client_updated_at)').bind(v.id,v.merchantPattern.toUpperCase(),v.categoryId,userId,serverTime,serverTime,clientUpdatedAt,userId);
  }
}

async function serverChanges(db: D1Database, since: string): Promise<Array<{ entity: SyncEntity; operation: 'UPSERT' | 'DELETE'; data: Record<string, unknown> }>> {
  const result: Array<{ entity: SyncEntity; operation: 'UPSERT' | 'DELETE'; data: Record<string, unknown> }> = [];
  const definitions = [
    ['transaction','transactions',mapTransaction],['category','categories',mapCategory],['account','accounts',mapAccount],['merchant_rule','merchant_rules',mapMerchantRule],
  ] as const;
  for (const [entity,table,mapper] of definitions) {
    const rows = await db.prepare(`SELECT * FROM ${table} WHERE julianday(updated_at)>=julianday(?) ORDER BY julianday(updated_at) LIMIT 2000`).bind(since).all();
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
    const statements = orderSyncChanges(input.changes).map(change => prepareChange(c.env.DB, change, c.get('auth').userId, serverTime));
    if (statements.length) await c.env.DB.batch(statements);
  } catch (error) {
    console.warn('sync_change_failed', error instanceof z.ZodError
      ? { issues: error.issues.map(({ code, path }) => ({ code, path })) }
      : { kind: String(error).includes('FOREIGN KEY') ? 'foreign_key' : String(error).includes('UNIQUE constraint') ? 'unique_constraint' : 'database_or_runtime' });
    return c.json({ error: 'A local change could not be synchronized', details: error instanceof Error ? error.message : 'Unknown validation error' }, 400);
  }
  return c.json({ serverChanges: await serverChanges(c.env.DB, input.lastSyncAt ?? '1970-01-01T00:00:00.000Z'), serverTime });
});
