package info.qianqiu.ashechoes.controller.vo.wx;

import lombok.Data;

@Data
public class WeChatMessage {
    private String toUserName;
    private String fromUserName;
    private String createTime;
    private String msgType;
    private String content;
    private String msgId;
    private String event;
    private String eventKey;
    private String picUrl;
    private String mediaId;
    private String format;
    private String recognition;
}
