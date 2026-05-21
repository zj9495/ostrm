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
 * fanza 站点爬虫
 *
 * <p>从 dmm.co.jp 站点抓取影片信息（用于 CID 类型）。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
public class FanzaCrawler implements JavCrawler {

  private static final String BASE_URL = "https://www.dmm.co.jp";
  private static final String SEARCH_URL = BASE_URL + "/digital/videoa/-/list/search/=/searchstr=%s";

  @Override
  public JavCrawlerId getCrawlerId() {
    return JavCrawlerId.FANZA;
  }

  @Override
  public String getName() {
    return "fanza";
  }

  @Override
  public boolean supports(JavDataSourceType dataSourceType) {
    return dataSourceType == JavDataSourceType.CID;
  }

  @Override
  public JavMovieInfo crawl(JavHttpClient httpClient, String number, String cid,
      JavDataSourceType dataSourceType) throws Exception {

    log.info("开始从 fanza 抓取: {}", number);

    try {
      // 使用 CID 搜索
      String searchStr = cid != null ? cid : number;
      String searchUrl = String.format(SEARCH_URL, searchStr);
      Document searchDoc = httpClient.getDocument(searchUrl);

      // 查找影片链接
      String detailUrl = findDetailUrl(searchDoc, searchStr);
      if (detailUrl == null) {
        throw new JavNotFoundException(number, getCrawlerId());
      }

      // 获取详情页
      Document detailDoc = httpClient.getDocument(detailUrl);

      // 解析详情页
      JavMovieInfo result = parseDetailPage(detailDoc, number);
      result.setSourceSite("fanza");

      log.info("成功从 fanza 抓取: {}", number);
      return result;

    } catch (JavNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new JavWebsiteException("fanza 抓取失败: " + e.getMessage(), e, getCrawlerId());
    }
  }

  /**
   * 查找影片详情页 URL
   */
  private String findDetailUrl(Document searchDoc, String searchStr) {
    // 查找搜索结果中的影片链接
    Elements links = searchDoc.select("a[href*=/cid=]");
    for (Element link : links) {
      String href = link.attr("href");
      if (href.contains(searchStr.toLowerCase())) {
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
  private JavMovieInfo parseDetailPage(Document doc, String number) {
    JavMovieInfo info = new JavMovieInfo();
    info.setNumber(number);

    // 标题
    Element titleElement = doc.selectFirst("h1#title");
    if (titleElement != null) {
      info.setTitle(titleElement.text());
    }

    // 封面
    Element coverElement = doc.selectFirst("#sample-video img");
    if (coverElement != null) {
      String coverUrl = coverElement.attr("src");
      if (!coverUrl.isEmpty()) {
        info.setCovers(List.of(coverUrl));
        info.setBigCovers(List.of(coverUrl));
      }
    }

    // 解析元数据
    Elements infoItems = doc.select("table.mg-b20 tr");
    for (Element item : infoItems) {
      String label = item.selectFirst("td:first-child").text();
      String value = item.selectFirst("td:last-child").text();

      if (label.contains("品番：")) {
        info.setNumber(value);
      } else if (label.contains("配信開始日：")) {
        info.setReleaseDate(value);
      } else if (label.contains("収録時間：")) {
        info.setDuration(parseDuration(value));
      } else if (label.contains("メーカー：")) {
        info.setStudio(value);
      } else if (label.contains("レーベル：")) {
        info.setLabel(value);
      } else if (label.contains("シリーズ：")) {
        info.setSeries(value);
      } else if (label.contains("監督：")) {
        info.setDirector(value);
      }
    }

    // 演员
    Elements actorElements = doc.select("a[href*=/article=actress/id=]");
    List<String> actors = new ArrayList<>();
    for (Element actorElement : actorElements) {
      actors.add(actorElement.text());
    }
    info.setActors(actors);

    // 分类
    Elements genreElements = doc.select("a[href*=/article=keyword/id=]");
    List<String> genres = new ArrayList<>();
    for (Element genreElement : genreElements) {
      genres.add(genreElement.text());
    }
    info.setGenres(genres);

    // 简介
    Element plotElement = doc.selectFirst(".txt.introduction");
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
