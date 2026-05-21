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
 * dl_getchu 站点爬虫
 *
 * <p>从 dl.getchu.com 站点抓取影片信息（用于 GETCHU 类型）。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
public class DlGetchuCrawler implements JavCrawler {

  private static final String BASE_URL = "https://dl.getchu.com";
  private static final String SEARCH_URL = BASE_URL + "/search/search.cgi?keyword=%s";

  @Override
  public JavCrawlerId getCrawlerId() {
    return JavCrawlerId.DL_GETCHU;
  }

  @Override
  public String getName() {
    return "dl_getchu";
  }

  @Override
  public boolean supports(JavDataSourceType dataSourceType) {
    return dataSourceType == JavDataSourceType.GETCHU;
  }

  @Override
  public JavMovieInfo crawl(JavHttpClient httpClient, String number, String cid,
      JavDataSourceType dataSourceType) throws Exception {

    log.info("开始从 dl_getchu 抓取: {}", number);

    try {
      // 提取 GETCHU 编号
      String getchuId = extractGetchuId(number);
      if (getchuId == null) {
        throw new JavNotFoundException(number, getCrawlerId());
      }

      // 搜索影片
      String searchUrl = String.format(SEARCH_URL, getchuId);
      Document searchDoc = httpClient.getDocument(searchUrl);

      // 查找影片链接
      String detailUrl = findDetailUrl(searchDoc, getchuId);
      if (detailUrl == null) {
        throw new JavNotFoundException(number, getCrawlerId());
      }

      // 获取详情页
      Document detailDoc = httpClient.getDocument(detailUrl);

      // 解析详情页
      JavMovieInfo result = parseDetailPage(detailDoc, number);
      result.setSourceSite("dl_getchu");

      log.info("成功从 dl_getchu 抓取: {}", number);
      return result;

    } catch (JavNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new JavWebsiteException("dl_getchu 抓取失败: " + e.getMessage(), e, getCrawlerId());
    }
  }

  /**
   * 提取 GETCHU 编号
   */
  private String extractGetchuId(String number) {
    // 移除 GETCHU- 前缀
    String id = number.toUpperCase().replace("GETCHU-", "").replace("GETCHU", "");
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
  private String findDetailUrl(Document searchDoc, String getchuId) {
    // 查找搜索结果中的影片链接
    Elements links = searchDoc.select("a[href*=/item/]");
    for (Element link : links) {
      String href = link.attr("href");
      if (href.contains(getchuId)) {
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
    Element coverElement = doc.selectFirst(".item-detail img");
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
    Elements infoItems = doc.select(".item-info tr");
    for (Element item : infoItems) {
      String label = item.selectFirst("td:first-child").text();
      String value = item.selectFirst("td:last-child").text();

      if (label.contains("品番：")) {
        info.setNumber(value);
      } else if (label.contains("配信日：")) {
        info.setReleaseDate(value);
      } else if (label.contains("ファイル容量：")) {
        // GETCHU 通常不提供时长，但可以记录文件大小
      }
    }

    // 分类
    Elements genreElements = doc.select("a[href*=/search/]");
    List<String> genres = new ArrayList<>();
    for (Element genreElement : genreElements) {
      genres.add(genreElement.text());
    }
    info.setGenres(genres);

    // 简介
    Element plotElement = doc.selectFirst(".item-detail");
    if (plotElement != null) {
      info.setPlot(plotElement.text());
    }

    return info;
  }
}
