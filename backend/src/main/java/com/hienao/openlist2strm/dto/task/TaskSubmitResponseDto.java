package com.hienao.openlist2strm.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 任务提交响应DTO */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmitResponseDto {

  /** 任务运行记录ID */
  private Long runId;

  /** 响应消息 */
  private String message;
}
