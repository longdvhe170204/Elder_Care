package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.internal.menu.domain.Menu;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/** Thực đơn trong danh sách (api-011 {@code Menu}; FR-029): người gửi, thời điểm gửi để Quản lý viện duyệt. */
@Schema(name = "Menu")
public record MenuResponse(
        Long id,
        LocalDate weekStart,
        MenuStatus status,
        Instant submittedAt,
        @Schema(description = "Tên người gửi duyệt") String submittedBy,
        Instant publishedAt,
        String returnReason,
        Integer version) {

    public static MenuResponse of(Menu menu, String submitterName) {
        return new MenuResponse(
                menu.id(),
                menu.weekStart(),
                menu.status(),
                menu.submittedAt(),
                submitterName,
                menu.publishedAt(),
                menu.returnReason(),
                menu.version());
    }
}
