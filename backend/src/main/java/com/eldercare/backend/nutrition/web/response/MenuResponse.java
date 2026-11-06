package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.internal.menu.domain.Menu;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Thực đơn trong danh sách (api-011 {@code Menu}). */
@Schema(name = "Menu")
public record MenuResponse(Long id, LocalDate weekStart, MenuStatus status, Integer version) {

    public static MenuResponse of(Menu menu) {
        return new MenuResponse(menu.id(), menu.weekStart(), menu.status(), menu.version());
    }
}
