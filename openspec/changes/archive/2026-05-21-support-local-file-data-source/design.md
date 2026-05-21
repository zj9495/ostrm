## Context

配置与任务当前以 OpenList 为中心：`openlist_config` 保存 Base URL、Token、用户名等字段，任务通过 `openlist_config_id` 关联配置，`FileDiscoveryHandler` 固定调用 `OpenlistApiService` 递归获取目录内容。前端首页配置弹窗只支持 OpenList 凭据，任务配置页的任务路径是文本输入，并在保存前调用 `/api/openlist-config/validate-path` 校验 OpenList 路径。

本变更需要在不拆除现有 OpenList 流程的前提下，让同一套配置、任务和处理链支持本地文件目录。

## Goals / Non-Goals

**Goals:**
- 配置支持明确的数据源类型：`OPENLIST` 和 `LOCAL`。
- 本地数据源配置不要求输入本地路径，OpenList 专属字段只对 OpenList 数据源生效。
- 任务路径在本地模式下表示本地文件系统目录，并通过后端接口校验。
- 前端任务路径在本地模式下提供目录树级选择。
- 文件发现按数据源类型分别使用 OpenList API 或 Java NIO 遍历目录，并输出现有处理器可消费的文件模型。

**Non-Goals:**
- 不引入新的外部存储数据源。
- 不改变任务调度、任务运行记录、过滤规则和 STRM URL 任务级替换的既有语义。
- 不为本地文件数据源生成 OpenList `sign` 参数。
- 不实现浏览器端直接访问宿主机任意目录；本地目录以服务端进程可访问路径为准。

## Decisions

1. **保留现有 `openlist_config` 表与接口前缀，扩展为数据源配置。**
   - Rationale: 任务、前端路由和已有 API 都围绕配置 ID 工作，保留表和 `/api/openlist-config` 路径能最小化改动面。
   - Alternative: 新建 `data_source_config` 表并重命名 API。该方案命名更准确，但会触及更多迁移、路由和兼容性改动。

2. **新增 `sourceType` 字段，OpenList 字段按类型校验。**
   - `sourceType` 使用枚举值 `OPENLIST`、`LOCAL`。
   - `LOCAL` 配置不提交本地路径；本地路径只在任务配置的 `path` 字段中填写。
   - `OPENLIST` 配置必须提交 `baseUrl`、`token`，并通过现有 OpenList 校验得到 `username` 与 `basePath`。
   - Rationale: 避免用空字符串或占位值伪装不适用字段，数据库与 DTO 语义保持清楚。
   - Alternative: 将本地模式写入现有 `baseUrl/token/username` 字段。该方案字段语义不真实，后续分支会依赖隐含约定。

3. **本地目录树由后端提供，节点只暴露目录。**
   - 新接口按配置 ID 和可选父路径返回下一层本地目录节点；未传父路径时返回服务端可见的文件系统根节点。
   - 节点字段包含 `name`、`path`、`hasChildren`。
   - 任务路径选择只允许选中目录节点，保存时后端再次校验该路径存在且是目录。
   - Rationale: 后端拥有实际文件系统视角，也能统一做路径归属校验。
   - Alternative: 前端输入任意路径后只在提交时校验。该方案无法满足“下拉树级选择”。

4. **复用 `OpenlistApiService.OpenlistFile` 作为处理链文件模型，新增本地文件发现服务生成同结构对象。**
   - 本地目录遍历为目录设置 `type = "folder"`，为文件设置 `type = "file"`，并填充真实文件名、绝对路径和大小。
   - 本地文件的 `url` 使用规范化本地路径；`sign` 为空。
   - Rationale: 现有过滤、STRM 生成、NFO/图片/字幕处理都消费同一文件模型，复用模型可让变更集中在数据源适配层。
   - Alternative: 新建独立本地文件模型并改造所有处理器。该方案更纯粹，但会扩大本次变更范围。

5. **任务执行先读取配置，再按 `sourceType` 分派文件发现与下载路径。**
   - OpenList 模式继续调用现有 OpenList API。
   - 本地模式由本地文件服务列目录、读关联资源内容，并在 STRM 文件中写入本地文件路径。
   - Rationale: 数据源差异集中在发现和读取文件内容两类能力上，其余处理链保持任务级配置驱动。
   - Alternative: 为本地模式创建一条独立任务执行流程。该方案会复制过滤、刮削、清理和日志逻辑。

## Risks / Trade-offs

- [Risk] SQLite 对移除 `NOT NULL` 约束支持有限，迁移可能需要重建 `openlist_config` 表。→ Migration 使用临时表复制数据，并为既有记录写入 `source_type = 'OPENLIST'`。
- [Risk] 本地目录树会展示服务端进程可见目录。→ 接口只返回目录节点元数据，任务保存与执行只接受存在且为目录的本地任务路径。
- [Risk] 本地模式下已有 URL 编码、STRM Base URL 和 `generateSign` 对 OpenList URL 的含义不完全适用。→ UI 在本地模式下隐藏 OpenList 专属字段；后端按 `sourceType` 执行对应字段校验和 STRM 内容生成。
- [Risk] 现有处理器部分下载关联文件时直接依赖 `OpenlistApiService.getFileContent()`。→ 引入数据源文件内容读取抽象或在调用点按配置类型选择本地读取服务，保证本地 NFO/图片/字幕可复制。

## Migration Plan

1. 新增数据库迁移：为既有配置标记 `OPENLIST`，并允许 OpenList 专属字段在本地模式为空。
2. 扩展实体、DTO、Mapper 和校验逻辑，确保创建/更新按 `sourceType` 执行字段校验。
3. 增加本地目录树和本地任务路径校验接口。
4. 增加本地文件发现与文件内容读取能力，并接入任务执行。
5. 更新前端配置弹窗、配置卡片和任务配置弹窗。
6. 如需回滚，先确认没有 `LOCAL` 配置；删除本地相关字段和接口后，恢复 OpenList-only 校验。

## Open Questions

- 本地模式 STRM 内容是否必须写入绝对路径，还是需要支持任务级替换后写入播放器可访问路径？当前设计按“任务路径就是本地文件路径”处理，写入本地文件路径。
