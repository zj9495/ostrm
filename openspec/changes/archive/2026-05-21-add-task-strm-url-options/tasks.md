## 1. 后端数据模型

- [x] 1.1 新增 Flyway 迁移，为 `task_config` 添加 `strm_url_replace_from`、`strm_url_replace_to` 和 `generate_sign` 字段，并让 `generate_sign` 默认值为 true。
- [x] 1.2 在 `TaskConfig` 和 `TaskConfigDto` 中添加 `strmUrlReplaceFrom`、`strmUrlReplaceTo` 和 `generateSign` 字段。
- [x] 1.3 更新 `TaskConfigMapper.xml` 的结果映射、基础字段列表、新增映射和更新映射，完整传递新字段。
- [x] 1.4 更新任务配置默认值处理：请求未提交 `generateSign` 时，新任务保持 `generateSign=true`；内容地址替换字段按提交值原样保存。

## 2. 后端 STRM URL 生成

- [x] 2.1 将任务 URL 选项从 `StrmGenerationHandler` 传入 STRM URL 生成流程。
- [x] 2.2 将当前无条件追加 `sign` 的逻辑改为由 `generateSign` 控制。
- [x] 2.3 在 `StrmFileService` 中新增任务级内容地址替换逻辑，在 OpenList `strmBaseUrl` 替换后，对最终 STRM 内容地址应用 `strmUrlReplaceFrom` -> `strmUrlReplaceTo`。
- [x] 2.4 任务级内容地址替换按精确字符串替换执行，不解析 URL 组件。

## 3. 前端任务管理

- [x] 3.1 在任务创建和编辑表单中添加 STRM 内容地址替换输入项。
- [x] 3.2 在任务创建和编辑表单中添加 `sign` 生成开关。
- [x] 3.3 创建和更新任务请求中提交 `strmUrlReplaceFrom`、`strmUrlReplaceTo` 和 `generateSign`。
- [x] 3.4 在任务详情区域展示已保存的 STRM URL 选项。

## 4. 验证

- [x] 4.1 验证任务创建、更新、列表和详情响应都包含新字段。
- [x] 4.2 验证配置 `http://host:port/d` -> `/mnt/...url=` 的任务会写入替换后的 STRM 内容地址。
- [x] 4.3 验证 `generateSign=false` 时写入的 STRM 内容不包含 `sign` 查询参数。
- [x] 4.4 验证未配置内容地址替换值的既有任务默认继续写入带 `sign` 的 STRM URL。
