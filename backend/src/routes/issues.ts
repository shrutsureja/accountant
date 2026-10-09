import { Hono } from 'hono';
import { z } from 'zod';
import type { Bindings, Variables } from '../types';
import { isResponse, nowIso, parseJson } from '../utils/http';

export const issueRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

export const createIssueSchema = z.object({
  category: z.enum(['BUG', 'FEATURE', 'OTHER']),
  title: z.string().trim().min(3).max(120),
  description: z.string().trim().min(10).max(4000),
  screenshots: z.array(z.object({
    mimeType: z.enum(['image/jpeg', 'image/png']),
    base64: z.string().regex(/^[A-Za-z0-9+/]+={0,2}$/).max(1_200_000),
  })).max(3).default([]),
});

issueRoutes.get('/', async (c) => {
  const rows = await c.env.DB.prepare(`
    SELECT i.id,i.category,i.title,i.description,i.status,i.created_at createdAt,
      u.display_name reporterName,COUNT(s.id) screenshotCount
    FROM issue_reports i JOIN users u ON u.id=i.reporter_user_id
      LEFT JOIN issue_screenshots s ON s.issue_id=i.id
    GROUP BY i.id ORDER BY i.created_at DESC LIMIT 100
  `).all();
  return c.json({ issues: rows.results });
});

issueRoutes.post('/', async (c) => {
  const input = await parseJson(c, createIssueSchema);
  if (isResponse(input)) return input;
  const images: Array<{ id: string; mimeType: string; base64: string }> = [];
  for (const screenshot of input.screenshots) {
    const decoded = (() => { try { return atob(screenshot.base64); } catch { return null; } })();
    if (!decoded || decoded.length > 850_000) return c.json({ error: 'Each screenshot must be a valid image under 850 KB' }, 400);
    const isJpeg = decoded.charCodeAt(0) === 0xff && decoded.charCodeAt(1) === 0xd8 && decoded.charCodeAt(2) === 0xff;
    const isPng = decoded.startsWith('\x89PNG\r\n\x1a\n');
    if (!(screenshot.mimeType === 'image/jpeg' ? isJpeg : isPng)) return c.json({ error: 'Screenshot format does not match its file type' }, 400);
    images.push({ id: crypto.randomUUID(), ...screenshot });
  }
  const id = crypto.randomUUID();
  const timestamp = nowIso();
  await c.env.DB.batch([
    c.env.DB.prepare('INSERT INTO issue_reports (id,reporter_user_id,category,title,description,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)')
      .bind(id,c.get('auth').userId,input.category,input.title,input.description,'OPEN',timestamp,timestamp),
    ...images.map(image => c.env.DB.prepare('INSERT INTO issue_screenshots (id,issue_id,mime_type,data_base64,created_at) VALUES (?,?,?,?,?)')
      .bind(image.id,id,image.mimeType,image.base64,timestamp)),
  ]);
  return c.json({ id, status: 'OPEN' }, 201);
});

issueRoutes.get('/:id', async (c) => {
  const issue = await c.env.DB.prepare(`
    SELECT i.id,i.category,i.title,i.description,i.status,i.created_at createdAt,
      u.display_name reporterName FROM issue_reports i JOIN users u ON u.id=i.reporter_user_id WHERE i.id=?
  `).bind(c.req.param('id')).first();
  if (!issue) return c.json({ error: 'Issue not found' }, 404);
  const screenshots = await c.env.DB.prepare('SELECT id,mime_type mimeType FROM issue_screenshots WHERE issue_id=? ORDER BY created_at,id').bind(c.req.param('id')).all();
  return c.json({ issue, screenshots: screenshots.results });
});

issueRoutes.get('/:id/screenshots/:imageId', async (c) => {
  const image = await c.env.DB.prepare('SELECT mime_type mimeType,data_base64 base64 FROM issue_screenshots WHERE issue_id=? AND id=?')
    .bind(c.req.param('id'),c.req.param('imageId')).first<{ mimeType: string; base64: string }>();
  if (!image) return c.json({ error: 'Screenshot not found' }, 404);
  const bytes = Uint8Array.from(atob(image.base64), character => character.charCodeAt(0));
  return new Response(bytes, { headers: { 'Content-Type': image.mimeType, 'Cache-Control': 'private, no-store' } });
});
