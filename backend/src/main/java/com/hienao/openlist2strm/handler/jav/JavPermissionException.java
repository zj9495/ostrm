package com.hienao.openlist2strm.handler.jav;

/**
 * JAV 权限异常
 *
 * @author hienao
 * @since 2024-01-01
 */
public class JavPermissionException extends JavCrawlerException {

  public JavPermissionException(String message, JavCrawlerId crawlerId) {
    super("权限错误: " + message, crawlerId);
  }
}
