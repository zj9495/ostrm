-- 为 task_config 表添加 scraper_type 列
-- 现有行默认设置为 TMDB
ALTER TABLE task_config ADD COLUMN scraper_type VARCHAR(20) NOT NULL DEFAULT 'TMDB';
