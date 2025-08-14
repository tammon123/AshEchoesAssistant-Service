package info.qianqiu.ashechoes.utils.http;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import info.qianqiu.ashechoes.controller.vo.bot.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * @author admin
 */
@Slf4j
public class ReqUtils {

    private static final RestTemplate restTemplate = SpringUtil.getBean("restTemplate");
    private static final BotConfig botConfig = SpringUtil.getBean("botConfig");

    public static String get(String url) {
        return get(url, true);
    }

    public static String get(String url, boolean needLog) {
        // 请求头
        HttpHeaders headers = new HttpHeaders();
        return get(url, headers, needLog);
    }

    /**
     * 自定义超时时间
     *
     * @param url
     * @return
     */
    public static String get(String url, HttpHeaders headers, boolean needLog) {
        URI uri = URI.create(url);
        // 请求头
        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        }
        HttpEntity<MultiValueMap<String, Object>> formEntity = new HttpEntity<MultiValueMap<String, Object>>(headers);
        ResponseEntity<String> exchange = restTemplate.exchange(uri, HttpMethod.GET, formEntity, String.class);
        return exchange.getBody();
    }

    public static String fpost(
            String url,
            String body
    ) {
        return fpost(url, body, null);
    }

    public static String fpost(
            String url,
            String body,
            HttpHeaders headers
    ) {
        URI uri = URI.create(url);
        if (headers == null) {
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("accept", "*/*");
        }
        HttpEntity<String> request = new HttpEntity<String>(body, headers);

        return restTemplate.postForObject(uri, request, String.class);
    }

    public static String botPost(String url,
                                 String body
    ) {
        return botPost(url, body, 0);
    }

    public static String botPost(String url,
                                 String body,
                                 int max
    ) {
        if (max > 2) {
            return "{}";
        }
        long systemTime = System.currentTimeMillis() / 1000;
        // token过期时间是7200, 最后60S内可以直接刷新
        if (botConfig.getTOKEN_EXPIRE_TIME() == 0L || (botConfig.getTOKEN_EXPIRE_TIME() - systemTime) < 60) {
            refreshBotToken();
        }
        URI uri = URI.create(url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "*/*");
        headers.set("Authorization", "QQBot " + botConfig.getAUTH_TOKEN());
        HttpEntity<String> request = new HttpEntity<String>(body, headers);
//        {"message":"token not exist or expire","code":11244,"err_code":11244,"trace_id":"44f01fbc8ad9416bb2bf40a07e69ef10"}
        String resul = restTemplate.postForObject(uri, request, String.class);
        max += 1;
        JSONObject jo = JSONObject.parseObject(resul);
        if (jo != null && "11244".equals(jo.getString("code"))) {
            refreshBotToken();
            resul = botPost(url, body, max);
        }
        return resul;
    }

    public static void refreshBotToken() {
        String fpost = fpost(botConfig.getTokenUrl(), botConfig.getTokenParam());
        JSONObject result = JSONObject.parseObject(fpost);
        String token = result.getString("access_token");
        botConfig.setAUTH_TOKEN(token);
        botConfig.setTOKEN_EXPIRE_TIME(System.currentTimeMillis() / 1000 + 7200);
    }

}
