package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.spi.PickupRecordPort;
import com.eldercare.backend.care.api.spi.DayCarePickupPort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.family.api.events.PickupBlockNotice;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * 012 T018 (US2 kịch bản 1 → 3, 7, 9 → 11; FR-021 → FR-023, FR-026, FR-017; BR-M10-03; Q13 → Q16): ghi đón kiểm người
 * đón thuộc danh sách tại thời điểm và giấy tờ khớp; không đạt thì ghi lần chặn (còn lại sau khi lệnh bị từ chối) và báo
 * Trưởng tầng; người thực hiện phải có quyền lệnh nguồn; bản ghi đón dùng một lần qua hai port của 004, 005; bỏ người đón
 * thì bản ghi chưa dùng chuyển Không dùng ngay, kể cả khi hai lệnh chạy cùng lúc.
 */
class BR_M10_03_PickupProcessIT extends PickupTestBase {

    @Autowired
    PickupRecordPort absencePickups;

    @Autowired
    DayCarePickupPort dayCarePickups;

    @Test
    void US2_1_listedPickerWithMatchingDocument_createsUnusedRecordForTheSourceCommand() throws Exception {
        Instant before = time.now();

        JsonNode record =
                json(pickup(office, "TEMPORARY_ABSENCE", rMember, R_DOC).andExpect(status().isCreated()));

        assertThat(record.get("type").asString()).isEqualTo("TEMPORARY_ABSENCE");
        assertThat(record.get("familyMemberId").asLong()).isEqualTo(rMember);
        assertThat(record.get("basis").asString()).isEqualTo("PICKUP_LIST");
        assertThat(record.get("status").asString()).isEqualTo("UNUSED");
        Instant at = Instant.parse(record.get("at").asString());
        assertThat(at).isAfterOrEqualTo(before.minusMillis(1));
        assertThat(Instant.parse(record.get("expiresAt").asString())).isEqualTo(at.plus(Duration.ofHours(2)));
        assertThat(absencePickups.validPickupRecord(a, rMember, time.now()))
                .hasValueSatisfying(
                        v -> assertThat(v.id()).isEqualTo(record.get("id").asLong()));
        JsonNode list = getJson("/api/v1/residents/" + a + "/pickup-records");
        assertThat(list)
                .singleElement()
                .satisfies(p -> assertThat(p.get("id").asLong())
                        .isEqualTo(record.get("id").asLong()));
    }

    @Test
    void US2_2_documentMismatch_isBlocked_blockIsKept_floorManagerIsTold() throws Exception {
        int blocksBefore = blocks();

        pickup(office, "TEMPORARY_ABSENCE", mMember, "000000000000")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PICKUP_BLOCKED"))
                .andExpect(jsonPath("$.blockReason").value("ID_DOCUMENT_MISMATCH"));

        assertThat(pickupRecords()).isZero();
        assertThat(blocks()).isEqualTo(blocksBefore + 1);
        assertThat(lastBlockReason()).isEqualTo("ID_DOCUMENT_MISMATCH");
        assertThat(events.stream(PickupBlockNotice.class)).singleElement().satisfies(n -> {
            assertThat(n.residentId()).isEqualTo(a);
            assertThat(n.reason()).isEqualTo("ID_DOCUMENT_MISMATCH");
        });
    }

    @Test
    void US2_3_pickerNotOnTheList_isBlocked() throws Exception {
        pickup(office, "FAMILY_OUTING", xMember, X_DOC)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PICKUP_BLOCKED"))
                .andExpect(jsonPath("$.blockReason").value("NOT_ON_LIST"));

        assertThat(pickupRecords()).isZero();
        assertThat(lastBlockReason()).isEqualTo("NOT_ON_LIST");
    }

    @Test
    void US2_9_pickerRemovedEarlier_isBlockedLikeAnOutsider() throws Exception {
        requestRight(family(rMember), a, r, "PICKUP_ALLOWED", false).andExpect(status().isCreated());

        pickup(office, "TEMPORARY_ABSENCE", rMember, R_DOC)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.blockReason").value("NOT_ON_LIST"));
    }

    @Test
    void US2_10_selfReturn_needsTheFlag_hasNoPicker() throws Exception {
        pickup(office, "SELF_RETURN", null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.blockReason").value("SELF_RETURN_OFF"));

        selfReturn(true);
        JsonNode record = json(pickup(office, "SELF_RETURN", null, null).andExpect(status().isCreated()));

        assertThat(record.get("basis").asString()).isEqualTo("SELF_RETURN");
        assertThat(record.has("familyMemberId") && !record.get("familyMemberId").isNull())
                .isFalse();
        assertThat(dayCarePickups.valid(record.get("id").asLong(), a, time.now()))
                .hasValueSatisfying(v -> assertThat(v.pickupType()).isEqualTo("SELF_RETURN"));
    }

    @Test
    void US2_7_dayCareDeparture_isOfferedToCareAsPickup() throws Exception {
        long id = id(pickup(office, "DAY_CARE_DEPARTURE", mMember, M_DOC).andExpect(status().isCreated()));

        assertThat(dayCarePickups.valid(id, a, time.now()))
                .hasValueSatisfying(v -> assertThat(v.pickupType()).isEqualTo("PICKUP"));
        assertThat(absencePickups.validPickupRecord(a, mMember, time.now())).isEmpty();
    }

    @Test
    void Q13_recordIsUsedOnce_secondUseFails() throws Exception {
        long id = id(pickup(office, "TEMPORARY_ABSENCE", rMember, R_DOC).andExpect(status().isCreated()));

        tx.executeWithoutResult(s -> absencePickups.markUsed(id, "GRANT_TEMPORARY_ABSENCE", 1L));

        assertThatThrownBy(
                        () -> tx.executeWithoutResult(s -> absencePickups.markUsed(id, "GRANT_TEMPORARY_ABSENCE", 2L)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("PICKUP_RECORD_ALREADY_USED"));
        assertThat(absencePickups.validPickupRecord(a, rMember, time.now())).isEmpty();
        assertThat(getJson("/api/v1/residents/" + a + "/pickup-records")
                        .get(0)
                        .get("status")
                        .asString())
                .isEqualTo("USED");
    }

    @Test
    void Q14_markUsed_requiresTheSourceCommandTransaction() throws Exception {
        long id = id(pickup(office, "TEMPORARY_ABSENCE", rMember, R_DOC).andExpect(status().isCreated()));

        assertThatThrownBy(() -> absencePickups.markUsed(id, "GRANT_TEMPORARY_ABSENCE", 1L))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

    @Test
    void US2_11_removingThePicker_turnsTheUnusedRecordNotUsed_immediately() throws Exception {
        long id = id(pickup(office, "TEMPORARY_ABSENCE", mMember, M_DOC).andExpect(status().isCreated()));

        requestRight(family(rMember), a, m, "PICKUP_ALLOWED", false).andExpect(status().isCreated());

        assertThat(getJson("/api/v1/residents/" + a + "/pickup-records")
                        .get(0)
                        .get("status")
                        .asString())
                .isEqualTo("NOT_USED");
        assertThat(absencePickups.validPickupRecord(a, mMember, time.now())).isEmpty();
        assertThatThrownBy(
                        () -> tx.executeWithoutResult(s -> absencePickups.markUsed(id, "GRANT_TEMPORARY_ABSENCE", 1L)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("PICKUP_RECORD_ALREADY_USED"));
    }

    @Test
    void US2_11_nurseHasNoSourceCommandForTemporaryAbsence_isRefused() throws Exception {
        pickup(staffSession(Role.NURSING_STAFF), "TEMPORARY_ABSENCE", rMember, R_DOC)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("SOURCE_COMMAND_PERMISSION_DENIED"));
        assertThat(pickupRecords()).isZero();
    }

    @Test
    void FR_022_doctorMayEndResidencyButNotRunThePickupProcess() throws Exception {
        pickup(staffSession(Role.DOCTOR), "RESIDENCY_END", rMember, R_DOC).andExpect(status().isForbidden());
        pickup(family(rMember), "TEMPORARY_ABSENCE", rMember, R_DOC).andExpect(status().isForbidden());
    }

    @Test
    void Q15_removalAndPickupAtTheSameTime_neverLeaveAnUnusedRecordOfARemovedPicker() throws Exception {
        String rSession = family(rMember);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> remove = () -> requestRight(rSession, a, m, "PICKUP_ALLOWED", false)
                    .andReturn()
                    .getResponse()
                    .getStatus();
            Callable<Integer> record = () -> pickup(office, "TEMPORARY_ABSENCE", mMember, M_DOC)
                    .andReturn()
                    .getResponse()
                    .getStatus();
            Future<Integer> removed = pool.submit(remove);
            Future<Integer> recorded = pool.submit(record);
            assertThat(removed.get()).isEqualTo(201);
            assertThat(recorded.get()).isIn(201, 409);
        } finally {
            pool.shutdownNow();
        }

        assertThat(absencePickups.validPickupRecord(a, mMember, time.now())).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM pickup_record p WHERE p.resident_id = ? AND NOT EXISTS (SELECT 1 FROM"
                                + " record_event e WHERE e.kind = 'M10.PICKUP_RECORD_EVENT'"
                                + " AND e.entity_type = 'M10.PICKUP_RECORD' AND e.entity_id = p.id)",
                        Integer.class,
                        a))
                .isZero();
    }

    @Test
    void Q16_finalStatusResident_hasNoPickupProcess() throws Exception {
        jdbc.update("UPDATE resident SET status = 'DECEASED' WHERE id = ?", a);

        pickup(office, "RESIDENCY_END", rMember, R_DOC)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
    }
}
