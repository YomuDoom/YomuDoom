CREATE TABLE `daily_metrics` (
	`day` text NOT NULL,
	`event` text NOT NULL,
	`language` text NOT NULL,
	`count` integer DEFAULT 0 NOT NULL,
	`updated_at` text NOT NULL,
	PRIMARY KEY(`day`, `event`, `language`)
);
