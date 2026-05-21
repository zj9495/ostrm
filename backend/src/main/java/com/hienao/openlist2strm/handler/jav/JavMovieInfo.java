package com.hienao.openlist2strm.handler.jav;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * JAV 影片详细信息
 *
 * <p>对应 JavSP 的 MovieInfo 类，包含从站点抓取的完整影片信息。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Data
@Accessors(chain = true)
public class JavMovieInfo {

  /** 影片番号 */
  private String number;

  /** 影片 CID */
  private String cid;

  /** 数据源类型 */
  private JavDataSourceType dataSourceType;

  /** 影片标题 */
  private String title;

  /** 原始标题 */
  private String originalTitle;

  /** 发行日期 */
  private String releaseDate;

  /** 时长（分钟） */
  private Integer duration;

  /** 制作商 */
  private String studio;

  /** 发行商 */
  private String label;

  /** 系列 */
  private String series;

  /** 导演 */
  private String director;

  /** 简介 */
  private String plot;

  /** 评分 */
  private String rating;

  /** 分类标签列表 */
  private List<String> genres = new ArrayList<>();

  /** 演员列表 */
  private List<String> actors = new ArrayList<>();

  /** 封面地址列表（用于排序和选择） */
  private List<String> covers = new ArrayList<>();

  /** 高清封面地址列表（用于排序和选择） */
  private List<String> bigCovers = new ArrayList<>();

  /** 剧照地址列表 */
  private List<String> extraFanartUrls = new ArrayList<>();

  /** 预告片地址 */
  private String trailerUrl;

  /** 国家 */
  private String country;

  /** 番号唯一标识 */
  private String uniqueId;

  /** 抓取来源站点 */
  private String sourceSite;

  /** 是否有硬字幕 */
  private boolean hasHardSubtitle;

  /** 是否无码 */
  private boolean isUncensored;

  /**
   * 合并另一个 JavMovieInfo 的信息
   *
   * <p>按优先级填充当前为空的字段
   *
   * @param other 另一个 JavMovieInfo
   */
  public void mergeFrom(JavMovieInfo other) {
    if (other == null) {
      return;
    }

    // 按优先级填充空字段
    if (isEmpty(this.number) && !isEmpty(other.number)) {
      this.number = other.number;
    }
    if (isEmpty(this.cid) && !isEmpty(other.cid)) {
      this.cid = other.cid;
    }
    if (isEmpty(this.title) && !isEmpty(other.title)) {
      this.title = other.title;
    }
    if (isEmpty(this.originalTitle) && !isEmpty(other.originalTitle)) {
      this.originalTitle = other.originalTitle;
    }
    if (isEmpty(this.releaseDate) && !isEmpty(other.releaseDate)) {
      this.releaseDate = other.releaseDate;
    }
    if (this.duration == null && other.duration != null) {
      this.duration = other.duration;
    }
    if (isEmpty(this.studio) && !isEmpty(other.studio)) {
      this.studio = other.studio;
    }
    if (isEmpty(this.label) && !isEmpty(other.label)) {
      this.label = other.label;
    }
    if (isEmpty(this.series) && !isEmpty(other.series)) {
      this.series = other.series;
    }
    if (isEmpty(this.director) && !isEmpty(other.director)) {
      this.director = other.director;
    }
    if (isEmpty(this.plot) && !isEmpty(other.plot)) {
      this.plot = other.plot;
    }
    if (isEmpty(this.rating) && !isEmpty(other.rating)) {
      this.rating = other.rating;
    }
    if (isEmpty(this.trailerUrl) && !isEmpty(other.trailerUrl)) {
      this.trailerUrl = other.trailerUrl;
    }
    if (isEmpty(this.country) && !isEmpty(other.country)) {
      this.country = other.country;
    }
    if (isEmpty(this.uniqueId) && !isEmpty(other.uniqueId)) {
      this.uniqueId = other.uniqueId;
    }

    // 合并列表字段
    if (other.genres != null && !other.genres.isEmpty()) {
      for (String genre : other.genres) {
        if (!this.genres.contains(genre)) {
          this.genres.add(genre);
        }
      }
    }
    if (other.actors != null && !other.actors.isEmpty()) {
      for (String actor : other.actors) {
        if (!this.actors.contains(actor)) {
          this.actors.add(actor);
        }
      }
    }
    if (other.covers != null && !other.covers.isEmpty()) {
      this.covers.addAll(other.covers);
    }
    if (other.bigCovers != null && !other.bigCovers.isEmpty()) {
      this.bigCovers.addAll(other.bigCovers);
    }
    if (other.extraFanartUrls != null && !other.extraFanartUrls.isEmpty()) {
      this.extraFanartUrls.addAll(other.extraFanartUrls);
    }

    // 合并布尔字段
    if (other.hasHardSubtitle) {
      this.hasHardSubtitle = true;
    }
    if (other.isUncensored) {
      this.isUncensored = true;
    }
  }

  /**
   * 检测特殊属性
   *
   * <p>移植自 JavSP 的特殊属性检测逻辑
   *
   * @param fileName 文件名
   * @param genres 分类标签列表
   */
  public void detectSpecialAttributes(String fileName, java.util.List<String> genres) {
    // 检测硬字幕
    this.hasHardSubtitle = detectHardSubtitle(fileName, genres);

    // 检测无码
    this.isUncensored = detectUncensored(fileName, genres, this.number);
  }

  /**
   * 检测硬字幕
   *
   * <p>移植自 JavSP 的硬字幕检测逻辑
   *
   * @param fileName 文件名
   * @param genres 分类标签列表
   * @return 是否有硬字幕
   */
  private boolean detectHardSubtitle(String fileName, java.util.List<String> genres) {
    if (fileName == null) {
      return false;
    }

    String lowerFileName = fileName.toLowerCase();

    // 文件名中包含硬字幕标记
    if (lowerFileName.contains("chs") || lowerFileName.contains("cht") ||
        lowerFileName.contains("sub") || lowerFileName.contains("字幕")) {
      return true;
    }

    // 分类中包含硬字幕标签
    if (genres != null) {
      for (String genre : genres) {
        String lowerGenre = genre.toLowerCase();
        if (lowerGenre.contains("hard") && lowerGenre.contains("sub") ||
            lowerGenre.contains("硬字幕")) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 检测无码
   *
   * <p>移植自 JavSP 的无码检测逻辑
   *
   * @param fileName 文件名
   * @param genres 分类标签列表
   * @param number 番号
   * @return 是否无码
   */
  private boolean detectUncensored(String fileName, java.util.List<String> genres, String number) {
    if (fileName == null) {
      return false;
    }

    String lowerFileName = fileName.toLowerCase();

    // 文件名中包含无码标记
    if (lowerFileName.contains("uncensored") || lowerFileName.contains("无码") ||
        lowerFileName.contains("无修正")) {
      return true;
    }

    // 分类中包含无码标签
    if (genres != null) {
      for (String genre : genres) {
        String lowerGenre = genre.toLowerCase();
        if (lowerGenre.contains("uncensored") || lowerGenre.contains("无码") ||
            lowerGenre.contains("无修正")) {
          return true;
        }
      }
    }

    // 特定番号前缀通常是无码
    if (number != null) {
      String upperNumber = number.toUpperCase();
      String[] uncensoredPrefixes = {
        "HEYZO", "259LUXU", "200GANA", "1000GIRL", "1PONDO", "CARIB",
        "PACOPACOMAMA", "10MUSUME", "GACHINCO", "AZYO", "FSET"
      };
      for (String prefix : uncensoredPrefixes) {
        if (upperNumber.startsWith(prefix)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean isEmpty(String str) {
    return str == null || str.trim().isEmpty();
  }
}
