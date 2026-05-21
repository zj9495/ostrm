## Why

当前任务生成的 STRM 内容只支持 OpenList 配置级 `strmBaseUrl` 替换，无法针对单个任务把最终 STRM 内容地址中的指定字符串替换为另一个字符串。与此同时，`StrmGenerationHandler` 现在会固定追加 `sign` 查询参数，用户无法按任务关闭该参数生成。

## What Changes

- 任务配置新增 STRM 内容地址替换设置，支持把生成地址中的指定字符串替换为另一个字符串，例如将 `http://host:port/d` 替换为 `/mnt/...url=`。
- 任务配置新增是否生成 `sign` 查询参数的开关。
- 任务创建、更新、查询接口包含上述新配置字段。
- STRM 生成时按任务配置处理 URL：先沿用现有 OpenList 配置级 Base URL 替换，再应用任务级内容地址替换，再按开关决定是否追加 `sign` 参数。
- 前端任务管理页面在创建和编辑任务时提供对应配置项，并展示已保存配置。

## Capabilities

### New Capabilities

- `task-strm-url-options`: 任务支持配置 STRM 内容地址替换规则和 `sign` 查询参数生成开关。

### Modified Capabilities

- 无。

## Impact

- 后端：
  - 新增 Flyway 迁移，为 `task_config` 增加任务级 STRM 内容地址替换和 `sign` 开关字段。
  - 更新 `TaskConfig`、`TaskConfigDto`、`TaskConfigMapper.xml`、任务配置校验和默认值。
  - 更新 `StrmGenerationHandler` 和 `StrmFileService` 的 URL 处理流程，使任务配置参与 STRM 内容 URL 生成。
- 前端：
  - 更新 `frontend/app/pages/task-management/[id].vue` 的任务创建、编辑、展示和提交数据。
- 兼容性：
  - 现有任务迁移后默认保持当前行为：不做任务级内容地址替换，并继续生成 `sign` 查询参数。
  - 不新增外部依赖。
