package info.qianqiu.ashechoes.controller.vo.bot;

import lombok.Data;

@Data
public class BotAttachment {

    private String content_type;
    private String filename;
    private String url;
    private Long height;
    private Long width;
    private Long size;

}
