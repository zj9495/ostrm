## MODIFIED Requirements

### Requirement: 递归目录遍历

系统 SHALL 按任务关联配置的数据源类型递归遍历指定目录，收集所有文件和子目录信息：

1. 从任务配置的 `path` 起始目录开始
2. `OPENLIST` 数据源 SHALL 通过 OpenList API 获取每个目录的内容列表
3. `LOCAL` 数据源 SHALL 通过本地文件系统获取每个目录的内容列表
4. 对于子目录，先应用任务配置的目录名称排除规则
5. 未被排除的子目录，递归调用获取其内容
6. 对于文件，记录文件元数据（名称、路径、URL/本地路径、签名等）

#### Scenario: OpenList 从根目录开始遍历
- **WHEN** 任务配置的 path 为 `/movies`
- **AND** 任务关联配置的 `sourceType` 为 `OPENLIST`
- **THEN** 系统 SHALL 从 OpenList 的 `/movies` 目录开始遍历

#### Scenario: 本地文件从目录开始遍历
- **WHEN** 任务配置的 path 为 `/media/movies`
- **AND** 任务关联配置的 `sourceType` 为 `LOCAL`
- **THEN** 系统 SHALL 从本地文件系统的 `/media/movies` 目录开始遍历

#### Scenario: 遍历子目录
- **WHEN** 当前目录包含子目录 `/movies/Action`
- **THEN** 系统 SHALL 递归遍历 `/movies/Action` 目录

#### Scenario: 跳过被目录名称排除规则匹配的子目录
- **WHEN** 任务配置了 `directoryNameExcludeRegex`
- **AND** 当前目录包含名称匹配该正则的子目录
- **THEN** 系统 SHALL 跳过该子目录递归
- **AND** 系统 SHALL 向本次任务日志写入目录路径和跳过原因
- **AND** 系统 SHALL NOT 为该目录内未枚举的文件补写文件级跳过日志

#### Scenario: 收集文件元数据
- **WHEN** 遍历过程中发现文件 `movie.mp4`
- **THEN** 系统 SHALL 记录文件名、完整路径、URL 或本地文件路径，以及数据源提供的签名参数

#### Scenario: 处理空目录
- **WHEN** 遇到空目录
- **THEN** 系统 SHALL 跳过该目录并继续处理其他内容

---

### Requirement: 文件模型定义

系统 SHALL 定义文件模型来表示数据源中的文件/目录，包含以下属性：

1. `name` - 文件/目录名称
2. `type` - 类型（"file" 或 "folder"）
3. `path` - 数据源内完整路径；本地模式为规范化本地路径
4. `url` - STRM 内容可使用的地址；OpenList 模式为文件访问 URL，本地模式为本地文件路径
5. `sign` - 签名参数；仅当数据源提供签名参数时填充
6. `size` - 文件大小；当数据源提供文件大小时填充

#### Scenario: 创建 OpenList 文件模型
- **WHEN** 从 OpenList API 响应中解析文件信息
- **THEN** 系统 SHALL 创建文件模型并填充 OpenList 返回的属性

#### Scenario: 创建本地文件模型
- **WHEN** 从本地文件系统读取文件信息
- **THEN** 系统 SHALL 创建文件模型并填充本地文件名、类型、规范化路径和文件大小

#### Scenario: 文件类型判断
- **WHEN** 数据源条目表示目录
- **THEN** 文件类型 SHALL 设置为 "folder"

#### Scenario: 设置签名参数
- **WHEN** 数据源响应中包含 sign 字段
- **THEN** 系统 SHALL 将 sign 值保存到文件模型中

#### Scenario: 本地文件不设置签名参数
- **WHEN** 数据源类型为 `LOCAL`
- **THEN** 系统 SHALL NOT 为文件模型设置 sign 值

## ADDED Requirements

### Requirement: 本地文件系统目录读取

系统 SHALL 为 `LOCAL` 数据源读取本地文件系统目录内容：

1. 目录读取 SHALL 使用服务端进程可访问的路径
2. 目录读取 SHALL 只从任务路径开始
3. 目录读取 SHALL 将普通文件和目录转换为文件模型
4. 目录读取 SHALL 保留文件大小信息
5. 本地路径 SHALL 使用规范化后的绝对路径

#### Scenario: 读取本地目录内容
- **WHEN** 任务路径为 `/media/movies`
- **AND** 该目录包含 `A.mkv` 和 `Series`
- **THEN** 系统 SHALL 返回 `A.mkv` 文件模型和 `Series` 目录模型

#### Scenario: 本地目录读取失败
- **WHEN** 系统无法读取任务路径对应目录
- **THEN** 文件发现 SHALL 返回失败结果
- **AND** 任务 SHALL 记录该目录读取失败原因

---

### Requirement: 本地文件任务生成 STRM 内容

系统 SHALL 在 `LOCAL` 数据源任务中使用本地文件路径生成 STRM 文件内容：

1. 视频文件的 STRM 内容 SHALL 使用文件模型中的本地文件路径
2. 本地模式 SHALL NOT 追加 OpenList sign 查询参数
3. 本地模式 SHALL 继续应用任务级 STRM 内容地址替换字段

#### Scenario: 本地视频生成 STRM
- **WHEN** 本地模式发现视频文件 `/media/movies/A.mkv`
- **THEN** 系统 SHALL 为该视频生成 STRM 文件
- **AND** STRM 文件内容 SHALL 包含 `/media/movies/A.mkv`

#### Scenario: 本地视频不追加 sign
- **WHEN** 本地模式生成 STRM 文件内容
- **THEN** 系统 SHALL NOT 在 STRM 内容中追加 `sign` 查询参数

#### Scenario: 本地模式应用任务级地址替换
- **WHEN** 本地模式任务配置了 `strmUrlReplaceFrom` 和 `strmUrlReplaceTo`
- **AND** 本地文件路径包含 `strmUrlReplaceFrom`
- **THEN** 系统 SHALL 对写入 STRM 的本地文件路径执行精确字符串替换
