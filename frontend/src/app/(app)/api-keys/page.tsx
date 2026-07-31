"use client";

import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/components/AuthProvider";
import { useI18n } from "@/components/I18nProvider";
import { apiFetch } from "@/lib/api";
import type { ApiKey, ApiKeyCreated } from "@/lib/types";

export default function ApiKeysPage() {
  const { t } = useI18n();
  const { token } = useAuth();
  const qc = useQueryClient();
  const [name, setName] = useState("dashboard-key");
  const [created, setCreated] = useState<ApiKeyCreated | null>(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["api-keys"],
    enabled: !!token,
    queryFn: () => apiFetch<ApiKey[]>("/api/api-keys", token),
  });

  const create = useMutation({
    mutationFn: () =>
      apiFetch<ApiKeyCreated>("/api/api-keys", token, {
        method: "POST",
        body: JSON.stringify({ name }),
      }),
    onSuccess: (key) => {
      setCreated(key);
      qc.invalidateQueries({ queryKey: ["api-keys"] });
    },
  });

  const revoke = useMutation({
    mutationFn: (id: string) =>
      apiFetch<ApiKey>(`/api/api-keys/${id}/revoke`, token, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["api-keys"] }),
  });

  const rotate = useMutation({
    mutationFn: (id: string) =>
      apiFetch<ApiKeyCreated>(`/api/api-keys/${id}/rotate`, token, { method: "POST" }),
    onSuccess: (key) => {
      setCreated(key);
      qc.invalidateQueries({ queryKey: ["api-keys"] });
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    create.mutate();
  }

  return (
    <div className="space-y-8">
      <header>
        <h1 className="font-display text-3xl font-semibold">{t.apiKeys.title}</h1>
        <p className="mt-1 text-muted">{t.apiKeys.subtitle}</p>
      </header>

      <form onSubmit={onSubmit} className="flex flex-wrap items-end gap-3 rounded-lg border border-line bg-bg-2/40 p-4">
        <label className="text-sm">
          <span className="text-muted">{t.apiKeys.name}</span>
          <input
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="mt-1 block rounded-md border border-line bg-bg-0 px-3 py-2"
          />
        </label>
        <button
          type="submit"
          className="rounded-md bg-accent px-4 py-2 text-sm font-semibold text-bg-0"
        >
          {t.apiKeys.create}
        </button>
      </form>

      {created?.rawKey && (
        <div className="rounded-lg border border-accent/40 bg-accent/10 px-4 py-3">
          <p className="text-sm text-accent">{t.apiKeys.once}</p>
          <code className="mt-2 block break-all font-mono text-sm">{created.rawKey}</code>
        </div>
      )}

      {isLoading && <p className="text-muted">{t.common.loading}</p>}
      {isError && <p className="text-danger">{t.common.error}</p>}

      <div className="overflow-hidden rounded-lg border border-line">
        <table className="w-full text-left text-sm">
          <thead className="bg-bg-2/80 text-xs uppercase tracking-wider text-muted">
            <tr>
              <th className="px-3 py-2">{t.apiKeys.name}</th>
              <th className="px-3 py-2">{t.apiKeys.prefix}</th>
              <th className="px-3 py-2">{t.apiKeys.status}</th>
              <th className="px-3 py-2" />
            </tr>
          </thead>
          <tbody>
            {(data ?? []).map((key) => (
              <tr key={key.id} className="border-t border-line/70">
                <td className="px-3 py-2">{key.name}</td>
                <td className="px-3 py-2 font-mono text-xs">{key.keyPrefix}</td>
                <td className="px-3 py-2">
                  {key.valid ? (
                    <span className="text-ok">{t.apiKeys.valid}</span>
                  ) : (
                    <span className="text-danger">{t.apiKeys.revoked}</span>
                  )}
                </td>
                <td className="px-3 py-2 text-right">
                  <button
                    type="button"
                    disabled={!key.valid}
                    onClick={() => rotate.mutate(key.id)}
                    className="mr-2 text-xs text-accent-2 disabled:opacity-40"
                  >
                    {t.apiKeys.rotate}
                  </button>
                  <button
                    type="button"
                    disabled={!key.valid}
                    onClick={() => revoke.mutate(key.id)}
                    className="text-xs text-danger disabled:opacity-40"
                  >
                    {t.apiKeys.revoke}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
