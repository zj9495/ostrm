package com.hienao.openlist2strm.handler.jav.crawlers;

import com.hienao.openlist2strm.handler.jav.JavCrawler;
import com.hienao.openlist2strm.handler.jav.JavCrawlerId;
import com.hienao.openlist2strm.handler.jav.JavDataSourceType;
import com.hienao.openlist2strm.handler.jav.JavHttpClient;
import com.hienao.openlist2strm.handler.jav.JavMovieInfo;
import com.hienao.openlist2strm.handler.jav.JavNotFoundException;
import com.hienao.openlist2strm.handler.jav.JavWebsiteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * javdb 站点爬虫
 *
 * <p>从 javdb.com 站点抓取影片信息。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
public class JavdbCrawler implements JavCrawler {

  private static final String BASE_URL = "https://javdb.com";
  private static final String SEARCH_URL = BASE_URL + "/search?q=%s";

  // javdb 分类映射
  private static final Map<String, String> GENRE_MAP = new HashMap<>();

  static {
    // 初始化分类映射（从 javdb 分类到标准分类）
    GENRE_MAP.put("高清", "高清");
    GENRE_MAP.put("字幕", "字幕");
    GENRE_MAP.put("制服", "制服");
    GENRE_MAP.put("学生", "学生");
    GENRE_MAP.put("OL", "OL");
    GENRE_MAP.put("护士", "护士");
    GENRE_MAP.put("女教師", "女教师");
    GENRE_MAP.put("巨乳", "巨乳");
    GENRE_MAP.put("貧乳", "贫乳");
    GENRE_MAP.put("美乳", "美乳");
    GENRE_MAP.put("淫亂", "淫乱");
    GENRE_MAP.put("素人", "素人");
    GENRE_MAP.put("企畫", "企画");
    GENRE_MAP.put("単体作品", "単体作品");
    GENRE_MAP.put("多人數", "多人数");
    GENRE_MAP.put("美少女", "美少女");
    GENRE_MAP.put("美腳", "美脚");
    GENRE_MAP.put("痴女", "痴女");
    GENRE_MAP.put("人妻", "人妻");
    GENRE_MAP.put("熟女", "熟女");
    GENRE_MAP.put("中出", "中出");
    GENRE_MAP.put("顏射", "颜射");
    GENRE_MAP.put("口交", "口交");
    GENRE_MAP.put("乳交", "乳交");
    GENRE_MAP.put("肛交", "肛交");
    GENRE_MAP.put("SM", "SM");
    GENRE_MAP.put("捆綁", "捆绑");
    GENRE_MAP.put("調教", "调教");
    GENRE_MAP.put("羞恥", "羞耻");
    GENRE_MAP.put("露出", "露出");
    GENRE_MAP.put("偷拍", "偷拍");
    GENRE_MAP.put("痴漢", "痴汉");
    GENRE_MAP.put("校園", "校园");
    GENRE_MAP.put("職場", "职场");
    GENRE_MAP.put("家庭", "家庭");
    GENRE_MAP.put("溫泉", "温泉");
    GENRE_MAP.put("泳裝", "泳装");
    GENRE_MAP.put("Cosplay", "Cosplay");
    GENRE_MAP.put("動畫", "动画");
  }

  @Override
  public JavCrawlerId getCrawlerId() {
    return JavCrawlerId.JAVDB;
  }

  @Override
  public String getName() {
    return "javdb";
  }

  @Override
  public boolean supports(JavDataSourceType dataSourceType) {
    // javdb 支持 normal 和 fc2 类型
    return dataSourceType == JavDataSourceType.NORMAL ||
           dataSourceType == JavDataSourceType.FC2;
  }

  @Override
  public JavMovieInfo crawl(JavHttpClient httpClient, String number, String cid,
      JavDataSourceType dataSourceType) throws Exception {

    log.info("开始从 javdb 抓取: {}", number);

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
      result.setSourceSite("javdb");

      log.info("成功从 javdb 抓取: {}", number);
      return result;

    } catch (JavNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new JavWebsiteException("javdb 抓取失败: " + e.getMessage(), e, getCrawlerId());
    }
  }

  /**
   * 查找影片详情页 URL
   */
  private String findDetailUrl(Document searchDoc, String number) {
    // 查找搜索结果中的影片链接
    Elements links = searchDoc.select("a[href*=/v/]");
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
    Element titleElement = doc.selectFirst("h2.title");
    if (titleElement != null) {
      info.setTitle(titleElement.text());
    }

    // 封面
    Element coverElement = doc.selectFirst("img.video-cover");
    if (coverElement != null) {
      String coverUrl = coverElement.attr("src");
      if (!coverUrl.isEmpty()) {
        info.setCovers(List.of(coverUrl));
        info.setBigCovers(List.of(coverUrl));
      }
    }

    // 解析元数据
    Elements infoItems = doc.select(".movie-panel-info .panel-block");
    for (Element item : infoItems) {
      String label = item.selectFirst("strong").text();
      String value = item.selectFirst("span").text();

      switch (label) {
        case "番號:":
          info.setNumber(value);
          break;
        case "日期:":
          info.setReleaseDate(value);
          break;
        case "時長:":
          info.setDuration(parseDuration(value));
          break;
        case "片商:":
          info.setStudio(value);
          break;
        case "發行:":
          info.setLabel(value);
          break;
        case "系列:":
          info.setSeries(value);
          break;
        case "導演:":
          info.setDirector(value);
          break;
      }
    }

    // 演员
    Elements actorElements = doc.select(".panel-block a[href*=/actors/]");
    List<String> actors = new ArrayList<>();
    for (Element actorElement : actorElements) {
      actors.add(actorElement.text());
    }
    info.setActors(actors);

    // 分类
    Elements genreElements = doc.select(".panel-block a[href*=/tags/]");
    List<String> genres = new ArrayList<>();
    for (Element genreElement : genreElements) {
      String genre = genreElement.text();
      // 应用分类映射
      String mappedGenre = GENRE_MAP.getOrDefault(genre, genre);
      genres.add(mappedGenre);
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
