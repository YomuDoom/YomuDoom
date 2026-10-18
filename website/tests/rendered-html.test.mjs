import assert from "node:assert/strict";
import test from "node:test";
import { readFile } from "node:fs/promises";

async function loadWorker() {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}-${Math.random()}`);
  return (await import(workerUrl.href)).default;
}

const assets = { fetch: async () => new Response("Not found", { status: 404 }) };
const context = { waitUntil() {}, passThroughOnException() {} };

test("renders the YomuDoom landing page", async () => {
  const worker = await loadWorker();
  const response = await worker.fetch(new Request("https://yomudoom.example/", { headers: { accept: "text/html", host: "yomudoom.example", "x-forwarded-proto": "https" } }), { ASSETS: assets }, context);
  assert.equal(response.status, 200);
  const html = await response.text();
  assert.match(html, /YomuDoom/);
  assert.match(html, /Um leitor feito para o meu jeito de ler/);
  assert.match(html, /O que eu mudei/);
  assert.match(html, /Busca consolidada, na prática/);
  assert.match(html, /Busca consolidada do YomuDoom mostrando fontes ordenadas pela quantidade de capítulos/);
  assert.match(html, /O que continua igual ao Mihon/);
  assert.match(html, /Downloads, histórico, backup e a retomada básica de leitura já faziam parte do Mihon/);
  assert.match(html, /Mantido por uma pessoa/);
  assert.doesNotMatch(html, /Usei IA na criação do ícone/);
  assert.match(html, /github\.com\/YomuDoom\/YomuDoom\/releases\/latest/);
  assert.match(html, /og\.png/);
  assert.doesNotMatch(html, /feature-number|trust-strip|glow/);
  assert.doesNotMatch(html, /codex-preview|react-loading-skeleton|Your site is taking shape/);
});

test("renders the complete privacy route without the generic social image", async () => {
  const worker = await loadWorker();
  const response = await worker.fetch(new Request("https://yomudoom.example/privacy", { headers: { accept: "text/html", host: "yomudoom.example", "x-forwarded-proto": "https" } }), { ASSETS: assets }, context);
  assert.equal(response.status, 200);
  const html = await response.text();
  assert.match(html, /Política de Privacidade/);
  assert.match(html, /Métricas deste site/);
  assert.doesNotMatch(html, /property="og:image"[^>]*og\.png/);
  const source = await readFile(new URL("../app/privacy/PrivacyPage.tsx", import.meta.url), "utf8");
  assert.match(source, /Privacy Policy/);
  assert.match(source, /Website metrics/);
});

test("accepts only aggregate metric events", async () => {
  const route = await readFile(new URL("../app/api/metrics/route.ts", import.meta.url), "utf8");
  assert.match(route, /new Set\(\["page_view", "download_click"\]\)/);
  assert.match(route, /new Set\(\["pt-BR", "en"\]\)/);
  assert.match(route, /ON CONFLICT\(day, event, language\) DO UPDATE/);
  assert.match(route, /origin !== url\.origin/);
  assert.doesNotMatch(route, /user-agent|referrer|cookie/i);
});

test("metrics worker keeps the endpoint write-only and origin restricted", async () => {
  const source = await readFile(new URL("../metrics-worker/src/index.ts", import.meta.url), "utf8");
  assert.match(source, /url\.pathname !== "\/metrics"/);
  assert.match(source, /origin !== env\.ALLOWED_ORIGIN/);
  assert.match(source, /daily_metrics/);
  assert.doesNotMatch(source, /SELECT/i);
});

test("connects the official site to the GitHub repository in links and metadata", async () => {
  const worker = await loadWorker();
  const response = await worker.fetch(new Request("https://yomudoom.example/", { headers: { accept: "text/html", host: "yomudoom.example", "x-forwarded-proto": "https" } }), { ASSETS: assets }, context);
  assert.equal(response.status, 200);
  const html = await response.text();

  assert.match(html, /rel="canonical" href="https:\/\/yomudoom\.github\.io\/YomuDoom\/"/);
  assert.match(html, /property="og:url" content="https:\/\/yomudoom\.github\.io\/YomuDoom\/"/);
  assert.match(html, /YomuDoom no GitHub — código-fonte/);
  assert.match(html, /Repositório oficial no GitHub/);
  assert.match(html, /"@type":"SoftwareApplication"/);
  assert.match(html, /"codeRepository":"https:\/\/github\.com\/YomuDoom\/YomuDoom"/);
  assert.match(html, /"downloadUrl":"https:\/\/github\.com\/YomuDoom\/YomuDoom\/releases\/latest"/);
  assert.match(html, /"sameAs":\["https:\/\/github\.com\/YomuDoom\/YomuDoom"\]/);
});
