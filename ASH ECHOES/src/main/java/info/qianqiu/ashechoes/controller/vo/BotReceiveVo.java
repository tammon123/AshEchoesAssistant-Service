package info.qianqiu.ashechoes.controller.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BotReceiveVo {

    private String id;
    private Long op;
    private BotPayload d;
    private Long s;
    private String t;

}
