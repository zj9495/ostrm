# file-processing-chain Specification

## Purpose
TBD - created by archiving change optimize-file-processing-pipeline. Update Purpose after archive.
## Requirements
### Requirement: Handler 接口定义

系统 SHALL 定义 `FileProcessorHandler` 接口，该接口包含以下方法：

1. `ProcessingResult process(FileProcessingContext context)` - 处理文件并返回结果
2. `int getOrder()` - 返回处理器执行顺序（数值越小越先执行）
3. `Set<FileType> getHandledTypes()` - 返回处理器支持的文件类型

#### Scenario: Handler 处理文件
- **WHEN** 调用 `process()` 方法并传入有效的 `FileProcessingContext`
- **THEN** 处理器 SHALL 执行文件处理逻辑并返回 `ProcessingResult`

#### Scenario: Handler 获取执行顺序
- **WHEN** 调用 `getOrder()` 方法
- **THEN** 处理器 SHALL 返回表示执行顺序的整数值

#### Scenario: Handler 获取支持的文件类型
- **WHEN** 调用 `getHandledTypes()` 方法
- **THEN** 处理器 SHALL 返回其支持的文件类型集合

---

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

### Requirement: 文件类型枚举

系统 SHALL 定义 `FileType` 枚举，包含以下类型：

1. `VIDEO` - 视频文件
2. `NFO` - NFO 文件
3. `IMAGE` - 图片文件
4. `SUBTITLE` - 字幕文件
5. `ALL` - 所有类型

#### Scenario: 视频文件类型识别
- **WHEN** 文件扩展名为 `.mp4`、`.mkv`、`.avi` 等
- **THEN** 文件类型 SHALL 被识别为 `FileType.VIDEO`

#### Scenario: NFO 文件类型识别
- **WHEN** 文件扩展名为 `.nfo`
- **THEN** 文件类型 SHALL 被识别为 `FileType.NFO`

#### Scenario: 图片文件类型识别
- **WHEN** 文件扩展名为 `.jpg`、`.png`、`.webp` 等
- **THEN** 文件类型 SHALL 被识别为 `FileType.IMAGE`

#### Scenario: 字幕文件类型识别
- **WHEN** 文件扩展名为 `.srt`、`.ass`、`.vtt` 等
- **THEN** 文件类型 SHALL 被识别为 `FileType.SUBTITLE`

---

### Requirement: 处理上下文定义

系统 SHALL 定义 `FileProcessingContext` 类，用于在 Handler 之间传递处理上下文，包含以下属性：

1. `openlistConfig` - OpenList 配置
2. `taskConfig` - 任务配置
3. `currentFile` - 当前处理的文件
4. `relativePath` - 相对路径
5. `saveDirectory` - 保存目录
6. `directoryFiles` - 目录文件列表
7. `attributes` - 扩展属性
8. `stats` - 统计数据

#### Scenario: 创建处理上下文
- **WHEN** 初始化文件处理流程
- **THEN** 系统 SHALL 创建 `FileProcessingContext` 并填充必要配置信息

#### Scenario: 上下文传递到处理器
- **WHEN** 执行 `FileProcessorChain.execute()`
- **THEN** 上下文对象 SHALL 被传递到每个 Handler 的 `process()` 方法

#### Scenario: 上下文包含文件列表
- **WHEN** 处理视频文件时
- **THEN** 上下文 SHALL 包含该视频文件所在目录的所有文件列表

---

### Requirement: 处理链执行器

系统 SHALL 定义 `FileProcessorChain` 类，负责按顺序执行所有注册的 Handler：

1. 按 `getOrder()` 返回值排序
2. 依次调用每个 Handler 的 `process()` 方法
3. 收集并返回最终处理结果

#### Scenario: 处理链按顺序执行
- **WHEN** 调用 `FileProcessorChain.execute()` 方法
- **THEN** Handler SHALL 按 `getOrder()` 返回值从小到大依次执行

#### Scenario: 处理链跳过不支持的文件类型
- **WHEN** Handler 不支持当前文件类型
- **THEN** 处理链 SHALL 跳过该 Handler 继续执行下一个

#### Scenario: 处理链汇总处理结果
- **WHEN** 所有 Handler 执行完成
- **THEN** 处理链 SHALL 返回整体处理结果

#### Scenario: 处理链记录处理统计
- **WHEN** 处理链执行过程中
- **THEN** 系统 SHALL 统计成功、跳过、失败的文件数量

