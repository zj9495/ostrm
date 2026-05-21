package com.hienao.openlist2strm.handler.jav;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * JAV 图片下载服务
 *
 * <p>负责下载封面、海报和剧照。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Service
public class JavImageService {

  private final HttpClient httpClient;

  public JavImageService() {
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  /**
   * 下载 fanart 图片
   *
   * @param movieInfo 影片信息
   * @param outputDir 输出目录
   * @param baseFileName 基础文件名
   * @param useHighRes 是否使用高清封面
   * @return 下载的 fanart 文件路径，如果失败返回 null
   */
  public String downloadFanart(JavMovieInfo movieInfo, String outputDir, String baseFileName,
      boolean useHighRes) {
    List<String> covers = useHighRes ? movieInfo.getBigCovers() : movieInfo.getCovers();
    if (covers == null || covers.isEmpty()) {
      // 如果高清封面不可用，尝试普通封面
      covers = movieInfo.getCovers();
    }

    if (covers == null || covers.isEmpty()) {
      log.warn("没有可用的封面 URL: {}", movieInfo.getNumber());
      return null;
    }

    // 尝试下载第一个可用的封面
    for (String coverUrl : covers) {
      try {
        String fanartPath = Paths.get(outputDir, baseFileName + "-fanart.jpg").toString();
        downloadFile(coverUrl, fanartPath);
        log.info("已下载 fanart: {}", fanartPath);
        return fanartPath;
      } catch (Exception e) {
        log.warn("下载封面失败: {}, 错误: {}", coverUrl, e.getMessage());
      }
    }

    log.error("所有封面下载失败: {}", movieInfo.getNumber());
    return null;
  }

  /**
   * 生成 poster 图片
   *
   * <p>从 fanart 生成 poster（简单复制，实际项目中可能需要裁剪）
   *
   * @param fanartPath fanart 文件路径
   * @param outputDir 输出目录
   * @param baseFileName 基础文件名
   * @return poster 文件路径，如果失败返回 null
   */
  public String generatePoster(String fanartPath, String outputDir, String baseFileName) {
    if (fanartPath == null) {
      return null;
    }

    try {
      String posterPath = Paths.get(outputDir, baseFileName + "-poster.jpg").toString();
      Path source = Paths.get(fanartPath);
      Path target = Paths.get(posterPath);

      // 简单复制 fanart 作为 poster
      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
      log.info("已生成 poster: {}", posterPath);
      return posterPath;
    } catch (Exception e) {
      log.error("生成 poster 失败: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 下载剧照
   *
   * @param movieInfo 影片信息
   * @param outputDir 输出目录
   * @param baseFileName 基础文件名
   * @param maxCount 最大下载数量
   * @return 下载的剧照数量
   */
  public int downloadExtraFanarts(JavMovieInfo movieInfo, String outputDir, String baseFileName,
      int maxCount) {
    List<String> extraFanartUrls = movieInfo.getExtraFanartUrls();
    if (extraFanartUrls == null || extraFanartUrls.isEmpty()) {
      return 0;
    }

    // 创建 extrafanart 目录
    String extrafanartDir = Paths.get(outputDir, "extrafanart").toString();
    try {
      Files.createDirectories(Paths.get(extrafanartDir));
    } catch (IOException e) {
      log.error("创建 extrafanart 目录失败: {}", e.getMessage());
      return 0;
    }

    int downloaded = 0;
    for (int i = 0; i < Math.min(extraFanartUrls.size(), maxCount); i++) {
      String url = extraFanartUrls.get(i);
      try {
        String filename = String.format("%s_extrafanart_%02d.jpg", baseFileName, i + 1);
        String filepath = Paths.get(extrafanartDir, filename).toString();
        downloadFile(url, filepath);
        downloaded++;
        log.debug("已下载剧照: {}", filepath);
      } catch (Exception e) {
        log.warn("下载剧照失败: {}, 错误: {}", url, e.getMessage());
      }
    }

    log.info("已下载 {}/{} 张剧照: {}", downloaded, extraFanartUrls.size(), movieInfo.getNumber());
    return downloaded;
  }

  /**
   * 下载文件
   */
  private void downloadFile(String url, String outputPath) throws IOException, InterruptedException {
    Path path = Paths.get(outputPath);
    Files.createDirectories(path.getParent());

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(java.time.Duration.ofSeconds(60))
        .GET()
        .header("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .header("Referer", url)
        .build();

    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

    if (response.statusCode() == 200) {
      try (InputStream inputStream = response.body()) {
        Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
      }
    } else {
      throw new IOException("下载失败，状态码: " + response.statusCode());
    }
  }
}
