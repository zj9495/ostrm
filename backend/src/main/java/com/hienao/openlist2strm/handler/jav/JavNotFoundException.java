package com.hienao.openlist2strm.handler.jav;

/**
 * JAV 影片未找到异常
 *
 * @author hienao
 * @since 2024-01-01
 */
public class JavNotFoundException extends JavCrawlerException {

  public JavNotFoundException(String number, JavCrawlerId crawlerId) {
    super("未找到影片: " + number, crawlerId);
  }

  public JavNotFoundException(String number, JavCrawlerId crawlerId, Throwable cause) {
    super("未找到影片: " + number, cause, crawlerId);
  }
}
