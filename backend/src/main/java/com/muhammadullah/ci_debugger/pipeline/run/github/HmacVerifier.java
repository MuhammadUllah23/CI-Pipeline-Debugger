package com.muhammadullah.ci_debugger.pipeline.run.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class HmacVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final String secret;

    public HmacVerifier(@Value("${github.webhook.secret}") String secret) {
        this.secret = secret;
    }

    public boolean verify(byte[] payload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        String expectedHex = signatureHeader.substring(SIGNATURE_PREFIX.length());

        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(), ALGORITHM));
            byte[] computed = mac.doFinal(payload);
            byte[] expected = hexToBytes(expectedHex);
            return MessageDigest.isEqual(computed, expected);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}