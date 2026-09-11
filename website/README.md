# Site oficial do YomuDoom

Landing page bilíngue do YomuDoom, com apresentação do aplicativo, download oficial, política de privacidade e métricas agregadas de visitas e cliques.

## Desenvolvimento

Requer Node.js 22.13 ou superior.

```text
npm install
npm run dev
npm run build
npm test
```

As métricas usam o binding D1 `DB`. Somente contagens diárias de `page_view` e `download_click`, separadas por idioma, são armazenadas.

O site não hospeda o APK. Todos os downloads apontam para o GitHub Releases oficial do YomuDoom.
