# YomuDoom Changelog

## 0.21.6

### Português (Brasil)

- Corrige o download de atualizações no aplicativo garantindo o reconhecimento de APKs universais nos lançamentos do GitHub.
- Exibe barra de progresso em tempo real (0% a 100%) no diálogo de atualização ao clicar em baixar.
- Abre automaticamente a tela do instalador de pacotes do Android assim que o download do APK é finalizado.

### English

- Fixes in-app update downloads by ensuring universal APKs from GitHub releases are properly recognized.
- Displays a real-time progress bar (0% to 100%) inside the update dialog when downloading starts.
- Automatically launches the Android package installer prompt as soon as the APK download finishes.

## 0.21.5

### Português (Brasil)

- Corrige busca de capas alternativas no MangaDex e Kitsu executando em segundo plano (`Dispatchers.IO`), evitando erro de rede na interface.
- Adiciona campo de pesquisa no diálogo de capas disponíveis para buscar por nomes alternativos ou em outros idiomas.
- Melhora o carregamento das imagens de capa com fallback nativo, tratamento de erros e placeholder.

### English

- Fixes alternative cover searching on MangaDex and Kitsu by running in the background (`Dispatchers.IO`), avoiding network on main thread errors.
- Adds a search field in the available covers dialog to query with alternative or localized titles.
- Improves cover image loading with native source fallback, error handling, and placeholders.

## 0.21.4

### Português (Brasil)

- Exibe barra de progresso em tempo real na notificação durante a recuperação de capas ausentes em massa na biblioteca.
- Adiciona opção para visualizar e selecionar capas alternativas diretamente na tela da obra (capas de volumes do MangaDex e pôsteres do Kitsu).
- Permite trocar ou definir a capa da obra facilmente pelo menu da obra, pelo diálogo de visualização da capa ou no painel de capa ausente.

### English

- Displays a real-time progress bar in notification during mass recovery of missing covers in the library.
- Adds option to browse and select alternative covers directly from the manga screen (volume covers from MangaDex and posters from Kitsu).
- Allows changing or setting the manga cover easily from the manga menu, the full cover dialog, or the missing cover panel.

## 0.21.2

### Português (Brasil)

- Pergunta a idade na primeira instalação para configurar a exibição de conteúdo +18.
- Torna o estado do filtro +18 mais visível quando esse conteúdo está desativado.
- Mantém a escolha de idade local ao dispositivo, sem incluí-la ou sobrescrevê-la em backups.

### English

- Asks for age on first install to configure whether 18+ content is shown.
- Makes the 18+ filter state clearer when this content is disabled.
- Keeps the age choice local to the device without including or overwriting it in backups.

## 0.21.1

### Português (Brasil)

- Remove a loja de extensões pré-configurada; novas instalações começam sem lojas.
- Mantém lojas e extensões já configuradas por usuários existentes.
- Mantém cadastro manual de lojas externas com aviso de segurança e responsabilidade.
- Remove deeplinks de lojas, recomendações de fontes e instalação coletiva de extensões.
- Remove a variante e os recursos planejados para a Google Play.

### English

- Removes the preconfigured extension store; new installations start with no stores.
- Preserves stores and extensions already configured by existing users.
- Keeps manual external-store configuration with a security and responsibility notice.
- Removes store deeplinks, source recommendations, and bulk extension installation.
- Removes the planned Google Play variant and assets.

## 0.21.0

### Português (Brasil)

- Inicia a beta pública do YomuDoom no GitHub.
- Corrige a verificação de permissão de instalação no Android 8 e 9 e mantém a compatibilidade com HyperOS.
- Corrige a atualização de idioma no calendário de próximos lançamentos.
- Adiciona uma política de privacidade pública e acessível pela tela Sobre.
- Torna o lint de release obrigatório e adiciona análise CodeQL.
- Adiciona SHA-256 e impressão digital do certificado aos releases oficiais.

### English

- Starts the public YomuDoom beta on GitHub.
- Fixes install-permission detection on Android 8 and 9 while preserving HyperOS compatibility.
- Fixes locale updates in the upcoming releases calendar.
- Adds a public privacy policy accessible from the About screen.
- Makes release lint mandatory and adds CodeQL analysis.
- Adds SHA-256 and the signing certificate fingerprint to official releases.

## 0.20.24

- Changed the default chapter order for new titles to ascending.
- Kept manually selected chapter ordering for each title unchanged.
- Kept the manual “Check for updates” action available in the About screen.

## 0.20.23

- Added a manual “Check for updates” action to the About screen.
- Reduced the automatic update-check interval from three days to one day.
- Ensured manual checks bypass the automatic interval and provide visible feedback.

## 0.20.22

- Added voluntary support options through Patreon, Stripe, and Pix.
- Added a Pix copy action and kept all support options free of rewards or exclusive features.
- Added a neutral support reminder shown only after six months of use.

## 0.20.21

- Documented the exact Mihon baseline and the complete YomuDoom modification inventory.
- Removed obsolete donation strings and cleaned visible Mihon branding from all translations.
- Added the public security policy, ownership rules, and repository hardening documentation.
- Documented extension and tracker compatibility boundaries.

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
- Improved extension onboarding guidance.
- Added a refresh button to the Recommendations tab.
- Replaced the full-screen update page with a compact dialog.
- Fixed the startup crash caused by the missing NSFW filter registration.

## 0.20.16

- Changed the Android application ID to `app.yomudoom` and the Debug suffix to `app.yomudoom.dev`.
- Added YomuDoom branding, launcher icon, splash icon, and notification icons.
- Removed donation campaigns, Support Us, legacy help links, and external Mihon social links.
- Removed the WhatsApp APK helper; releases are distributed through GitHub.
- Added GitHub-based update configuration for `YomuDoom/YomuDoom`.

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
