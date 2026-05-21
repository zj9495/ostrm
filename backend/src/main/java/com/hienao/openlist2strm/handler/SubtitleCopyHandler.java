package com.hienao.openlist2strm.handler;

import com.hienao.openlist2strm.handler.context.FileProcessingContext;
import com.hienao.openlist2strm.service.LocalFileService;
import com.hienao.openlist2strm.service.OpenlistApiService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 字幕文件复制处理器
 *
 * <p>负责字幕文件的三级优先级处理：
 *
 * <ol>
 *   <li>优先级 1 - 本地文件：检查本地是否存在对应字幕文件
 *   <li>优先级 2 - OpenList 文件：本地不存在时从 OpenList 同级目录下载
 *   <li>优先级 3 - 无刮削选项：字幕文件不支持 API 刮削
 * </ol>
 *
 * <p>Order: 42
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
@Order(42)
@RequiredArgsConstructor
public class SubtitleCopyHandler implements FileProcessorHandler {

  private final FilePriorityResolver priorityResolver;
  private final OpenlistApiService openlistApiService;
  private final LocalFileService localFileService;

  /** 已下载的字幕文件集合（用于防止重复下载） */
  private final Set<String> downloadedSubtitles = new HashSet<>();

  // ==================== 支持的字幕文件扩展名 ====================

  private static final Set<String> SUBTITLE_EXTENSIONS =
      Set.of(".srt", ".ass", ".vtt", ".ssa", ".sub", ".idx");

  // ==================== 接口实现 ====================

  @Override
  public FileProcessingResult process(FileProcessingContext context) {
    try {
      // 1. 检查配置是否启用
      boolean keepSubtitleEnabled = isKeepSubtitleEnabled(context);

      if (!keepSubtitleEnabled) {
        return FileProcessingResult.success();
      }

      // 2. 获取当前目录的所有字幕文件
      String currentDirectory =
          context
              .getCurrentFile()
              .getPath()
              .substring(0, context.getCurrentFile().getPath().lastIndexOf('/') + 1);

      java.util.List<OpenlistApiService.OpenlistFile> allDirectoryFiles =
          context.getDirectoryFiles();

      java.util.List<OpenlistApiService.OpenlistFile> subtitleFiles =
          allDirectoryFiles.stream()
              .filter(f -> "file".equals(f.getType()))
              .filter(f -> isSubtitleFile(f.getName()))
              .filter(f -> f.getPath().startsWith(currentDirectory))
              .filter(
                  f -> {
                    String fileName = f.getName();
                    return !downloadedSubtitles.contains(fileName.toLowerCase());
                  })
              .collect(Collectors.toList());

      log.debug("找到 {} 个字幕文件", subtitleFiles.size());

      if (subtitleFiles.isEmpty()) {
        log.debug("没有需要处理的字幕文件");
        context.getStats().incrementSkipped();
        return FileProcessingResult.skipped("当前目录没有需要处理的字幕文件");
      }

      // 3. 处理每个字幕文件
      int successCount = 0;
      String lastFailureReason = null;
      for (OpenlistApiService.OpenlistFile subtitleFile : subtitleFiles) {
        String failureReason = copySubtitleFile(context, subtitleFile);
        if (failureReason == null) {
          downloadedSubtitles.add(subtitleFile.getName().toLowerCase());
          successCount++;
        } else {
          lastFailureReason = failureReason;
        }
      }

      if (successCount > 0) {
        log.info("成功复制 {} 个字幕文件", successCount);
        context.getStats().incrementProcessed();
        return FileProcessingResult.success();
      }

      context.getStats().incrementSkipped();
      return FileProcessingResult.skipped(lastFailureReason);

    } catch (Exception e) {
      log.error("字幕文件处理失败: {}", context.getBaseFileName(), e);
      context.getStats().incrementFailed();
      return FileProcessingResult.failed("字幕文件处理失败: " + e.getMessage());
    }
  }

  @Override
  public Set<FileType> getHandledTypes() {
    return Set.of(FileType.SUBTITLE, FileType.VIDEO);
  }

  // ==================== 字幕复制逻辑 ====================

  /** 复制单个字幕文件 */
  private String copySubtitleFile(
      FileProcessingContext context, OpenlistApiService.OpenlistFile subtitleFile) {

    String saveDirectory = context.getSaveDirectory();
    String fileName = subtitleFile.getName();

    try {
      // 1. 检查本地是否已存在
      Path localPath = Paths.get(saveDirectory, fileName);
      if (Files.exists(localPath)) {
        log.debug("本地字幕文件已存在，跳过: {}", fileName);
        downloadedSubtitles.add(fileName.toLowerCase());
        return null;
      }

      // 2. 从数据源下载
      boolean isLocal = "LOCAL".equals(context.getOpenlistConfig().getSourceType());
      byte[] content;
      if (isLocal) {
        content = localFileService.getFileContent(subtitleFile.getPath());
      } else {
        String downloadUrl = subtitleFile.getUrl();
        if (subtitleFile.getSign() != null && !subtitleFile.getSign().isEmpty()) {
          downloadUrl = downloadUrl + "?sign=" + subtitleFile.getSign();
        }
        // 使用统一的智能编码方法处理中文路径
        downloadUrl = com.hienao.openlist2strm.util.UrlEncoder.encodeUrlSmart(downloadUrl);

        // 从 OpenList 下载
        content =
            openlistApiService.downloadWithEncodedUrl(
                context.getOpenlistConfig(), subtitleFile, downloadUrl);
      }

      if (content != null && content.length > 0) {
        // 4. 保存到本地
        Files.createDirectories(localPath.getParent());
        Files.write(localPath, content);

        log.info("已复制字幕文件: {} -> {} (大小: {} bytes)", fileName, localPath, content.length);
        return null;
      }

      log.debug("字幕文件内容为空: {}", fileName);
      return "字幕文件内容为空: " + fileName;

    } catch (Exception e) {
      log.warn("复制字幕文件失败: {}, 错误: {}", fileName, e.getMessage());
      return "复制字幕文件失败: " + fileName + "，错误: " + e.getMessage();
    }
  }

  // ==================== 配置检查 ====================

  /** 检查是否启用保留字幕文件 */
  private boolean isKeepSubtitleEnabled(FileProcessingContext context) {
    Object keepSubtitleValue = context.getAttribute("keepSubtitleFiles");
    return Boolean.TRUE.equals(keepSubtitleValue);
  }

  // ==================== 工具方法 ====================

  /** 检查是否为字幕文件 */
  public boolean isSubtitleFile(String fileName) {
    if (fileName == null) {
      return false;
    }
    String lower = fileName.toLowerCase();
    for (String ext : SUBTITLE_EXTENSIONS) {
      if (lower.endsWith(ext)) {
        return true;
      }
    }
    return false;
  }

  /** 获取字幕文件扩展名列表 */
  public Set<String> getSubtitleExtensions() {
    return new HashSet<>(SUBTITLE_EXTENSIONS);
  }

  /** 清空已下载字幕文件记录（用于新任务开始时） */
  public void clearDownloadedSubtitles() {
    downloadedSubtitles.clear();
  }

  /** 获取已下载字幕文件数量 */
  public int getDownloadedSubtitleCount() {
    return downloadedSubtitles.size();
  }
}
