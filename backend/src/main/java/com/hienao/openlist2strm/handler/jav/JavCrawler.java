package com.hienao.openlist2strm.handler.jav;

/**
 * JAV 站点爬虫接口
 *
 * <p>每个站点爬虫实现此接口，负责从特定站点抓取影片信息。
 *
 * @author hienao
 * @since 2024-01-01
 */
public interface JavCrawler {

  /**
   * 获取爬虫 ID
   *
   * @return 爬虫 ID
   */
  JavCrawlerId getCrawlerId();

  /**
   * 获取爬虫名称
   *
   * @return 爬虫名称
   */
  String getName();

  /**
   * 抓取影片信息
   *
   * @param httpClient HTTP 客户端
   * @param number 番号
   * @param cid CID
   * @param dataSourceType 数据源类型
   * @return 影片信息，如果未找到则返回 null
   * @throws Exception 抓取异常
   */
  JavMovieInfo crawl(JavHttpClient httpClient, String number, String cid,
      JavDataSourceType dataSourceType) throws Exception;

  /**
   * 检查是否支持指定的数据源类型
   *
   * @param dataSourceType 数据源类型
   * @return 是否支持
   */
  boolean supports(JavDataSourceType dataSourceType);
}
