/*
 * OStrm - Stream Management System
 * Copyright (C) 2024 OStrm Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.dto.task.TaskSubmitResponseDto;
import com.hienao.openlist2strm.entity.OpenlistConfig;
import com.hienao.openlist2strm.entity.TaskConfig;
import com.hienao.openlist2strm.entity.TaskRun;
import com.hienao.openlist2strm.exception.BusinessException;
import com.hienao.openlist2strm.handler.FileDiscoveryHandler;
import com.hienao.openlist2strm.handler.FileFilterHandler;
import com.hienao.openlist2strm.handler.FileProcessingResult;
import com.hienao.openlist2strm.handler.FileProcessorChain;
import com.hienao.openlist2strm.handler.context.FileProcessingContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 任务执行服务类
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionService {

  private final TaskConfigService taskConfigService;
  private final OpenlistConfigService openlistConfigService;
  private final OpenlistApiService openlistApiService;
  private final StrmFileService strmFileService;
  private final MediaScrapingService mediaScrapingService;
  private final SystemConfigService systemConfigService;
  private final FileProcessorChain fileProcessorChain;
  private final Executor taskSubmitExecutor;
  private final TaskRunService taskRunService;

  /**
   * 提交任务到线程池执行
   *
   * @param taskId 任务ID
   * @param isIncrement 是否增量执行（可选参数）
   */
  public TaskSubmitResponseDto submitTask(Long taskId, Boolean isIncrement) {
    log.info("提交任务到线程池 - 任务ID: {}, 增量模式: {}", taskId, isIncrement);

    TaskConfig taskConfig = getRunnableTaskConfig(taskId);
    boolean useIncrement = resolveIsIncrement(taskConfig, isIncrement);
    TaskRun taskRun = taskRunService.createRun(taskId, useIncrement);

    // 使用线程池异步执行任务
    taskSubmitExecutor.execute(
        () -> {
          try {
            executeTaskSync(taskConfig, useIncrement, taskRun.getId());
          } catch (Exception e) {
            log.error("任务执行失败 - 任务ID: {}, 错误信息: {}", taskId, e.getMessage(), e);
          }
        });

    log.info("任务已成功提交到线程池 - 任务ID: {}, 运行记录ID: {}", taskId, taskRun.getId());
    return new TaskSubmitResponseDto(taskRun.getId(), "任务已提交执行");
  }

  /**
   * 同步执行任务（在线程池中调用）
   *
   * @param taskConfig 任务配置
   * @param useIncrement 是否增量执行
   * @param taskRunId 任务运行记录ID
   */
  private void executeTaskSync(TaskConfig taskConfig, boolean useIncrement, Long taskRunId) {
    try {
      taskRunService.markRunning(taskRunId);
      log.info(
          "开始执行任务 - 任务ID: {}, 运行记录ID: {}, 增量模式: {}, 线程: {}",
          taskConfig.getId(),
          taskRunId,
          useIncrement,
          Thread.currentThread().getName());

      // 更新任务开始执行时间
      taskConfigService.updateLastExecTime(taskConfig.getId(), LocalDateTime.now());

      // 执行具体的任务逻辑
      executeTaskLogic(taskConfig, useIncrement, taskRunId);

      taskRunService.markSuccess(taskRunId);

      log.info(
          "任务执行完成 - 任务ID: {}, 任务名称: {}, 运行记录ID: {}, 增量模式: {}",
          taskConfig.getId(),
          taskConfig.getTaskName(),
          taskRunId,
          useIncrement);

    } catch (Exception e) {
      taskRunService.markFailed(taskRunId, e.getMessage());
      log.error("任务执行失败 - 任务ID: {}, 错误信息: {}", taskConfig.getId(), e.getMessage(), e);
      throw new BusinessException("任务执行失败: " + e.getMessage(), e);
    }
  }

  /**
   * 异步执行任务（保留原有方法以兼容其他调用）
   *
   * @param taskId 任务ID
   * @param isIncrement 是否增量执行（可选参数）
   * @return CompletableFuture<Void>
   */
  public CompletableFuture<Void> executeTask(Long taskId, Boolean isIncrement) {
    submitTask(taskId, isIncrement);
    return CompletableFuture.completedFuture(null);
  }

  /**
   * 执行具体的任务逻辑 1. 根据任务配置获取OpenList配置 2. 如果是全量执行，先清空STRM目录 3. 通过OpenList API递归获取所有文件 4. 对视频文件生成STRM文件
   * 5. 保持目录结构一致 6. 如果是增量执行，清理孤立的STRM文件
   *
   * @param taskConfig 任务配置
   * @param isIncrement 是否增量执行
   */
  private void executeTaskLogic(TaskConfig taskConfig, boolean isIncrement, Long taskRunId) {
    log.info("开始执行任务逻辑: {}, 增量模式: {}", taskConfig.getTaskName(), isIncrement);
    taskRunService.appendLog(taskRunId, "INFO", "开始执行任务逻辑: " + taskConfig.getTaskName());

    try {
      // 1. 获取OpenList配置
      OpenlistConfig openlistConfig = getOpenlistConfig(taskConfig);

      // 2. 使用 Handler 链处理方式执行任务
      log.info("使用 Handler 链处理方式执行任务");
      executeTaskWithHandlerChain(taskConfig, openlistConfig, isIncrement, taskRunId);

    } catch (Exception e) {
      log.error("任务执行失败: {}, 错误: {}", taskConfig.getTaskName(), e.getMessage(), e);
      throw new BusinessException("任务执行失败: " + e.getMessage(), e);
    }
  }

  /** 使用 Handler 链执行任务（新方式） */
  private void executeTaskWithHandlerChain(
      TaskConfig taskConfig, OpenlistConfig openlistConfig, boolean isIncrement, Long taskRunId) {

    // 1. 创建处理上下文
    FileProcessingContext context =
        FileProcessingContext.builder()
            .openlistConfig(openlistConfig)
            .taskConfig(taskConfig)
            .build();
    context.setAttribute("taskRunId", taskRunId);

    try {
      log.info("开始获取 OpenList 文件列表: {}", taskConfig.getPath());
      taskRunService.appendLog(taskRunId, "INFO", "开始获取 OpenList 文件列表: " + taskConfig.getPath());
      FileDiscoveryHandler discoveryHandler = fileProcessorChain.getHandler(FileDiscoveryHandler.class);
      if (discoveryHandler == null) {
        throw new BusinessException("文件发现处理器不存在");
      }
      FileProcessingResult discoveryResult = discoveryHandler.process(context);
      if (discoveryResult.isFailed()) {
        throw new BusinessException(discoveryResult.getReason());
      }
    } catch (Exception e) {
      log.error("获取 OpenList 文件列表失败，终止任务执行，STRM 目录未受影响: {}", e.getMessage(), e);
      taskRunService.appendLog(taskRunId, "ERROR", "获取 OpenList 文件列表失败: " + e.getMessage());
      throw new BusinessException("获取 OpenList 文件列表失败，任务终止: " + e.getMessage(), e);
    }

    List<OpenlistApiService.OpenlistFile> allFiles = context.getAttribute("discoveredFiles");
    if (allFiles == null) {
      throw new BusinessException("文件发现结果为空");
    }

    // 2. 验证文件列表有效性（空列表也继续执行，可能该路径下确实没有文件）
    log.info("成功获取 OpenList 文件列表，共 {} 个文件/目录", allFiles.size());
    taskRunService.appendLog(taskRunId, "INFO", "成功获取 OpenList 文件列表，共 " + allFiles.size() + " 个文件/目录");

    // 3. 文件列表获取成功后，全量模式下再清空 STRM 目录
    if (!isIncrement) {
      log.info("全量执行模式，文件列表获取成功（共 {} 个），开始清理STRM目录: {}", allFiles.size(), taskConfig.getStrmPath());
      taskRunService.appendLog(taskRunId, "INFO", "全量执行模式，开始清理STRM目录: " + taskConfig.getStrmPath());
      strmFileService.clearStrmDirectory(taskConfig.getStrmPath());
      taskRunService.appendLog(taskRunId, "INFO", "STRM目录清理完成: " + taskConfig.getStrmPath());
    }

    // 保存原始文件列表，用于 OrphanCleanupHandler 进行孤立文件检查
    context.setAttribute("originalFiles", allFiles);
    context.getStats().setTotalFiles(allFiles.size());

    // 5. 过滤出视频文件
    FileFilterHandler filterHandler = fileProcessorChain.getHandler(FileFilterHandler.class);
    if (filterHandler == null) {
      throw new BusinessException("文件过滤处理器不存在");
    }
    FileProcessingResult filterResult = filterHandler.process(context);
    if (filterResult.isFailed()) {
      throw new BusinessException(filterResult.getReason());
    }
    List<OpenlistApiService.OpenlistFile> videoFiles = context.getAttribute("videoFiles");
    if (videoFiles == null) {
      throw new BusinessException("文件过滤结果为空");
    }

    // 6. 获取配置
    Map<String, Object> scrapingConfig = systemConfigService.getScrapingConfig();
    boolean needScrap = Boolean.TRUE.equals(taskConfig.getNeedScrap());

    // 7. 处理每个视频文件
    int processedCount = 0;
    int scrapSkippedCount = 0;

    taskRunService.appendLog(taskRunId, "INFO", "Handler 链开始处理视频文件，共 " + videoFiles.size() + " 个");
    for (OpenlistApiService.OpenlistFile videoFile : videoFiles) {
      // 构建单个文件的上下文
      FileProcessingContext fileContext =
          createFileContext(context, videoFile, openlistConfig, scrapingConfig, taskRunId);

      // 执行处理器链
      fileProcessorChain.execute(fileContext);

      // 更新统计
      if (fileContext.getStats().getProcessedFiles() > 0) {
        processedCount++;
      }
      if (fileContext.getStats().getSkippedFiles() > 0) {
        scrapSkippedCount++;
      }
    }

    log.info("Handler 链处理完成: 处理 {} 个视频文件, 跳过 {} 个", processedCount, scrapSkippedCount);
    taskRunService.appendLog(
        taskRunId,
        "INFO",
        "Handler 链处理完成: 处理 " + processedCount + " 个视频文件, 跳过 " + scrapSkippedCount + " 个");

    // 8. 增量模式下清理孤立文件
    if (isIncrement) {
      log.info("增量执行模式，开始清理孤立的STRM文件");
      taskRunService.appendLog(taskRunId, "INFO", "增量执行模式，开始清理孤立的STRM文件");
      int cleanedCount =
          strmFileService.cleanOrphanedStrmFiles(
              taskConfig.getStrmPath(),
              allFiles,
              taskConfig.getPath(),
              taskConfig.getRenameRegex(),
              openlistConfig);
      log.info("清理了 {} 个孤立的STRM文件", cleanedCount);
      taskRunService.appendLog(taskRunId, "INFO", "清理了 " + cleanedCount + " 个孤立的STRM文件");
    }
  }

  /** 创建单个文件的处理上下文 */
  private FileProcessingContext createFileContext(
      FileProcessingContext parentContext,
      OpenlistApiService.OpenlistFile videoFile,
      OpenlistConfig openlistConfig,
      Map<String, Object> scrapingConfig,
      Long taskRunId) {

    // 计算相对路径
    String relativePath =
        strmFileService.calculateRelativePath(
            parentContext.getTaskConfig().getPath(), videoFile.getPath());

    // 构建保存目录
    String saveDirectory =
        buildScrapSaveDirectory(parentContext.getTaskConfig().getStrmPath(), relativePath);

    // 获取基础文件名
    String baseFileName = removeExtension(videoFile.getName());

    // 获取当前目录的所有文件
    String currentDirectory =
        videoFile.getPath().substring(0, videoFile.getPath().lastIndexOf('/') + 1);

    List<OpenlistApiService.OpenlistFile> directoryFiles =
        parentContext.getAttribute("discoveredFiles");
    List<OpenlistApiService.OpenlistFile> currentDirFiles =
        directoryFiles.stream()
            .filter(f -> f.getPath().startsWith(currentDirectory))
            .filter(
                f -> {
                  String subPath = f.getPath().substring(currentDirectory.length());
                  return subPath.isEmpty() || !subPath.contains("/");
                })
            .toList();

    FileProcessingContext fileContext =
        FileProcessingContext.builder()
            .openlistConfig(openlistConfig)
            .taskConfig(parentContext.getTaskConfig())
            .currentFile(videoFile)
            .relativePath(relativePath)
            .saveDirectory(saveDirectory)
            .baseFileName(baseFileName)
            .directoryFiles(currentDirFiles)
            .attributes(
                scrapingConfig != null
                    ? new java.util.HashMap<>(scrapingConfig)
                    : new java.util.HashMap<>())
            .build();
    fileContext.setAttribute("taskRunId", taskRunId);
    return fileContext;
  }

  private TaskConfig getRunnableTaskConfig(Long taskId) {
    TaskConfig taskConfig = taskConfigService.getById(taskId);
    if (taskConfig == null) {
      throw new BusinessException("任务配置不存在，ID: " + taskId);
    }

    if (!Boolean.TRUE.equals(taskConfig.getIsActive())) {
      throw new BusinessException("任务已禁用，无法执行，ID: " + taskId);
    }

    return taskConfig;
  }

  private boolean resolveIsIncrement(TaskConfig taskConfig, Boolean isIncrement) {
    if (isIncrement != null) {
      log.info("使用传入的增量参数: {}", isIncrement);
      return isIncrement;
    }

    boolean useIncrement = Boolean.TRUE.equals(taskConfig.getIsIncrement());
    log.info("使用任务配置的增量参数: {}", useIncrement);
    return useIncrement;
  }

  /** 移除文件扩展名 */
  private String removeExtension(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return fileName;
    }
    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex > 0) {
      return fileName.substring(0, lastDotIndex);
    }
    return fileName;
  }

  /**
   * 获取OpenList配置
   *
   * @param taskConfig 任务配置
   * @return OpenList配置
   */
  private OpenlistConfig getOpenlistConfig(TaskConfig taskConfig) {
    if (taskConfig.getOpenlistConfigId() == null) {
      throw new BusinessException("任务配置中未指定OpenList配置ID");
    }

    OpenlistConfig openlistConfig = openlistConfigService.getById(taskConfig.getOpenlistConfigId());
    if (openlistConfig == null) {
      throw new BusinessException("OpenList配置不存在，ID: " + taskConfig.getOpenlistConfigId());
    }

    if (!Boolean.TRUE.equals(openlistConfig.getIsActive())) {
      throw new BusinessException("OpenList配置已禁用，ID: " + taskConfig.getOpenlistConfigId());
    }

    return openlistConfig;
  }

  /**
   * 处理URL的baseUrl替换 这个方法会在StrmFileService中调用，用于在生成STRM文件时替换baseUrl
   *
   * @param originalUrl 原始URL
   * @param openlistConfig OpenList配置
   * @return 处理后的URL
   */
  public String processUrlWithBaseUrlReplacement(
      String originalUrl, OpenlistConfig openlistConfig) {
    if (originalUrl == null || openlistConfig == null) {
      return originalUrl;
    }

    // 如果没有配置strmBaseUrl，直接返回原始URL
    if (openlistConfig.getStrmBaseUrl() == null
        || openlistConfig.getStrmBaseUrl().trim().isEmpty()) {
      log.debug("未配置strmBaseUrl，直接使用原始URL: {}", originalUrl);
      return originalUrl;
    }

    try {
      // 解析原始URL
      java.net.URL url = new java.net.URL(originalUrl);
      String originalBaseUrl =
          url.getProtocol()
              + "://"
              + url.getHost()
              + (url.getPort() != -1 && url.getPort() != 80 && url.getPort() != 443
                  ? ":" + url.getPort()
                  : "");

      // 获取路径和查询参数
      String path = url.getPath();
      String query = url.getQuery();
      String ref = url.getRef();

      // 构建新的URL
      String newBaseUrl = openlistConfig.getStrmBaseUrl();
      if (!newBaseUrl.endsWith("/")) {
        newBaseUrl += "/";
      }

      // 确保路径不以/开头（避免双斜杠）
      if (path.startsWith("/")) {
        path = path.substring(1);
      }

      String newUrl = newBaseUrl + path;

      // 添加查询参数
      if (query != null && !query.isEmpty()) {
        newUrl += "?" + query;
      }

      // 添加锚点
      if (ref != null && !ref.isEmpty()) {
        newUrl += "#" + ref;
      }

      log.info("URL替换: {} -> {}", originalUrl, newUrl);
      return newUrl;

    } catch (Exception e) {
      log.warn("URL替换失败，使用原始URL: {}, 错误: {}", originalUrl, e.getMessage());
      return originalUrl;
    }
  }

  /**
   * 构建刮削保存目录路径 复用 MediaScrapingService 中的逻辑
   *
   * @param strmDirectory STRM文件目录
   * @param relativePath 相对路径
   * @return 保存目录路径
   */
  private String buildScrapSaveDirectory(String strmDirectory, String relativePath) {
    if (relativePath == null || relativePath.trim().isEmpty()) {
      return strmDirectory;
    }

    // 移除文件名，只保留目录路径
    String directoryPath = relativePath;
    int lastSlashIndex = relativePath.lastIndexOf('/');
    if (lastSlashIndex > 0) {
      directoryPath = relativePath.substring(0, lastSlashIndex);
    } else if (lastSlashIndex == 0) {
      directoryPath = "";
    }

    if (directoryPath.isEmpty()) {
      return strmDirectory;
    }

    return strmDirectory + "/" + directoryPath;
  }

  /** 内存优化的文件处理方法 分批处理文件，避免一次性加载所有文件到内存 */
  private List<OpenlistApiService.OpenlistFile> processFilesWithMemoryOptimization(
      OpenlistConfig openlistConfig,
      TaskConfig taskConfig,
      boolean isIncrement,
      boolean needScrap) {

    List<OpenlistApiService.OpenlistFile> allFiles = new ArrayList<>();
    int processedCount = 0;
    int scrapSkippedCount = 0;

    try {
      // 分批处理目录，每次只处理一个目录的文件
      processDirectoryBatch(
          openlistConfig,
          taskConfig.getPath(),
          taskConfig,
          isIncrement,
          needScrap,
          allFiles,
          processedCount,
          scrapSkippedCount);

      log.info("文件处理完成 - 处理了 {} 个视频文件", processedCount);
      if (needScrap && scrapSkippedCount > 0) {
        log.info("跳过了 {} 个已刮削目录中的文件", scrapSkippedCount);
      }

    } catch (Exception e) {
      log.error("内存优化文件处理失败: {}", e.getMessage(), e);
      // 降级到原始方法
      log.info("降级使用原始文件处理方法");
      allFiles = openlistApiService.getAllFilesRecursively(openlistConfig, taskConfig.getPath());
    }

    return allFiles;
  }

  /** 分批处理目录 */
  private void processDirectoryBatch(
      OpenlistConfig openlistConfig,
      String path,
      TaskConfig taskConfig,
      boolean isIncrement,
      boolean needScrap,
      List<OpenlistApiService.OpenlistFile> allFiles,
      int processedCount,
      int scrapSkippedCount) {

    try {
      List<OpenlistApiService.OpenlistFile> files =
          openlistApiService.getDirectoryContents(openlistConfig, path);

      for (OpenlistApiService.OpenlistFile file : files) {
        allFiles.add(file);

        if ("file".equals(file.getType()) && strmFileService.isVideoFile(file.getName())) {
          // 立即处理视频文件，不累积在内存中
          processVideoFile(
              openlistConfig,
              file,
              taskConfig,
              isIncrement,
              needScrap,
              files,
              processedCount,
              scrapSkippedCount);
        } else if ("folder".equals(file.getType())) {
          // 递归处理子目录
          String subPath = file.getPath();
          if (subPath == null || subPath.isEmpty()) {
            subPath = path + "/" + file.getName();
          }
          processDirectoryBatch(
              openlistConfig,
              subPath,
              taskConfig,
              isIncrement,
              needScrap,
              allFiles,
              processedCount,
              scrapSkippedCount);
        }
      }

      // 处理完一个目录后，清理局部变量引用（由JVM自动管理GC）
      // 移除显式 System.gc() 调用以提升性能

    } catch (Exception e) {
      log.error("处理目录失败: {}, 错误: {}", path, e.getMessage(), e);
    }
  }

  /** 处理单个视频文件 */
  private void processVideoFile(
      OpenlistConfig openlistConfig,
      OpenlistApiService.OpenlistFile file,
      TaskConfig taskConfig,
      boolean isIncrement,
      boolean needScrap,
      List<OpenlistApiService.OpenlistFile> directoryFiles,
      int processedCount,
      int scrapSkippedCount) {

    try {
      // 计算相对路径
      String relativePath =
          strmFileService.calculateRelativePath(taskConfig.getPath(), file.getPath());

      // 生成STRM文件（增量模式下强制重新生成）
      strmFileService.generateStrmFile(
          taskConfig.getStrmPath(),
          relativePath,
          file.getName(),
          file.getUrl(),
          isIncrement, // 增量模式下强制重新生成
          taskConfig.getRenameRegex(),
          openlistConfig,
          file.getSign(),
          taskConfig.getGenerateSign(),
          taskConfig.getStrmUrlReplaceFrom(),
          taskConfig.getStrmUrlReplaceTo());

      // 如果启用了刮削功能，执行媒体刮削
      if (needScrap) {
        try {
          String saveDirectory = buildScrapSaveDirectory(taskConfig.getStrmPath(), relativePath);

          // 检查是否需要刮削（在增量模式下检查NFO文件是否已存在）
          boolean needScrapFile =
              needScrapFile(
                  file.getName(),
                  taskConfig.getRenameRegex(),
                  taskConfig.getStrmPath(),
                  relativePath,
                  isIncrement);

          if (needScrapFile) {
            if (isIncrement && mediaScrapingService.isDirectoryFullyScraped(saveDirectory)) {
              log.debug("目录已完全刮削，跳过: {}", saveDirectory);
              scrapSkippedCount++;
            } else {
              mediaScrapingService.scrapMedia(
                  openlistConfig,
                  file.getName(),
                  taskConfig.getStrmPath(),
                  relativePath,
                  directoryFiles,
                  file.getPath());
            }
          } else {
            log.debug("NFO文件已存在，跳过刮削: {}", file.getName());
            scrapSkippedCount++;
          }
        } catch (Exception scrapException) {
          log.error(
              "刮削文件失败: {}, 错误: {}", file.getName(), scrapException.getMessage(), scrapException);
        }
      }

      processedCount++;

    } catch (Exception e) {
      log.error("处理文件失败: {}, 错误: {}", file.getName(), e.getMessage(), e);
    }
  }

  /**
   * 判断是否需要刮削文件 在增量模式下，检查NFO文件是否存在，如果NFO文件已存在则跳过刮削
   *
   * @param fileName 原始文件名
   * @param renameRegex 重命名正则表达式
   * @param strmPath STRM文件路径
   * @param relativePath 相对路径
   * @param isIncrement 是否增量模式
   * @return 是否需要刮削
   */
  private boolean needScrapFile(
      String fileName,
      String renameRegex,
      String strmPath,
      String relativePath,
      boolean isIncrement) {
    // 非增量模式下总是需要刮削
    if (!isIncrement) {
      return true;
    }

    try {
      // 增量模式下，检查NFO文件是否已存在
      String finalFileName = processFileNameForScraping(fileName, renameRegex);
      java.nio.file.Path strmFilePath =
          strmFileService.buildStrmFilePath(strmPath, relativePath, finalFileName);

      // 构建对应的NFO文件路径
      java.nio.file.Path nfoFilePath =
          strmFilePath.resolveSibling(
              strmFilePath.getFileName().toString().replace(".strm", ".nfo"));

      // 如果NFO文件存在，则跳过刮削
      return !java.nio.file.Files.exists(nfoFilePath);
    } catch (Exception e) {
      log.warn("检查NFO文件是否存在时发生错误: {}, 默认进行刮削", e.getMessage());
      return true;
    }
  }

  /**
   * 处理文件名（重命名和添加.strm扩展名） 这个方法复制自StrmFileService，用于判断STRM文件是否存在
   *
   * @param originalFileName 原始文件名
   * @param renameRegex 重命名正则表达式
   * @return 处理后的文件名
   */
  private String processFileNameForScraping(String originalFileName, String renameRegex) {
    String processedName = originalFileName;

    // 应用重命名规则
    if (renameRegex != null && !renameRegex.trim().isEmpty()) {
      try {
        // 简单的正则替换，可以根据需要扩展
        // 格式: "原始模式|替换内容"
        if (renameRegex.contains("|")) {
          String[] parts = renameRegex.split("\\|", 2);
          String pattern = parts[0];
          String replacement = parts[1];
          processedName = processedName.replaceAll(pattern, replacement);
          log.debug("文件重命名: {} -> {}", originalFileName, processedName);
        }
      } catch (Exception e) {
        log.warn("重命名规则应用失败: {}, 使用原始文件名", renameRegex, e);
      }
    }

    // 移除原始扩展名并添加.strm扩展名
    int lastDotIndex = processedName.lastIndexOf('.');
    if (lastDotIndex > 0) {
      processedName = processedName.substring(0, lastDotIndex);
    }
    processedName += ".strm";

    return processedName;
  }
}
