package info.qianqiu.ashechoes.utils.playwright;

import com.microsoft.playwright.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
public class PlaywrightConfig {

    // 全局唯一的 Playwright 实例
    private Playwright playwright;
    // 全局复用的浏览器实例（无头模式，性能更好）
    private Browser browser;

    @Bean(destroyMethod = "close")
    public Browser browser() {
        // 初始化 Playwright 和浏览器（只执行一次）
        if (playwright == null) {
            playwright = Playwright.create();
        }
        if (browser == null) {
            // 启动参数优化
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList(
                            "--disable-gpu",
                            "--no-sandbox",
                            "--disable-web-security",
                            "--allow-running-insecure-content",
                            "--disable-dev-shm-usage",
                            "--disable-extensions"
                    ))
                    .setTimeout(TimeUnit.SECONDS.toMillis(30));

            browser = playwright.chromium().launch(options);
        }
        return browser;
    }

    // 关闭资源（Spring 销毁时调用）
    public void close() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}