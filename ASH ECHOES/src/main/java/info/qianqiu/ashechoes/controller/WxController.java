package info.qianqiu.ashechoes.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import info.qianqiu.ashechoes.controller.vo.bot.BotCallbackData;
import info.qianqiu.ashechoes.controller.vo.bot.BotSendMsg;
import info.qianqiu.ashechoes.controller.vo.wx.WeChatMessage;
import info.qianqiu.ashechoes.controller.vo.wx.WxConfig;
import info.qianqiu.ashechoes.dto.domain.UserBot;
import info.qianqiu.ashechoes.dto.domain.UserBotSearch;
import info.qianqiu.ashechoes.dto.service.UserAuthService;
import info.qianqiu.ashechoes.dto.service.UserBotSearchService;
import info.qianqiu.ashechoes.dto.service.UserBotService;
import info.qianqiu.ashechoes.init.InitComputeData;
import info.qianqiu.ashechoes.utils.bot.BotScreen;
import info.qianqiu.ashechoes.utils.http.ReqUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * 废弃微信公众号接口
 */

@Deprecated
@RestController
@RequestMapping("/wx")
@Slf4j
@RequiredArgsConstructor
public class WxController {

    private final WxConfig wxConfig;
    private final UserBotService userBotService;
    private final UserAuthService authService;
    private final UserAuthService userAuthService;
    private final UserBotSearchService userBotSearchService;
    private final BotScreen botScreen;
    private final InitComputeData init;
    private static final String LOGIN_TEMPLATE_JPG =
            "http://mmbiz.qpic.cn/mmbiz_jpg/tlYmINXcib9wcOfONt1xJVjyWaOlFwsNiaNiaRYltSSd7oTT8x1LjiaPbxbbXxDKhb1mSEvUyfIOTxIv3qq86urQ1g/0?wx_fmt=jpeg";
    private static final String LOGIN_MEDIA_ID = "YUpvQXmy8ZEB8jz84jJUS_bM95kw9JV5FvoYtog8WZ30i4fL-JSQxZ4padEcMHhG";
    private static final String LOGIN_URL = "http://bjhl.qianqiu.info";

    private static final String SEARCH_URL = "http://s.qianqiu.info/s?k=";

    private static final HashSet<String> BOT_COMMAND = new HashSet<>();

    static {
        BOT_COMMAND.add("绑定");
        BOT_COMMAND.add("档案");
        BOT_COMMAND.add("卡池");
        BOT_COMMAND.add("全部抽取角色");
        BOT_COMMAND.add("全部抽取烙痕");
    }

    @GetMapping("/test")
    public String test() {
        ReqUtils.wxFormdataPost(
                wxConfig.getUrl() + "/cgi-bin/material/add_material?access_token=ACCCESS_TOKEN&type=image",
                new FileSystemResource("C:\\Users\\Nan\\Downloads\\236191027543744513_pool=0.jpg")
        );
        return "";
    }

    /**
     * 接收微信普通消息（POST请求）
     * 微信服务器会以XML格式推送消息到这里
     */
    @PostMapping(value = "/receive", produces = MediaType.APPLICATION_XML_VALUE)
    public String receiveMessage(@RequestBody String xmlBody) {
        try {
            // 解析XML消息
            WeChatMessage message = parseXmlMessage(xmlBody);

            // 根据消息类型进行不同处理
            String response = processMessage(message);

            return response;

        } catch (Exception e) {
            System.err.println("处理微信消息时出错: " + e.getMessage());
            e.printStackTrace();
            return "success"; // 返回success表示接收成功，即使处理出错
        }
    }

    /**
     * 处理文本消息 - 返回单图文消息
     */
    private String processMessage(WeChatMessage message) {
        String msgType = message.getMsgType();

        // 只接受图文消息
        if ("text".equals(msgType)) {
            String key = message.getMsgId() + message.getFromUserName();
            try {
                if (!init.reciveBotMsg(key)) {
                    log.info("当前消息{}，已处理", message.getMsgId());
                    return "success";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            UserBot userBot = null;

            if ("教程".equals(message.getContent())) {
                String s =
                        "APP导入教程：https://mp.weixin.qq.com/s/GkHZ3tHSvitb7JU2A2hXjA" + System.lineSeparator() +
                                System.lineSeparator() +
                                "网页端导入教程：https://mp.weixin.qq.com/s/GkHZ3tHSvitb7JU2A2hXjA" +
                                System.lineSeparator() +
                                System.lineSeparator() + "小助手链接：http://bjhl.qianqiu.info" +
                                System.lineSeparator() +
                                System.lineSeparator() + "APP下载链接：http://r.qianqiu.info/aes120.apk" +
                                System.lineSeparator() +
                                System.lineSeparator() + "QQ交流群：865686593" + System.lineSeparator();
                return buildTextResponse(message, s);
            }

            HashSet<String> ex = new HashSet<>();
            ex.add("绑定");
            ex.add("档案");
            boolean skip = false;
            for (String e : ex) {
                if (message.getContent().contains(e)) {
                    skip = true;
                    break;
                }
            }

            if (!skip) {
                userBot = checkLogin(message);
                if (userBot == null) {

                    return buildLoginResponse(message, "前往授权页",
                            "亲爱的小监督~" + System.lineSeparator() +
                                    "请使用绑定指令~" + System.lineSeparator(),
                            LOGIN_TEMPLATE_JPG, LOGIN_URL);
                }
            }
            if (message.getContent().contains("档案") ||
                    (!receiveHasBotCommand(message.getContent()) && !message.getContent().trim().isEmpty())) {
                String s = commandWiki(message.getContent());
                return buildTextResponse(message, s);
            } else if ((message.getContent().contains("全部抽取角色") ||
                    message.getContent().contains("全部抽取烙痕"))) {
//                commandAllShow(userBot, d, api, mediaApi);
            } else if (message.getContent().contains("绑定")) {
//                commandLogin(d, api, mediaApi);
            } else if ((message.getContent().contains("卡池"))) {
//                commandPoolShow(userBot, d, api, mediaApi);
            }
        }

        return buildTextResponse(message, "目前仅支持接收文本消息~");
    }

    private String commandWiki(String content) {
        String searchName = content.replaceAll("档案", "").replaceAll("/", "").trim();
        if (searchName.isEmpty()) {
            return "亲爱的小监督~" + System.lineSeparator() +
                    "请正确使用指令~" + System.lineSeparator() + "例：/档案 余音" + System.lineSeparator();
        }
        String[] name = new String[1];
        String[] image = new String[1];
        String[] sname = new String[1];
        String[] url = new String[1];

        userBotService.searchData(searchName, name, image, sname, url);
        if (name[0] == null) {
            return "亲爱的小监督~" + System.lineSeparator() +
                    "当前检索词未查询到对应档案~" + System.lineSeparator() + "请修改后尝试" + System.lineSeparator();
        }
        UserBotSearch one = userBotSearchService.getOne(
                new LambdaQueryWrapper<UserBotSearch>().eq(UserBotSearch::getKey, searchName));

        if (one == null) {
            one = new UserBotSearch(0L, searchName);
            userBotSearchService.save(one);
        }

        return
                name[0] + "\uD83D\uDDBC\uFE0F️" + System.lineSeparator() +
                        "检索名称\uD83D\uDD0D：" + sname[0] + System.lineSeparator()
                        + "WIKI档案\uD83D\uDCD6：" + SEARCH_URL + one.getSearchCacheId() + System.lineSeparator();
    }

    private boolean receiveHasBotCommand(String content) {
        boolean has = false;
        for (String key : BOT_COMMAND) {
            if (content.contains(key)) {
                return true;
            }
        }
        return has;
    }

    private UserBot checkLogin(WeChatMessage message) {
        List<UserBot> list = userBotService.list(new LambdaQueryWrapper<UserBot>()
                .eq(UserBot::getMemberId, message.getFromUserName())
                .eq(UserBot::getGroupId, "wx"));

        if (list.isEmpty()) {
            return null;
        }
        return list.getLast();
    }

    /**
     * 构建单图文消息回复
     */
    private String buildLoginResponse(WeChatMessage received, String title, String content, String picUrl,
                                      String aimUrl) {
        return "<xml>" +
                "<ToUserName><![CDATA[" + received.getFromUserName() + "]]></ToUserName>" +
                "<FromUserName><![CDATA[" + received.getToUserName() + "]]></FromUserName>" +
                "<CreateTime>" + (System.currentTimeMillis() / 1000) + "</CreateTime>" +
                "<MsgType><![CDATA[news]]></MsgType>" +
                "<ArticleCount>1</ArticleCount>" +
                "<Articles>" +
                "<item>" +
                "<Title><![CDATA[" + title + "]]></Title>" +
                "<Description><![CDATA[" + content + "]]></Description>" +
                "<PicUrl><![CDATA[" + picUrl + "]]></PicUrl>" +
                "<Url><![CDATA[" + aimUrl + "]]></Url>" +
                "</item>" +
                "</Articles>" +
                "</xml>";
    }

    /**
     * 构建文本消息回复
     */
    private String buildTextResponse(WeChatMessage received, String content) {
        return "<xml>" +
                "<ToUserName><![CDATA[" + received.getFromUserName() + "]]></ToUserName>" +
                "<FromUserName><![CDATA[" + received.getToUserName() + "]]></FromUserName>" +
                "<CreateTime>" + (System.currentTimeMillis() / 1000) + "</CreateTime>" +
                "<MsgType><![CDATA[text]]></MsgType>" +
                "<Content><![CDATA[" + content + "]]></Content>" +
                "</xml>";
    }

    /**
     * 解析XML消息内容
     */
    private WeChatMessage parseXmlMessage(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // 使用StringReader将字符串转换为InputStream
        InputStream inputStream = new java.io.ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8));

        Document document = builder.parse(inputStream);
        Element root = document.getDocumentElement();

        WeChatMessage message = new WeChatMessage();
        message.setToUserName(getElementText(root, "ToUserName"));
        message.setFromUserName(getElementText(root, "FromUserName"));
        message.setCreateTime(getElementText(root, "CreateTime"));
        message.setMsgType(getElementText(root, "MsgType"));
        message.setContent(getElementText(root, "Content"));
        message.setMsgId(getElementText(root, "MsgId"));

        // 其他可能的消息字段
        message.setEvent(getElementText(root, "Event"));
        message.setEventKey(getElementText(root, "EventKey"));
        message.setPicUrl(getElementText(root, "PicUrl"));
        message.setMediaId(getElementText(root, "MediaId"));
        message.setFormat(getElementText(root, "Format"));
        message.setRecognition(getElementText(root, "Recognition"));

        return message;
    }

    /**
     * 获取XML元素的文本内容
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    /**
     * 处理微信服务器发送的GET请求，用于验证服务器地址的有效性。
     *
     * @param signature 微信加密签名，signature结合了开发者填写的token参数和请求中的timestamp参数、nonce参数。
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param echostr   随机字符串
     * @return 如果验证成功，返回echostr；否则返回"invalid"
     */
    @GetMapping("/receive")
    public String auth(
            @RequestParam(name = "signature", required = false) String signature,
            @RequestParam(name = "timestamp", required = false) String timestamp,
            @RequestParam(name = "nonce", required = false) String nonce,
            @RequestParam(name = "echostr", required = false) String echostr) {

        log.info("接收到微信服务器认证请求: signature={}, timestamp={}, nonce={}, echostr={}",
                signature, timestamp, nonce, echostr);

        // 1. 校验参数是否为空（微信首次验证时会传这些参数）
        if (signature == null || timestamp == null || nonce == null || echostr == null) {
            log.warn("请求参数不完整，验证失败");
            return "invalid";
        }

        // 2. 将token、timestamp、nonce三个参数进行字典序排序
        String[] arr = new String[]{wxConfig.getToken(), timestamp, nonce};
        Arrays.sort(arr);

        // 3. 将三个参数字符串拼接成一个字符串进行sha1加密
        StringBuilder sb = new StringBuilder();
        for (String s : arr) {
            sb.append(s);
        }
        String combinedStr = sb.toString();

        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(combinedStr.getBytes());
            String calculatedSignature = bytesToHex(digest);

            // 4. 开发者获得加密后的字符串可与signature对比，标识该请求来源于微信
            if (calculatedSignature.equals(signature)) {
                log.info("微信服务器验证成功");
                return echostr; // 验证成功，返回echostr给微信服务器
            } else {
                log.warn("签名验证失败: 计算出的签名={}, 微信传来的签名={}", calculatedSignature, signature);
                return "invalid";
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-1算法不支持", e);
            return "error";
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
