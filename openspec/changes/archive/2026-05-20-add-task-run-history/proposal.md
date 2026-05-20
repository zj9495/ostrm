# 任务运行记录与单次任务日志提案

## Why

当前任务只能看到配置级别的 `lastExecTime`，用户无法确认每次执行的开始时间、结束时间、执行模式、结果和失败原因。现有日志页面展示的是全局后端/前端日志，无法按某一次任务执行单独查看日志，排查定时任务或后台执行问题时需要在全局日志中人工定位。

## What Changes

- 新增任务运行记录，任务每次提交执行后创建一条独立记录。
- 运行记录保存执行模式、状态、开始时间、结束时间、耗时和失败原因。
- 新增单次任务日志，任务执行过程中的关键日志写入对应运行记录。
- 新增后端接口：
  - `GET /api/task-config/{taskId}/runs` 查询指定任务的运行记录。
  - `GET /api/task-runs/{runId}` 查询单条运行记录详情。
  - `GET /api/task-runs/{runId}/logs` 查询单次运行日志。
- 调整任务提交接口返回本次运行记录 ID，便于前端提交后进入或刷新运行记录。
- 前端任务管理页新增运行记录入口，并支持从某条运行记录单独查看日志。

## Capabilities

### New Capabilities

- `task-run-history`: 任务支持查看运行记录和单次运行日志。

### Modified Capabilities

- 无。

## Impact

- 后端：
  - 新增 Flyway 迁移，创建 `task_run` 和 `task_run_log` 表。
  - 新增 `TaskRun`、`TaskRunLog` 实体、DTO、Mapper 和 XML 映射。
  - 新增 `TaskRunService`，负责创建运行记录、更新状态和写入运行日志。
  - 修改 `TaskExecutionService`，在提交、开始、成功、失败阶段维护运行记录，并在执行过程中写入单次运行日志。
  - 修改 `TaskConfigController.submitTask` 返回本次 `runId`。
  - 新增 `TaskRunController` 或在现有任务控制器中增加运行记录/日志查询接口。
- 前端：
  - 更新 `frontend/app/pages/task-management/[id].vue`，为任务卡片增加运行记录入口。
  - 新增运行记录列表视图，展示状态、执行模式、开始/结束时间、耗时和失败原因。
  - 新增单次运行日志查看视图，按运行记录 ID 拉取并展示日志。
- 兼容性：
  - 现有任务没有历史运行记录；迁移后从新执行开始产生记录。
  - 不解析历史 `backend.log` 生成运行记录。
  - 不新增外部依赖。
