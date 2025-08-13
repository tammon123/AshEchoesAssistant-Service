package info.qianqiu.ashechoes.controller.vo.bot;

import lombok.Data;

@Data
public class BotMediaRequest {

    private Integer file_type = 1;
    private String url;
    private boolean srv_send_msg = false;

}
