package com.hienao.openlist2strm.handler.jav;

/**
 * JAV 网站异常
 *
 * @author hienao
 * @since 2024-01-01
 */
public class JavWebsiteException extends JavCrawlerException {

  public JavWebsiteException(String message, JavCrawlerId crawlerId) {
    super("网站错误: " + message, crawlerId);
  }

  public JavWebsiteException(String message, Throwable cause, JavCrawlerId crawlerId) {
    super("网站错误: " + message, cause, crawlerId);
  }
}
