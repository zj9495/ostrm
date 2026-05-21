package com.hienao.openlist2strm.handler.jav;

/**
 * JAV 爬虫异常基类
 *
 * @author hienao
 * @since 2024-01-01
 */
public class JavCrawlerException extends Exception {

  private final JavCrawlerId crawlerId;

  public JavCrawlerException(String message, JavCrawlerId crawlerId) {
    super(message);
    this.crawlerId = crawlerId;
  }

  public JavCrawlerException(String message, Throwable cause, JavCrawlerId crawlerId) {
    super(message, cause);
    this.crawlerId = crawlerId;
  }

  public JavCrawlerId getCrawlerId() {
    return crawlerId;
  }
}
