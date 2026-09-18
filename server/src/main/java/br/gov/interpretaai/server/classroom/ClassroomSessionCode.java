package br.gov.interpretaai.server.classroom;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class ClassroomSessionCode {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private final byte[] secret;

    ClassroomSessionCode(@Value("${interpretaai.device-pairing.secret:disabled}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    String generate() {
        char[] value = new char[8];
        for (int index = 0; index < value.length; index++) {
            value[index] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(value);
    }

    String normalize(String value) {
        if (value == null) return "";
        return value.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }

    String format(String value) { return value.substring(0, 4) + "-" + value.substring(4); }

    String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
