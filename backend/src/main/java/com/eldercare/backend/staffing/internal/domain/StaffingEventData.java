package com.eldercare.backend.staffing.internal.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.staffing.internal.domain.StaffingCatalogData.Legacy;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Nội dung các dòng ghi nhận của 008, giữ loại logic dù đổi bảng lưu. */
public final class StaffingEventData {
    private StaffingEventData() {}

    public record Publication(@NotNull Map<String, Object> content, Legacy legacy) {}

    public record Comment(@NotNull String content, @NotNull String sender, Legacy legacy) {}

    public record Ratio(
            @NotNull Instant calculatedAt,

            @NotNull
            @Pattern(
                    regexp =
                            "DRAFT_SAVED|PUBLISHED|STAFF_ABSENT|STAFF_ADDED|EMPLOYMENT_ENDED|SHIFT_SWAP_015|CARE_LEVEL_CHANGED|ALLOCATION_CHANGED|RESIDENT_ABSENCE|FINAL_STATUS")
            String event,

            @NotNull @DecimalMin("0") BigDecimal totalWeight,
            @Min(0) int staffCount,
            @DecimalMin("0") BigDecimal ratio,
            @NotNull @Positive BigDecimal threshold,
            boolean meetsThreshold,
            Legacy legacy) {
        @AssertTrue(message = "Tỷ lệ phải rỗng khi không có nhân viên và có giá trị khi có nhân viên")
        @JsonIgnore
        public boolean isRatioConsistent() {
            return staffCount == 0 ? ratio == null : ratio != null;
        }
    }

    public static final SharedKind<Publication> PUBLICATION = SharedKind.of("M09.SHIFT_PUBLICATION", Publication.class);
    public static final SharedKind<Comment> COMMENT = SharedKind.of("M09.HANDOVER_COMMENT", Comment.class);
    public static final SharedKind<Comment> REPLY = SharedKind.of("M09.HANDOVER_REPLY", Comment.class);
    public static final SharedKind<Ratio> RATIO = SharedKind.of("M09.STAFFING_RATIO_CALCULATION", Ratio.class);
}
