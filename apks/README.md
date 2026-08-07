# YomuDoom APKs

APK binaries are kept locally by version and are intentionally ignored by Git. The source commit, tag and release description remain versioned here.

## Identity

- Display name: `YomuDoom`
- Technical package: `app.mihon.dev`
- The technical package remains unchanged to preserve app data and extension compatibility.

## Release History

| Version | Code | Variant | Changes | Commit | Tag | APK |
| --- | ---: | --- | --- | --- | --- | --- |
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
