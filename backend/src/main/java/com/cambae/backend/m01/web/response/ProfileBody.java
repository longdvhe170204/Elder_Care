package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService.ProfileView;
import com.cambae.backend.m15.account.api.FieldGroup;
import com.cambae.backend.m15.account.api.VisibilityTarget;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Hồ sơ cho nhân viên (contracts/api-001.yaml, schema {@code Profile}). Các trường của user story sau (loại hình lưu
 * trú, nhu cầu, lưu ý chăm sóc, mục sức khỏe) để rỗng tới khi làm; {@code hasEndOfLifeWish} và
 * {@code hasTreatmentWish} là cờ có/không (FR-023e, FR-023m) nên không gắn nhóm; {@code hiddenFields} do 002 điền khi
 * tuần tự hóa.
 */
@Schema(name = "Profile")
public record ProfileBody(
        Long id,
        String profileCode,
        PersonalInfoBody personalInfo,
        ResidentStatus status,
        Instant statusSince,
        String currentResidenceType,

        @FieldGroup(InfoGroup.CARE_LEVEL) @Schema(allowableValues = {"BASIC", "REGULAR", "SPECIAL"})
        String currentCareLevel,

        List<String> careNeeds,
        String careNote,
        @FieldGroup(InfoGroup.HEALTH) List<Object> activeHealthItems,
        Boolean hasActiveConsent,
        Boolean hasEndOfLifeWish,
        Boolean hasTreatmentWish,
        Long previousProfileId,
        Set<String> allowedCommands,
        List<String> hiddenFields,
        @JsonIgnore @Schema(hidden = true) Target target)
        implements VisibilityTarget {

    /**
     * {@code hasTreatmentWish} là {@code null} khi người xem không có quyền với nội dung phiếu và không phải Hành chính
     * (Kế toán không có cờ, FR-023m).
     */
    public static ProfileBody of(
            ProfileView view,
            Set<String> allowedCommands,
            boolean hasActiveConsent,
            boolean hasEndOfLifeWish,
            Boolean hasTreatmentWish) {
        return new ProfileBody(
                view.id(),
                view.profileCode(),
                PersonalInfoBody.of(view.personalInfo(), view.id()),
                view.status(),
                view.statusSince(),
                null,
                view.currentCareLevel(),
                List.of(),
                null,
                List.of(),
                hasActiveConsent,
                hasEndOfLifeWish,
                hasTreatmentWish,
                view.previousProfileId(),
                allowedCommands,
                List.of(),
                ResidentProfileService.target(view.id()));
    }

    @Override
    @JsonIgnore
    public Target visibilityTarget() {
        return target;
    }
}
