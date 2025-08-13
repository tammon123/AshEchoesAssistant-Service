package info.qianqiu.ashechoes.utils.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.NavigateOptions;
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
    // 页面池（根据并发量调整大小）
    private final ConcurrentLinkedQueue<Page> pagePool = new ConcurrentLinkedQueue<>();
    private static final int POOL_SIZE = 20;
    // 本地资源目录
    private static final String LOCAL_RESOURCE_DIR = "/home/www/qianqiu/";

    @Autowired
    public PlaywrightPageUtil(Browser browser) {
        this.browser = browser;
        // 初始化页面池
        for (int i = 0; i < POOL_SIZE; i++) {
            pagePool.add(createNewPage());
        }
    }

    // 从池获取页面（无可用则创建临时页面）
    public Page getPage() {
        Page page = pagePool.poll();
        return page != null ? page : createNewPage();
    }

    // 归还页面时清空localStorage和其他状态
    public void releasePage(Page page) {
        if (page == null) return;
        try {
            // 1. 清空localStorage
            page.evaluate("localStorage.clear()");

            // 2. 清空sessionStorage（可选，根据需求添加）
            page.evaluate("sessionStorage.clear()");

            // 3. 导航到空白页并清除cookie
            page.navigate("about:blank");
            page.context().clearCookies();

            // 4. 放回页面池
            pagePool.offer(page);
        } catch (Exception e) {
            page.close();
            pagePool.offer(createNewPage());
        }
    }

    // 创建新页面并配置资源路由替换
    private Page createNewPage() {
        // 为每个页面创建独立上下文，彻底隔离存储
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080));
        Page page = context.newPage();
        configureResourceRouting(page);
        return page;
    }

    // 配置资源路由：替换指定域名的资源为本地文件
    private void configureResourceRouting(Page page) {
        // 拦截所有请求
        page.route("**/*", route -> {
            String url = route.request().url();
            String localFilePath = getLocalFilePath(url);

            // 如果是目标域名的资源且本地文件存在，则使用本地文件
            if (localFilePath != null) {
                File localFile = new File(localFilePath);
                if (localFile.exists() && localFile.isFile()) {
                    try {
                        // 读取本地文件并返回
                        route.fulfill(new Route.FulfillOptions()
                                .setPath(Paths.get(localFilePath))
                                .setContentType(getContentTypeFromUrl(url)));
                        return;
                    } catch (Exception e) {
                        // 本地文件处理失败，继续原始请求
                        e.printStackTrace();
                    }
                }
            }

            // 不匹配的请求正常继续
            route.resume();
        });
    }

    // 根据URL获取本地文件路径
    private String getLocalFilePath(String url) {
        // 检查URL是否包含目标域名
        if (url.contains("bjhl.qianqiu.info") || url.contains("r.qianqiu.info")) {
            // 提取域名后的路径部分
            String pathPart;
            if (url.contains("bjhl.qianqiu.info")) {
                pathPart = url.split("bjhl.qianqiu.info")[1];
            } else {
                pathPart = url.split("r.qianqiu.info")[1];
            }

            // 处理根路径情况
            if (pathPart.isEmpty() || pathPart.startsWith("/")) {
                pathPart = pathPart.startsWith("/") ? pathPart.substring(1) : pathPart;
            }
            // 拼接本地文件路径
            return LOCAL_RESOURCE_DIR + pathPart;
        }
        return null;
    }

    // 根据URL推断内容类型
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

    // 截图核心方法（封装导航、等待、截图逻辑）
    public byte[] screenshot(String url) {
        Page page = null;
        try {
            page = getPage();

            // 导航到目标URL
            NavigateOptions options = new NavigateOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)
                    .setTimeout(TimeUnit.SECONDS.toMillis(15));
            page.navigate(url, options);

            // 1. 等待页面基本加载完成
            page.waitForLoadState(LoadState.LOAD);

            // 2. 等待所有图片元素加载完成
            page.waitForSelector("img", new Page.WaitForSelectorOptions()
                    .setState(WaitForSelectorState.ATTACHED)
                    .setTimeout(TimeUnit.SECONDS.toMillis(10)));
            // 4. 额外等待网络空闲
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // 截图（按需配置参数）
            return page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(true) // 是否截取全页
                    .setOmitBackground(false) // 是否省略背景
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("截图失败: " + e.getMessage());
        } finally {
            // 归还页面到池
            if (page != null) {
                releasePage(page);
            }
        }
    }
}
    