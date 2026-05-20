## 1. 后端数据模型

- [x] 1.1 新增 Flyway 迁移，创建 `task_run` 表
- [x] 1.2 新增 Flyway 迁移，创建 `task_run_log` 表
- [x] 1.3 新增 `TaskRun`、`TaskRunLog` 实体
- [x] 1.4 新增 `TaskRunDto`、`TaskRunLogDto`、任务提交响应 DTO
- [x] 1.5 新增 `TaskRunMapper` 接口和 XML 映射

## 2. 后端执行链路

- [x] 2.1 新增 `TaskRunService`，提供创建运行记录、更新状态、写入日志和查询方法
- [x] 2.2 修改 `TaskExecutionService.submitTask`，提交时创建运行记录并返回 `runId`
- [x] 2.3 修改线程池执行入口，按 `runId` 更新运行状态
- [x] 2.4 在任务执行主流程写入单次运行日志
- [x] 2.5 将 `taskRunId` 放入 `FileProcessingContext`，供处理器按需写入运行日志

## 3. 后端接口

- [x] 3.1 修改 `POST /api/task-config/{id}/submit` 响应结构，返回 `runId`
- [x] 3.2 新增 `GET /api/task-config/{taskId}/runs`
- [x] 3.3 新增 `GET /api/task-runs/{runId}`
- [x] 3.4 新增 `GET /api/task-runs/{runId}/logs`
- [x] 3.5 为新增接口补充 OpenAPI 注解

## 4. 前端任务管理

- [x] 4.1 更新任务提交逻辑，接收并保存本次 `runId`
- [x] 4.2 在任务卡片增加运行记录入口
- [x] 4.3 新增运行记录列表弹窗或页面
- [x] 4.4 新增单次运行日志弹窗或页面
- [x] 4.5 补充运行状态、执行模式、耗时和失败原因展示

## 5. 验证

- [x] 5.1 手动验证手动提交任务后生成运行记录
- [x] 5.2 手动验证任务成功后运行记录状态为 `SUCCESS`
- [x] 5.3 手动验证任务失败后运行记录状态为 `FAILED` 且包含失败原因
- [x] 5.4 手动验证可按任务查看运行记录列表
- [x] 5.5 手动验证可按单次运行查看对应日志
