ALTER TABLE task_config ADD COLUMN strm_url_replace_from TEXT DEFAULT '';
ALTER TABLE task_config ADD COLUMN strm_url_replace_to TEXT DEFAULT '';
ALTER TABLE task_config ADD COLUMN generate_sign INTEGER DEFAULT 1 NOT NULL;
