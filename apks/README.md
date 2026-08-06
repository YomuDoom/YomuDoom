# APK Archive

APK binaries are kept locally by version and are intentionally ignored by Git. The source commit and build metadata remain versioned here and in Git tags.

## Builds

| Version | Version code | Variant | Commit | Tag | APK |
| --- | ---: | --- | --- | --- | --- |
| `0.20.2` | `27` | `app.mihon.dev` / debug | `ac7205199405` | `v0.20.2` | [`YomuDoom-v0.20.2-dev-7805-ac7205199405-universal.apk`](0.20.2/YomuDoom-v0.20.2-dev-7805-ac7205199405-universal.apk) |
| `0.20.1` | `26` | `app.mihon.dev` / debug | `9b09c4c79c7b` | `v0.20.1` | [`YomuDoom-v0.20.1-dev-7803-9b09c4c79c7b-universal.apk`](0.20.1/YomuDoom-v0.20.1-dev-7803-9b09c4c79c7b-universal.apk) |

## Naming

Use `YomuDoom-v<version>-<variant>-<commit-count>-<sha>-universal.apk`. Never overwrite an existing APK; create a new version or build entry.

## Archive Command

Run `./gradlew :app:archiveDebugUniversalApk` after changing the code. The task creates the version directory and refuses to overwrite an existing APK.

`./gradlew :app:installDebug` also archives the APK automatically before installing it on the connected device.
