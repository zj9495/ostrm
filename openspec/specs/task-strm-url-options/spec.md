# task-strm-url-options Specification

## Purpose
TBD - created by archiving change add-task-strm-url-options. Update Purpose after archive.
## Requirements
### Requirement: 任务配置 STRM URL 选项字段

系统 SHALL 在任务配置中保存以下 STRM URL 生成选项：

1. `strmUrlReplaceFrom`
2. `strmUrlReplaceTo`
3. `generateSign`

`strmUrlReplaceFrom` 为空时，系统 SHALL 不启用任务级 STRM 内容地址替换。`generateSign` SHALL 控制该任务生成 STRM 内容 URL 时是否追加 `sign` 查询参数。

#### Scenario: 创建未配置内容地址替换的任务

- **WHEN** 用户创建任务且 `strmUrlReplaceFrom` 为空
- **THEN** 系统 SHALL 保存任务
- **AND** 该任务 SHALL 不启用任务级 STRM 内容地址替换

#### Scenario: 创建配置内容地址替换的任务

- **WHEN** 用户创建任务并设置 `strmUrlReplaceFrom` 为 `http://host:port/d` 且 `strmUrlReplaceTo` 为 `/mnt/...url=`
- **THEN** 系统 SHALL 保存该替换配置

#### Scenario: 创建未显式设置 sign 开关的任务

- **WHEN** 用户创建任务且未提交 `generateSign`
- **THEN** 系统 SHALL 将 `generateSign` 保存为 true

---

### Requirement: 任务配置接口传递 STRM URL 选项

系统 SHALL 在任务配置创建、更新、列表查询和详情查询接口中接收并返回 STRM URL 选项字段。

#### Scenario: 查询任务配置列表

- **WHEN** 用户查询任务配置列表
- **THEN** 响应 SHALL 包含每个任务的 `strmUrlReplaceFrom`、`strmUrlReplaceTo` 和 `generateSign`

#### Scenario: 更新任务配置

- **WHEN** 用户更新任务配置并提交 STRM URL 选项字段
- **THEN** 系统 SHALL 保存提交的字段值
- **AND** 后续查询 SHALL 返回更新后的字段值

---

### Requirement: STRM 内容地址替换

系统 SHALL 在生成 STRM 文件内容 URL 时，对 OpenList 配置级 Base URL 替换后的最终 STRM 内容地址应用任务级内容地址替换。替换 SHALL 使用 `strmUrlReplaceFrom` 和 `strmUrlReplaceTo` 对该地址进行精确字符串替换。

#### Scenario: 替换 STRM 内容地址前缀

- **WHEN** 任务配置 `strmUrlReplaceFrom` 为 `http://host:port/d` 且 `strmUrlReplaceTo` 为 `/mnt/...url=`
- **AND** 待写入 STRM 的内容地址包含 `http://host:port/d`
- **THEN** 写入 STRM 文件的内容地址 SHALL 将 `http://host:port/d` 替换为 `/mnt/...url=`

#### Scenario: 未配置内容地址替换

- **WHEN** 任务配置 `strmUrlReplaceFrom` 为空
- **THEN** 系统 SHALL 不执行任务级 STRM 内容地址替换

---

### Requirement: STRM 内容 URL sign 参数开关

系统 SHALL 根据任务配置 `generateSign` 决定是否在 STRM 内容 URL 中生成 `sign` 查询参数。

#### Scenario: 启用 sign 参数生成

- **WHEN** 任务配置 `generateSign` 为 true
- **AND** 当前 OpenList 文件提供 `sign`
- **THEN** 系统 SHALL 在 STRM 内容 URL 中追加 `sign` 查询参数

#### Scenario: 关闭 sign 参数生成

- **WHEN** 任务配置 `generateSign` 为 false
- **THEN** 系统 SHALL 不在 STRM 内容 URL 中追加 `sign` 查询参数

---

### Requirement: 前端任务管理 STRM URL 选项

前端 SHALL 在任务创建、编辑和展示区域提供 STRM URL 选项。

#### Scenario: 创建任务时配置 STRM URL 选项

- **WHEN** 用户在任务创建表单中填写内容地址替换字段并选择 sign 开关
- **THEN** 前端 SHALL 在创建任务请求中提交 `strmUrlReplaceFrom`、`strmUrlReplaceTo` 和 `generateSign`

#### Scenario: 编辑任务时回显 STRM URL 选项

- **WHEN** 用户编辑已有任务
- **THEN** 前端 SHALL 回显该任务保存的 `strmUrlReplaceFrom`、`strmUrlReplaceTo` 和 `generateSign`

