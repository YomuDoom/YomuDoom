import { copyFile, mkdir, writeFile } from "node:fs/promises";

const clientOutput = new URL("../dist/client/", import.meta.url);
const privacyDirectory = new URL("privacy/", clientOutput);

await mkdir(privacyDirectory, { recursive: true });
await copyFile(
  new URL("privacy.html", clientOutput),
  new URL("index.html", privacyDirectory),
);
await writeFile(new URL(".nojekyll", clientOutput), "");
