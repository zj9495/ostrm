package com.hienao.openlist2strm.handler.jav;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * JAV 爬虫 HTTP 客户端助手
 *
 * <p>提供统一的 HTTP 请求功能，支持代理、超时、重试和 HTML 解析。
 *
 * @author hienao
 * @since 2024-01-01
 */
@Slf4j
public class JavHttpClient {

  private final HttpClient httpClient;
  private final Map<String, Object> networkConfig;
  private final Map<String, String> proxyFreeDomains;

  /**
   * 构造函数
   *
   * @param networkConfig 网络配置
   */
  public JavHttpClient(Map<String, Object> networkConfig) {
    this.networkConfig = networkConfig;
    this.proxyFreeDomains = getProxyFreeDomains();
    this.httpClient = createHttpClient();
  }

  /**
   * 创建 HTTP 客户端
   */
  private HttpClient createHttpClient() {
    HttpClient.Builder builder = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(getTimeoutSeconds()))
        .followRedirects(HttpClient.Redirect.NORMAL);

    // 配置代理
    String proxyServer = getProxyServer();
    if (proxyServer != null && !proxyServer.isEmpty()) {
      try {
        URI proxyUri = URI.create(proxyServer);
        String host = proxyUri.getHost();
        int port = proxyUri.getPort();
        if (port == -1) {
          port = proxyUri.getScheme().equals("https") ? 443 : 80;
        }
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        builder.proxy(new ProxySelector(proxy));
        log.info("已配置 HTTP 代理: {}:{}", host, port);
      } catch (Exception e) {
        log.warn("解析代理服务器地址失败: {}, 错误: {}", proxyServer, e.getMessage());
      }
    }

    return builder.build();
  }

  /**
   * 获取页面文档
   *
   * @param url 页面 URL
   * @return Jsoup 文档对象
   * @throws IOException IO 异常
   * @throws InterruptedException 中断异常
   */
  public Document getDocument(String url) throws IOException, InterruptedException {
    return getDocument(url, getDefaultHeaders());
  }

  /**
   * 获取页面文档（自定义标头）
   *
   * @param url 页面 URL
   * @param headers 自定义标头
   * @return Jsoup 文档对象
   * @throws IOException IO 异常
   * @throws InterruptedException 中断异常
   */
  public Document getDocument(String url, Map<String, String> headers)
      throws IOException, InterruptedException {
    String html = get(url, headers);
    return Jsoup.parse(html);
  }

  /**
   * 发送 GET 请求
   *
   * @param url 请求 URL
   * @return 响应内容
   * @throws IOException IO 异常
   * @throws InterruptedException 中断异常
   */
  public String get(String url) throws IOException, InterruptedException {
    return get(url, getDefaultHeaders());
  }

  /**
   * 发送 GET 请求（自定义标头）
   *
   * @param url 请求 URL
   * @param headers 自定义标头
   * @return 响应内容
   * @throws IOException IO 异常
   * @throws InterruptedException 中断异常
   */
  public String get(String url, Map<String, String> headers)
      throws IOException, InterruptedException {
    int maxRetries = getRetryCount();
    Exception lastException = null;

    for (int attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        if (attempt > 0) {
          log.debug("重试请求 ({}/{}): {}", attempt, maxRetries, url);
          TimeUnit.SECONDS.sleep(1); // 重试前等待 1 秒
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(getTimeoutSeconds()))
            .GET();

        // 添加标头
        if (headers != null) {
          headers.forEach(requestBuilder::header);
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          return response.body();
        } else if (response.statusCode() == 403) {
          log.warn("请求被拒绝 (403): {}", url);
          throw new IOException("请求被拒绝 (403): " + url);
        } else if (response.statusCode() == 404) {
          log.debug("页面不存在 (404): {}", url);
          throw new IOException("页面不存在 (404): " + url);
        } else {
          log.warn("请求失败，状态码: {}, URL: {}", response.statusCode(), url);
          throw new IOException("请求失败，状态码: " + response.statusCode());
        }

      } catch (IOException | InterruptedException e) {
        lastException = e;
        if (attempt == maxRetries) {
          log.error("请求失败，已重试 {} 次: {}", maxRetries, url, e);
          throw e;
        }
        log.debug("请求失败，将重试: {}, 错误: {}", url, e.getMessage());
      }
    }

    if (lastException instanceof IOException ioException) {
      throw ioException;
    }
    if (lastException instanceof InterruptedException interruptedException) {
      throw interruptedException;
    }
    throw new IOException("请求失败");
  }

  /**
   * 检查是否需要代理
   *
   * @param url URL 地址
   * @return 是否需要代理
   */
  public boolean needsProxy(String url) {
    if (proxyFreeDomains == null || proxyFreeDomains.isEmpty()) {
      return true;
    }

    try {
      URI uri = URI.create(url);
      String host = uri.getHost();
      if (host == null) {
        return true;
      }

      // 检查是否是免代理域名
      for (Map.Entry<String, String> entry : proxyFreeDomains.entrySet()) {
        if (host.contains(entry.getValue())) {
          return false;
        }
      }
    } catch (Exception e) {
      log.debug("解析 URL 失败: {}", url);
    }

    return true;
  }

  /**
   * 获取默认标头
   */
  private Map<String, String> getDefaultHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
    headers.put("Accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
    headers.put("Accept-Language", "ja,en-US;q=0.7,en;q=0.3");
    headers.put("Accept-Encoding", "gzip, deflate");
    headers.put("Connection", "keep-alive");
    headers.put("Upgrade-Insecure-Requests", "1");
    return headers;
  }

  /**
   * 获取代理服务器地址
   */
  private String getProxyServer() {
    if (networkConfig == null) {
      return null;
    }
    Object proxyServer = networkConfig.get("proxyServer");
    return proxyServer != null ? proxyServer.toString() : null;
  }

  /**
   * 获取超时时间（秒）
   */
  private int getTimeoutSeconds() {
    if (networkConfig == null) {
      return 30;
    }
    Object timeout = networkConfig.get("timeoutSeconds");
    if (timeout instanceof Number) {
      return ((Number) timeout).intValue();
    }
    return 30;
  }

  /**
   * 获取重试次数
   */
  private int getRetryCount() {
    if (networkConfig == null) {
      return 3;
    }
    Object retry = networkConfig.get("retry");
    if (retry instanceof Number) {
      return ((Number) retry).intValue();
    }
    return 3;
  }

  /**
   * 获取免代理域名配置
   */
  @SuppressWarnings("unchecked")
  private Map<String, String> getProxyFreeDomains() {
    if (networkConfig == null) {
      return new HashMap<>();
    }
    Object proxyFree = networkConfig.get("proxyFree");
    if (proxyFree instanceof Map) {
      return (Map<String, String>) proxyFree;
    }
    return new HashMap<>();
  }

  /**
   * 自定义 ProxySelector（简化实现）
   */
  private class ProxySelector extends java.net.ProxySelector {
    private final Proxy proxy;

    private ProxySelector(Proxy proxy) {
      this.proxy = proxy;
    }

    @Override
    public java.util.List<Proxy> select(URI uri) {
      if (needsProxy(uri.toString())) {
        return java.util.List.of(proxy);
      }
      return java.util.List.of(Proxy.NO_PROXY);
    }

    @Override
    public void connectFailed(URI uri, java.net.SocketAddress sa, IOException ioe) {
      log.warn("代理连接失败: {}, 错误: {}", uri, ioe.getMessage());
    }
  }
}
