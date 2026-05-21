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
 * javmenu 站点爬虫
 *
 * <p>从 javmenu.com 站点抓取影片信息。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
public class JavmenuCrawler implements JavCrawler {

  private static final String BASE_URL = "https://javmenu.com";
  private static final String SEARCH_URL = BASE_URL + "/search?q=%s";

  @Override
  public JavCrawlerId getCrawlerId() {
    return JavCrawlerId.JAVMENU;
  }

  @Override
  public String getName() {
    return "javmenu";
  }

  @Override
  public boolean supports(JavDataSourceType dataSourceType) {
    return dataSourceType == JavDataSourceType.FC2;
  }

  @Override
  public JavMovieInfo crawl(JavHttpClient httpClient, String number, String cid,
      JavDataSourceType dataSourceType) throws Exception {

    log.info("开始从 javmenu 抓取: {}", number);

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
      result.setSourceSite("javmenu");

      log.info("成功从 javmenu 抓取: {}", number);
      return result;

    } catch (JavNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new JavWebsiteException("javmenu 抓取失败: " + e.getMessage(), e, getCrawlerId());
    }
  }

  /**
   * 查找影片详情页 URL
   */
  private String findDetailUrl(Document searchDoc, String number) {
    // 查找搜索结果中的影片链接
    Elements links = searchDoc.select("a[href*=/movie/]");
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
    Element coverElement = doc.selectFirst(".movie-poster img");
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
    Elements infoItems = doc.select(".movie-info .info-item");
    for (Element item : infoItems) {
      String label = item.selectFirst(".label").text();
      String value = item.selectFirst(".value").text();

      switch (label) {
        case "ID:":
          info.setNumber(value);
          break;
        case "配信日:":
          info.setReleaseDate(value);
          break;
        case "収録時間:":
          info.setDuration(parseDuration(value));
          break;
        case "メーカー:":
          info.setStudio(value);
          break;
        case "レーベル:":
          info.setLabel(value);
          break;
        case "シリーズ:":
          info.setSeries(value);
          break;
      }
    }

    // 演员
    Elements actorElements = doc.select("a[href*=/actor/]");
    List<String> actors = new ArrayList<>();
    for (Element actorElement : actorElements) {
      actors.add(actorElement.text());
    }
    info.setActors(actors);

    // 分类
    Elements genreElements = doc.select("a[href*=/genre/]");
    List<String> genres = new ArrayList<>();
    for (Element genreElement : genreElements) {
      genres.add(genreElement.text());
    }
    info.setGenres(genres);

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
