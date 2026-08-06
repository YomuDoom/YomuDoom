# APK Archive

APK binaries are kept locally by version and are intentionally ignored by Git. The source commit and build metadata remain versioned here and in Git tags.

## Builds

| Version | Version code | Variant | Commit | Tag | APK |
| --- | ---: | --- | --- | --- | --- |
| `0.20.3` | `28` | `app.mihon.dev` / debug | `5c566c6fb77d` | `v0.20.3` | [`YomuDoom-v0.20.3-dev-7810-5c566c6fb77d-universal.apk`](0.20.3/YomuDoom-v0.20.3-dev-7810-5c566c6fb77d-universal.apk) |
| `0.20.2` | `27` | `app.mihon.dev` / debug | `1c8b9e2c90fe` | `v0.20.2` | [`YomuDoom-v0.20.2-dev-7808-1c8b9e2c90fe-universal.apk`](0.20.2/YomuDoom-v0.20.2-dev-7808-1c8b9e2c90fe-universal.apk) |
| `0.20.1` | `26` | `app.mihon.dev` / debug | `9b09c4c79c7b` | `v0.20.1` | [`YomuDoom-v0.20.1-dev-7803-9b09c4c79c7b-universal.apk`](0.20.1/YomuDoom-v0.20.1-dev-7803-9b09c4c79c7b-universal.apk) |

## Naming

Use `YomuDoom-v<version>-<variant>-<commit-count>-<sha>-universal.apk`. Never overwrite an existing APK; create a new version or build entry.

## Archive Command

Run `./gradlew :app:archiveDebugUniversalApk` after changing the code. The task creates the version directory and refuses to overwrite an existing APK.

`./gradlew :app:installDebug` also archives the APK automatically before installing it on the connected device.
