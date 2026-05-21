## Context

数据源配置当前使用 `openlist_config.username` 作为列表中的显示名称和查重字段。OpenList 配置创建时前端会调用校验接口并把远端返回的 `username` 合并进保存请求；LOCAL 配置创建时后端会在缺失 `username` 时生成 `local_<timestamp>`。这让用户无法在新增配置时设置便于识别的配置名称。

该变更只调整配置名称的输入、提交和保存语义，不改变数据源类型、任务路径、OpenList 凭据校验或 STRM URL 生成行为。

## Goals / Non-Goals

**Goals:**

- 新增配置时由用户手动输入配置名称。
- OpenList 和 LOCAL 数据源都使用同一配置名称输入。
- 后端保存请求中的配置名称，并在缺失时拒绝创建。
- 保持现有数据库字段，不引入迁移。

**Non-Goals:**

- 不新增独立 `displayName` 数据库列。
- 不保留自动生成本地配置名称。
- 不改变 OpenList 校验接口的用途；它仍只验证 `baseUrl` 和 `token` 并返回 OpenList 侧信息。
- 不实现批量重命名或配置名称历史迁移。

## Decisions

1. 继续使用 `openlist_config.username` 存储配置名称。
   - 现有前端、DTO、Mapper 和索引已经围绕 `username` 工作，最小充分方案是改变该字段的业务含义为“配置名称”。
   - 备选方案是新增 `name` 或 `displayName` 字段，但这需要数据库迁移和更多映射改动，本次需求不需要。

2. 前端保存 OpenList 配置时不再用校验接口返回的 `username` 覆盖用户输入的配置名称。
   - 校验接口返回的 OpenList 用户信息不等同于用户想要的配置名称。
   - 保存请求 SHALL 携带用户填写的 `username`，校验结果只用于需要保存的 OpenList 路径信息。

3. 后端创建配置时统一要求 `username` 有文本内容。
   - LOCAL 配置不再自动生成名称。
   - OPENLIST 和 LOCAL 配置都使用相同的唯一性检查，避免列表中出现同名配置。

4. 前端显示文案面向用户使用“配置名称”，内部字段名暂用 `username`。
   - 这样可以减少接口和数据库改动，同时把用户可见语义调整到需求要求的配置名称。

## Risks / Trade-offs

- 现有数据中可能已有 `local_<timestamp>` 名称 → 本变更不迁移既有名称，用户可通过编辑配置调整名称。
- API 字段名仍是 `username` → 前端文案和 OpenAPI 描述需要明确其用户可见含义为配置名称，避免再把它当作 OpenList 账号。
- 同名配置会被拒绝 → 沿用现有 `selectByUsername` 查重逻辑，错误信息应改为配置名称冲突。
