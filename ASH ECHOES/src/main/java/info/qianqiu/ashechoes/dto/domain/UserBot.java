package info.qianqiu.ashechoes.dto.domain;

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
@TableName("`user_bot`")
public class UserBot {

    @TableId
    private Long botUid;
    private Long userId;
    private String memberId;
    private String groupId;
}
