package com.hienao.openlist2strm.handler;

import com.hienao.openlist2strm.handler.context.FileProcessingContext;
import com.hienao.openlist2strm.handler.jav.JavCrawler;
import com.hienao.openlist2strm.handler.jav.JavCrawlerException;
import com.hienao.openlist2strm.handler.jav.JavCrawlerRegistry;
import com.hienao.openlist2strm.handler.jav.JavDataSourceType;
import com.hienao.openlist2strm.handler.jav.JavHttpClient;
import com.hienao.openlist2strm.handler.jav.JavIdExtractor;
import com.hienao.openlist2strm.handler.jav.JavIdExtractor.JavIdentifier;
import com.hienao.openlist2strm.handler.jav.JavImageService;
import com.hienao.openlist2strm.handler.jav.JavInfoSummarizer;
import com.hienao.openlist2strm.handler.jav.JavMovieInfo;
import com.hienao.openlist2strm.handler.jav.JavNfoGeneratorService;
import com.hienao.openlist2strm.handler.jav.JavNotFoundException;
import com.hienao.openlist2strm.service.SystemConfigService;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JAV 媒体刮削器
 *
 * <p>编排 JAV 刮削流程：识别番号、确定数据源、运行爬虫、汇总字段、生成 NFO 和下载图片。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JavMediaScraper implements MediaScraper {

  private final SystemConfigService systemConfigService;
  private final JavCrawlerRegistry crawlerRegistry;
  private final JavInfoSummarizer infoSummarizer;
  private final JavNfoGeneratorService nfoGeneratorService;
  private final JavImageService imageService;

  @Override
  public String getScraperType() {
    return "JAV";
  }

  @Override
  @SuppressWarnings("unchecked")
  public FileProcessingResult scrape(FileProcessingContext context) {
    try {
      String fileName = context.getCurrentFile().getName();
      String relativePath = context.getRelativePath();
      String saveDirectory = context.getSaveDirectory();

      log.info("开始 JAV 刮削: {}", fileName);

      // 1. 识别番号
      JavIdentifier identifier = JavIdExtractor.extract(relativePath);
      if (identifier == null) {
        log.warn("无法识别番号: {}", fileName);
        return FileProcessingResult.skipped("无法识别番号: " + fileName);
      }

      log.info("识别到番号: {}, 数据源类型: {}", identifier.getNumber(), identifier.getDataSourceType());

      // 2. 获取网络配置
      Map<String, Object> javConfig = systemConfigService.getJavScrapingConfig();
      Map<String, Object> networkConfig = (Map<String, Object>) javConfig.getOrDefault("network", Map.of());
      JavHttpClient httpClient = new JavHttpClient(networkConfig);

      // 3. 获取爬虫列表
      JavDataSourceType dataSourceType = identifier.getDataSourceType();
      List<JavCrawler> crawlers = crawlerRegistry.getCrawlers(dataSourceType);
      if (crawlers.isEmpty()) {
        log.warn("没有可用的爬虫: {}", dataSourceType);
        return FileProcessingResult.skipped("没有可用的爬虫: " + dataSourceType);
      }

      // 4. 运行爬虫
      List<JavMovieInfo> crawlerResults = new ArrayList<>();
      for (JavCrawler crawler : crawlers) {
        try {
          log.debug("运行爬虫: {}", crawler.getName());
          JavMovieInfo result = crawler.crawl(httpClient, identifier.getNumber(),
              identifier.getCid(), dataSourceType);
          if (result != null) {
            crawlerResults.add(result);
          }
        } catch (JavNotFoundException e) {
          log.debug("爬虫 {} 未找到影片: {}", crawler.getName(), identifier.getNumber());
        } catch (Exception e) {
          log.warn("爬虫 {} 失败: {}", crawler.getName(), e.getMessage());
        }
      }

      if (crawlerResults.isEmpty()) {
        log.warn("所有爬虫都未找到影片: {}", identifier.getNumber());
        return FileProcessingResult.skipped("所有爬虫都未找到影片: " + identifier.getNumber());
      }

      // 5. 汇总信息
      JavMovieInfo movieInfo;
      try {
        movieInfo = infoSummarizer.summarize(crawlerResults, identifier);
      } catch (JavCrawlerException e) {
        log.warn("汇总信息失败: {}", e.getMessage());
        return FileProcessingResult.skipped("汇总信息失败: " + e.getMessage());
      }

      // 6. 检测特殊属性
      List<String> genres = movieInfo.getGenres();
      movieInfo.detectSpecialAttributes(fileName, genres);

      // 7. 生成 NFO
      String baseFileName = getStrmCompatibleBaseFileName(fileName);
      String nfoPath = Paths.get(saveDirectory, baseFileName + ".nfo").toString();
      try {
        nfoGeneratorService.generateNfo(movieInfo, nfoPath);
      } catch (Exception e) {
        log.error("生成 NFO 失败: {}", e.getMessage());
        return FileProcessingResult.failed("生成 NFO 失败: " + e.getMessage());
      }

      // 8. 下载图片
      Map<String, Object> summarizerConfig = (Map<String, Object>) javConfig.getOrDefault("summarizer", Map.of());
      boolean useHighRes = (Boolean) summarizerConfig.getOrDefault("coverHighres", true);
      boolean downloadExtraFanarts = (Boolean) summarizerConfig.getOrDefault("downloadExtraFanarts", false);

      // 下载 fanart
      String fanartPath = imageService.downloadFanart(movieInfo, saveDirectory, baseFileName, useHighRes);

      // 生成 poster
      if (fanartPath != null) {
        imageService.generatePoster(fanartPath, saveDirectory, baseFileName);
      }

      // 下载剧照
      if (downloadExtraFanarts) {
        imageService.downloadExtraFanarts(movieInfo, saveDirectory, baseFileName, 10);
      }

      log.info("JAV 刮削完成: {}", identifier.getNumber());
      return FileProcessingResult.success();

    } catch (Exception e) {
      log.error("JAV 刮削失败: {}", context.getBaseFileName(), e);
      return FileProcessingResult.failed("JAV 刮削失败: " + e.getMessage());
    }
  }

  /**
   * 获取 STRM 兼容的基础文件名
   */
  private String getStrmCompatibleBaseFileName(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return "unknown";
    }
    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex > 0) {
      return fileName.substring(0, lastDotIndex);
    }
    return fileName;
  }
}
