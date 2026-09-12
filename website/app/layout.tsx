import type { Metadata } from "next";
import { headers } from "next/headers";
import "./globals.css";

const SITE_BASE_PATH = process.env.NEXT_PUBLIC_BASE_PATH ?? "";
const CANONICAL_URL = "https://yomudoom.github.io/YomuDoom/";
const REPOSITORY_URL = "https://github.com/YomuDoom/YomuDoom";
const RELEASE_URL = "https://github.com/YomuDoom/YomuDoom/releases/latest";

const jsonLd = {
  "@context": "https://schema.org",
  "@graph": [
    {
      "@type": "WebSite",
      "@id": `${CANONICAL_URL}#website`,
      url: CANONICAL_URL,
      name: "YomuDoom",
      description: "Site oficial do YomuDoom, leitor gratuito e open source para Android baseado no Mihon.",
      inLanguage: "pt-BR",
      sameAs: [REPOSITORY_URL],
    },
    {
      "@type": "SoftwareApplication",
      "@id": `${CANONICAL_URL}#software`,
      name: "YomuDoom",
      applicationCategory: "MultimediaApplication",
      operatingSystem: "Android 8.0 ou superior",
      url: CANONICAL_URL,
      codeRepository: REPOSITORY_URL,
      downloadUrl: RELEASE_URL,
      image: `${CANONICAL_URL}yomudoom/icon.png`,
      license: "https://www.apache.org/licenses/LICENSE-2.0",
      isAccessibleForFree: true,
      description: "Leitor gratuito e open source para Android baseado no Mihon, com busca consolidada, recomendações, obras parecidas e autoscroll.",
      sameAs: [REPOSITORY_URL],
      offers: {
        "@type": "Offer",
        price: "0",
        priceCurrency: "BRL",
      },
    },
  ],
};

export async function generateMetadata(): Promise<Metadata> {
  const incoming = await headers();
  const host = incoming.get("x-forwarded-host") ?? incoming.get("host") ?? "localhost:3000";
  const protocol = incoming.get("x-forwarded-proto") ?? (host.startsWith("localhost") ? "http" : "https");
  const origin = process.env.NEXT_PUBLIC_SITE_ORIGIN ?? `${protocol}://${host}`;
  const title = "YomuDoom — um leitor de fã para fã";
  const description = "Projeto aberto baseado no Mihon, com busca mais prática, recomendações, obras parecidas e autoscroll para Android.";
  const image = new URL(`${SITE_BASE_PATH}/og.png`, origin).toString();

  return {
    metadataBase: new URL(origin),
    title,
    description,
    alternates: { canonical: CANONICAL_URL },
    icons: { icon: `${SITE_BASE_PATH}/yomudoom/icon.png`, shortcut: `${SITE_BASE_PATH}/yomudoom/icon.png` },
    openGraph: { title, description, url: CANONICAL_URL, siteName: "YomuDoom", locale: "pt_BR", type: "website", images: [{ url: image, width: 1731, height: 909, alt: "YomuDoom — um leitor de fã para fã" }] },
    twitter: { card: "summary_large_image", title, description, images: [image] },
  };
}

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR">
      <head>
        <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
      </head>
      <body>{children}</body>
    </html>
  );
}
