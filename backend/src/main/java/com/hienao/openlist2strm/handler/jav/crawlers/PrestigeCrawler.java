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
 * prestige 站点爬虫
 *
 * <p>从 prestige-av.com 站点抓取影片信息。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
public class PrestigeCrawler implements JavCrawler {

  private static final String BASE_URL = "https://www.prestige-av.com";
  private static final String SEARCH_URL = BASE_URL + "/goods/search/?keyword=%s";

  @Override
  public JavCrawlerId getCrawlerId() {
    return JavCrawlerId.PRESTIGE;
  }

  @Override
  public String getName() {
    return "prestige";
  }

  @Override
  public boolean supports(JavDataSourceType dataSourceType) {
    return dataSourceType == JavDataSourceType.NORMAL;
  }

  @Override
  public JavMovieInfo crawl(JavHttpClient httpClient, String number, String cid,
      JavDataSourceType dataSourceType) throws Exception {

    log.info("开始从 prestige 抓取: {}", number);

    try {
      // 搜索影片
      String searchUrl = String.format(SEARCH_URL, number);
      Document searchDoc = httpClient.getDocument(searchUrl);

      // 查找影片链接
      String detailUrl = findDetailUrl(searchDoc, number);
      if (detailUrl == null) {
        throw new JavNotFoundException(number, getCrawlerId());
      }

      // 获取详情页
      Document detailDoc = httpClient.getDocument(detailUrl);

      // 解析详情页
      JavMovieInfo result = parseDetailPage(detailDoc, number);
      result.setSourceSite("prestige");

      log.info("成功从 prestige 抓取: {}", number);
      return result;

    } catch (JavNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new JavWebsiteException("prestige 抓取失败: " + e.getMessage(), e, getCrawlerId());
    }
  }

  /**
   * 查找影片详情页 URL
   */
  private String findDetailUrl(Document searchDoc, String number) {
    // 查找搜索结果中的影片链接
    Elements links = searchDoc.select("a[href*=/goods/]");
    for (Element link : links) {
      String href = link.attr("href");
      String text = link.text();
      if (text.toUpperCase().contains(number.toUpperCase())) {
        return BASE_URL + href;
      }
    }

    // 如果没有精确匹配，尝试第一个结果
    if (!links.isEmpty()) {
      return BASE_URL + links.first().attr("href");
    }

    return null;
  }

  /**
   * 解析详情页
   */
  private JavMovieInfo parseDetailPage(Document doc, String number) {
    JavMovieInfo info = new JavMovieInfo();
    info.setNumber(number);

    // 标题
    Element titleElement = doc.selectFirst("h1");
    if (titleElement != null) {
      info.setTitle(titleElement.text());
    }

    // 封面
    Element coverElement = doc.selectFirst(".goods-detail-image img");
    if (coverElement != null) {
      String coverUrl = coverElement.attr("src");
      if (!coverUrl.isEmpty()) {
        if (!coverUrl.startsWith("http")) {
          coverUrl = BASE_URL + coverUrl;
        }
        info.setCovers(List.of(coverUrl));
        info.setBigCovers(List.of(coverUrl));
      }
    }

    // 解析元数据
    Elements infoItems = doc.select(".goods-detail-info .detail-item");
    for (Element item : infoItems) {
      String label = item.selectFirst(".label").text();
      String value = item.selectFirst(".value").text();

      switch (label) {
        case "品番":
          info.setNumber(value);
          break;
        case "発売日":
          info.setReleaseDate(value);
          break;
        case "収録時間":
          info.setDuration(parseDuration(value));
          break;
        case "メーカー":
          info.setStudio(value);
          break;
        case "レーベル":
          info.setLabel(value);
          break;
        case "シリーズ":
          info.setSeries(value);
          break;
        case "監督":
          info.setDirector(value);
          break;
      }
    }

    // 演员
    Elements actorElements = doc.select(".goods-detail-actress a");
    List<String> actors = new ArrayList<>();
    for (Element actorElement : actorElements) {
      actors.add(actorElement.text());
    }
    info.setActors(actors);

    // 分类
    Elements genreElements = doc.select(".goods-detail-genre a");
    List<String> genres = new ArrayList<>();
    for (Element genreElement : genreElements) {
      genres.add(genreElement.text());
    }
    info.setGenres(genres);

    // 简介
    Element plotElement = doc.selectFirst(".goods-detail-comment");
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
