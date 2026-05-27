package com.bintech.metrix.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
public final class WechatCryptoUtil {

    private static final int RANDOM_BYTES_LENGTH = 16;

    private WechatCryptoUtil() {}

    public static byte[] decodeAesKey(String encodingAesKey) {
        String key = encodingAesKey.length() == 43 ? encodingAesKey + "=" : encodingAesKey;
        return Base64.getDecoder().decode(key);
    }

    public static String decrypt(String encryptedData, String encodingAesKey, String appId) {
        try {
            byte[] aesKey = decodeAesKey(encodingAesKey);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            int pad = decryptedBytes[decryptedBytes.length - 1] & 0xFF;
            if (pad < 1 || pad > 32) {
                pad = 0;
            }
            byte[] plainBytes = Arrays.copyOfRange(decryptedBytes, 0, decryptedBytes.length - pad);

            int xmlLength = ByteBuffer.wrap(plainBytes, RANDOM_BYTES_LENGTH, 4).getInt();
            String xmlContent = new String(plainBytes, RANDOM_BYTES_LENGTH + 4, xmlLength, StandardCharsets.UTF_8);

            String decryptAppId = new String(plainBytes, RANDOM_BYTES_LENGTH + 4 + xmlLength,
                    plainBytes.length - RANDOM_BYTES_LENGTH - 4 - xmlLength, StandardCharsets.UTF_8);

            if (!appId.equals(decryptAppId)) {
                log.warn("微信消息解密后appId不匹配: expected={}, actual={}", appId, decryptAppId);
            }

            return xmlContent;
        } catch (Exception e) {
            log.error("微信消息解密失败", e);
            throw new RuntimeException("微信消息解密失败", e);
        }
    }

    public static String encrypt(String plainXml, String encodingAesKey, String appId) {
        try {
            byte[] aesKey = decodeAesKey(encodingAesKey);
            byte[] xmlBytes = plainXml.getBytes(StandardCharsets.UTF_8);
            byte[] appIdBytes = appId.getBytes(StandardCharsets.UTF_8);

            byte[] randomBytes = new byte[RANDOM_BYTES_LENGTH];
            new SecureRandom().nextBytes(randomBytes);

            ByteBuffer buffer = ByteBuffer.allocate(RANDOM_BYTES_LENGTH + 4 + xmlBytes.length + appIdBytes.length);
            buffer.put(randomBytes);
            buffer.putInt(xmlBytes.length);
            buffer.put(xmlBytes);
            buffer.put(appIdBytes);

            byte[] plainBytes = buffer.array();
            int blockSize = 32;
            int amountToPad = blockSize - (plainBytes.length % blockSize);
            if (amountToPad == 0) {
                amountToPad = blockSize;
            }
            byte pad = (byte) (amountToPad & 0xFF);
            byte[] paddedBytes = Arrays.copyOf(plainBytes, plainBytes.length + amountToPad);
            for (int i = plainBytes.length; i < paddedBytes.length; i++) {
                paddedBytes[i] = pad;
            }

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encryptedBytes = cipher.doFinal(paddedBytes);
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("微信消息加密失败", e);
            throw new RuntimeException("微信消息加密失败", e);
        }
    }

    public static String generateSignature(String token, String timestamp, String nonce, String encrypt) {
        return SHA1Util.getSHA1(token, timestamp, nonce, encrypt);
    }
}
