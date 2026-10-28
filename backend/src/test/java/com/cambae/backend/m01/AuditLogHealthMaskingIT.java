package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.HealthItemType;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService;
import com.cambae.backend.m01.internal.health.domain.InitialHealthRecordContent;
import com.cambae.backend.m01.internal.health.service.HealthItemService;
import com.cambae.backend.m01.internal.health.service.HealthItemService.RecordCommand;
import com.cambae.backend.m01.internal.health.service.InitialHealthRecordService;
import com.cambae.backend.m15.account.api.FieldVisibilityApi;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Visibility;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import com.cambae.backend.support.TestUsers.TestUser;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 001 T011 (data-model mục 10; FR-049, BR-M15-11): nhật ký của mục sức khỏe, hồ sơ sức khỏe ban đầu và đánh giá chỉ
 * hiện giá trị cho người xem được nhóm sức khỏe. Mỗi loại đối tượng phải có phân loại trường, và mọi trường mà lệnh
 * thật ghi vào {@code audit_log} phải có trong phân loại (trường lọt ra ngoài sẽ mặc định là công khai nội bộ, nên
 * Hành chính đọc được giá trị khi 000/002 mở tra cứu theo phạm vi). Đánh giá: riêng {@code careLevel} thuộc nhóm mức
 * chăm sóc.
 */
class AuditLogHealthMaskingIT extends AssessmentTestBase {

    private static final String HEALTH_ITEM = "M01.HEALTH_ITEM";
    private static final String INITIAL_RECORD = "M01.INITIAL_HEALTH_RECORD";
    private static final String ASSESSMENT = "M01.ASSESSMENT";

    @Autowired
    List<FieldClassificationProvider> providers;

    @Autowired
    FieldVisibilityApi visibility;

    @Autowired
    HealthItemService items;

    @Autowired
    InitialHealthRecordService records;

    @Autowired
    AssessmentService assessmentService;

    @Autowired
    JsonMapper json;

    private Map<String, InfoGroup> groupsOf(String entityType) {
        return providers.stream()
                .filter(p -> p.entityType().equals(entityType))
                .findFirst()
                .map(FieldClassificationProvider::fieldGroups)
                .orElse(Map.of());
    }

    /** Mọi tên trường xuất hiện ở data_before/data_after của các dòng nhật ký của loại đối tượng này. */
    private Set<String> loggedFields(String entityType, long residentId) {
        Set<String> fields = new HashSet<>();
        List<String> rows = jdbc.queryForList(
                "SELECT CONCAT_WS('|', COALESCE(data_before, ''), COALESCE(data_after, '')) FROM audit_log"
                        + " WHERE entity_type = ? AND resident_id = ?",
                String.class,
                entityType,
                residentId);
        for (String row : rows) {
            for (String part : row.split("\\|")) {
                if (part.isBlank()) {
                    continue;
                }
                try {
                    JsonNode node = json.readTree(part);
                    fields.addAll(node.propertyNames());
                } catch (RuntimeException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return fields;
    }

    private long residentWithRecordedHealthData() {
        long resident = newResident();
        assignNurse(nurse, resident);

        user.set(doctor.user());
        items.record(
                target(resident),
                new RecordCommand(HealthItemType.ALLERGY, allergenId(), null, severityId(), sourceId(), false));
        items.record(
                target(resident),
                new RecordCommand(
                        HealthItemType.CHRONIC_CONDITION, null, "Tăng huyết áp độ 2", null, sourceId(), false));

        user.set(nurse.user());
        var draft = records.saveDraft(
                target(resident),
                null,
                new InitialHealthRecordContent(
                        "O+", "Đi lại khó", "Tự vệ sinh", "Tỉnh", "Ghi chú", "Ít muối", "Thuốc A", "Tập"));
        records.saveDraft(
                target(resident),
                "\"" + draft.version() + "\"",
                new InitialHealthRecordContent(
                        "O+", "Đi lại khó", "Tự vệ sinh", "Tỉnh", "Ghi chú mới", "Ít muối", "Thuốc B", "Tập"));

        confirmed(resident, AssessmentType.REASSESSMENT, CareLevel.REGULAR, "Nhận xét nội bộ", "Lưu ý chăm sóc");
        return resident;
    }

    @Test
    void T011_everyHealthEntityType_hasAClassification() {
        assertThat(groupsOf(HEALTH_ITEM)).isNotEmpty();
        assertThat(groupsOf(INITIAL_RECORD)).isNotEmpty();
        assertThat(groupsOf(ASSESSMENT)).isNotEmpty();
    }

    @Test
    void T011_everyFieldTheCommandsLog_isClassified_asHealth_exceptCareLevel() {
        long resident = residentWithRecordedHealthData();

        for (String type : List.of(HEALTH_ITEM, INITIAL_RECORD, ASSESSMENT)) {
            Set<String> logged = loggedFields(type, resident);
            assertThat(logged).as("trường được ghi cho %s", type).isNotEmpty();
            Map<String, InfoGroup> groups = groupsOf(type);
            assertThat(groups.keySet()).as("trường chưa phân loại của %s", type).containsAll(logged);
            Set<InfoGroup> used = logged.stream().map(groups::get).collect(Collectors.toSet());
            if (ASSESSMENT.equals(type)) {
                assertThat(groups.get("careLevel")).isEqualTo(InfoGroup.CARE_LEVEL);
                logged.stream()
                        .filter(f -> !f.equals("careLevel"))
                        .forEach(f ->
                                assertThat(groups.get(f)).as("%s.%s", type, f).isEqualTo(InfoGroup.HEALTH));
            } else {
                assertThat(used).as("nhóm của %s", type).containsExactly(InfoGroup.HEALTH);
            }
        }
    }

    @Test
    void T011_healthGroup_isVisibleToDoctorOnly_officeAndAccountingSeeOnlyThatItChanged() {
        long resident = newResident();
        assignNurse(nurse, resident);
        TestUser accountant = users.staff(Role.ACCOUNTANT);

        assertThat(visibility.canView(doctor.user(), InfoGroup.HEALTH, target(resident)))
                .isEqualTo(Visibility.FULL);
        assertThat(visibility.canView(office.user(), InfoGroup.HEALTH, target(resident)))
                .isNotEqualTo(Visibility.FULL);
        assertThat(visibility.canView(accountant.user(), InfoGroup.HEALTH, target(resident)))
                .isNotEqualTo(Visibility.FULL);
    }
}
