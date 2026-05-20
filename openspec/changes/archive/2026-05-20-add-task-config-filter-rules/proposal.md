# 任务配置过滤规则提案

## Why

当前任务配置只能指定 OpenList 路径、STRM 输出路径、刮削和重命名等选项，文件处理阶段只按视频扩展名过滤。用户无法在任务级别排除样片、小体积广告文件、特定命名文件，或跳过特定目录，导致 STRM 生成范围不够精确。

## What Changes

- 在任务配置中新增过滤规则字段：
  - `minFileSizeBytes`: 最小文件大小，单位 bytes
  - `fileNameExcludeRegex`: 文件名不得匹配的 Java 正则
  - `directoryNameExcludeRegex`: 父目录名称不得匹配的 Java 正则
- 修改任务配置的数据库表、实体、DTO、Mapper 和前端表单，确保规则可创建、编辑、查看和持久化。
- 修改文件过滤流程：视频扩展名过滤后，再应用任务配置过滤规则；不满足规则的文件不进入 STRM 生成、字幕复制、图片/NFO 下载或刮削流程。
- 对被任务配置过滤规则跳过的文件，向本次任务日志写入文件路径和具体跳过原因。
- 目录排除规则应在文件发现阶段生效：被 `directoryNameExcludeRegex` 匹配的目录不再递归扫描。
- 正则表达式统一使用 Java `Pattern`，匹配方式为 `matcher(value).find()`；需要完整匹配时由用户在正则中显式使用 `^` 和 `$`。

## Capabilities

### New Capabilities

- `task-config-filtering`: 任务配置支持按文件大小、文件名、目录名称声明过滤规则。

### Modified Capabilities

- `file-filtering`: 文件过滤在现有视频扩展名识别基础上应用任务配置过滤规则。
- `file-discovery`: 文件发现可根据任务配置跳过被排除的目录。

## Impact

- 后端：
  - 新增 Flyway 迁移，为 `task_config` 添加过滤规则字段。
  - 更新 `TaskConfig`、`TaskConfigDto`、`TaskConfigMapper.xml`。
  - 更新 `FileDiscoveryHandler` 和 `FileFilterHandler`。
  - 更新 `TaskExecutionService` 中当前直接筛选视频文件的位置，确保执行入口与 handler 过滤规则一致。
  - 更新任务日志写入逻辑，记录过滤跳过原因。
- 前端：
  - 更新任务创建/编辑弹窗，增加文件大小、文件名、目录名称过滤输入项。
  - 更新任务卡片展示已配置过滤规则。
- 兼容性：
  - 现有任务的新增字段为空；空字段表示不启用对应过滤规则。
  - 不新增外部依赖。
