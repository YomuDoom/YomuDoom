import type { Metadata } from "next";
import PrivacyPage from "./PrivacyPage";

export const dynamic = "force-static";

const title = "Política de Privacidade — YomuDoom";
const description = "Política de privacidade do aplicativo e do site oficial do YomuDoom.";

export const metadata: Metadata = {
  title,
  description,
  openGraph: { title, description, images: [] },
  twitter: { title, description, images: [] },
};

export default function Page() { return <PrivacyPage />; }
