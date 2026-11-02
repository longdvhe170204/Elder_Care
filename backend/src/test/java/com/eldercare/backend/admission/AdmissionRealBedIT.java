package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.dto.DepositStatus;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.AssessmentTestBase;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.status.service.ResidentStatusService;
import com.eldercare.backend.roombedallocation.api.spi.AllocationResidencySource;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import com.eldercare.backend.roombedallocation.api.spi.DayCareArrivalCounter;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationCommands;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.admission.FakeDepositStatuses;
import com.eldercare.backend.support.fake.admission.FakeM02PortsConfig;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus;
import com.eldercare.backend.support.fake.roombedallocation.FakeBedAssets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T010, T070 (003 contracts mục 2.1; 001 FR-047; hồi quy 001, 003, 004): hai SPI 004 cài cho 003 chạy thật với {@code
 * AllocationStatusParticipant}, không bản giả ở phần nguồn lưu trú. Hoàn tất tiếp nhận, Chuyển viện, Ghi nhận trở về của 001 không còn
 * báo {@code ALLOCATION_SOURCE_UNAVAILABLE}, giường thật đổi trạng thái theo lệnh. Đặt cọc vẫn là bản giả vì 017 chưa có.
 */
@Import({FakeM02PortsConfig.class, AdmissionRealBedIT.OtherModulePorts.class})
class AdmissionRealBedIT extends AssessmentTestBase {

    /**
     * {@code AllocationInfectionStatus} (007, D) và {@code BedAssetPort} (019, C) chưa có bản cài production: thiếu hai cổng này thì lệnh
     * phân bổ giường của 003 trả 503 {@code SOURCE_UNAVAILABLE} (team-plan mục 8). Nguồn lưu trú của 004 vẫn là bean thật.
     */
    @TestConfiguration
    static class OtherModulePorts {
        @Bean
        FakeAllocationInfectionStatus fakeAllocationInfectionStatus() {
            return new FakeAllocationInfectionStatus();
        }

        @Bean
        FakeBedAssets fakeBedAssets() {
            return new FakeBedAssets();
        }
    }

    @Autowired
    ResidentStatusService statusService;

    @Autowired
    AllocationCommands allocations;

    @Autowired
    AllocationResidencySource source;

    @Autowired
    DayCareArrivalCounter arrivals;

    @Autowired
    FakeDepositStatuses deposits;

    private long bed;

    @BeforeEach
    void bedSetup() {
        deposits.reset();
        long area = TestUsers.nextId();
        long floor = TestUsers.nextId();
        long type = TestUsers.nextId();
        long room = TestUsers.nextId();
        bed = TestUsers.nextId();
        jdbc.update("INSERT INTO area(id,code,name) VALUES(?,?,?)", area, "A" + area, "Khu " + area);
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)", floor, area, "F" + floor, "Tầng " + floor);
        jdbc.update("INSERT INTO room_type(id,name,standard_beds) VALUES(?,?,2)", type, "Loại " + type);
        jdbc.update("INSERT INTO room_type_care_level VALUES(?,'BASIC')", type);
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(?,?,?,?,?,'UNRESTRICTED')",
                room,
                floor,
                type,
                "R" + room,
                "Phòng " + room);
        jdbc.update("INSERT INTO room_allowed_care_level VALUES(?,'BASIC')", room);
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G1')", bed, room);
    }

    private long signedContract(long residentId, ResidenceType type) {
        new ContractFixtures(jdbc).contract(residentId, ContractStatus.ACTIVE, type, CareLevel.BASIC, time.today());
        return residentId;
    }

    private long readyResident(ResidenceType type) {
        long residentId = newResident();
        setStatus(residentId, "ADMISSION_IN_PROGRESS");
        confirmed(residentId, AssessmentType.INITIAL, CareLevel.BASIC, "Nhận xét đầu vào", "Lưu ý");
        user.set(office.user());
        return signedContract(residentId, type);
    }

    private void allocate(long residentId) {
        user.set(office.user());
        allocations.create(residentId, bed, time.now(), new Reason("Phân bổ thử"));
    }

    private String bedState() {
        return jdbc.queryForObject("SELECT state FROM bed WHERE id = ?", String.class, bed);
    }

    private String statusOf(long residentId) {
        return jdbc.queryForObject("SELECT status FROM resident WHERE id = ?", String.class, residentId);
    }

    private void complete(long residentId) {
        user.set(office.user());
        statusService.completeAdmission(
                ResidentStatusService.target(residentId),
                new Reason("Hoàn tất thử"),
                JsonNodeFactory.instance.objectNode());
    }

    @SuppressWarnings("unchecked")
    private static List<String> unmetCodes(BusinessException e) {
        return ((List<Condition>) e.properties().get("unmetConditions"))
                .stream().map(Condition::code).toList();
    }

    @Test
    void FR_047_inpatientWithoutBed_isBlockedByTheBedCondition_notBySourceUnavailable() {
        long residentId = readyResident(ResidenceType.LONG_TERM_RESIDENTIAL);

        try {
            complete(residentId);
            throw new AssertionError("lệnh phải bị từ chối");
        } catch (BusinessException e) {
            assertThat(unmetCodes(e))
                    .contains("RESERVED_BED_NOT_READY")
                    .doesNotContain("ALLOCATION_SOURCE_UNAVAILABLE");
        }
        assertThat(statusOf(residentId)).isEqualTo("ADMISSION_IN_PROGRESS");
    }

    @Test
    void FR_047_inpatientWithARealBed_completesAdmission_thenTransferOutAndReturn_keepTheBed() {
        long residentId = readyResident(ResidenceType.LONG_TERM_RESIDENTIAL);
        allocate(residentId);

        complete(residentId);

        assertThat(statusOf(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE.name());
        assertThat(bedState()).isEqualTo("OCCUPIED");

        user.set(doctor.user());
        ObjectNode transfer = JsonNodeFactory.instance.objectNode();
        transfer.put("transferUnit", "Bệnh viện Đa khoa");
        transfer.put("handoverStaffId", doctor.staffId());
        transfer.putObject("basis").put("type", "FAMILY_INFORMATION").put("note", "Gia đình báo");
        statusService.transferOut(ResidentStatusService.target(residentId), new Reason("Chuyển viện thử"), transfer);

        assertThat(statusOf(residentId)).isEqualTo(ResidentStatus.HOSPITALIZED.name());
        assertThat(bedState()).as("Q-395: vắng luôn giữ giường").isEqualTo("HELD");

        user.set(office.user());
        statusService.recordReturn(
                ResidentStatusService.target(residentId),
                new Reason("Về viện thử"),
                JsonNodeFactory.instance.objectNode());

        assertThat(statusOf(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE.name());
        assertThat(bedState()).isEqualTo("OCCUPIED");
    }

    @Test
    void FR_018_dayCareResident_needsNoBed() {
        long residentId = readyResident(ResidenceType.DAY_CARE);

        complete(residentId);

        assertThat(statusOf(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE.name());
    }

    @Test
    void CHK007_residencyType_contractFirst_thenDraft_thenApplication() {
        long residentId = newResident();
        setStatus(residentId, "ADMISSION_IN_PROGRESS");
        assertThat(source.residencyType(residentId)).isEmpty();

        ContractFixtures contracts = new ContractFixtures(jdbc);
        contracts.contract(
                residentId, ContractStatus.DRAFT, ResidenceType.SHORT_TERM_RESIDENTIAL, CareLevel.BASIC, time.today());
        assertThat(source.residencyType(residentId)).contains(ResidencyType.SHORT_TERM);
        assertThat(source.careLevelFromContract(residentId)).contains(CareLevelCode.BASIC);
        Instant end = source.expectedEnd(residentId).orElseThrow();
        assertThat(end).isAfter(time.now());

        contracts.contract(residentId, ContractStatus.ACTIVE, ResidenceType.DAY_CARE, CareLevel.BASIC, time.today());
        assertThat(source.residencyType(residentId)).contains(ResidencyType.DAY_CARE);
        assertThat(source.expectedEnd(residentId)).as("chỉ nội trú ngắn ngày").isEmpty();
    }

    @Test
    void Q_395_holdBedWhenAbsent_isAlwaysHeld_withoutLimit() {
        var hold = source.holdBedWhenAbsent(newResident());

        assertThat(hold.held()).isTrue();
        assertThat(hold.until()).isEmpty();
    }

    @Test
    void FR_030_differentUnitPrice_comparesFeeServices_andMissingPriceCountsAsDifferent() {
        long cheap = TestUsers.nextId();
        long dear = TestUsers.nextId();
        long none = TestUsers.nextId();
        LocalDate date = time.today();
        price(cheap, 1_000_000);
        price(dear, 2_000_000);

        assertThat(source.differentUnitPrice(1L, cheap, cheap, date)).isFalse();
        assertThat(source.differentUnitPrice(1L, cheap, dear, date)).isTrue();
        assertThat(source.differentUnitPrice(1L, cheap, none, date)).isTrue();
    }

    private void price(long roomTypeId, int amount) {
        String name = "Phí " + roomTypeId;
        jdbc.update(
                M02Sql.INSERT_SERVICE,
                name,
                M02Sql.attributes("RESIDENCY_FEE", "tháng", "MONTH", null, roomTypeId, null));
        long id = jdbc.queryForObject(M02Sql.SERVICE_ID, Long.class, name);
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from) VALUES (?, ?, '2020-01-01')",
                id,
                amount);
    }

    @Test
    void FR_040_futureArrivalsAndCounts_comeFromActiveDayCareContracts() {
        long residentId = newResident();
        long contractId = new ContractFixtures(jdbc)
                .contract(residentId, ContractStatus.ACTIVE, ResidenceType.DAY_CARE, CareLevel.BASIC, time.today());
        LocalDate monday = time.today().plusDays(8 - time.today().getDayOfWeek().getValue());
        jdbc.update(
                "INSERT INTO contract_version (kind, contract_id, sequence_no, effective_from, content, residence_type,"
                        + " care_level, created_at, locked_at) VALUES ('CONTENT', ?, 0, ?, ?, 'DAY_CARE', 'BASIC', NOW(6),"
                        + " NOW(6))",
                contractId,
                time.today(),
                "{\"residenceType\":\"DAY_CARE\",\"arrivalSchedule\":[{\"dayOfWeek\":1,\"date\":null,"
                        + "\"arrivalTime\":\"07:30\",\"departureTime\":\"11:00\"}]}");
        var morning = new com.eldercare.backend.roombedallocation.api.dto.DayCareSession(
                7L, "Sáng", java.time.LocalTime.of(7, 0), java.time.LocalTime.of(11, 30), true);
        var afternoon = new com.eldercare.backend.roombedallocation.api.dto.DayCareSession(
                8L, "Chiều", java.time.LocalTime.of(13, 0), java.time.LocalTime.of(17, 0), true);

        var counts = arrivals.countFrom(time.today(), List.of(morning, afternoon));

        assertThat(arrivals.hasFutureArrivals(time.today())).isTrue();
        assertThat(counts.get(new com.eldercare.backend.roombedallocation.api.dto.SessionDate(monday, 7L)))
                .isGreaterThanOrEqualTo(1);
        assertThat(counts)
                .doesNotContainKey(
                        new com.eldercare.backend.roombedallocation.api.dto.SessionDate(monday.plusDays(1), 7L));
        assertThat(DepositStatus.values()).isNotEmpty();
    }
}
