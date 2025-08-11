package info.qianqiu.ashechoes.controller.vo.bot;

import info.qianqiu.ashechoes.utils.string.StringUtils;
import lombok.Data;

import java.util.ArrayList;

@Data
public class BotCallbackData {
    private String plain_token;
    private String event_ts;
    // 其他可能的字段...
    private BotFromAuthor author;
    private String id;
    private String group_openid;
    private String content;
    private String timestamp;
    private ArrayList<BotAttachment> attachments;

    public boolean groupChat() {
        return StringUtils.isNotEmpty(group_openid);
    }

    public String getUserOpenId() {
        if (groupChat()) {
            return author.getMember_openid();
        }
        return author.getUser_openid();
    }
}