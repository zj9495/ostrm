package com.hienao.openlist2strm.handler.jav;

/**
 * JAV 通用爬虫异常
 *
 * @author hienao
 * @since 2024-01-01
 */
public class JavGenericCrawlerException extends JavCrawlerException {

  public JavGenericCrawlerException(String message, JavCrawlerId crawlerId) {
    super(message, crawlerId);
  }

  public JavGenericCrawlerException(String message, Throwable cause, JavCrawlerId crawlerId) {
    super(message, cause, crawlerId);
  }
}
