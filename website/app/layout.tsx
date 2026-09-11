import type { Metadata } from "next";
import { headers } from "next/headers";
import "./globals.css";

const SITE_BASE_PATH = process.env.NEXT_PUBLIC_BASE_PATH ?? "/YomuDoom";
const CANONICAL_URL = "https://yomudoom.github.io/YomuDoom/";

const jsonLd = {
  "@context": "https://schema.org",
  "@graph": [
    {
      "@type": "WebSite",
      "@id": "https://yomudoom.github.io/YomuDoom/#website",
      "url": CANONICAL_URL,
      "name": "YomuDoom",
      "description": "Site oficial do YomuDoom, leitor gratuito e open source para Android baseado no Mihon.",
      "inLanguage": "pt-BR",
      "sameAs": ["https://github.com/YomuDoom/YomuDoom"],
    },
    {
      "@type": "SoftwareApplication",
      "@id": "https://yomudoom.github.io/YomuDoom/#software",
      "name": "YomuDoom",
      "applicationCategory": "Leitor de quadrinhos e mangás",
      "operatingSystem": "Android 8.0 ou superior",
      "url": CANONICAL_URL,
      "downloadUrl": "https://github.com/YomuDoom/YomuDoom/releases/latest",
      "image": "https://yomudoom.github.io/YomuDoom/yomudoom/icon.png",
      "offers": {
        "@type": "Offer",
        "price": "0",
        "priceCurrency": "BRL",
      },
      "license": "https://www.apache.org/licenses/LICENSE-2.0",
      "isAccessibleForFree": true,
      "description": "Leitor gratuito e open source para Android baseado no Mihon, com busca consolidada, recomendações, obras semelhantes e autoscroll.",
      "sameAs": ["https://github.com/YomuDoom/YomuDoom"],
    },
  ],
};

export async function generateMetadata(): Promise<Metadata> {
  const incoming = await headers().catch(() => null);
  const host = incoming?.get("x-forwarded-host") ?? incoming?.get("host");
  const protocol = incoming?.get("x-forwarded-proto") ?? (host && !host.startsWith("localhost") ? "https" : "https");
  const origin = process.env.NEXT_PUBLIC_SITE_ORIGIN ?? (host && !host.startsWith("localhost") ? `${protocol}://${host}` : "https://yomudoom.github.io");
  const title = "YomuDoom — Leitor de mangás e webtoons para Android | Site oficial";
  const description = "Site oficial do YomuDoom, leitor gratuito e open source para Android baseado no Mihon. Busca consolidada, recomendações, obras semelhantes e autoscroll.";
  const image = new URL(`${SITE_BASE_PATH}/og.png`, origin).toString();

  return {
    metadataBase: new URL(origin),
    title,
    description,
    alternates: {
      canonical: CANONICAL_URL,
    },
    icons: {
      icon: `${SITE_BASE_PATH}/yomudoom/icon.png`,
      shortcut: `${SITE_BASE_PATH}/yomudoom/icon.png`,
    },
    openGraph: {
      title,
      description,
      url: CANONICAL_URL,
      siteName: "YomuDoom",
      locale: "pt_BR",
      type: "website",
      images: [{ url: image, width: 1731, height: 909, alt: "YomuDoom — Leitor de mangás e webtoons para Android | Site oficial" }],
    },
    twitter: { card: "summary_large_image", title, description, images: [image] },
  };
}

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR">
      <head>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      </head>
      <body>{children}</body>
    </html>
  );
}
