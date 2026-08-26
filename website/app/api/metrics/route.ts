import { env } from "cloudflare:workers";

const allowedEvents = new Set(["page_view", "download_click"]);
const allowedLanguages = new Set(["pt-BR", "en"]);

const createTable = `CREATE TABLE IF NOT EXISTS daily_metrics (
  day TEXT NOT NULL,
  event TEXT NOT NULL CHECK (event IN ('page_view', 'download_click')),
  language TEXT NOT NULL CHECK (language IN ('pt-BR', 'en')),
  count INTEGER NOT NULL DEFAULT 0,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (day, event, language)
)`;

export async function POST(request: Request) {
  const url = new URL(request.url);
  const origin = request.headers.get("origin");
  if (origin && origin !== url.origin) return Response.json({ error: "forbidden" }, { status: 403 });

  try {
    const payload = await request.json() as { event?: unknown; language?: unknown };
    if (typeof payload.event !== "string" || !allowedEvents.has(payload.event) || typeof payload.language !== "string" || !allowedLanguages.has(payload.language)) {
      return Response.json({ error: "invalid metric" }, { status: 400 });
    }

    const day = new Date().toISOString().slice(0, 10);
    await env.DB.prepare(createTable).run();
    await env.DB.prepare(`INSERT INTO daily_metrics (day, event, language, count, updated_at)
      VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP)
      ON CONFLICT(day, event, language) DO UPDATE SET count = count + 1, updated_at = CURRENT_TIMESTAMP`)
      .bind(day, payload.event, payload.language)
      .run();
    return new Response(null, { status: 204 });
  } catch {
    return Response.json({ error: "metric unavailable" }, { status: 500 });
  }
}
