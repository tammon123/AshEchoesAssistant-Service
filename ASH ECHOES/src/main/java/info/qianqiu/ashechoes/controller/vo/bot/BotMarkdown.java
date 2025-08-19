package info.qianqiu.ashechoes.controller.vo.bot;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;

@Data
@Builder
public class BotMarkdown {

    private String custom_template_id;
    private ArrayList<BotMarkdownData> params;

}
