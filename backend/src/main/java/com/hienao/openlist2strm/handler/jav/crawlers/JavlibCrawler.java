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
 * javlib 站点爬虫
 *
 * <p>从 javlibrary.com 站点抓取影片信息。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
public class JavlibCrawler implements JavCrawler {

  private static final String BASE_URL = "https://www.javlibrary.com/cn";
  private static final String SEARCH_URL = BASE_URL + "/vl_searchbyid.php?keyword=%s";

  @Override
  public JavCrawlerId getCrawlerId() {
    return JavCrawlerId.JAVLIB;
  }

  @Override
  public String getName() {
    return "javlib";
  }

  @Override
  public boolean supports(JavDataSourceType dataSourceType) {
    return dataSourceType == JavDataSourceType.NORMAL;
  }

  @Override
  public JavMovieInfo crawl(JavHttpClient httpClient, String number, String cid,
      JavDataSourceType dataSourceType) throws Exception {

    log.info("开始从 javlib 抓取: {}", number);

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
      result.setSourceSite("javlib");

      log.info("成功从 javlib 抓取: {}", number);
      return result;

    } catch (JavNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new JavWebsiteException("javlib 抓取失败: " + e.getMessage(), e, getCrawlerId());
    }
  }

  /**
   * 查找影片详情页 URL
   */
  private String findDetailUrl(Document searchDoc, String number) {
    // 检查是否直接跳转到详情页
    Element videoTitle = searchDoc.selectFirst("#video_title a");
    if (videoTitle != null) {
      return BASE_URL + "/" + videoTitle.attr("href");
    }

    // 查找搜索结果中的影片链接
    Elements links = searchDoc.select(".video a[href*=v=]");
    for (Element link : links) {
      String href = link.attr("href");
      String text = link.text();
      if (text.toUpperCase().contains(number.toUpperCase())) {
        return BASE_URL + "/" + href;
      }
    }

    // 如果没有精确匹配，尝试第一个结果
    if (!links.isEmpty()) {
      return BASE_URL + "/" + links.first().attr("href");
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
    Element titleElement = doc.selectFirst("#video_title a");
    if (titleElement != null) {
      info.setTitle(titleElement.text());
    }

    // 封面
    Element coverElement = doc.selectFirst("#video_jacket_img");
    if (coverElement != null) {
      String coverUrl = coverElement.attr("src");
      if (!coverUrl.isEmpty()) {
        info.setCovers(List.of(coverUrl));
        info.setBigCovers(List.of(coverUrl));
      }
    }

    // 解析元数据
    Element infoElement = doc.selectFirst("#video_info");
    if (infoElement != null) {
      // 番号
      Element idElement = infoElement.selectFirst("#video_id .text");
      if (idElement != null) {
        info.setNumber(idElement.text());
      }

      // 发行日期
      Element dateElement = infoElement.selectFirst("#video_date .text");
      if (dateElement != null) {
        info.setReleaseDate(dateElement.text());
      }

      // 时长
      Element durationElement = infoElement.selectFirst("#video_length .text");
      if (durationElement != null) {
        info.setDuration(parseDuration(durationElement.text()));
      }

      // 导演
      Element directorElement = infoElement.selectFirst("#video_director .text a");
      if (directorElement != null) {
        info.setDirector(directorElement.text());
      }

      // 制作商
      Element makerElement = infoElement.selectFirst("#video_maker .text a");
      if (makerElement != null) {
        info.setStudio(makerElement.text());
      }

      // 发行商
      Element labelElement = infoElement.selectFirst("#video_label .text a");
      if (labelElement != null) {
        info.setLabel(labelElement.text());
      }

      // 评分
      Element ratingElement = infoElement.selectFirst("#video_review .score");
      if (ratingElement != null) {
        info.setRating(ratingElement.text());
      }
    }

    // 演员
    Elements actorElements = doc.select("#starlist a");
    List<String> actors = new ArrayList<>();
    for (Element actorElement : actorElements) {
      actors.add(actorElement.text());
    }
    info.setActors(actors);

    // 分类
    Elements genreElements = doc.select("#genrelist a");
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
