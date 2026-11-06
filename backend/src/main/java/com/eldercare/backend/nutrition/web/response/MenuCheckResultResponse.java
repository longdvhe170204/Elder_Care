package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.internal.menu.service.MenuService.CheckView;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Kết quả kiểm tra thực đơn (api-011 {@code MenuCheckResult}). Lệnh trên món trả kết quả kiểm tra lại thực đơn chưa hết
 * tuần sau khi xác nhận bản sửa (FR-004). Cảnh báo dị ứng cho bếp chỉ có số người ({@code KitchenMenuWarning}); tên người
 * chỉ có khi người gọi được xem dị ứng (FR-025 c, SC-014).
 */
@Schema(name = "MenuCheckResult")
public record MenuCheckResultResponse(
        List<CoverageGap> coverageGaps,
        List<Object> dishRepetitions,

        @Schema(description = "Bếp: residentCount; Bác sĩ: kèm tên người")
        List<Object> allergies,

        boolean professionalReasonRequired,
        List<String> warningCodes) {

    @Schema(name = "MenuCoverageGap")
    public record CoverageGap(Long dietId, LocalDate date, Long mealId) {}

    /** Lặp món (FR-026): chế độ ăn, món, chuỗi ngày. */
    public record Repetition(long dietId, long dishId, LocalDate from, LocalDate to, String code) {}

    /** Cảnh báo dị ứng; {@code residents} vắng mặt với người không có quyền xem dị ứng. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Allergy(
            LocalDate date, long mealId, long dietId, long dishId, int residentCount, List<Resident> residents) {}

    public record Resident(long id, String name) {}

    public static MenuCheckResultResponse empty() {
        return new MenuCheckResultResponse(List.of(), List.of(), List.of(), false, List.of());
    }

    public static MenuCheckResultResponse of(CheckView view) {
        return new MenuCheckResultResponse(
                view.coverageGaps().stream()
                        .map(g -> new CoverageGap(g.dietId(), g.date(), g.mealId()))
                        .toList(),
                view.repetitions().stream()
                        .<Object>map(r -> new Repetition(r.dietId(), r.dishId(), r.from(), r.to(), r.code()))
                        .toList(),
                view.allergies().stream()
                        .<Object>map(a -> new Allergy(
                                a.date(),
                                a.mealId(),
                                a.dietId(),
                                a.dishId(),
                                a.residentCount(),
                                a.residents() == null
                                        ? null
                                        : a.residents().stream()
                                                .map(r -> new Resident(r.id(), r.name()))
                                                .toList()))
                        .toList(),
                false,
                view.codes());
    }
}
