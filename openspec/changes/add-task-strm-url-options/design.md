## Context

任务 STRM 生成目前由 `StrmGenerationHandler` 组装文件 URL，并调用 `StrmFileService.generateStrmFile()` 写入 STRM 文件。现有行为有两个限制：

- URL 替换只来自 OpenList 配置级 `strmBaseUrl`，任务无法声明自己的 STRM 内容地址替换。
- `StrmGenerationHandler` 当前固定把 `OpenlistFile.sign` 追加成 `sign` 查询参数，任务无法关闭。

本次变更是任务配置能力扩展，涉及数据库字段、DTO/API、前端表单和 STRM URL 生成流程。

## Goals / Non-Goals

**Goals:**

- 在任务配置上保存 STRM 内容地址替换字段：`strmUrlReplaceFrom` 和 `strmUrlReplaceTo`。
- 在任务配置上保存 `generateSign` 开关，控制是否生成 `sign` 查询参数。
- 创建、更新、查询任务时完整传递这些字段。
- STRM 内容 URL 生成使用明确顺序：OpenList 配置级 Base URL 替换、任务级内容地址替换、按 `generateSign` 追加 `sign`、现有 URL 编码处理。
- 现有任务迁移后保持当前行为：不做任务级内容地址替换，继续生成 `sign` 查询参数。

**Non-Goals:**

- 不新增多条替换规则、正则替换、通配符替换或表达式语言。
- 不改变 STRM 文件输出目录 `strmPath` 的含义。
- 不改变 OpenList 配置级 `strmBaseUrl` 语义。
- 不通过解析已生成 STRM 文件来补齐或迁移内容。

## Decisions

1. 使用两个字符串字段表示任务级替换：`strmUrlReplaceFrom` 和 `strmUrlReplaceTo`。
   - Rationale: 用户需求是“把最终生成地址中的指定字符串替换为另一个字符串”，单组精确字符串替换是最小充分模型。
   - Alternative considered: 保存 JSON 规则数组。该方案支持多规则但超出当前需求，会引入额外校验和 UI 状态。

2. 任务级替换作用于 OpenList `strmBaseUrl` 替换后的完整 STRM 内容地址字符串。
   - Rationale: 用户需要把 `http://host:port/d` 这类最终地址前缀替换为本地挂载地址前缀，完整字符串精确替换与保存字段一一对应。
   - Alternative considered: 只对 URL path 执行替换。该方案无法匹配包含 scheme 和 host 的配置值。

3. `strmUrlReplaceFrom` 为空时不启用任务级替换；`strmUrlReplaceTo` 使用用户保存的原始字符串。
   - Rationale: 空查找串没有明确路径匹配语义；替换目标应完全按用户配置写入，不增加默认值或推断。
   - Alternative considered: 当 `strmUrlReplaceTo` 为空时自动使用原地址片段。该行为不是用户配置，属于兜底推断。

4. 使用 `generateSign` 控制 `sign` 参数，数据库默认值为 true。
   - Rationale: true 能保持现有任务行为，false 明确表示本任务不生成 `sign` 查询参数。
   - Alternative considered: 使用 OpenList 配置级开关。用户提出的是“任务应当支持配置”，任务级字段更贴近需求。

5. URL 处理顺序为 Base URL 替换 -> 内容地址替换 -> `sign` 参数 -> URL 编码。
   - Rationale: Base URL 替换先确定最终内容地址；任务级替换处理该地址中的精确字符串；`sign` 最后作为查询参数参与现有编码流程。
   - Alternative considered: 先追加 `sign` 再替换内容地址。该方案容易把内容地址替换逻辑和已有 query 混在一起。

## Risks / Trade-offs

- [Risk] 任务内容地址替换配置错误会生成不可播放的 STRM URL。→ Mitigation: 前端文案和后端字段命名明确“STRM 内容地址替换”，实现不自动纠正用户输入。
- [Risk] 旧数据没有新字段。→ Mitigation: Flyway 迁移为 `generate_sign` 设置默认值 true，替换字段为空。
- [Risk] 完整地址替换可能影响匹配到的 query 或 fragment。→ Mitigation: 替换发生在追加 `sign` 之前，且只执行用户配置的精确字符串替换。
