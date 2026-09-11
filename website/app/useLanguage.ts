"use client";

import { useCallback, useEffect, useState } from "react";

export type Language = "pt-BR" | "en";

export function useLanguage() {
  const [language, setLanguageState] = useState<Language>("pt-BR");
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const query = new URLSearchParams(window.location.search).get("lang");
    const saved = window.localStorage.getItem("yomudoom-language");
    const detected = window.navigator.language.toLowerCase().startsWith("pt") ? "pt-BR" : "en";
    const initial: Language = query === "en" || query === "pt-BR"
      ? query
      : saved === "en" || saved === "pt-BR"
        ? saved
        : detected;
    queueMicrotask(() => {
      setLanguageState(initial);
      setReady(true);
    });
  }, []);

  const setLanguage = useCallback((next: Language) => {
    setLanguageState(next);
    window.localStorage.setItem("yomudoom-language", next);
    document.documentElement.lang = next;
    const url = new URL(window.location.href);
    if (next === "en") url.searchParams.set("lang", "en");
    else url.searchParams.delete("lang");
    window.history.replaceState({}, "", url);
  }, []);

  useEffect(() => {
    if (ready) document.documentElement.lang = language;
  }, [language, ready]);

  return { language, setLanguage, ready };
}
