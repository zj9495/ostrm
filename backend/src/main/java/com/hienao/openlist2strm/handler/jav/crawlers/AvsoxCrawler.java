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
 * avsox 站点爬虫
 *
 * <p>从 avsox.click 站点抓取影片信息。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
public class AvsoxCrawler implements JavCrawler {

  private static final String BASE_URL = "https://avsox.click";
  private static final String SEARCH_URL = BASE_URL + "/cn/search/%s";

  @Override
  public JavCrawlerId getCrawlerId() {
    return JavCrawlerId.AVSOX;
  }

  @Override
  public String getName() {
    return "avsox";
  }

  @Override
  public boolean supports(JavDataSourceType dataSourceType) {
    // avsox 支持 normal 和 fc2 类型
    return dataSourceType == JavDataSourceType.NORMAL ||
           dataSourceType == JavDataSourceType.FC2;
  }

  @Override
  public JavMovieInfo crawl(JavHttpClient httpClient, String number, String cid,
      JavDataSourceType dataSourceType) throws Exception {

    log.info("开始从 avsox 抓取: {}", number);

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
      result.setSourceSite("avsox");

      log.info("成功从 avsox 抓取: {}", number);
      return result;

    } catch (JavNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new JavWebsiteException("avsox 抓取失败: " + e.getMessage(), e, getCrawlerId());
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
    Element titleElement = doc.selectFirst("h3");
    if (titleElement != null) {
      info.setTitle(titleElement.text());
    }

    // 封面
    Element coverElement = doc.selectFirst("a.bigImage img");
    if (coverElement != null) {
      String coverUrl = coverElement.attr("src");
      if (!coverUrl.isEmpty()) {
        info.setCovers(List.of(coverUrl));
        info.setBigCovers(List.of(coverUrl));
      }
    }

    // 解析元数据
    Elements infoItems = doc.select(".info p");
    for (Element item : infoItems) {
      String text = item.text();
      if (text.contains("识别码:")) {
        String id = text.replace("识别码:", "").trim();
        info.setNumber(id);
      } else if (text.contains("发行时间:")) {
        String date = text.replace("发行时间:", "").trim();
        info.setReleaseDate(date);
      } else if (text.contains("时长:")) {
        String durationStr = text.replace("时长:", "").trim();
        info.setDuration(parseDuration(durationStr));
      } else if (text.contains("制作商:")) {
        String studio = text.replace("制作商:", "").trim();
        info.setStudio(studio);
      } else if (text.contains("发行商:")) {
        String label = text.replace("发行商:", "").trim();
        info.setLabel(label);
      } else if (text.contains("系列:")) {
        String series = text.replace("系列:", "").trim();
        info.setSeries(series);
      }
    }

    // 演员
    Elements actorElements = doc.select("#star-list a");
    List<String> actors = new ArrayList<>();
    for (Element actorElement : actorElements) {
      actors.add(actorElement.text());
    }
    info.setActors(actors);

    // 分类
    Elements genreElements = doc.select("#genre-list a");
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
