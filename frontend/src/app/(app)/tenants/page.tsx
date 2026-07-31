"use client";

import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/components/AuthProvider";
import { useI18n } from "@/components/I18nProvider";
import { apiFetch } from "@/lib/api";
import type { Tenant } from "@/lib/types";

export default function TenantsPage() {
  const { t } = useI18n();
  const { token } = useAuth();
  const qc = useQueryClient();
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [rate, setRate] = useState(60);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["tenants"],
    enabled: !!token,
    queryFn: () => apiFetch<Tenant[]>("/api/tenants", token),
  });

  const create = useMutation({
    mutationFn: () =>
      apiFetch<Tenant>("/api/tenants", token, {
        method: "POST",
        body: JSON.stringify({ name, slug, rateLimitPerMinute: rate }),
      }),
    onSuccess: () => {
      setName("");
      setSlug("");
      setRate(60);
      qc.invalidateQueries({ queryKey: ["tenants"] });
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    create.mutate();
  }

  return (
    <div className="space-y-8">
      <header>
        <h1 className="font-display text-3xl font-semibold">{t.tenants.title}</h1>
        <p className="mt-1 text-muted">{t.tenants.subtitle}</p>
      </header>

      <form onSubmit={onSubmit} className="grid gap-3 rounded-lg border border-line bg-bg-2/40 p-4 sm:grid-cols-4">
        <label className="text-sm">
          <span className="text-muted">{t.tenants.name}</span>
          <input
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="mt-1 w-full rounded-md border border-line bg-bg-0 px-3 py-2"
          />
        </label>
        <label className="text-sm">
          <span className="text-muted">{t.tenants.slug}</span>
          <input
            required
            value={slug}
            onChange={(e) => setSlug(e.target.value)}
            className="mt-1 w-full rounded-md border border-line bg-bg-0 px-3 py-2 font-mono text-sm"
          />
        </label>
        <label className="text-sm">
          <span className="text-muted">{t.tenants.rate}</span>
          <input
            type="number"
            min={1}
            required
            value={rate}
            onChange={(e) => setRate(Number(e.target.value))}
            className="mt-1 w-full rounded-md border border-line bg-bg-0 px-3 py-2"
          />
        </label>
        <div className="flex items-end">
          <button
            type="submit"
            disabled={create.isPending}
            className="w-full rounded-md bg-accent px-4 py-2 text-sm font-semibold text-bg-0 disabled:opacity-60"
          >
            {t.tenants.create}
          </button>
        </div>
      </form>

      {isLoading && <p className="text-muted">{t.common.loading}</p>}
      {isError && <p className="text-danger">{t.common.error}</p>}

      <div className="overflow-hidden rounded-lg border border-line">
        <table className="w-full text-left text-sm">
          <thead className="bg-bg-2/80 text-xs uppercase tracking-wider text-muted">
            <tr>
              <th className="px-3 py-2">{t.tenants.name}</th>
              <th className="px-3 py-2">{t.tenants.slug}</th>
              <th className="px-3 py-2">{t.tenants.rate}</th>
            </tr>
          </thead>
          <tbody>
            {(data ?? []).map((tenant) => (
              <tr key={tenant.id} className="border-t border-line/70">
                <td className="px-3 py-2">{tenant.name}</td>
                <td className="px-3 py-2 font-mono text-xs text-accent-2">{tenant.slug}</td>
                <td className="px-3 py-2">{tenant.rateLimitPerMinute}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
