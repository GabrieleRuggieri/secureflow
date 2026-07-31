"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/AuthProvider";
import { AppShell } from "@/components/AppShell";
import { useI18n } from "@/components/I18nProvider";

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, loading, login } = useAuth();
  const { t } = useI18n();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) {
      // stay on page with CTA — or auto redirect
    }
  }, [loading, user, router]);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center text-muted">
        {t.common.loading}
      </div>
    );
  }

  if (!user) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-4">
        <p className="text-muted">{t.tagline}</p>
        <button
          type="button"
          onClick={() => login()}
          className="rounded-md bg-accent px-5 py-2.5 text-sm font-semibold text-bg-0"
        >
          {t.login}
        </button>
      </div>
    );
  }

  return <AppShell>{children}</AppShell>;
}
