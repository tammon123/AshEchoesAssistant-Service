package info.qianqiu.ashechoes.controller.vo.bot;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component("botConfig")
@ConfigurationProperties(prefix = "bot")
public class BotConfig {

    private String tokenUrl;
    private String secret;
    private String appid;
    private String url;
    private String surl;

    // token失效时间，写在这里面算求
    private Long TOKEN_EXPIRE_TIME = 0L;
    private String AUTH_TOKEN = "";

    public String getTokenParam() {
        JSONObject jo = new JSONObject();
        jo.put("appId", appid);
        jo.put("clientSecret", secret);
        return jo.toJSONString();
    }

}
