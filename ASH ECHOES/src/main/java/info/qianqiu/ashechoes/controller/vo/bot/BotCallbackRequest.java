package info.qianqiu.ashechoes.controller.vo.bot;

import lombok.Data;

@Data
public class BotCallbackRequest {
    private Long op;
    private BotCallbackData d;
    private String id;
    private Long s;
    private String t;
}