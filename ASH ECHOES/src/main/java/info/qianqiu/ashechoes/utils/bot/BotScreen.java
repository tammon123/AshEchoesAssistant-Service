package info.qianqiu.ashechoes.utils.bot;

import info.qianqiu.ashechoes.utils.playwright.PlaywrightPageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotScreen {

    private final PlaywrightPageUtil pageUtil;

    public CompletableFuture<String> screen(String url, String uid, String behavior) {
        FileOutputStream fos = null;
        log.error("截图路径：{}", url);
        try {
            byte[] imageData = pageUtil.screenshot(url);
            if (imageData == null || imageData.length == 0) {
                return CompletableFuture.completedFuture("截图失败：未获取到图片数据");
            }

            // 2. 准备保存路径：D盘1.jpg
            String path = "/home/www/qianqiu/bot/chouka/";
            String fileName = uid + "_" + behavior + ".jpg";
            if (System.getenv("LOCAL_MACHINE") != null) {
                path = "D:/";
            }
            File outputFile = new File(path + fileName);

            // 3. 确保目录存在（D盘通常无需创建，但以防万一）
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }

            // 4. 写入文件
            fos = new FileOutputStream(outputFile);
            fos.write(imageData);
            fos.flush();

            return CompletableFuture.completedFuture("https://bjhl.qianqiu.info/bot/chouka/" + fileName);

        } catch (IOException e) {
            String errorMsg = "请联系开发者(865686593)：保存截图失败：" + e.getMessage();
            if (e.getMessage().contains("拒绝访问")) {
                errorMsg += "，请检查写入权限";
            } else if (e.getMessage().contains("系统找不到指定的路径")) {
                errorMsg += "，请确认存在";
            }
            return CompletableFuture.completedFuture(errorMsg);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }
    }

}
