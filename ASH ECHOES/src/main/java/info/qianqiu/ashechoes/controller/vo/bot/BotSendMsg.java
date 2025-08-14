package info.qianqiu.ashechoes.controller.vo.bot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotSendMsg {

    private String content = " ";
    private Integer msg_type = 0;
    private String msg_id;
    private String event_id;
    private Integer msg_seq = 1;
    private BotMediaResponse media;

}
