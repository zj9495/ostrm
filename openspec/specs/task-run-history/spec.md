# task-run-history Specification

## Purpose

任务 SHALL 支持查看每次运行记录，并支持按单次运行记录查看该次任务日志。

## Requirements

### Requirement: 任务提交生成运行记录

系统 SHALL 在任务每次提交执行时创建一条运行记录。

#### Scenario: 手动提交任务
- **WHEN** 用户调用 `POST /api/task-config/{id}/submit`
- **THEN** 系统 SHALL 创建一条 `SUBMITTED` 状态的运行记录
- **AND** 运行记录 SHALL 保存任务 ID、执行模式和提交时间
- **AND** 接口响应 SHALL 返回本次运行记录 ID

#### Scenario: 定时任务提交
- **WHEN** Quartz 定时任务提交任务执行
- **THEN** 系统 SHALL 创建一条运行记录
- **AND** 运行记录 SHALL 保存任务 ID、执行模式和提交时间

---

### Requirement: 运行记录状态随执行生命周期更新

系统 SHALL 根据任务执行结果更新运行记录状态。

#### Scenario: 任务开始执行
- **WHEN** 线程池开始执行某条运行记录对应的任务
- **THEN** 系统 SHALL 将该运行记录状态更新为 `RUNNING`
- **AND** 系统 SHALL 保存开始时间

#### Scenario: 任务执行成功
- **WHEN** 任务执行完成且未抛出异常
- **THEN** 系统 SHALL 将该运行记录状态更新为 `SUCCESS`
- **AND** 系统 SHALL 保存结束时间和执行耗时

#### Scenario: 任务执行失败
- **WHEN** 任务执行过程中抛出异常
- **THEN** 系统 SHALL 将该运行记录状态更新为 `FAILED`
- **AND** 系统 SHALL 保存结束时间、执行耗时和失败原因

---

### Requirement: 查询任务运行记录

系统 SHALL 支持按任务查询运行记录列表。

#### Scenario: 查看指定任务运行记录
- **WHEN** 用户调用 `GET /api/task-config/{taskId}/runs`
- **THEN** 系统 SHALL 返回该任务的运行记录列表
- **AND** 运行记录 SHALL 按提交时间倒序排列

#### Scenario: 任务没有运行记录
- **WHEN** 用户查询从未执行过的任务运行记录
- **THEN** 系统 SHALL 返回空列表

---

### Requirement: 单次运行日志归属到运行记录

系统 SHALL 将任务执行过程日志写入对应运行记录。

#### Scenario: 任务执行过程写入日志
- **WHEN** 任务执行进入提交、开始、文件列表获取、清理、处理、完成或失败节点
- **THEN** 系统 SHALL 写入一条包含运行记录 ID、时间、级别和消息的运行日志

#### Scenario: 查询单次运行日志
- **WHEN** 用户调用 `GET /api/task-runs/{runId}/logs`
- **THEN** 系统 SHALL 返回该运行记录对应的日志列表
- **AND** 日志 SHALL 按写入顺序正序排列

#### Scenario: 运行记录没有日志
- **WHEN** 用户查询存在但尚未写入日志的运行记录
- **THEN** 系统 SHALL 返回空列表

---

### Requirement: 前端查看运行记录和运行日志

前端 SHALL 在任务管理页面提供运行记录和单次运行日志查看入口。

#### Scenario: 打开任务运行记录
- **WHEN** 用户在任务卡片点击运行记录入口
- **THEN** 前端 SHALL 调用 `GET /api/task-config/{taskId}/runs`
- **AND** 前端 SHALL 展示运行状态、执行模式、提交时间、开始时间、结束时间、耗时和失败原因

#### Scenario: 打开单次运行日志
- **WHEN** 用户在运行记录中点击日志入口
- **THEN** 前端 SHALL 调用 `GET /api/task-runs/{runId}/logs`
- **AND** 前端 SHALL 只展示该运行记录对应的日志
