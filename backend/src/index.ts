import { Hono } from 'hono';
import { cors } from 'hono/cors';
import type { Bindings, Variables } from './types';
import { authRoutes } from './routes/auth';
import { transactionRoutes } from './routes/transactions';
import { catalogRoutes } from './routes/catalog';
import { analyticsRoutes } from './routes/analytics';
import { syncRoutes } from './routes/sync';
import { requireAuth } from './middleware/auth';

const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

app.use('*', cors({ origin: '*', allowHeaders: ['Authorization','Content-Type'], allowMethods: ['GET','POST','PATCH','DELETE','OPTIONS'] }));
app.get('/health', (c) => c.json({ status: 'ok', service: 'accountant-api' }));
app.route('/api/v1/auth', authRoutes);
app.use('/api/v1/*', requireAuth);
app.route('/api/v1/transactions', transactionRoutes);
app.route('/api/v1/analytics', analyticsRoutes);
app.route('/api/v1/sync', syncRoutes);
app.route('/api/v1', catalogRoutes);
app.notFound((c) => c.json({ error: 'Not found' }, 404));
app.onError((error, c) => {
  console.error('request_failed', { path: c.req.path, message: error.message });
  return c.json({ error: 'Something went wrong' }, 500);
});

export default app;

