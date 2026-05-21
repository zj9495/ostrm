package com.hienao.openlist2strm.handler.jav.crawlers;

import com.hienao.openlist2strm.handler.jav.JavCrawler;
import com.hienao.openlist2strm.handler.jav.JavCrawlerId;
import com.hienao.openlist2strm.handler.jav.JavDataSourceType;
import com.hienao.openlist2strm.handler.jav.JavHttpClient;
import com.hienao.openlist2strm.handler.jav.JavMovieInfo;
import com.hienao.openlist2strm.handler.jav.JavNotFoundException;
import com.hienao.openlist2strm.handler.jav.JavWebsiteException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * fc2 站点爬虫
 *
 * <p>从 fc2.com 站点抓取影片信息。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
public class Fc2Crawler implements JavCrawler {

  private static final String BASE_URL = "https://adult.contents.fc2.com";
  private static final String SEARCH_URL = BASE_URL + "/search/%s";

  @Override
  public JavCrawlerId getCrawlerId() {
    return JavCrawlerId.FC2;
  }

  @Override
  public String getName() {
    return "fc2";
  }

  @Override
  public boolean supports(JavDataSourceType dataSourceType) {
    return dataSourceType == JavDataSourceType.FC2;
  }

  @Override
  public JavMovieInfo crawl(JavHttpClient httpClient, String number, String cid,
      JavDataSourceType dataSourceType) throws Exception {

    log.info("开始从 fc2 抓取: {}", number);

    try {
      // 提取 FC2 编号
      String fc2Id = extractFc2Id(number);
      if (fc2Id == null) {
        throw new JavNotFoundException(number, getCrawlerId());
      }

      // 搜索影片
      String searchUrl = String.format(SEARCH_URL, fc2Id);
      Document searchDoc = httpClient.getDocument(searchUrl);

      // 查找影片链接
      String detailUrl = findDetailUrl(searchDoc, fc2Id);
      if (detailUrl == null) {
        throw new JavNotFoundException(number, getCrawlerId());
      }

      // 获取详情页
      Document detailDoc = httpClient.getDocument(detailUrl);

      // 解析详情页
      JavMovieInfo result = parseDetailPage(detailDoc, number, fc2Id);
      result.setSourceSite("fc2");

      log.info("成功从 fc2 抓取: {}", number);
      return result;

    } catch (JavNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new JavWebsiteException("fc2 抓取失败: " + e.getMessage(), e, getCrawlerId());
    }
  }

  /**
   * 提取 FC2 编号
   */
  private String extractFc2Id(String number) {
    // 移除 FC2- 前缀
    String id = number.toUpperCase().replace("FC2-", "").replace("FC2", "");
    // 移除非数字字符
    id = id.replaceAll("[^0-9]", "");
    if (id.isEmpty()) {
      return null;
    }
    return id;
  }

  /**
   * 查找影片详情页 URL
   */
  private String findDetailUrl(Document searchDoc, String fc2Id) {
    // 查找搜索结果中的影片链接
    Elements links = searchDoc.select("a[href*=/article/]");
    for (Element link : links) {
      String href = link.attr("href");
      if (href.contains(fc2Id)) {
        return href;
      }
    }

    // 如果没有精确匹配，尝试第一个结果
    if (!links.isEmpty()) {
      return links.first().attr("href");
    }

    return null;
  }

  /**
   * 解析详情页
   */
  private JavMovieInfo parseDetailPage(Document doc, String number, String fc2Id) {
    JavMovieInfo info = new JavMovieInfo();
    info.setNumber(number);

    // 标题
    Element titleElement = doc.selectFirst("h2");
    if (titleElement != null) {
      info.setTitle(titleElement.text());
    }

    // 封面
    Element coverElement = doc.selectFirst(".items_article_MainitemThumb img");
    if (coverElement != null) {
      String coverUrl = coverElement.attr("src");
      if (!coverUrl.isEmpty()) {
        info.setCovers(List.of(coverUrl));
        info.setBigCovers(List.of(coverUrl));
      }
    }

    // 解析元数据
    Elements infoItems = doc.select(".items_article_headerInfo li");
    for (Element item : infoItems) {
      String text = item.text();
      if (text.contains("ID：")) {
        info.setNumber("FC2-" + text.replace("ID：", "").trim());
      } else if (text.contains("販売日：")) {
        String date = text.replace("販売日：", "").trim();
        info.setReleaseDate(date);
      } else if (text.contains("再生時間：")) {
        String durationStr = text.replace("再生時間：", "").replace("分", "").trim();
        info.setDuration(parseDuration(durationStr));
      }
    }

    // 演员
    Elements actorElements = doc.select("a[href*=/creater/]");
    List<String> actors = new ArrayList<>();
    for (Element actorElement : actorElements) {
      actors.add(actorElement.text());
    }
    info.setActors(actors);

    // 分类
    Elements genreElements = doc.select("a[href*=/listpages/]");
    List<String> genres = new ArrayList<>();
    for (Element genreElement : genreElements) {
      genres.add(genreElement.text());
    }
    info.setGenres(genres);

    // 简介
    Element plotElement = doc.selectFirst(".items_article_headerInfo");
    if (plotElement != null) {
      info.setPlot(plotElement.text());
    }

    return info;
  }

  /**
   * 解析时长
   */
  private Integer parseDuration(String durationStr) {
    try {
      // 移除非数字字符
      String digits = durationStr.replaceAll("[^0-9]", "");
      if (!digits.isEmpty()) {
        return Integer.parseInt(digits);
      }
    } catch (NumberFormatException e) {
      log.debug("解析时长失败: {}", durationStr);
    }
    return null;
  }
}
