## ADDED Requirements

### Requirement: 任务级刮削器选择
系统 SHALL 在任务配置中保存刮削器类型，并在任务执行时根据该类型选择在线刮削实现。

1. 支持的刮削器类型 SHALL 包含 `TMDB` 和 `JAV`
2. 未设置刮削器类型的既有任务 SHALL 按 `TMDB` 处理
3. `needScrap` 为 `false` 时 SHALL 不执行 `TMDB` 或 `JAV` 在线刮削
4. `needScrap` 为 `true` 时 SHALL 仅执行任务选择的一个刮削器

#### Scenario: 创建任务选择 TMDB 刮削器
- **WHEN** 用户创建任务并选择 `TMDB` 刮削器
- **THEN** 系统 SHALL 保存该任务的刮削器类型为 `TMDB`

#### Scenario: 创建任务选择 Jav 刮削器
- **WHEN** 用户创建任务并选择 `JAV` 刮削器
- **THEN** 系统 SHALL 保存该任务的刮削器类型为 `JAV`

#### Scenario: 执行 TMDB 任务
- **WHEN** 任务的 `needScrap` 为 `true` 且刮削器类型为 `TMDB`
- **THEN** 系统 SHALL 执行原有 TMDB 刮削流程

#### Scenario: 执行 Jav 任务
- **WHEN** 任务的 `needScrap` 为 `true` 且刮削器类型为 `JAV`
- **THEN** 系统 SHALL 执行 Jav 刮削流程

#### Scenario: 禁用任务刮削
- **WHEN** 任务的 `needScrap` 为 `false`
- **THEN** 系统 SHALL 不执行任何在线刮削器

## MODIFIED Requirements

### Requirement: 刮削作为 Fallback

系统 SHALL 将媒体刮削作为优先级最低的 Fallback 机制：

1. 仅当本地和 OpenList 都不存在对应文件时才执行任务选择的在线刮削器
2. 刮削成功后自动保存 NFO 和图片文件
3. 刮削失败时记录错误但不影响其他文件处理

#### Scenario: NFO 文件的 Fallback
- **WHEN** 本地和 OpenList 都不存在 NFO 文件
- **THEN** 系统 SHALL 执行任务选择的在线刮削器生成 NFO 文件

#### Scenario: 图片文件的 Fallback
- **WHEN** 本地和 OpenList 都不存在图片文件
- **THEN** 系统 SHALL 执行任务选择的在线刮削器下载图片

#### Scenario: 刮削成功保存文件
- **WHEN** 任务选择的在线刮削器刮削成功
- **THEN** 系统 SHALL 自动保存 NFO 和图片文件到本地目录

#### Scenario: 刮削失败继续处理
- **WHEN** 任务选择的在线刮削器刮削失败（API 错误、网络问题等）
- **THEN** 系统 SHALL 记录错误日志并继续处理其他文件

### Requirement: 刮削条件检查

系统 SHALL 在执行刮削前检查必要条件：

1. 刮削功能是否启用
2. 任务是否启用在线刮削
3. 任务选择的刮削器类型是否有效
4. `TMDB` 刮削器是否已配置 TMDB API Key
5. `TMDB` 刮削器是否满足置信度要求

#### Scenario: 检查刮削功能启用
- **WHEN** 系统配置中 `scraping.enabled` 为 `false`
- **THEN** 系统 SHALL 跳过所有在线刮削操作

#### Scenario: 检查任务刮削启用
- **WHEN** 任务配置中 `needScrap` 为 `false`
- **THEN** 系统 SHALL 跳过所有在线刮削操作

#### Scenario: 检查刮削器类型
- **WHEN** 任务配置中的刮削器类型不是 `TMDB` 或 `JAV`
- **THEN** 系统 SHALL 拒绝保存该任务配置

#### Scenario: 检查 TMDB API Key
- **WHEN** 任务选择 `TMDB` 刮削器且 TMDB API Key 未配置
- **THEN** 系统 SHALL 记录警告并跳过该文件的 TMDB 刮削

#### Scenario: Jav 刮削不要求 TMDB API Key
- **WHEN** 任务选择 `JAV` 刮削器
- **THEN** 系统 SHALL 不检查 TMDB API Key

#### Scenario: 置信度检查
- **WHEN** `TMDB` 刮削器的正则解析置信度低于 70%
- **THEN** 系统 SHALL 尝试使用 AI 识别或跳过 TMDB 刮削

#### Scenario: AI 识别增强
- **WHEN** `TMDB` 刮削器的正则解析置信度低且 AI 识别已启用
- **THEN** 系统 SHALL 使用 AI 辅助识别文件名

### Requirement: 刮削配置管理

系统 SHALL 管理刮削相关的配置选项：

1. 刮削功能开关
2. 是否生成 NFO
3. 是否下载海报
4. 是否下载背景图
5. AI 识别开关
6. 任务级刮削器类型

#### Scenario: 获取刮削配置
- **WHEN** 需要获取刮削配置
- **THEN** 系统 SHALL 从 `SystemConfigService` 返回配置映射

#### Scenario: NFO 生成配置
- **WHEN** 配置中 `generateNfo` 为 `true`
- **THEN** 系统 SHALL 在刮削时生成 NFO 文件

#### Scenario: 海报下载配置
- **WHEN** 配置中 `downloadPoster` 为 `true`
- **THEN** 系统 SHALL 在刮削时下载海报图片

#### Scenario: 背景图下载配置
- **WHEN** 配置中 `downloadBackdrop` 为 `true`
- **THEN** 系统 SHALL 在刮削时下载背景图片

#### Scenario: 读取任务刮削器类型
- **WHEN** 系统执行任务在线刮削
- **THEN** 系统 SHALL 从当前任务配置读取刮削器类型
