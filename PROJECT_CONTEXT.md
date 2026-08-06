# Project Context

## Workspace
- Outer workspace: `C:\Users\marco\OneDrive\Área de Trabalho\Opencode\Yomudoom`
- Main app repo: `mihon-fork/`
- Work in `mihon-fork/` for app changes, builds, and APK sending.

## Goal
- Fork Mihon for personal use.
- Keep a fast flow to build APKs and send them to my brother on WhatsApp.
- Prefer small, correct changes and verify with a build or install when possible.

## Current Notes
- Two package variants were seen before: `app.mihon` and `app.mihon.dev`; the brother's current package is `app.mihon.dev`.
- `adb` is not directly in PATH; use `D:\AndroidSDK\platform-tools\adb.exe`.
- The brother's device is `25078PC3EG`, Android 15 / SDK 35, serial previously seen as `6XL7PBCMDQOZNB49`.
- `android.permission.QUERY_ALL_PACKAGES` is already in the manifest.
- `MangaBottomActionMenu` has a deliberate 1s delay for long-press confirmation and is not the nav lag issue.
- The brother's app data contains about 104 private `.ext` extensions; startup previously took about 20 seconds in Logcat and felt close to one minute in the UI.
- The device Logcat showed repeated `ChildFirstPathClassLoader` verification warnings, but no current `Extension load error` or `Failed to load extensions` for `app.mihon.dev`.

## Recent Code Work
- Global library play opens the last read chapter.
- Home tab switching was made more responsive.
- Browse sources now keeps its original live flow so it can show initial sources and update as extensions finish loading.
- Extension language seeding now runs at startup and on extension install/update.
- Startup splash waits for extension initialization with a timeout fallback.
- `AndroidSourceManager` now publishes persisted stub sources instead of discarding the updated map.
- Cached `StubSource` metadata now appears in the source list while extensions are loading; live sources replace matching stubs as they become available.
- Cached source items are marked as stubs and cannot be opened until their extension is loaded.
- `SourceRepositoryImpl` now preserves the stub flag for the source UI.
- `ExtensionLoader.loadExtensions` now accepts `onResult` and publishes/logs each result as it completes.
- `ExtensionManager` updates installed/untrusted extension maps atomically per result, so sources can appear progressively instead of waiting for all extensions.
- Per-extension Logcat entries now include loaded source counts, untrusted packages, and failed package names.
- A final Logcat summary now reports installed, untrusted, failed extension counts and total sources.

## WhatsApp APK Helper
- Script: `send_apk_whatsapp.py`
- Legacy compiled helper: `../__pycache__/send_apk_whatsapp.cpython-312.pyc`
- Default target: `+55 11 95917-8959`
- Behavior: pick the newest universal APK under `app/build/outputs/apk`, build a short patch note from git diff, paste the APK into the WhatsApp message box via clipboard file data, then send after confirmation.
- Run `python send_apk_whatsapp.py --dry-run` first.

## Useful Commands
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:assembleDebug`
- `./gradlew :app:archiveDebugUniversalApk`
- `./gradlew :app:installDebug`
- `python send_apk_whatsapp.py --dry-run`
- `python send_apk_whatsapp.py`

## Open Items
- The phone was intentionally disconnected; do not install through ADB for this iteration.
- Send the latest universal debug APK manually and test a cold start on the brother's phone.
- Latest successful build: `app/build/outputs/apk/debug/app-universal-debug.apk` (`0.20.1`, versionCode `26`).
- Versioned manual-send copy: `apks/0.20.1/YomuDoom-v0.20.1-dev-7801-313220cf7013-universal.apk` (generated 2026-08-06 16:16).
- Confirm installed extensions are shown as installed immediately after startup.
- Compare launch/source timing against the user's device, ideally with the same extension count.
- Keep this file updated when a new fix, script, or release flow is added.

## Versioning And Rollback
- Checkpoint commit before cached sources: `3829cf77facc`.
- Checkpoint tag: `checkpoint-before-cached-sources-2026-08-06`.
- Current app version: `0.20.1`, `versionCode=26`.
- Current debug identity: `app.mihon.dev`, `0.20.1-7801`, commit SHA `313220cf7013`.
- Keep APK filenames with version, commit count, and SHA; do not overwrite the only copy with `YomuDoom.apk`.
- Store local APK binaries under `apks/<version>/`; `apks/README.md` tracks their metadata and `.gitignore` keeps large binaries out of Git.
- Use `:app:archiveDebugUniversalApk` for future versioned APK generation; it refuses to overwrite an existing filename.
- If this feature is bad, restore the checkpoint code and publish it with a new higher `versionCode`; Android normally rejects downgrades over an installed newer version.

## Session Memory Rule
- On session compaction, read this file before inspecting or changing code.
- Treat this file as the live project memory: preserve completed work, current evidence, blockers, and next steps here.
- Update it whenever a fix, test result, device fact, APK, or decision changes the work state.
