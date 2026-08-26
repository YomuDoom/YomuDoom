import type { Metadata } from "next";
import { headers } from "next/headers";
import "./globals.css";

const SITE_BASE_PATH = process.env.NEXT_PUBLIC_BASE_PATH ?? "";

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
    icons: { icon: `${SITE_BASE_PATH}/yomudoom/icon.png`, shortcut: `${SITE_BASE_PATH}/yomudoom/icon.png` },
    openGraph: { title, description, type: "website", images: [{ url: image, width: 1731, height: 909, alt: "YomuDoom — um leitor de fã para fã" }] },
    twitter: { card: "summary_large_image", title, description, images: [image] },
  };
}

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="pt-BR"><body>{children}</body></html>;
}
