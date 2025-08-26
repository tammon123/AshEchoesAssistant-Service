package info.qianqiu.ashechoes.controller.vo.wx;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component("wxConfig")
@ConfigurationProperties(prefix = "wx")
public class WxConfig {

    private String url;
    private String token;
    private String aes;
    private String secret;
    private String appid;

    // token失效时间，写在这里面算求
    private Long TOKEN_EXPIRE_TIME = 0L;
    private String AUTH_TOKEN = "";

    public String getTokenParam() {
        JSONObject jo = new JSONObject();
        jo.put("grant_type", "client_credential");
        jo.put("appid", appid);
        jo.put("secret", secret);

        return jo.toJSONString();
    }
}
