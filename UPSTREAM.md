# YomuDoom upstream and modifications

## Provenance

YomuDoom is a derivative of the open-source [Mihon](https://github.com/mihonapp/mihon) Android reader and earlier Tachiyomi work. The recorded comparison baseline is Mihon commit [`ab58a9952e55485d81193f13ffc262725d8cae32`](https://github.com/mihonapp/mihon/commit/ab58a9952e55485d81193f13ffc262725d8cae32), dated July 8, 2026.

The upstream code and YomuDoom modifications are distributed under the Apache License 2.0. Existing copyright, patent, trademark, and attribution notices remain in effect. YomuDoom-specific branding, documentation, visual assets, and modifications are maintained by YomuDoom Project / Marco.

## YomuDoom work

The main YomuDoom-specific areas are:

- Product identity, Android application ID, launcher and notification artwork, onboarding, documentation, and GitHub release flow.
- Consolidated source search, source selection and ordering, similar works, and local recommendations.
- Reader Webtoon auto-scroll, touch cancellation, and contextual chapter resume.
- Extension discovery, curated default store support, installation flow, startup caching, and NSFW filtering.
- Library, history, download, update, migration, and performance refinements.
- HyperOS-aware onboarding permission detection and continuous status synchronization.

The complete path-by-path snapshot is maintained in [UPSTREAM_DIFF.md](./UPSTREAM_DIFF.md). It is regenerated after public-release preparation so the inventory matches the published source tree.

## External compatibility

YomuDoom preconfigures the independent [Keiyoushi extension store](https://keiyoushi.github.io) as a deliberate project curation choice. Keiyoushi, its repository, extensions, signing infrastructure, and services are not operated by YomuDoom.

Tracker integrations retain OAuth clients and `mihon://` callbacks inherited from Mihon so existing AniList, MyAnimeList, Shikimori, Bangumi, Kitsu, Hikka, and MangaBaka login flows continue to work. These compatibility identifiers are not YomuDoom-owned infrastructure.

Internal package and resource identifiers containing `tachiyomi` or `mihon` are retained where changing them would harm extension, backup, tracker, or binary compatibility. Their presence does not imply affiliation or ownership.
