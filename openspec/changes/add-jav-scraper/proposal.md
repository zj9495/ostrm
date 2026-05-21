## Why

当前任务只能通过原有 TMDB 刮削流程生成影视元数据，无法面向 JAV 文件按番号、多站点数据源和成人影片 NFO 字段生成更贴合媒体库识别的结果。新增 Jav 刮削器可以让用户在创建任务时按内容类型选择刮削来源，并复刻 JavSP 的番号识别、多站点抓取、字段汇总和图片/NFO 生成能力。

## What Changes

- 任务配置新增刮削器选择：用户创建或编辑任务时，可以选择原有 TMDB 刮削器或 Jav 刮削器。
- 保持 `needScrap=false` 时不执行任何在线刮削；`needScrap=true` 时按任务选择的刮削器执行。
- 新增 Jav 刮削能力，按照 `/Users/zj9495/code/JavSP` 的核心流程实现：
  - 从文件路径或文件名识别番号、CID、FC2、GETCHU、GYUTTO 等类型。
  - 按类型选择站点抓取器集合，抓取并汇总标题、番号、演员、发行日期、时长、制作商、发行商、系列、分类、评分、简介、封面和剧照等字段。
  - 生成 Kodi 兼容的 JAV NFO，并下载 fanart/poster 图片。
- Jav 刮削器使用 Ostrm 当前任务输出目录，不移动或重命名原始媒体文件。
- 原有 TMDB 刮削行为作为 `TMDB` 刮削器继续可用，并作为现有任务的兼容默认值。

## Capabilities

### New Capabilities
- `jav-scraping`: Jav 刮削器的番号识别、站点抓取、字段汇总、JAV NFO 生成和图片下载行为。

### Modified Capabilities
- `media-scraping`: 任务级刮削器选择，以及原有 TMDB 刮削器与新增 Jav 刮削器的执行分派。

## Impact

- 后端任务配置：`TaskConfig` / `TaskConfigDto` / MyBatis 映射 / Flyway 迁移需要新增刮削器类型字段。
- 后端刮削服务：需要抽象现有 TMDB 刮削实现，并新增 Jav 刮削服务、Jav 数据模型、站点客户端、NFO 生成和图片下载集成。
- 前端任务管理页：任务创建、编辑、列表展示需要增加刮削器选择控件。
- 系统配置：需要新增 Jav 刮削器网络配置、站点顺序和输出命名配置，并继续保留现有 TMDB 配置。
- 运行行为：现有任务没有显式刮削器字段时应按 `TMDB` 解释，以保持兼容。
