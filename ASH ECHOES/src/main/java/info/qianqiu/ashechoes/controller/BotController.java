package info.qianqiu.ashechoes.controller;

import info.qianqiu.ashechoes.controller.vo.bot.*;
import info.qianqiu.ashechoes.dto.service.UserBotService;
import info.qianqiu.ashechoes.utils.playwright.PlaywrightPageUtil;
import info.qianqiu.ashechoes.utils.string.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/bot")
@Slf4j
@RequiredArgsConstructor
public class BotController {

    private static final int ED25519_SEED_SIZE = 32; // ed25519.SeedSize = 32

    private final BotConfig botSecret;
    private final UserBotService userBotService;
    private final PlaywrightPageUtil pageUtil;

    @GetMapping("/initPlaywright")
    public CompletableFuture<byte[]> initPw() {
        FileOutputStream fos = null;
        try {
            byte[] imageData = pageUtil.screenshot("http://127.0.0.1");
            if (imageData == null || imageData.length == 0) {
                return CompletableFuture.completedFuture("截图失败：未获取到图片数据".getBytes());
            }

            // 2. 准备保存路径：D盘1.jpg
            File outputFile = new File("/home/www/qianqiu/bot/chouka/1.jpg");

            // 3. 确保目录存在（D盘通常无需创建，但以防万一）
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }

            // 4. 写入文件
            fos = new FileOutputStream(outputFile);
            fos.write(imageData);
            fos.flush();

            return CompletableFuture.completedFuture(("截图成功，保存路径：" + outputFile.getAbsolutePath()).getBytes());

        } catch (IOException e) {
            String errorMsg = "保存截图失败：" + e.getMessage();
            if (e.getMessage().contains("拒绝访问")) {
                errorMsg += "，请检查D盘写入权限";
            } else if (e.getMessage().contains("系统找不到指定的路径")) {
                errorMsg += "，请确认D盘存在";
            }
            return CompletableFuture.completedFuture(errorMsg.getBytes());
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

    @PostMapping("/receive")
    public ResponseEntity<BotCallbackResponse> handleCallback(
            @RequestBody BotCallbackRequest request) {
        try {
            if (request.getOp() == 13) {
                responseSign(request);
            }
            // 收消息
            if (request.getOp() == 0) {

                return userBotService.groupChat(request);
            }
            return ResponseEntity.ok(new BotCallbackResponse(
                    "",
                    ""
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<BotCallbackResponse> responseSign(BotCallbackRequest request) {
        BotCallbackData validationRequest = new BotCallbackData();
        validationRequest.setEvent_ts(request.getD().getEvent_ts());
        validationRequest.setPlain_token(request.getD().getPlain_token());

        // 2. 生成种子
        String seed = generateSeed(botSecret.getSecret()); // 替换实际密钥

        // 3. 生成 Ed25519 私钥
        Ed25519PrivateKeyParameters privateKey = generatePrivateKey(seed);

        // 4. 生成签名
        String signature = generateSignature(
                privateKey,
                validationRequest.getEvent_ts(),
                validationRequest.getPlain_token()
        );

        // 5. 构建响应
        return ResponseEntity.ok(new BotCallbackResponse(
                validationRequest.getPlain_token(),
                signature
        ));
    }

    // 生成符合长度的种子字符串
    private String generateSeed(String botSecret) {
        StringBuilder seedBuilder = new StringBuilder(botSecret);
        while (seedBuilder.length() < ED25519_SEED_SIZE) {
            seedBuilder.append(seedBuilder);
        }
        return seedBuilder.substring(0, ED25519_SEED_SIZE);
    }

    // 生成 Ed25519 私钥
    private Ed25519PrivateKeyParameters generatePrivateKey(String seed) {
        byte[] seedBytes = seed.getBytes();
        return new Ed25519PrivateKeyParameters(seedBytes, 0);
    }

    // 生成签名
    private String generateSignature(
            Ed25519PrivateKeyParameters privateKey,
            String eventTs,
            String plainToken
    ) {
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);
        byte[] message = (eventTs + plainToken).getBytes();
        signer.update(message, 0, message.length);
        byte[] signatureBytes = signer.generateSignature();
        // 将签名转换为十六进制字符串
        return StringUtils.bytesToHex(signatureBytes);
    }

}