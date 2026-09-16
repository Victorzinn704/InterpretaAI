package br.gov.interpretaai.server.device;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class DeviceCredentialCodec {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final byte[] secret;

    DeviceCredentialCodec(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    String pairingCode() {
        char[] alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
        char[] code = new char[8];
        for (int index = 0; index < code.length; index++) {
            code[index] = alphabet[RANDOM.nextInt(alphabet.length)];
        }
        return new String(code);
    }

    String deviceSecret() {
        byte[] value = new byte[32];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    boolean matches(String value, String expectedHash) {
        return MessageDigest.isEqual(
                hash(value).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII));
    }
}
