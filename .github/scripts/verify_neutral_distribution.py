#!/usr/bin/env python3

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "app/src/main/assets/extension_stores.json"


def fail(message: str) -> None:
    raise SystemExit(f"Neutral distribution check failed: {message}")


catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
if catalog.get("stores") != []:
    fail("extension_stores.json must contain an empty stores list")

for removed_path in (
    ROOT / "app/src/playRelease",
    ROOT / "play-store",
):
    if removed_path.exists():
        fail(f"removed Google Play artifact returned: {removed_path.relative_to(ROOT)}")

manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
for token in ("add-repo", 'android:host="extension-store"'):
    if token in manifest:
        fail(f"extension-store deeplink returned to the manifest: {token}")

extension_model = (
    ROOT / "app/src/main/java/eu/kanade/tachiyomi/ui/browse/extension/ExtensionsScreenModel.kt"
).read_text(encoding="utf-8")
for token in ("recommendedInstallNamesByLanguage", "InstallMode.Recommended", "BulkActionType.Install"):
    if token in extension_model:
        fail(f"bulk or recommended extension installation returned: {token}")

public_surfaces = [
    ROOT / "README.md",
    ROOT / "PRIVACY.md",
    ROOT / "NOTICE",
    ROOT / "UPSTREAM.md",
    ROOT / "CONTRIBUTING.md",
    ROOT / "docs/privacy/index.html",
    ROOT / "app/src/main/assets/extension_stores.json",
]
public_surfaces.extend((ROOT / "i18n/src/commonMain/moko-resources").glob("*/strings.xml"))
for path in public_surfaces:
    text = path.read_text(encoding="utf-8").casefold()
    for token in (
        "keiyoushi",
        "app.yomudoom.play",
        "playrelease",
        "extensionstorescreen.addstoredeeplink",
    ):
        if token in text:
            fail(f"forbidden bundled-store or Play reference in {path.relative_to(ROOT)}: {token}")

print("Neutral distribution checks passed.")
