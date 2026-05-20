package com.hienao.openlist2strm.handler;

import com.hienao.openlist2strm.entity.TaskConfig;
import com.hienao.openlist2strm.handler.context.FileProcessingContext;
import com.hienao.openlist2strm.service.OpenlistApiService;
import com.hienao.openlist2strm.service.TaskRunService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 文件过滤处理器
 *
 * <p>负责过滤出视频文件，只将视频文件传递给后续处理器。
 *
 * <p>Order: 20
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class FileFilterHandler implements FileProcessorHandler {

  private final TaskRunService taskRunService;

  // ==================== 支持的视频文件扩展名 ====================

  private static final Set<String> VIDEO_EXTENSIONS =
      Set.of(
          ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".m2ts", ".ts", ".rmvb",
          ".rm", ".3gp", ".mpeg", ".mpg");

  private static final Set<String> SUBTITLE_EXTENSIONS =
      Set.of(".srt", ".ass", ".vtt", ".ssa", ".sub", ".idx");

  private static final Set<String> IMAGE_EXTENSIONS =
      Set.of(".jpg", ".jpeg", ".png", ".webp", ".bmp", ".gif", ".tiff", ".tif");

  // ==================== 接口实现 ====================

  @Override
  public FileProcessingResult process(FileProcessingContext context) {
    try {
      @SuppressWarnings("unchecked")
      List<OpenlistApiService.OpenlistFile> allFiles =
          (List<OpenlistApiService.OpenlistFile>) context.getAttribute("discoveredFiles");

      if (allFiles == null || allFiles.isEmpty()) {
        log.debug("没有发现文件可过滤");
        return FileProcessingResult.success();
      }

      FilterStats filterStats = new FilterStats();
      List<OpenlistApiService.OpenlistFile> videoFiles =
          filterVideoFiles(allFiles, context, filterStats);

      // 过滤出字幕文件
      List<OpenlistApiService.OpenlistFile> subtitleFiles =
          filterByExtensions(allFiles, SUBTITLE_EXTENSIONS);

      // 过滤出图片文件
      List<OpenlistApiService.OpenlistFile> imageFiles =
          filterByExtensions(allFiles, IMAGE_EXTENSIONS);

      // 设置过滤结果到上下文
      context.setAttribute("videoFiles", videoFiles);
      context.setAttribute("subtitleFiles", subtitleFiles);
      context.setAttribute("imageFiles", imageFiles);

      log.debug(
          "文件过滤完成: {} 视频, {} 字幕, {} 图片",
          videoFiles.size(),
          subtitleFiles.size(),
          imageFiles.size());
      appendSummaryLog(
          context, filterStats, videoFiles.size(), subtitleFiles.size(), imageFiles.size());

      return FileProcessingResult.success();

    } catch (Exception e) {
      log.error("文件过滤失败: {}", e.getMessage(), e);
      return FileProcessingResult.failed("文件过滤失败: " + e.getMessage());
    }
  }

  @Override
  public Set<FileType> getHandledTypes() {
    return Set.of();
  }

  // ==================== 过滤方法 ====================

  /** 过滤出视频文件 */
  public List<OpenlistApiService.OpenlistFile> filterVideoFiles(
      List<OpenlistApiService.OpenlistFile> files) {
    return files.stream()
        .filter(f -> "file".equals(f.getType()))
        .filter(f -> isVideoFile(f.getName()))
        .collect(Collectors.toList());
  }

  /** 过滤出满足任务配置规则的视频文件 */
  private List<OpenlistApiService.OpenlistFile> filterVideoFiles(
      List<OpenlistApiService.OpenlistFile> files,
      FileProcessingContext context,
      FilterStats filterStats) {
    TaskConfig taskConfig = context.getTaskConfig();
    Pattern fileNameExcludePattern = compilePattern(taskConfig.getFileNameExcludeRegex());
    Pattern directoryNameExcludePattern = compilePattern(taskConfig.getDirectoryNameExcludeRegex());
    List<OpenlistApiService.OpenlistFile> videoFiles = new ArrayList<>();

    for (OpenlistApiService.OpenlistFile file : files) {
      if (!"file".equals(file.getType()) || !isVideoFile(file.getName())) {
        continue;
      }

      String skipReason =
          getTaskFilterSkipReason(
              file, taskConfig, fileNameExcludePattern, directoryNameExcludePattern);
      if (skipReason == null) {
        videoFiles.add(file);
      } else {
        filterStats.increment(skipReason);
        appendFileSkipLog(context, file, skipReason);
      }
    }

    return videoFiles;
  }

  /** 按扩展名过滤文件 */
  private List<OpenlistApiService.OpenlistFile> filterByExtensions(
      List<OpenlistApiService.OpenlistFile> files, Set<String> extensions) {
    return files.stream()
        .filter(f -> "file".equals(f.getType()))
        .filter(f -> hasExtension(f.getName(), extensions))
        .collect(Collectors.toList());
  }

  // ==================== 工具方法 ====================

  /** 检查是否为视频文件 */
  public boolean isVideoFile(String fileName) {
    if (fileName == null) {
      return false;
    }
    String lower = fileName.toLowerCase();
    // 跳过隐藏文件
    if (lower.startsWith(".")) {
      return false;
    }
    return hasExtension(fileName, VIDEO_EXTENSIONS);
  }

  /** 检查是否为字幕文件 */
  public boolean isSubtitleFile(String fileName) {
    if (fileName == null) {
      return false;
    }
    return hasExtension(fileName.toLowerCase(), SUBTITLE_EXTENSIONS);
  }

  /** 检查是否为图片文件 */
  public boolean isImageFile(String fileName) {
    if (fileName == null) {
      return false;
    }
    return hasExtension(fileName.toLowerCase(), IMAGE_EXTENSIONS);
  }

  /** 检查文件是否具有指定扩展名之一 */
  private boolean hasExtension(String fileName, Set<String> extensions) {
    if (fileName == null) {
      return false;
    }
    String lower = fileName.toLowerCase();
    for (String ext : extensions) {
      if (lower.endsWith(ext)) {
        return true;
      }
    }
    return false;
  }

  /** 获取所有视频扩展名 */
  public Set<String> getVideoExtensions() {
    return new HashSet<>(VIDEO_EXTENSIONS);
  }

  /** 获取所有字幕扩展名 */
  public Set<String> getSubtitleExtensions() {
    return new HashSet<>(SUBTITLE_EXTENSIONS);
  }

  private Pattern compilePattern(String regex) {
    if (regex == null || regex.trim().isEmpty()) {
      return null;
    }
    return Pattern.compile(regex);
  }

  private String getTaskFilterSkipReason(
      OpenlistApiService.OpenlistFile file,
      TaskConfig taskConfig,
      Pattern fileNameExcludePattern,
      Pattern directoryNameExcludePattern) {
    Long minFileSizeBytes = taskConfig.getMinFileSizeBytes();
    if (minFileSizeBytes != null) {
      Long size = file.getSize();
      if (size == null) {
        return "文件大小缺失，无法应用 minFileSizeBytes: size=null, minFileSizeBytes="
            + minFileSizeBytes;
      }
      if (size < minFileSizeBytes) {
        return "文件大小小于 minFileSizeBytes: size="
            + size
            + ", minFileSizeBytes="
            + minFileSizeBytes;
      }
    }

    if (fileNameExcludePattern != null && fileNameExcludePattern.matcher(file.getName()).find()) {
      return "文件名匹配 fileNameExcludeRegex: name="
          + file.getName()
          + ", fileNameExcludeRegex="
          + taskConfig.getFileNameExcludeRegex();
    }

    String matchedDirectory = findMatchedDirectory(file, taskConfig, directoryNameExcludePattern);
    if (matchedDirectory != null) {
      return "目录名称匹配 directoryNameExcludeRegex: directory="
          + matchedDirectory
          + ", directoryNameExcludeRegex="
          + taskConfig.getDirectoryNameExcludeRegex();
    }

    return null;
  }

  private String findMatchedDirectory(
      OpenlistApiService.OpenlistFile file,
      TaskConfig taskConfig,
      Pattern directoryNameExcludePattern) {
    if (directoryNameExcludePattern == null || file.getPath() == null) {
      return null;
    }

    String relativePath = file.getPath();
    String taskPath = taskConfig.getPath();
    if (taskPath != null && relativePath.startsWith(taskPath)) {
      relativePath = relativePath.substring(taskPath.length());
    }
    if (relativePath.startsWith("/")) {
      relativePath = relativePath.substring(1);
    }

    int lastSlashIndex = relativePath.lastIndexOf('/');
    if (lastSlashIndex < 0) {
      return null;
    }

    String directoryPath = relativePath.substring(0, lastSlashIndex);
    if (directoryPath.isEmpty()) {
      return null;
    }

    String[] directories = directoryPath.split("/");
    for (String directory : directories) {
      if (directoryNameExcludePattern.matcher(directory).find()) {
        return directory;
      }
    }
    return null;
  }

  private void appendFileSkipLog(
      FileProcessingContext context, OpenlistApiService.OpenlistFile file, String skipReason) {
    Long taskRunId = context.getAttribute("taskRunId");
    if (taskRunId == null) {
      return;
    }

    taskRunService.appendLog(
        taskRunId,
        "WARN",
        "文件跳过: " + file.getPath() + "，处理器: FileFilterHandler，原因: " + skipReason);
  }

  private void appendSummaryLog(
      FileProcessingContext context,
      FilterStats filterStats,
      int videoCount,
      int subtitleCount,
      int imageCount) {
    Long taskRunId = context.getAttribute("taskRunId");
    if (taskRunId == null) {
      return;
    }

    taskRunService.appendLog(
        taskRunId,
        "INFO",
        "文件过滤完成: "
            + videoCount
            + " 视频, "
            + subtitleCount
            + " 字幕, "
            + imageCount
            + " 图片, "
            + "文件大小过滤 "
            + filterStats.minFileSizeSkipped
            + " 个, 文件名过滤 "
            + filterStats.fileNameSkipped
            + " 个, 目录名过滤 "
            + filterStats.directoryNameSkipped
            + " 个");
  }

  private static class FilterStats {
    private int minFileSizeSkipped;
    private int fileNameSkipped;
    private int directoryNameSkipped;

    private void increment(String skipReason) {
      if (skipReason.startsWith("文件大小")) {
        minFileSizeSkipped++;
      } else if (skipReason.startsWith("文件名")) {
        fileNameSkipped++;
      } else if (skipReason.startsWith("目录名称")) {
        directoryNameSkipped++;
      }
    }
  }
}
