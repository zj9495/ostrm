package com.hienao.openlist2strm.handler;

import com.hienao.openlist2strm.exception.BusinessException;
import lombok.Getter;

/** 文件处理结果，包含状态和失败/跳过原因。 */
@Getter
public class FileProcessingResult {

  private final ProcessingResult status;
  private final String reason;

  private FileProcessingResult(ProcessingResult status, String reason) {
    this.status = status;
    this.reason = reason;
  }

  public static FileProcessingResult success() {
    return new FileProcessingResult(ProcessingResult.SUCCESS, null);
  }

  public static FileProcessingResult skipped(String reason) {
    return new FileProcessingResult(ProcessingResult.SKIPPED, requireReason(reason));
  }

  public static FileProcessingResult failed(String reason) {
    return new FileProcessingResult(ProcessingResult.FAILED, requireReason(reason));
  }

  public static FileProcessingResult fallback() {
    return new FileProcessingResult(ProcessingResult.FALLBACK, null);
  }

  public boolean isFailed() {
    return ProcessingResult.FAILED.equals(status);
  }

  public boolean isSkipped() {
    return ProcessingResult.SKIPPED.equals(status);
  }

  private static String requireReason(String reason) {
    if (reason == null || reason.trim().isEmpty()) {
      throw new BusinessException("文件处理结果原因不能为空");
    }
    return reason;
  }
}
