package com.hienao.openlist2strm.handler.jav;

import com.hienao.openlist2strm.service.SystemConfigService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JAV 爬虫注册表
 *
 * <p>管理所有站点爬虫，并根据配置返回指定数据源类型的爬虫列表。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
public class JavCrawlerRegistry {

  private final Map<JavCrawlerId, JavCrawler> crawlers = new HashMap<>();
  private final SystemConfigService systemConfigService;

  /**
   * 构造函数
   *
   * @param crawlerList 自动注入的所有爬虫实现
   * @param systemConfigService 系统配置服务
   */
  public JavCrawlerRegistry(
      List<JavCrawler> crawlerList,
      SystemConfigService systemConfigService) {
    this.systemConfigService = systemConfigService;

    // 注册所有爬虫
    for (JavCrawler crawler : crawlerList) {
      crawlers.put(crawler.getCrawlerId(), crawler);
      log.info("已注册 JAV 爬虫: {}", crawler.getName());
    }

    log.info("已注册 {} 个 JAV 爬虫", crawlers.size());
  }

  /**
   * 获取指定数据源类型的爬虫列表
   *
   * <p>根据配置中的选择顺序返回爬虫列表
   *
   * @param dataSourceType 数据源类型
   * @return 爬虫列表
   */
  public List<JavCrawler> getCrawlers(JavDataSourceType dataSourceType) {
    List<JavCrawler> result = new ArrayList<>();

    // 从配置中获取选择顺序
    List<String> selection = getSelection(dataSourceType);
    if (selection == null || selection.isEmpty()) {
      log.warn("未找到数据源类型的爬虫配置: {}", dataSourceType);
      return result;
    }

    // 按选择顺序添加爬虫
    for (String crawlerName : selection) {
      JavCrawlerId crawlerId = JavCrawlerId.fromValue(crawlerName);
      if (crawlerId == null) {
        log.warn("未知的爬虫 ID: {}", crawlerName);
        continue;
      }

      JavCrawler crawler = crawlers.get(crawlerId);
      if (crawler == null) {
        log.warn("未找到爬虫实现: {}", crawlerName);
        continue;
      }

      // 检查爬虫是否支持该数据源类型
      if (crawler.supports(dataSourceType)) {
        result.add(crawler);
      } else {
        log.debug("爬虫 {} 不支持数据源类型: {}", crawlerName, dataSourceType);
      }
    }

    log.debug("为数据源类型 {} 找到 {} 个爬虫", dataSourceType, result.size());
    return result;
  }

  /**
   * 获取爬虫配置的选择顺序
   *
   * @param dataSourceType 数据源类型
   * @return 选择顺序列表
   */
  @SuppressWarnings("unchecked")
  private List<String> getSelection(JavDataSourceType dataSourceType) {
    Map<String, Object> crawlerConfig = getCrawlerConfig();
    if (crawlerConfig == null) {
      return getDefaultSelection(dataSourceType);
    }

    Object selectionObj = crawlerConfig.get("selection");
    if (selectionObj instanceof Map) {
      Map<String, List<String>> selectionMap = (Map<String, List<String>>) selectionObj;
      List<String> selection = selectionMap.get(dataSourceType.getValue());
      if (selection != null && !selection.isEmpty()) {
        return selection;
      }
    }

    return getDefaultSelection(dataSourceType);
  }

  /**
   * 获取默认选择顺序
   *
   * @param dataSourceType 数据源类型
   * @return 默认选择顺序
   */
  private List<String> getDefaultSelection(JavDataSourceType dataSourceType) {
    switch (dataSourceType) {
      case NORMAL:
        return List.of("airav", "avsox", "javbus", "javdb", "javlib", "jav321", "mgstage", "prestige");
      case FC2:
        return List.of("fc2", "avsox", "javdb", "javmenu", "fc2ppvdb");
      case CID:
        return List.of("fanza");
      case GETCHU:
        return List.of("dl_getchu");
      case GYUTTO:
        return List.of("gyutto");
      default:
        return List.of();
    }
  }

  /**
   * 获取必需字段配置
   *
   * @return 必需字段列表
   */
  @SuppressWarnings("unchecked")
  public List<String> getRequiredKeys() {
    Map<String, Object> crawlerConfig = getCrawlerConfig();
    if (crawlerConfig == null) {
      return List.of("cover", "title");
    }
    Object requiredKeys = crawlerConfig.get("requiredKeys");
    if (requiredKeys instanceof List) {
      return (List<String>) requiredKeys;
    }
    return List.of("cover", "title");
  }

  /**
   * 是否尊重站点番号
   *
   * @return 是否尊重站点番号
   */
  public boolean isRespectSiteAvid() {
    Map<String, Object> crawlerConfig = getCrawlerConfig();
    if (crawlerConfig == null) {
      return true;
    }
    Object respectSiteAvid = crawlerConfig.get("respectSiteAvid");
    if (respectSiteAvid instanceof Boolean) {
      return (Boolean) respectSiteAvid;
    }
    return true;
  }

  /**
   * 获取 javdb 封面策略
   *
   * @return 封面策略
   */
  public String getUseJavdbCover() {
    Map<String, Object> crawlerConfig = getCrawlerConfig();
    if (crawlerConfig == null) {
      return "fallback";
    }
    Object useJavdbCover = crawlerConfig.get("useJavdbCover");
    if (useJavdbCover instanceof String) {
      return (String) useJavdbCover;
    }
    return "fallback";
  }

  /**
   * 是否标准化演员名
   *
   * @return 是否标准化演员名
   */
  public boolean isNormalizeActressName() {
    Map<String, Object> crawlerConfig = getCrawlerConfig();
    if (crawlerConfig == null) {
      return true;
    }
    Object normalizeActressName = crawlerConfig.get("normalizeActressName");
    if (normalizeActressName instanceof Boolean) {
      return (Boolean) normalizeActressName;
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getCrawlerConfig() {
    Map<String, Object> javScrapingConfig = systemConfigService.getJavScrapingConfig();
    Object crawlerConfig = javScrapingConfig.get("crawler");
    if (crawlerConfig instanceof Map) {
      return (Map<String, Object>) crawlerConfig;
    }
    return null;
  }
}
