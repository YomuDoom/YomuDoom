"use client";

import { useLanguage } from "../useLanguage";
import Image from "next/image";

const SITE_BASE_PATH = process.env.NEXT_PUBLIC_BASE_PATH ?? "";
const sitePath = (path: string) => `${SITE_BASE_PATH}${path}`;

const policy = {
  "pt-BR": {
    title: "Política de Privacidade",
    effective: "Em vigor desde 17 de agosto de 2026",
    back: "Voltar ao site",
    sections: [
      ["Visão geral", [
        "O YomuDoom é um leitor Android gratuito e de código aberto distribuído oficialmente pelo GitHub. O aplicativo não inclui publicidade, analytics, telemetria nem envio automático de relatórios de falha.",
        "O projeto não exige uma conta YomuDoom e não opera servidores que armazenem sua biblioteca, histórico ou conteúdo de leitura.",
      ]],
      ["Dados armazenados no aparelho", [
        "Biblioteca, histórico, progresso de leitura, preferências, metadados, backups, downloads e arquivos locais permanecem no aparelho ou na pasta escolhida pelo usuário. Esses dados não são enviados ao projeto YomuDoom.",
        "Para excluir dados internos, use as opções de limpeza do aplicativo ou limpe os dados/desinstale o YomuDoom nas configurações do Android. Backups, downloads e outros arquivos mantidos em pastas compartilhadas devem ser removidos pelo próprio usuário.",
      ]],
      ["Serviços externos", [
        "O aplicativo acessa a Internet somente para funções solicitadas ou configuradas pelo usuário: GitHub para atualizações; lojas, extensões ou provedores configurados pelo usuário; serviços de tracking autenticados pelo usuário; e Stripe somente quando um link de apoio é aberto voluntariamente.",
        "Novas instalações não incluem lojas de extensões. Serviços externos são independentes, não são operados nem recomendados pelo YomuDoom e podem receber os dados necessários à solicitação. O tratamento segue a política de cada serviço.",
        "O Stripe abre no navegador externo. A chave Pix é apenas exibida e copiada localmente. Nenhuma contribuição libera conteúdo, recurso ou benefício digital.",
      ]],
      ["Permissões", [
        "O YomuDoom pode solicitar rede, notificações, execução de tarefas e uso de bateria em segundo plano para atualizações e downloads. Permissões para consultar, instalar, atualizar ou remover pacotes são usadas para gerenciar extensões Android configuradas pelo usuário. O armazenamento é usado para leitura local, downloads e backups.",
      ]],
      ["Métricas deste site", [
        "Esta versão estática do site pode enviar somente contagens agregadas de visitas e cliques de download para o endpoint de métricas do projeto. Não são enviados cookies, endereço IP, agente do navegador, referência ou identificadores pessoais. Se o endpoint estiver indisponível, o site continua funcionando normalmente.",
      ]],
      ["Crianças, alterações e contato", [
        "O YomuDoom não é direcionado a crianças e não coleta intencionalmente seus dados pessoais. Esta política será atualizada se o comportamento de dados mudar.",
        "Dúvidas gerais podem ser abertas no GitHub Issues. O projeto não oferece suporte a lojas, extensões ou provedores externos. Vulnerabilidades devem seguir a Política de Segurança do repositório.",
      ]],
    ],
  },
  en: {
    title: "Privacy Policy",
    effective: "Effective August 17, 2026",
    back: "Back to the website",
    sections: [
      ["Overview", [
        "YomuDoom is a free and open-source Android reader officially distributed through GitHub. The application includes no advertising, analytics, telemetry, or automatic crash reporting.",
        "The project requires no YomuDoom account and operates no servers that store your library, history, or reading content.",
      ]],
      ["Data stored on the device", [
        "Library entries, history, reading progress, preferences, metadata, backups, downloads, and local files remain on the device or in the folder selected by the user. This data is not sent to the YomuDoom project.",
        "To delete internal data, use the application's cleanup options or clear its data/uninstall YomuDoom through Android settings. Backups, downloads, and other files kept in shared folders must be removed by the user.",
      ]],
      ["External services", [
        "The app accesses the Internet only for features requested or configured by the user: GitHub for updates; stores, extensions, or providers configured by the user; tracking services authenticated by the user; and Stripe only when a support link is opened voluntarily.",
        "New installations include no extension stores. External services are independent, are not operated or recommended by YomuDoom, and may receive data required for a request. Their own policies govern this processing.",
        "Stripe opens in the external browser. The Pix key is only displayed and copied locally. Contributions unlock no content, feature, or digital benefit.",
      ]],
      ["Permissions", [
        "YomuDoom may request network access, notifications, background task execution, and battery usage for updates and downloads. Permissions to query, install, update, or remove packages manage Android extensions configured by the user. Storage access is used for local reading, downloads, and backups.",
      ]],
      ["Website metrics", [
        "This static version of the website may send only aggregate counts of visits and download clicks to the project's metrics endpoint. It does not send cookies, IP addresses, browser agents, referrers, or personal identifiers. If the endpoint is unavailable, the website continues to work normally.",
      ]],
      ["Children, changes, and contact", [
        "YomuDoom is not directed to children and does not intentionally collect their personal information. This policy will be updated if data behavior changes.",
        "General questions may be opened in GitHub Issues. The project does not support external stores, extensions, or providers. Vulnerabilities must follow the repository's Security Policy.",
      ]],
    ],
  },
} as const;

export default function PrivacyPage() {
  const { language, setLanguage } = useLanguage();
  const text = policy[language];
  return (
    <main className="privacy-shell">
      <header className="privacy-header">
        <a className="brand" href={language === "en" ? sitePath("/?lang=en") : sitePath("/")}><Image src={sitePath("/yomudoom/icon.png")} alt="" width={44} height={44} /><span>YomuDoom</span></a>
        <div className="language-switch" aria-label={language === "en" ? "Language" : "Idioma"}>
          <button className={language === "pt-BR" ? "active" : ""} onClick={() => setLanguage("pt-BR")} type="button">PT</button><span aria-hidden="true">/</span><button className={language === "en" ? "active" : ""} onClick={() => setLanguage("en")} type="button">EN</button>
        </div>
      </header>
      <article className="policy">
        <a className="back-link" href={language === "en" ? sitePath("/?lang=en") : sitePath("/")}>← {text.back}</a>
        <h1>{text.title}</h1><p className="effective-date">{text.effective}</p>
        {text.sections.map(([title, paragraphs]) => <section key={title}><h2>{title}</h2>{paragraphs.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}</section>)}
        <div className="policy-links"><a href="https://github.com/YomuDoom/YomuDoom/issues">GitHub Issues</a><a href="https://github.com/YomuDoom/YomuDoom/security/policy">Security Policy</a></div>
      </article>
    </main>
  );
}
