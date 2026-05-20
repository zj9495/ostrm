# file-filtering Specification Delta

## Modified Requirements

### Requirement: 视频文件过滤

系统 SHALL 在文件处理管道中过滤出视频文件，只将视频文件传递给后续处理器：

1. 检查每个文件的类型和扩展名
2. 过滤出类型为 "file" 且扩展名为视频格式的文件
3. 应用任务配置中的文件大小、文件名和目录名称过滤规则
4. 跳过目录、非视频文件、隐藏文件以及不满足任务配置过滤规则的文件

#### Scenario: 过滤视频文件
- **WHEN** 目录包含视频文件和非视频文件
- **THEN** 系统 SHALL 只将视频文件传递给后续处理器

#### Scenario: 按最小文件大小过滤
- **WHEN** 任务配置的 `minFileSizeBytes` 为 `1073741824`
- **AND** 视频文件大小小于 `1073741824`
- **THEN** 系统 SHALL 跳过该视频文件
- **AND** 系统 SHALL 向本次任务日志写入该文件路径和跳过原因

#### Scenario: 文件大小缺失
- **WHEN** 任务启用了文件大小过滤
- **AND** OpenList 返回的视频文件 `size` 为 `null`
- **THEN** 系统 SHALL 跳过该视频文件
- **AND** 系统 SHALL 向本次任务日志写入该文件路径和跳过原因

#### Scenario: 按文件名排除正则过滤
- **WHEN** 任务配置了 `fileNameExcludeRegex`
- **AND** 视频文件名匹配该正则
- **THEN** 系统 SHALL 跳过该视频文件
- **AND** 系统 SHALL 向本次任务日志写入该文件路径和跳过原因

#### Scenario: 按目录名称排除正则过滤
- **WHEN** 任务配置了 `directoryNameExcludeRegex`
- **AND** 视频文件在任务根路径下的任一父目录名称匹配该正则
- **THEN** 系统 SHALL 跳过该视频文件
- **AND** 系统 SHALL 向本次任务日志写入该文件路径和跳过原因

---

### Requirement: 过滤跳过原因日志

系统 SHALL 为被任务配置过滤规则跳过的文件写入任务日志。

#### Scenario: 文件被任务配置规则跳过
- **WHEN** 文件因 `minFileSizeBytes`、`fileNameExcludeRegex` 或 `directoryNameExcludeRegex` 被跳过
- **THEN** 系统 SHALL 向本次任务日志写入一条跳过日志
- **AND** 日志 SHALL 包含文件路径、触发的过滤字段、文件实际值、配置规则值和跳过原因

#### Scenario: 目录被目录名称排除规则跳过
- **WHEN** `FileDiscoveryHandler` 因 `directoryNameExcludeRegex` 跳过目录递归
- **THEN** 系统 SHALL 向本次任务日志写入一条目录跳过日志
- **AND** 日志 SHALL 包含目录路径、触发的过滤字段、目录名称、配置规则值和跳过原因
- **AND** 系统 SHALL NOT 为该目录内未枚举的文件补写文件级跳过日志

---

### Requirement: 批量文件过滤

系统 SHALL 支持批量过滤目录中的所有文件：

1. 接收文件列表作为输入
2. 过滤出所有视频文件
3. 应用任务配置过滤规则
4. 返回过滤后的视频文件列表

#### Scenario: 过滤结果顺序保持
- **WHEN** 批量过滤文件并应用任务配置过滤规则
- **THEN** 过滤后的文件列表 SHALL 保持原始顺序
