## Why

新增数据源配置时，配置名称目前依赖系统自动生成或远端返回的用户名，用户无法按自己的媒体库、用途或环境命名配置。支持手动设置配置名称可以让配置列表、任务管理入口和后续维护更清晰。

## What Changes

- 新增配置弹窗提供必填的配置名称输入项。
- 创建配置时前端 SHALL 将用户填写的配置名称提交给后端。
- 后端创建配置时 SHALL 保存用户提交的配置名称，并 SHALL NOT 为缺失名称自动生成配置名称。
- 配置列表和任务管理中展示用户设置的配置名称。

## Capabilities

### New Capabilities

### Modified Capabilities

- `data-source-configuration`: 数据源配置创建时由用户手动提交并保存配置名称。

## Impact

- 前端配置管理页面：新增/编辑配置表单、配置卡片展示和提交载荷。
- 后端 OpenList 配置 DTO、服务校验和创建逻辑。
- 数据库沿用现有 `openlist_config.username` 字段存储配置名称，不新增表或迁移。
