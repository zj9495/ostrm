package com.hienao.openlist2strm.handler.jav;

/**
 * JAV 凭据异常
 *
 * @author hienao
 * @since 2024-01-01
 */
public class JavCredentialException extends JavCrawlerException {

  public JavCredentialException(String site, JavCrawlerId crawlerId) {
    super("凭据错误: " + site, crawlerId);
  }
}
