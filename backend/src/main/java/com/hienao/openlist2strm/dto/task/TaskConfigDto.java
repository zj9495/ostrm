package com.hienao.openlist2strm.dto.task;

import com.hienao.openlist2strm.validation.ValidCronExpression;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 任务配置DTO
 *
 * @author hienao
 * @since 2024-01-01
 */
@Data
@Accessors(chain = true)
public class TaskConfigDto {

  /** 主键ID */
  private Long id;

  /** 任务名称 */
  @NotBlank(message = "任务名称不能为空") @Size(max = 200, message = "任务名称长度不能超过200个字符") private String taskName;

  /** 任务路径 */
  @NotBlank(message = "任务路径不能为空") @Size(max = 500, message = "任务路径长度不能超过500个字符") private String path;

  /** 关联的openlist_config表ID */
  private Long openlistConfigId;

  /** 是否需要刮削：true-需要，false-不需要 */
  private Boolean needScrap;

  /** 刮削器类型：TMDB-默认，JAV-Jav刮削器 */
  @Pattern(regexp = "^(TMDB|JAV)$", message = "刮削器类型必须是 TMDB 或 JAV")
  private String scraperType;

  /** 重命名正则表达式，为空时表示不需要重命名 */
  @Size(max = 500, message = "重命名正则表达式长度不能超过500个字符") private String renameRegex;

  /** 定时任务表达式 */
  @Size(max = 100, message = "定时任务表达式长度不能超过100个字符") @ValidCronExpression(message = "定时任务表达式格式不正确")
  private String cron;

  /** 是否是增量更新：true-是，false-否 */
  private Boolean isIncrement;

  /** 生成strm的基础路径 */
  @Size(max = 500, message = "strm路径长度不能超过500个字符") private String strmPath;

  /** 上次执行的时间戳 */
  private Long lastExecTime;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;

  /** 是否启用：true-启用，false-禁用 */
  private Boolean isActive;

  /** 最小文件大小，单位 bytes，为空时不启用 */
  private Long minFileSizeBytes;

  /** 文件名排除正则表达式，为空时不启用 */
  @Size(max = 500, message = "文件名排除正则表达式长度不能超过500个字符") private String fileNameExcludeRegex;

  /** 目录名称排除正则表达式，为空时不启用 */
  @Size(max = 500, message = "目录名称排除正则表达式长度不能超过500个字符") private String directoryNameExcludeRegex;

  /** STRM 内容地址替换来源字符串，为空时不启用 */
  @Size(max = 500, message = "STRM URL替换来源长度不能超过500个字符") private String strmUrlReplaceFrom;

  /** STRM 内容地址替换目标字符串 */
  @Size(max = 500, message = "STRM URL替换目标长度不能超过500个字符") private String strmUrlReplaceTo;

  /** 是否生成 sign 查询参数 */
  private Boolean generateSign;
}
