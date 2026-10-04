package com.musicplayer.api;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class NeteaseEncrypt {

    private static final String AES_KEY = "0CoJUm6Qyw8W8jud";
    private static final String AES_IV = "0102030405060708";
    private static final String RSA_PUBLIC_KEY_MODULUS =
            "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b7251" +
            "52b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ec" +
            "bda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d81" +
            "3cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
    private static final String RSA_PUBLIC_KEY_EXPONENT = "010001";

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomKey() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static String aesEncrypt(String data, String key) throws Exception {
        SecretKeySpec spec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        IvParameterSpec iv = new IvParameterSpec(AES_IV.getBytes("UTF-8"));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, spec, iv);
        byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String rsaEncrypt(String text) {
        try {
            // Build public key from modulus and exponent
            java.math.BigInteger modulus = new java.math.BigInteger(RSA_PUBLIC_KEY_MODULUS, 16);
            java.math.BigInteger exponent = new java.math.BigInteger(RSA_PUBLIC_KEY_EXPONENT, 16);
            java.security.spec.RSAPublicKeySpec keySpec = new java.security.spec.RSAPublicKeySpec(modulus, exponent);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            java.security.PublicKey pubKey = kf.generatePublic(keySpec);

            // Reverse the hex string to bytes
            byte[] inputBytes = hexToBytes(hexReverse(text));

            // Encrypt with PKCS1 padding (matches Node.js crypto.publicEncrypt)
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encrypted = cipher.doFinal(inputBytes);

            // Convert to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : encrypted) {
                sb.append(String.format("%02x", b & 0xff));
            }
            String hexResult = sb.toString();
            if (hexResult.length() >= 256) {
                return hexResult.substring(0, 256);
            }
            StringBuilder pad = new StringBuilder();
            while (pad.length() + hexResult.length() < 256) {
                pad.append("0");
            }
            pad.append(hexResult);
            return pad.toString();
        } catch (Exception e) {
            // Fallback to raw RSA
            BigInteger modulus = new BigInteger(RSA_PUBLIC_KEY_MODULUS, 16);
            BigInteger exponent = new BigInteger(RSA_PUBLIC_KEY_EXPONENT, 16);
            BigInteger data = new BigInteger(hexReverse(text), 16);
            BigInteger result = data.modPow(exponent, modulus);
            String hexResult = result.toString(16);
            if (hexResult.length() >= 256) {
                return hexResult.substring(0, 256);
            }
            StringBuilder sb = new StringBuilder();
            while (sb.length() + hexResult.length() < 256) {
                sb.append("0");
            }
            sb.append(hexResult);
            return sb.toString();
        }
    }

    private static String hexReverse(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = text.length() - 2; i >= 0; i -= 2) {
            sb.append(text.substring(i, i + 2));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    public static String md5(String data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(data.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static EncryptedData encrypt(String data) throws Exception {
        String secKey = generateRandomKey();
        String params1 = aesEncrypt(data, AES_KEY);
        String params = aesEncrypt(params1, secKey);
        String encSecKey = rsaEncrypt(secKey);
        return new EncryptedData(params, encSecKey);
    }

    public static class EncryptedData {
        private final String params;
        private final String encSecKey;

        public EncryptedData(String params, String encSecKey) {
            this.params = params;
            this.encSecKey = encSecKey;
        }

        public String getParams() {
            return params;
        }

        public String getEncSecKey() {
            return encSecKey;
        }
    }
}