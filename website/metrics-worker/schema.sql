CREATE TABLE IF NOT EXISTS daily_metrics (
  day TEXT NOT NULL,
  event TEXT NOT NULL CHECK (event IN ('page_view', 'download_click')),
  language TEXT NOT NULL CHECK (language IN ('pt-BR', 'en')),
  count INTEGER NOT NULL DEFAULT 0,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (day, event, language)
);
