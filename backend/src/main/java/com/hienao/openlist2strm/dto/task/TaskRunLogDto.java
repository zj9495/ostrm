package com.hienao.openlist2strm.dto.task;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/** 任务运行日志DTO */
@Data
@Accessors(chain = true)
public class TaskRunLogDto {

  /** 主键ID */
  private Long id;

  /** 任务运行记录ID */
  private Long taskRunId;

  /** 日志时间 */
  private LocalDateTime loggedAt;

  /** 日志级别 */
  private String level;

  /** 日志内容 */
  private String message;
}
