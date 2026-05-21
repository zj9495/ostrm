package com.hienao.openlist2strm.handler;

import com.hienao.openlist2strm.handler.context.FileProcessingContext;
import com.hienao.openlist2strm.service.LocalFileService;
import com.hienao.openlist2strm.service.MediaScrapingService;
import com.hienao.openlist2strm.service.OpenlistApiService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * NFO 文件下载处理器
 *
 * <p>负责 NFO 文件的三级优先级处理：
 *
 * <ol>
 *   <li>优先级 1 - 本地文件：检查本地是否存在对应 NFO 文件
 *   <li>优先级 2 - OpenList 文件：本地不存在时从 OpenList 同级目录下载
 *   <li>优先级 3 - 刮削文件：前两级都不存在时执行 TMDB 刮削
 * </ol>
 *
 * <p>Order: 40
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class NfoDownloadHandler implements FileProcessorHandler {

  private final FilePriorityResolver priorityResolver;
  private final OpenlistApiService openlistApiService;
  private final LocalFileService localFileService;
  private final MediaScrapingService mediaScrapingService;

  // ==================== 接口实现 ====================

  @Override
  public FileProcessingResult process(FileProcessingContext context) {
    try {
      // 1. 检查配置是否启用
      if (!isNfoScrapingEnabled(context)) {
        log.debug("NFO 刮削未启用，跳过");
        context.getStats().incrementSkipped();
        return FileProcessingResult.skipped("NFO 刮削未启用");
      }

      // 2. 优先级判断
      PriorityResult priority = priorityResolver.resolve(FileType.NFO, context);

      switch (priority.getPriority()) {
        case LOCAL:
          log.debug("本地 NFO 文件已存在，跳过: {}", context.getBaseFileName());
          context.getStats().incrementSkipped();
          return FileProcessingResult.skipped("本地 NFO 文件已存在: " + priority.getFileName());

        case OPENLIST:
          return downloadFromOpenList(context);

        case SCRAPING:
          return fallbackToScraping(context);

        case SKIPPED:
        default:
          context.getStats().incrementSkipped();
          return FileProcessingResult.skipped(priority.getMessage());
      }

    } catch (Exception e) {
      log.error("NFO 文件处理失败: {}", context.getBaseFileName(), e);
      context.getStats().incrementFailed();
      return FileProcessingResult.failed("NFO 文件处理失败: " + e.getMessage());
    }
  }

  @Override
  public Set<FileType> getHandledTypes() {
    return Set.of(FileType.NFO);
  }

  // ==================== 下载逻辑 ====================

  /** 从 OpenList 下载 NFO 文件 */
  private FileProcessingResult downloadFromOpenList(FileProcessingContext context) {
    try {
      String nfoFileName = context.getBaseFileName() + ".nfo";

      // 在目录文件列表中查找同名 NFO 文件
      OpenlistFileWrapper nfoFile =
          findNfoFileInDirectory(context.getDirectoryFiles(), nfoFileName);

      if (nfoFile != null) {
        // 下载 NFO 文件内容
        boolean isLocal = "LOCAL".equals(context.getOpenlistConfig().getSourceType());
        byte[] content =
            isLocal
                ? localFileService.getFileContent(nfoFile.getOpenlistFile().getPath())
                : openlistApiService.getFileContent(
                    context.getOpenlistConfig(), nfoFile.getOpenlistFile(), false);

        if (content != null && content.length > 0) {
          // 保存到本地
          Path targetPath = Paths.get(context.getSaveDirectory(), nfoFileName);
          Files.createDirectories(targetPath.getParent());
          Files.write(targetPath, content);

          log.info("从 OpenList 下载 NFO 文件成功: {}", nfoFileName);
          context.getStats().incrementProcessed();
          return FileProcessingResult.success();
        }
      }

      // OpenList 不存在，执行刮削
      log.debug("OpenList 中不存在 NFO 文件，执行刮削");
      return fallbackToScraping(context);

    } catch (Exception e) {
      log.error("从 OpenList 下载 NFO 文件失败: {}", context.getBaseFileName(), e);
      return FileProcessingResult.failed("从 OpenList 下载 NFO 文件失败: " + e.getMessage());
    }
  }

  /** Fallback 到刮削 */
  private FileProcessingResult fallbackToScraping(FileProcessingContext context) {
    try {
      mediaScrapingService.scrapMedia(
          context.getOpenlistConfig(),
          context.getCurrentFile().getName(),
          context.getTaskConfig().getStrmPath(),
          context.getRelativePath(),
          context.getDirectoryFiles(),
          context.getCurrentFile().getPath());

      log.debug("NFO 文件刮削完成: {}", context.getBaseFileName());
      context.getStats().incrementProcessed();
      return FileProcessingResult.success();

    } catch (Exception e) {
      log.error("NFO 文件刮削失败: {}", context.getBaseFileName(), e);
      context.getStats().incrementFailed();
      return FileProcessingResult.failed("NFO 文件刮削失败: " + e.getMessage());
    }
  }

  // ==================== 工具方法 ====================

  /** 检查 NFO 刮削是否启用 */
  private boolean isNfoScrapingEnabled(FileProcessingContext context) {
    // 优先使用现有刮削信息配置
    var scrapingConfig = context.getAttribute("scrapingConfig");
    if (scrapingConfig != null && scrapingConfig instanceof java.util.Map) {
      @SuppressWarnings("unchecked")
      java.util.Map<String, Object> config = (java.util.Map<String, Object>) scrapingConfig;
      return Boolean.TRUE.equals(config.get("useExistingScrapingInfo"));
    }
    return false;
  }

  /** 在目录文件中查找 NFO 文件 */
  private OpenlistFileWrapper findNfoFileInDirectory(
      java.util.List<OpenlistApiService.OpenlistFile> files, String fileName) {
    if (files == null) {
      return null;
    }

    return files.stream()
        .filter(f -> "file".equals(f.getType()))
        .filter(f -> f.getName().equalsIgnoreCase(fileName))
        .map(OpenlistFileWrapper::new)
        .findFirst()
        .orElse(null);
  }

  /** 内部类：包装 OpenlistFile */
  private static class OpenlistFileWrapper {
    private final OpenlistApiService.OpenlistFile openlistFile;

    public OpenlistFileWrapper(OpenlistApiService.OpenlistFile openlistFile) {
      this.openlistFile = openlistFile;
    }

    public OpenlistApiService.OpenlistFile getOpenlistFile() {
      return openlistFile;
    }
  }
}
