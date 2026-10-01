import { Hono } from 'hono';
import type { Bindings, Variables } from '../types';

export const analyticsRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

function monthBounds(month?: string): [string, string] {
  const match = month?.match(/^(\d{4})-(\d{2})$/);
  const start = match ? new Date(Date.UTC(Number(match[1]), Number(match[2]) - 1, 1)) : new Date();
  if (!match) start.setUTCDate(1), start.setUTCHours(0, 0, 0, 0);
  const end = new Date(Date.UTC(start.getUTCFullYear(), start.getUTCMonth() + 1, 1));
  return [start.toISOString(), end.toISOString()];
}

const confirmed = "t.status='CONFIRMED' AND t.deleted_at IS NULL AND t.occurred_at>=? AND t.occurred_at<?";

analyticsRoutes.get('/monthly', async (c) => {
  const [from, to] = monthBounds(c.req.query('month'));
  const row = await c.env.DB.prepare(`SELECT COALESCE(SUM(amount_paise),0) totalPaise,COUNT(*) count FROM transactions t WHERE ${confirmed}`).bind(from,to).first();
  return c.json({ month: from.slice(0,7), ...row });
});

analyticsRoutes.get('/daily', async (c) => {
  const [from, to] = monthBounds(c.req.query('month'));
  const rows = await c.env.DB.prepare(`SELECT substr(occurred_at,1,10) day,SUM(amount_paise) totalPaise FROM transactions t WHERE ${confirmed} GROUP BY day ORDER BY day`).bind(from,to).all();
  return c.json({ data: rows.results });
});

analyticsRoutes.get('/weekly', async (c) => {
  const [from, to] = monthBounds(c.req.query('month'));
  const rows = await c.env.DB.prepare(`SELECT CAST((CAST(substr(occurred_at,9,2) AS INTEGER)-1)/7 AS INTEGER)+1 week,SUM(amount_paise) totalPaise FROM transactions t WHERE ${confirmed} GROUP BY week ORDER BY week`).bind(from,to).all();
  return c.json({ data: rows.results });
});

analyticsRoutes.get('/categories', async (c) => {
  const [from, to] = monthBounds(c.req.query('month'));
  const rows = await c.env.DB.prepare(`SELECT c.id,c.name,COALESCE(SUM(t.amount_paise),0) totalPaise FROM transactions t LEFT JOIN categories c ON c.id=t.category_id WHERE ${confirmed} GROUP BY c.id,c.name ORDER BY totalPaise DESC`).bind(from,to).all();
  return c.json({ data: rows.results });
});

analyticsRoutes.get('/members', async (c) => {
  const [from, to] = monthBounds(c.req.query('month'));
  const rows = await c.env.DB.prepare(`SELECT u.id,u.display_name displayName,COALESCE(SUM(t.amount_paise),0) totalPaise FROM users u LEFT JOIN transactions t ON t.paid_by_user_id=u.id AND ${confirmed} GROUP BY u.id,u.display_name ORDER BY totalPaise DESC`).bind(from,to).all();
  return c.json({ data: rows.results });
});

analyticsRoutes.get('/month-comparison', async (c) => {
  const [currentFrom,currentTo] = monthBounds(c.req.query('month'));
  const date = new Date(currentFrom); date.setUTCMonth(date.getUTCMonth()-1);
  const [previousFrom] = monthBounds(date.toISOString().slice(0,7));
  const row = await c.env.DB.prepare(`SELECT COALESCE(SUM(CASE WHEN occurred_at>=? AND occurred_at<? THEN amount_paise END),0) currentPaise,COALESCE(SUM(CASE WHEN occurred_at>=? AND occurred_at<? THEN amount_paise END),0) previousPaise FROM transactions WHERE status='CONFIRMED' AND deleted_at IS NULL`).bind(currentFrom,currentTo,previousFrom,currentFrom).first<Record<string, number>>();
  const currentPaise = row?.currentPaise ?? 0;
  const previousPaise = row?.previousPaise ?? 0;
  const percentChange = previousPaise ? ((currentPaise-previousPaise)/previousPaise)*100 : null;
  return c.json({ ...row, percentChange });
});
