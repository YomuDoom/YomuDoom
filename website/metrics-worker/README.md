# YomuDoom metrics worker

Endpoint privado de escrita usado pela versão do site publicada no GitHub Pages.

## Configuração inicial

1. Crie uma conta gratuita na Cloudflare e instale/autentique o Wrangler.
2. Crie o banco:

   ```bash
   npx wrangler d1 create yomudoom-metrics
   ```

3. Copie o `database_id` retornado para `wrangler.toml`.
4. Crie a tabela:

   ```bash
   npx wrangler d1 execute yomudoom-metrics --remote --file=schema.sql
   ```

5. Faça o deploy:

   ```bash
   npx wrangler deploy
   ```

6. Configure a variável do repositório GitHub `YOMUDOOM_METRICS_URL` com a URL publicada terminando em `/metrics`.

O Worker aceita somente `POST /metrics` com origem `https://yomudoom.github.io`, eventos `page_view` ou `download_click` e idiomas `pt-BR` ou `en`. Não existe endpoint público para leitura.
