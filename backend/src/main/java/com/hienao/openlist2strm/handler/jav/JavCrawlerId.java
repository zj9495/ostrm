package com.hienao.openlist2strm.handler.jav;

/**
 * JAV 站点爬虫 ID 枚举
 *
 * <p>对应 JavSP 的各个站点爬虫实现。
 *
 * @author hienao
 * @since 2024-01-01
 */
public enum JavCrawlerId {

  /** airav */
  AIRAV("airav"),

  /** avsox */
  AVSOX("avsox"),

  /** javbus */
  JAVBUS("javbus"),

  /** javdb */
  JAVDB("javdb"),

  /** javlib */
  JAVLIB("javlib"),

  /** jav321 */
  JAV321("jav321"),

  /** mgstage */
  MGSTAGE("mgstage"),

  /** prestige */
  PRESTIGE("prestige"),

  /** fc2 */
  FC2("fc2"),

  /** fc2ppvdb */
  FC2PPVDB("fc2ppvdb"),

  /** javmenu */
  JAVMENU("javmenu"),

  /** fanza */
  FANZA("fanza"),

  /** dl_getchu */
  DL_GETCHU("dl_getchu"),

  /** gyutto */
  GYUTTO("gyutto");

  private final String value;

  JavCrawlerId(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  /**
   * 根据值获取枚举
   *
   * @param value 枚举值
   * @return 对应的枚举，如果不存在则返回 null
   */
  public static JavCrawlerId fromValue(String value) {
    for (JavCrawlerId id : values()) {
      if (id.value.equalsIgnoreCase(value)) {
        return id;
      }
    }
    return null;
  }
}
