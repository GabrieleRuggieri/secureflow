"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/components/AuthProvider";
import { useI18n } from "@/components/I18nProvider";
import { LanguageToggle } from "@/components/LanguageToggle";

const links = [
  { href: "/dashboard", key: "dashboard" as const },
  { href: "/tenants", key: "tenants" as const },
  { href: "/rbac", key: "rbac" as const },
  { href: "/api-keys", key: "apiKeys" as const },
  { href: "/audit", key: "audit" as const },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { t } = useI18n();
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-20 border-b border-line bg-bg-0/80 backdrop-blur-md">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <div className="flex items-center gap-6">
            <Link href="/dashboard" className="font-display text-lg font-semibold tracking-tight">
              <span className="text-accent">Secure</span>Flow
            </Link>
            <nav className="hidden items-center gap-1 md:flex">
              {links.map((link) => {
                const active = pathname.startsWith(link.href);
                return (
                  <Link
                    key={link.href}
                    href={link.href}
                    className={`rounded-md px-3 py-1.5 text-sm transition ${
                      active ? "bg-bg-2 text-accent" : "text-muted hover:text-text"
                    }`}
                  >
                    {t.nav[link.key]}
                  </Link>
                );
              })}
            </nav>
          </div>
          <div className="flex items-center gap-3">
            <LanguageToggle />
            <span className="hidden text-xs text-muted sm:inline">
              {user?.profile?.preferred_username ?? user?.profile?.email}
            </span>
            <button
              type="button"
              onClick={() => logout()}
              className="rounded-md border border-line px-3 py-1.5 text-xs font-semibold text-muted hover:border-accent hover:text-accent"
            >
              {t.logout}
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8 animate-rise">{children}</main>
    </div>
  );
}
