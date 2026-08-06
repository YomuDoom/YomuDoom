# APK Archive

APK binaries are kept locally by version and are intentionally ignored by Git. The source commit and build metadata remain versioned here and in Git tags.

## Builds

| Version | Version code | Variant | Commit | Tag | APK |
| --- | ---: | --- | --- | --- | --- |
| `0.20.1` | `26` | `app.mihon.dev` / debug | `313220cf7013` | `v0.20.1` | [`YomuDoom-v0.20.1-dev-7801-313220cf7013-universal.apk`](0.20.1/YomuDoom-v0.20.1-dev-7801-313220cf7013-universal.apk) |

## Naming

Use `YomuDoom-v<version>-<variant>-<commit-count>-<sha>-universal.apk`. Never overwrite an existing APK; create a new version or build entry.

## Archive Command

Run `./gradlew :app:archiveDebugUniversalApk` after changing the code. The task creates the version directory and refuses to overwrite an existing APK.
