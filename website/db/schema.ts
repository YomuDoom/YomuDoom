import { integer, primaryKey, sqliteTable, text } from "drizzle-orm/sqlite-core";

export const dailyMetrics = sqliteTable("daily_metrics", {
  day: text("day").notNull(),
  event: text("event", { enum: ["page_view", "download_click"] }).notNull(),
  language: text("language", { enum: ["pt-BR", "en"] }).notNull(),
  count: integer("count").notNull().default(0),
  updatedAt: text("updated_at").notNull(),
}, (table) => [primaryKey({ columns: [table.day, table.event, table.language] })]);
