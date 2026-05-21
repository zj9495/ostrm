package com.hienao.openlist2strm.handler.jav;

/**
 * JAV 重复结果异常
 *
 * @author hienao
 * @since 2024-01-01
 */
public class JavDuplicateResultException extends JavCrawlerException {

  public JavDuplicateResultException(String number, JavCrawlerId crawlerId) {
    super("找到多个匹配结果: " + number, crawlerId);
  }
}
