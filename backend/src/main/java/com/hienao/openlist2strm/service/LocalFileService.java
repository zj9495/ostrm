package com.hienao.openlist2strm.service;

import com.hienao.openlist2strm.exception.BusinessException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 本地文件服务
 *
 * <p>提供本地文件系统目录遍历和文件内容读取能力，用于 LOCAL 数据源模式。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileService {

  /**
   * 目录树节点
   *
   * @param name 目录名称
   * @param path 目录绝对路径
   * @param hasChildren 是否包含子目录
   */
  public record DirectoryNode(String name, String path, boolean hasChildren) {}

  /**
   * 查询本地目录树的下一层目录节点
   *
   * @param parentPath 父目录路径，为空时返回文件系统根节点
   * @return 目录节点列表
   */
  public List<DirectoryNode> listDirectories(String parentPath) {
    Path dir;
    if (parentPath == null || parentPath.isBlank()) {
      // 返回文件系统根节点
      return listRootDirectories();
    }

    dir = Paths.get(parentPath);
    if (!Files.exists(dir)) {
      throw new BusinessException("目录不存在: " + parentPath);
    }
    if (!Files.isDirectory(dir)) {
      throw new BusinessException("路径不是目录: " + parentPath);
    }

    return listSubDirectories(dir);
  }

  /**
   * 验证本地路径是否存在且为目录
   *
   * @param path 本地路径
   * @throws BusinessException 路径不存在或不是目录时抛出
   */
  public void validateLocalPath(String path) {
    if (path == null || path.isBlank()) {
      throw new BusinessException("本地路径不能为空");
    }
    Path dir = Paths.get(path);
    if (!Files.exists(dir)) {
      throw new BusinessException("本地路径不存在: " + path);
    }
    if (!Files.isDirectory(dir)) {
      throw new BusinessException("本地路径不是目录: " + path);
    }
  }

  /**
   * 列出本地目录内容，返回 OpenlistApiService.OpenlistFile 兼容结构
   *
   * @param dirPath 目录路径
   * @return 文件列表
   */
  public List<OpenlistApiService.OpenlistFile> listDirectoryContents(String dirPath) {
    Path dir = Paths.get(dirPath);
    if (!Files.exists(dir) || !Files.isDirectory(dir)) {
      throw new BusinessException("目录不存在或不是目录: " + dirPath);
    }

    List<OpenlistApiService.OpenlistFile> files = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path entry : stream) {
        OpenlistApiService.OpenlistFile file = new OpenlistApiService.OpenlistFile();
        file.setName(entry.getFileName().toString());
        file.setPath(entry.toAbsolutePath().normalize().toString());
        file.setType(Files.isDirectory(entry) ? "folder" : "file");
        if (Files.isRegularFile(entry)) {
          try {
            file.setSize(Files.size(entry));
          } catch (IOException e) {
            log.warn("获取文件大小失败: {}", entry, e);
          }
        }
        // 本地模式：url 使用本地路径，sign 为空
        file.setUrl(entry.toAbsolutePath().normalize().toString());
        file.setSign("");
        files.add(file);
      }
    } catch (IOException e) {
      throw new BusinessException("读取目录失败: " + dirPath + ", 错误: " + e.getMessage(), e);
    }
    return files;
  }

  /**
   * 读取本地文件内容
   *
   * @param filePath 文件路径
   * @return 文件内容字节数组
   */
  public byte[] getFileContent(String filePath) {
    Path path = Paths.get(filePath);
    if (!Files.exists(path) || !Files.isRegularFile(path)) {
      throw new BusinessException("文件不存在或不是普通文件: " + filePath);
    }
    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      throw new BusinessException("读取文件失败: " + filePath + ", 错误: " + e.getMessage(), e);
    }
  }

  private List<DirectoryNode> listRootDirectories() {
    List<DirectoryNode> nodes = new ArrayList<>();
    for (Path root : Path.of("/").getFileSystem().getRootDirectories()) {
      try {
        if (Files.isDirectory(root)) {
          boolean hasChildren = hasSubDirectories(root);
          nodes.add(new DirectoryNode(root.toString(), root.toAbsolutePath().toString(), hasChildren));
        }
      } catch (Exception e) {
        log.debug("无法访问根目录: {}", root, e);
      }
    }
    return nodes;
  }

  private List<DirectoryNode> listSubDirectories(Path parent) {
    List<DirectoryNode> nodes = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, Files::isDirectory)) {
      for (Path entry : stream) {
        // 跳过隐藏目录
        if (entry.getFileName().toString().startsWith(".")) {
          continue;
        }
        boolean hasChildren = hasSubDirectories(entry);
        nodes.add(
            new DirectoryNode(
                entry.getFileName().toString(),
                entry.toAbsolutePath().normalize().toString(),
                hasChildren));
      }
    } catch (IOException e) {
      throw new BusinessException("读取目录失败: " + parent + ", 错误: " + e.getMessage(), e);
    }
    return nodes;
  }

  private boolean hasSubDirectories(Path dir) {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, Files::isDirectory)) {
      return stream.iterator().hasNext();
    } catch (IOException e) {
      return false;
    }
  }
}
