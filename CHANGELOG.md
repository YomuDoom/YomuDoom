# YomuDoom Changelog

## 0.20.20

- Added HyperOS-compatible permission detection through Android AppOps.
- Kept onboarding permission checks synchronized while the step is visible.

## 0.20.19

- Fixed onboarding permission checks not refreshing after returning from Android or HyperOS settings.
- Added safe fallbacks when device permission settings cannot be opened.
- Prevented intermittent onboarding crashes caused by unavailable system settings activities.

## 0.20.18

- Added a YomuDoom launcher icon generated specifically for the project.
- Stopped Webtoon auto-scroll whenever the reader is touched.
- Preserved the selected chapter order and centered the last opened chapter when entering a library title.
- Consolidated project identity, attribution, and release documentation for the new repository baseline.

## 0.20.17

- Added a subtle indicator when 18+ content is hidden.
- Curated the recommended source list and added onboarding guidance.
- Added a refresh button to the Recommendations tab.
- Replaced the full-screen update page with a compact dialog.
- Fixed the startup crash caused by the missing NSFW filter registration.

## 0.20.16

- Changed the Android application ID to `app.yomudoom` and the Debug suffix to `app.yomudoom.dev`.
- Added YomuDoom branding, launcher icon, splash icon, and notification icons.
- Removed donation campaigns, Support Us, legacy help links, and external Mihon social links.
- Removed the WhatsApp APK helper; releases are distributed through GitHub.
- Added GitHub-based update configuration for `Marco-arch/YomuDoom`.

## 0.20.15

- Moved Continue reading to a themed floating action button.
- Adjusted the button to a 75dp touch target.

## 0.20.10 - 0.20.14

- Added the YomuDoom onboarding feature guide.
- Fixed the onboarding scroll crash.
- Added onboarding descriptions for search, similar works, recommendations, auto-scroll, and automatic ordering.
- Added similar works and local recommendations.
- Added configurable Webtoon auto-scroll.
- Improved consolidated search and source ordering.

## 0.20.1 - 0.20.9

- Added progressive source loading and cached source state.
- Added source grouping and consolidated search selection.
- Added source language details and search mode controls.
