package com.hienao.openlist2strm.handler;

import com.hienao.openlist2strm.entity.OpenlistConfig;
import com.hienao.openlist2strm.entity.TaskConfig;
import com.hienao.openlist2strm.exception.BusinessException;
import com.hienao.openlist2strm.handler.context.FileProcessingContext;
import com.hienao.openlist2strm.service.LocalFileService;
import com.hienao.openlist2strm.service.OpenlistApiService;
import com.hienao.openlist2strm.service.TaskRunService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件发现处理器
 *
 * <p>负责递归遍历目录，发现所有文件和子目录。
 *
 * <p>Order: 10
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class FileDiscoveryHandler implements FileProcessorHandler {

  private final OpenlistApiService openlistApiService;
  private final LocalFileService localFileService;
  private final TaskRunService taskRunService;

  // ==================== 接口实现 ====================

  @Override
  public FileProcessingResult process(FileProcessingContext context) {
    try {
      log.debug("开始文件发现: {}", context.getRelativePath());

      List<OpenlistApiService.OpenlistFile> allFiles = new ArrayList<>();
      Pattern directoryNameExcludePattern = compileDirectoryNameExcludePattern(context);
      Long taskRunId = context.getAttribute("taskRunId");

      // 递归遍历目录
      processDirectory(
          context.getOpenlistConfig(),
          context.getTaskConfig().getPath(),
          context.getTaskConfig(),
          allFiles,
          directoryNameExcludePattern,
          taskRunId);

      // 将发现的文件列表设置到上下文中
      context.setAttribute("discoveredFiles", allFiles);
      context.getStats().setTotalFiles(allFiles.size());

      log.info("文件发现完成，共发现 {} 个文件/目录", allFiles.size());
      return FileProcessingResult.success();

    } catch (Exception e) {
      log.error("文件发现失败: {}", e.getMessage(), e);
      return FileProcessingResult.failed("文件发现失败: " + e.getMessage());
    }
  }

  @Override
  public Set<FileType> getHandledTypes() {
    return Set.of();
  }

  // ==================== 递归目录遍历 ====================

  /**
   * 递归处理目录
   *
   * @param openlistConfig OpenList 配置
   * @param path 当前路径
   * @param taskConfig 任务配置
   * @param allFiles 收集的文件列表
   */
  private void processDirectory(
      OpenlistConfig openlistConfig,
      String path,
      TaskConfig taskConfig,
      List<OpenlistApiService.OpenlistFile> allFiles,
      Pattern directoryNameExcludePattern,
      Long taskRunId) {

    try {
      boolean isLocal = "LOCAL".equals(openlistConfig.getSourceType());
      List<OpenlistApiService.OpenlistFile> files =
          isLocal
              ? localFileService.listDirectoryContents(path)
              : openlistApiService.getDirectoryContents(openlistConfig, path);

      for (OpenlistApiService.OpenlistFile file : files) {
        if ("folder".equals(file.getType())
            && isDirectoryExcluded(file, directoryNameExcludePattern)) {
          appendDirectorySkipLog(taskRunId, file, taskConfig.getDirectoryNameExcludeRegex());
          continue;
        }

        allFiles.add(file);

        if ("folder".equals(file.getType())) {
          // 递归处理子目录
          String subPath = file.getPath();
          if (subPath == null || subPath.isEmpty()) {
            subPath = path + "/" + file.getName();
          }
          processDirectory(
              openlistConfig,
              subPath,
              taskConfig,
              allFiles,
              directoryNameExcludePattern,
              taskRunId);
        }
      }

      log.trace("处理目录完成: {}, 包含 {} 个文件", path, files.size());

    } catch (Exception e) {
      log.warn("处理目录失败: {}, 错误: {}", path, e.getMessage());
      throw new BusinessException("处理目录失败: " + path + ", 错误: " + e.getMessage(), e);
    }
  }

  private Pattern compileDirectoryNameExcludePattern(FileProcessingContext context) {
    String regex = context.getTaskConfig().getDirectoryNameExcludeRegex();
    if (!StringUtils.hasText(regex)) {
      return null;
    }
    return Pattern.compile(regex);
  }

  private boolean isDirectoryExcluded(
      OpenlistApiService.OpenlistFile file, Pattern directoryNameExcludePattern) {
    if (directoryNameExcludePattern == null) {
      return false;
    }
    return directoryNameExcludePattern.matcher(file.getName()).find();
  }

  private void appendDirectorySkipLog(
      Long taskRunId, OpenlistApiService.OpenlistFile file, String directoryNameExcludeRegex) {
    if (taskRunId == null) {
      return;
    }

    taskRunService.appendLog(
        taskRunId,
        "WARN",
        "目录跳过: "
            + file.getPath()
            + "，字段: directoryNameExcludeRegex"
            + "，目录名称: "
            + file.getName()
            + "，规则: "
            + directoryNameExcludeRegex
            + "，原因: 目录名称匹配 directoryNameExcludeRegex");
  }
}
