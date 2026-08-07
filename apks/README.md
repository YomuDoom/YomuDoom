# YomuDoom APKs

APK binaries are kept locally by version and are intentionally ignored by Git. The source commit, tag and release description remain versioned here.

## Identity

- Display name: `YomuDoom`
- Current debug package: `app.yomudoom.dev`
- Current release package: `app.yomudoom`
- The package change creates a separate Android app; migrate data with a backup before removing the legacy app.

## Release History

| Version | Code | Variant | Changes | Commit | Tag | APK |
| --- | ---: | --- | --- | --- | --- | --- |
| `0.20.16` | `41` | YomuDoom Dev (`app.yomudoom.dev`) | Changes the Android application ID to the YomuDoom package; backup and restore are required from the legacy app | `1504954a2048` (working tree base) | `v0.20.16` (private pre-release) | [`APK`](0.20.16/YomuDoom-v0.20.16-dev-7819-1504954a2048-universal.apk) |
| `0.20.15` | `40` | YomuDoom Dev (`app.mihon.dev`) | Reduces the lower-right continue-reading button to 75dp | `1504954a2048` (working tree base) | pending | [`APK`](0.20.15/YomuDoom-v0.20.15-dev-7819-1504954a2048-universal.apk) |
| `0.20.14` | `39` | YomuDoom Dev (`app.mihon.dev`) | Enlarges the lower-right continue-reading button to 2.5x its previous size | `1504954a2048` (working tree base) | pending | [`APK`](0.20.14/YomuDoom-v0.20.14-dev-7819-1504954a2048-universal.apk) |
| `0.20.13` | `38` | YomuDoom Dev (`app.mihon.dev`) | Moves the continue-reading action to a theme-colored lower-right floating button | `1504954a2048` (working tree base) | pending | [`APK`](0.20.13/YomuDoom-v0.20.13-dev-7819-1504954a2048-universal.apk) |
| `0.20.12` | `37` | YomuDoom Dev (`app.mihon.dev`) | Removes AI wording from the Recommendations onboarding text | `1504954a2048` (working tree base) | pending | [`APK`](0.20.12/YomuDoom-v0.20.12-dev-7819-1504954a2048-universal.apk) |
| `0.20.11` | `36` | YomuDoom Dev (`app.mihon.dev`) | Fixes the onboarding crash caused by nesting a LazyColumn inside the InfoScreen scroll container | `1504954a2048` (working tree base) | pending | [`APK`](0.20.11/YomuDoom-v0.20.11-dev-7819-1504954a2048-universal.apk) |
| `0.20.10` | `35` | YomuDoom Dev (`app.mihon.dev`) | Adds a scrollable YomuDoom features guide to the first-run onboarding | `1504954a2048` (working tree base) | pending | [`APK`](0.20.10/YomuDoom-v0.20.10-dev-7819-1504954a2048-universal.apk) |
| `0.20.9` | `34` | YomuDoom Dev (`app.mihon.dev`) | Adapts library chapter order to the last read position; adds configurable Webtoon auto-scroll | `1504954a2048` (working tree base) | pending | [`APK`](0.20.9/YomuDoom-v0.20.9-dev-7819-1504954a2048-universal.apk) |
| `0.20.8` | `33` | YomuDoom Dev (`app.mihon.dev`) | Adds the direct Recommendations tab with local history-based ranking; warns before the slow Similar works search | `1504954a2048` (working tree base) | pending | [`APK`](0.20.8/YomuDoom-v0.20.8-dev-7819-1504954a2048-universal.apk) |
| `0.20.7` | `32` | YomuDoom Dev (`app.mihon.dev`) | Adds `Similar works` to the manga menu; searches enabled sources and ranks candidates locally by metadata | `1504954a2048` (working tree base) | pending | [`APK`](0.20.7/YomuDoom-v0.20.7-dev-7819-1504954a2048-universal.apk) |
| `0.20.6` | `31` | YomuDoom Dev (`app.mihon.dev`) | Faster splash; background extension loading; throttled progress updates | `0f09b7c479eb` | `v0.20.6` | [`APK`](0.20.6/YomuDoom-v0.20.6-dev-7817-0f09b7c479eb-universal.apk) |
| `0.20.5` | `30` | YomuDoom Dev (`app.mihon.dev`) | Consolidated search prioritizes title similarity, then chapter count; source language in menu | `d2eb193dbc00` | `v0.20.5` | [`APK`](0.20.5/YomuDoom-v0.20.5-dev-7814-d2eb193dbc00-universal.apk) |
| `0.20.4` | `29` | YomuDoom Dev (`app.mihon.dev`) | Source-by-source legacy search and source switching in consolidated search | `ef9053819a2a` | `v0.20.4` | [`APK`](0.20.4/YomuDoom-v0.20.4-dev-7812-ef9053819a2a-universal.apk) |
| `0.20.3` | `28` | YomuDoom Dev (`app.mihon.dev`) | Groups the same work and selects the source with the most chapters | `5c566c6fb77d` | `v0.20.3` | [`APK`](0.20.3/YomuDoom-v0.20.3-dev-7810-5c566c6fb77d-universal.apk) |
| `0.20.2` | `27` | YomuDoom Dev (`app.mihon.dev`) | Three-dot menu for Legacy and Consolidated search modes | `1c8b9e2c90fe` | `v0.20.2` | [`APK`](0.20.2/YomuDoom-v0.20.2-dev-7808-1c8b9e2c90fe-universal.apk) |
| `0.20.1` | `26` | YomuDoom Dev (`app.mihon.dev`) | Persisted source cache and progressive source loading | `9b09c4c79c7b` | `v0.20.1` | [`APK`](0.20.1/YomuDoom-v0.20.1-dev-7803-9b09c4c79c7b-universal.apk) |

## Naming

Use `YomuDoom-v<version>-<variant>-<commit-count>-<sha>-universal.apk`. Never overwrite an existing APK; create a new version or build entry.

The version number maps to the release description above and to the matching Git tag. For rollback, use the tag and APK from the desired row.

## Archive Command

Run `./gradlew :app:archiveDebugUniversalApk` after changing the code. The task creates the version directory and refuses to overwrite an existing APK.

`./gradlew :app:installDebug` also archives the APK automatically before installing it on the connected device.
