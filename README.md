# YomuDoom

## Português (Brasil)

YomuDoom é um leitor Android de código aberto para mangás, webtoons e quadrinhos. O projeto é baseado no Mihon, com identidade própria, melhorias de busca, leitura e organização da biblioteca.

### Funcionalidades do YomuDoom

- Busca consolidada que prioriza a fonte instalada com mais capítulos.
- Busca padrão ordenada da fonte com mais capítulos para a fonte com menos capítulos.
- Obras semelhantes encontradas nas fontes habilitadas.
- Recomendações baseadas no conteúdo da biblioteca e do histórico local.
- Autoscroll configurável no leitor Webtoon.
- Ordenação automática da Biblioteca.
- Botão de continuar leitura em um FAB maior e temático.
- Onboarding inicial com as principais funcionalidades do aplicativo.
- Backup e restauração para migração entre instalações.

### Download

Baixe a versão mais recente na página de [Releases](https://github.com/Marco-arch/YomuDoom/releases). O YomuDoom requer Android 8.0 ou superior.

As versões Debug usam o pacote `app.yomudoom.dev`. As versões Release usam `app.yomudoom`.

### Migração

O YomuDoom usa um pacote Android separado do aplicativo legado. Crie um backup no aplicativo antigo, instale o YomuDoom e restaure o backup em **Mais > Configurações > Dados e armazenamento**. Não desinstale o aplicativo antigo antes de confirmar que a restauração foi concluída.

### Desenvolvimento

```text
./gradlew :app:compileDebugKotlin
./gradlew :app:installDebug
```

Pull requests são bem-vindos. O código mantém a licença Apache 2.0 e a atribuição aos projetos originais utilizados como base.

### Aviso

O aplicativo não hospeda conteúdo e não possui afiliação com os provedores de conteúdo acessados por suas fontes.

### Licença

Este projeto é distribuído sob a [Apache License 2.0](./LICENSE). Os direitos autorais e atribuições existentes no código-fonte original são preservados.

## English

YomuDoom is an open-source Android reader for manga, webtoons, comics, and similar content. It is based on Mihon and adds its own identity together with search, reading, and library improvements.

### YomuDoom Features

- Consolidated search that prioritizes the installed source with the most chapters.
- Standard search ordered from the source with the most chapters to the source with the fewest chapters.
- Similar works found across enabled sources.
- Recommendations based on local library and reading history data.
- Configurable Webtoon auto-scroll.
- Automatic library ordering.
- A larger, themed continue-reading floating action button.
- First-run onboarding for the main application features.
- Backup and restore support for moving between installations.

### Download

Download the latest version from the [Releases](https://github.com/Marco-arch/YomuDoom/releases) page. YomuDoom requires Android 8.0 or newer.

Debug builds use the `app.yomudoom.dev` package. Release builds use `app.yomudoom`.

### Migration

YomuDoom uses a separate Android package from the legacy application. Create a backup in the old application, install YomuDoom, and restore it from **More > Settings > Data and storage**. Do not uninstall the old application until the restore has been verified.

### Development

```text
./gradlew :app:compileDebugKotlin
./gradlew :app:installDebug
```

Pull requests are welcome. The code keeps the Apache 2.0 license and attribution for the original projects used as its foundation.

### Disclaimer

The application hosts no content and is not affiliated with the content providers accessed through its sources.

### License

This project is distributed under the [Apache License 2.0](./LICENSE). Existing copyright notices and attributions in the original source code are preserved.
