## MODIFIED Requirements

### Requirement: 数据源类型配置字段
系统 SHALL 在配置中保存数据源类型，并使用明确字段表达不同数据源的必填信息：

1. `sourceType` SHALL 仅允许 `OPENLIST` 或 `LOCAL`
2. 所有配置 SHALL 保存用户手动提交的配置名称
3. `OPENLIST` 配置 SHALL 保存 `baseUrl`、`token`、`basePath`、`username`、`strmBaseUrl` 和 `enableUrlEncoding`
4. `LOCAL` 配置 SHALL 保存 `sourceType` 和 `username`
5. `LOCAL` 配置 SHALL NOT 要求提交 `baseUrl`、`token`、`basePath` 或本地路径

#### Scenario: 创建 OpenList 数据源配置
- **WHEN** 用户选择数据源类型为 `OPENLIST`
- **AND** 用户提交有效的配置名称、`baseUrl` 和 `token`
- **THEN** 系统 SHALL 保存该配置为 OpenList 数据源
- **AND** 系统 SHALL 返回 `sourceType`、`baseUrl`、`basePath`、`username`、`strmBaseUrl` 和 `enableUrlEncoding`
- **AND** 返回的 `username` SHALL 为用户提交的配置名称

#### Scenario: 创建本地文件数据源配置
- **WHEN** 用户选择数据源类型为 `LOCAL`
- **AND** 用户提交有效的配置名称
- **THEN** 系统 SHALL 保存该配置为本地文件数据源
- **AND** 系统 SHALL 返回 `sourceType` 和 `username`
- **AND** 返回的 `username` SHALL 为用户提交的配置名称

#### Scenario: 本地配置不提交 OpenList 字段
- **WHEN** 用户创建 `LOCAL` 配置
- **AND** 请求包含有效配置名称
- **AND** 请求未包含 `baseUrl`、`token`、`basePath` 和本地路径
- **THEN** 系统 SHALL 接受该请求

### Requirement: 数据源配置校验
系统 SHALL 根据数据源类型执行对应校验：

1. 所有配置 SHALL 要求用户提交非空配置名称
2. `OPENLIST` 配置 SHALL 使用 OpenList 配置校验接口验证 `baseUrl` 和 `token`
3. `LOCAL` 配置 SHALL 校验 `sourceType` 和配置名称
4. 数据源类型以外的字段 SHALL NOT 参与该类型的必填校验

#### Scenario: OpenList 凭据无效
- **WHEN** 用户创建或更新 `OPENLIST` 配置
- **AND** OpenList 校验失败
- **THEN** 系统 SHALL 拒绝保存配置

#### Scenario: 本地配置不校验本地路径
- **WHEN** 用户创建或更新 `LOCAL` 配置
- **AND** 请求提交有效配置名称
- **AND** 请求未提交本地路径
- **THEN** 系统 SHALL 接受该请求

#### Scenario: 创建配置缺少配置名称
- **WHEN** 用户创建 `OPENLIST` 或 `LOCAL` 配置
- **AND** 请求未提交有效配置名称
- **THEN** 系统 SHALL 拒绝保存配置
- **AND** 系统 SHALL NOT 自动生成配置名称

### Requirement: 前端数据源感知配置界面
前端 SHALL 在配置管理和任务配置界面按数据源类型展示对应字段：

1. 添加/编辑配置弹窗 SHALL 提供数据源类型选择
2. 添加/编辑配置弹窗 SHALL 提供配置名称输入项
3. 选择 `OPENLIST` 时 SHALL 展示配置名称和 OpenList 专属字段
4. 选择 `LOCAL` 时 SHALL 展示配置名称，隐藏 OpenList 专属字段，且 SHALL NOT 展示本地路径输入字段
5. 配置列表 SHALL 展示每个配置的数据源类型和配置名称
6. 任务配置弹窗 SHALL 根据关联配置的数据源类型展示任务路径输入控件

#### Scenario: 添加配置时选择 OpenList
- **WHEN** 用户在添加配置弹窗选择 `OPENLIST`
- **THEN** 前端 SHALL 展示配置名称、Base URL、Token、STRM Base URL 和 URL 编码字段

#### Scenario: 添加配置时选择本地文件
- **WHEN** 用户在添加配置弹窗选择 `LOCAL`
- **THEN** 前端 SHALL 展示配置名称
- **AND** 前端 SHALL 隐藏 Base URL、Token、STRM Base URL、URL 编码和本地路径字段

#### Scenario: 任务配置使用本地路径树
- **WHEN** 用户进入 `LOCAL` 配置的任务管理页
- **THEN** 任务路径控件 SHALL 为本地目录树选择器
