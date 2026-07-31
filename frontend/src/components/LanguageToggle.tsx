"use client";

import { useI18n } from "@/components/I18nProvider";

export function LanguageToggle() {
  const { locale, setLocale, t } = useI18n();
  return (
    <div className="inline-flex items-center gap-1 rounded-md border border-line bg-bg-2/70 p-1" aria-label={t.common.language}>
      <button
        type="button"
        onClick={() => setLocale("it")}
        className={`rounded px-2.5 py-1 text-xs font-semibold tracking-wide transition ${
          locale === "it" ? "bg-accent text-bg-0" : "text-muted hover:text-text"
        }`}
      >
        IT
      </button>
      <button
        type="button"
        onClick={() => setLocale("en")}
        className={`rounded px-2.5 py-1 text-xs font-semibold tracking-wide transition ${
          locale === "en" ? "bg-accent text-bg-0" : "text-muted hover:text-text"
        }`}
      >
        EN
      </button>
    </div>
  );
}
