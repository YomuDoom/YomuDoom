import { access, copyFile, cp, mkdir, rm, writeFile } from "node:fs/promises";

const clientOutput = new URL("../dist/client/", import.meta.url);
const privacyDirectory = new URL("privacy/", clientOutput);
const rootNextAssets = new URL("_next/", clientOutput);

await mkdir(privacyDirectory, { recursive: true });
await copyFile(
  new URL("privacy.html", clientOutput),
  new URL("index.html", privacyDirectory),
);
const assetCandidates = ["YomuDoom/_next/", "yomudoom/_next/"];
let nestedNextAssets;

for (const candidate of assetCandidates) {
  const candidateUrl = new URL(candidate, clientOutput);
  try {
    await access(candidateUrl);
    nestedNextAssets = candidateUrl;
    break;
  } catch {
    // Vinext preserves the asset prefix case on Linux but not on Windows.
  }
}

if (nestedNextAssets) {
  await cp(nestedNextAssets, rootNextAssets, { recursive: true });
  await rm(nestedNextAssets, { recursive: true, force: true });
} else {
  await access(rootNextAssets);
}
await writeFile(new URL(".nojekyll", clientOutput), "");
