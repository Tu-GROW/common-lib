package com.iris.common.lib.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility for masking sensitive data in log output.
 *
 * Field-level masking operates by field name pattern (case-insensitive).
 * Value-level masking uses regex for JWT, email, and phone patterns.
 *
 * Usage:
 *   log.info("Response: {}", DataMasker.maskObject(body));
 *   log.info("Token: {}",   DataMasker.maskToken(token));
 *   log.info("Phone: {}",   DataMasker.maskPhone(phone));
 */
public final class DataMasker {

    private DataMasker() {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Fields whose entire value is replaced with ****. */
    private static final Set<String> FULLY_MASKED = Set.of(
            "otp", "password", "pin", "secret", "auth_token"
    );

    /** Substrings that, when found in a field name, trigger token masking. */
    private static final Set<String> TOKEN_KEYWORDS = Set.of(
            "token", "authorization", "apikey", "api_key", "account_sid", "service_sid"
    );

    private static final Pattern JWT_PATTERN =
            Pattern.compile("eyJ[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("\\+?[0-9]{9,15}");

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Serializes {@code obj} to JSON and masks sensitive fields.
     * Safe to use directly in log statements — never throws.
     */
    public static String maskObject(Object obj) {
        if (obj == null) return "null";
        try {
            return maskJson(MAPPER.writeValueAsString(obj));
        } catch (Exception e) {
            return "[unserializable]";
        }
    }

    /**
     * Masks sensitive fields in a JSON string by field name.
     * Falls back to {@link #maskRaw} if the input is not valid JSON.
     */
    public static String maskJson(String json) {
        if (json == null || json.isBlank()) return json;
        try {
            JsonNode root = MAPPER.readTree(json);
            maskNode(root);
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return maskRaw(json);
        }
    }

    /**
     * Applies regex-based masking to a plain string.
     * Detects and masks JWTs and email addresses in-place.
     */
    public static String maskRaw(String value) {
        if (value == null) return null;
        value = JWT_PATTERN.matcher(value).replaceAll(m -> maskToken(m.group()));
        value = EMAIL_PATTERN.matcher(value).replaceAll(m -> maskEmail(m.group()));
        return value;
    }

    /**
     * +254712345678 → +254****78
     * Keeps country-code prefix (3–4 chars) and last 2 digits.
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "****";
        int prefixLen = phone.startsWith("+") ? 4 : 3;
        if (phone.length() <= prefixLen + 2) return phone.substring(0, prefixLen) + "****";
        return phone.substring(0, prefixLen) + "****" + phone.substring(phone.length() - 2);
    }

    /**
     * elvis@example.com → e****@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****";
        int at = email.indexOf('@');
        return email.charAt(0) + "****" + email.substring(at);
    }

    /**
     * eyJhbGciOiJ... → eyJhbG...[masked]
     * Shows first 6 chars only.
     */
    public static String maskToken(String token) {
        if (token == null || token.length() < 6) return "****";
        return token.substring(0, 6) + "...[masked]";
    }

    /**
     * Elvis Mutende → E****
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) return "****";
        return name.charAt(0) + "****";
    }

    // -------------------------------------------------------------------------
    // Internal JSON tree traversal
    // -------------------------------------------------------------------------

    private static void maskNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;

            List<String> fields = new ArrayList<>();
            obj.fieldNames().forEachRemaining(fields::add);

            for (String field : fields) {
                JsonNode child = obj.get(field);
                String lower = field.toLowerCase();

                if (child.isTextual()) {
                    String val = child.textValue();
                    if (FULLY_MASKED.contains(lower)) {
                        obj.put(field, "****");
                    } else if (TOKEN_KEYWORDS.stream().anyMatch(lower::contains)) {
                        obj.put(field, maskToken(val));
                    } else if (lower.contains("phone") || lower.equals("msisdn") || lower.equals("to")) {
                        obj.put(field, maskPhone(val));
                    } else if (lower.contains("email")) {
                        obj.put(field, maskEmail(val));
                    } else if (lower.equals("name") || lower.endsWith("name")) {
                        obj.put(field, maskName(val));
                    } else if (JWT_PATTERN.matcher(val).find()) {
                        obj.put(field, maskToken(val));
                    }
                } else if (child.isObject() || child.isArray()) {
                    maskNode(child);
                }
            }
        } else if (node.isArray()) {
            node.forEach(DataMasker::maskNode);
        }
    }
}
