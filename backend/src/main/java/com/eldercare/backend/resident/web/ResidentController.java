package com.eldercare.backend.resident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.resident.api.ConsentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService.CreateResult;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService.LinkedProfile;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService.ProfileView;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService.SearchPage;
import com.eldercare.backend.resident.internal.status.service.ResidentStatusService;
import com.eldercare.backend.resident.internal.wish.service.EndOfLifeWishService;
import com.eldercare.backend.resident.internal.wish.service.TreatmentWishService;
import com.eldercare.backend.resident.web.request.CreateResidentBody;
import com.eldercare.backend.resident.web.request.UpdatePersonalInfoBody;
import com.eldercare.backend.resident.web.response.CreateProfileResultBody;
import com.eldercare.backend.resident.web.response.ProfileBody;
import com.eldercare.backend.resident.web.response.ProfilePage;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hồ sơ người cao tuổi: tạo, tra cứu, xem, cập nhật thông tin cá nhân, chuỗi hồ sơ trước (001 T020;
 * contracts/api-001.yaml, tag {@code profiles}). Lệnh trạng thái của hồ sơ thuộc 004.
 */
@RestController
@RequestMapping("/api/v1/residents")
class ResidentController {

    private static final String PROBLEM = "application/problem+json";
    private static final String UPDATE_PERSONAL_INFO = "UPDATE_PERSONAL_INFO";
    // Lệnh sức khỏe (US2): ghi mục và hồ sơ ban đầu cho Bác sĩ, Điều dưỡng; xác nhận hồ sơ ban đầu chỉ Bác sĩ (lọc
    // thêm)
    private static final String CONFIRM_INITIAL_HEALTH_RECORD = "CONFIRM_INITIAL_HEALTH_RECORD";
    private static final PermissionKey WRITE_HEALTH = new PermissionKey(Function.ALLERGY_CONDITION, Operation.UPDATE);
    private static final Map<String, PermissionKey> PROFILE_COMMANDS = Map.ofEntries(
            Map.entry(UPDATE_PERSONAL_INFO, new PermissionKey(Function.RESIDENT_PROFILE, Operation.UPDATE)),
            Map.entry("RECORD_HEALTH_ITEM", new PermissionKey(Function.ALLERGY_CONDITION, Operation.CREATE)),
            Map.entry("EXCLUDE_HEALTH_ITEM", WRITE_HEALTH),
            Map.entry("WRITE_INITIAL_HEALTH_RECORD", WRITE_HEALTH),
            Map.entry(CONFIRM_INITIAL_HEALTH_RECORD, WRITE_HEALTH),
            // Đánh giá (US3): Bác sĩ, Điều dưỡng lập và sửa nháp; xác nhận chỉ Bác sĩ (quyền APPROVE, Phụ lục 27)
            Map.entry("CREATE_ASSESSMENT", new PermissionKey(Function.ASSESSMENT_CARE_LEVEL, Operation.CREATE)),
            Map.entry("CONFIRM_ASSESSMENT", new PermissionKey(Function.ASSESSMENT_CARE_LEVEL, Operation.APPROVE)),
            // Bản đồng ý (US5): Hành chính ghi nhận và rút lại (AC-09, Phụ lục 27)
            Map.entry("RECORD_CONSENT", new PermissionKey(Function.DATA_SHARING_CONSENT, Operation.CREATE)),
            Map.entry("WITHDRAW_CONSENT", new PermissionKey(Function.DATA_SHARING_CONSENT, Operation.UPDATE)),
            // Phiếu nguyện vọng (US7): Bác sĩ, Điều dưỡng ghi phiên bản mới (Phụ lục 27)
            Map.entry("RECORD_END_OF_LIFE_WISH", new PermissionKey(Function.END_OF_LIFE_WISHES, Operation.CREATE)),
            Map.entry("RECORD_TREATMENT_WISH", new PermissionKey(Function.ALLERGY_CONDITION, Operation.CREATE)));

    // Lệnh trạng thái (US4): khóa quyền theo data-model mục 2; hợp trạng thái hiện tại lọc thêm ở body()
    private static final Map<StatusCommand, PermissionKey> STATUS_COMMANDS = Map.of(
            StatusCommand.COMPLETE_ADMISSION,
            new PermissionKey(Function.ADMISSION_CONTRACT_DEPOSIT, Operation.CREATE),
            StatusCommand.CANCEL_ADMISSION,
            new PermissionKey(Function.ADMISSION_CONTRACT_DEPOSIT, Operation.CREATE),
            StatusCommand.GRANT_TEMPORARY_ABSENCE,
            new PermissionKey(Function.ABSENCE_RETURN, Operation.CREATE),
            StatusCommand.RECORD_RETURN,
            new PermissionKey(Function.ABSENCE_RETURN, Operation.CREATE),
            StatusCommand.TRANSFER_OUT,
            new PermissionKey(Function.TRANSFER_OUT, Operation.CREATE),
            StatusCommand.END_RESIDENCY,
            new PermissionKey(Function.RESIDENCY_END_DEATH, Operation.CREATE),
            StatusCommand.RECORD_DEATH,
            new PermissionKey(Function.RESIDENCY_END_DEATH, Operation.CREATE));

    private static final Map<String, PermissionKey> COMMANDS = allCommands();
    private static final PermissionKey SEE_TREATMENT_WISH =
            new PermissionKey(Function.ALLERGY_CONDITION, Operation.VIEW);
    private static final PermissionKey OFFICE_PROFILE = new PermissionKey(Function.RESIDENT_PROFILE, Operation.CREATE);

    private final ResidentProfileService profiles;
    private final PermissionApi permissions;
    private final CurrentUser currentUser;
    private final ConsentApi consents;
    private final EndOfLifeWishService endOfLifeWishes;
    private final TreatmentWishService treatmentWishes;

    ResidentController(
            ResidentProfileService profiles,
            PermissionApi permissions,
            CurrentUser currentUser,
            ConsentApi consents,
            EndOfLifeWishService endOfLifeWishes,
            TreatmentWishService treatmentWishes) {
        this.profiles = profiles;
        this.permissions = permissions;
        this.currentUser = currentUser;
        this.consents = consents;
        this.endOfLifeWishes = endOfLifeWishes;
        this.treatmentWishes = treatmentWishes;
    }

    @GetMapping
    @Tag(name = "profiles")
    ProfilePage search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<ResidentStatus> status,
            @RequestParam(required = false) String cursor) {
        SearchPage page = profiles.search(q, status == null ? Set.of() : EnumSet.copyOf(status), cursor, 20);
        return new ProfilePage(page.items().stream().map(this::body).toList(), page.nextCursor());
    }

    @PostMapping
    @Tag(name = "profiles")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo, trạng thái ADMISSION_IN_PROGRESS"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<CreateProfileResultBody> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CreateResidentBody body) {
        CreateResult created = profiles.create(body.personalInfo(), body.confirmed());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.profile().version()))
                .body(new CreateProfileResultBody(body(created.profile()), created.warnings()));
    }

    @GetMapping("/{id}")
    @Tag(name = "profiles")
    ResponseEntity<ProfileBody> get(@PathVariable long id) {
        ProfileView view = profiles.view(ResidentProfileService.target(id));
        return ResponseEntity.ok().eTag(VersionEtag.of(view.version())).body(body(view));
    }

    @PostMapping("/{id}/update-personal-info")
    @Tag(name = "profiles")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ProfileBody> updatePersonalInfo(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody UpdatePersonalInfoBody body) {
        ProfileView updated = profiles.updatePersonalInfo(
                ResidentProfileService.target(id), ifMatch, body.personalInfo(), body.reason());
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).body(body(updated));
    }

    @GetMapping("/{id}/linked-profiles")
    @Tag(name = "profiles")
    List<LinkedProfile> linkedProfiles(@PathVariable long id) {
        return profiles.linkedProfiles(ResidentProfileService.target(id));
    }

    private static Map<String, PermissionKey> allCommands() {
        Map<String, PermissionKey> all = new HashMap<>(PROFILE_COMMANDS);
        STATUS_COMMANDS.forEach((command, key) -> all.put(command.name(), key));
        return Map.copyOf(all);
    }

    private ProfileBody body(ProfileView view) {
        Set<String> commands = new HashSet<>(
                permissions.allowedCommands(currentUser, ResidentProfileService.target(view.id()), COMMANDS));
        if (!currentUser.roles().contains(Role.DOCTOR)) {
            commands.remove(CONFIRM_INITIAL_HEALTH_RECORD);
        }
        Set<String> statusCommands =
                ResidentStatusService.availableCommands(view.status(), currentUser.roles()).stream()
                        .map(Enum::name)
                        .collect(java.util.stream.Collectors.toSet());
        STATUS_COMMANDS.keySet().stream()
                .map(Enum::name)
                .filter(c -> !statusCommands.contains(c))
                .forEach(commands::remove);
        Target target = ResidentProfileService.target(view.id());
        // FR-023m: cờ phiếu nguyện vọng khi điều trị chỉ cho người đọc được nội dung hoặc Hành chính; Kế toán không có
        boolean sees = permissions
                        .hasPermission(currentUser, SEE_TREATMENT_WISH, target)
                        .allowed()
                || permissions
                        .hasPermission(currentUser, OFFICE_PROFILE, target)
                        .allowed();
        return ProfileBody.of(
                view,
                commands,
                consents.current(view.id()).isPresent(),
                endOfLifeWishes.hasActive(view.id()),
                sees ? treatmentWishes.hasActive(view.id()) : null);
    }
}
