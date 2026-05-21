## ADDED Requirements

### Requirement: 数据源类型配置字段
系统 SHALL 在配置中保存数据源类型，并使用明确字段表达不同数据源的必填信息：

1. `sourceType` SHALL 仅允许 `OPENLIST` 或 `LOCAL`
2. `OPENLIST` 配置 SHALL 保存 `baseUrl`、`token`、`basePath`、`username`、`strmBaseUrl` 和 `enableUrlEncoding`
3. `LOCAL` 配置 SHALL 保存 `sourceType`
4. `LOCAL` 配置 SHALL NOT 要求提交 `baseUrl`、`token`、`basePath`、`username` 或本地路径

#### Scenario: 创建 OpenList 数据源配置
- **WHEN** 用户选择数据源类型为 `OPENLIST`
- **AND** 用户提交有效的 `baseUrl` 和 `token`
- **THEN** 系统 SHALL 保存该配置为 OpenList 数据源
- **AND** 系统 SHALL 返回 `sourceType`、`baseUrl`、`basePath`、`username`、`strmBaseUrl` 和 `enableUrlEncoding`

#### Scenario: 创建本地文件数据源配置
- **WHEN** 用户选择数据源类型为 `LOCAL`
- **THEN** 系统 SHALL 保存该配置为本地文件数据源
- **AND** 系统 SHALL 返回 `sourceType`

#### Scenario: 本地配置不提交 OpenList 字段
- **WHEN** 用户创建 `LOCAL` 配置
- **AND** 请求未包含 `baseUrl`、`token`、`basePath`、`username` 和本地路径
- **THEN** 系统 SHALL 接受该请求

---

### Requirement: 数据源配置校验
系统 SHALL 根据数据源类型执行对应校验：

1. `OPENLIST` 配置 SHALL 使用 OpenList 配置校验接口验证 `baseUrl` 和 `token`
2. `LOCAL` 配置 SHALL 只校验 `sourceType`
3. 数据源类型以外的字段 SHALL NOT 参与该类型的必填校验

#### Scenario: OpenList 凭据无效
- **WHEN** 用户创建或更新 `OPENLIST` 配置
- **AND** OpenList 校验失败
- **THEN** 系统 SHALL 拒绝保存配置

#### Scenario: 本地配置不校验本地路径
- **WHEN** 用户创建或更新 `LOCAL` 配置
- **AND** 请求未提交本地路径
- **THEN** 系统 SHALL 接受该请求

---

### Requirement: 本地目录树查询
系统 SHALL 为本地文件数据源提供目录树级查询接口，用于任务路径选择：

1. 接口 SHALL 按配置 ID 查询本地数据源配置
2. 接口 SHALL 接收可选父目录路径
3. 接口 SHALL 返回父目录下一层目录节点
4. 每个目录节点 SHALL 包含 `name`、`path` 和 `hasChildren`
5. 接口 SHALL 只返回目录，不返回普通文件
6. 未提交父目录路径时，接口 SHALL 返回服务端可见的文件系统根节点

#### Scenario: 查询本地文件系统根节点
- **WHEN** 用户打开 `LOCAL` 配置的任务路径选择器
- **THEN** 前端 SHALL 请求该配置的本地目录根节点
- **AND** 系统 SHALL 返回服务端可见的文件系统根节点

#### Scenario: 展开本地目录节点
- **WHEN** 用户展开目录节点 `/media/movies`
- **THEN** 前端 SHALL 请求该目录的下一层目录节点
- **AND** 系统 SHALL 返回 `/media/movies` 下的目录列表

#### Scenario: 查询无效父目录
- **WHEN** 用户请求的父目录不存在或不是目录
- **THEN** 系统 SHALL 拒绝该目录树查询

---

### Requirement: 任务路径按数据源类型选择与校验
系统 SHALL 在任务配置中按关联配置的数据源类型处理任务路径：

1. `OPENLIST` 任务路径 SHALL 表示 OpenList 目录路径
2. `LOCAL` 任务路径 SHALL 表示本地文件系统目录路径
3. 创建或更新任务时，系统 SHALL 根据关联配置的数据源类型校验任务路径
4. `LOCAL` 任务路径 SHALL 存在且为目录

#### Scenario: OpenList 任务路径校验
- **WHEN** 用户为 `OPENLIST` 配置创建任务
- **AND** 任务路径为 `/movies`
- **THEN** 系统 SHALL 使用 OpenList 路径校验确认该路径存在且为目录

#### Scenario: 本地任务路径树级选择
- **WHEN** 用户为 `LOCAL` 配置创建任务
- **THEN** 前端 SHALL 使用本地目录树选择器填写任务路径
- **AND** 任务路径 SHALL 为用户选中的本地目录路径

#### Scenario: 本地任务路径保存校验
- **WHEN** 用户保存 `LOCAL` 任务
- **AND** 任务路径存在且为目录
- **THEN** 系统 SHALL 保存任务配置

#### Scenario: 本地任务路径非法
- **WHEN** 用户保存 `LOCAL` 任务
- **AND** 任务路径不存在或不是目录
- **THEN** 系统 SHALL 拒绝保存任务配置

---

### Requirement: 前端数据源感知配置界面
前端 SHALL 在配置管理和任务配置界面按数据源类型展示对应字段：

1. 添加/编辑配置弹窗 SHALL 提供数据源类型选择
2. 选择 `OPENLIST` 时 SHALL 展示 OpenList 专属字段
3. 选择 `LOCAL` 时 SHALL 隐藏 OpenList 专属字段，且 SHALL NOT 展示本地路径输入字段
4. 配置列表 SHALL 展示每个配置的数据源类型
5. 任务配置弹窗 SHALL 根据关联配置的数据源类型展示任务路径输入控件

#### Scenario: 添加配置时选择 OpenList
- **WHEN** 用户在添加配置弹窗选择 `OPENLIST`
- **THEN** 前端 SHALL 展示 Base URL、Token、STRM Base URL 和 URL 编码字段

#### Scenario: 添加配置时选择本地文件
- **WHEN** 用户在添加配置弹窗选择 `LOCAL`
- **THEN** 前端 SHALL 隐藏 Base URL、Token、STRM Base URL、URL 编码和本地路径字段

#### Scenario: 任务配置使用本地路径树
- **WHEN** 用户进入 `LOCAL` 配置的任务管理页
- **THEN** 任务路径控件 SHALL 为本地目录树选择器
