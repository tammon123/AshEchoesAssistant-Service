package info.qianqiu.ashechoes.dto.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("`user_bot_search`")
public class UserBotSearch {

    @TableId(type = IdType.AUTO)
    private Long searchCacheId;
    @TableField("`key`")
    private String key;
}
