package info.qianqiu.ashechoes.utils.http;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import info.qianqiu.ashechoes.controller.vo.bot.BotConfig;
import info.qianqiu.ashechoes.controller.vo.wx.WxConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author admin
 */
@Slf4j
public class ReqUtils {

    private static final RestTemplate restTemplate = SpringUtil.getBean("restTemplate");
    private static final RestTemplate restTemplateNoReqLog = SpringUtil.getBean("restTemplateNoReqLog");
    private static final BotConfig botConfig = SpringUtil.getBean("botConfig");
    private static final WxConfig wxConfig = SpringUtil.getBean("wxConfig");

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

    public static String fget(
            String url,
            String body
    ) {
        return fget(url, body, null);
    }

    public static String fget(
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

    public static String wxPost(String url,
                                String body
    ) {
        return wxPost(url, body, 0);
    }

    public static String wxPost(String url,
                                String body,
                                int max
    ) {
        if (max > 2) {
            return "{}";
        }
        long systemTime = System.currentTimeMillis() / 1000;
        // token过期时间是7200, 最后60S内可以直接刷新
        if (wxConfig.getTOKEN_EXPIRE_TIME() == 0L || (wxConfig.getTOKEN_EXPIRE_TIME() - systemTime) < 60) {
            refreshWxToken();
        }

        url = url.replace("ACCCESS_TOKEN", wxConfig.getAUTH_TOKEN());

        URI uri = URI.create(url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "*/*");
        headers.set("Authorization", "QQBot " + wxConfig.getAUTH_TOKEN());
        HttpEntity<String> request = new HttpEntity<String>(body, headers);
        String resul = restTemplate.postForObject(uri, request, String.class);
        max += 1;
        JSONObject jo = JSONObject.parseObject(resul);
        ArrayList<String> errCodeList = new ArrayList<>();
        errCodeList.add("40014");
        errCodeList.add("61014");
        errCodeList.add("40001");
        errCodeList.add("41001");
        errCodeList.add("42001");
        if (jo != null && errCodeList.contains(jo.getString("errcode"))) {
            refreshWxToken();
            resul = wxPost(url, body, max);
        }
        return resul;
    }
//    ReqUtils.wxFormdataPost(
//            wxConfig.getUrl() + "/cgi-bin/material/add_material?access_token=ACCCESS_TOKEN&type=image",
//            new FileSystemResource("D:\\login-template (1).jpg")
//        );
    // 正确返回结果
//    {
//        "media_id": "YUpvQXmy8ZEB8jz84jJUS_bM95kw9JV5FvoYtog8WZ30i4fL-JSQxZ4padEcMHhG",
//            "url": "http://mmbiz.qpic.cn/mmbiz_jpg/tlYmINXcib9wcOfONt1xJVjyWaOlFwsNiaNiaRYltSSd7oTT8x1LjiaPbxbbXxDKhb1mSEvUyfIOTxIv3qq86urQ1g/0?wx_fmt=jpeg",
//            "item": []
//    }
    public static String wxFormdataPost(
            String url,
            FileSystemResource fileData
    ) {
        return wxFormdataPost(url, fileData, 0);
    }

    public static String wxFormdataPost(
            String url,
            FileSystemResource fileData,
            int max
    ) {
        if (max > 2) {
            return "{}";
        }

        long systemTime = System.currentTimeMillis() / 1000;
        // token过期时间是7200, 最后60S内可以直接刷新
        if (wxConfig.getTOKEN_EXPIRE_TIME() == 0L || (wxConfig.getTOKEN_EXPIRE_TIME() - systemTime) < 60) {
            refreshWxToken();
        }

        url = url.replace("ACCCESS_TOKEN", wxConfig.getAUTH_TOKEN());

        URI uri = URI.create(url);
        // 请求头
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        // 请求体容器，MultiValueMap是存储一键多值的结构
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();

        if (fileData != null) {
            formData.add("media", fileData);
        }
        //HttpEntity 代表一个request请求或者响应，包含header和body
        HttpEntity<MultiValueMap<String, Object>> files = new HttpEntity<>(formData, httpHeaders);
        String resul = restTemplateNoReqLog.postForObject(uri, files, String.class);
        max += 1;
        JSONObject jo = JSONObject.parseObject(resul);
        ArrayList<String> errCodeList = new ArrayList<>();
        errCodeList.add("40014");
        errCodeList.add("61014");
        errCodeList.add("40001");
        errCodeList.add("41001");
        errCodeList.add("42001");
        if (jo != null && errCodeList.contains(jo.getString("errcode"))) {
            refreshWxToken();
            resul = wxFormdataPost(url, fileData, max);
        }
        return resul;
    }

//    {"access_token":"95_EVUwrEkR7B1Ru3G5GYHM5l7qIqK3tFQHTjKl0EyFdZUn2gBs3X8qEjvlvpNy7SgptlAOMFAcAUhAMk96iXQJnrc6Q1oqJJny6tLWtdad72rpDPBRy7drReXMPNQGUKaAGAZND","expires_in":7200}
    public static void refreshWxToken() {
        String fpost =
                fget(wxConfig.getUrl() + "/cgi-bin/token?grant_type=client_credential&appid=" + wxConfig.getAppid() +
                        "&&secret=" + wxConfig.getSecret(), wxConfig.getTokenParam());
        JSONObject result = JSONObject.parseObject(fpost);
        String token = result.getString("access_token");
        wxConfig.setAUTH_TOKEN(token);
        wxConfig.setTOKEN_EXPIRE_TIME(System.currentTimeMillis() / 1000 + 7200);
    }

}
