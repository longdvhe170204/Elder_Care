package com.eldercare.backend.family.internal.right.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Dấu "được tự về" của người cao tuổi bán trú ({@code dau_tu_ve}; data-model mục 2, 0.1; FR-013, FR-021, Q-281). Theo
 * R-15 là một dòng {@code simple_record} loại {@link #KIND} mỗi người cao tuổi ({@link #key}); chỉ đổi qua yêu cầu.
 */
public record SelfReturnFlag(boolean enabled, long requestId, Instant from) {

    public static final SharedKind<SelfReturnFlag> KIND = SharedKind.of("M10.SELF_RETURN_FLAG", SelfReturnFlag.class);

    public static String key(long residentId) {
        return "M10.SELF_RETURN:" + residentId;
    }
}
