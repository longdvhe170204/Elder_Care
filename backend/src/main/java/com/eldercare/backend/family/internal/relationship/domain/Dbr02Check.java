package com.eldercare.backend.family.internal.relationship.domain;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * DBR-02 ({@code KiemTraDbr02}; research F-05, FR-003): người cao tuổi Đang lưu trú, Tạm vắng, Điều trị tại bệnh viện có
 * đúng một người liên hệ chính và ít nhất một người đại diện trong các quan hệ Hiệu lực. Đang tiếp nhận được lưu khi chưa
 * đủ; điều kiện "đủ DBR-02" của Hoàn tất tiếp nhận dùng {@link #satisfied}.
 */
public final class Dbr02Check {

    public enum Violation {
        NO_PRIMARY_CONTACT,
        MORE_THAN_ONE_PRIMARY_CONTACT,
        NO_REPRESENTATIVE
    }

    /** Vai trò của một quan hệ Hiệu lực. */
    public record Roles(boolean representative, boolean primaryContact) {}

    private static final Set<ResidentStatus> CHECKED =
            Set.of(ResidentStatus.IN_RESIDENCE, ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.HOSPITALIZED);

    private Dbr02Check() {}

    public static List<Violation> violations(ResidentStatus status, List<Roles> active) {
        return CHECKED.contains(status) ? violations(active) : List.of();
    }

    public static boolean satisfied(List<Roles> active) {
        return violations(active).isEmpty();
    }

    private static List<Violation> violations(List<Roles> active) {
        long primaryContacts = active.stream().filter(Roles::primaryContact).count();
        List<Violation> found = new ArrayList<>();
        if (primaryContacts == 0) {
            found.add(Violation.NO_PRIMARY_CONTACT);
        } else if (primaryContacts > 1) {
            found.add(Violation.MORE_THAN_ONE_PRIMARY_CONTACT);
        }
        if (active.stream().noneMatch(Roles::representative)) {
            found.add(Violation.NO_REPRESENTATIVE);
        }
        return found;
    }
}
