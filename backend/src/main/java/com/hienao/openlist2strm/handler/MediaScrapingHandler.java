package com.hienao.openlist2strm.handler;

import com.hienao.openlist2strm.entity.TaskConfig;
import com.hienao.openlist2strm.handler.context.FileProcessingContext;
import com.hienao.openlist2strm.service.SystemConfigService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 媒体刮削处理器
 *
 * <p>负责执行媒体刮削，从 TMDB API 获取媒体信息并生成 NFO 和下载图片。
 *
 * <p>Order: 50
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class MediaScrapingHandler implements FileProcessorHandler {

  private final SystemConfigService systemConfigService;
  private final List<MediaScraper> mediaScrapers;

  private Map<String, MediaScraper> scraperMap;

  @jakarta.annotation.PostConstruct
  public void init() {
    scraperMap = new HashMap<>();
    for (MediaScraper scraper : mediaScrapers) {
      scraperMap.put(scraper.getScraperType(), scraper);
    }
    log.info("已注册 {} 个媒体刮削器: {}", scraperMap.size(), scraperMap.keySet());
  }

  // ==================== 接口实现 ====================

  @Override
  public FileProcessingResult process(FileProcessingContext context) {
    try {
      // 检查刮削是否启用
      if (!isScrapingEnabled(context)) {
        log.debug("刮削功能未启用，跳过");
        return FileProcessingResult.success();
      }

      // 检查任务是否需要刮削
      TaskConfig taskConfig = context.getTaskConfig();
      if (taskConfig == null || !Boolean.TRUE.equals(taskConfig.getNeedScrap())) {
        log.debug("任务不需要刮削，跳过");
        return FileProcessingResult.success();
      }

      // 获取刮削器类型
      String scraperType = taskConfig.getScraperType();
      if (scraperType == null || scraperType.isEmpty()) {
        scraperType = "TMDB";
      }

      // 查找对应的刮削器
      MediaScraper scraper = scraperMap.get(scraperType);
      if (scraper == null) {
        log.error("未找到刮削器类型: {}", scraperType);
        context.getStats().incrementFailed();
        return FileProcessingResult.failed("未找到刮削器类型: " + scraperType);
      }

      // 执行刮削
      log.info("使用 {} 刮削器处理文件: {}", scraperType, context.getCurrentFile().getName());
      FileProcessingResult scrapingResult = scraper.scrape(context);
      if (scrapingResult.isSkipped()) {
        context.getStats().incrementSkipped();
        return scrapingResult;
      }
      if (scrapingResult.isFailed()) {
        context.getStats().incrementFailed();
        return scrapingResult;
      }

      context.getStats().incrementProcessed();
      return FileProcessingResult.success();

    } catch (Exception e) {
      log.error("媒体刮削失败: {}", context.getBaseFileName(), e);
      context.getStats().incrementFailed();
      return FileProcessingResult.failed("媒体刮削失败: " + e.getMessage());
    }
  }

  @Override
  public java.util.Set<FileType> getHandledTypes() {
    return java.util.Set.of(FileType.VIDEO);
  }

  // ==================== 辅助方法 ====================

  private boolean isScrapingEnabled(FileProcessingContext context) {
    Map<String, Object> config = systemConfigService.getScrapingConfig();
    return Boolean.TRUE.equals(config.getOrDefault("enabled", true));
  }
}
