import { z } from 'zod';

export const loginSchema = z.object({
  username: z.string().trim().min(1).max(40),
  pin: z.string().regex(/^\d{4,8}$/),
  deviceId: z.string().min(8).max(100),
  deviceName: z.string().trim().min(1).max(100),
});

export const transactionSchema = z.object({
  id: z.string().min(8).max(100),
  amountPaise: z.number().int().positive(),
  currency: z.literal('INR').default('INR'),
  categoryId: z.string().nullable().optional(),
  paidByUserId: z.string().min(1),
  paymentMethod: z.enum(['CASH', 'UPI', 'OTHER']),
  accountId: z.string().nullable().optional(),
  merchant: z.string().trim().max(200).nullable().optional(),
  note: z.string().trim().max(500).nullable().optional(),
  occurredAt: z.string().datetime({ offset: true }),
  source: z.enum(['MANUAL', 'SMS', 'NOTIFICATION']).default('MANUAL'),
  status: z.enum(['DETECTED', 'CONFIRMED', 'IGNORED', 'DELETED']).default('CONFIRMED'),
  sourceReference: z.string().max(200).nullable().optional(),
  fingerprint: z.string().max(200).nullable().optional(),
  updatedAt: z.string().datetime({ offset: true }).optional(),
  version: z.number().int().positive().optional(),
});

export const transactionPatchSchema = transactionSchema.omit({ id: true }).partial();

export const categorySchema = z.object({
  id: z.string().min(8), name: z.string().trim().min(1).max(80), icon: z.string().max(80).default('category'), active: z.boolean().default(true), updatedAt: z.string().datetime({ offset: true }).optional(),
});

export const accountSchema = z.object({
  id: z.string().min(8), name: z.string().trim().min(1).max(100), bankName: z.string().max(100).nullable().optional(), last4: z.string().regex(/^\d{4}$/).nullable().optional(), ownerUserId: z.string().nullable().optional(), paymentMethod: z.enum(['CASH', 'UPI', 'OTHER']), active: z.boolean().default(true), updatedAt: z.string().datetime({ offset: true }).optional(),
});

export const merchantRuleSchema = z.object({
  id: z.string().min(8), merchantPattern: z.string().trim().min(2).max(100), categoryId: z.string().min(1), updatedAt: z.string().datetime({ offset: true }).optional(),
});

