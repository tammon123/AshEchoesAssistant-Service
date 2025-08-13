package info.qianqiu.ashechoes.dto.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import info.qianqiu.ashechoes.controller.vo.bot.*;
import info.qianqiu.ashechoes.dto.domain.UserAuth;
import info.qianqiu.ashechoes.dto.domain.UserBot;
import info.qianqiu.ashechoes.dto.mapper.UserBotMapper;
import info.qianqiu.ashechoes.init.InitComputeData;
import info.qianqiu.ashechoes.utils.bot.BotScreen;
import info.qianqiu.ashechoes.utils.http.ReqUtils;
import info.qianqiu.ashechoes.utils.string.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

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
    private final BotScreen botScreen;
    private final InitComputeData init;

    public ResponseEntity<BotCallbackResponse> groupChat(BotCallbackRequest request) {

        log.warn("接受消息:{}", JSONObject.toJSONString(request));
        BotCallbackData d = request.getD();
        // 只接受群聊消息
        if (d.groupChat()) {
            String api = "v2/groups/" + d.getGroup_openid() + "/messages";
            String mediaApi = "v2/groups/" + d.getGroup_openid() + "/files";

            String key = d.getGroup_openid() + d.getUserOpenId() + d.getId();
            try {
                if (!init.reciveBotMsg(key)) {
                    log.error("当前消息{}，已处理", d.getId());
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!checkLogin(d, api)) {
                return null;
            }

            if (StringUtils.isNotEmpty(d.getContent()) && d.getContent().contains("绑定")) {
                commandLogin(d, api);
            } else if (StringUtils.isNotEmpty(d.getContent()) && (d.getContent().contains("卡池") ||
                    d.getContent().contains("总览"))) {
                commandPoolShow(d, api, mediaApi);
            }

        }
        return ResponseEntity.ok(new BotCallbackResponse(
                "",
                ""
        ));

    }

    private void commandPoolShow(BotCallbackData d, String msgApi, String mediaApi) {

        UserBot one = getOne(new LambdaQueryWrapper<UserBot>()
                .eq(UserBot::getGroupId, d.getGroup_openid())
                .eq(UserBot::getMemberId, d.getUserOpenId()));

        if (one == null) {
            BotSendMsg.BotSendMsgBuilder builder = BotSendMsg.builder();
            builder.content("\n亲爱的小监督~\n请先绑定小助手账号哦~");
            sendMsg(d, msgApi, builder);
            return;
        }

        if (d.getContent().contains("卡池") && d.getContent().contains("list")) {
            String result = "\n /卡池 0（定向共鸣）" +
                    "\n /卡池 1（定向潜航）" +
                    "\n /卡池 2（联合共鸣/阶梯卡池）" +
                    "\n /卡池 3（常态共鸣）" +
                    "\n /卡池 4（寻迹潜航/先觉潜航）" +
                    "\n /卡池 5（识海甄录）";
            BotSendMsg.BotSendMsgBuilder content = BotSendMsg.builder().content(result);
            sendMsg(d, msgApi, content);
            return;
        } else if (d.getContent().contains("总览") && d.getContent().contains("list")) {
            String result = "\n /总览 0（查看所有角色）" +
                    "\n /总览 1（查看所有烙痕）";
            BotSendMsg.BotSendMsgBuilder content = BotSendMsg.builder().content(result);
            sendMsg(d, msgApi, content);
            return;
        }

        String baseUrl = "https://bjhl.qianqiu.info";
        if (System.getenv("LOCAL_MACHINE") != null) {
            baseUrl = "https://bjhl.qianqiu.info";
        }
        String path = "/pages/chouka/chouka";
        String uid = one.getUserId().toString();
        String behavior = "";
        Long status = Long.parseLong(d.getContent().replaceAll("^\\D*(\\d+).*$", "$1"));
        if (d.getContent().contains("卡池")) {
            behavior = "pool=" + status;
        } else if (d.getContent().contains("总览")) {
            behavior = "total=" + status;
        }

        String query = STR."?uid=\{uid}&bot=1&\{behavior}";
        CompletableFuture<String> screen = botScreen.screen(baseUrl + path + query, uid, behavior);

        try {
            String result = screen.get();
            if (result.contains("qianqiu.info")) {
                BotMediaResponse botMediaResponse = genMediaInfo(mediaApi, result);
                BotSendMsg.BotSendMsgBuilder content = BotSendMsg.builder().content(" ").media(botMediaResponse);
                sendMedia(d, msgApi, content);
            } else {
                BotSendMsg.BotSendMsgBuilder content = BotSendMsg.builder().content(result);
                sendMsg(d, msgApi, content);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

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
            if (74185296336L != authCode) {
                userAuthService.update(new LambdaUpdateWrapper<UserAuth>().set(UserAuth::getUsed, 1)
                        .eq(UserAuth::getAuthCode, authCode));
            }
        }

        ReqUtils.botPost(botConfig.getSurl() + api, JSONObject.toJSONString(builder.build()));
    }

    private boolean checkLogin(BotCallbackData d, String api) {
        List<UserBot> list = list(new LambdaQueryWrapper<UserBot>()
                .eq(UserBot::getMemberId, d.getUserOpenId())
                .eq(UserBot::getGroupId, d.getGroup_openid()));

        if (list.isEmpty() && !d.getContent().contains(" /绑定")) {
            BotSendMsg.BotSendMsgBuilder builder = BotSendMsg.builder();
            builder.content("\n亲爱的小监督~\n请先绑定小助手账号哦~");
            sendMsg(d, api, builder);
            return false;
        }

        return true;
    }

    private void sendMsg(BotCallbackData d, String api, BotSendMsg.BotSendMsgBuilder msg) {
        msg.msg_id(d.getId())
                .msg_type(MsgTypeConstant.TXT);
        ReqUtils.botPost(botConfig.getSurl() + api, JSONObject.toJSONString(msg.build()));
    }

    private void sendMedia(BotCallbackData d, String api, BotSendMsg.BotSendMsgBuilder media) {
        media.msg_id(d.getId())
                .msg_type(MsgTypeConstant.MEDIA);
        ReqUtils.botPost(botConfig.getSurl() + api, JSONObject.toJSONString(media.build()));
    }

    private BotMediaResponse genMediaInfo(String api, String mediaUrl) {
        BotMediaRequest botMediaRequest = new BotMediaRequest();
        botMediaRequest.setUrl(mediaUrl);

        String result = ReqUtils.botPost(botConfig.getSurl() + api, JSONObject.toJSONString(botMediaRequest));

        return JSONObject.parseObject(result, BotMediaResponse.class);

    }

}
