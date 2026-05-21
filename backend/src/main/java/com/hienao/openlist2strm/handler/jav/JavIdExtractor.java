package com.hienao.openlist2strm.handler.jav;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * JAV 番号识别器
 *
 * <p>移植自 JavSP 的 avid.py，实现番号识别、CID 识别和数据源类型判定。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
public class JavIdExtractor {

  // 普通番号正则表达式（如 ABC-123, ABC123, HEYDOUGA-1234-567, RED096）
  private static final Pattern NORMAL_NUMBER_PATTERN =
      Pattern.compile("([a-zA-Z]{2,10})-?(\\d{2,8})", Pattern.CASE_INSENSITIVE);

  // FC2 编号正则表达式（如 FC2-123456, FC2-1234567）
  private static final Pattern FC2_PATTERN =
      Pattern.compile("FC2-?(\\d{5,7})", Pattern.CASE_INSENSITIVE);

  // GETCHU 编号正则表达式
  private static final Pattern GETCHU_PATTERN =
      Pattern.compile("GETCHU-?(\\d+)", Pattern.CASE_INSENSITIVE);

  // GYUTTO 编号正则表达式
  private static final Pattern GYUTTO_PATTERN =
      Pattern.compile("GYUTTO-?(\\d+)", Pattern.CASE_INSENSITIVE);

  // CID 正则表达式（DMM 的内容 ID，通常是字母数字混合）
  private static final Pattern CID_PATTERN =
      Pattern.compile("([a-zA-Z]{2,10}\\d{3,10})", Pattern.CASE_INSENSITIVE);

  // 特殊番号正则表达式（如 HEYDOUGA-1234-567）
  private static final Pattern SPECIAL_NUMBER_PATTERN =
      Pattern.compile("(HEYDOUGA)-(\\d{4})-(\\d{3})", Pattern.CASE_INSENSITIVE);

  // 带有日期格式的番号（如 2020.01.01 或 2020-01-01）
  private static final Pattern DATE_FORMAT_PATTERN =
      Pattern.compile("(\\d{4})[.-](\\d{2})[.-](\\d{2})");

  /**
   * 从文件路径或文件名中提取 JAV 标识
   *
   * @param filePath 文件路径
   * @return JAV 标识信息，如果无法识别则返回 null
   */
  public static JavIdentifier extract(String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      return null;
    }

    // 尝试从文件名识别
    String fileName = getFileName(filePath);
    JavIdentifier result = extractFromFileName(fileName);
    if (result != null) {
      return result;
    }

    // 如果文件名无法识别，尝试从父目录识别
    String parentDir = getParentDirName(filePath);
    if (parentDir != null && !parentDir.isEmpty()) {
      result = extractFromFileName(parentDir);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  /**
   * 从文件名中提取 JAV 标识
   *
   * @param fileName 文件名
   * @return JAV 标识信息，如果无法识别则返回 null
   */
  public static JavIdentifier extractFromFileName(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return null;
    }

    // 移除文件扩展名
    String nameWithoutExt = removeFileExtension(fileName);

    // 尝试识别 FC2
    JavIdentifier fc2Result = extractFc2(nameWithoutExt);
    if (fc2Result != null) {
      return fc2Result;
    }

    // 尝试识别 GETCHU
    JavIdentifier getchuResult = extractGetchu(nameWithoutExt);
    if (getchuResult != null) {
      return getchuResult;
    }

    // 尝试识别 GYUTTO
    JavIdentifier gyuttoResult = extractGyutto(nameWithoutExt);
    if (gyuttoResult != null) {
      return gyuttoResult;
    }

    // 尝试识别特殊番号（如 HEYDOUGA）
    JavIdentifier specialResult = extractSpecialNumber(nameWithoutExt);
    if (specialResult != null) {
      return specialResult;
    }

    // 尝试识别普通番号
    JavIdentifier normalResult = extractNormalNumber(nameWithoutExt);
    if (normalResult != null) {
      return normalResult;
    }

    // 尝试识别 CID
    JavIdentifier cidResult = extractCid(nameWithoutExt);
    if (cidResult != null) {
      return cidResult;
    }

    return null;
  }

  /**
   * 提取 FC2 编号
   */
  private static JavIdentifier extractFc2(String text) {
    Matcher matcher = FC2_PATTERN.matcher(text);
    if (matcher.find()) {
      String number = "FC2-" + matcher.group(1);
      return new JavIdentifier(number, number, JavDataSourceType.FC2);
    }
    return null;
  }

  /**
   * 提取 GETCHU 编号
   */
  private static JavIdentifier extractGetchu(String text) {
    Matcher matcher = GETCHU_PATTERN.matcher(text);
    if (matcher.find()) {
      String number = "GETCHU-" + matcher.group(1);
      return new JavIdentifier(number, number, JavDataSourceType.GETCHU);
    }
    return null;
  }

  /**
   * 提取 GYUTTO 编号
   */
  private static JavIdentifier extractGyutto(String text) {
    Matcher matcher = GYUTTO_PATTERN.matcher(text);
    if (matcher.find()) {
      String number = "GYUTTO-" + matcher.group(1);
      return new JavIdentifier(number, number, JavDataSourceType.GYUTTO);
    }
    return null;
  }

  /**
   * 提取特殊番号（如 HEYDOUGA-1234-567）
   */
  private static JavIdentifier extractSpecialNumber(String text) {
    Matcher matcher = SPECIAL_NUMBER_PATTERN.matcher(text);
    if (matcher.find()) {
      String number = matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3);
      return new JavIdentifier(number, number, JavDataSourceType.NORMAL);
    }
    return null;
  }

  /**
   * 提取普通番号
   */
  private static JavIdentifier extractNormalNumber(String text) {
    Matcher matcher = NORMAL_NUMBER_PATTERN.matcher(text);
    if (matcher.find()) {
      String prefix = matcher.group(1).toUpperCase();
      String digits = matcher.group(2);

      // 标准化番号格式
      String number = prefix + "-" + digits;

      // 生成 CID（用于 DMM 查询）
      String cid = prefix.toLowerCase() + digits;

      return new JavIdentifier(number, cid, JavDataSourceType.NORMAL);
    }
    return null;
  }

  /**
   * 提取 CID
   */
  private static JavIdentifier extractCid(String text) {
    Matcher matcher = CID_PATTERN.matcher(text);
    if (matcher.find()) {
      String cid = matcher.group(1);

      // 尝试从 CID 推断番号
      String number = inferNumberFromCid(cid);

      return new JavIdentifier(number, cid, JavDataSourceType.CID);
    }
    return null;
  }

  /**
   * 从 CID 推断番号
   */
  private static String inferNumberFromCid(String cid) {
    if (cid == null || cid.isEmpty()) {
      return null;
    }

    // 尝试匹配常见的番号前缀
    String upperCid = cid.toUpperCase();
    for (String prefix : getCommonPrefixes()) {
      if (upperCid.startsWith(prefix)) {
        String digits = upperCid.substring(prefix.length());
        if (digits.matches("\\d+")) {
          return prefix + "-" + digits;
        }
      }
    }

    return cid;
  }

  /**
   * 获取常见的番号前缀列表
   */
  private static String[] getCommonPrefixes() {
    return new String[] {
      "IPX", "ABP", "SSNI", "SSIS", "PRED", "MIDE", "STARS", "SONE", "SDDE", "SOE",
      "AP", "AR", "ATID", "AWM", "BF", "BLK", "BRAZZ", "CWM", "DAS", "DCV",
      "DDF", "DLDSS", "DM", "DOCP", "DVAJ", "EBOD", "FSDSS", "GANA", "GVG", "HND",
      "IESP", "IPZ", "JUFE", "JUL", "KAWD", "KBI", "LAF", "LULU", "MADM", "MDVR",
      "MEYD", "MIAA", "MIDD", "MIFD", "MIID", "MKMP", "MMND", "MUM", "MXGS", "NHDTA",
      "NKKD", "NSPS", "NSFS", "OAE", "OFJE", "OGPP", "OKAD", "PRTD", "PPPD", "PSD",
      "RCT", "REBD", "RTP", "RVG", "SABA", "SAN", "SCOP", "SDDE", "SDMU", "SIRO",
      "SKY", "SMD", "SNIS", "SOE", "SSPD", "START", "SVDVD", "TEK", "TRE", "UMD",
      "VAGU", "VEO", "VEMA", "WAAA", "WANZ", "YMDD"
    };
  }

  /**
   * 获取文件名（不含路径）
   */
  private static String getFileName(String filePath) {
    if (filePath == null) {
      return "";
    }
    int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
    return lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
  }

  /**
   * 获取父目录名
   */
  private static String getParentDirName(String filePath) {
    if (filePath == null) {
      return null;
    }
    try {
      java.nio.file.Path path = java.nio.file.Paths.get(filePath);
      java.nio.file.Path parent = path.getParent();
      return parent != null ? parent.getFileName().toString() : null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 移除文件扩展名
   */
  private static String removeFileExtension(String fileName) {
    if (fileName == null) {
      return "";
    }
    int lastDot = fileName.lastIndexOf('.');
    return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
  }

  /**
   * JAV 标识信息
   */
  public static class JavIdentifier {

    /** 番号 */
    private final String number;

    /** CID */
    private final String cid;

    /** 数据源类型 */
    private final JavDataSourceType dataSourceType;

    public JavIdentifier(String number, String cid, JavDataSourceType dataSourceType) {
      this.number = number;
      this.cid = cid;
      this.dataSourceType = dataSourceType;
    }

    public String getNumber() {
      return number;
    }

    public String getCid() {
      return cid;
    }

    public JavDataSourceType getDataSourceType() {
      return dataSourceType;
    }

    @Override
    public String toString() {
      return "JavIdentifier{"
          + "number='"
          + number
          + "', cid='"
          + cid
          + "', dataSourceType="
          + dataSourceType
          + "}";
    }
  }
}
