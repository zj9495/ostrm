package com.hienao.openlist2strm.handler;

import com.hienao.openlist2strm.handler.context.FileProcessingContext;

/**
 * 媒体刮削器接口
 *
 * <p>抽象不同刮削器的实现，支持 TMDB 和 JAV 等不同刮削器类型。
 *
 * @author hienao
 * @since 2024-01-01
 */
public interface MediaScraper {

  /**
   * 执行媒体刮削
   *
   * @param context 文件处理上下文
   * @return 处理结果
   */
  FileProcessingResult scrape(FileProcessingContext context);

  /**
   * 获取刮削器类型
   *
   * @return 刮削器类型标识
   */
  String getScraperType();
}
