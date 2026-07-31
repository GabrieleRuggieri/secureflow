"use client";

import { LanguageToggle } from "@/components/LanguageToggle";
import { useAuth } from "@/components/AuthProvider";
import { useI18n } from "@/components/I18nProvider";
import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function HomePage() {
  const { t } = useI18n();
  const { user, loading, login } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && user) {
      router.replace("/dashboard");
    }
  }, [loading, user, router]);

  return (
    <div className="relative min-h-screen overflow-hidden">
      <div className="pointer-events-none absolute inset-0 opacity-40">
        <div className="absolute inset-x-0 top-24 h-px bg-gradient-to-r from-transparent via-accent to-transparent animate-pulse-line" />
        <div className="absolute inset-y-0 left-[12%] w-px bg-gradient-to-b from-transparent via-accent-2/40 to-transparent" />
      </div>

      <div className="relative mx-auto flex min-h-screen max-w-5xl flex-col justify-between px-6 py-8">
        <div className="flex items-center justify-between">
          <p className="font-display text-sm tracking-[0.2em] text-muted uppercase">Control plane</p>
          <LanguageToggle />
        </div>

        <section className="animate-rise max-w-2xl py-16">
          <h1 className="font-display text-5xl font-semibold tracking-tight text-text sm:text-6xl">
            <span className="text-accent">Secure</span>Flow
          </h1>
          <p className="mt-5 max-w-xl text-lg text-muted">{t.tagline}</p>
          <div className="mt-8 flex flex-wrap items-center gap-3">
            <button
              type="button"
              onClick={() => login()}
              className="rounded-md bg-accent px-5 py-2.5 text-sm font-semibold text-bg-0 shadow-[0_0_24px_rgba(45,212,191,0.25)] transition hover:brightness-110"
            >
              {t.login}
            </button>
            <span className="font-mono text-xs text-muted">Keycloak · PKCE · tenant-aware JWT</span>
          </div>
        </section>

        <p className="font-mono text-xs text-muted">self-hosted · docker · zero license cost</p>
      </div>
    </div>
  );
}
