package info.qianqiu.ashechoes.dto.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("`user_auth`")
public class UserAuth {

    @TableId
    private Long authId;
    private Long userId;
    private Long authCode;
    private Byte used;
}
