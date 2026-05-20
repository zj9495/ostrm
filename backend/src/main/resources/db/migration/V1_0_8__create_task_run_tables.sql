-- 创建任务运行记录表
CREATE TABLE task_run
(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_config_id INTEGER NOT NULL,
    is_increment INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    duration_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_run_task_config_id ON task_run(task_config_id);
CREATE INDEX idx_task_run_status ON task_run(status);
CREATE INDEX idx_task_run_submitted_at ON task_run(submitted_at);

CREATE TRIGGER update_task_run_updated_at
    AFTER UPDATE ON task_run
    FOR EACH ROW
BEGIN
    UPDATE task_run SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- 创建任务运行日志表
CREATE TABLE task_run_log
(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_run_id INTEGER NOT NULL,
    logged_at TIMESTAMP NOT NULL,
    level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL
);

CREATE INDEX idx_task_run_log_task_run_id ON task_run_log(task_run_id);
CREATE INDEX idx_task_run_log_logged_at ON task_run_log(logged_at);
