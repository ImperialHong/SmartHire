package com.smarthire.modules.notification.messaging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class NotificationEventKeyFactory {

    private NotificationEventKeyFactory() {
    }

    public static String generate(
        Long recipientUserId,
        String type,
        String title,
        String content,
        String relatedType,
        Long relatedId
    ) {
        String rawValue = String.join(
            "|",
            String.valueOf(recipientUserId),
            safe(type),
            safe(title),
            safe(content),
            safe(relatedType),
            String.valueOf(relatedId)
        );
        return sha256Hex(rawValue);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String sha256Hex(String rawValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(Character.forDigit((current >> 4) & 0xF, 16));
                builder.append(Character.forDigit(current & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
