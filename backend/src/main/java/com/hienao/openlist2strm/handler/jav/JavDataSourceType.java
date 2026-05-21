package com.hienao.openlist2strm.handler.jav;

/**
 * JAV 数据源类型枚举
 *
 * <p>对应 JavSP 的数据源分类，用于确定使用哪些站点抓取器。
 *
 * @author hienao
 * @since 2024-01-01
 */
public enum JavDataSourceType {

  /** 普通番号类型 */
  NORMAL("normal"),

  /** FC2 类型 */
  FC2("fc2"),

  /** CID 类型（DMM） */
  CID("cid"),

  /** GETCHU 类型 */
  GETCHU("getchu"),

  /** GYUTTO 类型 */
  GYUTTO("gyutto");

  private final String value;

  JavDataSourceType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  /**
   * 根据值获取枚举
   *
   * @param value 枚举值
   * @return 对应的枚举，如果不存在则返回 NORMAL
   */
  public static JavDataSourceType fromValue(String value) {
    for (JavDataSourceType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    return NORMAL;
  }
}
