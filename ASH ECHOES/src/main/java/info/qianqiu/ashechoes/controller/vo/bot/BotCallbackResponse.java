package info.qianqiu.ashechoes.controller.vo.bot;

import lombok.Data;

@Data
public class BotCallbackResponse {
    private String plain_token;
    private String signature;
}