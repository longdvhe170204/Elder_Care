package com.eldercare.backend.nutrition.internal.assignment.domain;

import com.eldercare.backend.nutrition.api.dto.Texture;
import java.util.Set;

/**
 * Hàm thuần {@code CanLyDoChuyenMon} (FR-013 (a)–(d), BR-M08-03, Q-145, Q-283; research M-05): bản gán mới so với bản
 * Hiệu lực hiện có, hoặc với chế độ ăn mặc định (kết cấu thường, không hạn chế riêng) khi chưa có, có cần lý do chuyên
 * môn khi Áp dụng không. Dấu "liên quan điều trị" lấy theo danh mục tại lúc lệnh (FR-002). Thứ tự kết cấu cố định từ mềm
 * tới cứng: xay nhuyễn → mềm → thường (12.5).
 */
public final class ProfessionalReasonRule {

    private ProfessionalReasonRule() {}

    /** Một phía của phép so: chế độ ăn, dấu liên quan điều trị, kết cấu, hạn chế riêng. */
    public record Side(long dietId, boolean treatmentRelated, Texture texture, Set<Long> restrictions) {}

    /** {@code current} rỗng khi chưa có bản Hiệu lực và viện chưa có chế độ ăn mặc định. */
    public static boolean required(Side current, Side proposed) {
        if (current == null) {
            return proposed.treatmentRelated();
        }
        if (current.dietId() != proposed.dietId()) {
            return proposed.treatmentRelated() || current.treatmentRelated();
        }
        if (!proposed.treatmentRelated()) {
            return false;
        }
        return hardness(proposed.texture()) > hardness(current.texture())
                || !proposed.restrictions().containsAll(current.restrictions());
    }

    private static int hardness(Texture texture) {
        return switch (texture) {
            case PUREED -> 0;
            case SOFT -> 1;
            case REGULAR -> 2;
        };
    }
}
