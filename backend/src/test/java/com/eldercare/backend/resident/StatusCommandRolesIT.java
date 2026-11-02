package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Hồi quy lỗi quyền phát hiện khi B tự kiểm thử 2026-11-02: {@code demo.ketoan} Kết thúc lưu trú được hồ sơ
 * NCT-2026-000008. Ma trận thật (không dùng bản giả quyền) cấp ô T²⁶, T³⁹ cho Kế toán và T⁸ cho Quản lý viện, nên chỉ kiểm
 * khóa quyền là chưa đủ: chú thích mô tả do module sở hữu lệnh kiểm ({@code FootnoteRules}). Bảng dưới theo spec 001 mục
 * "Bảng trạng thái" và data-model cột "Người thực hiện"; mọi vai trò nhân viên × mọi lệnh.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(FakeFileConfig.class)
class StatusCommandRolesIT extends StatusTestBase {

    private record Command(ResidentStatus from, String path, Set<Role> roles) {}

    private static final Map<StatusCommand, Command> COMMANDS = Map.of(
            StatusCommand.COMPLETE_ADMISSION,
            new Command(ResidentStatus.ADMISSION_IN_PROGRESS, "complete-admission", Set.of(Role.OFFICE_STAFF)),
            StatusCommand.CANCEL_ADMISSION,
            new Command(ResidentStatus.ADMISSION_IN_PROGRESS, "cancel-admission", Set.of(Role.OFFICE_STAFF)),
            StatusCommand.GRANT_TEMPORARY_ABSENCE,
            new Command(
                    ResidentStatus.IN_RESIDENCE,
                    "grant-temporary-absence",
                    Set.of(Role.OFFICE_STAFF, Role.FLOOR_MANAGER)),
            StatusCommand.RECORD_RETURN,
            new Command(
                    ResidentStatus.TEMPORARILY_ABSENT, "record-return", Set.of(Role.OFFICE_STAFF, Role.FLOOR_MANAGER)),
            StatusCommand.TRANSFER_OUT,
            new Command(ResidentStatus.IN_RESIDENCE, "transfer-out", Set.of(Role.DOCTOR, Role.NURSING_STAFF)),
            StatusCommand.END_RESIDENCY,
            new Command(ResidentStatus.IN_RESIDENCE, "end-residency", Set.of(Role.OFFICE_STAFF, Role.DOCTOR)),
            // Ngoài viện: Hành chính được làm khi kèm bằng chứng; tại viện chỉ Bác sĩ đã có ở RecordDeathIT
            StatusCommand.RECORD_DEATH,
            new Command(ResidentStatus.TEMPORARILY_ABSENT, "record-death", Set.of(Role.OFFICE_STAFF, Role.DOCTOR)));

    private static final Set<Role> STAFF_ROLES = EnumSet.of(
            Role.NURSING_HOME_MANAGER,
            Role.FLOOR_MANAGER,
            Role.DOCTOR,
            Role.NURSING_STAFF,
            Role.CAREGIVER,
            Role.KITCHEN_STAFF,
            Role.OFFICE_STAFF,
            Role.ACCOUNTANT);

    @Autowired
    MockMvc mvc;

    @Autowired
    FileApi files;

    static Stream<Arguments> everyRoleAndCommand() {
        List<Arguments> all = new ArrayList<>();
        for (Role role : STAFF_ROLES) {
            for (StatusCommand command : COMMANDS.keySet()) {
                all.add(Arguments.of(role, command));
            }
        }
        return all.stream();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("everyRoleAndCommand")
    void onlyTheRolesOfTheStatusTable_canRunTheCommand(Role role, StatusCommand command) throws Exception {
        Command spec = COMMANDS.get(command);
        long id = residentIn(spec.from());
        if (command == StatusCommand.COMPLETE_ADMISSION) {
            confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Nhận xét đầu vào", "Lưu ý");
        }
        TestUser actor = actor(id, role);
        user.set(actor.user());
        int historyBefore = historyCount(id);
        int auditBefore = auditCount(id);

        var result = mvc.perform(post("/api/v1/residents/" + id + "/" + spec.path())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(command)));

        if (spec.roles().contains(role)) {
            result.andExpect(status().isOk());
            assertThat(currentStatus(id)).isNotEqualTo(spec.from());
            assertThat(historyCount(id)).isEqualTo(historyBefore + 1);
        } else {
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("PERMISSION_DENIED"));
            assertThat(currentStatus(id)).isEqualTo(spec.from());
            assertThat(historyCount(id)).isEqualTo(historyBefore);
            assertThat(auditCount(id)).isEqualTo(auditBefore);
        }
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("everyRoleAndCommand")
    void allowedCommands_matchTheStatusTable(Role role, StatusCommand command) throws Exception {
        Command spec = COMMANDS.get(command);
        long id = residentIn(spec.from());
        user.set(actor(id, role).user());

        // Bếp không có ô nào ở dòng Hồ sơ người cao tuổi (Phụ lục 27): không mở được hồ sơ thì không có nút lệnh
        if (role == Role.KITCHEN_STAFF) {
            mvc.perform(get("/api/v1/residents/" + id)).andExpect(status().isForbidden());
            return;
        }
        var profile = mvc.perform(get("/api/v1/residents/" + id)).andExpect(status().isOk());

        if (spec.roles().contains(role)) {
            profile.andExpect(jsonPath("$.allowedCommands", hasItem(command.name())));
        } else {
            profile.andExpect(jsonPath("$.allowedCommands", not(hasItem(command.name()))));
        }
    }

    /** Tài khoản nhiều vai trò: giới hạn là của vai trò được phép, nên Kế toán kiêm Hành chính vẫn thực hiện được. */
    @ParameterizedTest
    @EnumSource(
            value = StatusCommand.class,
            names = {"CANCEL_ADMISSION", "END_RESIDENCY", "RECORD_DEATH"})
    void accountantWhoIsAlsoOfficeStaff_keepsTheCommandsOfOfficeStaff(StatusCommand command) throws Exception {
        Command spec = COMMANDS.get(command);
        long id = residentIn(spec.from());
        user.set(users.staff(Role.ACCOUNTANT, Role.OFFICE_STAFF).user());

        mvc.perform(post("/api/v1/residents/" + id + "/" + spec.path())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(command)))
                .andExpect(status().isOk());
    }

    private TestUser actor(long residentId, Role role) {
        TestUser actor = users.staff(role);
        if (role == Role.FLOOR_MANAGER || role == Role.NURSING_STAFF || role == Role.CAREGIVER) {
            assignNurse(actor, residentId);
        }
        return actor;
    }

    private String body(StatusCommand command) {
        String details = switch (command) {
            case TRANSFER_OUT -> "{\"basis\":{\"note\":\"Chỉ định của bác sĩ\"}}";
            case RECORD_DEATH -> "{\"evidenceFileId\":" + evidence() + "}";
            default -> "{}";
        };
        return "{\"reason\":\"Lý do thử\",\"details\":" + details + "}";
    }

    private long evidence() {
        return files.upload(
                "giay-bao-tu.pdf",
                FileTypes.PDF,
                "%PDF-1.4 giấy báo tử".getBytes(StandardCharsets.UTF_8),
                office.accountId());
    }

    private int auditCount(long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M01.RESIDENT' AND entity_id = ?",
                Integer.class,
                residentId);
    }
}
