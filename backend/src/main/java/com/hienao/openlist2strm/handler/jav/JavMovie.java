package com.hienao.openlist2strm.handler.jav;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * JAV 影片信息
 *
 * <p>对应 JavSP 的 Movie 类，包含影片的基本标识信息。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Data
@Accessors(chain = true)
public class JavMovie {

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

  /** 分类标签 */
  private String genres;

  /** 演员列表 */
  private String actors;

  /** 封面地址 */
  private String coverUrl;

  /** 高清封面地址 */
  private String bigCoverUrl;

  /** 剧照地址列表 */
  private String extraFanartUrls;

  /** 预告片地址 */
  private String trailerUrl;

  /** 国家 */
  private String country;

  /** 番号唯一标识 */
  private String uniqueId;
}
