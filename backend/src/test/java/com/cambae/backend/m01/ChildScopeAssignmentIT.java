package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ConsenterType;
import com.cambae.backend.m01.api.dto.HealthItemType;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.WishRecordSource;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.consent.service.ConsentService;
import com.cambae.backend.m01.internal.consent.service.ConsentService.RecordConsent;
import com.cambae.backend.m01.internal.health.domain.InitialHealthRecordContent;
import com.cambae.backend.m01.internal.health.service.HealthItemService;
import com.cambae.backend.m01.internal.health.service.HealthItemService.RecordCommand;
import com.cambae.backend.m01.internal.health.service.InitialHealthRecordService;
import com.cambae.backend.m01.internal.wish.service.EndOfLifeWishService;
import com.cambae.backend.m01.internal.wish.service.TreatmentWishService;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import com.cambae.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 001 T011 (002 contracts mục 1.9; FR-034): M01 gắn phạm vi cho hồ sơ người cao tuổi và mọi bản ghi con có nhật ký
 * (lịch sử trạng thái, đồng ý, hồ sơ sức khỏe ban đầu, mục sức khỏe, đánh giá, phiếu nguyện vọng cuối đời, phiếu
 * nguyện vọng khi điều trị): {@code Target} mang {@code residentId} của chủ bản ghi để 002 tính phạm vi theo phân bổ
 * giường và quan hệ người thân. Tầng/khu vực để trống (lấy từ phân bổ giường của 003). Mã không tồn tại thì không có
 * người cao tuổi, nên phạm vi không bao giờ mở ra cho bản ghi lạ.
 */
@Import(FakeFileConfig.class)
class ChildScopeAssignmentIT extends StatusTestBase {

    private static final Reason REASON = new Reason("Kiểm thử phạm vi");

    @Autowired
    List<ScopeAssignmentProvider> providers;

    @Autowired
    FileApi files;

    @Autowired
    HealthItemService items;

    @Autowired
    InitialHealthRecordService records;

    @Autowired
    ConsentService consents;

    @Autowired
    EndOfLifeWishService wishes;

    @Autowired
    TreatmentWishService treatments;

    private long pdf() {
        return files.upload(
                "ban-ky.pdf", FileTypes.PDF, "%PDF-1.4 thử".getBytes(StandardCharsets.UTF_8), office.accountId());
    }

    private Target scopeOf(String entityType, long id) {
        return providers.stream()
                .filter(p -> p.entityType().equals(entityType))
                .findFirst()
                .orElseThrow(() -> new AssertionError("chưa có provider cho " + entityType))
                .assignScope(id);
    }

    @Test
    void T011_everyM01EntityType_resolvesToTheOwningResident() {
        long resident = newResident();
        setStatus(resident, "ADMISSION_IN_PROGRESS");
        confirmed(resident, AssessmentType.INITIAL, CareLevel.REGULAR, "Nhận xét", "Lưu ý");
        setStatus(resident, "IN_RESIDENCE");
        Map<String, Long> ids = new LinkedHashMap<>();
        ids.put(
                "M01.ASSESSMENT",
                jdbc.queryForObject("SELECT id FROM assessment WHERE resident_id = ?", Long.class, resident));

        user.set(doctor.user());
        ids.put(
                "M01.HEALTH_ITEM",
                items.record(
                                target(resident),
                                new RecordCommand(
                                        HealthItemType.ALLERGY, allergenId(), null, severityId(), sourceId(), false))
                        .item()
                        .id());
        assignNurse(nurse, resident);
        user.set(nurse.user());
        ids.put(
                "M01.INITIAL_HEALTH_RECORD",
                records.saveDraft(
                                target(resident),
                                null,
                                new InitialHealthRecordContent(
                                        "O+", "Đi lại khó", "Tự vệ sinh", "Tỉnh", "Ghi", "Ít muối", "Thuốc A", "Tập"))
                        .id());
        ids.put(
                "M01.END_OF_LIFE_WISH",
                wishes.record(
                                target(resident),
                                new WishCommandBuilder(resident, doctor.accountId(), pdf()).build(),
                                WishRecordSource.ADMISSION)
                        .id());
        ids.put(
                "M01.TREATMENT_WISH",
                treatments
                        .record(target(resident), new TreatmentCommandBuilder().build())
                        .wish()
                        .id());
        user.set(office.user());
        ids.put(
                "M01.CONSENT",
                consents.record(
                                target(resident),
                                new RecordConsent(
                                        ConsenterType.RESIDENT, null, null, Set.of(), true, true, Instant.now(), pdf()))
                        .id());
        statusService.grantTemporaryAbsence(target(resident), REASON, noDetails());
        ids.put(
                "M01.RESIDENT_STATUS_HISTORY",
                jdbc.queryForObject(
                        "SELECT id FROM resident_status_history WHERE resident_id = ? ORDER BY id DESC LIMIT 1",
                        Long.class,
                        resident));
        ids.put("M01.RESIDENT", resident);

        ids.forEach((type, id) -> {
            Target target = scopeOf(type, id);
            assertThat(target.residentId()).as("resident của %s", type).isEqualTo(resident);
            assertThat(target.type()).isEqualTo(type);
            assertThat(target.id()).isEqualTo(String.valueOf(id));
            assertThat(target.floorAreaId())
                    .as("tầng của %s do 003 cung cấp", type)
                    .isNull();
        });
    }

    @Test
    void T011_unknownRecord_hasNoResident() {
        for (String type : List.of(
                "M01.ASSESSMENT",
                "M01.HEALTH_ITEM",
                "M01.INITIAL_HEALTH_RECORD",
                "M01.END_OF_LIFE_WISH",
                "M01.TREATMENT_WISH",
                "M01.CONSENT",
                "M01.RESIDENT_STATUS_HISTORY")) {
            assertThat(scopeOf(type, 987_654_321L).residentId()).as(type).isNull();
        }
    }

    @Test
    void T011_statusOfTheResidentIsUntouched() {
        long resident = residentIn(ResidentStatus.IN_RESIDENCE);

        scopeOf("M01.RESIDENT", resident);

        assertThat(currentStatus(resident)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }
}
