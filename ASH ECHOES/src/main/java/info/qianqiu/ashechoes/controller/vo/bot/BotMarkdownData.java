package info.qianqiu.ashechoes.controller.vo.bot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotMarkdownData {

    private String key;
    private String[] values;

}
