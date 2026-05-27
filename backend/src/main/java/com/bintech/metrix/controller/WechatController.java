package com.bintech.metrix.controller;

import cn.hutool.core.util.RandomUtil;
import com.bintech.metrix.config.WechatConfig;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.service.impl.WechatAuthServiceImpl;
import com.bintech.metrix.util.MessageUtil;
import com.bintech.metrix.util.SHA1Util;
import com.bintech.metrix.util.WechatCryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/wechat")
@RequiredArgsConstructor
public class WechatController {

    private final WechatConfig wechatConfig;

    @GetMapping("/callback")
    public String verifyCallback(
            @RequestParam(name = "signature", required = false) String signature,
            @RequestParam(name = "timestamp", required = false) String timestamp,
            @RequestParam(name = "nonce", required = false) String nonce,
            @RequestParam(name = "echostr", required = false) String echostr) {
        if (signature == null || timestamp == null || nonce == null || echostr == null) {
            return "fail";
        }
        String sha1 = SHA1Util.getSHA1(wechatConfig.getToken(), timestamp, nonce);
        if (sha1 != null && sha1.equals(signature)) {
            log.info("微信服务器验证通过");
            return echostr;
        }
        return "fail";
    }

    @PostMapping(value = "/callback", produces = "application/xml;charset=UTF-8")
    public String handleMessage(
            @RequestBody String requestBody,
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam(name = "msg_signature", required = false) String msgSignature,
            @RequestParam(name = "encrypt_type", required = false) String encryptType) {
        Map<String, String> msgMap = MessageUtil.parseXml(requestBody);
        String encrypt = msgMap.get("Encrypt");
        boolean isSafeMode = "aes".equals(encryptType);

        if (isSafeMode) {
            // 安全模式（安全模式）：URL 有 msg_signature，body 仅有 <Encrypt>
            if (msgSignature == null) {
                log.warn("安全模式缺少 msg_signature 参数");
                return "";
            }
            if (!SHA1Util.checkSignature(wechatConfig.getToken(), timestamp, nonce, encrypt, msgSignature)) {
                log.warn("安全模式消息签名验证失败");
                return "";
            }
            if (encrypt == null) {
                log.warn("安全模式缺少 Encrypt 字段");
                return "";
            }

            String decryptedXml = WechatCryptoUtil.decrypt(encrypt, wechatConfig.getEncodingAesKey(), wechatConfig.getAppId());
            msgMap = MessageUtil.parseXml(decryptedXml);
            String result = processMessage(msgMap, msgMap.get("ToUserName"));

            String encryptedResult = WechatCryptoUtil.encrypt(result, wechatConfig.getEncodingAesKey(), wechatConfig.getAppId());
            String respTimestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String respNonce = String.valueOf(RandomUtil.randomInt(100000, 999999));
            String respMsgSignature = SHA1Util.getSHA1(wechatConfig.getToken(), respTimestamp, respNonce, encryptedResult);

            return "<xml>" +
                    "<Encrypt><![CDATA[" + encryptedResult + "]]></Encrypt>" +
                    "<MsgSignature><![CDATA[" + respMsgSignature + "]]></MsgSignature>" +
                    "<TimeStamp>" + respTimestamp + "</TimeStamp>" +
                    "<Nonce><![CDATA[" + respNonce + "]]></Nonce>" +
                    "</xml>";
        }

        // 兼容模式或明文模式：使用标准 signature 验证
        if (!SHA1Util.checkSignature(wechatConfig.getToken(), timestamp, nonce, signature)) {
            log.warn("微信消息签名验证失败");
            return "";
        }

        // 兼容模式下 body 已有完整消息字段，可直接使用
        return processMessage(msgMap, msgMap.get("ToUserName"));
    }

    private String processMessage(Map<String, String> msgMap, String toUser) {
        String fromUser = msgMap.get("FromUserName");
        String content = msgMap.get("Content");
        String msgType = msgMap.get("MsgType");

        if ("text".equals(msgType) && BusinessConstants.WECHAT_LOGIN_TRIGGER_KEYWORD.equals(content)) {
            String code = RandomUtil.randomNumbers(BusinessConstants.LOGIN_CODE_LENGTH);
            WechatAuthServiceImpl.LOGIN_CACHE.put(code, fromUser);
            log.info("微信验证码已生成: openid={}, code={}", fromUser, code);

            return MessageUtil.textMessageToXml(fromUser, toUser,
                    "您的验证码是：" + code + "\n有效期5分钟，请勿泄露。");
        }

        if ("event".equals(msgType) && "subscribe".equals(msgMap.get("Event"))) {
            return MessageUtil.textMessageToXml(fromUser, toUser,
                    "感谢关注！回复「验证码」获取登录验证码。");
        }

        return MessageUtil.textMessageToXml(fromUser, toUser,
                "回复「验证码」获取登录验证码。");
    }
}
