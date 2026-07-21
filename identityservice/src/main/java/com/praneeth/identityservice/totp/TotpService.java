package com.praneeth.identityservice.totp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class TotpService {
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String issuer;

    public TotpService(@Value("${application.totp.issuer:ShortLink}") String issuer) {
        this.issuer = issuer;
    }

    public String generateSecret() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String buildOtpAuthUri(String email, String secret) {
        String label = issuer + ":" + email;
        return "otpauth://totp/" + encode(label)
                + "?secret=" + secret
                + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1"
                + "&digits=" + CODE_DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    public String buildQrCodeDataUri(String otpAuthUri) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpAuthUri, BarcodeFormat.QR_CODE, 220, 220);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate TOTP QR code", exception);
        }
    }

    public boolean verify(String secret, String submittedCode) {
        if (secret == null || submittedCode == null || !submittedCode.matches("^[0-9]{6}$")) {
            return false;
        }

        long currentWindow = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (long offset = -1; offset <= 1; offset++) {
            if (generateCode(secret, currentWindow + offset).equals(submittedCode)) {
                return true;
            }
        }
        return false;
    }

    private String generateCode(String secret, long counter) {
        try {
            byte[] key = base32Decode(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);

            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate TOTP code", exception);
        }
    }

    private String base32Encode(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 31));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 31));
        }
        return result.toString();
    }

    private byte[] base32Decode(String secret) {
        String normalized = secret.replace("=", "").replace(" ", "").toUpperCase();
        ByteBuffer buffer = ByteBuffer.allocate(normalized.length() * 5 / 8 + 1);
        int current = 0;
        int bits = 0;
        for (char character : normalized.toCharArray()) {
            int value = BASE32_ALPHABET.indexOf(character);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid base32 secret");
            }
            current = (current << 5) | value;
            bits += 5;
            if (bits >= 8) {
                buffer.put((byte) ((current >> (bits - 8)) & 0xff));
                bits -= 8;
            }
        }
        byte[] decoded = new byte[buffer.position()];
        buffer.flip();
        buffer.get(decoded);
        return decoded;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}