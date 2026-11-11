package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.ContractStatus;
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
import com.eldercare.backend.roombedallocation.api.spi.DayCareArrivalCounter;
import com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationCommands;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.admission.FakeDepositStatuses;
import com.eldercare.backend.support.fake.admission.FakeM02PortsConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T010, T070 (003 contracts mục 2.1; 001 FR-047; hồi quy 001, 003, 004): hai SPI 004 cài cho 003 chạy thật với {@code
 * AllocationStatusParticipant}, không bản giả ở phần nguồn lưu trú. Hoàn tất tiếp nhận, Chuyển viện, Ghi nhận trở về của 001 không còn
 * báo {@code ALLOCATION_SOURCE_UNAVAILABLE}, giường thật đổi trạng thái theo lệnh. Đặt cọc vẫn là bản giả vì 017 chưa có.
 * Điều kiện DBR_02 của 012 không thuộc phạm vi test này (bean của 012 thay bằng mock; ca thật ở DBR_02_PrimaryContactIT).
 */
@org.springframework.test.context.bean.override.mockito.MockitoBean(
        name = "m10StatusParticipant",
        types = com.eldercare.backend.resident.api.spi.StatusTransitionParticipant.class)
@Import(FakeM02PortsConfig.class)
class AdmissionRealBedIT extends AssessmentTestBase {

    // 003 T063 (2026-11-11): nguồn lưu trú của 004, {@code BedAssetPort} (019) và {@code AllocationInfectionStatus}
    // (007
    // {@code IncidentAllocationStatus}) đều là bean thật; trước đây cổng của 007 là bản giả.

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
        jdbc.update("UPDATE room SET allowed_care_levels=JSON_ARRAY('BASIC') WHERE id=?", room);
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
}
