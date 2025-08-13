package info.qianqiu.ashechoes.utils.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.NavigateOptions;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Component
public class PlaywrightPageUtil {

    private final Browser browser;
    private final ConcurrentLinkedQueue<Page> pagePool = new ConcurrentLinkedQueue<>();
    private static final int POOL_SIZE = 20;
    private static final String LOCAL_RESOURCE_DIR = "/home/www/qianqiu/";
    // 新增：重试次数配置
    private static final int MAX_RETRY = 3;

    @Autowired
    public PlaywrightPageUtil(Browser browser) {
        this.browser = browser;
        // 初始化页面池
        for (int i = 0; i < POOL_SIZE; i++) {
            pagePool.add(createNewPage());
        }
    }

    // 改进：从池获取页面时校验有效性
    public Page getPage() {
        Page page;
        // 循环检查池中页面是否有效
        while ((page = pagePool.poll()) != null) {
            if (isPageValid(page)) {
                return page;
            } else {
                // 无效页面直接关闭
                safeClosePage(page);
            }
        }
        // 池中无有效页面，创建新页面
        return createNewPage();
    }

    // 改进：释放页面时增强校验和清理
    public void releasePage(Page page) {
        if (page == null) return;

        try {
            // 先校验页面是否有效
            if (!isPageValid(page)) {
                safeClosePage(page);
                pagePool.offer(createNewPage());
                return;
            }

            // 清空页面状态（保持原有逻辑）
            page.evaluate("localStorage.clear()");
            page.evaluate("sessionStorage.clear()");
            page.navigate("about:blank", new NavigateOptions().setTimeout(TimeUnit.SECONDS.toMillis(5)));
            page.context().clearCookies();

            // 若池未满则放回，否则关闭（避免池无限膨胀）
            if (pagePool.size() < POOL_SIZE) {
                pagePool.offer(page);
            } else {
                safeClosePage(page);
            }
        } catch (Exception e) {
            // 处理过程中出错，直接替换为新页面
            safeClosePage(page);
            pagePool.offer(createNewPage());
        }
    }

    // 新增：校验页面是否有效
    private boolean isPageValid(Page page) {
        try {
            // 通过访问页面上下文判断是否已关闭
            page.context();
            // 尝试获取页面标题（轻量操作）
            page.title();
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    // 新增：安全关闭页面（避免关闭已关闭的页面导致异常）
    private void safeClosePage(Page page) {
        try {
            if (isPageValid(page)) {
                page.close();
            }
        } catch (Exception e) {
            // 忽略关闭时的异常
        }
    }

    // 创建新页面（保持原有逻辑）
    private Page createNewPage() {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(350, 1080));
        Page page = context.newPage();
        configureResourceRouting(page);
        return page;
    }

    // 配置资源路由（保持原有逻辑）
    private void configureResourceRouting(Page page) {
        page.route("**/*", route -> {
            String url = route.request().url();
            String localFilePath = getLocalFilePath(url);
            if (localFilePath != null) {
                File localFile = new File(localFilePath);
                if (localFile.exists() && localFile.isFile()) {
                    try {
                        route.fulfill(new Route.FulfillOptions()
                                .setPath(Paths.get(localFilePath))
                                .setContentType(getContentTypeFromUrl(url)));
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            route.resume();
        });
    }

    // 根据URL获取本地文件路径（保持原有逻辑）
    private String getLocalFilePath(String url) {
        if (url.contains("bjhl.qianqiu.info") || url.contains("r.qianqiu.info")) {
            String pathPart;
            if (url.contains("bjhl.qianqiu.info")) {
                pathPart = url.split("bjhl.qianqiu.info")[1];
            } else {
                pathPart = url.split("r.qianqiu.info")[1];
            }

            if (pathPart.isEmpty() || pathPart.startsWith("/")) {
                pathPart = pathPart.startsWith("/") ? pathPart.substring(1) : pathPart;
            }
            return LOCAL_RESOURCE_DIR + pathPart;
        }
        return null;
    }

    // 根据URL推断内容类型（保持原有逻辑）
    private String getContentTypeFromUrl(String url) {
        if (url.endsWith(".html") || url.endsWith(".htm")) {
            return "text/html";
        } else if (url.endsWith(".css")) {
            return "text/css";
        } else if (url.endsWith(".js")) {
            return "application/javascript";
        } else if (url.endsWith(".png")) {
            return "image/png";
        } else if (url.endsWith(".jpg") || url.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (url.endsWith(".gif")) {
            return "image/gif";
        } else if (url.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (url.endsWith(".json")) {
            return "application/json";
        } else {
            return "application/octet-stream";
        }
    }

    // 改进：截图方法增加重试机制
    public byte[] screenshot(String url) {
        Page page = null;
        // 重试逻辑
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                page = getPage();
                if (page == null) {
                    throw new RuntimeException("无法获取有效的页面实例");
                }

                NavigateOptions options = new NavigateOptions()
                        .setWaitUntil(WaitUntilState.NETWORKIDLE)
                        .setTimeout(TimeUnit.SECONDS.toMillis(15));
                page.navigate(url, options);
                page.waitForLoadState(LoadState.LOAD);

                // 截图
                return page.screenshot(new Page.ScreenshotOptions()
                        .setFullPage(true)
                        .setOmitBackground(false));
            } catch (PlaywrightException e) {
                // 仅捕获Playwright相关异常（连接关闭、超时等）
                System.err.println("截图失败（第" + (retry + 1) + "次）：" + e.getMessage());
                // 出错的页面直接关闭，不再放回池
                safeClosePage(page);
                page = null;
                // 最后一次重试失败则抛出异常
                if (retry == MAX_RETRY - 1) {
                    throw new RuntimeException("达到最大重试次数，截图失败: " + e.getMessage());
                }
            } catch (Exception e) {
                // 其他异常直接抛出
                safeClosePage(page);
                throw new RuntimeException("截图发生未知错误: " + e.getMessage());
            } finally {
                if (page != null) {
                    releasePage(page);
                }
            }
        }
        throw new RuntimeException("截图逻辑异常");
    }
}