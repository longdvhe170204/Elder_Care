package com.eldercare.backend.belongings.internal.returns.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Đủ bằng chứng liên hệ ({@code DuBangChungLienHe}; 013 T018, FR-017a (a), CFG-M12-06, Q-157): với mỗi người đại diện
 * Hiệu lực có ít nhất {@code attemptsPerRepresentative} lần liên hệ qua ít nhất {@code channels} kênh khác nhau, mỗi
 * lần có kết quả. Không có người đại diện Hiệu lực thì căn cứ (a) không áp dụng mà dùng căn cứ (b).
 */
public final class ContactEvidence {

    private ContactEvidence() {}

    /** Một lần liên hệ. */
    public record Attempt(long familyMemberId, ContactChannel channel, String result) {}

    public static boolean sufficient(
            List<Long> effectiveRepresentatives, List<Attempt> attempts, int attemptsPerRepresentative, int channels) {
        if (effectiveRepresentatives.isEmpty()) {
            return false;
        }
        for (long representative : effectiveRepresentatives) {
            List<Attempt> own = attempts.stream()
                    .filter(a -> a.familyMemberId() == representative
                            && a.result() != null
                            && !a.result().isBlank())
                    .toList();
            Set<ContactChannel> used = new HashSet<>();
            own.forEach(a -> used.add(a.channel()));
            if (own.size() < attemptsPerRepresentative || used.size() < channels) {
                return false;
            }
        }
        return true;
    }
}
