# YomuDoom

**YomuDoom** é um leitor de mangás, webtoons e quadrinhos gratuito e open source para Android (8.0+), baseado no Mihon.

- 🌐 **Site Oficial / Official Website**: [yomudoom.github.io/YomuDoom](https://yomudoom.github.io/YomuDoom/)
- 📦 **Download Oficial / Official Releases**: [GitHub Releases](https://github.com/YomuDoom/YomuDoom/releases/latest)
- 💻 **Repositório Oficial / Source Code**: [github.com/YomuDoom/YomuDoom](https://github.com/YomuDoom/YomuDoom)

---

## Português (Brasil)

YomuDoom é um leitor Android para mangás, webtoons e quadrinhos. O projeto é baseado no Mihon, com identidade própria, melhorias de busca, leitura e organização da biblioteca.

### Funcionalidades adicionais do YomuDoom

- Busca consolidada que prioriza a fonte instalada com mais capítulos.
- Busca padrão ordenada da fonte com mais capítulos para a fonte com menos capítulos.
- Obras semelhantes encontradas nas fontes habilitadas.
- Recomendações baseadas no conteúdo da biblioteca e do histórico local.
- Autoscroll configurável no leitor Webtoon.
- Retomada contextual que centraliza o último capítulo acessado sem alterar a ordenação escolhida.

### Download

Baixe a versão mais recente na página oficial de [Releases](https://github.com/YomuDoom/YomuDoom/releases/latest) ou conheça os detalhes no [Site Oficial](https://yomudoom.github.io/YomuDoom/). O YomuDoom requer Android 8.0 ou superior.

Novas instalações não incluem lojas de extensões. O aplicativo pode ler arquivos locais e permite que o próprio usuário cadastre manualmente uma loja externa compatível sob sua responsabilidade.

### Desenvolvimento

```text
./gradlew :app:compileDebugKotlin
./gradlew :app:installDebug
./gradlew lintRelease
./gradlew assembleRelease -Penable-updater
```

O código mantém a licença Apache 2.0 e a atribuição aos projetos originais utilizados como base.

O YomuDoom não fornece, recomenda, mantém nem presta suporte a lojas, extensões ou provedores de conteúdo externos. Integrações de trackers preservam clientes e callbacks do Mihon para compatibilidade.

### Aviso

O aplicativo não hospeda conteúdo e não possui afiliação com os provedores de conteúdo acessados por suas fontes.
Consulte a [Política de Privacidade](./PRIVACY.md) para entender quais dados permanecem no aparelho e quais serviços externos podem ser acessados.

### Licença

Este projeto é distribuído sob a [Apache License 2.0](./LICENSE). Os direitos autorais e atribuições existentes no código-fonte original são preservados.
As modificações e a identidade YomuDoom são mantidas por **YomuDoom Project / Marco**. Consulte também os arquivos [NOTICE](./NOTICE) e [UPSTREAM](./UPSTREAM.md) para conhecer a origem e o inventário das mudanças.

O ícone e os recursos visuais identificados como YomuDoom foram gerados com IA especificamente para este projeto e revisados como parte de sua identidade visual.

## English

YomuDoom is an Android reader for manga, webtoons, comics, and similar content. It is based on Mihon and adds its own identity together with search, reading, and library improvements.

### Additional YomuDoom Features

- Consolidated search that prioritizes the installed source with the most chapters.
- Standard search ordered from the source with the most chapters to the source with the fewest chapters.
- Similar works found across enabled sources.
- Recommendations based on local library and reading history data.
- Configurable Webtoon auto-scroll.
- Contextual resume that centers the last opened chapter without changing the selected order.

### Download

Download the latest version from the official [Releases](https://github.com/YomuDoom/YomuDoom/releases/latest) page or explore the [Official Website](https://yomudoom.github.io/YomuDoom/). YomuDoom requires Android 8.0 or newer.

New installations include no extension stores. The application can read local files and lets users manually configure a compatible external store under their own responsibility.

### Development

```text
./gradlew :app:compileDebugKotlin
./gradlew :app:installDebug
./gradlew lintRelease
./gradlew assembleRelease -Penable-updater
```

The code keeps the Apache 2.0 license and attribution for the original projects used as its foundation.

YomuDoom does not provide, recommend, maintain, or support external stores, extensions, or content providers. Tracker integrations retain Mihon clients and callbacks for compatibility.

### Disclaimer

The application hosts no content and is not affiliated with the content providers accessed through its sources.
See the [Privacy Policy](./PRIVACY.md) to understand which data remains on the device and which external services may be contacted.

### License

This project is distributed under the [Apache License 2.0](./LICENSE). Existing copyright notices and attributions in the original source code are preserved.
YomuDoom modifications and product identity are maintained by **YomuDoom Project / Marco**. See [NOTICE](./NOTICE) and [UPSTREAM](./UPSTREAM.md) for provenance and the modification inventory.

The YomuDoom icon and project-specific visual assets were generated with AI specifically for this project and reviewed as part of its visual identity.
