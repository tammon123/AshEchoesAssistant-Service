package info.qianqiu.ashechoes.dto.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import info.qianqiu.ashechoes.controller.vo.bot.*;
import info.qianqiu.ashechoes.dto.domain.UserAuth;
import info.qianqiu.ashechoes.dto.domain.UserBot;
import info.qianqiu.ashechoes.dto.mapper.UserBotMapper;
import info.qianqiu.ashechoes.utils.http.ReqUtils;
import info.qianqiu.ashechoes.utils.string.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 默认名称Service业务层处理
 *
 * @author TianYiLuo
 * @date 2024-08-30
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserBotService extends ServiceImpl<UserBotMapper, UserBot> {

    private final BotConfig botConfig;
    private final UserAuthService authService;
    private final UserAuthService userAuthService;

    public ResponseEntity<BotCallbackResponse> groupChat(BotCallbackRequest request) {

        log.warn("接受消息:{}", JSONObject.toJSONString(request));
        BotCallbackData d = request.getD();
        // 只接受群聊消息
        if (d.groupChat()) {
            String api = "v2/groups/" + d.getGroup_openid() + "/messages";

            if (!checkLogin(d, api)) {
                return null;
            }

            if (StringUtils.isNotEmpty(d.getContent()) && d.getContent().contains(" /绑定")) {
                commandLogin(d, api);
            }

        }
        return ResponseEntity.ok(new BotCallbackResponse(
                "",
                ""
        ));

    }

    private void commandLogin(BotCallbackData d, String api) {
        BotSendMsg.BotSendMsgBuilder builder = BotSendMsg.builder();
        builder.msg_id(d.getId())
                .msg_type(MsgTypeConstant.TXT);
        Long authCode = Long.parseLong(d.getContent().replaceAll("^\\D*(\\d+).*$", "$1"));
        List<UserAuth> list = authService.list(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getAuthCode, authCode)
                .eq(UserAuth::getUsed, 0));
        if (list.isEmpty()) {
            builder.content("\n亲爱的小监督~\n该授权码错误或已被使用~\n请前往小助手APP/网页端重新生成授权码~");
        } else {
            UserAuth first = list.getFirst();
            long count = count(new LambdaQueryWrapper<UserBot>()
                    .eq(UserBot::getGroupId, d.getGroup_openid())
                    .eq(UserBot::getMemberId, d.getUserOpenId()));
            if (count != 0) {
                update(new LambdaUpdateWrapper<UserBot>()
                        .set(UserBot::getUserId, first.getUserId())
                        .eq(UserBot::getGroupId, d.getGroup_openid())
                        .eq(UserBot::getMemberId, d.getUserOpenId())
                );
                builder.content("\n亲爱的小监督~\n您已成功更新小助手账号，欢迎使用~");
            } else {
                UserBot build =
                        UserBot.builder().groupId(d.getGroup_openid()).memberId(d.getUserOpenId())
                                .userId(first.getUserId())
                                .build();
                save(build);

                builder.content("\n亲爱的小监督~\n您已成功绑定小助手账号，欢迎使用~");
            }
            userAuthService.update(new LambdaUpdateWrapper<UserAuth>().set(UserAuth::getUsed, 1)
                    .eq(UserAuth::getAuthCode, authCode));
        }

        ReqUtils.botPost(botConfig.getSurl() + api, JSONObject.toJSONString(builder.build()));
    }

    private boolean checkLogin(BotCallbackData d, String api) {
        List<UserBot> list = list(new LambdaQueryWrapper<UserBot>()
                .eq(UserBot::getMemberId, d.getUserOpenId())
                .eq(UserBot::getGroupId, d.getGroup_openid()));

        if (list.isEmpty() && !d.getContent().contains(" /绑定")) {
            BotSendMsg.BotSendMsgBuilder builder = BotSendMsg.builder();
            builder.msg_id(d.getId())
                    .msg_type(MsgTypeConstant.TXT).content("\n亲爱的小监督~\n请先绑定小助手账号哦~");
            ReqUtils.botPost(botConfig.getSurl() + api, JSONObject.toJSONString(builder.build()));
            return false;
        }

        return true;
    }

}
