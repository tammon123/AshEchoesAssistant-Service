package info.qianqiu.ashechoes.controller.vo;

import lombok.Data;

@Data
public class BotPayload {

    private String plain_token;
    private String event_ts;

}
