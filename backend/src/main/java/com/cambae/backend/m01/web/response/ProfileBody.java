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
 * trú, nhu cầu, lưu ý chăm sóc, mục sức khỏe, nguyện vọng, phiếu nguyện vọng khi điều trị) để rỗng tới khi làm (T079 giới hạn {@code hasTreatmentWish} theo vai trò); {@code hiddenFields} do 002 điền
 * khi tuần tự hóa.
 */
@Schema(name = "Profile")
public record ProfileBody(
        Long id,
        String profileCode,
        PersonalInfoBody personalInfo,
        ResidentStatus status,
        Instant statusSince,
        String currentResidenceType,
        @FieldGroup(InfoGroup.CARE_LEVEL) String currentCareLevel,
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

    public static ProfileBody of(ProfileView view, Set<String> allowedCommands, boolean hasActiveConsent) {
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
                false,
                false,
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
