package com.eldercare.backend.health.internal.threshold.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Kiểm phiên bản ngưỡng theo FR-021 (research S-07 {@code KiemTraNguong}): thành phần thuộc chỉ số, mỗi cận có giá trị
 * nằm trong khoảng vật lý CFG-M06-03, thứ tự nghiêm ngặt dưới nguy hiểm < dưới cảnh báo < trên cảnh báo < trên nguy
 * hiểm, bỏ qua cận trống.
 */
public final class ThresholdRules {
    private ThresholdRules() {}

    /** Danh sách vi phạm; rỗng là hợp lệ. {@code range} là mục của chỉ số trong CFG-M06-03 ({@code {COMP:{min,max}}}). */
    public static List<String> violations(
            Map<String, ThresholdBand> components, Set<String> componentCodes, JsonNode range) {
        var out = new ArrayList<String>();
        if (components == null || components.isEmpty()) {
            out.add("Phải có ít nhất một thành phần");
            return out;
        }
        components.forEach((code, band) -> {
            if (!componentCodes.contains(code)) {
                out.add(code + ": không thuộc chỉ số");
                return;
            }
            var bounds = new BigDecimal[] {
                band.lowerDangerous(), band.lowerWarning(), band.upperWarning(), band.upperDangerous()
            };
            var names = new String[] {"dangerBelow", "warningBelow", "warningAbove", "dangerAbove"};
            JsonNode limits = range == null ? null : range.get(code);
            for (int i = 0; i < bounds.length; i++) {
                if (bounds[i] == null || limits == null) continue;
                var min = limits.get("min").decimalValue();
                var max = limits.get("max").decimalValue();
                if (bounds[i].compareTo(min) < 0 || bounds[i].compareTo(max) > 0) {
                    out.add(code + "." + names[i] + ": ngoài khoảng hợp lệ " + min + " – " + max);
                }
            }
            BigDecimal previous = null;
            String previousName = null;
            for (int i = 0; i < bounds.length; i++) {
                if (bounds[i] == null) continue;
                if (previous != null && previous.compareTo(bounds[i]) >= 0) {
                    out.add(code + ": " + previousName + " phải nhỏ hơn " + names[i]);
                }
                previous = bounds[i];
                previousName = names[i];
            }
        });
        return out;
    }
}
