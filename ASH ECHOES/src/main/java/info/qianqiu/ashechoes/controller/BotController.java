package info.qianqiu.ashechoes.controller;

import com.alibaba.fastjson2.JSONObject;
import info.qianqiu.ashechoes.controller.vo.*;
import info.qianqiu.ashechoes.controller.vo.bot.BotCallbackRequest;
import info.qianqiu.ashechoes.controller.vo.bot.BotCallbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.HexFormat;

@RestController
@RequestMapping("/bot")
@RequiredArgsConstructor
@Slf4j
public class BotController {

    private static final EdDSAParameterSpec ED25519 = EdDSANamedCurveTable.getByName("Ed25519");
    private static final HexFormat HEX = HexFormat.of();

    @Value("${bot.appid}")
    private String appId;

    @Value("${bot.secret}")
    private String botSecret;

    @PostMapping("/receive")
    public ResponseEntity<BotCallbackResponse> handleCallback(
            @RequestHeader("X-Bot-Appid") String headerAppId,
            @RequestBody BotCallbackRequest request) {

        try {
            // 1. 验证AppId
            if (!appId.equals(headerAppId)) {
                return ResponseEntity.status(403).build();
            }

            // 2. 验证请求体
            if (request.getD() == null ||
                    request.getD().getPlain_token() == null ||
                    request.getD().getEvent_ts() == null) {
                return ResponseEntity.badRequest().build();
            }

            // 3. 生成私钥
            EdDSAPrivateKey privateKey = generatePrivateKey(botSecret);

            // 4. 构建签名消息
            byte[] message = buildSignatureMessage(
                    request.getD().getEvent_ts(),
                    request.getD().getPlain_token()
            );

            // 5. 生成签名
            String signature = generateSignature(privateKey, message);

            // 6. 构建响应
            BotCallbackResponse response = new BotCallbackResponse();
            response.setPlain_token(request.getD().getPlain_token());
            response.setSignature(signature);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] buildSignatureMessage(String eventTs, String plainToken) {
        return (eventTs + plainToken).getBytes(StandardCharsets.UTF_8);
    }

    private EdDSAPrivateKey generatePrivateKey(String botSecret) throws Exception {
        MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
        byte[] hash = sha512.digest(botSecret.getBytes(StandardCharsets.UTF_8));

        byte[] seed = new byte[32];
        System.arraycopy(hash, 0, seed, 0, 32);

        return new EdDSAPrivateKey(new EdDSAPrivateKeySpec(seed, ED25519));
    }

    private String generateSignature(EdDSAPrivateKey privateKey, byte[] message) throws Exception {
        Signature signer = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
        signer.initSign(privateKey);
        signer.update(message);
        return HEX.formatHex(signer.sign());
    }

}