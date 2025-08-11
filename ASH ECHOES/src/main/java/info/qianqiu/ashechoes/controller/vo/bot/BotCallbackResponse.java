package info.qianqiu.ashechoes.controller.vo.bot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BotCallbackResponse {
    private String plain_token;
    private String signature;
}