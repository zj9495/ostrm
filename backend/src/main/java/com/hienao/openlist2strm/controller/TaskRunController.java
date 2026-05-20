package com.hienao.openlist2strm.controller;

import com.hienao.openlist2strm.dto.ApiResponse;
import com.hienao.openlist2strm.dto.task.TaskRunDto;
import com.hienao.openlist2strm.dto.task.TaskRunLogDto;
import com.hienao.openlist2strm.entity.TaskRun;
import com.hienao.openlist2strm.entity.TaskRunLog;
import com.hienao.openlist2strm.service.TaskRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 任务运行记录控制器 */
@RestController
@RequestMapping("/api/task-runs")
@RequiredArgsConstructor
@Tag(name = "任务运行记录", description = "任务运行记录和单次运行日志接口")
public class TaskRunController {

  private final TaskRunService taskRunService;

  /** 查询单条运行记录 */
  @GetMapping("/{runId}")
  @Operation(summary = "查询运行记录详情", description = "根据运行记录ID查询单条运行记录")
  public ResponseEntity<ApiResponse<TaskRunDto>> getRunById(
      @Parameter(description = "运行记录ID", required = true) @PathVariable Long runId) {
    TaskRun taskRun = taskRunService.getRunById(runId);
    if (taskRun == null) {
      return ResponseEntity.ok(ApiResponse.error(404, "运行记录不存在"));
    }
    return ResponseEntity.ok(ApiResponse.success(convertToDto(taskRun)));
  }

  /** 查询单次运行日志 */
  @GetMapping("/{runId}/logs")
  @Operation(summary = "查询单次运行日志", description = "根据运行记录ID查询该次任务运行日志")
  public ResponseEntity<ApiResponse<List<TaskRunLogDto>>> getLogsByRunId(
      @Parameter(description = "运行记录ID", required = true) @PathVariable Long runId) {
    List<TaskRunLog> taskRunLogs = taskRunService.getLogsByRunId(runId);
    List<TaskRunLogDto> taskRunLogDtos =
        taskRunLogs.stream().map(this::convertToDto).collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success(taskRunLogDtos));
  }

  private TaskRunDto convertToDto(TaskRun taskRun) {
    TaskRunDto dto = new TaskRunDto();
    BeanUtils.copyProperties(taskRun, dto);
    return dto;
  }

  private TaskRunLogDto convertToDto(TaskRunLog taskRunLog) {
    TaskRunLogDto dto = new TaskRunLogDto();
    BeanUtils.copyProperties(taskRunLog, dto);
    return dto;
  }
}
