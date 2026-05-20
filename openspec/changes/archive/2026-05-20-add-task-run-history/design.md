# 设计说明

## 数据模型

新增 `task_run` 表保存一次任务执行的元数据：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | INTEGER | 主键 |
| `task_config_id` | INTEGER | 任务配置 ID |
| `is_increment` | INTEGER | 本次执行模式，1 表示增量，0 表示全量 |
| `status` | VARCHAR(20) | `SUBMITTED`、`RUNNING`、`SUCCESS`、`FAILED` |
| `submitted_at` | TIMESTAMP | 提交时间 |
| `started_at` | TIMESTAMP | 开始执行时间 |
| `finished_at` | TIMESTAMP | 结束时间 |
| `duration_ms` | BIGINT | 执行耗时 |
| `error_message` | TEXT | 失败原因 |
| `created_at` | TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | 更新时间 |

新增 `task_run_log` 表保存一次任务执行的日志：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | INTEGER | 主键 |
| `task_run_id` | INTEGER | 运行记录 ID |
| `logged_at` | TIMESTAMP | 日志时间 |
| `level` | VARCHAR(10) | 日志级别 |
| `message` | TEXT | 日志内容 |

运行日志按 `id ASC` 返回。同一运行记录下的日志由明确的 `task_run_id` 关联，不从全局日志文件按文本匹配或时间区间推断。

## 执行生命周期

`TaskExecutionService.submitTask` 在提交任务时创建运行记录：

1. 校验任务存在且启用。
2. 计算本次执行模式。
3. 创建 `SUBMITTED` 运行记录。
4. 将 `taskId`、`runId` 和执行模式提交到线程池。
5. 接口返回 `runId`。

线程池执行时按生命周期更新记录：

1. 开始执行：状态更新为 `RUNNING`，写入 `started_at`。
2. 成功完成：状态更新为 `SUCCESS`，写入 `finished_at` 和 `duration_ms`。
3. 执行失败：状态更新为 `FAILED`，写入 `finished_at`、`duration_ms` 和 `error_message`。

## 日志写入范围

单次任务日志记录任务执行主流程中的明确节点：

1. 任务提交、开始、完成、失败。
2. OpenList 文件列表获取开始、成功、失败。
3. 全量模式清理 STRM 目录开始和完成。
4. Handler 链处理开始和完成。
5. 增量模式孤立 STRM 清理开始和完成。

处理器内部如果需要展示在单次任务日志中，由处理器通过 `FileProcessingContext` 获取 `taskRunId` 后调用 `TaskRunService.appendLog` 写入。提案不通过扫描 `backend.log` 的方式补全处理器日志。

## API

`POST /api/task-config/{id}/submit`

响应 `data` 改为对象：

```json
{
  "runId": 1,
  "message": "任务已提交执行"
}
```

`GET /api/task-config/{taskId}/runs`

查询指定任务运行记录，按 `submitted_at DESC` 返回。

`GET /api/task-runs/{runId}`

查询单条运行记录详情。

`GET /api/task-runs/{runId}/logs`

查询单次运行日志，按 `id ASC` 返回。

## 前端交互

任务卡片新增运行记录入口。用户点击后打开该任务的运行记录列表：

- 状态：提交中、运行中、成功、失败。
- 执行模式：全量或增量。
- 时间：提交时间、开始时间、结束时间。
- 耗时：由后端 `durationMs` 展示。
- 失败原因：仅失败记录展示。

运行记录列表中每条记录提供日志入口。用户点击后打开单次运行日志视图，只展示该 `runId` 对应的 `task_run_log`。
