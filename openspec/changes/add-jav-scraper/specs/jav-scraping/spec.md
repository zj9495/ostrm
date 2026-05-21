## ADDED Requirements

### Requirement: Jav 番号识别
系统 SHALL 按 JavSP 的番号识别规则从视频文件路径或文件名中识别 JAV 标识。

1. 系统 SHALL 支持普通番号识别，例如 `ABC-123`、`ABC123`、`HEYDOUGA-1234-567`、`RED096`
2. 系统 SHALL 支持 `FC2` 编号识别，例如 `FC2-123456`
3. 系统 SHALL 支持 `GETCHU` 和 `GYUTTO` 编号识别
4. 系统 SHALL 支持 DMM CID 识别
5. 文件名无法识别时，系统 SHALL 按 JavSP 规则尝试使用父目录名称识别番号

#### Scenario: 从文件名识别普通番号
- **WHEN** Jav 刮削器处理文件 `IPX-177.mp4`
- **THEN** 系统 SHALL 识别番号为 `IPX-177`

#### Scenario: 从无分隔符文件名识别普通番号
- **WHEN** Jav 刮削器处理文件 `IPX177.mkv`
- **THEN** 系统 SHALL 识别番号为 `IPX-177`

#### Scenario: 从文件名识别 FC2 编号
- **WHEN** Jav 刮削器处理文件 `FC2-718323.mp4`
- **THEN** 系统 SHALL 识别番号为 `FC2-718323`

#### Scenario: 从父目录识别番号
- **WHEN** Jav 刮削器无法从文件名识别番号且父目录名为 `ABP-647`
- **THEN** 系统 SHALL 识别番号为 `ABP-647`

#### Scenario: 无法识别番号
- **WHEN** Jav 刮削器无法从文件名和父目录识别番号
- **THEN** 系统 SHALL 跳过该文件的 Jav 刮削并记录原因

### Requirement: Jav 数据源类型判定
系统 SHALL 按 JavSP 的类型判定规则将识别出的 JAV 标识归类为抓取数据源类型。

1. `FC2-\d{5,7}` SHALL 归类为 `fc2`
2. `GETCHU-*` SHALL 归类为 `getchu`
3. `GYUTTO-*` SHALL 归类为 `gyutto`
4. 符合 CID 格式的标识 SHALL 归类为 `cid`
5. 其他有效番号 SHALL 归类为 `normal`

#### Scenario: 判定 FC2 类型
- **WHEN** 系统识别番号为 `FC2-718323`
- **THEN** 系统 SHALL 将数据源类型判定为 `fc2`

#### Scenario: 判定 CID 类型
- **WHEN** 系统识别标识符合 JavSP CID 格式
- **THEN** 系统 SHALL 将数据源类型判定为 `cid`

#### Scenario: 判定普通类型
- **WHEN** 系统识别番号为 `IPX-177`
- **THEN** 系统 SHALL 将数据源类型判定为 `normal`

### Requirement: Jav 站点抓取
系统 SHALL 按 JavSP 的抓取器选择配置调用对应站点抓取器，并将每个成功站点的结果保存为独立 `JavMovieInfo`。

1. `normal` 类型 SHALL 使用 `airav`, `avsox`, `javbus`, `javdb`, `javlib`, `jav321`, `mgstage`, `prestige`
2. `fc2` 类型 SHALL 使用 `fc2`, `avsox`, `javdb`, `javmenu`, `fc2ppvdb`
3. `cid` 类型 SHALL 使用 `fanza`
4. `getchu` 类型 SHALL 使用 `dl_getchu`
5. `gyutto` 类型 SHALL 使用 `gyutto`
6. 单个站点未找到影片 SHALL 不覆盖其他站点已抓取的数据

#### Scenario: normal 类型选择抓取器
- **WHEN** Jav 刮削器处理 `normal` 类型影片
- **THEN** 系统 SHALL 按配置顺序调用 `normal` 抓取器集合

#### Scenario: fc2 类型选择抓取器
- **WHEN** Jav 刮削器处理 `fc2` 类型影片
- **THEN** 系统 SHALL 按配置顺序调用 `fc2` 抓取器集合

#### Scenario: cid 类型选择抓取器
- **WHEN** Jav 刮削器处理 `cid` 类型影片
- **THEN** 系统 SHALL 按配置顺序调用 `cid` 抓取器集合

#### Scenario: 站点未找到影片
- **WHEN** 一个 Jav 站点抓取器返回未找到影片
- **THEN** 系统 SHALL 保留其他成功站点的抓取结果

#### Scenario: 全部站点无结果
- **WHEN** 目标数据源类型的所有 Jav 站点抓取器均未获得影片信息
- **THEN** 系统 SHALL 跳过该文件的 Jav 刮削并记录原因

### Requirement: Jav 字段汇总
系统 SHALL 按 JavSP 的字段汇总规则生成最终 JAV 元数据。

1. 系统 SHALL 以识别出的番号或 CID 初始化最终元数据
2. 系统 SHALL 按抓取器配置顺序填充当前为空的字段
3. 系统 SHALL 汇总封面和高清封面候选列表
4. 系统 SHALL 支持 `javdb` 封面策略 `fallback`、`yes`、`no`
5. 系统 SHALL 在最终元数据缺少必需字段时判定 Jav 刮削失败
6. 必需字段默认 SHALL 为 `cover` 和 `title`

#### Scenario: 按优先级填充空字段
- **WHEN** 多个 Jav 站点返回同一影片的字段
- **THEN** 系统 SHALL 按抓取器配置顺序使用第一个非空字段值

#### Scenario: 汇总封面候选
- **WHEN** 多个 Jav 站点返回不同封面地址
- **THEN** 系统 SHALL 将封面地址保存为有序候选列表

#### Scenario: javdb 封面 fallback 策略
- **WHEN** `useJavdbCover` 为 `fallback` 且存在非 `javdb` 封面
- **THEN** 系统 SHALL 优先下载非 `javdb` 封面

#### Scenario: 缺少必需字段
- **WHEN** 最终 Jav 元数据缺少任一必需字段
- **THEN** 系统 SHALL 判定该文件 Jav 刮削失败并记录缺失字段

### Requirement: Jav NFO 生成
系统 SHALL 根据最终 JAV 元数据生成 Kodi movie NFO 文件。

1. NFO 根节点 SHALL 为 `movie`
2. NFO SHALL 写入标题、原始标题、评分、简介、时长、番号、CID、分类、标签、国家、系列、导演、发行日期、制作商、预告片和演员信息中已获取的字段
3. NFO SHALL 使用 UTF-8 编码
4. NFO 文件 SHALL 保存为当前 STRM 基名加 `.nfo`

#### Scenario: 生成 Jav NFO
- **WHEN** Jav 刮削器获得满足必需字段的最终元数据
- **THEN** 系统 SHALL 在当前输出目录生成 Kodi movie NFO 文件

#### Scenario: 写入番号 uniqueid
- **WHEN** 最终 JAV 元数据包含番号
- **THEN** 系统 SHALL 将番号写入 NFO `uniqueid` 且类型为 `num`

#### Scenario: 写入演员
- **WHEN** 最终 JAV 元数据包含演员列表
- **THEN** 系统 SHALL 将每个演员写入 NFO `actor` 节点

### Requirement: Jav 图片下载
系统 SHALL 根据最终 JAV 元数据下载图片文件。

1. 系统 SHALL 使用封面候选列表下载 fanart 图片
2. 系统 SHALL 使用可用的高清封面候选优先下载 fanart 图片
3. 系统 SHALL 从 fanart 生成 poster 图片
4. 当剧照下载启用且元数据包含剧照地址时，系统 SHALL 下载剧照到 `extrafanart` 目录

#### Scenario: 下载高清封面
- **WHEN** Jav 元数据包含高清封面候选且高清封面下载启用
- **THEN** 系统 SHALL 优先下载高清封面作为 fanart 图片

#### Scenario: 生成 poster
- **WHEN** fanart 图片下载成功
- **THEN** 系统 SHALL 生成 poster 图片

#### Scenario: 下载剧照
- **WHEN** 剧照下载启用且 Jav 元数据包含剧照地址
- **THEN** 系统 SHALL 将剧照下载到当前输出目录下的 `extrafanart` 目录

### Requirement: Jav 输出边界
系统 SHALL 将 Jav 刮削生成的文件保存到 Ostrm 当前任务的 STRM 输出目录中，并 SHALL NOT 移动、重命名或硬链接原始媒体文件。

#### Scenario: 保存到当前输出目录
- **WHEN** Jav 刮削器处理一个视频文件
- **THEN** 系统 SHALL 将 NFO、poster、fanart 和剧照保存到该视频对应的 STRM 输出目录

#### Scenario: 不移动原始媒体文件
- **WHEN** Jav 刮削器完成文件处理
- **THEN** 系统 SHALL NOT 移动、重命名或硬链接原始媒体文件
