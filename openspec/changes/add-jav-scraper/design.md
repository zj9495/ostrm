## Context

Ostrm 当前以 `TaskConfig.needScrap` 控制是否执行在线刮削，后端 `MediaScrapingHandler` 和 `MediaScrapingService` 都直接耦合 TMDB：解析普通影视文件名、调用 TMDB 搜索/详情接口、生成电影或电视剧 NFO，并下载 poster/backdrop。任务配置实体、DTO、MyBatis 映射和前端任务表单都没有刮削器类型字段。

JavSP 是一个面向 JAV 文件的 Python 刮削器。它的核心流程不是按影视标题搜索，而是先从文件路径识别番号或 CID，再按类型选择多个站点抓取器并行抓取，最后按配置顺序汇总字段、生成 Kodi movie NFO、下载 fanart/poster 和剧照。本次变更需要把该算法移植到 Ostrm 的 Java/Spring 体系内，同时保持原有 TMDB 任务行为。

## Goals / Non-Goals

**Goals:**

- 允许任务在 `TMDB` 和 `JAV` 两种刮削器之间选择。
- 对旧任务保持兼容：未存储刮削器类型的任务按 `TMDB` 执行。
- 新增 Jav 刮削器，复刻 JavSP 的番号识别、类型判定、站点选择、字段汇总、NFO 生成、封面下载和剧照下载流程。
- 保持 Ostrm 的文件处理责任链模型，Jav 刮削只生成附属元数据文件，不移动、不重命名原始媒体文件。

**Non-Goals:**

- 不把 JavSP 作为外部 Python 进程调用。
- 不实现 JavSP 的交互式手动更正、检查更新、自动更新、原始文件移动或硬链接整理。
- 不新增未在 JavSP 中存在的匹配启发式规则。
- 不改变字幕复制、STRM 生成、孤立文件清理的既有职责。

## Decisions

### 1. 使用任务字段选择刮削器

新增 `TaskConfig.scraperType` / `TaskConfigDto.scraperType`，数据库列为 `scraper_type`，允许值为 `TMDB`、`JAV`。前端在 `needScrap=true` 时显示单选控件，创建任务默认选择 `TMDB`；旧记录迁移为 `TMDB`。

Rationale: 用户要求“创建任务时可以选择原有的刮削器或者 Jav 刮削器”，任务字段是最直接的边界。系统级配置无法表达同一 OpenList 配置下不同任务的刮削需求。

Alternative considered: 使用全局系统配置切换刮削器。该方案不能满足不同任务选择不同刮削器。

### 2. 抽象刮削器接口并保留责任链入口

保留 `MediaScrapingHandler` 作为 Order 50 的责任链入口；它只检查 `needScrap`、全局刮削开关和任务刮削器类型，然后分派到 `MediaScraper` 接口实现：

- `TmdbMediaScraper`: 承接现有 TMDB 逻辑。
- `JavMediaScraper`: 实现 JavSP 复刻逻辑。

接口输入使用 `FileProcessingContext`，输出使用现有 `FileProcessingResult`，这样 NFO/Image handler 和运行统计不需要引入新的状态模型。

Alternative considered: 新增一个独立 `JavScrapingHandler`。这会让同一个视频文件经过两个在线刮削处理器，需要额外控制顺序和互斥，不如单一入口清晰。

### 3. Jav 刮削器按 JavSP 数据流拆分组件

Jav 实现拆成以下组件：

- `JavIdExtractor`: 复刻 JavSP `avid.py` 的 `get_id`、`get_cid`、`guess_av_type`。
- `JavMovie` / `JavMovieInfo`: 对齐 JavSP `Movie` / `MovieInfo` 字段。
- `JavCrawler`: 站点抓取器接口，每个站点一个实现，负责把站点数据写入 `JavMovieInfo`。
- `JavCrawlerRegistry`: 根据 `normal/fc2/cid/getchu/gyutto` 返回配置顺序中的抓取器。
- `JavInfoSummarizer`: 复刻 JavSP `info_summary`，按优先级填充字段、处理 `javdb` 封面策略、追加内嵌字幕/无码标签、检查必需字段。
- `JavNfoGeneratorService`: 复刻 JavSP `write_nfo` 输出 Kodi movie NFO。
- `JavImageService`: 下载 fanart/poster 和可选剧照。

Rationale: JavSP 的站点差异集中在 `javsp/web/*`，公共算法集中在番号识别、汇总和输出。按这个边界移植可以避免把站点解析细节散进任务处理代码。

### 4. Jav 站点范围按 JavSP 默认配置实现

首批实现 JavSP 默认选择中的站点：

- `normal`: `airav`, `avsox`, `javbus`, `javdb`, `javlib`, `jav321`, `mgstage`, `prestige`
- `fc2`: `fc2`, `avsox`, `javdb`, `javmenu`, `fc2ppvdb`
- `cid`: `fanza`
- `getchu`: `dl_getchu`
- `gyutto`: `gyutto`

每个站点实现只按 JavSP 对应 parser 的字段定义输出，不发明额外字段映射。遇到页面结构无法确认时，暂停该站点实现并依据实际页面和 JavSP 代码补齐。

### 5. Jav 配置归入系统配置

新增 `javScraping` 系统配置节点，用于表达 JavSP 中与任务无关的配置：

- `network.proxyServer`, `network.timeoutSeconds`, `network.retry`
- `network.proxyFree` 中各站点基础域名
- `crawler.selection`
- `crawler.requiredKeys`
- `crawler.respectSiteAvid`
- `crawler.useJavdbCover`
- `crawler.normalizeActressName`
- `summarizer.nfoTitlePattern`
- `summarizer.downloadExtraFanarts`
- `summarizer.coverHighres`

这些字段与 JavSP 配置名一一对应或语义一一对应。任务只保存刮削器类型。

### 6. 输出路径遵循 Ostrm STRM 目录

Jav 刮削输出保存到当前视频对应的 `saveDirectory`，NFO 文件使用当前 STRM 基名加 `.nfo`，poster/fanart 与当前基名关联。JavSP 的 `output_folder_pattern`、移动原始影片和硬链接整理不迁移到 Ostrm。

Rationale: Ostrm 的核心职责是生成 STRM 和附属刮削文件，原始文件在 OpenList 或本地源中，不应被 Jav 刮削流程重排。

## Risks / Trade-offs

- [Risk] 多个 JAV 站点页面结构变化频繁 → Mitigation: 每个站点 parser 独立实现，并用 JavSP fixture 中的样例字段建立针对性验证。
- [Risk] JavSP 的并行抓取可能增加外部站点访问压力 → Mitigation: 只按配置中的站点集合发起请求，并保留 retry/timeout/sleep 配置；不额外增加自动探测站点。
- [Risk] 旧任务缺少 `scraper_type` 字段 → Mitigation: Flyway 迁移将旧数据写为 `TMDB`，DTO 和实体默认值也使用 `TMDB`。
- [Risk] JavDB、JavBus 等站点可能需要代理或镜像域名 → Mitigation: 系统配置提供与 JavSP `proxyFree` 对应的站点基础域名和代理配置。
- [Risk] JavSP 有翻译、AI 裁剪、自动更新等非核心能力 → Mitigation: 本变更不包含这些能力，避免把不属于任务刮削选择的功能带入。
