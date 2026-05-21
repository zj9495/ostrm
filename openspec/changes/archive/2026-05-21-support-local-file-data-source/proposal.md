## Why

当前任务只能从 OpenList 发现媒体文件，已挂载到容器内的本地媒体目录无法直接作为 STRM 数据源。支持本地文件数据源后，用户可以在同一套任务配置和处理链中选择 OpenList 或本地目录，减少不必要的 OpenList 依赖。

## What Changes

- 在“添加/编辑配置”中增加数据源类型，支持 `OPENLIST` 和 `LOCAL`。
- OpenList 数据源继续使用 Base URL、Token、Base Path、STRM Base URL 和 URL 编码配置。
- 本地文件数据源配置只保存数据源类型，不要求输入本地路径；本地路径在任务配置中作为任务路径填写。
- 任务配置页面根据数据源类型切换任务路径输入方式：OpenList 使用现有路径输入与 OpenList 校验，本地文件使用下拉树级目录选择与本地目录校验。
- 后端增加本地文件目录树查询与本地任务路径校验接口。
- 文件发现支持按数据源类型从 OpenList API 或本地文件系统递归收集文件。

## Capabilities

### New Capabilities
- `data-source-configuration`: 管理 OpenList 与本地文件两种数据源配置、对应校验接口，以及任务配置中的数据源感知路径选择。

### Modified Capabilities
- `file-discovery`: 文件发现从仅支持 OpenList API 扩展为按数据源类型发现 OpenList 或本地文件。

## Impact

- 后端实体、DTO、Mapper、数据库迁移：为配置增加数据源类型字段，并让 OpenList 专属字段仅在 OpenList 模式下必填。
- 后端服务与控制器：增加本地目录树查询、路径校验，以及数据源类型驱动的任务执行分支。
- 文件处理链：`FileDiscoveryHandler` 需要在本地模式下生成与现有处理器兼容的文件模型。
- 前端首页配置弹窗与任务管理页：增加数据源类型选择、本地路径树选择、路径校验和展示。
- OpenAPI 文档与 Swagger 描述需要同步新的请求/响应字段与接口。
