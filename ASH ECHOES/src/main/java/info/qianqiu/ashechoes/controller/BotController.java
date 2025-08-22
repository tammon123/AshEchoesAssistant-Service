package info.qianqiu.ashechoes.controller;

import info.qianqiu.ashechoes.controller.vo.bot.*;
import info.qianqiu.ashechoes.dto.service.UserBotService;
import info.qianqiu.ashechoes.utils.http.R;
import info.qianqiu.ashechoes.utils.string.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bot")
@Slf4j
@RequiredArgsConstructor
public class BotController {

    private static final int ED25519_SEED_SIZE = 32; // ed25519.SeedSize = 32

    private final BotConfig botSecret;
    private final UserBotService userBotService;

    @GetMapping("/wiki/search/{id}")
    public R searchKey(@PathVariable("id") Long key) {
        String[] name = new String[1];
        String[] image = new String[1];
        String[] sname = new String[1];
        String[] url = new String[1];
        userBotService.searchDataById(key, name, image, sname, url);
        if (name[0] == null) {
            return R.fail("未检索到《" + key + "》的档案信息");
        }
        return R.ok(url[0]);
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