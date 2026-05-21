package com.hienao.openlist2strm.handler.jav;

/**
 * JAV 站点被阻止异常
 *
 * @author hienao
 * @since 2024-01-01
 */
public class JavSiteBlockedException extends JavCrawlerException {

  public JavSiteBlockedException(String site, JavCrawlerId crawlerId) {
    super("站点被阻止: " + site, crawlerId);
  }

  public JavSiteBlockedException(String site, JavCrawlerId crawlerId, Throwable cause) {
    super("站点被阻止: " + site, cause, crawlerId);
  }
}
