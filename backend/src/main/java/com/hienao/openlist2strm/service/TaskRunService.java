package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.entity.TaskRun;
import com.hienao.openlist2strm.entity.TaskRunLog;
import com.hienao.openlist2strm.exception.BusinessException;
import com.hienao.openlist2strm.mapper.TaskRunMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 任务运行记录服务类 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskRunService {

  public static final String STATUS_SUBMITTED = "SUBMITTED";
  public static final String STATUS_RUNNING = "RUNNING";
  public static final String STATUS_SUCCESS = "SUCCESS";
  public static final String STATUS_FAILED = "FAILED";

  private static final String LEVEL_INFO = "INFO";
  private static final String LEVEL_ERROR = "ERROR";

  private final TaskRunMapper taskRunMapper;

  /**
   * 创建运行记录
   *
   * @param taskConfigId 任务配置ID
   * @param isIncrement 是否增量执行
   * @return 运行记录
   */
  @Transactional(rollbackFor = Exception.class)
  public TaskRun createRun(Long taskConfigId, Boolean isIncrement) {
    if (taskConfigId == null) {
      throw new BusinessException("任务配置ID不能为空");
    }
    if (isIncrement == null) {
      throw new BusinessException("执行模式不能为空");
    }

    TaskRun taskRun =
        new TaskRun()
            .setTaskConfigId(taskConfigId)
            .setIsIncrement(isIncrement)
            .setStatus(STATUS_SUBMITTED)
            .setSubmittedAt(LocalDateTime.now());

    int result = taskRunMapper.insertRun(taskRun);
    if (result <= 0) {
      throw new BusinessException("创建任务运行记录失败");
    }

    appendLog(taskRun.getId(), LEVEL_INFO, "任务已提交执行");
    return taskRun;
  }

  /**
   * 标记任务开始运行
   *
   * @param runId 运行记录ID
   */
  @Transactional(rollbackFor = Exception.class)
  public void markRunning(Long runId) {
    TaskRun taskRun =
        new TaskRun()
            .setId(requireRun(runId).getId())
            .setStatus(STATUS_RUNNING)
            .setStartedAt(LocalDateTime.now());
    updateRun(taskRun, "更新任务运行状态失败");
    appendLog(runId, LEVEL_INFO, "任务开始执行");
  }

  /**
   * 标记任务执行成功
   *
   * @param runId 运行记录ID
   */
  @Transactional(rollbackFor = Exception.class)
  public void markSuccess(Long runId) {
    TaskRun existingRun = requireRun(runId);
    if (existingRun.getStartedAt() == null) {
      throw new BusinessException("任务运行记录开始时间不能为空，ID: " + runId);
    }

    LocalDateTime finishedAt = LocalDateTime.now();
    TaskRun taskRun =
        new TaskRun()
            .setId(existingRun.getId())
            .setStatus(STATUS_SUCCESS)
            .setFinishedAt(finishedAt)
            .setDurationMs(Duration.between(existingRun.getStartedAt(), finishedAt).toMillis());
    updateRun(taskRun, "更新任务成功状态失败");
    appendLog(runId, LEVEL_INFO, "任务执行完成");
  }

  /**
   * 标记任务执行失败
   *
   * @param runId 运行记录ID
   * @param errorMessage 失败原因
   */
  @Transactional(rollbackFor = Exception.class)
  public void markFailed(Long runId, String errorMessage) {
    TaskRun existingRun = requireRun(runId);
    if (existingRun.getStartedAt() == null) {
      throw new BusinessException("任务运行记录开始时间不能为空，ID: " + runId);
    }

    LocalDateTime finishedAt = LocalDateTime.now();
    TaskRun taskRun =
        new TaskRun()
            .setId(existingRun.getId())
            .setStatus(STATUS_FAILED)
            .setFinishedAt(finishedAt)
            .setDurationMs(Duration.between(existingRun.getStartedAt(), finishedAt).toMillis())
            .setErrorMessage(errorMessage);
    updateRun(taskRun, "更新任务失败状态失败");
    appendLog(runId, LEVEL_ERROR, "任务执行失败: " + errorMessage);
  }

  /**
   * 写入任务运行日志
   *
   * @param runId 运行记录ID
   * @param level 日志级别
   * @param message 日志内容
   */
  @Transactional(rollbackFor = Exception.class)
  public void appendLog(Long runId, String level, String message) {
    if (runId == null) {
      throw new BusinessException("任务运行记录ID不能为空");
    }
    if (level == null || level.trim().isEmpty()) {
      throw new BusinessException("日志级别不能为空");
    }
    if (message == null || message.trim().isEmpty()) {
      throw new BusinessException("日志内容不能为空");
    }

    TaskRunLog taskRunLog =
        new TaskRunLog()
            .setTaskRunId(runId)
            .setLoggedAt(LocalDateTime.now())
            .setLevel(level)
            .setMessage(message);

    int result = taskRunMapper.insertRunLog(taskRunLog);
    if (result <= 0) {
      throw new BusinessException("写入任务运行日志失败");
    }
  }

  /**
   * 根据ID查询运行记录
   *
   * @param runId 运行记录ID
   * @return 运行记录
   */
  public TaskRun getRunById(Long runId) {
    if (runId == null) {
      throw new BusinessException("任务运行记录ID不能为空");
    }
    return taskRunMapper.selectRunById(runId);
  }

  /**
   * 查询指定任务运行记录
   *
   * @param taskConfigId 任务配置ID
   * @return 运行记录列表
   */
  public List<TaskRun> getRunsByTaskConfigId(Long taskConfigId) {
    if (taskConfigId == null) {
      throw new BusinessException("任务配置ID不能为空");
    }
    return taskRunMapper.selectRunsByTaskConfigId(taskConfigId);
  }

  /**
   * 查询指定运行记录日志
   *
   * @param runId 运行记录ID
   * @return 运行日志列表
   */
  public List<TaskRunLog> getLogsByRunId(Long runId) {
    requireRun(runId);
    return taskRunMapper.selectLogsByTaskRunId(runId);
  }

  private TaskRun requireRun(Long runId) {
    TaskRun taskRun = getRunById(runId);
    if (taskRun == null) {
      throw new BusinessException("任务运行记录不存在，ID: " + runId);
    }
    return taskRun;
  }

  private void updateRun(TaskRun taskRun, String errorMessage) {
    int result = taskRunMapper.updateRunById(taskRun);
    if (result <= 0) {
      throw new BusinessException(errorMessage);
    }
  }
}
