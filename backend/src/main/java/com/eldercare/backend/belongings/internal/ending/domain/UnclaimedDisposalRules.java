package com.eldercare.backend.belongings.internal.ending.domain;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Kiểm nội dung đề nghị "Xử lý đồ không người nhận" ({@code M12.XU_LY_DO_KHONG_NGUOI_NHAN}; 013 T024, FR-027a, Q-152,
 * Q-157, Q-159, CFG-M12-03, CFG-M12-07; research K-09). Hàm thuần: trả danh sách mã vi phạm, rỗng là đạt.
 */
public final class UnclaimedDisposalRules {

    /** Hình thức xử lý (FR-027a). */
    public enum Method {
        SELL,
        DESTROY,
        TRANSFER_TO_AUTHORITY
    }

    /**
     * Một đồ trong đề nghị; {@code foundAt} là lúc Tìm thấy sau khi mục sau qua đời đã Hoàn thành (FR-026a), khi đó
     * CFG-M12-03 tính từ đó thay cho lúc hồ sơ sang trạng thái cuối.
     */
    public record DisposalItem(
            long id, BelongingStatus status, boolean cash, boolean valuable, boolean hasPhoto, Instant foundAt) {}

    /** Một lần liên hệ người đại diện hoặc người liên hệ chính (bằng chứng CFG-M12-07). */
    public record Contact(long familyMemberId, Instant at, String result) {}

    /**
     * @param finalStatusAt lúc hồ sơ sang trạng thái cuối
     * @param contactPeople người đại diện Hiệu lực và người liên hệ chính còn quan hệ
     * @param attempts số lần liên hệ tối thiểu mỗi người (CFG-M12-07)
     * @param gap khoảng cách tối thiểu giữa hai lần liên hệ (CFG-M12-02)
     */
    public record Check(
            Instant now,
            Duration threshold,
            Instant finalStatusAt,
            Method method,
            List<DisposalItem> items,
            boolean hasMinutes,
            List<Long> contactPeople,
            List<Contact> contacts,
            int attempts,
            Duration gap) {}

    private UnclaimedDisposalRules() {}

    public static List<String> violations(Check check) {
        List<String> violations = new ArrayList<>();
        if (check.items().isEmpty()) {
            violations.add("NO_BELONGINGS");
            return violations;
        }
        for (DisposalItem item : check.items()) {
            if (item.status() != BelongingStatus.HELD && item.status() != BelongingStatus.DAMAGED) {
                add(violations, "BELONGING_NOT_DISPOSABLE");
            }
            if (item.cash() && check.method() != Method.TRANSFER_TO_AUTHORITY) {
                add(violations, "CASH_ONLY_TRANSFER_TO_AUTHORITY");
            }
            if (item.valuable() && !item.hasPhoto()) {
                add(violations, "PHOTO_REQUIRED");
            }
            Instant since = item.foundAt() != null ? item.foundAt() : check.finalStatusAt();
            if (since == null || check.now().isBefore(since.plus(check.threshold()))) {
                add(violations, "DISPOSAL_TOO_EARLY");
            }
        }
        if (!check.hasMinutes()) {
            violations.add("MINUTES_REQUIRED");
        }
        if (!sufficientContacts(check)) {
            violations.add("CONTACT_EVIDENCE_INSUFFICIENT");
        }
        return violations;
    }

    /** Mỗi người đại diện, người liên hệ chính có đủ {@code attempts} lần có kết quả, hai lần liền nhau cách {@code gap}. */
    static boolean sufficientContacts(Check check) {
        if (check.contactPeople().isEmpty()) {
            return false;
        }
        Map<Long, List<Instant>> byPerson = new TreeMap<>(check.contacts().stream()
                .filter(c -> c.result() != null && !c.result().isBlank() && c.at() != null)
                .collect(Collectors.groupingBy(
                        Contact::familyMemberId, Collectors.mapping(Contact::at, Collectors.toList()))));
        for (long person : check.contactPeople()) {
            List<Instant> times = byPerson.getOrDefault(person, List.of()).stream()
                    .sorted(Comparator.naturalOrder())
                    .toList();
            if (!spaced(times, check.attempts(), check.gap())) {
                return false;
            }
        }
        return true;
    }

    /** Chọn tham lam {@code attempts} lần, mỗi lần cách lần đã chọn trước ít nhất {@code gap}. */
    private static boolean spaced(List<Instant> sorted, int attempts, Duration gap) {
        int chosen = 0;
        Instant last = null;
        for (Instant time : sorted) {
            if (last == null || !time.isBefore(last.plus(gap))) {
                chosen++;
                last = time;
            }
        }
        return chosen >= attempts;
    }

    private static void add(List<String> violations, String code) {
        if (!violations.contains(code)) {
            violations.add(code);
        }
    }
}
