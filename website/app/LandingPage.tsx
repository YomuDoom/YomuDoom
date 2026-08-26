"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { type Language, useLanguage } from "./useLanguage";

const DOWNLOAD_URL = "https://github.com/YomuDoom/YomuDoom/releases/latest";
const REPOSITORY_URL = "https://github.com/YomuDoom/YomuDoom";
const ISSUES_URL = "https://github.com/YomuDoom/YomuDoom/issues";
const MIHON_URL = "https://mihon.app/";
const STRIPE_URL = "https://donate.stripe.com/9B6eVc41KdyJ3Qg4yN0Fi00";
const PIX_KEY = "46da2cde-87ae-4a33-bc58-be969767dfa9";
const SITE_BASE_PATH = process.env.NEXT_PUBLIC_BASE_PATH ?? "";
const METRICS_URL = process.env.NEXT_PUBLIC_METRICS_URL ?? "";
const PRIVACY_PATH = SITE_BASE_PATH ? "/privacy.html" : "/privacy";

function sitePath(path: string) {
  return `${SITE_BASE_PATH}${path}`;
}

function sendMetric(event: "page_view" | "download_click", language: Language) {
  if (!METRICS_URL || typeof navigator === "undefined") return;
  const body = JSON.stringify({ event, language });
  try {
    if (navigator.sendBeacon) {
      navigator.sendBeacon(METRICS_URL, new Blob([body], { type: "application/json" }));
      return;
    }
    void fetch(METRICS_URL, { method: "POST", headers: { "content-type": "application/json" }, body, keepalive: true, mode: "cors" }).catch(() => undefined);
  } catch {
    // Metrics are best-effort and must never affect navigation.
  }
}

const copy = {
  "pt-BR": {
    nav: ["O que mudou", "Download", "Privacidade"],
    eyebrow: "YomuDoom para Android",
    title: "Um leitor feito para o meu jeito de ler.",
    intro: "O YomuDoom começou como uma modificação do Mihon para incluir algumas coisas que eu sentia falta no uso diário: procurar uma obra, descobrir outra parecida e voltar para onde eu tinha parado.",
    download: "Baixar o YomuDoom",
    code: "Ver o projeto no GitHub",
    releaseNote: "v0.21.1 · Android 8 ou mais recente · versão oficial no GitHub",
    plainFacts: "O aplicativo começa vazio. Ele não hospeda nem distribui obras, e a versão oficial não tem anúncios.",
    originTitle: "Por que eu fiz esta versão",
    originText: "Eu mantive a base do Mihon e mexi principalmente no caminho entre abrir o aplicativo e começar a ler. A ideia não é substituir o projeto original, mas deixar algumas tarefas mais diretas para quem prefere usar o leitor desse jeito.",
    changesTitle: "O que eu mudei",
    changesIntro: "A principal mudança está na busca consolidada: os resultados das configurações que você usa ficam juntos e podem ser ordenados pela opção com mais capítulos disponíveis. Também acrescentei recomendações, obras parecidas e autoscroll, mantendo o restante familiar para quem já conhece o Mihon.",
    searchTitle: "Busca consolidada, na prática",
    searchText: "Ao procurar uma obra, o YomuDoom reúne as opções encontradas nas configurações que você usa. A lista mostra quantos capítulos cada uma tem e deixa a opção com mais capítulos em destaque, para você decidir com menos tentativa e erro.",
    searchAlt: "Busca consolidada do YomuDoom mostrando fontes ordenadas pela quantidade de capítulos",
    changes: [
      ["Recomendações", "A biblioteca e o histórico ajudam a encontrar novas obras com base no que você já acompanha."],
      ["Obras parecidas", "Na página de uma obra, você pode encontrar outras opções semelhantes entre as configurações que usa."],
      ["Voltar para a leitura", "A retomada reúne o último capítulo acessado e leva você de volta ao ponto em que parou."],
    ],
    autoscrollTitle: "Também tem autoscroll",
    autoscrollText: "No modo Webtoon, a página pode avançar sozinha na velocidade que você escolher. Um toque interrompe o movimento.",
    inheritedTitle: "O que continua igual ao Mihon",
    inheritedText: "Downloads, histórico, backup e a retomada básica de leitura já faziam parte do Mihon. Eles continuam no YomuDoom; as mudanças desta versão estão principalmente na busca, nas recomendações, nas obras parecidas e no autoscroll.",
    downloadTitle: "Download",
    downloadText: "A versão oficial fica no GitHub. Lá estão o APK, o checksum, a impressão digital da assinatura, o código-fonte e o histórico de mudanças.",
    releaseFacts: [["Versão", "0.21.1"], ["Android", "8 ou mais recente"], ["Licença", "Apache 2.0"]],
    openRelease: "Abrir a versão no GitHub",
    openIssues: "Relatar um problema",
    upstreamTitle: "Um fork, com atribuição clara",
    upstreamText: "O YomuDoom é baseado no Mihon e preserva a licença, os direitos autorais e as atribuições do projeto original. As mudanças desta versão ficam públicas no repositório.",
    upstreamLink: "Conhecer o Mihon",
    helpText: "Se encontrar algum problema, fale comigo pelo Reddit ou abra uma issue no GitHub.",
    supportTitle: "Apoio é opcional",
    supportText: "Se o YomuDoom é útil para você, é possível apoiar o projeto. O apoio não libera funções ou benefícios.",
    stripe: "Apoiar pelo Stripe",
    pix: "Chave Pix",
    copyPix: "Copiar chave",
    copied: "Chave copiada",
    privacy: "Política de Privacidade",
    source: "Código-fonte",
    issues: "Issues",
    footer: "Mantido por uma pessoa · Software livre baseado no Mihon.",
    imageAlts: ["Biblioteca do YomuDoom com obras fictícias", "Recomendações no YomuDoom com obras fictícias", "Tela de obras parecidas do YomuDoom", "Retomada da leitura no YomuDoom"],
  },
  en: {
    nav: ["What changed", "Download", "Privacy"],
    eyebrow: "YomuDoom for Android",
    title: "A reader shaped around the way I read.",
    intro: "YomuDoom started as a Mihon modification to fill a few gaps I noticed in everyday use: finding a work, discovering something similar, and getting back to where I stopped.",
    download: "Download YomuDoom",
    code: "View the project on GitHub",
    releaseNote: "v0.21.1 · Android 8 or newer · official release on GitHub",
    plainFacts: "The app starts empty. It does not host or distribute works, and the official version has no ads.",
    originTitle: "Why I made this version",
    originText: "I kept Mihon's foundation and focused on the path between opening the app and getting back to reading. The goal is not to replace the original project, but to make a few everyday tasks more direct.",
    changesTitle: "What I changed",
    changesIntro: "The main change is consolidated search: results from the configurations you use appear together and can be sorted by the option with the most chapters available. I also added recommendations, similar works, and auto-scroll while keeping the rest familiar if you already know Mihon.",
    searchTitle: "Consolidated search, in practice",
    searchText: "When you look for a work, YomuDoom brings together the options found in the configurations you use. The list shows how many chapters each one has and puts the option with the most chapters first, so there is less trial and error.",
    searchAlt: "YomuDoom consolidated search showing sources ordered by chapter count",
    changes: [
      ["Recommendations", "Your library and history help you find new works based on what you already follow."],
      ["Similar works", "On a work’s page, you can find other options that are similar across the configurations you use."],
      ["Get back to reading", "The resume area gathers the last chapter you opened and takes you back to where you stopped."],
    ],
    autoscrollTitle: "There’s auto-scroll too",
    autoscrollText: "In Webtoon mode, the page can move at the speed you choose. Touch the screen to stop it.",
    inheritedTitle: "What stays the same as Mihon",
    inheritedText: "Downloads, history, backup, and basic reading resume were already part of Mihon. They remain in YomuDoom; this version’s changes are mainly in search, recommendations, similar works, and auto-scroll.",
    downloadTitle: "Download",
    downloadText: "The official version is on GitHub. The release page includes the APK, checksum, signing fingerprint, source code, and change history.",
    releaseFacts: [["Version", "0.21.1"], ["Android", "8 or newer"], ["License", "Apache 2.0"]],
    openRelease: "Open the GitHub release",
    openIssues: "Report a problem",
    upstreamTitle: "A fork, with clear attribution",
    upstreamText: "YomuDoom is based on Mihon and preserves the original project's license, copyright notices, and attribution. This version's changes are public in the repository.",
    upstreamLink: "Learn about Mihon",
    helpText: "If you find a problem, reach me on Reddit or open an issue on GitHub.",
    supportTitle: "Support is optional",
    supportText: "If YomuDoom is useful to you, you can support the project. Support does not unlock features or benefits.",
    stripe: "Support through Stripe",
    pix: "Pix key",
    copyPix: "Copy key",
    copied: "Key copied",
    privacy: "Privacy Policy",
    source: "Source code",
    issues: "Issues",
    footer: "Maintained by one person · Open-source software based on Mihon.",
    imageAlts: ["YomuDoom library with fictional works", "YomuDoom recommendations with fictional works", "Similar works screen in YomuDoom", "Resume reading in YomuDoom"],
  },
} as const;

export default function LandingPage() {
  const { language, setLanguage, ready } = useLanguage();
  const [pixCopied, setPixCopied] = useState(false);
  const text = copy[language];

  useEffect(() => {
    if (!ready || !METRICS_URL) return;
    const key = `yomudoom-view-${language}`;
    if (!sessionStorage.getItem(key)) {
      sessionStorage.setItem(key, "1");
      sendMetric("page_view", language);
    }
  }, [language, ready]);

  async function copyPix() {
    await navigator.clipboard.writeText(PIX_KEY);
    setPixCopied(true);
    window.setTimeout(() => setPixCopied(false), 2200);
  }

  const suffix = language === "en" ? "-en.png" : "-pt.png";
  const images = ["library", "recommendations", "similar", "resume"].map((name) => sitePath(`/yomudoom/${name}${suffix}`));

  return (
    <main>
      <header className="site-header">
        <a className="brand" href="#top" aria-label="YomuDoom — home">
          <Image src={sitePath("/yomudoom/icon.png")} alt="" width={42} height={42} />
          <span>YomuDoom</span>
        </a>
        <nav aria-label={language === "en" ? "Main navigation" : "Navegação principal"}>
          <a href="#changes">{text.nav[0]}</a>
          <a href="#download">{text.nav[1]}</a>
          <a href={language === "en" ? sitePath(`${PRIVACY_PATH}?lang=en`) : sitePath(PRIVACY_PATH)}>{text.nav[2]}</a>
          <div className="language-switch" aria-label={language === "en" ? "Language" : "Idioma"}>
            <button className={language === "pt-BR" ? "active" : ""} onClick={() => setLanguage("pt-BR")} type="button">PT</button>
            <span aria-hidden="true">/</span>
            <button className={language === "en" ? "active" : ""} onClick={() => setLanguage("en")} type="button">EN</button>
          </div>
        </nav>
      </header>

      <section className="hero" id="top">
        <div className="hero-copy">
          <p className="section-label">{text.eyebrow}</p>
          <h1>{text.title}</h1>
          <p className="hero-text">{text.intro}</p>
          <div className="hero-actions">
            <a className="button button-primary" href={DOWNLOAD_URL} onClick={() => sendMetric("download_click", language)}>{text.download}</a>
            <a className="text-link" href={REPOSITORY_URL}>{text.code} <span aria-hidden="true">↗</span></a>
          </div>
          <p className="hero-note">{text.releaseNote}</p>
        </div>
        <figure className="phone-stage">
          <Image className="phone-screen" src={images[0]} alt={text.imageAlts[0]} width={556} height={1236} priority />
        </figure>
      </section>

      <aside className="plain-note"><p>{text.plainFacts}</p></aside>

      <section className="origin-section">
        <h2>{text.originTitle}</h2>
        <p>{text.originText}</p>
      </section>

      <section className="changes-section" id="changes">
        <header className="content-heading">
          <h2>{text.changesTitle}</h2>
          <p>{text.changesIntro}</p>
        </header>
        <article className="search-showcase">
          <div className="search-showcase-copy">
            <h3>{text.searchTitle}</h3>
            <p>{text.searchText}</p>
          </div>
          <div className="search-showcase-images">
            <figure><Image src={sitePath("/yomudoom/search-consolidated.png")} alt={text.searchAlt} width={556} height={1236} loading="lazy" /></figure>
            <figure><Image src={sitePath("/yomudoom/search-mode.png")} alt={language === "en" ? "YomuDoom search mode selector" : "Seletor do modo de busca no YomuDoom"} width={556} height={1236} loading="lazy" /></figure>
          </div>
        </article>
        <div className="change-list">
          {text.changes.map(([title, detail], index) => (
            <article className="change-row" key={title}>
              <figure className="screen-figure">
                <Image src={images[index + 1]} alt={text.imageAlts[index + 1]} width={556} height={1236} loading="lazy" />
              </figure>
              <div className="change-copy"><h3>{title}</h3><p>{detail}</p></div>
            </article>
          ))}
        </div>
      </section>

      <section className="autoscroll-section">
        <h2>{text.autoscrollTitle}</h2>
        <p>{text.autoscrollText}</p>
      </section>

      <section className="inherited-section">
        <h2>{text.inheritedTitle}</h2>
        <p>{text.inheritedText}</p>
      </section>

      <section className="download-section" id="download">
        <div className="download-copy">
          <h2>{text.downloadTitle}</h2>
          <p>{text.downloadText}</p>
          <div className="hero-actions">
            <a className="button button-primary" href={DOWNLOAD_URL} onClick={() => sendMetric("download_click", language)}>{text.openRelease}</a>
            <a className="text-link" href={ISSUES_URL}>{text.openIssues} <span aria-hidden="true">↗</span></a>
          </div>
          <p className="help-text">{text.helpText}</p>
          <dl className="release-facts">
            {text.releaseFacts.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}
          </dl>
        </div>
      </section>

      <section className="upstream-section">
        <div><h2>{text.upstreamTitle}</h2><p>{text.upstreamText}</p></div>
        <a className="text-link" href={MIHON_URL}>{text.upstreamLink} <span aria-hidden="true">↗</span></a>
      </section>

      <footer>
        <section className="support-section">
          <div><h2>{text.supportTitle}</h2><p>{text.supportText}</p></div>
          <div className="support-actions">
            <a className="button button-secondary" href={STRIPE_URL}>{text.stripe}</a>
            <div className="pix-line"><span><small>{text.pix}</small>{PIX_KEY}</span><button type="button" onClick={copyPix}>{pixCopied ? text.copied : text.copyPix}</button></div>
          </div>
        </section>
        <div className="footer-main">
          <a className="brand" href="#top"><Image src={sitePath("/yomudoom/icon.png")} alt="" width={40} height={40} /><span>YomuDoom</span></a>
          <div className="footer-links"><a href={REPOSITORY_URL}>{text.source}</a><a href={ISSUES_URL}>{text.issues}</a><a href={language === "en" ? sitePath(`${PRIVACY_PATH}?lang=en`) : sitePath(PRIVACY_PATH)}>{text.privacy}</a></div>
        </div>
        <div className="footer-bottom"><p>{text.footer}</p></div>
      </footer>
    </main>
  );
}
