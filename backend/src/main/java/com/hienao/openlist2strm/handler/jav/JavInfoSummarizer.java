package com.hienao.openlist2strm.handler.jav;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JAV 信息汇总器
 *
 * <p>移植自 JavSP 的 info_summary，按优先级填充字段、处理封面策略、追加内嵌字幕/无码标签、检查必需字段。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
public class JavInfoSummarizer {

  private final JavCrawlerRegistry crawlerRegistry;

  public JavInfoSummarizer(JavCrawlerRegistry crawlerRegistry) {
    this.crawlerRegistry = crawlerRegistry;
  }

  /**
   * 汇总多个爬虫的结果
   *
   * @param crawlerResults 爬虫结果列表
   * @param identifier 番号标识
   * @return 汇总后的影片信息
   * @throws JavCrawlerException 汇总异常
   */
  public JavMovieInfo summarize(List<JavMovieInfo> crawlerResults,
      JavIdExtractor.JavIdentifier identifier) throws JavCrawlerException {

    if (crawlerResults == null || crawlerResults.isEmpty()) {
      throw new JavNotFoundException(identifier.getNumber(), null);
    }

    // 初始化最终结果
    JavMovieInfo result = new JavMovieInfo();
    result.setNumber(identifier.getNumber());
    result.setCid(identifier.getCid());
    result.setDataSourceType(identifier.getDataSourceType());

    // 按优先级填充字段
    for (JavMovieInfo info : crawlerResults) {
      if (info != null) {
        result.mergeFrom(info);
      }
    }

    // 处理封面策略
    processCoverStrategy(result, crawlerResults);

    // 追加特殊标签
    appendSpecialTags(result);

    // 检查必需字段
    checkRequiredKeys(result);

    log.info("已汇总影片信息: {}, 来源: {} 个爬虫",
        result.getNumber(), crawlerResults.size());

    return result;
  }

  /**
   * 处理封面策略
   *
   * <p>根据配置处理 javdb 封面策略
   *
   * @param result 结果对象
   * @param crawlerResults 爬虫结果列表
   */
  private void processCoverStrategy(JavMovieInfo result, List<JavMovieInfo> crawlerResults) {
    String useJavdbCover = crawlerRegistry.getUseJavdbCover();

    // 获取 javdb 的封面
    String javdbCover = null;
    String javdbBigCover = null;
    for (JavMovieInfo info : crawlerResults) {
      if (info != null && "javdb".equals(info.getSourceSite())) {
        if (info.getCovers() != null && !info.getCovers().isEmpty()) {
          javdbCover = info.getCovers().get(0);
        }
        if (info.getBigCovers() != null && !info.getBigCovers().isEmpty()) {
          javdbBigCover = info.getBigCovers().get(0);
        }
        break;
      }
    }

    // 获取非 javdb 的封面
    String nonJavdbCover = null;
    String nonJavdbBigCover = null;
    for (JavMovieInfo info : crawlerResults) {
      if (info != null && !"javdb".equals(info.getSourceSite())) {
        if (info.getCovers() != null && !info.getCovers().isEmpty()) {
          nonJavdbCover = info.getCovers().get(0);
          break;
        }
      }
    }
    for (JavMovieInfo info : crawlerResults) {
      if (info != null && !"javdb".equals(info.getSourceSite())) {
        if (info.getBigCovers() != null && !info.getBigCovers().isEmpty()) {
          nonJavdbBigCover = info.getBigCovers().get(0);
          break;
        }
      }
    }

    // 根据策略选择封面
    List<String> finalCovers = new ArrayList<>();
    List<String> finalBigCovers = new ArrayList<>();

    switch (useJavdbCover) {
      case "yes":
        // 始终使用 javdb 封面
        if (javdbCover != null) {
          finalCovers.add(javdbCover);
        }
        if (javdbBigCover != null) {
          finalBigCovers.add(javdbBigCover);
        }
        break;

      case "no":
        // 不使用 javdb 封面
        if (nonJavdbCover != null) {
          finalCovers.add(nonJavdbCover);
        }
        if (nonJavdbBigCover != null) {
          finalBigCovers.add(nonJavdbBigCover);
        }
        break;

      case "fallback":
      default:
        // 优先使用非 javdb 封面，javdb 作为备选
        if (nonJavdbCover != null) {
          finalCovers.add(nonJavdbCover);
        } else if (javdbCover != null) {
          finalCovers.add(javdbCover);
        }

        if (nonJavdbBigCover != null) {
          finalBigCovers.add(nonJavdbBigCover);
        } else if (javdbBigCover != null) {
          finalBigCovers.add(javdbBigCover);
        }
        break;
    }

    // 添加其他封面作为备选
    for (JavMovieInfo info : crawlerResults) {
      if (info != null) {
        if (info.getCovers() != null) {
          for (String cover : info.getCovers()) {
            if (!finalCovers.contains(cover)) {
              finalCovers.add(cover);
            }
          }
        }
        if (info.getBigCovers() != null) {
          for (String bigCover : info.getBigCovers()) {
            if (!finalBigCovers.contains(bigCover)) {
              finalBigCovers.add(bigCover);
            }
          }
        }
      }
    }

    result.setCovers(finalCovers);
    result.setBigCovers(finalBigCovers);
  }

  /**
   * 追加特殊标签
   *
   * <p>根据硬字幕和无码状态追加标签
   *
   * @param result 结果对象
   */
  private void appendSpecialTags(JavMovieInfo result) {
    List<String> genres = result.getGenres();
    if (genres == null) {
      genres = new ArrayList<>();
      result.setGenres(genres);
    }

    // 追加硬字幕标签
    if (result.isHasHardSubtitle()) {
      if (!genres.contains("内嵌字幕")) {
        genres.add("内嵌字幕");
      }
    }

    // 追加无码标签
    if (result.isUncensored()) {
      if (!genres.contains("无码")) {
        genres.add("无码");
      }
    }
  }

  /**
   * 检查必需字段
   *
   * @param result 结果对象
   * @throws JavCrawlerException 必需字段缺失异常
   */
  private void checkRequiredKeys(JavMovieInfo result) throws JavCrawlerException {
    List<String> requiredKeys = crawlerRegistry.getRequiredKeys();
    List<String> missingKeys = new ArrayList<>();

    for (String key : requiredKeys) {
      switch (key.toLowerCase()) {
        case "cover":
          if (result.getCovers() == null || result.getCovers().isEmpty()) {
            missingKeys.add("cover");
          }
          break;

        case "title":
          if (isEmpty(result.getTitle())) {
            missingKeys.add("title");
          }
          break;

        case "number":
          if (isEmpty(result.getNumber())) {
            missingKeys.add("number");
          }
          break;

        case "release_date":
          if (isEmpty(result.getReleaseDate())) {
            missingKeys.add("release_date");
          }
          break;

        case "studio":
          if (isEmpty(result.getStudio())) {
            missingKeys.add("studio");
          }
          break;

        case "actors":
          if (result.getActors() == null || result.getActors().isEmpty()) {
            missingKeys.add("actors");
          }
          break;

        default:
          log.debug("未知的必需字段: {}", key);
          break;
      }
    }

    if (!missingKeys.isEmpty()) {
      String message = "缺少必需字段: " + String.join(", ", missingKeys);
      log.warn(message + ", 番号: {}", result.getNumber());
      throw new JavNotFoundException(result.getNumber(), null);
    }
  }

  private boolean isEmpty(String str) {
    return str == null || str.trim().isEmpty();
  }
}
