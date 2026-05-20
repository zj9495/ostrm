## MODIFIED Requirements

### Requirement: 处理结果枚举

系统 SHALL 定义处理结果状态，包含以下状态：

1. `SUCCESS` - 处理成功
2. `SKIPPED` - 跳过（文件已存在/配置禁用）
3. `FAILED` - 处理失败
4. `FALLBACK` - 需要 fallback 到其他处理方式

系统 SHALL 定义处理器返回结果模型，包含处理结果状态和原因。`SKIPPED` 与 `FAILED` 结果 SHALL 携带非空原因，原因 SHALL 来自触发该结果的业务分支或捕获到的异常。

#### Scenario: 处理成功结果
- **WHEN** 文件处理顺利完成
- **THEN** 处理器 SHALL 返回 `SUCCESS` 状态

#### Scenario: 文件已存在跳过处理
- **WHEN** 本地文件已存在，无需重复处理
- **THEN** 处理器 SHALL 返回 `SKIPPED` 状态
- **AND** 处理器 SHALL 返回说明本地文件已存在的原因

#### Scenario: 配置禁用跳过处理
- **WHEN** 当前 Handler 对应功能被任务配置禁用
- **THEN** 处理器 SHALL 返回 `SKIPPED` 状态
- **AND** 处理器 SHALL 返回说明配置禁用的原因

#### Scenario: 处理失败结果
- **WHEN** 文件处理过程中发生错误
- **THEN** 处理器 SHALL 返回 `FAILED` 状态
- **AND** 处理器 SHALL 返回该错误对应的失败原因

#### Scenario: 需要 fallback
- **WHEN** 当前处理器无法处理，需要其他处理器处理
- **THEN** 处理器 SHALL 返回 `FALLBACK` 状态
