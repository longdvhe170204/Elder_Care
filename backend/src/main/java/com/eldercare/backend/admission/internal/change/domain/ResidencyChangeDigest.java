package com.eldercare.backend.admission.internal.change.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Dấu SHA-256 phần thực chất của yêu cầu thay đổi lưu trú (Q-416, Q-417; R12, R18): loại thay đổi, giá trị mới (mức, loại
 * lưu trú, giường, lịch đến, dịch vụ, thời hạn) và ngày hiệu lực mong muốn. Ghi chú, lý do, tệp, xác nhận không thuộc
 * phần thực chất. Khóa đối tượng JSON được sắp, nên thứ tự trường không làm đổi dấu.
 */
public final class ResidencyChangeDigest {

    private ResidencyChangeDigest() {}

    public static String of(JsonNode content, LocalDate requestedEffectiveDate) {
        StringBuilder canonical = new StringBuilder();
        List<String> types = new ArrayList<>();
        content.path("changeTypes").forEach(t -> types.add(t.asString()));
        Collections.sort(types);
        canonical.append(String.join(",", types)).append('|');
        write(content.path("newValue"), canonical);
        canonical.append('|').append(requestedEffectiveDate);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void write(JsonNode node, StringBuilder out) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            out.append("null");
        } else if (node.isObject()) {
            List<String> keys = new ArrayList<>();
            for (Map.Entry<String, JsonNode> e : node.properties()) {
                keys.add(e.getKey());
            }
            Collections.sort(keys);
            out.append('{');
            for (String key : keys) {
                out.append(key).append(':');
                write(node.get(key), out);
                out.append(',');
            }
            out.append('}');
        } else if (node.isArray()) {
            out.append('[');
            node.forEach(item -> {
                write(item, out);
                out.append(',');
            });
            out.append(']');
        } else {
            out.append(node.toString());
        }
    }
}
