export type Tenant = {
  id: string;
  name: string;
  slug: string;
  rateLimitPerMinute: number;
  createdAt?: string;
};

export type Role = {
  id: string;
  name: string;
  description?: string;
  permissionIds?: string[];
};

export type Permission = {
  id: string;
  resource: string;
  action: string;
  description?: string;
};

export type UserRow = {
  id: string;
  email: string;
  username: string;
  keycloakUserId?: string;
};

export type ApiKey = {
  id: string;
  keyPrefix: string;
  name: string;
  expiresAt?: string | null;
  revokedAt?: string | null;
  createdAt: string;
  valid: boolean;
};

export type ApiKeyCreated = {
  id: string;
  keyPrefix: string;
  name: string;
  rawKey: string;
  expiresAt?: string | null;
  createdAt: string;
};

export type AuditEvent = {
  id: string;
  eventId: string;
  tenantId: string;
  keyPrefix?: string;
  method: string;
  path: string;
  status: number;
  durationMs: number;
  outcome: string;
  occurredAt: string;
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};
