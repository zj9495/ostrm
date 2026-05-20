-- 为任务配置添加过滤规则字段
ALTER TABLE task_config ADD COLUMN min_file_size_bytes BIGINT;
ALTER TABLE task_config ADD COLUMN file_name_exclude_regex VARCHAR(500);
ALTER TABLE task_config ADD COLUMN directory_name_exclude_regex VARCHAR(500);
