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

test("includes complete SEO metadata, canonical, Open Graph, Twitter and JSON-LD structured data", async () => {
  const worker = await loadWorker();
  const response = await worker.fetch(new Request("https://yomudoom.example/", { headers: { accept: "text/html", host: "yomudoom.example", "x-forwarded-proto": "https" } }), { ASSETS: assets }, context);
  assert.equal(response.status, 200);
  const html = await response.text();

  // Official title and description
  assert.match(html, /<title>YomuDoom — Leitor de mangás e webtoons para Android \| Site oficial<\/title>/);
  assert.match(html, /<meta name="description" content="Site oficial do YomuDoom, leitor gratuito e open source para Android baseado no Mihon\. Busca consolidada, recomendações, obras semelhantes e autoscroll\."\s*\/?>/);

  // Canonical tag
  assert.match(html, /<link rel="canonical" href="https:\/\/yomudoom\.github\.io\/YomuDoom\/"\s*\/?>/);
  assert.doesNotMatch(html, /href="https?:\/\/localhost[^"]*"/);
  assert.doesNotMatch(html, /rel="canonical"[^>]*localhost/);

  // Open Graph metadata
  assert.match(html, /property="og:title" content="YomuDoom — Leitor de mangás e webtoons para Android \| Site oficial"/);
  assert.match(html, /property="og:url" content="https:\/\/yomudoom\.github\.io\/YomuDoom\/"/);
  assert.match(html, /property="og:site_name" content="YomuDoom"/);
  assert.match(html, /property="og:locale" content="pt_BR"/);
  assert.match(html, /property="og:type" content="website"/);
  assert.match(html, /property="og:image" content="https:\/\/yomudoom\.github\.io\/YomuDoom\/og\.png"/);

  // Twitter card metadata
  assert.match(html, /name="twitter:card" content="summary_large_image"/);
  assert.match(html, /name="twitter:title" content="YomuDoom — Leitor de mangás e webtoons para Android \| Site oficial"/);
  assert.match(html, /name="twitter:image" content="https:\/\/yomudoom\.github\.io\/YomuDoom\/og\.png"/);

  // JSON-LD structured data with WebSite and SoftwareApplication
  assert.match(html, /<script type="application\/ld\+json">/);
  assert.match(html, /"@type":"WebSite"/);
  assert.match(html, /"@type":"SoftwareApplication"/);
  assert.match(html, /"operatingSystem":"Android 8\.0 ou superior"/);
  assert.match(html, /"downloadUrl":"https:\/\/github\.com\/YomuDoom\/YomuDoom\/releases\/latest"/);
  assert.match(html, /"image":"https:\/\/yomudoom\.github\.io\/YomuDoom\/yomudoom\/icon\.png"/);
  assert.match(html, /"price":"0"/);
  assert.match(html, /"priceCurrency":"BRL"/);
  assert.match(html, /"isAccessibleForFree":true/);
  assert.match(html, /"license":"https:\/\/www\.apache\.org\/licenses\/LICENSE-2\.0"/);
  assert.match(html, /"sameAs":\["https:\/\/github\.com\/YomuDoom\/YomuDoom"\]/);

  // Official links
  assert.match(html, /href="https:\/\/github\.com\/YomuDoom\/YomuDoom"/);
  assert.match(html, /href="https:\/\/github\.com\/YomuDoom\/YomuDoom\/releases\/latest"/);
});

test("privacy page includes absolute canonical without localhost", async () => {
  const worker = await loadWorker();
  const response = await worker.fetch(new Request("https://yomudoom.example/privacy", { headers: { accept: "text/html", host: "yomudoom.example", "x-forwarded-proto": "https" } }), { ASSETS: assets }, context);
  assert.equal(response.status, 200);
  const html = await response.text();

  assert.match(html, /<link rel="canonical" href="https:\/\/yomudoom\.github\.io\/YomuDoom\/privacy\/"\s*\/?>/);
  assert.match(html, /property="og:url" content="https:\/\/yomudoom\.github\.io\/YomuDoom\/privacy\/"/);
  assert.doesNotMatch(html, /localhost/);
});

test("public directory includes robots.txt and sitemap.xml with canonical URLs", async () => {
  const robots = await readFile(new URL("../public/robots.txt", import.meta.url), "utf8");
  assert.match(robots, /User-agent: \*/);
  assert.match(robots, /Allow: \//);
  assert.match(robots, /Sitemap: https:\/\/yomudoom\.github\.io\/YomuDoom\/sitemap\.xml/);

  const sitemap = await readFile(new URL("../public/sitemap.xml", import.meta.url), "utf8");
  assert.match(sitemap, /<loc>https:\/\/yomudoom\.github\.io\/YomuDoom\/<\/loc>/);
  assert.match(sitemap, /<loc>https:\/\/yomudoom\.github\.io\/YomuDoom\/privacy\/<\/loc>/);
});

