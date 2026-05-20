# 设计说明

## 数据模型

在 `task_config` 表上增加独立字段，避免把多种过滤语义塞进一个 JSON 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `min_file_size_bytes` | BIGINT | 文件大小下限，包含边界 |
| `file_name_exclude_regex` | VARCHAR(500) | 文件名排除规则 |
| `directory_name_exclude_regex` | VARCHAR(500) | 父目录名称排除规则 |

DTO 字段名使用 camelCase，与数据库字段一一映射。正则字段为空字符串或 `null` 时表示该规则未启用；大小字段为 `null` 时表示该边界未启用。

## 过滤语义

文件必须同时满足所有已启用规则：

1. `minFileSizeBytes`: `file.size >= minFileSizeBytes`
2. `fileNameExcludeRegex`: `!Pattern.matcher(file.name).find()`
3. `directoryNameExcludeRegex`: 没有任何父目录名称匹配

如果启用了文件大小规则且 OpenList 返回的 `size` 为 `null`，该文件不满足大小规则。

## 处理位置

- `FileDiscoveryHandler`: 只处理 `directoryNameExcludeRegex`，在递归前跳过被排除目录。
- `FileFilterHandler`: 在视频扩展名过滤之后应用完整任务过滤规则，设置 `videoFiles`。
- `TaskExecutionService`: 当前存在直接从 `allFiles` 计算 `videoFiles` 的逻辑，需要改为复用 `FileFilterHandler` 的规则结果，避免入口不一致。

## 跳过原因日志

`FileFilterHandler` SHALL 在文件被任务配置过滤规则跳过时，向当前任务运行日志写入一条记录。日志内容至少包含：

1. 文件路径
2. 触发的过滤字段
3. 文件实际值
4. 配置规则值
5. 明确的跳过原因文本

建议原因格式：

- `文件大小小于 minFileSizeBytes: size={size}, minFileSizeBytes={minFileSizeBytes}`
- `文件名匹配 fileNameExcludeRegex: name={name}, fileNameExcludeRegex={fileNameExcludeRegex}`
- `目录名称匹配 directoryNameExcludeRegex: directory={directory}, directoryNameExcludeRegex={directoryNameExcludeRegex}`

`FileDiscoveryHandler` 按 `directoryNameExcludeRegex` 跳过目录递归时，SHALL 向任务日志写入目录路径和跳过原因。由于该目录不再递归扫描，系统 SHALL NOT 为目录内未枚举的文件补写文件级跳过日志。

## 校验

- 后端保存任务时校验：
  - `minFileSizeBytes >= 0`
  - 所有正则字段必须能被 `Pattern.compile()` 编译
- 前端只做输入约束和提示，后端作为最终校验来源。
