package info.qianqiu.ashechoes.controller.vo.bot;

import lombok.Data;

@Data
public class BotCallbackData {
    private String plain_token;
    private String event_ts;
    // 其他可能的字段...
}