export type Bindings = {
  DB: D1Database;
  JWT_SECRET: string;
  ACCESS_TOKEN_TTL_SECONDS?: string;
};

export type AuthUser = { userId: string; deviceId: string };
export type Variables = { auth: AuthUser };

export type SyncEntity = 'transaction' | 'category' | 'account' | 'merchant_rule';
export type SyncChange = {
  entity: SyncEntity;
  operation: 'UPSERT' | 'DELETE';
  data: Record<string, unknown>;
};

