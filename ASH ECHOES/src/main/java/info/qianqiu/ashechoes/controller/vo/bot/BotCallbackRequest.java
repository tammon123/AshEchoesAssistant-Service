package info.qianqiu.ashechoes.controller.vo.bot;

import lombok.Data;

@Data
public class BotCallbackRequest {
    private Long op;
    private BotCallbackData d;
}