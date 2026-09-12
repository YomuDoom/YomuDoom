import { copyFile, cp, mkdir, rm, writeFile } from "node:fs/promises";

const clientOutput = new URL("../dist/client/", import.meta.url);
const privacyDirectory = new URL("privacy/", clientOutput);
const nestedNextAssets = new URL("yomudoom/_next/", clientOutput);
const rootNextAssets = new URL("_next/", clientOutput);

await mkdir(privacyDirectory, { recursive: true });
await copyFile(
  new URL("privacy.html", clientOutput),
  new URL("index.html", privacyDirectory),
);
await cp(nestedNextAssets, rootNextAssets, { recursive: true });
await rm(nestedNextAssets, { recursive: true, force: true });
await writeFile(new URL(".nojekyll", clientOutput), "");
