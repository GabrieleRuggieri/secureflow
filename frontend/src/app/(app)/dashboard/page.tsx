"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/components/AuthProvider";
import { useI18n } from "@/components/I18nProvider";
import { apiFetch } from "@/lib/api";
import type { AuditEvent, Page } from "@/lib/types";

export default function DashboardPage() {
  const { t } = useI18n();
  const { token } = useAuth();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["audit-metrics"],
    enabled: !!token,
    queryFn: () =>
      apiFetch<Page<AuditEvent>>("/api/audit-events?size=100&sort=occurredAt,desc", token),
  });

  const metrics = useMemo(() => {
    const events = data?.content ?? [];
    const total = events.length;
    const success = events.filter((e) => e.outcome === "success").length;
    const errors = events.filter((e) => e.outcome === "error" || e.status >= 500).length;
    const avgLatency =
      total === 0 ? 0 : Math.round(events.reduce((sum, e) => sum + e.durationMs, 0) / total);
    const successRate = total === 0 ? 0 : Math.round((success / total) * 100);
    return { total, successRate, errors, avgLatency, events: events.slice(0, 8) };
  }, [data]);

  return (
    <div className="space-y-8">
      <header>
        <h1 className="font-display text-3xl font-semibold tracking-tight">{t.dashboard.title}</h1>
        <p className="mt-1 text-muted">{t.dashboard.subtitle}</p>
      </header>

      {isLoading && <p className="text-muted">{t.common.loading}</p>}
      {isError && <p className="text-danger">{t.common.error}</p>}

      {!isLoading && !isError && metrics.total === 0 && (
        <p className="rounded-lg border border-line bg-bg-2/50 px-4 py-6 text-muted">
          {t.dashboard.empty}
        </p>
      )}

      {metrics.total > 0 && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {[
              { label: t.dashboard.total, value: String(metrics.total) },
              { label: t.dashboard.success, value: `${metrics.successRate}%` },
              { label: t.dashboard.errors, value: String(metrics.errors) },
              { label: t.dashboard.latency, value: `${metrics.avgLatency} ms` },
            ].map((card) => (
              <div key={card.label} className="rounded-lg border border-line bg-bg-2/40 px-4 py-5">
                <p className="text-xs uppercase tracking-wider text-muted">{card.label}</p>
                <p className="mt-2 font-display text-3xl font-semibold text-accent">{card.value}</p>
              </div>
            ))}
          </div>

          <div className="overflow-hidden rounded-lg border border-line">
            <table className="w-full text-left text-sm">
              <thead className="bg-bg-2/80 text-xs uppercase tracking-wider text-muted">
                <tr>
                  <th className="px-3 py-2">{t.audit.method}</th>
                  <th className="px-3 py-2">{t.audit.path}</th>
                  <th className="px-3 py-2">{t.audit.status}</th>
                  <th className="px-3 py-2">{t.audit.duration}</th>
                </tr>
              </thead>
              <tbody>
                {metrics.events.map((e) => (
                  <tr key={e.id} className="border-t border-line/70">
                    <td className="px-3 py-2 font-mono text-xs">{e.method}</td>
                    <td className="px-3 py-2 font-mono text-xs text-muted">{e.path}</td>
                    <td className="px-3 py-2">{e.status}</td>
                    <td className="px-3 py-2">{e.durationMs} ms</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
