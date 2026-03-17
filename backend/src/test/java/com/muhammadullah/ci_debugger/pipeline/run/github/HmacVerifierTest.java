package com.muhammadullah.ci_debugger.pipeline.run.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HmacVerifierTest {

    private static final String SECRET = "test-secret";
    private static final String ALGORITHM = "HmacSHA256";
    private static final byte[] PAYLOAD = "{\"action\":\"completed\"}".getBytes(StandardCharsets.UTF_8);

    private HmacVerifier hmacVerifier;

    @BeforeEach
    void setUp() {
        hmacVerifier = new HmacVerifier(SECRET);
    }

    @Test
    void verify_validSignature_returnsTrue() throws Exception {
        String signature = computeSignature(SECRET, PAYLOAD);
        assertTrue(hmacVerifier.verify(PAYLOAD, signature));
    }

    @Test
    void verify_wrongSignature_returnsFalse() {
        String signature = "sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        assertFalse(hmacVerifier.verify(PAYLOAD, signature));
    }

    @Test
    void verify_wrongSecret_returnsFalse() throws Exception {
        String signature = computeSignature("wrong-secret", PAYLOAD);
        assertFalse(hmacVerifier.verify(PAYLOAD, signature));
    }

    @Test
    void verify_nullSignatureHeader_returnsFalse() {
        assertFalse(hmacVerifier.verify(PAYLOAD, null));
    }

    @Test
    void verify_missingPrefix_returnsFalse() throws Exception {
        String signatureWithoutPrefix = computeSignature(SECRET, PAYLOAD).substring("sha256=".length());
        assertFalse(hmacVerifier.verify(PAYLOAD, signatureWithoutPrefix));
    }

    @Test
    void verify_tamperedPayload_returnsFalse() throws Exception {
        String signature = computeSignature(SECRET, PAYLOAD);
        byte[] tamperedPayload = "{\"action\":\"requested\"}".getBytes(StandardCharsets.UTF_8);
        assertFalse(hmacVerifier.verify(tamperedPayload, signature));
    }

    //** --- helpers ---

    private String computeSignature(String secret, byte[] payload) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
        byte[] hash = mac.doFinal(payload);
        return "sha256=" + bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}