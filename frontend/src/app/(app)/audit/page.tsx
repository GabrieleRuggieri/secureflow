"use client";

import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/components/AuthProvider";
import { useI18n } from "@/components/I18nProvider";
import { apiFetch, coreApiBase } from "@/lib/api";
import type { AuditEvent, Page } from "@/lib/types";

export default function AuditPage() {
  const { t } = useI18n();
  const { token } = useAuth();
  const qc = useQueryClient();
  const [outcome, setOutcome] = useState("");
  const [live, setLive] = useState(false);
  const [liveEvents, setLiveEvents] = useState<AuditEvent[]>([]);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["audit", outcome],
    enabled: !!token,
    queryFn: () => {
      const qs = new URLSearchParams({ size: "50", sort: "occurredAt,desc" });
      if (outcome) qs.set("outcome", outcome);
      return apiFetch<Page<AuditEvent>>(`/api/audit-events?${qs}`, token);
    },
  });

  useEffect(() => {
    if (!live || !token) return;
    const qs = new URLSearchParams();
    if (outcome) qs.set("outcome", outcome);
    const url = `${coreApiBase()}/api/audit-events/stream?${qs}`;
    const es = new EventSource(url + (token ? "" : ""), {
      withCredentials: false,
    } as EventSourceInit);

    // EventSource cannot set Authorization header — use fetch stream polyfill via query is not ideal.
    // For local admin UX we reopen history on toggle; live uses fetch+ReadableStream when possible.
    es.close();

    const controller = new AbortController();
    (async () => {
      try {
        const res = await fetch(url, {
          headers: { Authorization: `Bearer ${token}`, Accept: "text/event-stream" },
          signal: controller.signal,
        });
        if (!res.ok || !res.body) return;
        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          const chunks = buffer.split("\n\n");
          buffer = chunks.pop() ?? "";
          for (const chunk of chunks) {
            const dataLine = chunk
              .split("\n")
              .find((line) => line.startsWith("data:"));
            if (!dataLine) continue;
            const payload = dataLine.replace(/^data:\s?/, "");
            if (payload === "ok") continue;
            try {
              const event = JSON.parse(payload) as AuditEvent;
              setLiveEvents((prev) => [event, ...prev].slice(0, 50));
              qc.invalidateQueries({ queryKey: ["audit"] });
            } catch {
              // ignore non-json heartbeats
            }
          }
        }
      } catch {
        // aborted or network error
      }
    })();

    return () => controller.abort();
  }, [live, outcome, token, qc]);

  const rows = live ? [...liveEvents, ...(data?.content ?? [])] : (data?.content ?? []);
  const unique = Array.from(new Map(rows.map((e) => [e.eventId, e])).values());

  return (
    <div className="space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-semibold">{t.audit.title}</h1>
          <p className="mt-1 text-muted">{t.audit.subtitle}</p>
        </div>
        <div className="flex items-center gap-3">
          <label className="text-sm text-muted">
            {t.audit.outcome}
            <select
              value={outcome}
              onChange={(e) => setOutcome(e.target.value)}
              className="ml-2 rounded-md border border-line bg-bg-0 px-2 py-1.5 text-text"
            >
              <option value="">{t.audit.all}</option>
              <option value="success">success</option>
              <option value="denied">denied</option>
              <option value="rate_limited">rate_limited</option>
              <option value="error">error</option>
            </select>
          </label>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={live} onChange={(e) => setLive(e.target.checked)} />
            {t.audit.live}
          </label>
          <button
            type="button"
            onClick={() => refetch()}
            className="rounded-md border border-line px-3 py-1.5 text-xs text-muted hover:text-accent"
          >
            {t.common.refresh}
          </button>
        </div>
      </header>

      {isLoading && <p className="text-muted">{t.common.loading}</p>}
      {isError && <p className="text-danger">{t.common.error}</p>}

      <div className="overflow-hidden rounded-lg border border-line">
        <table className="w-full text-left text-sm">
          <thead className="bg-bg-2/80 text-xs uppercase tracking-wider text-muted">
            <tr>
              <th className="px-3 py-2">{t.audit.when}</th>
              <th className="px-3 py-2">{t.audit.method}</th>
              <th className="px-3 py-2">{t.audit.path}</th>
              <th className="px-3 py-2">{t.audit.status}</th>
              <th className="px-3 py-2">{t.audit.outcome}</th>
              <th className="px-3 py-2">{t.audit.duration}</th>
            </tr>
          </thead>
          <tbody>
            {unique.map((e) => (
              <tr key={e.eventId} className="border-t border-line/70">
                <td className="px-3 py-2 font-mono text-[11px] text-muted">
                  {new Date(e.occurredAt).toLocaleString()}
                </td>
                <td className="px-3 py-2 font-mono text-xs">{e.method}</td>
                <td className="px-3 py-2 font-mono text-xs">{e.path}</td>
                <td className="px-3 py-2">{e.status}</td>
                <td className="px-3 py-2 text-accent-2">{e.outcome}</td>
                <td className="px-3 py-2">{e.durationMs} ms</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
