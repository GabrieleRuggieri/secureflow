"use client";

import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/components/AuthProvider";
import { useI18n } from "@/components/I18nProvider";
import { apiFetch } from "@/lib/api";
import type { Permission, Role, UserRow } from "@/lib/types";

export default function RbacPage() {
  const { t } = useI18n();
  const { token } = useAuth();

  const roles = useQuery({
    queryKey: ["roles"],
    enabled: !!token,
    queryFn: () => apiFetch<Role[]>("/api/roles", token),
  });
  const permissions = useQuery({
    queryKey: ["permissions"],
    enabled: !!token,
    queryFn: () => apiFetch<Permission[]>("/api/permissions", token),
  });
  const users = useQuery({
    queryKey: ["users"],
    enabled: !!token,
    queryFn: () => apiFetch<UserRow[]>("/api/users", token),
  });

  return (
    <div className="space-y-8">
      <header>
        <h1 className="font-display text-3xl font-semibold">{t.rbac.title}</h1>
        <p className="mt-1 text-muted">{t.rbac.subtitle}</p>
      </header>

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="rounded-lg border border-line bg-bg-2/30 p-4">
          <h2 className="font-display text-lg text-accent">{t.rbac.roles}</h2>
          <ul className="mt-3 space-y-2 text-sm">
            {(roles.data ?? []).map((role) => (
              <li key={role.id} className="border-b border-line/50 pb-2">
                <p className="font-semibold">{role.name}</p>
                <p className="text-xs text-muted">{role.description}</p>
                <p className="mt-1 font-mono text-[11px] text-accent-2">
                  {(role.permissionIds ?? []).length} permissions
                </p>
              </li>
            ))}
          </ul>
        </section>

        <section className="rounded-lg border border-line bg-bg-2/30 p-4">
          <h2 className="font-display text-lg text-accent">{t.rbac.permissions}</h2>
          <ul className="mt-3 space-y-2 text-sm">
            {(permissions.data ?? []).map((p) => (
              <li key={p.id} className="font-mono text-xs">
                <span className="text-accent-2">{p.resource}</span>:{p.action}
              </li>
            ))}
          </ul>
        </section>

        <section className="rounded-lg border border-line bg-bg-2/30 p-4">
          <h2 className="font-display text-lg text-accent">{t.rbac.users}</h2>
          <ul className="mt-3 space-y-2 text-sm">
            {(users.data ?? []).map((u) => (
              <li key={u.id} className="border-b border-line/50 pb-2">
                <p className="font-semibold">{u.username}</p>
                <p className="text-xs text-muted">{u.email}</p>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </div>
  );
}
