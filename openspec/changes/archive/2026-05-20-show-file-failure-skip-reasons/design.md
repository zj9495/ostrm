## Context

项目已经有 `task_run` 和 `task_run_log`，任务执行时也会把 `taskRunId` 放入 `FileProcessingContext`。目前处理器只返回 `ProcessingResult` 枚举，失败或跳过原因主要散落在普通日志里，单次任务日志无法稳定展示某个文件为什么失败或为什么跳过。

这次变更横跨处理器结果模型、处理链日志写入和前端运行日志展示。核心约束是原因必须来自实际处理分支或异常，不从全局日志文本中解析，也不使用猜测性兜底文案。

## Goals / Non-Goals

**Goals:**

- 对每个文件级 `SKIPPED`、`FAILED` 结果记录明确原因。
- 单次任务日志中展示文件路径、处理器、结果状态和原因。
- 保持原因产生点靠近真实业务判断，避免后续二次推断。
- 复用现有 `task_run_log` 查询链路。

**Non-Goals:**

- 不新增独立的文件结果查询页面。
- 不从 `backend.log`、控制台日志或时间区间反推文件原因。
- 不改变任务提交、运行记录列表和运行日志接口路径。
- 不为成功文件增加逐文件明细。

## Decisions

### 1. 使用结果对象携带状态和原因

将处理器返回值从单纯 `ProcessingResult` 状态扩展为文件处理结果对象，例如 `FileProcessingResult`，字段包含：

| 字段 | 说明 |
| --- | --- |
| `status` | `SUCCESS`、`SKIPPED`、`FAILED`、`FALLBACK` |
| `reason` | 失败或跳过原因，`SKIPPED` 和 `FAILED` 必填 |

`ProcessingResult` 保留为状态枚举，处理器返回新的结果对象。`SUCCESS` 不要求原因；`SKIPPED` 和 `FAILED` 的原因由对应业务分支或捕获到的异常直接提供。

备选方案是继续返回枚举并通过 `FileProcessingContext` 写入 `reason` 属性。该方案会把结果状态和原因拆到两个通道，容易出现状态与原因不一致，因此不采用。

### 2. 由处理链统一写入文件结果日志

`FileProcessorChain` 在每个支持当前文件的 Handler 执行后检查结果：

- `SKIPPED`：写入 `WARN` 级别任务日志。
- `FAILED`：写入 `ERROR` 级别任务日志。
- `SUCCESS`：不写入逐文件日志。

日志内容使用统一格式，由后端代码直接拼装，例如：

```text
文件跳过: <relativePath>/<fileName>，处理器: <HandlerName>，原因: <reason>
文件失败: <relativePath>/<fileName>，处理器: <HandlerName>，原因: <reason>
```

当 Handler 抛出异常时，处理链将异常消息作为失败原因写入任务日志，并将整体结果标记为 `FAILED`。不支持当前文件类型而被处理链跳过的 Handler 属于内部调度行为，不作为“文件跳过”写入任务日志。

### 3. 处理器在真实分支返回真实原因

每个会返回 `SKIPPED` 或 `FAILED` 的 Handler 需要在返回点写明原因，例如：

- 配置未启用：`NFO 刮削未启用`
- 本地目标文件已存在：`本地 NFO 文件已存在`
- OpenList 资源为空：`OpenList 中未找到对应字幕文件`
- 写入失败：使用捕获异常的 `getMessage()`

如果某个返回点无法明确原因，实施时应回到该分支的业务条件补齐原因，而不是生成通用占位原因。

### 4. 前端继续展示任务运行日志

`GET /api/task-runs/{runId}/logs` 继续返回 `TaskRunLogDto`。前端运行日志弹窗继续按时间、级别、消息展示；需要补充适合长路径和长原因的换行展示，并确保 `WARN`、`ERROR` 级别可读。

由于原因已经由后端写入 `message`，前端不解析日志文本，不新增文件原因计算逻辑。

## Risks / Trade-offs

- [Risk] 修改处理器返回类型会触及多个 Handler。→ Mitigation: 一次性更新 `FileProcessorHandler` 接口和所有实现，保持编译期约束。
- [Risk] 日志量会随跳过文件数量增加。→ Mitigation: 只记录 `SKIPPED` 和 `FAILED`，不记录成功文件。
- [Risk] 异常消息为空时无法满足“具体原因”。→ Mitigation: 捕获异常的地方用当前业务操作和异常类型构造原因，但不猜测不存在的字段或接口含义。

## Migration Plan

无需数据库迁移。部署后新运行的任务会写入带原因的文件级任务日志；历史运行日志保持原样。

## Open Questions

- 无。
