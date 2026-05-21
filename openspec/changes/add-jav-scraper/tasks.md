## 1. Task Configuration

- [x] 1.1 Add `scraperType` to `TaskConfig`, `TaskConfigDto`, controller conversion, validation, and default-value handling.
- [x] 1.2 Add Flyway migration for `task_config.scraper_type` with existing rows set to `TMDB`.
- [x] 1.3 Update `TaskConfigMapper.xml` result mapping, select column list, insert, and update statements for `scraper_type`.
- [x] 1.4 Update task list and task form UI to display and edit `TMDB` / `JAV` when `needScrap` is enabled.

## 2. Scraper Dispatch

- [x] 2.1 Introduce a `MediaScraper` interface that accepts `FileProcessingContext` and returns `FileProcessingResult`.
- [x] 2.2 Move the current TMDB logic behind `TmdbMediaScraper` without changing existing TMDB behavior.
- [x] 2.3 Update `MediaScrapingHandler` to check global scraping enablement, task `needScrap`, and dispatch by `scraperType`.
- [x] 2.4 Ensure invalid scraper types are rejected at task save time, not during file processing.

## 3. Jav Core Model And Config

- [x] 3.1 Add Jav system config defaults under `javScraping` for network, crawler selection, required keys, cover policy, and output options.
- [x] 3.2 Implement `JavMovie`, `JavMovieInfo`, and enum types for Jav data source and crawler IDs.
- [x] 3.3 Implement `JavIdExtractor` by porting JavSP `get_id`, `get_cid`, and `guess_av_type`.
- [x] 3.4 Implement special attribute detection for hard subtitles and uncensored files from JavSP.

## 4. Jav Crawler Infrastructure

- [x] 4.1 Add an HTTP client helper for Jav crawlers with configured proxy, timeout, retry, headers, and HTML parsing.
- [x] 4.2 Add `JavCrawler` interface and `JavCrawlerRegistry` that resolves crawler order from `javScraping.crawler.selection`.
- [x] 4.3 Implement crawler exception types for not found, duplicate result, blocked site, permission, credential, website, and generic crawler errors.
- [x] 4.4 Implement `JavInfoSummarizer` to merge crawler outputs using JavSP priority and required-key rules.

## 5. Jav Site Crawlers

- [x] 5.1 Implement `airav` crawler parser.
- [x] 5.2 Implement `avsox` crawler parser.
- [x] 5.3 Implement `javbus` crawler parser and genre mapping.
- [x] 5.4 Implement `javdb` crawler parser and genre mapping.
- [x] 5.5 Implement `javlib` crawler parser.
- [x] 5.6 Implement `jav321` crawler parser.
- [x] 5.7 Implement `mgstage` crawler parser.
- [x] 5.8 Implement `prestige` crawler parser.
- [x] 5.9 Implement `fc2`, `fc2ppvdb`, and `javmenu` crawler parsers.
- [x] 5.10 Implement `fanza`, `dl_getchu`, and `gyutto` crawler parsers.

## 6. Jav Output Generation

- [x] 6.1 Implement `JavNfoGeneratorService` for Kodi movie NFO output using JavSP field semantics.
- [x] 6.2 Implement Jav cover download using ordered `bigCovers` and `covers`.
- [x] 6.3 Implement poster generation from downloaded fanart using the configured filename base.
- [x] 6.4 Implement optional `extrafanart` download when configured and preview pictures are present.
- [x] 6.5 Ensure Jav output writes into the current STRM output directory and never moves or renames source media files.

## 7. Integration

- [x] 7.1 Implement `JavMediaScraper` orchestration: identify Jav ID, determine data source, run configured crawlers, summarize fields, generate NFO, and download images.
- [x] 7.2 Wire `JavMediaScraper` into the scraper dispatch map for `scraperType=JAV`.
- [x] 7.3 Update logging and file processing result messages to include the selected scraper type and concrete skip/failure reason.
- [x] 7.4 Update OpenAPI annotations or generated API docs for the new task field.

## 8. Verification

- [x] 8.1 Verify task create/edit payloads persist and reload `scraperType`.
- [x] 8.2 Verify an existing task with no stored `scraper_type` executes as `TMDB`.
- [x] 8.3 Verify a `JAV` task produces `.nfo`, fanart, poster, and optional `extrafanart` for representative normal, fc2, and cid samples.
- [x] 8.4 When explicit verification is requested, use the project Docker build flow to validate the integrated application.
