export interface Env {
  DB: D1Database;
  ALLOWED_ORIGIN: string;
}

const allowedEvents = new Set(["page_view", "download_click"]);
const allowedLanguages = new Set(["pt-BR", "en"]);

function corsHeaders(origin: string | null, allowedOrigin: string) {
  return {
    "access-control-allow-origin": origin === allowedOrigin ? allowedOrigin : "",
    "access-control-allow-methods": "POST, OPTIONS",
    "access-control-allow-headers": "content-type",
    "access-control-max-age": "86400",
  };
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const origin = request.headers.get("origin");
    const headers = corsHeaders(origin, env.ALLOWED_ORIGIN);

    if (url.pathname !== "/metrics") return new Response("Not found", { status: 404 });
    if (request.method === "OPTIONS") {
      return new Response(null, { status: origin === env.ALLOWED_ORIGIN ? 204 : 403, headers });
    }
    if (request.method !== "POST" || origin !== env.ALLOWED_ORIGIN) {
      return Response.json({ error: "forbidden" }, { status: 403, headers });
    }

    try {
      const payload = await request.json() as { event?: unknown; language?: unknown };
      if (typeof payload.event !== "string" || !allowedEvents.has(payload.event) || typeof payload.language !== "string" || !allowedLanguages.has(payload.language)) {
        return Response.json({ error: "invalid metric" }, { status: 400, headers });
      }

      const day = new Date().toISOString().slice(0, 10);
      await env.DB.prepare(`INSERT INTO daily_metrics (day, event, language, count, updated_at)
        VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP)
        ON CONFLICT(day, event, language) DO UPDATE SET count = count + 1, updated_at = CURRENT_TIMESTAMP`)
        .bind(day, payload.event, payload.language)
        .run();
      return new Response(null, { status: 204, headers });
    } catch {
      return Response.json({ error: "metric unavailable" }, { status: 500, headers });
    }
  },
};
