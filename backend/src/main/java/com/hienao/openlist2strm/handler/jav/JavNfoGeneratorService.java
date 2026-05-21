package com.hienao.openlist2strm.handler.jav;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * JAV NFO 生成服务
 *
 * <p>移植自 JavSP 的 write_nfo，输出 Kodi movie NFO。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Service
public class JavNfoGeneratorService {

  /**
   * 生成 JAV NFO 文件
   *
   * @param movieInfo 影片信息
   * @param outputPath 输出文件路径
   * @throws IOException IO 异常
   */
  public void generateNfo(JavMovieInfo movieInfo, String outputPath) throws IOException {
    if (movieInfo == null) {
      throw new IllegalArgumentException("影片信息不能为空");
    }

    String nfoContent = buildNfoContent(movieInfo);

    Path path = Paths.get(outputPath);
    Files.createDirectories(path.getParent());
    Files.writeString(path, nfoContent, StandardCharsets.UTF_8);

    log.info("已生成 JAV NFO 文件: {}", outputPath);
  }

  /**
   * 构建 NFO 内容
   */
  private String buildNfoContent(JavMovieInfo movieInfo) {
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
    sb.append("<movie>\n");

    // 标题
    if (!isEmpty(movieInfo.getTitle())) {
      sb.append("  <title>").append(escapeXml(movieInfo.getTitle())).append("</title>\n");
    }

    // 原始标题
    if (!isEmpty(movieInfo.getOriginalTitle())) {
      sb.append("  <originaltitle>").append(escapeXml(movieInfo.getOriginalTitle())).append("</originaltitle>\n");
    }

    // 评分
    if (!isEmpty(movieInfo.getRating())) {
      sb.append("  <rating>").append(escapeXml(movieInfo.getRating())).append("</rating>\n");
    }

    // 简介
    if (!isEmpty(movieInfo.getPlot())) {
      sb.append("  <plot>").append(escapeXml(movieInfo.getPlot())).append("</plot>\n");
    }

    // 时长
    if (movieInfo.getDuration() != null) {
      sb.append("  <runtime>").append(movieInfo.getDuration()).append("</runtime>\n");
    }

    // 番号唯一标识
    if (!isEmpty(movieInfo.getNumber())) {
      sb.append("  <uniqueid type=\"num\">").append(escapeXml(movieInfo.getNumber())).append("</uniqueid>\n");
    }

    // CID
    if (!isEmpty(movieInfo.getCid())) {
      sb.append("  <uniqueid type=\"cid\">").append(escapeXml(movieInfo.getCid())).append("</uniqueid>\n");
    }

    // 分类
    if (movieInfo.getGenres() != null && !movieInfo.getGenres().isEmpty()) {
      for (String genre : movieInfo.getGenres()) {
        sb.append("  <genre>").append(escapeXml(genre)).append("</genre>\n");
      }
    }

    // 标签（与分类相同）
    if (movieInfo.getGenres() != null && !movieInfo.getGenres().isEmpty()) {
      for (String tag : movieInfo.getGenres()) {
        sb.append("  <tag>").append(escapeXml(tag)).append("</tag>\n");
      }
    }

    // 国家
    if (!isEmpty(movieInfo.getCountry())) {
      sb.append("  <country>").append(escapeXml(movieInfo.getCountry())).append("</country>\n");
    }

    // 系列
    if (!isEmpty(movieInfo.getSeries())) {
      sb.append("  <set>").append(escapeXml(movieInfo.getSeries())).append("</set>\n");
    }

    // 导演
    if (!isEmpty(movieInfo.getDirector())) {
      sb.append("  <director>").append(escapeXml(movieInfo.getDirector())).append("</director>\n");
    }

    // 发行日期
    if (!isEmpty(movieInfo.getReleaseDate())) {
      sb.append("  <premiered>").append(escapeXml(movieInfo.getReleaseDate())).append("</premiered>\n");
      sb.append("  <year>").append(extractYear(movieInfo.getReleaseDate())).append("</year>\n");
    }

    // 制作商
    if (!isEmpty(movieInfo.getStudio())) {
      sb.append("  <studio>").append(escapeXml(movieInfo.getStudio())).append("</studio>\n");
    }

    // 发行商
    if (!isEmpty(movieInfo.getLabel())) {
      sb.append("  <label>").append(escapeXml(movieInfo.getLabel())).append("</label>\n");
    }

    // 预告片
    if (!isEmpty(movieInfo.getTrailerUrl())) {
      sb.append("  <trailer>").append(escapeXml(movieInfo.getTrailerUrl())).append("</trailer>\n");
    }

    // 演员
    if (movieInfo.getActors() != null && !movieInfo.getActors().isEmpty()) {
      for (String actor : movieInfo.getActors()) {
        sb.append("  <actor>\n");
        sb.append("    <name>").append(escapeXml(actor)).append("</name>\n");
        sb.append("  </actor>\n");
      }
    }

    // 封面
    if (movieInfo.getCovers() != null && !movieInfo.getCovers().isEmpty()) {
      sb.append("  <fanart>\n");
      for (String cover : movieInfo.getCovers()) {
        sb.append("    <thumb>").append(escapeXml(cover)).append("</thumb>\n");
      }
      sb.append("  </fanart>\n");

      // 第一张封面作为 poster
      sb.append("  <thumb>").append(escapeXml(movieInfo.getCovers().get(0))).append("</thumb>\n");
    }

    sb.append("</movie>\n");
    return sb.toString();
  }

  /**
   * 转义 XML 特殊字符
   */
  private String escapeXml(String text) {
    if (text == null) {
      return "";
    }
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  /**
   * 提取年份
   */
  private String extractYear(String date) {
    if (date == null || date.isEmpty()) {
      return "";
    }
    // 尝试从日期中提取年份
    if (date.length() >= 4) {
      return date.substring(0, 4);
    }
    return "";
  }

  private boolean isEmpty(String str) {
    return str == null || str.trim().isEmpty();
  }
}
