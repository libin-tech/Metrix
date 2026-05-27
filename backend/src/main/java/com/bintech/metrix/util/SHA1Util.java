package com.bintech.metrix.util;

import cn.hutool.crypto.digest.DigestUtil;

import java.util.Arrays;

public final class SHA1Util {

    private SHA1Util() {}

    public static String getSHA1(String token, String timestamp, String nonce) {
        return getSHA1(token, timestamp, nonce, null);
    }

    public static String getSHA1(String token, String timestamp, String nonce, String encrypt) {
        String[] arr = encrypt != null
                ? new String[]{token, timestamp, nonce, encrypt}
                : new String[]{token, timestamp, nonce};
        Arrays.sort(arr);
        StringBuilder sb = new StringBuilder();
        for (String s : arr) {
            sb.append(s);
        }
        return DigestUtil.sha1Hex(sb.toString());
    }

    public static boolean checkSignature(String token, String timestamp, String nonce, String signature) {
        String sha1 = getSHA1(token, timestamp, nonce);
        return sha1 != null && sha1.equals(signature);
    }

    public static boolean checkSignature(String token, String timestamp, String nonce, String encrypt, String msgSignature) {
        String sha1 = getSHA1(token, timestamp, nonce, encrypt);
        return sha1 != null && sha1.equals(msgSignature);
    }
}
