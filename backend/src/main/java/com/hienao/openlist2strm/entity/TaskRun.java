package com.hienao.openlist2strm.entity;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/** 任务运行记录实体类 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TaskRun {

  /** 主键ID */
  private Long id;

  /** 任务配置ID */
  private Long taskConfigId;

  /** 是否增量执行 */
  private Boolean isIncrement;

  /** 运行状态 */
  private String status;

  /** 提交时间 */
  private LocalDateTime submittedAt;

  /** 开始时间 */
  private LocalDateTime startedAt;

  /** 结束时间 */
  private LocalDateTime finishedAt;

  /** 执行耗时 */
  private Long durationMs;

  /** 失败原因 */
  private String errorMessage;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
