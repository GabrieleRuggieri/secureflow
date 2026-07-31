import en from "@/messages/en.json";
import it from "@/messages/it.json";

export type Locale = "en" | "it";
export type Messages = typeof en;

const dictionaries: Record<Locale, Messages> = { en, it };

export function getMessages(locale: Locale): Messages {
  return dictionaries[locale] ?? en;
}

export const LOCALE_COOKIE = "sf_locale";
