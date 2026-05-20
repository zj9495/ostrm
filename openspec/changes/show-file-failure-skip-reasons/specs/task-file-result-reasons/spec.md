## ADDED Requirements

### Requirement: 任务运行日志记录文件级跳过原因

系统 SHALL 在单次任务运行日志中记录被跳过文件的具体原因。

#### Scenario: Handler 返回跳过结果
- **WHEN** 文件处理 Handler 对当前文件返回 `SKIPPED`
- **THEN** 系统 SHALL 向该任务运行记录写入一条运行日志
- **AND** 日志 SHALL 包含文件路径、处理器名称、跳过状态和跳过原因

#### Scenario: 内部处理器不支持当前文件类型
- **WHEN** 处理链因 Handler 不支持当前文件类型而未调用该 Handler
- **THEN** 系统 MUST NOT 将该内部调度行为记录为文件跳过原因

---

### Requirement: 任务运行日志记录文件级失败原因

系统 SHALL 在单次任务运行日志中记录处理失败文件的具体原因。

#### Scenario: Handler 返回失败结果
- **WHEN** 文件处理 Handler 对当前文件返回 `FAILED`
- **THEN** 系统 SHALL 向该任务运行记录写入一条运行日志
- **AND** 日志 SHALL 包含文件路径、处理器名称、失败状态和失败原因

#### Scenario: Handler 执行时抛出异常
- **WHEN** 文件处理 Handler 处理当前文件时抛出异常
- **THEN** 系统 SHALL 向该任务运行记录写入一条失败日志
- **AND** 日志 SHALL 包含文件路径、处理器名称和异常对应的失败原因

---

### Requirement: 前端展示文件级失败和跳过原因

前端 SHALL 在单次任务运行日志视图中展示后端返回的文件级失败和跳过原因。

#### Scenario: 查看包含文件结果原因的运行日志
- **WHEN** 用户打开某次任务运行日志
- **THEN** 前端 SHALL 调用 `GET /api/task-runs/{runId}/logs`
- **AND** 前端 SHALL 展示日志中的文件路径、结果状态和原因文本

#### Scenario: 文件路径或原因较长
- **WHEN** 运行日志中的文件路径或原因文本超过一行宽度
- **THEN** 前端 SHALL 保持文本可读并允许换行显示
