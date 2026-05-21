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
 * javbus 站点爬虫
 *
 * <p>从 javbus.com 站点抓取影片信息。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
public class JavbusCrawler implements JavCrawler {

  private static final String BASE_URL = "https://www.javbus.com";
  private static final String SEARCH_URL = BASE_URL + "/search/%s";

  // javbus 分类映射
  private static final Map<String, String> GENRE_MAP = new HashMap<>();

  static {
    // 初始化分类映射（从 javbus 分类到标准分类）
    GENRE_MAP.put("高清", "高清");
    GENRE_MAP.put("字幕", "字幕");
    GENRE_MAP.put("制服", "制服");
    GENRE_MAP.put("学生", "学生");
    GENRE_MAP.put("OL", "OL");
    GENRE_MAP.put("护士", "护士");
    GENRE_MAP.put("女教师", "女教师");
    GENRE_MAP.put("巨乳", "巨乳");
    GENRE_MAP.put("贫乳", "贫乳");
    GENRE_MAP.put("美乳", "美乳");
    GENRE_MAP.put("淫乱", "淫乱");
    GENRE_MAP.put("素人", "素人");
    GENRE_MAP.put("企画", "企画");
    GENRE_MAP.put("単体作品", "単体作品");
    GENRE_MAP.put("多人数", "多人数");
    GENRE_MAP.put("美少女", "美少女");
    GENRE_MAP.put("美脚", "美脚");
    GENRE_MAP.put("痴女", "痴女");
    GENRE_MAP.put("人妻", "人妻");
    GENRE_MAP.put("熟女", "熟女");
    GENRE_MAP.put("中出", "中出");
    GENRE_MAP.put("颜射", "颜射");
    GENRE_MAP.put("口交", "口交");
    GENRE_MAP.put("乳交", "乳交");
    GENRE_MAP.put("肛交", "肛交");
    GENRE_MAP.put("SM", "SM");
    GENRE_MAP.put("捆绑", "捆绑");
    GENRE_MAP.put("调教", "调教");
    GENRE_MAP.put("羞耻", "羞耻");
    GENRE_MAP.put("露出", "露出");
    GENRE_MAP.put("偷拍", "偷拍");
    GENRE_MAP.put("痴汉", "痴汉");
    GENRE_MAP.put("校园", "校园");
    GENRE_MAP.put("职场", "职场");
    GENRE_MAP.put("家庭", "家庭");
    GENRE_MAP.put("温泉", "温泉");
    GENRE_MAP.put("泳装", "泳装");
    GENRE_MAP.put("Cosplay", "Cosplay");
    GENRE_MAP.put("动画", "动画");
  }

  @Override
  public JavCrawlerId getCrawlerId() {
    return JavCrawlerId.JAVBUS;
  }

  @Override
  public String getName() {
    return "javbus";
  }

  @Override
  public boolean supports(JavDataSourceType dataSourceType) {
    return dataSourceType == JavDataSourceType.NORMAL;
  }

  @Override
  public JavMovieInfo crawl(JavHttpClient httpClient, String number, String cid,
      JavDataSourceType dataSourceType) throws Exception {

    log.info("开始从 javbus 抓取: {}", number);

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
      result.setSourceSite("javbus");

      log.info("成功从 javbus 抓取: {}", number);
      return result;

    } catch (JavNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new JavWebsiteException("javbus 抓取失败: " + e.getMessage(), e, getCrawlerId());
    }
  }

  /**
   * 查找影片详情页 URL
   */
  private String findDetailUrl(Document searchDoc, String number) {
    // 查找搜索结果中的影片链接
    Elements links = searchDoc.select("a[href*=/]");
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
        // 将缩略图 URL 转换为大图 URL
        coverUrl = coverUrl.replace("thumbs", "pics").replace("pl.jpg", ".jpg");
        info.setCovers(List.of(coverUrl));
        info.setBigCovers(List.of(coverUrl));
      }
    }

    // 解析元数据
    Elements infoItems = doc.select(".info p");
    for (Element item : infoItems) {
      String text = item.text();
      if (text.contains("識別碼:")) {
        String id = text.replace("識別碼:", "").trim();
        info.setNumber(id);
      } else if (text.contains("發行日期:")) {
        String date = text.replace("發行日期:", "").trim();
        info.setReleaseDate(date);
      } else if (text.contains("長度:")) {
        String durationStr = text.replace("長度:", "").replace("分鐘", "").trim();
        info.setDuration(parseDuration(durationStr));
      } else if (text.contains("製作商:")) {
        String studio = text.replace("製作商:", "").trim();
        info.setStudio(studio);
      } else if (text.contains("發行商:")) {
        String label = text.replace("發行商:", "").trim();
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
