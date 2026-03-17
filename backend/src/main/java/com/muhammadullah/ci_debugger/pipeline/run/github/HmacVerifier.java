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

    /**
     * Verifies that a GitHub webhook payload has not been tampered with by
     * validating its HMAC-SHA256 signature against the configured webhook secret.
     *
     * GitHub computes the signature by applying HMAC-SHA256 to the raw request
     * body using the shared secret, then prefixes the result with {@code sha256=}.
     * This method recomputes the same signature locally and compares the two using
     * a constant-time equality check to prevent timing attacks.
     *
     * @param payload         the raw request body bytes exactly as received
     * @param signatureHeader the value of the {@code X-Hub-Signature-256} header,
     *                        expected in the format {@code sha256=<hex>}
     * @return {@code true} if the signature is valid, {@code false} if the header
     *         is missing, malformed, or does not match the computed signature
     */
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